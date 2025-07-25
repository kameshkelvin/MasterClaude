package com.examSystem.userService.service.student;

import com.examSystem.userService.entity.Exam;
import com.examSystem.userService.entity.ExamAttempt;
import com.examSystem.userService.entity.Question;
import com.examSystem.userService.entity.Answer;
import com.examSystem.userService.repository.ExamRepository;
import com.examSystem.userService.repository.ExamAttemptRepository;
import com.examSystem.userService.repository.QuestionRepository;
import com.examSystem.userService.repository.AnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

/**
 * 学生考试服务类
 * 
 * 提供学生参与考试的核心功能：
 * - 考试列表查询
 * - 考试开始和结束
 * - 答题提交
 * - 成绩查询
 */
@Service
@Transactional
public class StudentExamService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    /**
     * 获取学生可参加的考试列表
     */
    @Transactional(readOnly = true)
    public Page<Exam> getAvailableExams(Long studentId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        return examRepository.findAvailableExamsForStudent(studentId, now, pageable);
    }

    /**
     * 获取考试详细信息（学生视角）
     */
    @Transactional(readOnly = true)
    public StudentExamInfo getExamInfo(Long examId, Long studentId) {
        Optional<Exam> examOpt = examRepository.findById(examId);
        if (!examOpt.isPresent()) {
            throw new RuntimeException("考试不存在");
        }

        Exam exam = examOpt.get();
        
        // 检查学生是否有权限参加此考试
        if (!canStudentTakeExam(studentId, examId)) {
            throw new RuntimeException("您没有权限参加此考试");
        }

        // 获取学生的考试尝试记录
        List<ExamAttempt> attempts = examAttemptRepository.findByExamIdAndStudentId(examId, studentId);
        
        // 检查考试状态
        ExamStatus status = determineExamStatus(exam, attempts);

        return new StudentExamInfo(
            exam.getId(),
            exam.getTitle(),
            exam.getDescription(),
            exam.getStartTime(),
            exam.getEndTime(),
            exam.getDuration(),
            exam.getMaxAttempts(),
            exam.getPassingScore(),
            attempts.size(),
            status,
            !attempts.isEmpty() ? attempts.get(attempts.size() - 1) : null
        );
    }

    /**
     * 开始考试
     */
    public ExamSession startExam(Long examId, Long studentId) {
        // 验证考试和学生权限
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("考试不存在"));

        if (!canStudentTakeExam(studentId, examId)) {
            throw new RuntimeException("您没有权限参加此考试");
        }

        // 检查考试时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime())) {
            throw new RuntimeException("考试尚未开始");
        }
        if (now.isAfter(exam.getEndTime())) {
            throw new RuntimeException("考试已结束");
        }

        // 检查尝试次数
        List<ExamAttempt> attempts = examAttemptRepository.findByExamIdAndStudentId(examId, studentId);
        if (attempts.size() >= exam.getMaxAttempts()) {
            throw new RuntimeException("已达到最大尝试次数");
        }

        // 检查是否有进行中的考试
        Optional<ExamAttempt> activeAttempt = attempts.stream()
            .filter(attempt -> "IN_PROGRESS".equals(attempt.getStatus()))
            .findFirst();

        if (activeAttempt.isPresent()) {
            // 返回现有的考试会话
            return createExamSession(activeAttempt.get(), exam);
        }

        // 创建新的考试尝试
        ExamAttempt newAttempt = new ExamAttempt();
        newAttempt.setExamId(examId);
        newAttempt.setStudentId(studentId);
        newAttempt.setStartTime(now);
        newAttempt.setEndTime(now.plus(exam.getDuration()));
        newAttempt.setStatus("IN_PROGRESS");
        newAttempt.setAttemptNumber(attempts.size() + 1);
        
        ExamAttempt savedAttempt = examAttemptRepository.save(newAttempt);

        return createExamSession(savedAttempt, exam);
    }

    /**
     * 获取考试题目列表
     */
    @Transactional(readOnly = true)
    public List<StudentQuestion> getExamQuestions(Long examId, Long studentId, Long attemptId) {
        // 验证考试会话
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("考试会话不存在"));

        if (!attempt.getStudentId().equals(studentId) || !attempt.getExamId().equals(examId)) {
            throw new RuntimeException("无效的考试会话");
        }

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new RuntimeException("考试已结束或未开始");
        }

        // 获取题目列表
        List<Question> questions = questionRepository.findByExamIdOrderByOrderNumber(examId);
        
        // 获取学生已提交的答案
        List<Answer> existingAnswers = answerRepository.findByAttemptId(attemptId);
        Map<Long, Answer> answerMap = new HashMap<>();
        for (Answer answer : existingAnswers) {
            answerMap.put(answer.getQuestionId(), answer);
        }

        // 转换为学生视角的题目信息
        return questions.stream().map(question -> {
            Answer existingAnswer = answerMap.get(question.getId());
            return new StudentQuestion(
                question.getId(),
                question.getType(),
                question.getContent(),
                question.getOptions(),
                question.getOrderNumber(),
                existingAnswer != null ? existingAnswer.getStudentAnswer() : null,
                existingAnswer != null
            );
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 提交单题答案
     */
    public AnswerSubmissionResult submitAnswer(Long examId, Long studentId, Long attemptId, 
                                             Long questionId, String studentAnswer) {
        // 验证考试会话
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("考试会话不存在"));

        if (!attempt.getStudentId().equals(studentId) || !attempt.getExamId().equals(examId)) {
            throw new RuntimeException("无效的考试会话");
        }

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new RuntimeException("考试已结束，无法提交答案");
        }

        // 检查考试时间
        if (LocalDateTime.now().isAfter(attempt.getEndTime())) {
            // 自动结束考试
            finishExam(attemptId, studentId);
            throw new RuntimeException("考试时间已到，无法提交答案");
        }

        // 验证题目
        Question question = questionRepository.findByIdAndExamId(questionId, examId)
            .orElseThrow(() -> new RuntimeException("题目不存在"));

        // 查找或创建答案记录
        Optional<Answer> existingAnswer = answerRepository.findByAttemptIdAndQuestionId(attemptId, questionId);
        Answer answer;
        
        if (existingAnswer.isPresent()) {
            answer = existingAnswer.get();
            answer.setStudentAnswer(studentAnswer);
            answer.setSubmitTime(LocalDateTime.now());
        } else {
            answer = new Answer();
            answer.setAttemptId(attemptId);
            answer.setQuestionId(questionId);
            answer.setStudentAnswer(studentAnswer);
            answer.setSubmitTime(LocalDateTime.now());
        }

        Answer savedAnswer = answerRepository.save(answer);

        return new AnswerSubmissionResult(
            savedAnswer.getId(),
            questionId,
            studentAnswer,
            LocalDateTime.now(),
            true,
            "答案提交成功"
        );
    }

    /**
     * 完成考试
     */
    public ExamCompletionResult finishExam(Long attemptId, Long studentId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("考试会话不存在"));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new RuntimeException("无效的考试会话");
        }

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new RuntimeException("考试已结束或未开始");
        }

        // 更新考试状态
        attempt.setStatus("COMPLETED");
        attempt.setSubmitTime(LocalDateTime.now());

        // 计算成绩
        ExamGradingResult gradingResult = calculateExamScore(attemptId);
        attempt.setScore(gradingResult.getTotalScore());
        attempt.setGradedAt(LocalDateTime.now());

        ExamAttempt savedAttempt = examAttemptRepository.save(attempt);

        return new ExamCompletionResult(
            savedAttempt.getId(),
            savedAttempt.getScore(),
            gradingResult.getCorrectAnswers(),
            gradingResult.getTotalQuestions(),
            gradingResult.isPassed(),
            LocalDateTime.now(),
            "考试完成"
        );
    }

    /**
     * 获取考试结果
     */
    @Transactional(readOnly = true)
    public ExamResult getExamResult(Long attemptId, Long studentId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("考试记录不存在"));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new RuntimeException("无权限查看此考试结果");
        }

        if (!"COMPLETED".equals(attempt.getStatus())) {
            throw new RuntimeException("考试尚未完成");
        }

        Exam exam = examRepository.findById(attempt.getExamId())
            .orElseThrow(() -> new RuntimeException("考试不存在"));

        // 获取详细答题情况
        List<Answer> answers = answerRepository.findByAttemptId(attemptId);
        List<Question> questions = questionRepository.findByExamIdOrderByOrderNumber(attempt.getExamId());

        List<QuestionResult> questionResults = questions.stream().map(question -> {
            Answer answer = answers.stream()
                .filter(a -> a.getQuestionId().equals(question.getId()))
                .findFirst()
                .orElse(null);

            return new QuestionResult(
                question.getId(),
                question.getContent(),
                question.getCorrectAnswer(),
                answer != null ? answer.getStudentAnswer() : null,
                answer != null ? answer.getIsCorrect() : false,
                answer != null ? answer.getScore() : BigDecimal.ZERO
            );
        }).collect(java.util.stream.Collectors.toList());

        return new ExamResult(
            attempt.getId(),
            exam.getTitle(),
            attempt.getScore(),
            exam.getPassingScore(),
            attempt.getScore().compareTo(exam.getPassingScore()) >= 0,
            attempt.getStartTime(),
            attempt.getSubmitTime(),
            questionResults
        );
    }

    /**
     * 获取学生考试历史
     */
    @Transactional(readOnly = true)
    public Page<ExamHistory> getStudentExamHistory(Long studentId, Pageable pageable) {
        Page<ExamAttempt> attempts = examAttemptRepository.findByStudentIdOrderByStartTimeDesc(studentId, pageable);
        
        return attempts.map(attempt -> {
            Exam exam = examRepository.findById(attempt.getExamId()).orElse(null);
            return new ExamHistory(
                attempt.getId(),
                exam != null ? exam.getTitle() : "未知考试",
                attempt.getScore(),
                attempt.getStatus(),
                attempt.getStartTime(),
                attempt.getSubmitTime(),
                attempt.getAttemptNumber()
            );
        });
    }

    // 私有辅助方法

    private boolean canStudentTakeExam(Long studentId, Long examId) {
        // 这里应该检查学生权限，比如课程注册、考试权限等
        // 暂时返回true，实际实现需要根据业务规则
        return true;
    }

    private ExamStatus determineExamStatus(Exam exam, List<ExamAttempt> attempts) {
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isBefore(exam.getStartTime())) {
            return ExamStatus.NOT_STARTED;
        }
        
        if (now.isAfter(exam.getEndTime())) {
            return ExamStatus.ENDED;
        }

        if (attempts.size() >= exam.getMaxAttempts()) {
            return ExamStatus.MAX_ATTEMPTS_REACHED;
        }

        Optional<ExamAttempt> activeAttempt = attempts.stream()
            .filter(attempt -> "IN_PROGRESS".equals(attempt.getStatus()))
            .findFirst();

        if (activeAttempt.isPresent()) {
            return ExamStatus.IN_PROGRESS;
        }

        return ExamStatus.AVAILABLE;
    }

    private ExamSession createExamSession(ExamAttempt attempt, Exam exam) {
        return new ExamSession(
            attempt.getId(),
            exam.getId(),
            exam.getTitle(),
            attempt.getStartTime(),
            attempt.getEndTime(),
            LocalDateTime.now(),
            exam.getDuration().toMinutes()
        );
    }

    private ExamGradingResult calculateExamScore(Long attemptId) {
        List<Answer> answers = answerRepository.findByAttemptId(attemptId);
        List<Question> questions = questionRepository.findByAttemptIdQuestions(attemptId);

        int totalQuestions = questions.size();
        int correctAnswers = 0;
        BigDecimal totalScore = BigDecimal.ZERO;

        for (Answer answer : answers) {
            Question question = questions.stream()
                .filter(q -> q.getId().equals(answer.getQuestionId()))
                .findFirst()
                .orElse(null);

            if (question != null) {
                boolean isCorrect = checkAnswer(question, answer.getStudentAnswer());
                answer.setIsCorrect(isCorrect);
                
                if (isCorrect) {
                    correctAnswers++;
                    answer.setScore(question.getPoints());
                    totalScore = totalScore.add(question.getPoints());
                } else {
                    answer.setScore(BigDecimal.ZERO);
                }
                
                answerRepository.save(answer);
            }
        }

        // 计算总分百分比
        BigDecimal maxPossibleScore = questions.stream()
            .map(Question::getPoints)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentageScore = maxPossibleScore.compareTo(BigDecimal.ZERO) > 0 
            ? totalScore.divide(maxPossibleScore, 2, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("考试记录不存在"));
        
        Exam exam = examRepository.findById(attempt.getExamId())
            .orElseThrow(() -> new RuntimeException("考试不存在"));

        boolean passed = percentageScore.compareTo(exam.getPassingScore()) >= 0;

        return new ExamGradingResult(
            percentageScore,
            correctAnswers,
            totalQuestions,
            passed
        );
    }

    private boolean checkAnswer(Question question, String studentAnswer) {
        if (studentAnswer == null || question.getCorrectAnswer() == null) {
            return false;
        }

        switch (question.getType()) {
            case "SINGLE_CHOICE":
            case "MULTIPLE_CHOICE":
            case "TRUE_FALSE":
                return question.getCorrectAnswer().trim().equalsIgnoreCase(studentAnswer.trim());
            case "FILL_BLANK":
                // 简单的文本匹配，可以扩展为更复杂的匹配规则
                return question.getCorrectAnswer().trim().equalsIgnoreCase(studentAnswer.trim());
            case "ESSAY":
                // 主观题需要人工评分，暂时返回false
                return false;
            default:
                return false;
        }
    }

    // 内部类定义

    public static class StudentExamInfo {
        private final Long examId;
        private final String title;
        private final String description;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final java.time.Duration duration;
        private final Integer maxAttempts;
        private final BigDecimal passingScore;
        private final Integer attemptCount;
        private final ExamStatus status;
        private final ExamAttempt lastAttempt;

        public StudentExamInfo(Long examId, String title, String description, 
                             LocalDateTime startTime, LocalDateTime endTime, 
                             java.time.Duration duration, Integer maxAttempts, 
                             BigDecimal passingScore, Integer attemptCount, 
                             ExamStatus status, ExamAttempt lastAttempt) {
            this.examId = examId;
            this.title = title;
            this.description = description;
            this.startTime = startTime;
            this.endTime = endTime;
            this.duration = duration;
            this.maxAttempts = maxAttempts;
            this.passingScore = passingScore;
            this.attemptCount = attemptCount;
            this.status = status;
            this.lastAttempt = lastAttempt;
        }

        // Getters
        public Long getExamId() { return examId; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public java.time.Duration getDuration() { return duration; }
        public Integer getMaxAttempts() { return maxAttempts; }
        public BigDecimal getPassingScore() { return passingScore; }
        public Integer getAttemptCount() { return attemptCount; }
        public ExamStatus getStatus() { return status; }
        public ExamAttempt getLastAttempt() { return lastAttempt; }
    }

    public static class ExamSession {
        private final Long attemptId;
        private final Long examId;
        private final String examTitle;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final LocalDateTime currentTime;
        private final Long remainingMinutes;

        public ExamSession(Long attemptId, Long examId, String examTitle,
                         LocalDateTime startTime, LocalDateTime endTime,
                         LocalDateTime currentTime, Long totalMinutes) {
            this.attemptId = attemptId;
            this.examId = examId;
            this.examTitle = examTitle;
            this.startTime = startTime;
            this.endTime = endTime;
            this.currentTime = currentTime;
            
            long elapsed = java.time.Duration.between(startTime, currentTime).toMinutes();
            this.remainingMinutes = Math.max(0, totalMinutes - elapsed);
        }

        // Getters
        public Long getAttemptId() { return attemptId; }
        public Long getExamId() { return examId; }
        public String getExamTitle() { return examTitle; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public LocalDateTime getCurrentTime() { return currentTime; }
        public Long getRemainingMinutes() { return remainingMinutes; }
    }

    public static class StudentQuestion {
        private final Long questionId;
        private final String type;
        private final String content;
        private final String options;
        private final Integer orderNumber;
        private final String studentAnswer;
        private final boolean answered;

        public StudentQuestion(Long questionId, String type, String content,
                             String options, Integer orderNumber,
                             String studentAnswer, boolean answered) {
            this.questionId = questionId;
            this.type = type;
            this.content = content;
            this.options = options;
            this.orderNumber = orderNumber;
            this.studentAnswer = studentAnswer;
            this.answered = answered;
        }

        // Getters
        public Long getQuestionId() { return questionId; }
        public String getType() { return type; }
        public String getContent() { return content; }
        public String getOptions() { return options; }
        public Integer getOrderNumber() { return orderNumber; }
        public String getStudentAnswer() { return studentAnswer; }
        public boolean isAnswered() { return answered; }
    }

    public static class AnswerSubmissionResult {
        private final Long answerId;
        private final Long questionId;
        private final String studentAnswer;
        private final LocalDateTime submitTime;
        private final boolean success;
        private final String message;

        public AnswerSubmissionResult(Long answerId, Long questionId, String studentAnswer,
                                    LocalDateTime submitTime, boolean success, String message) {
            this.answerId = answerId;
            this.questionId = questionId;
            this.studentAnswer = studentAnswer;
            this.submitTime = submitTime;
            this.success = success;
            this.message = message;
        }

        // Getters
        public Long getAnswerId() { return answerId; }
        public Long getQuestionId() { return questionId; }
        public String getStudentAnswer() { return studentAnswer; }
        public LocalDateTime getSubmitTime() { return submitTime; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    public static class ExamCompletionResult {
        private final Long attemptId;
        private final BigDecimal score;
        private final int correctAnswers;
        private final int totalQuestions;
        private final boolean passed;
        private final LocalDateTime completionTime;
        private final String message;

        public ExamCompletionResult(Long attemptId, BigDecimal score, int correctAnswers,
                                  int totalQuestions, boolean passed,
                                  LocalDateTime completionTime, String message) {
            this.attemptId = attemptId;
            this.score = score;
            this.correctAnswers = correctAnswers;
            this.totalQuestions = totalQuestions;
            this.passed = passed;
            this.completionTime = completionTime;
            this.message = message;
        }

        // Getters
        public Long getAttemptId() { return attemptId; }
        public BigDecimal getScore() { return score; }
        public int getCorrectAnswers() { return correctAnswers; }
        public int getTotalQuestions() { return totalQuestions; }
        public boolean isPassed() { return passed; }
        public LocalDateTime getCompletionTime() { return completionTime; }
        public String getMessage() { return message; }
    }

    public static class ExamResult {
        private final Long attemptId;
        private final String examTitle;
        private final BigDecimal score;
        private final BigDecimal passingScore;
        private final boolean passed;
        private final LocalDateTime startTime;
        private final LocalDateTime submitTime;
        private final List<QuestionResult> questionResults;

        public ExamResult(Long attemptId, String examTitle, BigDecimal score,
                         BigDecimal passingScore, boolean passed,
                         LocalDateTime startTime, LocalDateTime submitTime,
                         List<QuestionResult> questionResults) {
            this.attemptId = attemptId;
            this.examTitle = examTitle;
            this.score = score;
            this.passingScore = passingScore;
            this.passed = passed;
            this.startTime = startTime;
            this.submitTime = submitTime;
            this.questionResults = questionResults;
        }

        // Getters
        public Long getAttemptId() { return attemptId; }
        public String getExamTitle() { return examTitle; }
        public BigDecimal getScore() { return score; }
        public BigDecimal getPassingScore() { return passingScore; }
        public boolean isPassed() { return passed; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getSubmitTime() { return submitTime; }
        public List<QuestionResult> getQuestionResults() { return questionResults; }
    }

    public static class QuestionResult {
        private final Long questionId;
        private final String content;
        private final String correctAnswer;
        private final String studentAnswer;
        private final boolean correct;
        private final BigDecimal score;

        public QuestionResult(Long questionId, String content, String correctAnswer,
                            String studentAnswer, boolean correct, BigDecimal score) {
            this.questionId = questionId;
            this.content = content;
            this.correctAnswer = correctAnswer;
            this.studentAnswer = studentAnswer;
            this.correct = correct;
            this.score = score;
        }

        // Getters
        public Long getQuestionId() { return questionId; }
        public String getContent() { return content; }
        public String getCorrectAnswer() { return correctAnswer; }
        public String getStudentAnswer() { return studentAnswer; }
        public boolean isCorrect() { return correct; }
        public BigDecimal getScore() { return score; }
    }

    public static class ExamHistory {
        private final Long attemptId;
        private final String examTitle;
        private final BigDecimal score;
        private final String status;
        private final LocalDateTime startTime;
        private final LocalDateTime submitTime;
        private final Integer attemptNumber;

        public ExamHistory(Long attemptId, String examTitle, BigDecimal score,
                         String status, LocalDateTime startTime,
                         LocalDateTime submitTime, Integer attemptNumber) {
            this.attemptId = attemptId;
            this.examTitle = examTitle;
            this.score = score;
            this.status = status;
            this.startTime = startTime;
            this.submitTime = submitTime;
            this.attemptNumber = attemptNumber;
        }

        // Getters
        public Long getAttemptId() { return attemptId; }
        public String getExamTitle() { return examTitle; }
        public BigDecimal getScore() { return score; }
        public String getStatus() { return status; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getSubmitTime() { return submitTime; }
        public Integer getAttemptNumber() { return attemptNumber; }
    }

    public static class ExamGradingResult {
        private final BigDecimal totalScore;
        private final int correctAnswers;
        private final int totalQuestions;
        private final boolean passed;

        public ExamGradingResult(BigDecimal totalScore, int correctAnswers,
                               int totalQuestions, boolean passed) {
            this.totalScore = totalScore;
            this.correctAnswers = correctAnswers;
            this.totalQuestions = totalQuestions;
            this.passed = passed;
        }

        // Getters
        public BigDecimal getTotalScore() { return totalScore; }
        public int getCorrectAnswers() { return correctAnswers; }
        public int getTotalQuestions() { return totalQuestions; }
        public boolean isPassed() { return passed; }
    }

    public enum ExamStatus {
        NOT_STARTED,        // 尚未开始
        AVAILABLE,          // 可以参加
        IN_PROGRESS,        // 正在进行
        ENDED,              // 已结束
        MAX_ATTEMPTS_REACHED // 达到最大尝试次数
    }
}