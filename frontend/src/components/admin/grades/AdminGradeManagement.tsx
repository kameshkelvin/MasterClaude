import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  BarChart3,
  Download,
  Search,
  Filter,
  Eye,
  TrendingUp,
  TrendingDown,
  Users,
  FileText,
  Calendar,
  Clock,
  Target,
  Award,
  AlertCircle,
  CheckCircle,
  PieChart,
  LineChart,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { adminApi } from '@/lib/api';
import { formatDate, cn } from '@/lib/utils';

interface GradeRecord {
  id: string;
  studentId: string;
  studentName: string;
  studentEmail: string;
  examId: string;
  examTitle: string;
  score: number;
  totalScore: number;
  percentage: number;
  status: 'GRADED' | 'PENDING' | 'REVIEW_REQUIRED';
  timeSpent: number; // in minutes
  submittedAt: string;
  gradedAt?: string;
  attemptNumber: number;
  answers: {
    questionId: string;
    questionTitle: string;
    userAnswer: string;
    correctAnswer: string;
    isCorrect: boolean;
    points: number;
    maxPoints: number;
  }[];
}

interface GradeFilters {
  examId: string;
  studentId: string;
  status: string;
  search: string;
  dateRange: 'all' | '7d' | '30d' | '90d';
  sortBy: 'score' | 'submittedAt' | 'timeSpent' | 'studentName';
  sortOrder: 'asc' | 'desc';
}

interface GradeStatistics {
  overview: {
    totalSubmissions: number;
    averageScore: number;
    passRate: number;
    pendingGrades: number;
    averageTimeSpent: number;
  };
  scoreDistribution: {
    range: string;
    count: number;
    percentage: number;
  }[];
  examStats: {
    examId: string;
    examTitle: string;
    submissions: number;
    averageScore: number;
    passRate: number;
    maxScore: number;
    minScore: number;
  }[];
  trends: {
    date: string;
    submissions: number;
    averageScore: number;
  }[];
}

