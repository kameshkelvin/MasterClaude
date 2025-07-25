package com.examSystem.userService.service.security;

import com.examSystem.userService.entity.ExamAttempt;
import com.examSystem.userService.entity.SecurityLog;
import com.examSystem.userService.repository.ExamAttemptRepository;
import com.examSystem.userService.repository.SecurityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * 考试安全服务
 * 
 * 提供考试过程中的安全监控和防作弊功能：
 * - 会话完整性验证
 * - 防重复提交
 * - 异常行为检测
 * - 时间窗口验证
 * - IP地址监控
 */
@Service
@Transactional
public class ExamSecurityService {

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private SecurityLogRepository securityLogRepository;

    // 内存中的会话令牌存储
    private final Map<String, SessionToken> activeTokens = new ConcurrentHashMap<>();
    
    // 防重复提交的请求记录
    private final Map<String, Long> recentRequests = new ConcurrentHashMap<>();

    /**
     * 生成安全会话令牌
     */
    public SessionToken generateSessionToken(Long attemptId, Long studentId, String clientInfo) {
        try {
            String tokenData = attemptId + ":" + studentId + ":" + clientInfo + ":" + System.currentTimeMillis();
            String tokenHash = calculateHash(tokenData);
            
            SessionToken token = new SessionToken(
                tokenHash,
                attemptId,
                studentId,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(4), // 4小时有效期
                clientInfo
            );
            
            activeTokens.put(tokenHash, token);
            
            // 记录安全日志
            logSecurityEvent(studentId, attemptId, "TOKEN_GENERATED", 
                "会话令牌生成", clientInfo, SecurityLevel.INFO);
            
            return token;
        } catch (Exception e) {
            throw new RuntimeException("生成会话令牌失败", e);
        }
    }

    /**
     * 验证会话令牌
     */
    public TokenValidationResult validateSessionToken(String token, Long attemptId, Long studentId) {
        SessionToken sessionToken = activeTokens.get(token);
        
        if (sessionToken == null) {
            logSecurityEvent(studentId, attemptId, "TOKEN_INVALID", 
                "无效的会话令牌", null, SecurityLevel.WARNING);
            return new TokenValidationResult(false, "会话令牌无效");
        }
        
        if (sessionToken.isExpired()) {
            activeTokens.remove(token);
            logSecurityEvent(studentId, attemptId, "TOKEN_EXPIRED", 
                "会话令牌过期", null, SecurityLevel.WARNING);
            return new TokenValidationResult(false, "会话令牌已过期");
        }
        
        if (!sessionToken.getAttemptId().equals(attemptId) || 
            !sessionToken.getStudentId().equals(studentId)) {
            logSecurityEvent(studentId, attemptId, "TOKEN_MISMATCH", 
                "会话令牌不匹配", null, SecurityLevel.CRITICAL);
            return new TokenValidationResult(false, "会话令牌不匹配");
        }
        
        // 更新最后访问时间
        sessionToken.updateLastAccess();
        
        return new TokenValidationResult(true, "令牌验证成功");
    }

    /**
     * 防重复提交检查
     */
    public DuplicateSubmissionResult checkDuplicateSubmission(Long studentId, Long questionId, 
                                                            String answer, String clientFingerprint) {
        String requestKey = studentId + ":" + questionId + ":" + calculateHash(answer);
        Long lastSubmissionTime = recentRequests.get(requestKey);
        
        long currentTime = System.currentTimeMillis();
        
        if (lastSubmissionTime != null && (currentTime - lastSubmissionTime) < 2000) { // 2秒内重复提交
            logSecurityEvent(studentId, null, "DUPLICATE_SUBMISSION", 
                "重复提交检测", "题目ID: " + questionId, SecurityLevel.WARNING);
            return new DuplicateSubmissionResult(true, "请勿重复提交，请等待2秒后再试");
        }
        
        recentRequests.put(requestKey, currentTime);
        
        // 清理过期的请求记录
        cleanupExpiredRequests();
        
        return new DuplicateSubmissionResult(false, "提交检查通过");
    }

