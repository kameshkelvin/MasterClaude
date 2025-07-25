package com.examSystem.userService.service.monitoring;

import com.examSystem.userService.entity.ExamAttempt;
import com.examSystem.userService.entity.Exam;
import com.examSystem.userService.repository.ExamAttemptRepository;
import com.examSystem.userService.repository.ExamRepository;
import com.examSystem.userService.service.security.ExamSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 考试实时监控服务
 * 
 * 提供考试过程的实时监控功能：
 * - 学生状态实时跟踪
 * - 考试进度监控
 * - 异常行为预警
 * - 实时统计数据
 * - WebSocket实时通信
 */
@Service
@Transactional
public class ExamMonitoringService {

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamSecurityService securityService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 实时会话状态存储
    private final Map<Long, StudentSession> activeSessions = new ConcurrentHashMap<>();
    
    // 考试室状态缓存
    private final Map<Long, ExamRoomStatus> examRoomCache = new ConcurrentHashMap<>();

    /**
     * 学生进入考试监控
     */
    public void onStudentEnterExam(Long attemptId, Long studentId, Long examId, String clientInfo) {
        StudentSession session = new StudentSession(
            attemptId, studentId, examId, LocalDateTime.now(), clientInfo);
        
        activeSessions.put(attemptId, session);
        
        // 更新考试室状态
        updateExamRoomStatus(examId);
        
        // 通知监控端
        broadcastStudentStatusUpdate(examId, studentId, "ENTERED", session);
        
        // 记录监控日志
        logMonitoringEvent(studentId, attemptId, "STUDENT_ENTERED", 
            "学生进入考试", clientInfo);
    }

    /**
     * 学生退出考试监控
     */
    public void onStudentExitExam(Long attemptId, Long studentId, Long examId, String reason) {
        StudentSession session = activeSessions.remove(attemptId);
        
        if (session != null) {
            session.setExitTime(LocalDateTime.now());
            session.setExitReason(reason);
            
            // 更新考试室状态
            updateExamRoomStatus(examId);
            
            // 通知监控端
            broadcastStudentStatusUpdate(examId, studentId, "EXITED", session);
            
            // 记录监控日志
            logMonitoringEvent(studentId, attemptId, "STUDENT_EXITED", 
                "学生退出考试", "原因: " + reason);
        }
    }

    /**
     * 更新学生活动状态
     */
    public void updateStudentActivity(Long attemptId, StudentActivity activity) {
        StudentSession session = activeSessions.get(attemptId);
        
        if (session != null) {
            session.updateActivity(activity);
            
            // 检测异常行为
            if (activity.isAnomalous()) {
                handleAnomalousActivity(session, activity);
            }
            
            // 实时推送活动状态
            broadcastActivityUpdate(session.getExamId(), session.getStudentId(), activity);
        }
    }

    /**
     * 获取考试室实时状态
     */
    @Transactional(readOnly = true)
    public ExamRoomStatus getExamRoomStatus(Long examId) {
        ExamRoomStatus cached = examRoomCache.get(examId);
        if (cached != null && cached.isRecentlyUpdated()) {
            return cached;
        }
        
        // 重新计算考试室状态
        ExamRoomStatus status = calculateExamRoomStatus(examId);
        examRoomCache.put(examId, status);
        
        return status;
    }

    /**
     * 获取学生的实时状态
     */
    @Transactional(readOnly = true)
    public StudentMonitoringInfo getStudentStatus(Long attemptId) {
        StudentSession session = activeSessions.get(attemptId);
        
        if (session == null) {
            return null;
        }
        
        ExamAttempt attempt = examAttemptRepository.findById(attemptId).orElse(null);
        if (attempt == null) {
            return null;
        }
        
        // 计算进度信息
        ProgressInfo progress = calculateProgress(attemptId);
        
        // 获取安全评分
        ExamSecurityService.SecurityScore securityScore = 
            securityService.calculateSecurityScore(attemptId);
        
        return new StudentMonitoringInfo(
            session.getStudentId(),
            session.getAttemptId(),
            session.getExamId(),
            session.getEnterTime(),
            session.getLastActivityTime(),
            session.getCurrentActivity(),
            progress,
            securityScore,
            session.getAnomalyCount(),
            calculateRemainingTime(attempt)
        );
    }

