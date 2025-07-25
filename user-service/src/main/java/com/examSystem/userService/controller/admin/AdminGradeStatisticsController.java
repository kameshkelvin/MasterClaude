package com.examSystem.userService.controller.admin;

import com.examSystem.userService.dto.common.ApiResponse;
import com.examSystem.userService.service.admin.AdminGradeStatisticsService;
import com.examSystem.userService.service.admin.AdminGradeStatisticsService.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理员成绩统计控制器
 * 
 * 提供考试成绩统计、分析和报告功能的API接口
 */
@RestController
@RequestMapping("/api/admin/grade-statistics")
@CrossOrigin(origins = "*")
public class AdminGradeStatisticsController {

    @Autowired
    private AdminGradeStatisticsService gradeStatisticsService;

    /**
     * 获取考试详细统计信息
     */
    @GetMapping("/exam/{examId}")
    public ResponseEntity<ApiResponse<ExamDetailedStatistics>> getExamDetailedStatistics(@PathVariable Long examId) {
        try {
            ExamDetailedStatistics statistics = gradeStatisticsService.getExamDetailedStatistics(examId);
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取考试统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<UserStatistics>> getUserStatistics(@PathVariable Long userId) {
        try {
            UserStatistics statistics = gradeStatisticsService.getUserStatistics(userId);
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取用户统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取考试成绩分布
     */
    @GetMapping("/exam/{examId}/grade-distribution")
    public ResponseEntity<ApiResponse<List<Object[]>>> getExamGradeDistribution(@PathVariable Long examId) {
        try {
            List<Object[]> gradeDistribution = gradeStatisticsService.getExamGradeDistribution(examId);
            return ResponseEntity.ok(ApiResponse.success(gradeDistribution));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取成绩分布失败: " + e.getMessage()));
        }
    }

    /**
     * 获取考试及格率
     */
    @GetMapping("/exam/{examId}/passing-rate")
    public ResponseEntity<ApiResponse<BigDecimal>> getExamPassingRate(@PathVariable Long examId) {
        try {
            BigDecimal passingRate = gradeStatisticsService.getExamPassingRate(examId);
            return ResponseEntity.ok(ApiResponse.success(passingRate));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取及格率失败: " + e.getMessage()));
        }
    }

    /**
     * 获取考试最高分记录
     */
    @GetMapping("/exam/{examId}/top-scores")
    public ResponseEntity<ApiResponse<List<Object[]>>> getTopScoresByExam(
            @PathVariable Long examId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Object[]> topScores = gradeStatisticsService.getTopScoresByExam(examId, pageable);
            
            Map<String, Object> pagination = Map.of(
                "currentPage", topScores.getNumber(),
                "totalPages", topScores.getTotalPages(),
                "totalElements", topScores.getTotalElements(),
                "pageSize", topScores.getSize(),
                "hasNext", topScores.hasNext(),
                "hasPrevious", topScores.hasPrevious()
            );
            
            return ResponseEntity.ok(ApiResponse.<List<Object[]>>success(topScores.getContent()).withPagination(pagination));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取最高分记录失败: " + e.getMessage()));
        }
    }

    /**
     * 获取需要评分的考试记录
     */
    @GetMapping("/attempts/needing-grading")
    public ResponseEntity<ApiResponse<List<Object[]>>> getAttemptsNeedingGrading(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Object[]> attempts = gradeStatisticsService.getAttemptsNeedingGrading(pageable);
            
            Map<String, Object> pagination = Map.of(
                "currentPage", attempts.getNumber(),
                "totalPages", attempts.getTotalPages(),
                "totalElements", attempts.getTotalElements(),
                "pageSize", attempts.getSize(),
                "hasNext", attempts.hasNext(),
                "hasPrevious", attempts.hasPrevious()
            );
            
            return ResponseEntity.ok(ApiResponse.<List<Object[]>>success(attempts.getContent()).withPagination(pagination));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取待评分记录失败: " + e.getMessage()));
        }
    }

    /**
     * 获取可疑的考试记录
     */
    @GetMapping("/attempts/suspicious")
    public ResponseEntity<ApiResponse<List<Object[]>>> getSuspiciousAttempts(
            @RequestParam(defaultValue = "3") Integer violationThreshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Object[]> attempts = gradeStatisticsService.getSuspiciousAttempts(violationThreshold, pageable);
            
            Map<String, Object> pagination = Map.of(
                "currentPage", attempts.getNumber(),
                "totalPages", attempts.getTotalPages(),
                "totalElements", attempts.getTotalElements(),
                "pageSize", attempts.getSize(),
                "hasNext", attempts.hasNext(),
                "hasPrevious", attempts.hasPrevious()
            );
            
            return ResponseEntity.ok(ApiResponse.<List<Object[]>>success(attempts.getContent()).withPagination(pagination));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取可疑记录失败: " + e.getMessage()));
        }
    }

    /**
     * 获取监考评分低的记录
     */
    @GetMapping("/attempts/low-proctoring-score")
    public ResponseEntity<ApiResponse<List<Object[]>>> getLowProctoringScoreAttempts(
            @RequestParam(defaultValue = "0.5") BigDecimal threshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Object[]> attempts = gradeStatisticsService.getLowProctoringScoreAttempts(threshold, pageable);
            
            Map<String, Object> pagination = Map.of(
                "currentPage", attempts.getNumber(),
                "totalPages", attempts.getTotalPages(),
                "totalElements", attempts.getTotalElements(),
                "pageSize", attempts.getSize(),
                "hasNext", attempts.hasNext(),
                "hasPrevious", attempts.hasPrevious()
            );
            
            return ResponseEntity.ok(ApiResponse.<List<Object[]>>success(attempts.getContent()).withPagination(pagination));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取低监考评分记录失败: " + e.getMessage()));
        }
    }

