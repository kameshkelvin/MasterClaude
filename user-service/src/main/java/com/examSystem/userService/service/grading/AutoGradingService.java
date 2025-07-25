package com.examSystem.userService.service.grading;

import com.examSystem.userService.entity.Answer;
import com.examSystem.userService.entity.Question;
import com.examSystem.userService.entity.ExamAttempt;
import com.examSystem.userService.entity.Exam;
import com.examSystem.userService.repository.AnswerRepository;
import com.examSystem.userService.repository.QuestionRepository;
import com.examSystem.userService.repository.ExamAttemptRepository;
import com.examSystem.userService.repository.ExamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * 自动判卷服务
 * 
 * 提供多种题型的自动评分功能：
 * - 单选题自动判分
 * - 多选题自动判分
 * - 判断题自动判分
 * - 填空题智能匹配
 * - 主观题标记待人工评分
 */
@Service
@Transactional
public class AutoGradingService {

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private ExamRepository examRepository;

    /**
     * 自动为考试尝试评分
     */
    public GradingResult gradeExamAttempt(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("考试记录不存在"));

        Exam exam = examRepository.findById(attempt.getExamId())
            .orElseThrow(() -> new RuntimeException("考试不存在"));

        List<Answer> answers = answerRepository.findByAttemptId(attemptId);
        List<Question> questions = questionRepository.findByExamIdOrderByOrderNumber(attempt.getExamId());

        GradingResult result = performGrading(answers, questions, exam);

        // 更新考试尝试记录
        attempt.setScore(result.getTotalScore());
        attempt.setGradedAt(LocalDateTime.now());
        attempt.setStatus("GRADED");
        examAttemptRepository.save(attempt);

        // 更新答案记录
        for (QuestionGradingResult questionResult : result.getQuestionResults()) {
            Answer answer = answers.stream()
                .filter(a -> a.getQuestionId().equals(questionResult.getQuestionId()))
                .findFirst()
                .orElse(null);
            
            if (answer != null) {
                answer.setIsCorrect(questionResult.isCorrect());
                answer.setScore(questionResult.getScore());
                answer.setGradingComment(questionResult.getComment());
                answerRepository.save(answer);
            }
        }