    /**
     * 异常行为检测
     */
    public AnomalyDetectionResult detectAnomalies(Long studentId, Long attemptId, 
                                                 ExamBehaviorData behaviorData) {
        List<String> anomalies = new java.util.ArrayList<>();
        SecurityLevel maxLevel = SecurityLevel.INFO;
        
        // 1. 答题速度异常检测
        if (behaviorData.getAverageTimePerQuestion() < 5) { // 平均每题少于5秒
            anomalies.add("答题速度异常快");
            maxLevel = SecurityLevel.WARNING;
        }
        
        // 2. 连续正确率异常检测
        if (behaviorData.getConsecutiveCorrectStreak() > 15 && 
            behaviorData.getOverallAccuracy() > 0.95) {
            anomalies.add("连续正确率异常高");
            maxLevel = SecurityLevel.WARNING;
        }
        
        // 3. 操作模式异常检测
        if (behaviorData.getWindowSwitchCount() > 10) {
            anomalies.add("频繁切换窗口");
            maxLevel = SecurityLevel.CRITICAL;
        }
        
        // 4. 时间分布异常检测
        if (behaviorData.hasIrregularTimingPattern()) {
            anomalies.add("答题时间分布异常");
            maxLevel = SecurityLevel.WARNING;
        }
        
        // 5. 复制粘贴行为检测
        if (behaviorData.getPasteActionCount() > 5) {
            anomalies.add("频繁复制粘贴操作");
            maxLevel = SecurityLevel.CRITICAL;
        }
        
        // 记录异常行为
        if (!anomalies.isEmpty()) {
            String anomalyDetails = String.join("; ", anomalies);
            logSecurityEvent(studentId, attemptId, "ANOMALY_DETECTED", 
                "检测到异常行为", anomalyDetails, maxLevel);
            
            // 更新考试尝试的安全评分
            updateSecurityScore(attemptId, anomalies.size());
        }
        
        return new AnomalyDetectionResult(
            !anomalies.isEmpty(),
            anomalies,
            maxLevel,
            calculateRiskScore(anomalies.size(), maxLevel)
        );
    }

