package com.examSystem.userService.controller.student;

import com.examSystem.userService.dto.common.ApiResponse;
import com.examSystem.userService.entity.Exam;
import com.examSystem.userService.service.student.StudentExamService;
import com.examSystem.userService.service.student.StudentExamService.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生考试控制器
 * 
 * 提供学生考试相关的API接口：
 * - 考试列表查询
 * - 考试开始/结束
 * - 答题提交
 * - 成绩查询
 */
@RestController
@RequestMapping("/api/student/exams")
@CrossOrigin(origins = "*")
public class StudentExamController {

    @Autowired
    private StudentExamService studentExamService;

    /**
     * 获取可参加的考试列表
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<Exam>>> getAvailableExams(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Long studentId = getUserIdFromAuth(authentication);
            Pageable pageable = PageRequest.of(page, size);
            Page<Exam> exams = studentExamService.getAvailableExams(studentId, pageable);
            
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
     * 获取考试详细信息
     */
    @GetMapping("/{examId}")
    public ResponseEntity<ApiResponse<StudentExamInfo>> getExamInfo(
            @PathVariable Long examId,
            Authentication authentication) {
        
        try {
            Long studentId = getUserIdFromAuth(authentication);
            StudentExamInfo examInfo = studentExamService.getExamInfo(examId, studentId);
            return ResponseEntity.ok(ApiResponse.success(examInfo));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("获取考试信息失败: " + e.getMessage()));
        }
    }

    /**
     * 开始考试
     */
    @PostMapping("/{examId}/start")
    public ResponseEntity<ApiResponse<ExamSession>> startExam(
            @PathVariable Long examId,
            Authentication authentication) {
        
        try {
            Long studentId = getUserIdFromAuth(authentication);
            ExamSession session = studentExamService.startExam(examId, studentId);
            return ResponseEntity.ok(ApiResponse.success("考试开始成功", session));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("开始考试失败: " + e.getMessage()));
        }
    }

    /**
     * 获取考试题目列表
     */
    @GetMapping("/{examId}/questions")
    public ResponseEntity<ApiResponse<List<StudentQuestion>>> getExamQuestions(
            @PathVariable Long examId,
            @RequestParam Long attemptId,
            Authentication authentication) {
        
        try {
            Long studentId = getUserIdFromAuth(authentication);
            List<StudentQuestion> questions = studentExamService.getExamQuestions(examId, studentId, attemptId);
            return ResponseEntity.ok(ApiResponse.success(questions));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("获取题目失败: " + e.getMessage()));
        }
    }

    /**
     * 提交单题答案
     */
    @PostMapping("/{examId}/questions/{questionId}/answer")
    public ResponseEntity<ApiResponse<AnswerSubmissionResult>> submitAnswer(
            @PathVariable Long examId,
            @PathVariable Long questionId,
            @RequestBody AnswerSubmissionRequest request,
            Authentication authentication) {
        
        try {
            Long studentId = getUserIdFromAuth(authentication);
            AnswerSubmissionResult result = studentExamService.submitAnswer(
                examId, studentId, request.getAttemptId(), questionId, request.getStudentAnswer());
            return ResponseEntity.ok(ApiResponse.success("答案提交成功", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("提交答案失败: " + e.getMessage()));
        }
    }

    /**
     * 批量提交答案
     */
    @PostMapping("/{examId}/answers/batch")
    public ResponseEntity<ApiResponse<List<AnswerSubmissionResult>>> submitAnswersBatch(
            @PathVariable Long examId,
            @RequestBody BatchAnswerSubmissionRequest request,
            Authentication authentication) {
        
        try {
            Long studentId = getUserIdFromAuth(authentication);
            List<AnswerSubmissionResult> results = new java.util.ArrayList<>();
            
            for (BatchAnswerSubmissionRequest.Answer answer : request.getAnswers()) {
                try {
                    AnswerSubmissionResult result = studentExamService.submitAnswer(
                        examId, studentId, request.getAttemptId(), answer.getQuestionId(), answer.getStudentAnswer());
                    results.add(result);
                } catch (Exception e) {
                    results.add(new AnswerSubmissionResult(
                        null, answer.getQuestionId(), answer.getStudentAnswer(), 
                        java.time.LocalDateTime.now(), false, "提交失败: " + e.getMessage()));
                }
            }
            
            return ResponseEntity.ok(ApiResponse.success("批量提交完成", results));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("批量提交失败: " + e.getMessage()));
        }
    }

    /**
     * 完成考试
     */
    @PostMapping("/{examId}/finish")
    public ResponseEntity<ApiResponse<ExamCompletionResult>> finishExam(
            @PathVariable Long examId,
            @RequestBody ExamFinishRequest request,
            Authentication authentication) {
        
        try {
            Long studentId = getUserIdFromAuth(authentication);
            ExamCompletionResult result = studentExamService.finishExam(request.getAttemptId(), studentId);
            return ResponseEntity.ok(ApiResponse.success("考试完成", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("完成考试失败: " + e.getMessage()));
        }
    }

    /**
     * 获取考试结果
     */
    @GetMapping("/{examId}/result")
    public ResponseEntity<ApiResponse<ExamResult>> getExamResult(
            @PathVariable Long examId,
            @RequestParam Long attemptId,
            Authentication authentication) {
        
        try {
            Long studentId = getUserIdFromAuth(authentication);
            ExamResult result = studentExamService.getExamResult(attemptId, studentId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("获取考试结果失败: " + e.getMessage()));
        }
    }

    /**
     * 获取学生考试历史
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ExamHistory>>> getExamHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Long studentId = getUserIdFromAuth(authentication);
            Pageable pageable = PageRequest.of(page, size);
            Page<ExamHistory> history = studentExamService.getStudentExamHistory(studentId, pageable);
            
            Map<String, Object> pagination = Map.of(
                "currentPage", history.getNumber(),
                "totalPages", history.getTotalPages(),
                "totalElements", history.getTotalElements(),
                "pageSize", history.getSize(),
                "hasNext", history.hasNext(),
                "hasPrevious", history.hasPrevious()
            );
            
            return ResponseEntity.ok(ApiResponse.<List<ExamHistory>>success(history.getContent()).withPagination(pagination));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取考试历史失败: " + e.getMessage()));
        }
    }

    /**
     * 获取考试会话状态
     */
    @GetMapping("/{examId}/session/{attemptId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSessionStatus(
            @PathVariable Long examId,
            @PathVariable Long attemptId,
            Authentication authentication) {
        
        try {
            Long studentId = getUserIdFromAuth(authentication);
            
            // 获取考试信息和会话状态
            StudentExamInfo examInfo = studentExamService.getExamInfo(examId, studentId);
            
            Map<String, Object> sessionStatus = Map.of(
                "examId", examId,
                "attemptId", attemptId,
                "studentId", studentId,
                "currentTime", java.time.LocalDateTime.now(),
                "examStatus", examInfo.getStatus(),
                "remainingTime", calculateRemainingTime(examInfo)
            );
            
            return ResponseEntity.ok(ApiResponse.success(sessionStatus));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("获取会话状态失败: " + e.getMessage()));
        }
    }

    /**
     * 获取考试进度
     */
    @GetMapping("/{examId}/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExamProgress(
            @PathVariable Long examId,
            @RequestParam Long attemptId,
            Authentication authentication) {
        
        try {
            Long studentId = getUserIdFromAuth(authentication);
            List<StudentQuestion> questions = studentExamService.getExamQuestions(examId, studentId, attemptId);
            
            long answeredCount = questions.stream().mapToLong(q -> q.isAnswered() ? 1 : 0).sum();
            double progressPercentage = questions.isEmpty() ? 0 : (double) answeredCount / questions.size() * 100;
            
            Map<String, Object> progress = Map.of(
                "totalQuestions", questions.size(),
                "answeredQuestions", answeredCount,
                "unansweredQuestions", questions.size() - answeredCount,
                "progressPercentage", Math.round(progressPercentage * 100.0) / 100.0
            );
            
            return ResponseEntity.ok(ApiResponse.success(progress));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("获取考试进度失败: " + e.getMessage()));
        }
    }

    // 私有辅助方法

    private Long getUserIdFromAuth(Authentication authentication) {
        // 从认证信息中提取用户ID
        // 这里需要根据实际的认证实现来调整
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails = 
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
            // 假设用户名就是用户ID，实际实现可能需要从数据库查询
            return Long.parseLong(userDetails.getUsername());
        }
        throw new RuntimeException("用户未认证");
    }

    private long calculateRemainingTime(StudentExamInfo examInfo) {
        if (examInfo.getLastAttempt() != null && "IN_PROGRESS".equals(examInfo.getLastAttempt().getStatus())) {
            java.time.LocalDateTime endTime = examInfo.getLastAttempt().getEndTime();
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            return java.time.Duration.between(now, endTime).toMinutes();
        }
        return 0;
    }

    // 请求DTO类

    public static class AnswerSubmissionRequest {
        private Long attemptId;
        private String studentAnswer;

        // 构造函数
        public AnswerSubmissionRequest() {}

        public AnswerSubmissionRequest(Long attemptId, String studentAnswer) {
            this.attemptId = attemptId;
            this.studentAnswer = studentAnswer;
        }

        // Getters and Setters
        public Long getAttemptId() { return attemptId; }
        public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
        public String getStudentAnswer() { return studentAnswer; }
        public void setStudentAnswer(String studentAnswer) { this.studentAnswer = studentAnswer; }
    }

    public static class BatchAnswerSubmissionRequest {
        private Long attemptId;
        private List<Answer> answers;

        // 构造函数
        public BatchAnswerSubmissionRequest() {}

        public BatchAnswerSubmissionRequest(Long attemptId, List<Answer> answers) {
            this.attemptId = attemptId;
            this.answers = answers;
        }

        // Getters and Setters
        public Long getAttemptId() { return attemptId; }
        public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
        public List<Answer> getAnswers() { return answers; }
        public void setAnswers(List<Answer> answers) { this.answers = answers; }

        public static class Answer {
            private Long questionId;
            private String studentAnswer;

            // 构造函数
            public Answer() {}

            public Answer(Long questionId, String studentAnswer) {
                this.questionId = questionId;
                this.studentAnswer = studentAnswer;
            }

            // Getters and Setters
            public Long getQuestionId() { return questionId; }
            public void setQuestionId(Long questionId) { this.questionId = questionId; }
            public String getStudentAnswer() { return studentAnswer; }
            public void setStudentAnswer(String studentAnswer) { this.studentAnswer = studentAnswer; }
        }
    }

    public static class ExamFinishRequest {
        private Long attemptId;

        // 构造函数
        public ExamFinishRequest() {}

        public ExamFinishRequest(Long attemptId) {
            this.attemptId = attemptId;
        }

        // Getters and Setters
        public Long getAttemptId() { return attemptId; }
        public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
    }
}