    /**
     * 获取指定时间段的考试参与统计
     */
    @GetMapping("/attempts/date-range")
    public ResponseEntity<ApiResponse<List<Object[]>>> getAttemptCountByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        try {
            List<Object[]> attemptStats = gradeStatisticsService.getAttemptCountByDateRange(startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(attemptStats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取时间段统计失败: " + e.getMessage()));
        }
    }

    /**
     * 获取热门考试排行
     */
    @GetMapping("/exams/popular-ranking")
    public ResponseEntity<ApiResponse<List<Object[]>>> getPopularExamsRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Object[]> popularExams = gradeStatisticsService.getPopularExamsRanking(pageable);
            
            Map<String, Object> pagination = Map.of(
                "currentPage", popularExams.getNumber(),
                "totalPages", popularExams.getTotalPages(),
                "totalElements", popularExams.getTotalElements(),
                "pageSize", popularExams.getSize(),
                "hasNext", popularExams.hasNext(),
                "hasPrevious", popularExams.hasPrevious()
            );
            
            return ResponseEntity.ok(ApiResponse.<List<Object[]>>success(popularExams.getContent()).withPagination(pagination));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取热门考试排行失败: " + e.getMessage()));
        }
    }

    /**
     * 获取考试参与统计
     */
    @GetMapping("/exams/participation")
    public ResponseEntity<ApiResponse<List<Object[]>>> getExamParticipationStatistics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Object[]> participationStats = gradeStatisticsService.getExamParticipationStatistics(pageable);
            
            Map<String, Object> pagination = Map.of(
                "currentPage", participationStats.getNumber(),
                "totalPages", participationStats.getTotalPages(),
                "totalElements", participationStats.getTotalElements(),
                "pageSize", participationStats.getSize(),
                "hasNext", participationStats.hasNext(),
                "hasPrevious", participationStats.hasPrevious()
            );
            
            return ResponseEntity.ok(ApiResponse.<List<Object[]>>success(participationStats.getContent()).withPagination(pagination));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取参与统计失败: " + e.getMessage()));
        }
    }

    /**
     * 获取综合统计报告
     */
    @GetMapping("/comprehensive-report")
    public ResponseEntity<ApiResponse<ComprehensiveStatisticsReport>> getComprehensiveReport() {
        try {
            ComprehensiveStatisticsReport report = gradeStatisticsService.getComprehensiveReport();
            return ResponseEntity.ok(ApiResponse.success(report));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取综合统计报告失败: " + e.getMessage()));
        }
    }

    /**
     * 获取实时统计仪表板数据
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRealTimeDashboard() {
        try {
            Map<String, Object> dashboard = gradeStatisticsService.getRealTimeDashboard();
            return ResponseEntity.ok(ApiResponse.success(dashboard));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取仪表板数据失败: " + e.getMessage()));
        }
    }

    /**
     * 生成考试分析报告
     */
    @GetMapping("/exam/{examId}/analysis-report")
    public ResponseEntity<ApiResponse<ExamAnalysisReport>> generateExamAnalysisReport(@PathVariable Long examId) {
        try {
            ExamAnalysisReport report = gradeStatisticsService.generateExamAnalysisReport(examId);
            return ResponseEntity.ok(ApiResponse.success(report));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("生成考试分析报告失败: " + e.getMessage()));
        }
    }

    /**
     * 导出统计数据
     */
    @PostMapping("/export/{examId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportStatisticsData(
            @PathVariable Long examId,
            @RequestBody Map<String, String> exportRequest) {
        
        try {
            String format = exportRequest.getOrDefault("format", "excel");
            Map<String, Object> exportData = gradeStatisticsService.exportStatisticsData(examId, format);
            return ResponseEntity.ok(ApiResponse.success("数据导出成功", exportData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("数据导出失败: " + e.getMessage()));
        }
    }

    /**
     * 获取多个考试的对比统计
     */
    @PostMapping("/compare")
    public ResponseEntity<ApiResponse<Map<String, Object>>> compareExamStatistics(
            @RequestBody Map<String, Object> compareRequest) {
        
        try {
            @SuppressWarnings("unchecked")
            List<Long> examIds = (List<Long>) compareRequest.get("examIds");
            
            Map<String, Object> comparison = new java.util.HashMap<>();
            
            for (Long examId : examIds) {
                ExamDetailedStatistics statistics = gradeStatisticsService.getExamDetailedStatistics(examId);
                comparison.put("exam_" + examId, statistics);
            }
            
            comparison.put("compareTime", LocalDateTime.now());
            comparison.put("examCount", examIds.size());
            
            return ResponseEntity.ok(ApiResponse.success("考试对比统计", comparison));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("对比统计失败: " + e.getMessage()));
        }
    }

    /**
     * 获取统计数据趋势
     */
    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatisticsTrends(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) Long examId) {
        
        try {
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(days);
            
            List<Object[]> attemptTrends = gradeStatisticsService.getAttemptCountByDateRange(startDate, endDate);
            
            Map<String, Object> trends = Map.of(
                "period", days + " 天",
                "startDate", startDate,
                "endDate", endDate,
                "attemptTrends", attemptTrends,
                "examId", examId != null ? examId : "全部考试"
            );
            
            return ResponseEntity.ok(ApiResponse.success(trends));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取趋势数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取成绩预警信息
     */
    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGradeAlerts() {
        try {
            // 获取需要关注的数据
            Page<Object[]> needingGrading = gradeStatisticsService.getAttemptsNeedingGrading(PageRequest.of(0, 100));
            Page<Object[]> suspicious = gradeStatisticsService.getSuspiciousAttempts(3, PageRequest.of(0, 50));
            Page<Object[]> lowProctoring = gradeStatisticsService.getLowProctoringScoreAttempts(
                BigDecimal.valueOf(0.5), PageRequest.of(0, 50));
            
            Map<String, Object> alerts = Map.of(
                "needingGrading", Map.of(
                    "count", needingGrading.getTotalElements(),
                    "data", needingGrading.getContent()
                ),
                "suspicious", Map.of(
                    "count", suspicious.getTotalElements(),
                    "data", suspicious.getContent()
                ),
                "lowProctoring", Map.of(
                    "count", lowProctoring.getTotalElements(),
                    "data", lowProctoring.getContent()
                ),
                "alertTime", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(ApiResponse.success(alerts));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取预警信息失败: " + e.getMessage()));
        }
    }
}