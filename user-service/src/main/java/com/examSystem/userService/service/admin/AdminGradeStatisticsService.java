package com.examSystem.userService.service.admin;

import com.examSystem.userService.repository.ExamAttemptRepository;
import com.examSystem.userService.repository.ExamRepository;
import com.examSystem.userService.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员成绩统计服务
 * 
 * 提供考试成绩统计、分析和报告功能
 */
@Service
@Transactional(readOnly = true)
public class AdminGradeStatisticsService {

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private QuestionRepository questionRepository;

    /**
     * 获取考试的详细统计信息
     */
    public ExamDetailedStatistics getExamDetailedStatistics(Long examId) {
        // 基本统计信息
        List<Object[]> basicStats = examAttemptRepository.getExamStatistics(examId);
        
        // 成绩分布
        List<Object[]> gradeDistribution = examAttemptRepository.getGradeDistributionByExamId(examId);
        
        // 平均分、最高分、最低分
        BigDecimal avgScore = examAttemptRepository.getAverageScoreByExamId(examId);
        BigDecimal maxScore = examAttemptRepository.getMaxScoreByExamId(examId);
        BigDecimal minScore = examAttemptRepository.getMinScoreByExamId(examId);
        
        // 及格率
        BigDecimal passingScore = getExamPassingScore(examId);
        BigDecimal passingRate = examAttemptRepository.getPassingRateByExamId(examId, passingScore);
        
        // 尝试次数和完成次数
        long totalAttempts = examAttemptRepository.countByExamId(examId);
        long completedAttempts = examAttemptRepository.countCompletedByExamId(examId);
        
        return new ExamDetailedStatistics(
            examId, totalAttempts, completedAttempts, avgScore, maxScore, minScore,
            passingRate, gradeDistribution, basicStats
        );
    }

    /**
     * 获取用户的统计信息
     */
    public UserStatistics getUserStatistics(Long userId) {
        List<Object[]> userStats = examAttemptRepository.getUserStatistics(userId);
        
        long totalAttempts = examAttemptRepository.countByUserId(userId);
        long completedAttempts = examAttemptRepository.countCompletedByUserId(userId);
        
        BigDecimal passingScore = BigDecimal.valueOf(60); // 默认及格分数
        long passedExams = examAttemptRepository.countPassedExamsByUser(userId, passingScore);
        
        return new UserStatistics(
            userId, totalAttempts, completedAttempts, passedExams, userStats
        );
    }

    /**
     * 获取考试成绩分布
     */
    public List<Object[]> getExamGradeDistribution(Long examId) {
        return examAttemptRepository.getGradeDistributionByExamId(examId);
    }

    /**
     * 获取考试的及格率
     */
    public BigDecimal getExamPassingRate(Long examId) {
        BigDecimal passingScore = getExamPassingScore(examId);
        return examAttemptRepository.getPassingRateByExamId(examId, passingScore);
    }

    /**
     * 获取最高分记录
     */
    public Page<Object[]> getTopScoresByExam(Long examId, Pageable pageable) {
        return examAttemptRepository.findTopScoresByExam(examId, pageable);
    }

    /**
     * 获取需要评分的考试记录
     */
    public Page<Object[]> getAttemptsNeedingGrading(Pageable pageable) {
        return examAttemptRepository.findAttemptsNeedingGrading(pageable);
    }

    /**
     * 获取可疑的考试记录
     */
    public Page<Object[]> getSuspiciousAttempts(Integer violationThreshold, Pageable pageable) {
        return examAttemptRepository.findSuspiciousAttempts(violationThreshold, pageable);
    }

    /**
     * 获取监考评分低的记录
     */
    public Page<Object[]> getLowProctoringScoreAttempts(BigDecimal threshold, Pageable pageable) {
        return examAttemptRepository.findLowProctoringScoreAttempts(threshold, pageable);
    }

