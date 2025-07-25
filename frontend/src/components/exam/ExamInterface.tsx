import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Clock,
  AlertTriangle,
  CheckCircle,
  Circle,
  Flag,
  ChevronLeft,
  ChevronRight,
  Save,
  Send,
  Eye,
  EyeOff,
  Maximize,
  Minimize,
  AlertCircle,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { QuestionRenderer } from './QuestionRenderer';
import { ExamTimer } from './ExamTimer';
import { ProgressBar } from './ProgressBar';
import { ExamSidebar } from './ExamSidebar';
import { examApi } from '@/lib/api';
import { Question, ExamSession, ExamProgress } from '@/types';
import { cn, formatTimeRemaining } from '@/lib/utils';

interface ExamInterfaceProps {
  className?: string;
}

export const ExamInterface: React.FC<ExamInterfaceProps> = ({ className }) => {
  const { examId } = useParams<{ examId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  
  // Component state
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [answers, setAnswers] = useState<Map<string, string>>(new Map());
  const [markedForReview, setMarkedForReview] = useState<Set<string>>(new Set());
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [lastSaveTime, setLastSaveTime] = useState<Date>(new Date());
  const [attemptId, setAttemptId] = useState<string>('');
  const [showSubmitConfirm, setShowSubmitConfirm] = useState(false);
  const [violations, setViolations] = useState<any[]>([]);
  
  // Refs
  const autoSaveInterval = useRef<NodeJS.Timeout>();
  const examContainer = useRef<HTMLDivElement>(null);

  // Fetch exam session
  const { data: session, isLoading: sessionLoading } = useQuery({
    queryKey: ['examSession', examId],
    queryFn: () => examApi.startExam(examId!),
    select: (response) => response.data.data,
    enabled: !!examId,
    onSuccess: (data) => {
      setAttemptId(data.attemptId);
    },
  });

  // Fetch questions
  const { data: questions = [], isLoading: questionsLoading } = useQuery({
    queryKey: ['examQuestions', examId, attemptId],
    queryFn: () => examApi.getExamQuestions(examId!, attemptId),
    select: (response) => response.data.data,
    enabled: !!examId && !!attemptId,
  });

  // Fetch progress
  const { data: progress } = useQuery({
    queryKey: ['examProgress', examId, attemptId],
    queryFn: () => examApi.getExamProgress(examId!, attemptId),
    select: (response) => response.data.data,
    enabled: !!examId && !!attemptId,
    refetchInterval: 30000, // Refetch every 30 seconds
  });

  // Submit answer mutation
  const submitAnswerMutation = useMutation({
    mutationFn: ({ questionId, answer }: { questionId: string; answer: string }) =>
      examApi.submitAnswer(examId!, questionId, { attemptId, studentAnswer: answer }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['examProgress', examId, attemptId] });
      setLastSaveTime(new Date());
    },
  });

  // Batch submit answers mutation
  const batchSubmitMutation = useMutation({
    mutationFn: (answersData: { questionId: string; studentAnswer: string }[]) =>
      examApi.submitAnswersBatch(examId!, { attemptId, answers: answersData }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['examProgress', examId, attemptId] });
      setLastSaveTime(new Date());
    },
  });

  // Finish exam mutation
  const finishExamMutation = useMutation({
    mutationFn: () => examApi.finishExam(examId!, { attemptId }),
    onSuccess: () => {
      navigate(`/exam/${examId}/result?attemptId=${attemptId}`);
    },
  });

  // Current question
  const currentQuestion = questions[currentQuestionIndex];

  // Auto-save functionality
  const autoSave = useCallback(() => {
    const pendingAnswers = Array.from(answers.entries())
      .filter(([questionId, answer]) => answer.trim() !== '')
      .map(([questionId, answer]) => ({ questionId, studentAnswer: answer }));

    if (pendingAnswers.length > 0) {
      batchSubmitMutation.mutate(pendingAnswers);
    }
  }, [answers, batchSubmitMutation]);

  // Setup auto-save
  useEffect(() => {
    if (attemptId && answers.size > 0) {
      if (autoSaveInterval.current) {
        clearInterval(autoSaveInterval.current);
      }
      autoSaveInterval.current = setInterval(autoSave, 30000); // Auto-save every 30 seconds
    }
    
    return () => {
      if (autoSaveInterval.current) {
        clearInterval(autoSaveInterval.current);
      }
    };
  }, [autoSave, attemptId, answers.size]);

  // Handle answer changes
  const handleAnswerChange = (questionId: string, answer: string) => {
    setAnswers(prev => new Map(prev.set(questionId, answer)));
  };

  // Handle question navigation
  const goToQuestion = (index: number) => {
    if (index >= 0 && index < questions.length) {
      setCurrentQuestionIndex(index);
    }
  };

  const goToPrevious = () => {
    goToQuestion(currentQuestionIndex - 1);
  };

  const goToNext = () => {
    goToQuestion(currentQuestionIndex + 1);
  };

  // Handle mark for review
  const toggleMarkForReview = (questionId: string) => {
    setMarkedForReview(prev => {
      const newSet = new Set(prev);
      if (newSet.has(questionId)) {
        newSet.delete(questionId);
      } else {
        newSet.add(questionId);
      }
      return newSet;
    });
  };

  // Handle fullscreen
  const toggleFullscreen = () => {
    if (!isFullscreen) {
      examContainer.current?.requestFullscreen();
      setIsFullscreen(true);
    } else {
      document.exitFullscreen();
      setIsFullscreen(false);
    }
  };

  // Handle exam submission
  const handleSubmitExam = () => {
    autoSave(); // Save any pending answers
    setTimeout(() => {
      finishExamMutation.mutate();
    }, 1000);
  };

  // Security monitoring
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.hidden) {
        setViolations(prev => [...prev, {
          type: 'TAB_SWITCH',
          timestamp: new Date(),
          description: '切换到其他标签页',
        }]);
      }
    };

    const handleKeyDown = (e: KeyboardEvent) => {
      // Disable F12, Ctrl+Shift+I, etc.
      if (
        e.key === 'F12' ||
        (e.ctrlKey && e.shiftKey && e.key === 'I') ||
        (e.ctrlKey && e.shiftKey && e.key === 'C') ||
        (e.ctrlKey && e.key === 'u')
      ) {
        e.preventDefault();
        setViolations(prev => [...prev, {
          type: 'SUSPICIOUS_KEY',
          timestamp: new Date(),
          description: '尝试打开开发者工具',
        }]);
      }
    };

    const handleContextMenu = (e: Event) => {
      e.preventDefault();
      setViolations(prev => [...prev, {
        type: 'RIGHT_CLICK',
        timestamp: new Date(),
        description: '右键点击',
      }]);
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    document.addEventListener('keydown', handleKeyDown);
    document.addEventListener('contextmenu', handleContextMenu);

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      document.removeEventListener('keydown', handleKeyDown);
      document.removeEventListener('contextmenu', handleContextMenu);
    };
  }, []);

  // Handle time warnings
  const handleTimeWarning = (remainingMinutes: number) => {
    if (remainingMinutes <= 5) {
      // Show urgent warning
      alert(`考试时间仅剩 ${remainingMinutes} 分钟，请抓紧时间！`);
    } else if (remainingMinutes <= 15) {
      // Show warning notification
      console.log(`考试时间还剩 ${remainingMinutes} 分钟`);
    }
  };

  // Auto-submit when time expires
  const handleTimeExpired = () => {
    autoSave();
    setTimeout(() => {
      finishExamMutation.mutate();
    }, 1000);
  };

  if (sessionLoading || questionsLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-exam-500 mx-auto mb-4"></div>
          <p className="text-gray-600">正在加载考试...</p>
        </div>
      </div>
    );
  }

  if (!session || !currentQuestion) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <AlertTriangle className="h-12 w-12 text-yellow-500 mx-auto mb-4" />
          <p className="text-gray-600">考试加载失败</p>
          <Button onClick={() => navigate('/dashboard')} className="mt-4">
            返回首页
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div 
      ref={examContainer}
      className={cn('min-h-screen bg-gray-50 exam-mobile', className)}
    >
      {/* Mobile Header */}
      <div className="lg:hidden bg-white border-b border-gray-200 px-4 py-3 sticky top-0 z-40 safe-area-top">
        <div className="flex items-center justify-between mb-3">
          <h1 className="text-lg font-semibold text-gray-900 truncate flex-1 pr-4">
            {session.examTitle}
          </h1>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setSidebarOpen(true)}
            className="touch-target"
          >
            <Eye className="h-4 w-4" />
          </Button>
        </div>
        
        <div className="flex items-center justify-between text-sm mb-3">
          <div className="flex items-center gap-3">
            <Badge variant="exam" className="text-xs">
              {currentQuestionIndex + 1} / {questions.length}
            </Badge>
            <span className="text-gray-500">
              {Math.round(((currentQuestionIndex + 1) / questions.length) * 100)}% 完成
            </span>
          </div>
          
          <ExamTimer
            endTime={session.endTime}
            onTimeWarning={handleTimeWarning}
            onTimeExpired={handleTimeExpired}
          />
        </div>
        
        {/* Mobile Progress bar */}
        <div>
          <ProgressBar
            current={progress?.answeredQuestions || 0}
            total={questions.length}
            markedForReview={markedForReview.size}
          />
        </div>
      </div>

      {/* Desktop Header */}
      <div className="hidden lg:block bg-white border-b border-gray-200 px-4 py-3 sticky top-0 z-40">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <h1 className="text-lg font-semibold text-gray-900">
              {session.examTitle}
            </h1>
            <Badge variant="exam">
              {currentQuestionIndex + 1} / {questions.length}
            </Badge>
          </div>
          
          <div className="flex items-center gap-4">
            {/* Auto-save indicator */}
            <div className="flex items-center gap-2 text-sm text-gray-500">
              <Save className="h-4 w-4" />
              <span>上次保存：{formatTimeRemaining(Math.floor((Date.now() - lastSaveTime.getTime()) / 1000))}前</span>
            </div>
            
            {/* Timer */}
            <ExamTimer
              endTime={session.endTime}
              onTimeWarning={handleTimeWarning}
              onTimeExpired={handleTimeExpired}
            />
            
            {/* Fullscreen toggle */}
            <Button
              variant="outline"
              size="sm"
              onClick={toggleFullscreen}
            >
              {isFullscreen ? (
                <Minimize className="h-4 w-4" />
              ) : (
                <Maximize className="h-4 w-4" />
              )}
            </Button>
            
            {/* Sidebar toggle */}
            <Button
              variant="outline"
              size="sm"
              onClick={() => setSidebarOpen(!sidebarOpen)}
            >
              <Eye className="h-4 w-4 mr-2" />
              题目导航
            </Button>
          </div>
        </div>
        
        {/* Progress bar */}
        <div className="mt-3">
          <ProgressBar
            current={progress?.answeredQuestions || 0}
            total={questions.length}
            markedForReview={markedForReview.size}
          />
        </div>
      </div>

      <div className="lg:flex">
        {/* Main content */}
        <div className="flex-1 p-4 sm:p-6">
          <div className="max-w-4xl mx-auto spacing-responsive">
            {/* Security violations */}
            {violations.length > 0 && (
              <Card className="border-yellow-200 bg-yellow-50">
                <CardContent className="p-4">
                  <div className="flex items-center gap-2 text-yellow-800">
                    <AlertCircle className="h-5 w-5" />
                    <span className="font-medium">安全提醒</span>
                  </div>
                  <p className="text-sm text-yellow-700 mt-1">
                    检测到 {violations.length} 次违规行为，请遵守考试规则
                  </p>
                </CardContent>
              </Card>
            )}

            {/* Question */}
            <Card className="exam-question-mobile">
              <CardHeader className="pb-3">
                <CardTitle className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <span className="text-responsive-lg">第 {currentQuestionIndex + 1} 题</span>
                  <div className="flex items-center gap-2">
                    <Badge variant={getDifficultyVariant(currentQuestion.difficulty)} className="text-xs">
                      {getDifficultyLabel(currentQuestion.difficulty)}
                    </Badge>
                    <Badge variant="outline" className="text-xs">
                      {currentQuestion.points} 分
                    </Badge>
                  </div>
                </CardTitle>
              </CardHeader>
              <CardContent className="p-4 sm:p-6">
                <QuestionRenderer
                  question={currentQuestion}
                  answer={answers.get(currentQuestion.id) || ''}
                  onChange={(answer) => handleAnswerChange(currentQuestion.id, answer)}
                  isMarkedForReview={markedForReview.has(currentQuestion.id)}
                  onToggleMarkForReview={() => toggleMarkForReview(currentQuestion.id)}
                />
              </CardContent>
            </Card>

            {/* Mobile Navigation */}
            <div className="lg:hidden space-y-4">
              {/* Action buttons */}
              <div className="grid grid-cols-2 gap-3">
                <Button
                  variant="outline"
                  onClick={() => toggleMarkForReview(currentQuestion.id)}
                  className="btn-mobile justify-center"
                >
                  <Flag className={cn(
                    'h-4 w-4 mr-2',
                    markedForReview.has(currentQuestion.id) ? 'text-yellow-500 fill-current' : ''
                  )} />
                  <span className="text-sm">
                    {markedForReview.has(currentQuestion.id) ? '取消标记' : '标记复查'}
                  </span>
                </Button>
                
                <Button
                  variant="outline"
                  onClick={autoSave}
                  disabled={batchSubmitMutation.isPending}
                  className="btn-mobile justify-center"
                >
                  <Save className="h-4 w-4 mr-2" />
                  <span className="text-sm">保存答案</span>
                </Button>
              </div>
              
              {/* Navigation buttons */}
              <div className="flex items-center justify-between gap-4">
                <Button
                  variant="outline"
                  onClick={goToPrevious}
                  disabled={currentQuestionIndex === 0}
                  className="flex-1 btn-mobile"
                >
                  <ChevronLeft className="h-4 w-4 mr-2" />
                  上一题
                </Button>
                
                {currentQuestionIndex === questions.length - 1 ? (
                  <Button
                    onClick={() => setShowSubmitConfirm(true)}
                    className="flex-1 btn-mobile"
                    variant="destructive"
                  >
                    <Send className="h-4 w-4 mr-2" />
                    提交考试
                  </Button>
                ) : (
                  <Button
                    onClick={goToNext}
                    className="flex-1 btn-mobile"
                  >
                    下一题
                    <ChevronRight className="h-4 w-4 ml-2" />
                  </Button>
                )}
              </div>
            </div>

            {/* Desktop Navigation */}
            <div className="hidden lg:flex items-center justify-between">
              <Button
                variant="outline"
                onClick={goToPrevious}
                disabled={currentQuestionIndex === 0}
              >
                <ChevronLeft className="h-4 w-4 mr-2" />
                上一题
              </Button>
              
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  onClick={() => toggleMarkForReview(currentQuestion.id)}
                >
                  <Flag className={cn(
                    'h-4 w-4 mr-2',
                    markedForReview.has(currentQuestion.id) ? 'text-yellow-500 fill-current' : ''
                  )} />
                  {markedForReview.has(currentQuestion.id) ? '取消标记' : '标记复查'}
                </Button>
                
                <Button
                  variant="outline"
                  onClick={autoSave}
                  disabled={batchSubmitMutation.isPending}
                >
                  <Save className="h-4 w-4 mr-2" />
                  保存答案
                </Button>
              </div>
              
              {currentQuestionIndex === questions.length - 1 ? (
                <Button
                  variant="destructive"
                  onClick={() => setShowSubmitConfirm(true)}
                >
                  <Send className="h-4 w-4 mr-2" />
                  提交考试
                </Button>
              ) : (
                <Button
                  variant="outline"
                  onClick={goToNext}
                >
                  下一题
                  <ChevronRight className="h-4 w-4 ml-2" />
                </Button>
              )}
            </div>
          </div>
        </div>

        {/* Sidebar */}
        <ExamSidebar
          isOpen={sidebarOpen}
          onClose={() => setSidebarOpen(false)}
          questions={questions}
          currentIndex={currentQuestionIndex}
          answers={answers}
          markedForReview={markedForReview}
          onQuestionSelect={goToQuestion}
          onSubmitExam={() => setShowSubmitConfirm(true)}
        />
      </div>

      {/* Submit confirmation modal */}
      {showSubmitConfirm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-full max-w-md">
            <CardHeader>
              <CardTitle>确认提交考试</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <p className="text-gray-600">
                  您确定要提交考试吗？提交后将无法再修改答案。
                </p>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="font-medium">已答题目：</span>
                    <span className="ml-2">{progress?.answeredQuestions || 0} / {questions.length}</span>
                  </div>
                  <div>
                    <span className="font-medium">标记复查：</span>
                    <span className="ml-2">{markedForReview.size}</span>
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    className="flex-1"
                    onClick={() => setShowSubmitConfirm(false)}
                  >
                    取消
                  </Button>
                  <Button
                    variant="destructive"
                    className="flex-1"
                    onClick={handleSubmitExam}
                    loading={finishExamMutation.isPending}
                  >
                    确认提交
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
};

// Helper functions
const getDifficultyVariant = (difficulty: string) => {
  switch (difficulty) {
    case 'EASY': return 'success';
    case 'MEDIUM': return 'warning';
    case 'HARD': return 'error';
    default: return 'secondary';
  }
};

const getDifficultyLabel = (difficulty: string) => {
  switch (difficulty) {
    case 'EASY': return '简单';
    case 'MEDIUM': return '中等';
    case 'HARD': return '困难';
    default: return '未知';
  }
};