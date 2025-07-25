import React, { useState } from 'react';
import { useParams, useSearchParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Trophy,
  Target,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  Download,
  Share2,
  RotateCcw,
  ArrowLeft,
  Calendar,
  Award,
  TrendingUp,
  Eye,
  EyeOff,
  Medal,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { QuestionRenderer } from './QuestionRenderer';
import { examApi } from '@/lib/api';
import { ExamResult, QuestionResult } from '@/types';
import { 
  cn, 
  formatDate, 
  formatDuration, 
  getGrade, 
  getColorFromScore,
  copyToClipboard 
} from '@/lib/utils';

interface ExamResultsProps {
  className?: string;
}

export const ExamResults: React.FC<ExamResultsProps> = ({ className }) => {
  const { examId } = useParams<{ examId: string }>();
  const [searchParams] = useSearchParams();
  const attemptId = searchParams.get('attemptId');
  
  const [showAnswers, setShowAnswers] = useState(false);
  const [selectedQuestionIndex, setSelectedQuestionIndex] = useState<number | null>(null);

  // Fetch exam result
  const { data: result, isLoading, error } = useQuery({
    queryKey: ['examResult', examId, attemptId],
    queryFn: () => examApi.getExamResult(examId!, attemptId!),
    select: (response) => response.data.data,
    enabled: !!examId && !!attemptId,
  });

  const handleShare = async () => {
    const shareText = `我在「${result?.examTitle}」考试中取得了 ${result?.score}% 的成绩！`;
    
    if (navigator.share) {
      try {
        await navigator.share({
          title: '考试成绩分享',
          text: shareText,
          url: window.location.href,
        });
      } catch (error) {
        console.log('分享失败', error);
      }
    } else {
      await copyToClipboard(shareText + ' ' + window.location.href);
      alert('分享链接已复制到剪贴板');
    }
  };

  const handleDownloadCertificate = () => {
    if (result?.certificate) {
      // 这里应该调用下载证书的API
      console.log('下载证书');
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-exam-500 mx-auto mb-4"></div>
          <p className="text-gray-600">正在加载成绩...</p>
        </div>
      </div>
    );
  }

  if (error || !result) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <AlertCircle className="h-12 w-12 text-red-500 mx-auto mb-4" />
          <p className="text-gray-600 mb-4">加载成绩失败</p>
          <Button asChild variant="outline">
            <Link to="/dashboard">返回首页</Link>
          </Button>
        </div>
      </div>
    );
  }

  const { grade, color: gradeColor } = getGrade(result.score);
  const scoreColor = getColorFromScore(result.score);
  const accuracy = result.totalQuestions > 0 ? (result.correctAnswers / result.totalQuestions * 100) : 0;

  return (
    <div className={cn('min-h-screen bg-gray-50 py-8', className)}>
      <div className="max-w-6xl mx-auto px-4 space-y-8">
        {/* Header */}
        <div className="flex items-center justify-between">
          <Button variant="ghost" asChild>
            <Link to="/dashboard">
              <ArrowLeft className="h-4 w-4 mr-2" />
              返回首页
            </Link>
          </Button>
          
          <div className="flex items-center gap-2">
            <Button variant="outline" onClick={handleShare}>
              <Share2 className="h-4 w-4 mr-2" />
              分享成绩
            </Button>
            {result.certificate && (
              <Button variant="outline" onClick={handleDownloadCertificate}>
                <Download className="h-4 w-4 mr-2" />
                下载证书
              </Button>
            )}
          </div>
        </div>

        {/* Result Header */}
        <Card className="overflow-hidden">
          <div className={cn(
            'bg-gradient-to-r from-exam-500 to-exam-600 text-white p-8',
            result.isPassed ? 'from-green-500 to-green-600' : 'from-red-500 to-red-600'
          )}>
            <div className="text-center space-y-4">
              <div className="flex justify-center">
                {result.isPassed ? (
                  <Trophy className="h-16 w-16 text-yellow-300" />
                ) : (
                  <Target className="h-16 w-16 text-white" />
                )}
              </div>
              
              <div>
                <h1 className="text-3xl font-bold mb-2">{result.examTitle}</h1>
                <p className="text-lg opacity-90">考试完成</p>
              </div>
              
              <div className="flex items-center justify-center gap-8">
                <div className="text-center">
                  <div className="text-4xl font-bold">{result.score}%</div>
                  <div className="text-sm opacity-75">总分</div>
                </div>
                <div className="text-center">
                  <div className={cn('text-2xl font-bold', gradeColor)}>{grade}</div>
                  <div className="text-sm opacity-75">等级</div>
                </div>
                <div className="text-center">
                  <div className="text-xl font-bold">
                    {result.isPassed ? '通过' : '未通过'}
                  </div>
                  <div className="text-sm opacity-75">结果</div>
                </div>
              </div>
            </div>
          </div>
        </Card>

        {/* Statistics Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <Card>
            <CardContent className="p-6 text-center">
              <div className="mb-3">
                <CheckCircle className="h-8 w-8 text-green-500 mx-auto" />
              </div>
              <div className="text-2xl font-bold text-green-600 mb-1">
                {result.correctAnswers}
              </div>
              <div className="text-sm text-gray-500">正确答案</div>
              <div className="text-xs text-gray-400 mt-1">
                正确率 {accuracy.toFixed(1)}%
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6 text-center">
              <div className="mb-3">
                <XCircle className="h-8 w-8 text-red-500 mx-auto" />
              </div>
              <div className="text-2xl font-bold text-red-600 mb-1">
                {result.totalQuestions - result.correctAnswers}
              </div>
              <div className="text-sm text-gray-500">错误答案</div>
              <div className="text-xs text-gray-400 mt-1">
                错误率 {(100 - accuracy).toFixed(1)}%
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6 text-center">
              <div className="mb-3">
                <Clock className="h-8 w-8 text-blue-500 mx-auto" />
              </div>
              <div className="text-2xl font-bold text-blue-600 mb-1">
                {formatDuration(Math.round(result.totalTime / 60))}
              </div>
              <div className="text-sm text-gray-500">用时</div>
              <div className="text-xs text-gray-400 mt-1">
                {formatDate(result.startTime, 'HH:mm')} - {formatDate(result.submitTime, 'HH:mm')}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6 text-center">
              <div className="mb-3">
                <Target className="h-8 w-8 text-purple-500 mx-auto" />
              </div>
              <div className="text-2xl font-bold text-purple-600 mb-1">
                {result.passingScore}%
              </div>
              <div className="text-sm text-gray-500">及格线</div>
              <div className="text-xs text-gray-400 mt-1">
                {result.isPassed ? '已达到' : '未达到'}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Performance Analysis */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="h-5 w-5" />
              成绩分析
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {/* Score breakdown */}
              <div>
                <div className="flex justify-between items-center mb-2">
                  <span className="text-sm font-medium">总体表现</span>
                  <span className={cn('text-sm font-bold', scoreColor)}>
                    {result.score}% ({grade})
                  </span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-3">
                  <div
                    className={cn(
                      'h-3 rounded-full transition-all duration-1000',
                      result.score >= 90 ? 'bg-green-500' :
                      result.score >= 80 ? 'bg-blue-500' :
                      result.score >= 70 ? 'bg-yellow-500' :
                      result.score >= 60 ? 'bg-orange-500' : 'bg-red-500'
                    )}
                    style={{ width: `${Math.min(result.score, 100)}%` }}
                  />
                </div>
              </div>

              {/* Performance feedback */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className={cn(
                  'p-4 rounded-lg',
                  result.isPassed ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'
                )}>
                  <h4 className={cn(
                    'font-medium mb-2',
                    result.isPassed ? 'text-green-800' : 'text-red-800'
                  )}>
                    {result.isPassed ? '恭喜通过考试！' : '考试未通过'}
                  </h4>
                  <p className={cn(
                    'text-sm',
                    result.isPassed ? 'text-green-700' : 'text-red-700'
                  )}>
                    {result.feedback || (result.isPassed 
                      ? '您的表现优秀，继续保持！' 
                      : '建议复习相关知识点后重新参加考试。'
                    )}
                  </p>
                </div>

                {result.percentile && (
                  <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                    <h4 className="font-medium text-blue-800 mb-2">排名情况</h4>
                    <p className="text-sm text-blue-700">
                      您的成绩超过了 {result.percentile}% 的考生
                    </p>
                  </div>
                )}
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Question Review */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2">
                <Eye className="h-5 w-5" />
                答题详情
              </CardTitle>
              <Button
                variant="outline"
                onClick={() => setShowAnswers(!showAnswers)}
              >
                {showAnswers ? (
                  <>
                    <EyeOff className="h-4 w-4 mr-2" />
                    隐藏答案
                  </>
                ) : (
                  <>
                    <Eye className="h-4 w-4 mr-2" />
                    显示答案
                  </>
                )}
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {showAnswers ? (
              <div className="space-y-6">
                {result.questionResults.map((questionResult, index) => (
                  <div key={questionResult.questionId} className="border-b border-gray-100 pb-6 last:border-b-0">
                    <div className="flex items-start justify-between mb-4">
                      <div className="flex items-center gap-3">
                        <Badge variant="outline">第 {index + 1} 题</Badge>
                        <Badge 
                          variant={questionResult.isCorrect ? 'success' : 'error'}
                          className="flex items-center gap-1"
                        >
                          {questionResult.isCorrect ? (
                            <CheckCircle className="h-3 w-3" />
                          ) : (
                            <XCircle className="h-3 w-3" />
                          )}
                          {questionResult.isCorrect ? '正确' : '错误'}
                        </Badge>
                        <span className="text-sm text-gray-500">
                          {questionResult.score} / {questionResult.maxScore} 分
                        </span>
                      </div>
                    </div>
                    
                    <div className="space-y-4">
                      <div>
                        <h4 className="font-medium mb-2">题目</h4>
                        <p className="text-gray-700 whitespace-pre-wrap">{questionResult.question}</p>
                      </div>
                      
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                          <h4 className="font-medium mb-2 text-green-700">正确答案</h4>
                          <div className="p-3 bg-green-50 rounded-lg">
                            <p className="text-green-800">{questionResult.correctAnswer}</p>
                          </div>
                        </div>
                        
                        <div>
                          <h4 className="font-medium mb-2 text-blue-700">您的答案</h4>
                          <div className={cn(
                            'p-3 rounded-lg',
                            questionResult.isCorrect 
                              ? 'bg-green-50 text-green-800' 
                              : 'bg-red-50 text-red-800'
                          )}>
                            <p>{questionResult.studentAnswer || '未作答'}</p>
                          </div>
                        </div>
                      </div>
                      
                      {questionResult.explanation && (
                        <div>
                          <h4 className="font-medium mb-2 text-blue-700">解析</h4>
                          <div className="p-3 bg-blue-50 rounded-lg">
                            <p className="text-blue-800 whitespace-pre-wrap">{questionResult.explanation}</p>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-8">
                <Eye className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                <p className="text-gray-500 mb-4">点击"显示答案"查看详细解析</p>
                <Button
                  variant="outline"
                  onClick={() => setShowAnswers(true)}
                >
                  显示答案和解析
                </Button>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Certificate */}
        {result.certificate && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Award className="h-5 w-5" />
                证书
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center justify-between p-6 bg-gradient-to-r from-yellow-50 to-yellow-100 rounded-lg border border-yellow-200">
                <div className="flex items-center gap-4">
                  <Medal className="h-12 w-12 text-yellow-600" />
                  <div>
                    <h3 className="font-bold text-yellow-900 mb-1">考试通过证书</h3>
                    <p className="text-yellow-700 text-sm">
                      证书编号：{result.certificate.verificationCode}
                    </p>
                    <p className="text-yellow-600 text-xs">
                      颁发日期：{formatDate(result.certificate.issueDate)}
                    </p>
                  </div>
                </div>
                <Button onClick={handleDownloadCertificate}>
                  <Download className="h-4 w-4 mr-2" />
                  下载证书
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Actions */}
        <div className="flex justify-center gap-4">
          <Button asChild variant="outline">
            <Link to="/dashboard">
              <ArrowLeft className="h-4 w-4 mr-2" />
              返回首页
            </Link>
          </Button>
          
          <Button asChild variant="outline">
            <Link to="/exams">
              <RotateCcw className="h-4 w-4 mr-2" />
              继续考试
            </Link>
          </Button>
          
          <Button onClick={handleShare}>
            <Share2 className="h-4 w-4 mr-2" />
            分享成绩
          </Button>
        </div>
      </div>
    </div>
  );
};