    /**
     * 时间窗口验证
     */
    public TimeWindowValidationResult validateTimeWindow(Long attemptId, LocalDateTime actionTime) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("考试记录不存在"));
        
        LocalDateTime startTime = attempt.getStartTime();
        LocalDateTime endTime = attempt.getEndTime();
        
        if (actionTime.isBefore(startTime)) {
            logSecurityEvent(attempt.getStudentId(), attemptId, "TIME_VIOLATION", 
                "考试开始前操作", "操作时间: " + actionTime, SecurityLevel.CRITICAL);
            return new TimeWindowValidationResult(false, "考试尚未开始");
        }
        
        if (actionTime.isAfter(endTime)) {
            logSecurityEvent(attempt.getStudentId(), attemptId, "TIME_VIOLATION", 
                "考试结束后操作", "操作时间: " + actionTime, SecurityLevel.CRITICAL);
            return new TimeWindowValidationResult(false, "考试已结束");
        }
        
        // 检查是否在合理的时间缓冲区内
        LocalDateTime bufferEnd = endTime.plusMinutes(2); // 允许2分钟缓冲
        if (actionTime.isAfter(endTime) && actionTime.isBefore(bufferEnd)) {
            logSecurityEvent(attempt.getStudentId(), attemptId, "TIME_BUFFER", 
                "时间缓冲区操作", "操作时间: " + actionTime, SecurityLevel.WARNING);
            return new TimeWindowValidationResult(true, "在时间缓冲区内");
        }
        
        return new TimeWindowValidationResult(true, "时间验证通过");
    }

    /**
     * IP地址监控
     */
    public IPMonitoringResult monitorIPAddress(Long studentId, Long attemptId, String currentIP) {
        List<String> previousIPs = securityLogRepository.findRecentIPsByStudentAndAttempt(
            studentId, attemptId, LocalDateTime.now().minusHours(1));
        
        if (previousIPs.isEmpty()) {
            // 首次访问，记录IP
            logSecurityEvent(studentId, attemptId, "IP_FIRST_ACCESS", 
                "首次IP访问", "IP: " + currentIP, SecurityLevel.INFO);
            return new IPMonitoringResult(true, "首次IP访问");
        }
        
        if (!previousIPs.contains(currentIP)) {
            // IP地址变更
            logSecurityEvent(studentId, attemptId, "IP_CHANGE", 
                "IP地址变更", "新IP: " + currentIP + ", 之前IP: " + String.join(",", previousIPs), 
                SecurityLevel.WARNING);
            
            // 如果IP变更次数过多，提升安全级别
            if (previousIPs.size() > 3) {
                return new IPMonitoringResult(false, "IP地址变更过于频繁，存在安全风险");
            }
            
            return new IPMonitoringResult(true, "检测到IP地址变更");
        }
        
        return new IPMonitoringResult(true, "IP地址验证通过");
    }

    /**
     * 获取学生的安全违规记录
     */
    @Transactional(readOnly = true)
    public List<SecurityViolation> getSecurityViolations(Long studentId, Long attemptId) {
        List<SecurityLog> logs = securityLogRepository.findViolationsByStudentAndAttempt(studentId, attemptId);
        
        return logs.stream().map(log -> new SecurityViolation(
            log.getId(),
            log.getEventType(),
            log.getDescription(),
            log.getDetails(),
            log.getSecurityLevel().toString(),
            log.getCreatedAt()
        )).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 计算考试的整体安全评分
     */
    @Transactional(readOnly = true)
    public SecurityScore calculateSecurityScore(Long attemptId) {
        List<SecurityLog> logs = securityLogRepository.findByAttemptId(attemptId);
        
        int totalViolations = 0;
        int criticalViolations = 0;
        int warningViolations = 0;
        
        for (SecurityLog log : logs) {
            if (log.getSecurityLevel() == SecurityLevel.CRITICAL) {
                criticalViolations++;
                totalViolations += 3; // 严重违规权重更高
            } else if (log.getSecurityLevel() == SecurityLevel.WARNING) {
                warningViolations++;
                totalViolations += 1;
            }
        }
        
        // 计算安全评分 (0-100分，分数越低风险越高)
        int baseScore = 100;
        int deduction = Math.min(totalViolations * 5, 80); // 最多扣80分
        int finalScore = Math.max(baseScore - deduction, 0);
        
        SecurityRiskLevel riskLevel;
        if (finalScore >= 90) {
            riskLevel = SecurityRiskLevel.LOW;
        } else if (finalScore >= 70) {
            riskLevel = SecurityRiskLevel.MEDIUM;
        } else if (finalScore >= 50) {
            riskLevel = SecurityRiskLevel.HIGH;
        } else {
            riskLevel = SecurityRiskLevel.CRITICAL;
        }
        
        return new SecurityScore(
            finalScore,
            riskLevel,
            totalViolations,
            criticalViolations,
            warningViolations,
            logs.size()
        );
    }

    /**
     * 清理过期的会话令牌
     */
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        activeTokens.entrySet().removeIf(entry -> entry.getValue().getExpiresAt().isBefore(now));
    }

    // 私有辅助方法

    private void logSecurityEvent(Long studentId, Long attemptId, String eventType, 
                                 String description, String details, SecurityLevel level) {
        SecurityLog log = new SecurityLog();
        log.setStudentId(studentId);
        log.setAttemptId(attemptId);
        log.setEventType(eventType);
        log.setDescription(description);
        log.setDetails(details);
        log.setSecurityLevel(level);
        log.setCreatedAt(LocalDateTime.now());
        log.setClientIP(getCurrentClientIP());
        
        securityLogRepository.save(log);
    }

    private void updateSecurityScore(Long attemptId, int violationCount) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId).orElse(null);
        if (attempt != null) {
            Integer currentScore = attempt.getSecurityScore();
            int newScore = (currentScore != null ? currentScore : 100) - (violationCount * 5);
            attempt.setSecurityScore(Math.max(newScore, 0));
            examAttemptRepository.save(attempt);
        }
    }

    private int calculateRiskScore(int anomalyCount, SecurityLevel maxLevel) {
        int baseScore = anomalyCount * 10;
        if (maxLevel == SecurityLevel.CRITICAL) {
            baseScore += 30;
        } else if (maxLevel == SecurityLevel.WARNING) {
            baseScore += 15;
        }
        return Math.min(baseScore, 100);
    }

    private void cleanupExpiredRequests() {
        long cutoffTime = System.currentTimeMillis() - 300000; // 5分钟前
        recentRequests.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
    }

    private String calculateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("计算哈希失败", e);
        }
    }

    private String getCurrentClientIP() {
        // 这里应该从HTTP请求中获取真实IP
        // 暂时返回占位符
        return "127.0.0.1";
    }

    // 内部类定义

    public static class SessionToken {
        private final String token;
        private final Long attemptId;
        private final Long studentId;
        private final LocalDateTime createdAt;
        private final LocalDateTime expiresAt;
        private final String clientInfo;
        private LocalDateTime lastAccessAt;

        public SessionToken(String token, Long attemptId, Long studentId,
                          LocalDateTime createdAt, LocalDateTime expiresAt, String clientInfo) {
            this.token = token;
            this.attemptId = attemptId;
            this.studentId = studentId;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.clientInfo = clientInfo;
            this.lastAccessAt = createdAt;
        }

        public void updateLastAccess() {
            this.lastAccessAt = LocalDateTime.now();
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }

        // Getters
        public String getToken() { return token; }
        public Long getAttemptId() { return attemptId; }
        public Long getStudentId() { return studentId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public String getClientInfo() { return clientInfo; }
        public LocalDateTime getLastAccessAt() { return lastAccessAt; }
    }

    public static class TokenValidationResult {
        private final boolean valid;
        private final String message;

        public TokenValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }

    public static class DuplicateSubmissionResult {
        private final boolean isDuplicate;
        private final String message;

        public DuplicateSubmissionResult(boolean isDuplicate, String message) {
            this.isDuplicate = isDuplicate;
            this.message = message;
        }

        public boolean isDuplicate() { return isDuplicate; }
        public String getMessage() { return message; }
    }

    public static class AnomalyDetectionResult {
        private final boolean hasAnomalies;
        private final List<String> anomalies;
        private final SecurityLevel maxLevel;
        private final int riskScore;

        public AnomalyDetectionResult(boolean hasAnomalies, List<String> anomalies,
                                    SecurityLevel maxLevel, int riskScore) {
            this.hasAnomalies = hasAnomalies;
            this.anomalies = anomalies;
            this.maxLevel = maxLevel;
            this.riskScore = riskScore;
        }

        public boolean hasAnomalies() { return hasAnomalies; }
        public List<String> getAnomalies() { return anomalies; }
        public SecurityLevel getMaxLevel() { return maxLevel; }
        public int getRiskScore() { return riskScore; }
    }

    public static class TimeWindowValidationResult {
        private final boolean valid;
        private final String message;

        public TimeWindowValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }

    public static class IPMonitoringResult {
        private final boolean allowed;
        private final String message;

        public IPMonitoringResult(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }

        public boolean isAllowed() { return allowed; }
        public String getMessage() { return message; }
    }

    public static class SecurityViolation {
        private final Long id;
        private final String eventType;
        private final String description;
        private final String details;
        private final String securityLevel;
        private final LocalDateTime timestamp;

        public SecurityViolation(Long id, String eventType, String description,
                               String details, String securityLevel, LocalDateTime timestamp) {
            this.id = id;
            this.eventType = eventType;
            this.description = description;
            this.details = details;
            this.securityLevel = securityLevel;
            this.timestamp = timestamp;
        }

        // Getters
        public Long getId() { return id; }
        public String getEventType() { return eventType; }
        public String getDescription() { return description; }
        public String getDetails() { return details; }
        public String getSecurityLevel() { return securityLevel; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class SecurityScore {
        private final int score;
        private final SecurityRiskLevel riskLevel;
        private final int totalViolations;
        private final int criticalViolations;
        private final int warningViolations;
        private final int totalEvents;

        public SecurityScore(int score, SecurityRiskLevel riskLevel, int totalViolations,
                           int criticalViolations, int warningViolations, int totalEvents) {
            this.score = score;
            this.riskLevel = riskLevel;
            this.totalViolations = totalViolations;
            this.criticalViolations = criticalViolations;
            this.warningViolations = warningViolations;
            this.totalEvents = totalEvents;
        }

        // Getters
        public int getScore() { return score; }
        public SecurityRiskLevel getRiskLevel() { return riskLevel; }
        public int getTotalViolations() { return totalViolations; }
        public int getCriticalViolations() { return criticalViolations; }
        public int getWarningViolations() { return warningViolations; }
        public int getTotalEvents() { return totalEvents; }
    }

    public static class ExamBehaviorData {
        private final double averageTimePerQuestion;
        private final int consecutiveCorrectStreak;
        private final double overallAccuracy;
        private final int windowSwitchCount;
        private final int pasteActionCount;
        private final List<Long> questionTimings;

        public ExamBehaviorData(double averageTimePerQuestion, int consecutiveCorrectStreak,
                              double overallAccuracy, int windowSwitchCount,
                              int pasteActionCount, List<Long> questionTimings) {
            this.averageTimePerQuestion = averageTimePerQuestion;
            this.consecutiveCorrectStreak = consecutiveCorrectStreak;
            this.overallAccuracy = overallAccuracy;
            this.windowSwitchCount = windowSwitchCount;
            this.pasteActionCount = pasteActionCount;
            this.questionTimings = questionTimings;
        }

        public boolean hasIrregularTimingPattern() {
            if (questionTimings.size() < 3) return false;
            
            double variance = calculateVariance(questionTimings);
            double mean = questionTimings.stream().mapToLong(Long::longValue).average().orElse(0);
            
            return variance > mean * 2; // 方差大于均值的2倍认为是异常
        }

        private double calculateVariance(List<Long> values) {
            double mean = values.stream().mapToLong(Long::longValue).average().orElse(0);
            return values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
        }

        // Getters
        public double getAverageTimePerQuestion() { return averageTimePerQuestion; }
        public int getConsecutiveCorrectStreak() { return consecutiveCorrectStreak; }
        public double getOverallAccuracy() { return overallAccuracy; }
        public int getWindowSwitchCount() { return windowSwitchCount; }
        public int getPasteActionCount() { return pasteActionCount; }
        public List<Long> getQuestionTimings() { return questionTimings; }
    }

    public enum SecurityLevel {
        INFO, WARNING, CRITICAL
    }

    public enum SecurityRiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}