        return result;
    }

    /**
     * 异步批量评分
     */
    @Async
    public CompletableFuture<List<GradingResult>> gradeMultipleAttempts(List<Long> attemptIds) {
        List<GradingResult> results = new java.util.ArrayList<>();
        
        for (Long attemptId : attemptIds) {
            try {
                GradingResult result = gradeExamAttempt(attemptId);
                results.add(result);
            } catch (Exception e) {
                results.add(new GradingResult(attemptId, BigDecimal.ZERO, 0, 0, false, 
                    new java.util.ArrayList<>(), "评分失败: " + e.getMessage()));
            }
        }
        
        return CompletableFuture.completedFuture(results);
    }

    /**
     * 单个题目评分
     */
    public QuestionGradingResult gradeQuestion(Question question, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            return new QuestionGradingResult(
                question.getId(),
                false,
                BigDecimal.ZERO,
                "未作答"
            );
        }

        switch (question.getType()) {
            case "SINGLE_CHOICE":
                return gradeSingleChoice(question, studentAnswer);
            case "MULTIPLE_CHOICE":
                return gradeMultipleChoice(question, studentAnswer);
            case "TRUE_FALSE":
                return gradeTrueFalse(question, studentAnswer);
            case "FILL_BLANK":
                return gradeFillBlank(question, studentAnswer);
            case "ESSAY":
                return gradeEssay(question, studentAnswer);
            default:
                return new QuestionGradingResult(
                    question.getId(),
                    false,
                    BigDecimal.ZERO,
                    "不支持的题型"
                );
        }
    }

    /**
     * 重新评分（用于调整评分标准后）
     */
    public GradingResult regradeExamAttempt(Long attemptId) {
        // 重置所有答案的评分状态
        List<Answer> answers = answerRepository.findByAttemptId(attemptId);
        for (Answer answer : answers) {
            answer.setIsCorrect(null);
            answer.setScore(null);
            answer.setGradingComment(null);
            answerRepository.save(answer);
        }

        // 重新评分
        return gradeExamAttempt(attemptId);
    }

    /**
     * 获取需要人工评分的答案
     */
    @Transactional(readOnly = true)
    public List<Answer> getAnswersNeedingManualGrading(Long examId) {
        return answerRepository.findAnswersNeedingManualGrading(examId);
    }

    // 私有方法：具体评分逻辑

    private GradingResult performGrading(List<Answer> answers, List<Question> questions, Exam exam) {
        List<QuestionGradingResult> questionResults = new java.util.ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxPossibleScore = BigDecimal.ZERO;
        int correctCount = 0;

        Map<Long, Answer> answerMap = new HashMap<>();
        for (Answer answer : answers) {
            answerMap.put(answer.getQuestionId(), answer);
        }

        for (Question question : questions) {
            maxPossibleScore = maxPossibleScore.add(question.getPoints());
            
            Answer answer = answerMap.get(question.getId());
            String studentAnswer = answer != null ? answer.getStudentAnswer() : null;
            
            QuestionGradingResult questionResult = gradeQuestion(question, studentAnswer);
            questionResults.add(questionResult);
            
            totalScore = totalScore.add(questionResult.getScore());
            if (questionResult.isCorrect()) {
                correctCount++;
            }
        }

        // 计算百分比分数
        BigDecimal percentageScore = maxPossibleScore.compareTo(BigDecimal.ZERO) > 0 
            ? totalScore.divide(maxPossibleScore, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

        boolean passed = percentageScore.compareTo(exam.getPassingScore()) >= 0;

        return new GradingResult(
            null, // attemptId will be set by caller
            percentageScore,
            correctCount,
            questions.size(),
            passed,
            questionResults,
            passed ? "考试通过" : "考试未通过"
        );
    }

    private QuestionGradingResult gradeSingleChoice(Question question, String studentAnswer) {
        String correctAnswer = question.getCorrectAnswer();
        boolean isCorrect = correctAnswer != null && 
            correctAnswer.trim().equalsIgnoreCase(studentAnswer.trim());
        
        BigDecimal score = isCorrect ? question.getPoints() : BigDecimal.ZERO;
        String comment = isCorrect ? "正确" : "错误，正确答案是: " + correctAnswer;
        
        return new QuestionGradingResult(question.getId(), isCorrect, score, comment);
    }

    private QuestionGradingResult gradeMultipleChoice(Question question, String studentAnswer) {
        String correctAnswer = question.getCorrectAnswer();
        
        if (correctAnswer == null || studentAnswer == null) {
            return new QuestionGradingResult(question.getId(), false, BigDecimal.ZERO, "答案不完整");
        }

        // 解析多选答案（假设用逗号分隔）
        Set<String> correctOptions = new HashSet<>(Arrays.asList(correctAnswer.split(",")));
        Set<String> studentOptions = new HashSet<>(Arrays.asList(studentAnswer.split(",")));

        // 去除空格
        correctOptions = cleanOptions(correctOptions);
        studentOptions = cleanOptions(studentOptions);

        boolean isCorrect = correctOptions.equals(studentOptions);
        
        BigDecimal score;
        String comment;
        
        if (isCorrect) {
            score = question.getPoints();
            comment = "完全正确";
        } else {
            // 部分分数计算
            Set<String> intersection = new HashSet<>(correctOptions);
            intersection.retainAll(studentOptions);
            
            Set<String> union = new HashSet<>(correctOptions);
            union.addAll(studentOptions);
            
            if (intersection.isEmpty()) {
                score = BigDecimal.ZERO;
                comment = "完全错误";
            } else {
                double partialRatio = (double) intersection.size() / union.size();
                score = question.getPoints().multiply(BigDecimal.valueOf(partialRatio));
                comment = String.format("部分正确，得分比例: %.2f", partialRatio);
            }
        }
        
        return new QuestionGradingResult(question.getId(), isCorrect, score, comment);
    }

    private QuestionGradingResult gradeTrueFalse(Question question, String studentAnswer) {
        String correctAnswer = question.getCorrectAnswer();
        
        // 标准化答案格式
        String normalizedCorrect = normalizeBoolean(correctAnswer);
        String normalizedStudent = normalizeBoolean(studentAnswer);
        
        boolean isCorrect = normalizedCorrect != null && normalizedCorrect.equals(normalizedStudent);
        BigDecimal score = isCorrect ? question.getPoints() : BigDecimal.ZERO;
        String comment = isCorrect ? "正确" : "错误，正确答案是: " + (normalizedCorrect.equals("true") ? "正确" : "错误");
        
        return new QuestionGradingResult(question.getId(), isCorrect, score, comment);
    }

    private QuestionGradingResult gradeFillBlank(Question question, String studentAnswer) {
        String correctAnswer = question.getCorrectAnswer();
        
        if (correctAnswer == null || studentAnswer == null) {
            return new QuestionGradingResult(question.getId(), false, BigDecimal.ZERO, "答案不完整");
        }

        // 支持多种匹配模式
        boolean isCorrect = false;
        String comment = "";

        // 1. 精确匹配
        if (correctAnswer.trim().equalsIgnoreCase(studentAnswer.trim())) {
            isCorrect = true;
            comment = "完全匹配";
        }
        // 2. 多个可能答案（用分号分隔）
        else if (correctAnswer.contains(";")) {
            String[] possibleAnswers = correctAnswer.split(";");
            for (String possible : possibleAnswers) {
                if (possible.trim().equalsIgnoreCase(studentAnswer.trim())) {
                    isCorrect = true;
                    comment = "匹配可选答案";
                    break;
                }
            }
        }
        // 3. 数值答案的容差匹配
        else if (isNumeric(correctAnswer) && isNumeric(studentAnswer)) {
            double correct = Double.parseDouble(correctAnswer.trim());
            double student = Double.parseDouble(studentAnswer.trim());
            double tolerance = Math.abs(correct) * 0.01; // 1% 容差
            
            if (Math.abs(correct - student) <= tolerance) {
                isCorrect = true;
                comment = "数值匹配（容差范围内）";
            } else {
                comment = "数值不匹配，正确答案: " + correctAnswer;
            }
        }
        // 4. 模糊匹配（去除标点符号和多余空格）
        else {
            String cleanCorrect = cleanText(correctAnswer);
            String cleanStudent = cleanText(studentAnswer);
            
            if (cleanCorrect.equalsIgnoreCase(cleanStudent)) {
                isCorrect = true;
                comment = "文本匹配";
            } else {
                comment = "不匹配，正确答案: " + correctAnswer;
            }
        }

        BigDecimal score = isCorrect ? question.getPoints() : BigDecimal.ZERO;
        return new QuestionGradingResult(question.getId(), isCorrect, score, comment);
    }

    private QuestionGradingResult gradeEssay(Question question, String studentAnswer) {
        // 主观题标记为需要人工评分
        return new QuestionGradingResult(
            question.getId(),
            false, // 暂时标记为未评分
            BigDecimal.ZERO, // 暂时给0分
            "主观题，需要人工评分"
        );
    }

    // 辅助方法

    private Set<String> cleanOptions(Set<String> options) {
        Set<String> cleaned = new HashSet<>();
        for (String option : options) {
            if (option != null && !option.trim().isEmpty()) {
                cleaned.add(option.trim().toUpperCase());
            }
        }
        return cleaned;
    }

    private String normalizeBoolean(String answer) {
        if (answer == null) return null;
        
        String normalized = answer.trim().toLowerCase();
        switch (normalized) {
            case "true":
            case "正确":
            case "对":
            case "是":
            case "t":
            case "1":
                return "true";
            case "false":
            case "错误":
            case "错":
            case "否":
            case "f":
            case "0":
                return "false";
            default:
                return null;
        }
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String cleanText(String text) {
        if (text == null) return "";
        
        // 去除标点符号和多余空格
        return text.replaceAll("[\\p{Punct}\\s]+", " ").trim();
    }

    // 内部类定义

    public static class GradingResult {
        private final Long attemptId;
        private final BigDecimal totalScore;
        private final int correctCount;
        private final int totalQuestions;
        private final boolean passed;
        private final List<QuestionGradingResult> questionResults;
        private final String comment;

        public GradingResult(Long attemptId, BigDecimal totalScore, int correctCount,
                           int totalQuestions, boolean passed,
                           List<QuestionGradingResult> questionResults, String comment) {
            this.attemptId = attemptId;
            this.totalScore = totalScore;
            this.correctCount = correctCount;
            this.totalQuestions = totalQuestions;
            this.passed = passed;
            this.questionResults = questionResults;
            this.comment = comment;
        }

        // Getters
        public Long getAttemptId() { return attemptId; }
        public BigDecimal getTotalScore() { return totalScore; }
        public int getCorrectCount() { return correctCount; }
        public int getTotalQuestions() { return totalQuestions; }
        public boolean isPassed() { return passed; }
        public List<QuestionGradingResult> getQuestionResults() { return questionResults; }
        public String getComment() { return comment; }
    }

    public static class QuestionGradingResult {
        private final Long questionId;
        private final boolean correct;
        private final BigDecimal score;
        private final String comment;

        public QuestionGradingResult(Long questionId, boolean correct, BigDecimal score, String comment) {
            this.questionId = questionId;
            this.correct = correct;
            this.score = score;
            this.comment = comment;
        }

        // Getters
        public Long getQuestionId() { return questionId; }
        public boolean isCorrect() { return correct; }
        public BigDecimal getScore() { return score; }
        public String getComment() { return comment; }
    }
}