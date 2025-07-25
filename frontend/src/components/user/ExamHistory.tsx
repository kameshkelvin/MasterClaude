import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  Calendar,
  Clock,
  Trophy,
  Target,
  Filter,
  Search,
  Eye,
  Download,
  TrendingUp,
  TrendingDown,
  Minus,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { userApi } from '@/lib/api';
import { formatDate, formatDuration, getGrade, cn } from '@/lib/utils';

interface ExamHistoryFilters {
  search: string;
  status: 'all' | 'passed' | 'failed';
  dateRange: 'all' | '7d' | '30d' | '90d';
}

export const ExamHistory: React.FC = () => {
  const [filters, setFilters] = useState<ExamHistoryFilters>({
    search: '',
    status: 'all',
    dateRange: 'all',
  });
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;

  // Fetch exam history
  const { data, isLoading } = useQuery({
    queryKey: ['examHistory', filters, currentPage],
    queryFn: () => userApi.getExamHistory({
      ...filters,
      page: currentPage,
      limit: pageSize,
    }),
    select: (response) => response.data.data,
  });

  const results = data?.results || [];
  const pagination = data?.pagination || { total: 0, pages: 0 };
  const summary = data?.summary || {
    totalExams: 0,
    passedExams: 0,
    averageScore: 0,
    bestScore: 0,
    totalTime: 0,
  };

  const handleFilterChange = (key: keyof ExamHistoryFilters, value: string) => {
    setFilters(prev => ({ ...prev, [key]: value }));
    setCurrentPage(1); // Reset to first page when filters change
  };

  const getScoreColor = (score: number) => {
    if (score >= 90) return 'text-green-600';
    if (score >= 80) return 'text-blue-600';
    if (score >= 70) return 'text-yellow-600';
    if (score >= 60) return 'text-orange-600';
    return 'text-red-600';
  };

  const getScoreTrend = (score: number, previousScore?: number) => {
    if (!previousScore) return null;
    if (score > previousScore) return 'up';
    if (score < previousScore) return 'down';
    return 'same';
  };

  if (isLoading) {
    return (
      <div className="container-responsive py-6">
        <div className="animate-pulse space-y-6">
          <div className="h-8 bg-gray-200 rounded w-1/3"></div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="h-24 bg-gray-200 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container-responsive py-4 sm:py-6 spacing-responsive">
      {/* Header */}
      <div className="mb-6 sm:mb-8">
        <h1 className="text-responsive-xl font-bold text-gray-900 mb-2">考试历史</h1>
        <p className="text-gray-600">查看您的考试记录和成绩分析</p>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-3 sm:gap-6 mb-6 sm:mb-8">
        <Card className="card-responsive">
          <CardContent className="p-4 sm:p-6 text-center">
            <div className="mb-2">
              <Target className="h-6 w-6 sm:h-8 sm:w-8 text-blue-500 mx-auto" />
            </div>
            <div className="text-xl sm:text-2xl font-bold text-blue-600 mb-1">
              {summary.totalExams}
            </div>
            <div className="text-xs sm:text-sm text-gray-500">总考试</div>
          </CardContent>
        </Card>

        <Card className="card-responsive">
          <CardContent className="p-4 sm:p-6 text-center">
            <div className="mb-2">
              <Trophy className="h-6 w-6 sm:h-8 sm:w-8 text-green-500 mx-auto" />
            </div>
            <div className="text-xl sm:text-2xl font-bold text-green-600 mb-1">
              {summary.passedExams}
            </div>
            <div className="text-xs sm:text-sm text-gray-500">已通过</div>
          </CardContent>
        </Card>

        <Card className="card-responsive">
          <CardContent className="p-4 sm:p-6 text-center">
            <div className="mb-2">
              <TrendingUp className="h-6 w-6 sm:h-8 sm:w-8 text-purple-500 mx-auto" />
            </div>
            <div className="text-xl sm:text-2xl font-bold text-purple-600 mb-1">
              {summary.averageScore?.toFixed(1) || '0'}%
            </div>
            <div className="text-xs sm:text-sm text-gray-500">平均分</div>
          </CardContent>
        </Card>

        <Card className="card-responsive">
          <CardContent className="p-4 sm:p-6 text-center">
            <div className="mb-2">
              <Trophy className="h-6 w-6 sm:h-8 sm:w-8 text-yellow-500 mx-auto" />
            </div>
            <div className="text-xl sm:text-2xl font-bold text-yellow-600 mb-1">
              {summary.bestScore?.toFixed(1) || '0'}%
            </div>
            <div className="text-xs sm:text-sm text-gray-500">最高分</div>
          </CardContent>
        </Card>

        <Card className="card-responsive col-span-2 lg:col-span-1">
          <CardContent className="p-4 sm:p-6 text-center">
            <div className="mb-2">
              <Clock className="h-6 w-6 sm:h-8 sm:w-8 text-indigo-500 mx-auto" />
            </div>
            <div className="text-xl sm:text-2xl font-bold text-indigo-600 mb-1">
              {formatDuration(Math.round((summary.totalTime || 0) / 60))}
            </div>
            <div className="text-xs sm:text-sm text-gray-500">总时长</div>
          </CardContent>
        </Card>
      </div>

      {/* Filters */}
      <Card className="mb-6">
        <CardContent className="p-4 sm:p-6">
          <div className="flex flex-col sm:flex-row gap-4">
            {/* Search */}
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="搜索考试名称..."
                  value={filters.search}
                  onChange={(e) => handleFilterChange('search', e.target.value)}
                  className="pl-10 form-mobile"
                />
              </div>
            </div>

            {/* Status filter */}
            <select
              value={filters.status}
              onChange={(e) => handleFilterChange('status', e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg text-base min-h-[44px] sm:min-h-auto"
            >
              <option value="all">全部状态</option>
              <option value="passed">已通过</option>
              <option value="failed">未通过</option>
            </select>

            {/* Date range filter */}
            <select
              value={filters.dateRange}
              onChange={(e) => handleFilterChange('dateRange', e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg text-base min-h-[44px] sm:min-h-auto"
            >
              <option value="all">全部时间</option>
              <option value="7d">最近7天</option>
              <option value="30d">最近30天</option>
              <option value="90d">最近90天</option>
            </select>
          </div>
        </CardContent>
      </Card>

      {/* Results list */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between">
            <span>考试记录</span>
            <span className="text-sm font-normal text-gray-500">
              共 {pagination.total} 条记录
            </span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          {results.length === 0 ? (
            <div className="text-center py-8 sm:py-12">
              <Calendar className="h-12 w-12 sm:h-16 sm:w-16 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg sm:text-xl font-semibold text-gray-900 mb-2">
                暂无考试记录
              </h3>
              <p className="text-gray-600 mb-6">
                还没有参加过考试，立即开始您的第一次考试吧！
              </p>
              <Button asChild className="btn-mobile">
                <Link to="/exams">
                  开始考试
                </Link>
              </Button>
            </div>
          ) : (
            <div className="space-y-4">
              {results.map((result, index) => {
                const { grade } = getGrade(result.score);
                const trend = getScoreTrend(
                  result.score,
                  results[index + 1]?.score
                );

                return (
                  <div
                    key={result.id}
                    className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow"
                  >
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                      {/* Left side - Exam info */}
                      <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between mb-2">
                          <h3 className="font-semibold text-gray-900 truncate pr-2">
                            {result.examTitle}
                          </h3>
                          <div className="flex items-center gap-2 flex-shrink-0">
                            <Badge
                              variant={result.isPassed ? 'success' : 'error'}
                              className="text-xs"
                            >
                              {result.isPassed ? '通过' : '未通过'}
                            </Badge>
                            {trend && (
                              <div className="flex items-center">
                                {trend === 'up' && <TrendingUp className="h-4 w-4 text-green-500" />}
                                {trend === 'down' && <TrendingDown className="h-4 w-4 text-red-500" />}
                                {trend === 'same' && <Minus className="h-4 w-4 text-gray-500" />}
                              </div>
                            )}
                          </div>
                        </div>

                        <div className="flex flex-wrap items-center gap-4 text-sm text-gray-500">
                          <div className="flex items-center gap-1">
                            <Calendar className="h-4 w-4" />
                            {formatDate(result.submitTime, 'YYYY-MM-DD HH:mm')}
                          </div>
                          <div className="flex items-center gap-1">
                            <Clock className="h-4 w-4" />
                            {formatDuration(Math.round(result.totalTime / 60))}
                          </div>
                          <div className="flex items-center gap-1">
                            <Target className="h-4 w-4" />
                            {result.correctAnswers} / {result.totalQuestions} 正确
                          </div>
                        </div>
                      </div>

                      {/* Right side - Score and actions */}
                      <div className="flex items-center justify-between sm:justify-end gap-4">
                        <div className="text-center">
                          <div className={cn('text-2xl font-bold', getScoreColor(result.score))}>
                            {result.score}%
                          </div>
                          <div className="text-xs text-gray-500">{grade}</div>
                        </div>

                        <div className="flex gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            asChild
                            className="touch-target"
                          >
                            <Link to={`/exam/${result.examId}/result?attemptId=${result.id}`}>
                              <Eye className="h-4 w-4 mr-1 sm:mr-2" />
                              <span className="hidden sm:inline">查看</span>
                            </Link>
                          </Button>

                          {result.certificate && (
                            <Button
                              variant="outline"
                              size="sm"
                              className="touch-target"
                              onClick={() => {
                                // Download certificate logic
                                console.log('Download certificate for', result.id);
                              }}
                            >
                              <Download className="h-4 w-4" />
                            </Button>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Pagination */}
          {pagination.pages > 1 && (
            <div className="flex items-center justify-between mt-6 pt-6 border-t border-gray-200">
              <div className="text-sm text-gray-500">
                第 {currentPage} 页，共 {pagination.pages} 页
              </div>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
                  disabled={currentPage === 1}
                  className="touch-target"
                >
                  <ChevronLeft className="h-4 w-4" />
                  <span className="hidden sm:inline ml-1">上一页</span>
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setCurrentPage(prev => Math.min(pagination.pages, prev + 1))}
                  disabled={currentPage === pagination.pages}
                  className="touch-target"
                >
                  <span className="hidden sm:inline mr-1">下一页</span>
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};