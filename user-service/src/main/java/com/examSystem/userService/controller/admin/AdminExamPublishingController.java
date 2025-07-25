package com.examSystem.userService.controller.admin;

import com.examSystem.userService.dto.common.ApiResponse;
import com.examSystem.userService.entity.Exam;
import com.examSystem.userService.service.admin.AdminExamPublishingService;
import com.examSystem.userService.service.admin.AdminExamPublishingService.ExamPublishingStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理员考试发布和调度控制器
 * 
 * 提供考试发布、调度管理、状态控制等功能
 */
@RestController
@RequestMapping("/api/admin/exam-publishing")
@CrossOrigin(origins = "*")
public class AdminExamPublishingController {

    @Autowired
    private AdminExamPublishingService examPublishingService;

    /**
     * 发布考试
     */
    @PostMapping("/{examId}/publish")
    public ResponseEntity<ApiResponse<Void>> publishExam(
            @PathVariable Long examId,
            @RequestBody(required = false) Map<String, Object> publishData) {
        
        try {
            LocalDateTime publishTime = null;
            if (publishData != null && publishData.containsKey("publishTime")) {
                publishTime = LocalDateTime.parse(publishData.get("publishTime").toString());
            }
            
            examPublishingService.publishExam(examId, publishTime);
            return ResponseEntity.ok(ApiResponse.success("考试发布成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("考试发布失败: " + e.getMessage()));
        }
    }

    /**
     * 批量发布考试
     */
    @PostMapping("/batch-publish")
    public ResponseEntity<ApiResponse<Void>> batchPublishExams(@RequestBody Map<String, Object> publishData) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> examIds = (List<Long>) publishData.get("examIds");
            
            LocalDateTime publishTime = null;
            if (publishData.containsKey("publishTime")) {
                publishTime = LocalDateTime.parse(publishData.get("publishTime").toString());
            }
            
            examPublishingService.batchPublishExams(examIds, publishTime);
            return ResponseEntity.ok(ApiResponse.success("批量发布成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("批量发布失败: " + e.getMessage()));
        }
    }

    /**
     * 调度考试（设置可用时间）
     */
    @PostMapping("/{examId}/schedule")
    public ResponseEntity<ApiResponse<Void>> scheduleExam(
            @PathVariable Long examId,
            @RequestBody Map<String, Object> scheduleData) {
        
        try {
            LocalDateTime availableFrom = scheduleData.get("availableFrom") != null ?
                LocalDateTime.parse(scheduleData.get("availableFrom").toString()) : null;
            LocalDateTime availableUntil = scheduleData.get("availableUntil") != null ?
                LocalDateTime.parse(scheduleData.get("availableUntil").toString()) : null;
            
            examPublishingService.scheduleExam(examId, availableFrom, availableUntil);
            return ResponseEntity.ok(ApiResponse.success("考试调度设置成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("考试调度设置失败: " + e.getMessage()));
        }
    }

    /**
     * 立即发布并激活考试
     */
    @PostMapping("/{examId}/publish-activate")
    public ResponseEntity<ApiResponse<Void>> publishAndActivateExam(@PathVariable Long examId) {
        try {
            examPublishingService.publishAndActivateExam(examId);
            return ResponseEntity.ok(ApiResponse.success("考试发布并激活成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("考试发布并激活失败: " + e.getMessage()));
        }
    }

    /**
     * 激活考试
     */
    @PostMapping("/{examId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateExam(@PathVariable Long examId) {
        try {
            examPublishingService.activateExam(examId);
            return ResponseEntity.ok(ApiResponse.success("考试激活成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("考试激活失败: " + e.getMessage()));
        }
    }

    /**
     * 暂停考试
     */
    @PostMapping("/{examId}/pause")
    public ResponseEntity<ApiResponse<Void>> pauseExam(
            @PathVariable Long examId,
            @RequestBody Map<String, String> pauseData) {
        
        try {
            String reason = pauseData.getOrDefault("reason", "管理员手动暂停");
            examPublishingService.pauseExam(examId, reason);
            return ResponseEntity.ok(ApiResponse.success("考试暂停成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("考试暂停失败: " + e.getMessage()));
        }
    }

    /**
     * 恢复暂停的考试
     */
    @PostMapping("/{examId}/resume")
    public ResponseEntity<ApiResponse<Void>> resumeExam(@PathVariable Long examId) {
        try {
            examPublishingService.resumeExam(examId);
            return ResponseEntity.ok(ApiResponse.success("考试恢复成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("考试恢复失败: " + e.getMessage()));
        }
    }

    /**
     * 结束考试
     */
    @PostMapping("/{examId}/end")
    public ResponseEntity<ApiResponse<Void>> endExam(@PathVariable Long examId) {
        try {
            examPublishingService.endExam(examId);
            return ResponseEntity.ok(ApiResponse.success("考试结束成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("考试结束失败: " + e.getMessage()));
        }
    }

    /**
     * 延长考试时间
     */
    @PostMapping("/{examId}/extend")
    public ResponseEntity<ApiResponse<Void>> extendExamTime(
            @PathVariable Long examId,
            @RequestBody Map<String, Object> extendData) {
        
        try {
            LocalDateTime newEndTime = LocalDateTime.parse(extendData.get("newEndTime").toString());
            examPublishingService.extendExamTime(examId, newEndTime);
            return ResponseEntity.ok(ApiResponse.success("考试时间延长成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("考试时间延长失败: " + e.getMessage()));
        }
    }

    /**
     * 撤销发布
     */
    @PostMapping("/{examId}/unpublish")
    public ResponseEntity<ApiResponse<Void>> unpublishExam(
            @PathVariable Long examId,
            @RequestBody Map<String, String> unpublishData) {
        
        try {
            String reason = unpublishData.getOrDefault("reason", "管理员撤销发布");
            examPublishingService.unpublishExam(examId, reason);
            return ResponseEntity.ok(ApiResponse.success("考试撤销发布成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("考试撤销发布失败: " + e.getMessage()));
        }
    }

    /**
     * 获取即将开始的考试
     */
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<Exam>>> getUpcomingExams(
            @RequestParam(defaultValue = "24") int hours) {
        
        try {
            List<Exam> upcomingExams = examPublishingService.getUpcomingExams(hours);
            return ResponseEntity.ok(ApiResponse.success(upcomingExams));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取即将开始的考试失败: " + e.getMessage()));
        }
    }

    /**
     * 获取已过期的考试
     */
    @GetMapping("/expired")
    public ResponseEntity<ApiResponse<List<Exam>>> getExpiredExams() {
        try {
            List<Exam> expiredExams = examPublishingService.getExpiredExams();
            return ResponseEntity.ok(ApiResponse.success(expiredExams));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取已过期的考试失败: " + e.getMessage()));
        }
    }

    /**
     * 获取需要自动结束的考试
     */
    @GetMapping("/auto-end")
    public ResponseEntity<ApiResponse<List<Exam>>> getExamsToAutoEnd() {
        try {
            List<Exam> examsToEnd = examPublishingService.getExamsToAutoEnd();
            return ResponseEntity.ok(ApiResponse.success(examsToEnd));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取需要自动结束的考试失败: " + e.getMessage()));
        }
    }

    /**
     * 获取可以开始的考试
     */
    @GetMapping("/auto-start")
    public ResponseEntity<ApiResponse<List<Exam>>> getExamsToStart() {
        try {
            List<Exam> examsToStart = examPublishingService.getExamsToStart();
            return ResponseEntity.ok(ApiResponse.success(examsToStart));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取可以开始的考试失败: " + e.getMessage()));
        }
    }

    /**
     * 手动触发自动开始考试任务
     */
    @PostMapping("/trigger-auto-start")
    public ResponseEntity<ApiResponse<Void>> triggerAutoStartExams() {
        try {
            examPublishingService.autoStartExams();
            return ResponseEntity.ok(ApiResponse.success("自动开始考试任务执行成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("自动开始考试任务执行失败: " + e.getMessage()));
        }
    }

    /**
     * 手动触发自动结束考试任务
     */
    @PostMapping("/trigger-auto-end")
    public ResponseEntity<ApiResponse<Void>> triggerAutoEndExams() {
        try {
            examPublishingService.autoEndExams();
            return ResponseEntity.ok(ApiResponse.success("自动结束考试任务执行成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("自动结束考试任务执行失败: " + e.getMessage()));
        }
    }

    /**
     * 手动触发处理过期考试任务
     */
    @PostMapping("/trigger-process-expired")
    public ResponseEntity<ApiResponse<Void>> triggerProcessExpiredExams() {
        try {
            examPublishingService.processExpiredExams();
            return ResponseEntity.ok(ApiResponse.success("处理过期考试任务执行成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("处理过期考试任务执行失败: " + e.getMessage()));
        }
    }

    /**
     * 获取考试发布统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<ExamPublishingStatistics>> getPublishingStatistics() {
        try {
            ExamPublishingStatistics statistics = examPublishingService.getPublishingStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取发布统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取考试状态概览
     */
    @GetMapping("/status-overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatusOverview() {
        try {
            ExamPublishingStatistics stats = examPublishingService.getPublishingStatistics();
            List<Exam> upcomingExams = examPublishingService.getUpcomingExams(24);
            List<Exam> expiredExams = examPublishingService.getExpiredExams();
            List<Exam> examsToStart = examPublishingService.getExamsToStart();
            List<Exam> examsToEnd = examPublishingService.getExamsToAutoEnd();
            
            Map<String, Object> overview = Map.of(
                "statistics", stats,
                "upcoming", upcomingExams,
                "expired", expiredExams,
                "toStart", examsToStart,
                "toEnd", examsToEnd,
                "summary", Map.of(
                    "totalExams", stats.getTotalCount(),
                    "activeExams", stats.getActiveCount(),
                    "publishedExams", stats.getPublishedCount(),
                    "upcomingCount", upcomingExams.size(),
                    "expiredCount", expiredExams.size(),
                    "pendingStartCount", examsToStart.size(),
                    "pendingEndCount", examsToEnd.size()
                )
            );
            
            return ResponseEntity.ok(ApiResponse.success(overview));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取状态概览失败: " + e.getMessage()));
        }
    }

    /**
     * 批量操作考试状态
     */
    @PostMapping("/batch-operation")
    public ResponseEntity<ApiResponse<Void>> batchOperation(@RequestBody Map<String, Object> operationData) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> examIds = (List<Long>) operationData.get("examIds");
            String operation = operationData.get("operation").toString();
            
            switch (operation.toLowerCase()) {
                case "publish":
                    LocalDateTime publishTime = operationData.get("publishTime") != null ?
                        LocalDateTime.parse(operationData.get("publishTime").toString()) : null;
                    examPublishingService.batchPublishExams(examIds, publishTime);
                    break;
                    
                case "activate":
                    for (Long examId : examIds) {
                        try {
                            examPublishingService.activateExam(examId);
                        } catch (Exception e) {
                            System.err.println("激活考试失败 ID: " + examId + ", 错误: " + e.getMessage());
                        }
                    }
                    break;
                    
                case "pause":
                    String pauseReason = operationData.getOrDefault("reason", "批量暂停").toString();
                    for (Long examId : examIds) {
                        try {
                            examPublishingService.pauseExam(examId, pauseReason);
                        } catch (Exception e) {
                            System.err.println("暂停考试失败 ID: " + examId + ", 错误: " + e.getMessage());
                        }
                    }
                    break;
                    
                case "end":
                    for (Long examId : examIds) {
                        try {
                            examPublishingService.endExam(examId);
                        } catch (Exception e) {
                            System.err.println("结束考试失败 ID: " + examId + ", 错误: " + e.getMessage());
                        }
                    }
                    break;
                    
                default:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("不支持的操作类型: " + operation));
            }
            
            return ResponseEntity.ok(ApiResponse.success("批量操作执行成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("批量操作失败: " + e.getMessage()));
        }
    }

    /**
     * 获取考试时间轴
     */
    @GetMapping("/{examId}/timeline")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExamTimeline(@PathVariable Long examId) {
        try {
            // 这里可以扩展获取考试的完整时间轴信息
            // 包括创建、发布、开始、结束等关键时间点
            Map<String, Object> timeline = Map.of(
                "message", "考试时间轴功能待实现",
                "examId", examId
            );
            
            return ResponseEntity.ok(ApiResponse.success(timeline));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取考试时间轴失败: " + e.getMessage()));
        }
    }
}