    /**
     * 获取考试的所有在线学生
     */
    @Transactional(readOnly = true)
    public List<StudentMonitoringInfo> getOnlineStudents(Long examId) {
        return activeSessions.values().stream()
            .filter(session -> session.getExamId().equals(examId))
            .map(session -> getStudentStatus(session.getAttemptId()))
            .filter(info -> info != null)
            .collect(Collectors.toList());
    }

    /**
     * 生成实时监控报告
     */
    @Transactional(readOnly = true)
    public RealTimeReport generateRealTimeReport(Long examId) {
        ExamRoomStatus roomStatus = getExamRoomStatus(examId);
        List<StudentMonitoringInfo> onlineStudents = getOnlineStudents(examId);
        
        // 统计异常行为
        long anomalousStudents = onlineStudents.stream()
            .mapToLong(StudentMonitoringInfo::getAnomalyCount)
            .filter(count -> count > 0)
            .count();
        
        // 统计进度分布
        Map<String, Long> progressDistribution = onlineStudents.stream()
            .collect(Collectors.groupingBy(
                info -> getProgressCategory(info.getProgress().getCompletionPercentage()),
                Collectors.counting()
            ));
        
        return new RealTimeReport(
            examId,
            LocalDateTime.now(),
            roomStatus,
            onlineStudents.size(),
            anomalousStudents,
            progressDistribution,
            calculateAverageProgress(onlineStudents)
        );
    }

    /**
     * 发送实时警告
     */
    public void sendRealTimeAlert(Long examId, AlertType alertType, String message, 
                                 Long studentId, Long attemptId) {
        RealTimeAlert alert = new RealTimeAlert(
            alertType,
            message,
            studentId,
            attemptId,
            examId,
            LocalDateTime.now()
        );
        
        // 通过WebSocket发送警告
        messagingTemplate.convertAndSend("/topic/exam/" + examId + "/alerts", alert);
        
        // 记录警告日志
        logMonitoringEvent(studentId, attemptId, "ALERT_SENT", 
            alertType.toString(), message);
    }

    /**
     * 定时更新考试室状态
     */
    @Scheduled(fixedRate = 30000) // 每30秒更新一次
    public void updateExamRoomStatuses() {
        List<Long> activeExamIds = examRepository.findActiveExamIds();
        
        for (Long examId : activeExamIds) {
            try {
                updateExamRoomStatus(examId);
            } catch (Exception e) {
                System.err.println("更新考试室状态失败: " + examId + ", 错误: " + e.getMessage());
            }
        }
    }

    /**
     * 定时清理过期会话
     */
    @Scheduled(fixedRate = 300000) // 每5分钟清理一次
    public void cleanupExpiredSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        
        activeSessions.entrySet().removeIf(entry -> {
            StudentSession session = entry.getValue();
            return session.getLastActivityTime().isBefore(cutoffTime);
        });
        
