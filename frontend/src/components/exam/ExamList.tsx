import React, { useState, useCallback, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { 
  Search, 
  Filter, 
  SortDesc, 
  Grid3X3, 
  List, 
  RefreshCw,
  Calendar,
  Clock,
  Trophy,
  Users,
  BookOpen,
  X
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { ExamCard } from './ExamCard';
import { examApi } from '@/lib/api';
import { Exam, ExamFilters } from '@/types';
import { cn, debounce } from '@/lib/utils';

interface ExamListProps {
  className?: string;
}

export const ExamList: React.FC<ExamListProps> = ({ className }) => {
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [filters, setFilters] = useState<ExamFilters>({
    status: [],
    difficulty: [],
    tags: [],
    search: '',
    sortBy: 'startTime',
    sortOrder: 'desc',
    page: 0,
    size: 12,
  });
  const [showFilters, setShowFilters] = useState(false);

  // Fetch exams
  const {
    data: examData,
    isLoading,
    error,
    refetch,
    isFetching,
  } = useQuery({
    queryKey: ['exams', filters],
    queryFn: () => examApi.getAvailableExams(filters),
    select: (response) => response.data.data,
    staleTime: 1000 * 60 * 5, // 5 minutes
    gcTime: 1000 * 60 * 10, // 10 minutes
  });

  const exams = examData?.content || [];
  const pagination = examData?.pagination;

  // Debounced search
  const debouncedSearch = useCallback(
    debounce((search: string) => {
      setFilters(prev => ({ ...prev, search, page: 0 }));
    }, 300),
    []
  );

  // Filter options
  const statusOptions = [
    { value: 'SCHEDULED', label: '未开始', color: 'bg-gray-100 text-gray-800' },
    { value: 'ACTIVE', label: '进行中', color: 'bg-green-100 text-green-800' },
    { value: 'ENDED', label: '已结束', color: 'bg-red-100 text-red-800' },
  ];

  const difficultyOptions = [
    { value: 'EASY', label: '简单', color: 'bg-green-100 text-green-800' },
    { value: 'MEDIUM', label: '中等', color: 'bg-yellow-100 text-yellow-800' },
    { value: 'HARD', label: '困难', color: 'bg-red-100 text-red-800' },
  ];

  const sortOptions = [
    { value: 'startTime', label: '开始时间' },
    { value: 'title', label: '标题' },
    { value: 'difficulty', label: '难度' },
    { value: 'duration', label: '时长' },
  ];

  // Handle filter changes
  const handleStatusFilter = (status: string) => {
    setFilters(prev => ({
      ...prev,
      status: prev.status?.includes(status)
        ? prev.status.filter(s => s !== status)
        : [...(prev.status || []), status],
      page: 0,
    }));
  };

  const handleDifficultyFilter = (difficulty: string) => {
    setFilters(prev => ({
      ...prev,
      difficulty: prev.difficulty?.includes(difficulty)
        ? prev.difficulty.filter(d => d !== difficulty)
        : [...(prev.difficulty || []), difficulty],
      page: 0,
    }));
  };

  const handleSortChange = (sortBy: string) => {
    setFilters(prev => ({
      ...prev,
      sortBy: sortBy as any,
      sortOrder: prev.sortBy === sortBy && prev.sortOrder === 'asc' ? 'desc' : 'asc',
      page: 0,
    }));
  };

  const clearAllFilters = () => {
    setFilters(prev => ({
      ...prev,
      status: [],
      difficulty: [],
      tags: [],
      search: '',
      page: 0,
    }));
  };

  const hasActiveFilters = useMemo(() => {
    return (
      (filters.status && filters.status.length > 0) ||
      (filters.difficulty && filters.difficulty.length > 0) ||
      (filters.tags && filters.tags.length > 0) ||
      (filters.search && filters.search.length > 0)
    );
  }, [filters]);

  // Statistics
  const stats = useMemo(() => {
    const total = exams.length;
    const active = exams.filter(exam => exam.status === 'ACTIVE').length;
    const upcoming = exams.filter(exam => exam.status === 'SCHEDULED').length;
    const ended = exams.filter(exam => exam.status === 'ENDED').length;

    return { total, active, upcoming, ended };
  }, [exams]);

  if (error) {
    return (
      <div className="text-center py-12">
        <div className="text-red-500 mb-4">加载考试列表失败</div>
        <Button onClick={() => refetch()} variant="outline">
          重试
        </Button>
      </div>
    );
  }

  return (
    <div className={cn('space-y-6', className)}>
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">考试列表</h1>
          <p className="text-gray-600">选择考试开始答题</p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => refetch()}
            disabled={isFetching}
          >
            <RefreshCw className={cn('h-4 w-4 mr-2', isFetching && 'animate-spin')} />
            刷新
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setViewMode(viewMode === 'grid' ? 'list' : 'grid')}
          >
            {viewMode === 'grid' ? (
              <List className="h-4 w-4 mr-2" />
            ) : (
              <Grid3X3 className="h-4 w-4 mr-2" />
            )}
            {viewMode === 'grid' ? '列表' : '网格'}
          </Button>
        </div>
      </div>

      {/* Statistics */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-exam-100 rounded-lg">
                <BookOpen className="h-5 w-5 text-exam-600" />
              </div>
              <div>
                <p className="text-2xl font-bold">{stats.total}</p>
                <p className="text-sm text-gray-500">总考试数</p>
              </div>
            </div>
          </CardContent>
        </Card>
        
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-green-100 rounded-lg">
                <Clock className="h-5 w-5 text-green-600" />
              </div>
              <div>
                <p className="text-2xl font-bold">{stats.active}</p>
                <p className="text-sm text-gray-500">进行中</p>
              </div>
            </div>
          </CardContent>
        </Card>
        
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-yellow-100 rounded-lg">
                <Calendar className="h-5 w-5 text-yellow-600" />
              </div>
              <div>
                <p className="text-2xl font-bold">{stats.upcoming}</p>
                <p className="text-sm text-gray-500">即将开始</p>
              </div>
            </div>
          </CardContent>
        </Card>
        
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-gray-100 rounded-lg">
                <Trophy className="h-5 w-5 text-gray-600" />
              </div>
              <div>
                <p className="text-2xl font-bold">{stats.ended}</p>
                <p className="text-sm text-gray-500">已结束</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Search and Filters */}
      <div className="space-y-4">
        {/* Search Bar */}
        <div className="flex gap-2">
          <div className="flex-1">
            <Input
              placeholder="搜索考试标题、描述..."
              leftIcon={<Search className="h-4 w-4" />}
              onChange={(e) => debouncedSearch(e.target.value)}
              defaultValue={filters.search}
            />
          </div>
          <Button
            variant="outline"
            onClick={() => setShowFilters(!showFilters)}
            className={cn(showFilters && 'bg-exam-50 border-exam-200')}
          >
            <Filter className="h-4 w-4 mr-2" />
            筛选
            {hasActiveFilters && (
              <Badge variant="exam" className="ml-2 h-5 w-5 p-0 text-xs">
                !
              </Badge>
            )}
          </Button>
        </div>

        {/* Filter Panel */}
        {showFilters && (
          <Card>
            <CardHeader className="pb-4">
              <div className="flex items-center justify-between">
                <CardTitle className="text-lg">筛选条件</CardTitle>
                {hasActiveFilters && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={clearAllFilters}
                  >
                    <X className="h-4 w-4 mr-1" />
                    清空
                  </Button>
                )}
              </div>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* Status Filter */}
              <div>
                <h4 className="font-medium mb-3">考试状态</h4>
                <div className="flex flex-wrap gap-2">
                  {statusOptions.map((option) => (
                    <Button
                      key={option.value}
                      variant={filters.status?.includes(option.value) ? 'default' : 'outline'}
                      size="sm"
                      onClick={() => handleStatusFilter(option.value)}
                    >
                      {option.label}
                    </Button>
                  ))}
                </div>
              </div>

              {/* Difficulty Filter */}
              <div>
                <h4 className="font-medium mb-3">难度等级</h4>
                <div className="flex flex-wrap gap-2">
                  {difficultyOptions.map((option) => (
                    <Button
                      key={option.value}
                      variant={filters.difficulty?.includes(option.value) ? 'default' : 'outline'}
                      size="sm"
                      onClick={() => handleDifficultyFilter(option.value)}
                    >
                      {option.label}
                    </Button>
                  ))}
                </div>
              </div>

              {/* Sort Options */}
              <div>
                <h4 className="font-medium mb-3">排序方式</h4>
                <div className="flex flex-wrap gap-2">
                  {sortOptions.map((option) => (
                    <Button
                      key={option.value}
                      variant={filters.sortBy === option.value ? 'default' : 'outline'}
                      size="sm"
                      onClick={() => handleSortChange(option.value)}
                    >
                      {option.label}
                      {filters.sortBy === option.value && (
                        <SortDesc 
                          className={cn(
                            'h-3 w-3 ml-1',
                            filters.sortOrder === 'asc' && 'rotate-180'
                          )} 
                        />
                      )}
                    </Button>
                  ))}
                </div>
              </div>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Results */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 6 }).map((_, index) => (
            <Card key={index} className="animate-pulse">
              <CardHeader>
                <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
                <div className="h-3 bg-gray-200 rounded w-1/2"></div>
              </CardHeader>
              <CardContent>
                <div className="space-y-2">
                  <div className="h-3 bg-gray-200 rounded"></div>
                  <div className="h-3 bg-gray-200 rounded w-2/3"></div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <>
          {exams.length === 0 ? (
            <div className="text-center py-12">
              <BookOpen className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无考试</h3>
              <p className="text-gray-500">
                {hasActiveFilters ? '没有符合筛选条件的考试' : '当前没有可参加的考试'}
              </p>
              {hasActiveFilters && (
                <Button
                  variant="outline"
                  onClick={clearAllFilters}
                  className="mt-4"
                >
                  清空筛选条件
                </Button>
              )}
            </div>
          ) : (
            <div className={cn(
              viewMode === 'grid' 
                ? 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6'
                : 'space-y-4'
            )}>
              {exams.map((exam: Exam) => (
                <ExamCard
                  key={exam.id}
                  exam={exam}
                  variant={viewMode === 'list' ? 'compact' : 'default'}
                />
              ))}
            </div>
          )}

          {/* Pagination */}
          {pagination && pagination.totalPages > 1 && (
            <div className="flex justify-center items-center gap-4 mt-8">
              <Button
                variant="outline"
                disabled={!pagination.hasPrevious}
                onClick={() => setFilters(prev => ({ ...prev, page: prev.page! - 1 }))}
              >
                上一页
              </Button>
              <span className="text-sm text-gray-600">
                第 {pagination.currentPage + 1} 页，共 {pagination.totalPages} 页
              </span>
              <Button
                variant="outline"
                disabled={!pagination.hasNext}
                onClick={() => setFilters(prev => ({ ...prev, page: prev.page! + 1 }))}
              >
                下一页
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
};