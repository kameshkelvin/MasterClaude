package com.examSystem.userService.controller.admin;

import com.examSystem.userService.dto.common.ApiResponse;
import com.examSystem.userService.entity.Exam;
import com.examSystem.userService.entity.ExamQuestion;
import com.examSystem.userService.service.admin.AdminExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 管理员考试管理控制器
 * 
 * 提供考试的CRUD操作、试卷组卷、发布管理等功能
 */
@RestController
@RequestMapping("/api/admin/exams")
@CrossOrigin(origins = "*")
public class AdminExamController {

    @Autowired
    private AdminExamService adminExamService;

    /**
     * 创建新考试
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Exam>> createExam(@Valid @RequestBody Exam exam) {
        try {
            Exam createdExam = adminExamService.createExam(exam);
            return ResponseEntity.ok(ApiResponse.success("考试创建成功", createdExam));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("考试创建失败: " + e.getMessage()));
        }
    }

    /**
     * 根据ID获取考试详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Exam>> getExam(@PathVariable Long id) {
        try {
            Optional<Exam> exam = adminExamService.getExamById(id);
            if (exam.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(exam.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("考试不存在"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取考试失败: " + e.getMessage()));
        }
    }

    /**
     * 更新考试信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Exam>> updateExam(@PathVariable Long id, 
                                                       @Valid @RequestBody Exam exam) {
        try {
            Exam updatedExam = adminExamService.updateExam(id, exam);
            return ResponseEntity.ok(ApiResponse.success("考试更新成功", updatedExam));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("考试更新失败: " + e.getMessage()));
        }
    }

    /**
     * 删除考试
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExam(@PathVariable Long id) {
        try {
            adminExamService.deleteExam(id);
            return ResponseEntity.ok(ApiResponse.success("考试删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("考试删除失败: " + e.getMessage()));
        }
    }

    /**
     * 批量删除考试
     */
    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> batchDeleteExams(@RequestBody List<Long> examIds) {
        try {
            adminExamService.batchDeleteExams(examIds);
            return ResponseEntity.ok(ApiResponse.success("批量删除成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("批量删除失败: " + e.getMessage()));
        }
    }

    /**
     * 获取考试列表（支持分页和排序）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Exam>>> getExams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Exam> exams;
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                // 搜索功能
                exams = adminExamService.searchExams(keyword, pageable);
            } else if (status != null || type != null || courseId != null || createdBy != null) {
                // 高级搜索
                Exam.ExamType examType = type != null ? Exam.ExamType.valueOf(type.toUpperCase()) : null;
                Exam.ExamStatus examStatus = status != null ? Exam.ExamStatus.valueOf(status.toUpperCase()) : null;
                
                exams = adminExamService.advancedSearchExams(
                    examType, examStatus, courseId, createdBy, null, pageable);
            } else if (startDate != null || endDate != null) {
                // 时间范围查询
                exams = adminExamService.getExamsByDateRange(startDate, endDate, pageable);
            } else {
                // 默认获取所有考试
                exams = adminExamService.getExamsByStatus(null, pageable);
            }
            
            Map<String, Object> pagination = Map.of(
                "currentPage", exams.getNumber(),
                "totalPages", exams.getTotalPages(),
                "totalElements", exams.getTotalElements(),
                "pageSize", exams.getSize(),
                "hasNext", exams.hasNext(),
                "hasPrevious", exams.hasPrevious()
            );
            
            return ResponseEntity.ok(ApiResponse.<List<Exam>>success(exams.getContent()).withPagination(pagination));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取考试列表失败: " + e.getMessage()));
        }
    }

    /**
     * 高级搜索考试
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<Exam>>> advancedSearch(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestBody Map<String, Object> searchCriteria) {
        
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // 解析搜索条件
            Exam.ExamType type = searchCriteria.get("type") != null ? 
                Exam.ExamType.valueOf(searchCriteria.get("type").toString().toUpperCase()) : null;
            Exam.ExamStatus status = searchCriteria.get("status") != null ? 
                Exam.ExamStatus.valueOf(searchCriteria.get("status").toString().toUpperCase()) : null;
            Long courseId = searchCriteria.get("courseId") != null ? 
                Long.valueOf(searchCriteria.get("courseId").toString()) : null;
            Long createdBy = searchCriteria.get("createdBy") != null ? 
                Long.valueOf(searchCriteria.get("createdBy").toString()) : null;
            String keyword = searchCriteria.get("keyword") != null ? 
                searchCriteria.get("keyword").toString() : null;
            
            Page<Exam> exams = adminExamService.advancedSearchExams(
                type, status, courseId, createdBy, keyword, pageable);
            
            Map<String, Object> pagination = Map.of(
                "currentPage", exams.getNumber(),
                "totalPages", exams.getTotalPages(),
                "totalElements", exams.getTotalElements(),
                "pageSize", exams.getSize(),
                "hasNext", exams.hasNext(),
                "hasPrevious", exams.hasPrevious()
            );
            
            return ResponseEntity.ok(ApiResponse.<List<Exam>>success(exams.getContent()).withPagination(pagination));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("高级搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 获取草稿考试
     */
    @GetMapping("/drafts")
    public ResponseEntity<ApiResponse<List<Exam>>> getDraftExams(
            @RequestParam Long createdBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Exam> exams = adminExamService.getDraftExamsByCreator(createdBy, pageable);
            
            Map<String, Object> pagination = Map.of(
                "currentPage", exams.getNumber(),
                "totalPages", exams.getTotalPages(),
                "totalElements", exams.getTotalElements(),
                "pageSize", exams.getSize(),
                "hasNext", exams.hasNext(),
                "hasPrevious", exams.hasPrevious()
            );
            
            return ResponseEntity.ok(ApiResponse.<List<Exam>>success(exams.getContent()).withPagination(pagination));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取草稿考试失败: " + e.getMessage()));
        }
    }

    /**
     * 获取待审核考试
     */
    @GetMapping("/review")
    public ResponseEntity<ApiResponse<List<Exam>>> getExamsForReview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Exam> exams = adminExamService.getExamsForReview(pageable);
            
            Map<String, Object> pagination = Map.of(
                "currentPage", exams.getNumber(),
                "totalPages", exams.getTotalPages(),
                "totalElements", exams.getTotalElements(),
                "pageSize", exams.getSize(),
                "hasNext", exams.hasNext(),
                "hasPrevious", exams.hasPrevious()
            );
            
            return ResponseEntity.ok(ApiResponse.<List<Exam>>success(exams.getContent()).withPagination(pagination));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取待审核考试失败: " + e.getMessage()));
        }
    }

    /**
     * 获取热门考试
     */
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<Exam>>> getPopularExams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Exam> exams = adminExamService.getPopularExams(pageable);
            
            Map<String, Object> pagination = Map.of(
                "currentPage", exams.getNumber(),
                "totalPages", exams.getTotalPages(),
                "totalElements", exams.getTotalElements(),
                "pageSize", exams.getSize(),
                "hasNext", exams.hasNext(),
                "hasPrevious", exams.hasPrevious()
            );
            
            return ResponseEntity.ok(ApiResponse.<List<Exam>>success(exams.getContent()).withPagination(pagination));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取热门考试失败: " + e.getMessage()));
        }
    }

    /**
     * 获取考试题目列表
     */
    @GetMapping("/{examId}/questions")
    public ResponseEntity<ApiResponse<List<ExamQuestion>>> getExamQuestions(
            @PathVariable Long examId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            if (page == -1) {
                // 获取所有题目
                List<ExamQuestion> questions = adminExamService.getExamQuestions(examId);
                return ResponseEntity.ok(ApiResponse.success(questions));
            } else {
                // 分页获取
                Pageable pageable = PageRequest.of(page, size);
                Page<ExamQuestion> questions = adminExamService.getExamQuestions(examId, pageable);
                
                Map<String, Object> pagination = Map.of(
                    "currentPage", questions.getNumber(),
                    "totalPages", questions.getTotalPages(),
                    "totalElements", questions.getTotalElements(),
                    "pageSize", questions.getSize(),
                    "hasNext", questions.hasNext(),
                    "hasPrevious", questions.hasPrevious()
                );
                
                return ResponseEntity.ok(ApiResponse.<List<ExamQuestion>>success(questions.getContent()).withPagination(pagination));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取考试题目失败: " + e.getMessage()));
        }
    }

    /**
     * 添加题目到考试
     */
    @PostMapping("/{examId}/questions")
    public ResponseEntity<ApiResponse<ExamQuestion>> addQuestionToExam(
            @PathVariable Long examId,
            @RequestBody Map<String, Object> requestData) {
        
        try {
            Long questionId = Long.valueOf(requestData.get("questionId").toString());
            BigDecimal points = new BigDecimal(requestData.get("points").toString());
            Integer order = requestData.get("order") != null ? 
                Integer.valueOf(requestData.get("order").toString()) : null;
            
            ExamQuestion examQuestion = adminExamService.addQuestionToExam(examId, questionId, points, order);
            return ResponseEntity.ok(ApiResponse.success("题目添加成功", examQuestion));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("添加题目失败: " + e.getMessage()));
        }
    }

    /**
     * 批量添加题目到考试
     */
    @PostMapping("/{examId}/questions/batch")
    public ResponseEntity<ApiResponse<Void>> batchAddQuestionsToExam(
            @PathVariable Long examId,
            @RequestBody Map<String, Object> requestData) {
        
        try {
            @SuppressWarnings("unchecked")
            List<Long> questionIds = (List<Long>) requestData.get("questionIds");
            BigDecimal defaultPoints = new BigDecimal(requestData.get("defaultPoints").toString());
            
            adminExamService.batchAddQuestionsToExam(examId, questionIds, defaultPoints);
            return ResponseEntity.ok(ApiResponse.success("批量添加题目成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("批量添加题目失败: " + e.getMessage()));
        }
    }

    /**
     * 从考试中移除题目
     */
    @DeleteMapping("/{examId}/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> removeQuestionFromExam(
            @PathVariable Long examId,
            @PathVariable Long questionId) {
        
        try {
            adminExamService.removeQuestionFromExam(examId, questionId);
            return ResponseEntity.ok(ApiResponse.success("题目移除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("移除题目失败: " + e.getMessage()));
        }
    }

    /**
     * 批量移除考试题目
     */
    @DeleteMapping("/{examId}/questions/batch")
    public ResponseEntity<ApiResponse<Void>> batchRemoveQuestionsFromExam(
            @PathVariable Long examId,
            @RequestBody List<Long> questionIds) {
        
        try {
            adminExamService.batchRemoveQuestionsFromExam(examId, questionIds);
            return ResponseEntity.ok(ApiResponse.success("批量移除题目成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("批量移除题目失败: " + e.getMessage()));
        }
    }

    /**
     * 更新题目分值
     */
    @PutMapping("/{examId}/questions/{questionId}/points")
    public ResponseEntity<ApiResponse<Void>> updateQuestionPoints(
            @PathVariable Long examId,
            @PathVariable Long questionId,
            @RequestBody Map<String, Object> requestData) {
        
        try {
            BigDecimal points = new BigDecimal(requestData.get("points").toString());
            adminExamService.updateQuestionPoints(examId, questionId, points);
            return ResponseEntity.ok(ApiResponse.success("题目分值更新成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新题目分值失败: " + e.getMessage()));
        }
    }

    /**
     * 更新题目顺序
     */
    @PutMapping("/{examId}/questions/{questionId}/order")
    public ResponseEntity<ApiResponse<Void>> updateQuestionOrder(
            @PathVariable Long examId,
            @PathVariable Long questionId,
            @RequestBody Map<String, Object> requestData) {
        
        try {
            Integer order = Integer.valueOf(requestData.get("order").toString());
            adminExamService.updateQuestionOrder(examId, questionId, order);
            return ResponseEntity.ok(ApiResponse.success("题目顺序更新成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新题目顺序失败: " + e.getMessage()));
        }
    }

    /**
     * 重新排序考试题目
     */
    @PostMapping("/{examId}/questions/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderExamQuestions(@PathVariable Long examId) {
        try {
            adminExamService.reorderExamQuestions(examId);
            return ResponseEntity.ok(ApiResponse.success("题目重新排序成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("题目重新排序失败: " + e.getMessage()));
        }
    }

    /**
     * 更新考试状态
     */
    @PutMapping("/{examId}/status")
    public ResponseEntity<ApiResponse<Void>> updateExamStatus(
            @PathVariable Long examId,
            @RequestBody Map<String, String> statusData) {
        
        try {
            Exam.ExamStatus status = Exam.ExamStatus.valueOf(statusData.get("status").toUpperCase());
            adminExamService.updateExamStatus(examId, status);
            return ResponseEntity.ok(ApiResponse.success("考试状态更新成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("考试状态更新失败: " + e.getMessage()));
        }
    }

    /**
     * 发布考试
     */
    @PostMapping("/{examId}/publish")
    public ResponseEntity<ApiResponse<Void>> publishExam(@PathVariable Long examId) {
        try {
            adminExamService.publishExam(examId);
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
     * 归档考试
     */
    @PostMapping("/{examId}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveExam(@PathVariable Long examId) {
        try {
            adminExamService.archiveExam(examId);
            return ResponseEntity.ok(ApiResponse.success("考试归档成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("考试归档失败: " + e.getMessage()));
        }
    }

    /**
     * 批量更新考试状态
     */
    @PutMapping("/batch-status")
    public ResponseEntity<ApiResponse<Void>> batchUpdateExamStatus(@RequestBody Map<String, Object> statusData) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> examIds = (List<Long>) statusData.get("examIds");
            Exam.ExamStatus status = Exam.ExamStatus.valueOf(statusData.get("status").toString().toUpperCase());
            
            adminExamService.batchUpdateExamStatus(examIds, status);
            return ResponseEntity.ok(ApiResponse.success("批量状态更新成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("批量状态更新失败: " + e.getMessage()));
        }
    }

    /**
     * 复制考试
     */
    @PostMapping("/{examId}/copy")
    public ResponseEntity<ApiResponse<Exam>> copyExam(
            @PathVariable Long examId,
            @RequestBody Map<String, Object> copyData) {
        
        try {
            String newTitle = copyData.get("title").toString();
            Long createdBy = Long.valueOf(copyData.get("createdBy").toString());
            
            Exam copiedExam = adminExamService.copyExam(examId, newTitle, createdBy);
            return ResponseEntity.ok(ApiResponse.success("考试复制成功", copiedExam));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("考试复制失败: " + e.getMessage()));
        }
    }

    /**
     * 获取考试统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExamStatistics() {
        try {
            List<Object[]> basicStats = adminExamService.getExamStatistics();
            List<Object[]> typeStats = adminExamService.countExamsByType();
            List<Object[]> statusStats = adminExamService.countExamsByStatus();
            List<Object[]> difficultyStats = adminExamService.getExamDifficultyDistribution();
            
            Map<String, Object> statistics = Map.of(
                "basic", basicStats,
                "byType", typeStats,
                "byStatus", statusStats,
                "byDifficulty", difficultyStats
            );
            
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取考试参与统计
     */
    @GetMapping("/participation-statistics")
    public ResponseEntity<ApiResponse<List<Object[]>>> getExamParticipationStatistics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Object[]> participationStats = adminExamService.getExamParticipationStatistics(pageable);
            
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
     * 获取指定时间段的考试统计
     */
    @GetMapping("/date-statistics")
    public ResponseEntity<ApiResponse<List<Object[]>>> getExamCountByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        try {
            List<Object[]> dateStats = adminExamService.getExamCountByDateRange(startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(dateStats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取时间统计失败: " + e.getMessage()));
        }
    }
}