        // 清理考试室缓存
        examRoomCache.entrySet().removeIf(entry -> {
            ExamRoomStatus status = entry.getValue();
            return status.getLastUpdated().isBefore(cutoffTime);
        });
    }

    /**
     * 定时发送心跳检测
     */
    @Scheduled(fixedRate = 60000) // 每分钟发送一次
    public void sendHeartbeat() {
        for (StudentSession session : activeSessions.values()) {
            try {
                // 通过WebSocket发送心跳
                messagingTemplate.convertAndSendToUser(
                    session.getStudentId().toString(),
                    "/queue/heartbeat",
                    new HeartbeatMessage(session.getAttemptId(), LocalDateTime.now())
                );
            } catch (Exception e) {
                System.err.println("发送心跳失败: " + session.getAttemptId() + ", 错误: " + e.getMessage());
            }
        }
    }

    // 私有辅助方法

    private void updateExamRoomStatus(Long examId) {
        List<StudentSession> examSessions = activeSessions.values().stream()
            .filter(session -> session.getExamId().equals(examId))
            .collect(Collectors.toList());
        
        int onlineCount = examSessions.size();
        int activeCount = (int) examSessions.stream()
            .filter(session -> session.isActive())
            .count();
        int anomalousCount = (int) examSessions.stream()
            .filter(session -> session.getAnomalyCount() > 0)
            .count();
        
        ExamRoomStatus status = new ExamRoomStatus(
            examId,
            onlineCount,
            activeCount,
            anomalousCount,
            LocalDateTime.now()
        );
        
        examRoomCache.put(examId, status);
        
        // 广播考试室状态更新
        messagingTemplate.convertAndSend("/topic/exam/" + examId + "/room-status", status);
    }

    private void handleAnomalousActivity(StudentSession session, StudentActivity activity) {
        session.incrementAnomalyCount();
        
        // 发送实时警告
        sendRealTimeAlert(
            session.getExamId(),
            AlertType.ANOMALOUS_BEHAVIOR,
            "检测到异常行为: " + activity.getDescription(),
            session.getStudentId(),
            session.getAttemptId()
        );
        
        // 记录安全事件
        ExamSecurityService.ExamBehaviorData behaviorData = 
            createBehaviorDataFromActivity(activity);
        
        securityService.detectAnomalies(
            session.getStudentId(),
            session.getAttemptId(),
            behaviorData
        );
    }

    private ExamRoomStatus calculateExamRoomStatus(Long examId) {
        // 从数据库获取实时数据
        long totalAttempts = examAttemptRepository.countActiveAttemptsByExamId(examId);
        List<StudentSession> sessions = activeSessions.values().stream()
            .filter(session -> session.getExamId().equals(examId))
            .collect(Collectors.toList());
        
        int onlineCount = sessions.size();
        int activeCount = (int) sessions.stream().filter(StudentSession::isActive).count();
        int anomalousCount = (int) sessions.stream()
            .filter(session -> session.getAnomalyCount() > 0).count();
        
        return new ExamRoomStatus(examId, onlineCount, activeCount, anomalousCount, LocalDateTime.now());
    }

    private ProgressInfo calculateProgress(Long attemptId) {
        // 这里应该查询答题进度
        // 暂时返回模拟数据
        return new ProgressInfo(attemptId, 65.0, 13, 20, LocalDateTime.now());
    }

    private long calculateRemainingTime(ExamAttempt attempt) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = attempt.getEndTime();
        
        if (now.isAfter(endTime)) {
            return 0;
        }
        
        return Duration.between(now, endTime).toMinutes();
    }

    private void broadcastStudentStatusUpdate(Long examId, Long studentId, String status, StudentSession session) {
        Map<String, Object> update = Map.of(
            "studentId", studentId,
            "status", status,
            "timestamp", LocalDateTime.now(),
            "sessionInfo", session
        );
        
        messagingTemplate.convertAndSend("/topic/exam/" + examId + "/student-status", update);
    }

    private void broadcastActivityUpdate(Long examId, Long studentId, StudentActivity activity) {
        Map<String, Object> update = Map.of(
            "studentId", studentId,
            "activity", activity,
            "timestamp", LocalDateTime.now()
        );
        
        messagingTemplate.convertAndSend("/topic/exam/" + examId + "/activity", update);
    }

    private void logMonitoringEvent(Long studentId, Long attemptId, String eventType, 
                                   String description, String details) {
        // 这里应该记录到监控日志表
        System.out.println("监控事件: " + eventType + " - " + description + 
            " (学生: " + studentId + ", 尝试: " + attemptId + ")");
    }

    private String getProgressCategory(double percentage) {
        if (percentage < 25) return "刚开始";
        if (percentage < 50) return "进行中";
        if (percentage < 75) return "接近完成";
        return "即将结束";
    }

    private double calculateAverageProgress(List<StudentMonitoringInfo> students) {
        return students.stream()
            .mapToDouble(info -> info.getProgress().getCompletionPercentage())
            .average()
            .orElse(0.0);
    }

    private ExamSecurityService.ExamBehaviorData createBehaviorDataFromActivity(StudentActivity activity) {
        // 从活动数据创建行为分析数据
        return new ExamSecurityService.ExamBehaviorData(
            activity.getTimeSpent() / 1000.0, // 转换为秒
            0, // 连续正确数需要从其他地方获取
            0.0, // 整体准确率需要计算
            activity.getType().equals("WINDOW_SWITCH") ? 1 : 0,
            activity.getType().equals("PASTE") ? 1 : 0,
            new java.util.ArrayList<>() // 题目时间需要单独收集
        );
    }

    // 内部类定义

    public static class StudentSession {
        private final Long attemptId;
        private final Long studentId;
        private final Long examId;
        private final LocalDateTime enterTime;
        private final String clientInfo;
        private LocalDateTime lastActivityTime;
        private LocalDateTime exitTime;
        private String exitReason;
        private StudentActivity currentActivity;
        private int anomalyCount;

        public StudentSession(Long attemptId, Long studentId, Long examId,
                            LocalDateTime enterTime, String clientInfo) {
            this.attemptId = attemptId;
            this.studentId = studentId;
            this.examId = examId;
            this.enterTime = enterTime;
            this.clientInfo = clientInfo;
            this.lastActivityTime = enterTime;
            this.anomalyCount = 0;
        }

        public void updateActivity(StudentActivity activity) {
            this.currentActivity = activity;
            this.lastActivityTime = LocalDateTime.now();
        }

        public boolean isActive() {
            return exitTime == null && 
                   lastActivityTime.isAfter(LocalDateTime.now().minusMinutes(5));
        }

        public void incrementAnomalyCount() {
            this.anomalyCount++;
        }

        // Getters and Setters
        public Long getAttemptId() { return attemptId; }
        public Long getStudentId() { return studentId; }
        public Long getExamId() { return examId; }
        public LocalDateTime getEnterTime() { return enterTime; }
        public String getClientInfo() { return clientInfo; }
        public LocalDateTime getLastActivityTime() { return lastActivityTime; }
        public LocalDateTime getExitTime() { return exitTime; }
        public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }
        public String getExitReason() { return exitReason; }
        public void setExitReason(String exitReason) { this.exitReason = exitReason; }
        public StudentActivity getCurrentActivity() { return currentActivity; }
        public int getAnomalyCount() { return anomalyCount; }
    }

    public static class StudentActivity {
        private final String type;
        private final String description;
        private final long timeSpent;
        private final Map<String, Object> metadata;
        private final LocalDateTime timestamp;

        public StudentActivity(String type, String description, long timeSpent,
                             Map<String, Object> metadata) {
            this.type = type;
            this.description = description;
            this.timeSpent = timeSpent;
            this.metadata = metadata != null ? metadata : new HashMap<>();
            this.timestamp = LocalDateTime.now();
        }

        public boolean isAnomalous() {
            // 定义异常行为的判断逻辑
            switch (type) {
                case "WINDOW_SWITCH":
                case "TAB_SWITCH":
                case "COPY_PASTE":
                    return true;
                case "ANSWER_SUBMIT":
                    return timeSpent < 2000; // 少于2秒提交答案
                default:
                    return false;
            }
        }

        // Getters
        public String getType() { return type; }
        public String getDescription() { return description; }
        public long getTimeSpent() { return timeSpent; }
        public Map<String, Object> getMetadata() { return metadata; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class ExamRoomStatus {
        private final Long examId;
        private final int onlineCount;
        private final int activeCount;
        private final int anomalousCount;
        private final LocalDateTime lastUpdated;

        public ExamRoomStatus(Long examId, int onlineCount, int activeCount,
                            int anomalousCount, LocalDateTime lastUpdated) {
            this.examId = examId;
            this.onlineCount = onlineCount;
            this.activeCount = activeCount;
            this.anomalousCount = anomalousCount;
            this.lastUpdated = lastUpdated;
        }

        public boolean isRecentlyUpdated() {
            return lastUpdated.isAfter(LocalDateTime.now().minusMinutes(1));
        }

        // Getters
        public Long getExamId() { return examId; }
        public int getOnlineCount() { return onlineCount; }
        public int getActiveCount() { return activeCount; }
        public int getAnomalousCount() { return anomalousCount; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }

    public static class StudentMonitoringInfo {
        private final Long studentId;
        private final Long attemptId;
        private final Long examId;
        private final LocalDateTime enterTime;
        private final LocalDateTime lastActivityTime;
        private final StudentActivity currentActivity;
        private final ProgressInfo progress;
        private final ExamSecurityService.SecurityScore securityScore;
        private final int anomalyCount;
        private final long remainingMinutes;

        public StudentMonitoringInfo(Long studentId, Long attemptId, Long examId,
                                   LocalDateTime enterTime, LocalDateTime lastActivityTime,
                                   StudentActivity currentActivity, ProgressInfo progress,
                                   ExamSecurityService.SecurityScore securityScore,
                                   int anomalyCount, long remainingMinutes) {
            this.studentId = studentId;
            this.attemptId = attemptId;
            this.examId = examId;
            this.enterTime = enterTime;
            this.lastActivityTime = lastActivityTime;
            this.currentActivity = currentActivity;
            this.progress = progress;
            this.securityScore = securityScore;
            this.anomalyCount = anomalyCount;
            this.remainingMinutes = remainingMinutes;
        }

        // Getters
        public Long getStudentId() { return studentId; }
        public Long getAttemptId() { return attemptId; }
        public Long getExamId() { return examId; }
        public LocalDateTime getEnterTime() { return enterTime; }
        public LocalDateTime getLastActivityTime() { return lastActivityTime; }
        public StudentActivity getCurrentActivity() { return currentActivity; }
        public ProgressInfo getProgress() { return progress; }
        public ExamSecurityService.SecurityScore getSecurityScore() { return securityScore; }
        public int getAnomalyCount() { return anomalyCount; }
        public long getRemainingMinutes() { return remainingMinutes; }
    }

    public static class ProgressInfo {
        private final Long attemptId;
        private final double completionPercentage;
        private final int answeredQuestions;
        private final int totalQuestions;
        private final LocalDateTime lastAnswerTime;

        public ProgressInfo(Long attemptId, double completionPercentage,
                          int answeredQuestions, int totalQuestions,
                          LocalDateTime lastAnswerTime) {
            this.attemptId = attemptId;
            this.completionPercentage = completionPercentage;
            this.answeredQuestions = answeredQuestions;
            this.totalQuestions = totalQuestions;
            this.lastAnswerTime = lastAnswerTime;
        }

        // Getters
        public Long getAttemptId() { return attemptId; }
        public double getCompletionPercentage() { return completionPercentage; }
        public int getAnsweredQuestions() { return answeredQuestions; }
        public int getTotalQuestions() { return totalQuestions; }
        public LocalDateTime getLastAnswerTime() { return lastAnswerTime; }
    }

    public static class RealTimeReport {
        private final Long examId;
        private final LocalDateTime timestamp;
        private final ExamRoomStatus roomStatus;
        private final int totalOnlineStudents;
        private final long anomalousStudents;
        private final Map<String, Long> progressDistribution;
        private final double averageProgress;

        public RealTimeReport(Long examId, LocalDateTime timestamp, ExamRoomStatus roomStatus,
                            int totalOnlineStudents, long anomalousStudents,
                            Map<String, Long> progressDistribution, double averageProgress) {
            this.examId = examId;
            this.timestamp = timestamp;
            this.roomStatus = roomStatus;
            this.totalOnlineStudents = totalOnlineStudents;
            this.anomalousStudents = anomalousStudents;
            this.progressDistribution = progressDistribution;
            this.averageProgress = averageProgress;
        }

        // Getters
        public Long getExamId() { return examId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public ExamRoomStatus getRoomStatus() { return roomStatus; }
        public int getTotalOnlineStudents() { return totalOnlineStudents; }
        public long getAnomalousStudents() { return anomalousStudents; }
        public Map<String, Long> getProgressDistribution() { return progressDistribution; }
        public double getAverageProgress() { return averageProgress; }
    }

    public static class RealTimeAlert {
        private final AlertType type;
        private final String message;
        private final Long studentId;
        private final Long attemptId;
        private final Long examId;
        private final LocalDateTime timestamp;

        public RealTimeAlert(AlertType type, String message, Long studentId,
                           Long attemptId, Long examId, LocalDateTime timestamp) {
            this.type = type;
            this.message = message;
            this.studentId = studentId;
            this.attemptId = attemptId;
            this.examId = examId;
            this.timestamp = timestamp;
        }

        // Getters
        public AlertType getType() { return type; }
        public String getMessage() { return message; }
        public Long getStudentId() { return studentId; }
        public Long getAttemptId() { return attemptId; }
        public Long getExamId() { return examId; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class HeartbeatMessage {
        private final Long attemptId;
        private final LocalDateTime timestamp;

        public HeartbeatMessage(Long attemptId, LocalDateTime timestamp) {
            this.attemptId = attemptId;
            this.timestamp = timestamp;
        }

        // Getters
        public Long getAttemptId() { return attemptId; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public enum AlertType {
        ANOMALOUS_BEHAVIOR,    // 异常行为
        TIME_WARNING,          // 时间警告
        SECURITY_VIOLATION,    // 安全违规
        SYSTEM_ERROR,          // 系统错误
        CONNECTION_ISSUE       // 连接问题
    }
}