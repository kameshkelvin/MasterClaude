package com.examSystem.userService.repository;

import com.examSystem.userService.entity.ExamQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 考试题目关联数据访问接口
 * 
 * 基于数据库设计文档中的exam_questions表结构
 * 管理考试和题目的多对多关系
 */
@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {

    /**
     * 根据考试ID查找所有题目
     */
    List<ExamQuestion> findByExamIdOrderByQuestionOrder(Long examId);

    /**
     * 根据考试ID分页查找题目
     */
    Page<ExamQuestion> findByExamIdOrderByQuestionOrder(Long examId, Pageable pageable);

    /**
     * 根据题目ID查找所有使用该题目的考试
     */
    List<ExamQuestion> findByQuestionId(Long questionId);

    /**
     * 查找考试中的特定题目
     */
    Optional<ExamQuestion> findByExamIdAndQuestionId(Long examId, Long questionId);

    /**
     * 根据考试ID和章节查找题目
     */
    List<ExamQuestion> findByExamIdAndSectionIdOrderByQuestionOrder(Long examId, Long sectionId);

    /**
     * 根据考试ID和章节名称查找题目
     */
    List<ExamQuestion> findByExamIdAndSectionNameOrderByQuestionOrder(Long examId, String sectionName);

    /**
     * 查找考试中的必答题
     */
    @Query("SELECT eq FROM ExamQuestion eq WHERE eq.examId = :examId AND eq.isRequired = true " +
           "ORDER BY eq.questionOrder")
    List<ExamQuestion> findRequiredQuestionsByExam(@Param("examId") Long examId);

    /**
     * 查找考试中的选答题
     */
    @Query("SELECT eq FROM ExamQuestion eq WHERE eq.examId = :examId AND eq.isRequired = false " +
           "ORDER BY eq.questionOrder")
    List<ExamQuestion> findOptionalQuestionsByExam(@Param("examId") Long examId);

    /**
     * 统计考试题目总数
     */
    @Query("SELECT COUNT(eq) FROM ExamQuestion eq WHERE eq.examId = :examId")
    int countByExamId(@Param("examId") Long examId);

    /**
     * 计算考试总分
     */
    @Query("SELECT SUM(eq.points) FROM ExamQuestion eq WHERE eq.examId = :examId")
    BigDecimal sumPointsByExamId(@Param("examId") Long examId);

    /**
     * 计算考试章节总分
     */
    @Query("SELECT SUM(eq.points) FROM ExamQuestion eq WHERE eq.examId = :examId AND eq.sectionId = :sectionId")
    BigDecimal sumPointsByExamIdAndSectionId(@Param("examId") Long examId, @Param("sectionId") Long sectionId);

    /**
     * 获取考试的章节列表
     */
    @Query("SELECT DISTINCT eq.sectionId, eq.sectionName FROM ExamQuestion eq " +
           "WHERE eq.examId = :examId AND eq.sectionId IS NOT NULL " +
           "ORDER BY eq.sectionId")
    List<Object[]> findSectionsByExamId(@Param("examId") Long examId);

    /**
     * 统计每个章节的题目数量
     */
    @Query("SELECT eq.sectionName, COUNT(eq) FROM ExamQuestion eq " +
           "WHERE eq.examId = :examId AND eq.sectionName IS NOT NULL " +
           "GROUP BY eq.sectionName ORDER BY eq.sectionName")
    List<Object[]> countQuestionsBySection(@Param("examId") Long examId);

    /**
     * 查找考试中最大的题目顺序号
     */
    @Query("SELECT MAX(eq.questionOrder) FROM ExamQuestion eq WHERE eq.examId = :examId")
    Integer findMaxQuestionOrderByExamId(@Param("examId") Long examId);

    /**
     * 查找指定顺序的题目
     */
    Optional<ExamQuestion> findByExamIdAndQuestionOrder(Long examId, Integer questionOrder);

    /**
     * 检查考试中是否存在指定题目
     */
    boolean existsByExamIdAndQuestionId(Long examId, Long questionId);

    /**
     * 删除考试中的所有题目
     */
    @Modifying
    @Query("DELETE FROM ExamQuestion eq WHERE eq.examId = :examId")
    void deleteByExamId(@Param("examId") Long examId);

    /**
     * 删除指定题目在所有考试中的关联
     */
    @Modifying
    @Query("DELETE FROM ExamQuestion eq WHERE eq.questionId = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);

    /**
     * 批量删除考试题目
     */
    @Modifying
    @Query("DELETE FROM ExamQuestion eq WHERE eq.examId = :examId AND eq.questionId IN :questionIds")
    void batchDeleteByExamIdAndQuestionIds(@Param("examId") Long examId, @Param("questionIds") List<Long> questionIds);

    /**
     * 更新题目分值
     */
    @Modifying
    @Query("UPDATE ExamQuestion eq SET eq.points = :points WHERE eq.examId = :examId AND eq.questionId = :questionId")
    void updatePoints(@Param("examId") Long examId, @Param("questionId") Long questionId, @Param("points") BigDecimal points);

    /**
     * 更新题目顺序
     */
    @Modifying
    @Query("UPDATE ExamQuestion eq SET eq.questionOrder = :questionOrder " +
           "WHERE eq.examId = :examId AND eq.questionId = :questionId")
    void updateQuestionOrder(@Param("examId") Long examId, @Param("questionId") Long questionId, 
                           @Param("questionOrder") Integer questionOrder);

    /**
     * 批量更新题目顺序
     */
    @Modifying
    @Query("UPDATE ExamQuestion eq SET eq.questionOrder = eq.questionOrder + :increment " +
           "WHERE eq.examId = :examId AND eq.questionOrder >= :startOrder")
    void batchUpdateQuestionOrderFrom(@Param("examId") Long examId, @Param("startOrder") Integer startOrder, 
                                    @Param("increment") Integer increment);

    /**
     * 更新章节信息
     */
    @Modifying
    @Query("UPDATE ExamQuestion eq SET eq.sectionId = :sectionId, eq.sectionName = :sectionName " +
           "WHERE eq.examId = :examId AND eq.questionId = :questionId")
    void updateSection(@Param("examId") Long examId, @Param("questionId") Long questionId,
                     @Param("sectionId") Long sectionId, @Param("sectionName") String sectionName);

    /**
     * 更新题目时间限制
     */
    @Modifying
    @Query("UPDATE ExamQuestion eq SET eq.timeLimitSeconds = :timeLimitSeconds " +
           "WHERE eq.examId = :examId AND eq.questionId = :questionId")
    void updateTimeLimit(@Param("examId") Long examId, @Param("questionId") Long questionId,
                       @Param("timeLimitSeconds") Integer timeLimitSeconds);

    /**
     * 批量更新章节内所有题目的信息
     */
    @Modifying
    @Query("UPDATE ExamQuestion eq SET eq.sectionName = :newSectionName " +
           "WHERE eq.examId = :examId AND eq.sectionName = :oldSectionName")
    void updateSectionName(@Param("examId") Long examId, @Param("oldSectionName") String oldSectionName,
                         @Param("newSectionName") String newSectionName);

    /**
     * 获取考试题目的详细统计信息
     */
    @Query("SELECT " +
           "COUNT(eq) as totalQuestions, " +
           "SUM(eq.points) as totalPoints, " +
           "COUNT(DISTINCT eq.sectionName) as sectionCount, " +
           "AVG(eq.points) as avgPoints " +
           "FROM ExamQuestion eq WHERE eq.examId = :examId")
    List<Object[]> getExamQuestionStatistics(@Param("examId") Long examId);

    /**
     * 获取题目在不同考试中的使用情况
     */
    @Query("SELECT eq.examId, COUNT(eq) FROM ExamQuestion eq WHERE eq.questionId = :questionId GROUP BY eq.examId")
    List<Object[]> getQuestionUsageInExams(@Param("questionId") Long questionId);

    /**
     * 查找有时间限制的题目
     */
    @Query("SELECT eq FROM ExamQuestion eq WHERE eq.examId = :examId AND eq.timeLimitSeconds IS NOT NULL " +
           "ORDER BY eq.questionOrder")
    List<ExamQuestion> findTimedQuestionsByExam(@Param("examId") Long examId);

    /**
     * 统计考试中各分值的题目数量
     */
    @Query("SELECT eq.points, COUNT(eq) FROM ExamQuestion eq WHERE eq.examId = :examId " +
           "GROUP BY eq.points ORDER BY eq.points")
    List<Object[]> countQuestionsByPoints(@Param("examId") Long examId);

    /**
     * 查找考试中分值最高的题目
     */
    @Query("SELECT eq FROM ExamQuestion eq WHERE eq.examId = :examId AND " +
           "eq.points = (SELECT MAX(eq2.points) FROM ExamQuestion eq2 WHERE eq2.examId = :examId)")
    List<ExamQuestion> findHighestPointQuestions(@Param("examId") Long examId);

    /**
     * 查找考试中分值最低的题目
     */
    @Query("SELECT eq FROM ExamQuestion eq WHERE eq.examId = :examId AND " +
           "eq.points = (SELECT MIN(eq2.points) FROM ExamQuestion eq2 WHERE eq2.examId = :examId)")
    List<ExamQuestion> findLowestPointQuestions(@Param("examId") Long examId);

    /**
     * 验证考试题目顺序的连续性
     */
    @Query("SELECT COUNT(eq) FROM ExamQuestion eq WHERE eq.examId = :examId AND " +
           "eq.questionOrder BETWEEN 1 AND (SELECT COUNT(eq2) FROM ExamQuestion eq2 WHERE eq2.examId = :examId)")
    int validateQuestionOrderContinuity(@Param("examId") Long examId);

    /**
     * 重新排序考试题目
     */
    @Modifying
    @Query("UPDATE ExamQuestion eq SET eq.questionOrder = " +
           "(SELECT ROW_NUMBER() OVER (ORDER BY eq2.questionOrder) FROM ExamQuestion eq2 " +
           "WHERE eq2.examId = :examId AND eq2.id = eq.id) " +
           "WHERE eq.examId = :examId")
    void reorderQuestions(@Param("examId") Long examId);

    /**
     * 复制考试题目到另一个考试
     */
    @Query("SELECT eq FROM ExamQuestion eq WHERE eq.examId = :sourceExamId ORDER BY eq.questionOrder")
    List<ExamQuestion> findQuestionsForCopy(@Param("sourceExamId") Long sourceExamId);
}