    /**
     * 获取指定时间段的考试参与统计
     */
    public List<Object[]> getAttemptCountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return examAttemptRepository.getAttemptCountByDateRange(startDate, endDate);
    }

    /**
     * 获取热门考试排行
     */
    public Page<Object[]> getPopularExamsRanking(Pageable pageable) {
        return examAttemptRepository.getPopularExamsRanking(pageable);
    }

    /**
     * 获取考试参与统计
     */
    public Page<Object[]> getExamParticipationStatistics(Pageable pageable) {
        return examRepository.getExamParticipationStatistics(pageable);
    }

    /**
     * 获取综合统计报告
     */
    public ComprehensiveStatisticsReport getComprehensiveReport() {
        // 考试统计
        List<Object[]> examStats = examRepository.getExamStatistics();
        List<Object[]> examsByType = examRepository.countExamsByType();
        List<Object[]> examsByStatus = examRepository.countExamsByStatus();
        
        // 题目统计
        List<Object[]> questionStats = questionRepository.getQuestionStatistics();
        List<Object[]> questionsByType = questionRepository.countQuestionsByType();
        List<Object[]> questionsByDifficulty = questionRepository.countQuestionsByDifficulty();
        
        return new ComprehensiveStatisticsReport(
            examStats, examsByType, examsByStatus,
            questionStats, questionsByType, questionsByDifficulty
        );
    }

    /**
     * 获取实时统计仪表板数据
     */
    public Map<String, Object> getRealTimeDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // 今日统计
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        
        List<Object[]> todayAttempts = examAttemptRepository.getAttemptCountByDateRange(todayStart, todayEnd);
        
        // 进行中的考试
        Page<Object[]> activeAttempts = examAttemptRepository.findActiveAttempts(
            org.springframework.data.domain.PageRequest.of(0, 100));
        
        // 需要评分的考试
        Page<Object[]> needingGrading = examAttemptRepository.findAttemptsNeedingGrading(
            org.springframework.data.domain.PageRequest.of(0, 50));
        
        // 热门考试
        Page<Object[]> popularExams = examAttemptRepository.getPopularExamsRanking(
            org.springframework.data.domain.PageRequest.of(0, 10));
        
        dashboard.put("todayAttempts", todayAttempts);
        dashboard.put("activeAttempts", activeAttempts.getContent());
        dashboard.put("needingGrading", needingGrading.getContent());
        dashboard.put("popularExams", popularExams.getContent());
        dashboard.put("summary", Map.of(
            "todayAttemptsCount", todayAttempts.size(),
            "activeAttemptsCount", activeAttempts.getTotalElements(),
            "needingGradingCount", needingGrading.getTotalElements(),
            "popularExamsCount", popularExams.getTotalElements()
        ));
        
        return dashboard;
    }

    /**
     * 生成考试分析报告
     */
    public ExamAnalysisReport generateExamAnalysisReport(Long examId) {
        ExamDetailedStatistics statistics = getExamDetailedStatistics(examId);
        
        // 难度分析
        String difficultyAnalysis = analyzeDifficulty(statistics);
        
        // 参与度分析
        String participationAnalysis = analyzeParticipation(statistics);
        
        // 成绩分析
        String gradeAnalysis = analyzeGrades(statistics);
        
        // 建议
        List<String> recommendations = generateRecommendations(statistics);
        
        return new ExamAnalysisReport(
            examId, statistics, difficultyAnalysis, 
            participationAnalysis, gradeAnalysis, recommendations
        );
    }

    /**
     * 导出统计数据
     */
    public Map<String, Object> exportStatisticsData(Long examId, String format) {
        ExamDetailedStatistics statistics = getExamDetailedStatistics(examId);
        
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("examId", examId);
        exportData.put("statistics", statistics);
        exportData.put("format", format);
        exportData.put("exportTime", LocalDateTime.now());
        
        // 根据格式生成不同的导出数据
        switch (format.toLowerCase()) {
            case "excel":
                exportData.put("data", generateExcelData(statistics));
                break;
            case "pdf":
                exportData.put("data", generatePdfData(statistics));
                break;
            case "csv":
                exportData.put("data", generateCsvData(statistics));
                break;
            default:
                exportData.put("data", statistics);
        }
        
        return exportData;
    }

    // 私有辅助方法

    private BigDecimal getExamPassingScore(Long examId) {
        return examRepository.findById(examId)
            .map(exam -> exam.getPassingScore())
            .orElse(BigDecimal.valueOf(60)); // 默认及格分数
    }

    private String analyzeDifficulty(ExamDetailedStatistics statistics) {
        BigDecimal avgScore = statistics.getAvgScore();
        BigDecimal passingRate = statistics.getPassingRate();
        
        if (avgScore != null && passingRate != null) {
            if (avgScore.compareTo(BigDecimal.valueOf(85)) > 0 && passingRate.compareTo(BigDecimal.valueOf(90)) > 0) {
                return "考试难度较低，学生普遍表现良好";
            } else if (avgScore.compareTo(BigDecimal.valueOf(60)) < 0 || passingRate.compareTo(BigDecimal.valueOf(50)) < 0) {
                return "考试难度较高，建议调整题目难度或增加复习时间";
            } else {
                return "考试难度适中，符合教学目标";
            }
        }
        return "数据不足，无法分析难度";
    }

    private String analyzeParticipation(ExamDetailedStatistics statistics) {
        long totalAttempts = statistics.getTotalAttempts();
        long completedAttempts = statistics.getCompletedAttempts();
        
        if (totalAttempts > 0) {
            double completionRate = (double) completedAttempts / totalAttempts * 100;
            if (completionRate > 90) {
                return "参与度很高，完成率达到" + String.format("%.1f%%", completionRate);
            } else if (completionRate > 70) {
                return "参与度良好，完成率为" + String.format("%.1f%%", completionRate);
            } else {
                return "参与度较低，需要关注学生完成情况，完成率仅为" + String.format("%.1f%%", completionRate);
            }
        }
        return "暂无参与数据";
    }

    private String analyzeGrades(ExamDetailedStatistics statistics) {
        List<Object[]> gradeDistribution = statistics.getGradeDistribution();
        
        if (gradeDistribution != null && !gradeDistribution.isEmpty()) {
            Map<String, Long> grades = new HashMap<>();
            for (Object[] grade : gradeDistribution) {
                grades.put((String) grade[0], ((Number) grade[1]).longValue());
            }
            
            long totalGraded = grades.values().stream().mapToLong(Long::longValue).sum();
            if (totalGraded > 0) {
                long excellentCount = grades.getOrDefault("A", 0L);
                long goodCount = grades.getOrDefault("B", 0L);
                long failCount = grades.getOrDefault("F", 0L);
                
                double excellentRate = (double) excellentCount / totalGraded * 100;
                double failRate = (double) failCount / totalGraded * 100;
                
                return String.format("优秀率%.1f%%，不及格率%.1f%%", excellentRate, failRate);
            }
        }
        return "暂无成绩分布数据";
    }

    private List<String> generateRecommendations(ExamDetailedStatistics statistics) {
        List<String> recommendations = new java.util.ArrayList<>();
        
        BigDecimal avgScore = statistics.getAvgScore();
        BigDecimal passingRate = statistics.getPassingRate();
        
        if (avgScore != null && avgScore.compareTo(BigDecimal.valueOf(50)) < 0) {
            recommendations.add("平均分较低，建议检查教学内容和题目设置");
        }
        
        if (passingRate != null && passingRate.compareTo(BigDecimal.valueOf(60)) < 0) {
            recommendations.add("及格率偏低，建议增加辅导或调整评分标准");
        }
        
        long totalAttempts = statistics.getTotalAttempts();
        long completedAttempts = statistics.getCompletedAttempts();
        if (totalAttempts > 0 && (double) completedAttempts / totalAttempts < 0.8) {
            recommendations.add("完成率较低，建议检查考试时间设置和技术问题");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("考试整体表现良好，继续保持当前教学策略");
        }
        
        return recommendations;
    }

    private Object generateExcelData(ExamDetailedStatistics statistics) {
        // 这里应该生成Excel格式的数据
        return Map.of("type", "excel", "message", "Excel导出功能待实现");
    }

    private Object generatePdfData(ExamDetailedStatistics statistics) {
        // 这里应该生成PDF格式的数据
        return Map.of("type", "pdf", "message", "PDF导出功能待实现");
    }

    private Object generateCsvData(ExamDetailedStatistics statistics) {
        // 这里应该生成CSV格式的数据
        return Map.of("type", "csv", "message", "CSV导出功能待实现");
    }

    // 内部类定义

    public static class ExamDetailedStatistics {
        private final Long examId;
        private final long totalAttempts;
        private final long completedAttempts;
        private final BigDecimal avgScore;
        private final BigDecimal maxScore;
        private final BigDecimal minScore;
        private final BigDecimal passingRate;
        private final List<Object[]> gradeDistribution;
        private final List<Object[]> basicStats;

        public ExamDetailedStatistics(Long examId, long totalAttempts, long completedAttempts,
                                    BigDecimal avgScore, BigDecimal maxScore, BigDecimal minScore,
                                    BigDecimal passingRate, List<Object[]> gradeDistribution,
                                    List<Object[]> basicStats) {
            this.examId = examId;
            this.totalAttempts = totalAttempts;
            this.completedAttempts = completedAttempts;
            this.avgScore = avgScore;
            this.maxScore = maxScore;
            this.minScore = minScore;
            this.passingRate = passingRate;
            this.gradeDistribution = gradeDistribution;
            this.basicStats = basicStats;
        }

        // Getters
        public Long getExamId() { return examId; }
        public long getTotalAttempts() { return totalAttempts; }
        public long getCompletedAttempts() { return completedAttempts; }
        public BigDecimal getAvgScore() { return avgScore; }
        public BigDecimal getMaxScore() { return maxScore; }
        public BigDecimal getMinScore() { return minScore; }
        public BigDecimal getPassingRate() { return passingRate; }
        public List<Object[]> getGradeDistribution() { return gradeDistribution; }
        public List<Object[]> getBasicStats() { return basicStats; }
    }

    public static class UserStatistics {
        private final Long userId;
        private final long totalAttempts;
        private final long completedAttempts;
        private final long passedExams;
        private final List<Object[]> detailStats;

        public UserStatistics(Long userId, long totalAttempts, long completedAttempts,
                            long passedExams, List<Object[]> detailStats) {
            this.userId = userId;
            this.totalAttempts = totalAttempts;
            this.completedAttempts = completedAttempts;
            this.passedExams = passedExams;
            this.detailStats = detailStats;
        }

        // Getters
        public Long getUserId() { return userId; }
        public long getTotalAttempts() { return totalAttempts; }
        public long getCompletedAttempts() { return completedAttempts; }
        public long getPassedExams() { return passedExams; }
        public List<Object[]> getDetailStats() { return detailStats; }
    }

    public static class ComprehensiveStatisticsReport {
        private final List<Object[]> examStats;
        private final List<Object[]> examsByType;
        private final List<Object[]> examsByStatus;
        private final List<Object[]> questionStats;
        private final List<Object[]> questionsByType;
        private final List<Object[]> questionsByDifficulty;

        public ComprehensiveStatisticsReport(List<Object[]> examStats, List<Object[]> examsByType,
                                           List<Object[]> examsByStatus, List<Object[]> questionStats,
                                           List<Object[]> questionsByType, List<Object[]> questionsByDifficulty) {
            this.examStats = examStats;
            this.examsByType = examsByType;
            this.examsByStatus = examsByStatus;
            this.questionStats = questionStats;
            this.questionsByType = questionsByType;
            this.questionsByDifficulty = questionsByDifficulty;
        }

        // Getters
        public List<Object[]> getExamStats() { return examStats; }
        public List<Object[]> getExamsByType() { return examsByType; }
        public List<Object[]> getExamsByStatus() { return examsByStatus; }
        public List<Object[]> getQuestionStats() { return questionStats; }
        public List<Object[]> getQuestionsByType() { return questionsByType; }
        public List<Object[]> getQuestionsByDifficulty() { return questionsByDifficulty; }
    }

    public static class ExamAnalysisReport {
        private final Long examId;
        private final ExamDetailedStatistics statistics;
        private final String difficultyAnalysis;
        private final String participationAnalysis;
        private final String gradeAnalysis;
        private final List<String> recommendations;

        public ExamAnalysisReport(Long examId, ExamDetailedStatistics statistics,
                                String difficultyAnalysis, String participationAnalysis,
                                String gradeAnalysis, List<String> recommendations) {
            this.examId = examId;
            this.statistics = statistics;
            this.difficultyAnalysis = difficultyAnalysis;
            this.participationAnalysis = participationAnalysis;
            this.gradeAnalysis = gradeAnalysis;
            this.recommendations = recommendations;
        }

        // Getters
        public Long getExamId() { return examId; }
        public ExamDetailedStatistics getStatistics() { return statistics; }
        public String getDifficultyAnalysis() { return difficultyAnalysis; }
        public String getParticipationAnalysis() { return participationAnalysis; }
        public String getGradeAnalysis() { return gradeAnalysis; }
        public List<String> getRecommendations() { return recommendations; }
    }
}