export const AdminGradeManagement: React.FC = () => {
  const [selectedGrades, setSelectedGrades] = useState<string[]>([]);
  const [showAnalyticsModal, setShowAnalyticsModal] = useState(false);
  const [selectedExamForAnalytics, setSelectedExamForAnalytics] = useState<string | null>(null);
  const [filters, setFilters] = useState<GradeFilters>({
    examId: 'all',
    studentId: 'all',
    status: 'all',
    search: '',
    dateRange: '30d',
    sortBy: 'submittedAt',
    sortOrder: 'desc',
  });

  const queryClient = useQueryClient();

  // Fetch grade records
  const { data: gradesData, isLoading, error } = useQuery({
    queryKey: ['adminGrades', filters],
    queryFn: () => adminApi.getGradeStatistics('comprehensive'),
    select: (response) => response.data.data as { 
      grades: GradeRecord[]; 
      statistics: GradeStatistics;
      exams: Array<{ id: string; title: string; }>;
      students: Array<{ id: string; name: string; }>;
    },
  });

  // Export grades mutation
  const exportGradesMutation = useMutation({
    mutationFn: (data: { examIds: string[]; format: 'excel' | 'csv' }) => 
      adminApi.exportStatistics(data.examIds[0], data.format),
    onSuccess: (response) => {
      // Handle file download
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `grades_export_${Date.now()}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    },
  });

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'GRADED': return 'bg-green-100 text-green-800 border-green-200';
      case 'PENDING': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'REVIEW_REQUIRED': return 'bg-red-100 text-red-800 border-red-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'GRADED': return '已评分';
      case 'PENDING': return '待评分';
      case 'REVIEW_REQUIRED': return '需复审';
      default: return '未知';
    }
  };

  const getScoreColor = (percentage: number) => {
    if (percentage >= 90) return 'text-green-600';
    if (percentage >= 80) return 'text-blue-600';
    if (percentage >= 70) return 'text-yellow-600';
    if (percentage >= 60) return 'text-orange-600';
    return 'text-red-600';
  };

  const handleExportGrades = (format: 'excel' | 'csv') => {
    const examIds = filters.examId === 'all' 
      ? gradesData?.exams.map(e => e.id) || []
      : [filters.examId];
    
    exportGradesMutation.mutate({ examIds, format });
  };

  if (isLoading) {
    return (
      <div className="p-4 sm:p-6 space-y-6">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-slate-200 rounded w-1/3"></div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="h-32 bg-slate-200 rounded"></div>
            ))}
          </div>
          <div className="space-y-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="h-24 bg-slate-200 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4 sm:p-6">
        <div className="text-center py-12">
          <AlertCircle className="h-12 w-12 text-red-500 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-slate-900 mb-2">
            数据加载失败
          </h3>
          <p className="text-slate-600">
            无法加载成绩数据，请稍后重试
          </p>
        </div>
      </div>
    );
  }

  const { grades = [], statistics, exams = [], students = [] } = gradesData || {};

  return (
    <div className="p-4 sm:p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-slate-900">
            成绩管理
          </h1>
          <p className="text-slate-600 mt-1">
            查看和分析考试成绩，管理评分工作
          </p>
        </div>
        
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => handleExportGrades('csv')}
            loading={exportGradesMutation.isPending}
          >
            <Download className="h-4 w-4 mr-1" />
            导出CSV
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => handleExportGrades('excel')}
            loading={exportGradesMutation.isPending}
          >
            <Download className="h-4 w-4 mr-1" />
            导出Excel
          </Button>
          <Button
            size="sm"
            onClick={() => setShowAnalyticsModal(true)}
            className="bg-slate-700 hover:bg-slate-800"
          >
            <BarChart3 className="h-4 w-4 mr-1" />
            详细分析
          </Button>
        </div>
      </div>

      {/* Statistics Overview */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-blue-100 rounded-lg">
                <FileText className="h-5 w-5 text-blue-600" />
              </div>
              <div>
                <p className="text-sm text-slate-600">总提交数</p>
                <p className="text-xl font-bold text-slate-900">
                  {statistics?.overview.totalSubmissions || 0}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-green-100 rounded-lg">
                <Target className="h-5 w-5 text-green-600" />
              </div>
              <div>
                <p className="text-sm text-slate-600">平均分</p>
                <p className="text-xl font-bold text-slate-900">
                  {statistics?.overview.averageScore?.toFixed(1) || '0.0'}%
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-purple-100 rounded-lg">
                <Award className="h-5 w-5 text-purple-600" />
              </div>
              <div>
                <p className="text-sm text-slate-600">通过率</p>
                <p className="text-xl font-bold text-slate-900">
                  {statistics?.overview.passRate?.toFixed(1) || '0.0'}%
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-yellow-100 rounded-lg">
                <Clock className="h-5 w-5 text-yellow-600" />
              </div>
              <div>
                <p className="text-sm text-slate-600">平均用时</p>
                <p className="text-xl font-bold text-slate-900">
                  {Math.round(statistics?.overview.averageTimeSpent || 0)}分钟
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-red-100 rounded-lg">
                <AlertCircle className="h-5 w-5 text-red-600" />
              </div>
              <div>
                <p className="text-sm text-slate-600">待评分</p>
                <p className="text-xl font-bold text-slate-900">
                  {statistics?.overview.pendingGrades || 0}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Score Distribution Chart */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <PieChart className="h-5 w-5" />
              分数分布
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {statistics?.scoreDistribution?.map((item, index) => (
                <div key={index} className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div 
                      className="w-4 h-4 rounded"
                      style={{ backgroundColor: `hsl(${index * 45}, 70%, 50%)` }}
                    />
                    <span className="text-sm text-slate-700">{item.range}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-medium text-slate-900">
                      {item.count} 人
                    </span>
                    <span className="text-xs text-slate-500">
                      ({item.percentage.toFixed(1)}%)
                    </span>
                  </div>
                </div>
              )) || (
                <p className="text-sm text-slate-500 text-center py-4">
                  暂无数据
                </p>
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BarChart3 className="h-5 w-5" />
              考试成绩概览
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {statistics?.examStats?.slice(0, 5).map((exam) => (
                <div key={exam.examId} className="space-y-2">
                  <div className="flex items-center justify-between">
                    <h4 className="text-sm font-medium text-slate-900 truncate">
                      {exam.examTitle}
                    </h4>
                    <span className="text-sm text-slate-600">
                      {exam.submissions} 人参加
                    </span>
                  </div>
                  <div className="flex items-center gap-4 text-xs text-slate-500">
                    <span>平均分: {exam.averageScore.toFixed(1)}%</span>
                    <span>最高分: {exam.maxScore}%</span>
                    <span>最低分: {exam.minScore}%</span>
                    <span>通过率: {exam.passRate.toFixed(1)}%</span>
                  </div>
                  <div className="w-full bg-slate-200 rounded-full h-2">
                    <div 
                      className="bg-blue-600 h-2 rounded-full transition-all"
                      style={{ width: `${exam.averageScore}%` }}
                    />
                  </div>
                </div>
              )) || (
                <p className="text-sm text-slate-500 text-center py-4">
                  暂无数据
                </p>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="flex-1">
              <Input
                placeholder="搜索学生姓名、邮箱..."
                leftIcon={<Search className="h-4 w-4" />}
                value={filters.search}
                onChange={(e) => setFilters({ ...filters, search: e.target.value })}
              />
            </div>

            <select
              value={filters.examId}
              onChange={(e) => setFilters({ ...filters, examId: e.target.value })}
              className="px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500"
            >
              <option value="all">所有考试</option>
              {exams.map(exam => (
                <option key={exam.id} value={exam.id}>{exam.title}</option>
              ))}
            </select>

            <select
              value={filters.status}
              onChange={(e) => setFilters({ ...filters, status: e.target.value })}
              className="px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500"
            >
              <option value="all">所有状态</option>
              <option value="GRADED">已评分</option>
              <option value="PENDING">待评分</option>
              <option value="REVIEW_REQUIRED">需复审</option>
            </select>

            <select
              value={filters.dateRange}
              onChange={(e) => setFilters({ ...filters, dateRange: e.target.value as any })}
              className="px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500"
            >
              <option value="all">所有时间</option>
              <option value="7d">最近7天</option>
              <option value="30d">最近30天</option>
              <option value="90d">最近90天</option>
            </select>
          </div>
        </CardContent>
      </Card>

      {/* Grade Records */}
      <div className="space-y-4">
        {grades.slice(0, 20).map((grade) => (
          <Card key={grade.id} className="hover:shadow-lg transition-shadow">
            <CardContent className="p-4 sm:p-6">
              <div className="flex items-start justify-between mb-4">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h3 className="text-lg font-semibold text-slate-900">
                      {grade.studentName}
                    </h3>
                    <Badge className={cn('text-xs', getStatusColor(grade.status))}>
                      {getStatusText(grade.status)}
                    </Badge>
                    {grade.attemptNumber > 1 && (
                      <Badge variant="outline" className="text-xs">
                        第 {grade.attemptNumber} 次
                      </Badge>
                    )}
                  </div>
                  <p className="text-sm text-slate-600 mb-1">
                    {grade.examTitle}
                  </p>
                  <div className="flex items-center gap-4 text-sm text-slate-500">
                    <span className="flex items-center gap-1">
                      <Calendar className="h-4 w-4" />
                      {formatDate(grade.submittedAt, 'MM-DD HH:mm')}
                    </span>
                    <span className="flex items-center gap-1">
                      <Clock className="h-4 w-4" />
                      {grade.timeSpent} 分钟
                    </span>
                    <span className="flex items-center gap-1">
                      <Users className="h-4 w-4" />
                      {grade.studentEmail}
                    </span>
                  </div>
                </div>

                <div className="text-right">
                  <div className={cn('text-2xl font-bold', getScoreColor(grade.percentage))}>
                    {grade.percentage.toFixed(1)}%
                  </div>
                  <div className="text-sm text-slate-500">
                    {grade.score}/{grade.totalScore} 分
                  </div>
                </div>
              </div>

              {/* Answer Summary */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-4">
                <div className="text-center p-2 bg-green-50 rounded-lg">
                  <div className="text-lg font-semibold text-green-700">
                    {grade.answers.filter(a => a.isCorrect).length}
                  </div>
                  <div className="text-xs text-green-600">正确</div>
                </div>
                <div className="text-center p-2 bg-red-50 rounded-lg">
                  <div className="text-lg font-semibold text-red-700">
                    {grade.answers.filter(a => !a.isCorrect).length}
                  </div>
                  <div className="text-xs text-red-600">错误</div>
                </div>
                <div className="text-center p-2 bg-blue-50 rounded-lg">
                  <div className="text-lg font-semibold text-blue-700">
                    {grade.answers.length}
                  </div>
                  <div className="text-xs text-blue-600">总题数</div>
                </div>
                <div className="text-center p-2 bg-purple-50 rounded-lg">
                  <div className="text-lg font-semibold text-purple-700">
                    {((grade.answers.filter(a => a.isCorrect).length / grade.answers.length) * 100).toFixed(0)}%
                  </div>
                  <div className="text-xs text-purple-600">正确率</div>
                </div>
              </div>

              {/* Actions */}
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => window.open(`/admin/grades/${grade.id}/details`, '_blank')}
                  >
                    <Eye className="h-4 w-4 mr-1" />
                    查看详情
                  </Button>
                  
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => window.open(`/admin/grades/${grade.id}/review`, '_blank')}
                  >
                    <FileText className="h-4 w-4 mr-1" />
                    评分审核
                  </Button>
                </div>

                <div className="flex items-center gap-2">
                  {grade.percentage >= 60 ? (
                    <CheckCircle className="h-5 w-5 text-green-500" />
                  ) : (
                    <AlertCircle className="h-5 w-5 text-red-500" />
                  )}
                  <span className="text-sm text-slate-600">
                    {grade.percentage >= 60 ? '通过' : '未通过'}
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}

        {grades.length === 0 && (
          <Card>
            <CardContent className="p-12 text-center">
              <BarChart3 className="h-12 w-12 text-slate-400 mx-auto mb-4" />
              <h3 className="text-lg font-semibold text-slate-900 mb-2">
                暂无成绩记录
              </h3>
              <p className="text-slate-600">
                还没有学生提交考试，请等待学生完成考试
              </p>
            </CardContent>
          </Card>
        )}
      </div>

      {/* TODO: Add GradeAnalyticsModal component */}
    </div>
  );
};