package com.examSystem.userService.controller.admin;

import com.examSystem.userService.entity.Question;
import com.examSystem.userService.service.admin.AdminQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 管理员题库管理控制器
 * 
 * 提供题目的CRUD操作、批量管理、搜索和统计功能
 */
@RestController
@RequestMapping("/api/admin/questions")
@CrossOrigin(origins = "*")
public class AdminQuestionController {

    @Autowired
    private AdminQuestionService adminQuestionService;

    /**
     * 创建新题目
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createQuestion(@Valid @RequestBody Question question) {
        Map<String, Object> response = new HashMap<>();
        try {
            Question createdQuestion = adminQuestionService.createQuestion(question);
            response.put("success", true);
            response.put("message", "题目创建成功");
            response.put("data", createdQuestion);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "题目创建失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 根据ID获取题目详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getQuestion(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Question> question = adminQuestionService.getQuestionById(id);
            if (question.isPresent()) {
                response.put("success", true);
                response.put("data", question.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "题目不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取题目失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新题目信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateQuestion(@PathVariable Long id, 
                                                             @Valid @RequestBody Question question) {
        Map<String, Object> response = new HashMap<>();
        try {
            Question updatedQuestion = adminQuestionService.updateQuestion(id, question);
            response.put("success", true);
            response.put("message", "题目更新成功");
            response.put("data", updatedQuestion);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "题目更新失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 删除题目
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteQuestion(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            adminQuestionService.deleteQuestion(id);
            response.put("success", true);
            response.put("message", "题目删除成功");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "题目删除失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 批量删除题目
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchDeleteQuestions(@RequestBody List<Long> questionIds) {
        Map<String, Object> response = new HashMap<>();
        try {
            adminQuestionService.batchDeleteQuestions(questionIds);
            response.put("success", true);
            response.put("message", "批量删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "批量删除失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取题目列表（支持分页和排序）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) String keyword) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Question> questions;
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                // 如果有搜索关键字，使用搜索功能
                questions = adminQuestionService.searchQuestions(keyword, pageable);
            } else if (status != null || type != null || difficulty != null) {
                // 高级搜索
                Question.QuestionType questionType = type != null ? Question.QuestionType.valueOf(type.toUpperCase()) : null;
                Question.DifficultyLevel difficultyLevel = difficulty != null ? Question.DifficultyLevel.valueOf(difficulty.toUpperCase()) : null;
                Question.QuestionStatus questionStatus = status != null ? Question.QuestionStatus.valueOf(status.toUpperCase()) : null;
                
                questions = adminQuestionService.advancedSearchQuestions(
                    questionType, difficultyLevel, null, null, questionStatus, null, pageable);
            } else if (createdBy != null) {
                // 按创建者过滤
                questions = adminQuestionService.getQuestionsByCreator(createdBy, pageable);
            } else {
                // 获取活跃题目
                questions = adminQuestionService.getActiveQuestions(pageable);
            }
            
            response.put("success", true);
            response.put("data", questions.getContent());
            response.put("pagination", Map.of(
                "currentPage", questions.getNumber(),
                "totalPages", questions.getTotalPages(),
                "totalElements", questions.getTotalElements(),
                "pageSize", questions.getSize(),
                "hasNext", questions.hasNext(),
                "hasPrevious", questions.hasPrevious()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取题目列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 高级搜索题目
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> advancedSearch(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestBody Map<String, Object> searchCriteria) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // 解析搜索条件
            Question.QuestionType type = searchCriteria.get("type") != null ? 
                Question.QuestionType.valueOf(searchCriteria.get("type").toString().toUpperCase()) : null;
            Question.DifficultyLevel difficulty = searchCriteria.get("difficulty") != null ? 
                Question.DifficultyLevel.valueOf(searchCriteria.get("difficulty").toString().toUpperCase()) : null;
            Question.QuestionStatus status = searchCriteria.get("status") != null ? 
                Question.QuestionStatus.valueOf(searchCriteria.get("status").toString().toUpperCase()) : null;
            Long categoryId = searchCriteria.get("categoryId") != null ? 
                Long.valueOf(searchCriteria.get("categoryId").toString()) : null;
            Long organizationId = searchCriteria.get("organizationId") != null ? 
                Long.valueOf(searchCriteria.get("organizationId").toString()) : null;
            String keyword = searchCriteria.get("keyword") != null ? 
                searchCriteria.get("keyword").toString() : null;
            
            Page<Question> questions = adminQuestionService.advancedSearchQuestions(
                type, difficulty, categoryId, organizationId, status, keyword, pageable);
            
            response.put("success", true);
            response.put("data", questions.getContent());
            response.put("pagination", Map.of(
                "currentPage", questions.getNumber(),
                "totalPages", questions.getTotalPages(),
                "totalElements", questions.getTotalElements(),
                "pageSize", questions.getSize(),
                "hasNext", questions.hasNext(),
                "hasPrevious", questions.hasPrevious()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "高级搜索失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取随机题目
     */
    @GetMapping("/random")
    public ResponseEntity<Map<String, Object>> getRandomQuestions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "10") int limit) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Question.QuestionType questionType = type != null ? Question.QuestionType.valueOf(type.toUpperCase()) : null;
            Question.DifficultyLevel difficultyLevel = difficulty != null ? Question.DifficultyLevel.valueOf(difficulty.toUpperCase()) : null;
            
            List<Question> questions = adminQuestionService.getRandomQuestions(questionType, difficultyLevel, limit);
            
            response.put("success", true);
            response.put("data", questions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取随机题目失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取热门题目
     */
    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Question> questions = adminQuestionService.getPopularQuestions(pageable);
            
            response.put("success", true);
            response.put("data", questions.getContent());
            response.put("pagination", Map.of(
                "currentPage", questions.getNumber(),
                "totalPages", questions.getTotalPages(),
                "totalElements", questions.getTotalElements(),
                "pageSize", questions.getSize(),
                "hasNext", questions.hasNext(),
                "hasPrevious", questions.hasPrevious()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取热门题目失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取需要审核的题目
     */
    @GetMapping("/review")
    public ResponseEntity<Map<String, Object>> getQuestionsNeedingReview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Question> questions = adminQuestionService.getQuestionsNeedingReview(pageable);
            
            response.put("success", true);
            response.put("data", questions.getContent());
            response.put("pagination", Map.of(
                "currentPage", questions.getNumber(),
                "totalPages", questions.getTotalPages(),
                "totalElements", questions.getTotalElements(),
                "pageSize", questions.getSize(),
                "hasNext", questions.hasNext(),
                "hasPrevious", questions.hasPrevious()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取待审核题目失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 审核题目
     */
    @PostMapping("/{id}/review")
    public ResponseEntity<Map<String, Object>> reviewQuestion(
            @PathVariable Long id,
            @RequestBody Map<String, Object> reviewData) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            String reviewStatus = reviewData.get("reviewStatus").toString();
            Long reviewedBy = Long.valueOf(reviewData.get("reviewedBy").toString());
            String reviewNotes = reviewData.get("reviewNotes") != null ? 
                reviewData.get("reviewNotes").toString() : null;
            
            adminQuestionService.reviewQuestion(id, reviewStatus, reviewedBy, reviewNotes);
            
            response.put("success", true);
            response.put("message", "题目审核成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "题目审核失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 批量审核题目
     */
    @PostMapping("/batch-review")
    public ResponseEntity<Map<String, Object>> batchReviewQuestions(@RequestBody Map<String, Object> reviewData) {
        Map<String, Object> response = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            List<Long> questionIds = (List<Long>) reviewData.get("questionIds");
            String reviewStatus = reviewData.get("reviewStatus").toString();
            Long reviewedBy = Long.valueOf(reviewData.get("reviewedBy").toString());
            String reviewNotes = reviewData.get("reviewNotes") != null ? 
                reviewData.get("reviewNotes").toString() : null;
            
            adminQuestionService.batchReviewQuestions(questionIds, reviewStatus, reviewedBy, reviewNotes);
            
            response.put("success", true);
            response.put("message", "批量审核成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "批量审核失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新题目状态
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateQuestionStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusData) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Question.QuestionStatus status = Question.QuestionStatus.valueOf(statusData.get("status").toUpperCase());
            adminQuestionService.updateQuestionStatus(id, status);
            
            response.put("success", true);
            response.put("message", "题目状态更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "题目状态更新失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 批量更新题目状态
     */
    @PutMapping("/batch-status")
    public ResponseEntity<Map<String, Object>> batchUpdateQuestionStatus(@RequestBody Map<String, Object> statusData) {
        Map<String, Object> response = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            List<Long> questionIds = (List<Long>) statusData.get("questionIds");
            Question.QuestionStatus status = Question.QuestionStatus.valueOf(statusData.get("status").toString().toUpperCase());
            
            adminQuestionService.batchUpdateQuestionStatus(questionIds, status);
            
            response.put("success", true);
            response.put("message", "批量状态更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "批量状态更新失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 创建题目新版本
     */
    @PostMapping("/{id}/version")
    public ResponseEntity<Map<String, Object>> createNewVersion(
            @PathVariable Long id,
            @Valid @RequestBody Question newVersionData) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Question newVersion = adminQuestionService.createNewVersion(id, newVersionData);
            response.put("success", true);
            response.put("message", "新版本创建成功");
            response.put("data", newVersion);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "创建新版本失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取题目的所有版本
     */
    @GetMapping("/{id}/versions")
    public ResponseEntity<Map<String, Object>> getQuestionVersions(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Question> versions = adminQuestionService.getQuestionVersions(id);
            response.put("success", true);
            response.put("data", versions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取版本列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取相似题目
     */
    @GetMapping("/{id}/similar")
    public ResponseEntity<Map<String, Object>> getSimilarQuestions(
            @PathVariable Long id,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Pageable pageable = PageRequest.of(page, size);
            List<Question> similarQuestions = adminQuestionService.findSimilarQuestions(id, keyword, pageable);
            
            response.put("success", true);
            response.put("data", similarQuestions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取相似题目失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取题目统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getQuestionStatistics() {
        Map<String, Object> response = new HashMap<>();
        try {
            // 获取基本统计
            List<Object[]> basicStats = adminQuestionService.getQuestionStatistics();
            List<Object[]> typeStats = adminQuestionService.countQuestionsByType();
            List<Object[]> difficultyStats = adminQuestionService.countQuestionsByDifficulty();
            
            response.put("success", true);
            response.put("data", Map.of(
                "basic", basicStats,
                "byType", typeStats,
                "byDifficulty", difficultyStats
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取题目使用情况统计
     */
    @GetMapping("/usage-statistics")
    public ResponseEntity<Map<String, Object>> getQuestionUsageStatistics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Object[]> usageStats = adminQuestionService.getQuestionUsageStatistics(pageable);
            
            response.put("success", true);
            response.put("data", usageStats.getContent());
            response.put("pagination", Map.of(
                "currentPage", usageStats.getNumber(),
                "totalPages", usageStats.getTotalPages(),
                "totalElements", usageStats.getTotalElements(),
                "pageSize", usageStats.getSize(),
                "hasNext", usageStats.hasNext(),
                "hasPrevious", usageStats.hasPrevious()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取使用统计失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新题目使用统计
     */
    @PostMapping("/{id}/usage")
    public ResponseEntity<Map<String, Object>> updateUsageStatistics(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            adminQuestionService.updateUsageStatistics(id);
            response.put("success", true);
            response.put("message", "使用统计更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "使用统计更新失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}