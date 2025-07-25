import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  TrendingUp,
  TrendingDown,
  Target,
  Clock,
  Trophy,
  BookOpen,
  Users,
  BarChart3,
  Calendar,
  Award,
  Star,
  Zap,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { userApi } from '@/lib/api';
import { formatDate, cn } from '@/lib/utils';

interface ProgressData {
  overall: {
    totalExams: number;
    completedExams: number;
    averageScore: number;
    totalTime: number;
    rank: number;
    percentile: number;
  };
  recent: {
    last7Days: {
      examsCompleted: number;
      averageScore: number;
      timeSpent: number;
      improvement: number;
    };
    last30Days: {
      examsCompleted: number;
      averageScore: number;
      timeSpent: number;
      improvement: number;
    };
  };
  streaks: {
    currentStreak: number;
    longestStreak: number;
    lastActivity: string;
  };
  achievements: {
    totalBadges: number;
    recentBadges: Array<{
      id: string;
      name: string;
      description: string;
      icon: string;
      earnedAt: string;
    }>;
  };
  goals: {
    weeklyTarget: number;
    weeklyProgress: number;
    monthlyTarget: number;
    monthlyProgress: number;
  };
  subjects: Array<{
    name: string;
    completedExams: number;
    averageScore: number;
    improvement: number;
    rank: number;
  }>;
}

interface ProgressTrackerProps {
  className?: string;
  showDetailed?: boolean;
}

export const ProgressTracker: React.FC<ProgressTrackerProps> = ({
  className,
  showDetailed = false,
}) => {
  const [timeRange, setTimeRange] = useState<'7d' | '30d' | '90d'>('7d');

  // Fetch progress data
  const { data: progress, isLoading } = useQuery({
    queryKey: ['progress', timeRange],
    queryFn: () => userApi.getProgressData(timeRange),
    select: (response) => response.data.data,
    refetchInterval: 60000, // Refresh every minute
  });

  // Calculate completion rate
  const completionRate = progress?.overall.totalExams 
    ? (progress.overall.completedExams / progress.overall.totalExams) * 100 
    : 0;

  // Get trend icon and color
  const getTrendIcon = (improvement: number) => {
    if (improvement > 0) {
      return <TrendingUp className="h-4 w-4 text-green-500" />;
    } else if (improvement < 0) {
      return <TrendingDown className="h-4 w-4 text-red-500" />;
    }
    return <div className="h-4 w-4 bg-gray-300 rounded-full" />;
  };

  // Get score color
  const getScoreColor = (score: number) => {
    if (score >= 90) return 'text-green-600';
    if (score >= 80) return 'text-blue-600';
    if (score >= 70) return 'text-yellow-600';
    if (score >= 60) return 'text-orange-600';
    return 'text-red-600';
  };

  if (isLoading) {
    return (
      <div className={cn('space-y-6', className)}>
        <div className="animate-pulse space-y-4">
          <div className="h-6 bg-gray-200 rounded w-1/3"></div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="h-24 bg-gray-200 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (!progress) {
    return (
      <div className={cn('text-center py-8', className)}>
        <BarChart3 className="h-12 w-12 text-gray-400 mx-auto mb-3" />
        <p className="text-gray-500">暂无进度数据</p>
      </div>
    );
  }

  return (
    <div className={cn('space-y-6', className)}>
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-gray-900 mb-1">学习进度</h2>
          <p className="text-gray-600">跟踪您的学习进展和成就</p>
        </div>
        
        <div className="flex items-center gap-2">
          <Button
            variant={timeRange === '7d' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setTimeRange('7d')}
          >
            最近7天
          </Button>
          <Button
            variant={timeRange === '30d' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setTimeRange('30d')}
          >
            最近30天
          </Button>
          <Button
            variant={timeRange === '90d' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setTimeRange('90d')}
          >
            最近90天
          </Button>
        </div>
      </div>

      {/* Overview Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="card-responsive">
          <CardContent className="p-4 sm:p-6 text-center">
            <div className="mb-2 sm:mb-3">
              <BookOpen className="h-6 w-6 sm:h-8 sm:w-8 text-blue-500 mx-auto" />
            </div>
            <div className="text-xl sm:text-2xl font-bold text-blue-600 mb-1">
              {progress.overall.completedExams}
            </div>
            <div className="text-xs sm:text-sm text-gray-500">已完成考试</div>
            <div className="text-xs text-gray-400 mt-1">
              完成率 {completionRate.toFixed(1)}%
            </div>
          </CardContent>
        </Card>

        <Card className="card-responsive">
          <CardContent className="p-4 sm:p-6 text-center">
            <div className="mb-2 sm:mb-3">
              <Target className="h-6 w-6 sm:h-8 sm:w-8 text-green-500 mx-auto" />
            </div>
            <div className="text-xl sm:text-2xl font-bold text-green-600 mb-1">
              {progress.overall.averageScore?.toFixed(1) || '0'}%
            </div>
            <div className="text-xs sm:text-sm text-gray-500">平均分</div>
            <div className="flex items-center justify-center mt-1">
              {getTrendIcon(progress.recent.last7Days.improvement)}
              <span className="text-xs text-gray-400 ml-1">
                {progress.recent.last7Days.improvement > 0 ? '+' : ''}
                {progress.recent.last7Days.improvement?.toFixed(1) || '0'}%
              </span>
            </div>
          </CardContent>
        </Card>

        <Card className="card-responsive">
          <CardContent className="p-4 sm:p-6 text-center">
            <div className="mb-2 sm:mb-3">
              <Users className="h-6 w-6 sm:h-8 sm:w-8 text-purple-500 mx-auto" />
            </div>
            <div className="text-xl sm:text-2xl font-bold text-purple-600 mb-1">
              #{progress.overall.rank}
            </div>
            <div className="text-xs sm:text-sm text-gray-500">当前排名</div>
            <div className="text-xs text-gray-400 mt-1">
              超过 {progress.overall.percentile}% 用户
            </div>
          </CardContent>
        </Card>

        <Card className="card-responsive">
          <CardContent className="p-4 sm:p-6 text-center">
            <div className="mb-2 sm:mb-3">
              <Zap className="h-6 w-6 sm:h-8 sm:w-8 text-orange-500 mx-auto" />
            </div>
            <div className="text-xl sm:text-2xl font-bold text-orange-600 mb-1">
              {progress.streaks.currentStreak}
            </div>
            <div className="text-xs sm:text-sm text-gray-500">连续天数</div>
            <div className="text-xs text-gray-400 mt-1">
              最长 {progress.streaks.longestStreak} 天
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Goals Progress */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-lg">
              <Calendar className="h-5 w-5" />
              本周目标
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">进度</span>
                <span className="text-sm font-medium">
                  {progress.goals.weeklyProgress} / {progress.goals.weeklyTarget}
                </span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-3">
                <div
                  className="bg-blue-500 h-3 rounded-full transition-all duration-1000"
                  style={{ 
                    width: `${Math.min((progress.goals.weeklyProgress / progress.goals.weeklyTarget) * 100, 100)}%` 
                  }}
                />
              </div>
              <div className="text-xs text-gray-500">
                还需完成 {Math.max(progress.goals.weeklyTarget - progress.goals.weeklyProgress, 0)} 次考试
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-lg">
              <Target className="h-5 w-5" />
              本月目标
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">进度</span>
                <span className="text-sm font-medium">
                  {progress.goals.monthlyProgress} / {progress.goals.monthlyTarget}
                </span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-3">
                <div
                  className="bg-green-500 h-3 rounded-full transition-all duration-1000"
                  style={{ 
                    width: `${Math.min((progress.goals.monthlyProgress / progress.goals.monthlyTarget) * 100, 100)}%` 
                  }}
                />
              </div>
              <div className="text-xs text-gray-500">
                还需完成 {Math.max(progress.goals.monthlyTarget - progress.goals.monthlyProgress, 0)} 次考试
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Recent Achievements */}
      {progress.achievements.recentBadges.length > 0 && (
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-lg">
              <Award className="h-5 w-5" />
              最新成就
              <Badge variant="outline" className="ml-2">
                {progress.achievements.totalBadges}
              </Badge>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {progress.achievements.recentBadges.map((badge) => (
                <div
                  key={badge.id}
                  className="flex items-start gap-3 p-3 bg-gradient-to-r from-yellow-50 to-yellow-100 rounded-lg border border-yellow-200"
                >
                  <div className="w-10 h-10 bg-yellow-500 rounded-full flex items-center justify-center flex-shrink-0">
                    <Trophy className="h-5 w-5 text-yellow-100" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h4 className="font-medium text-yellow-900 text-sm truncate">
                      {badge.name}
                    </h4>
                    <p className="text-xs text-yellow-700 mt-1">
                      {badge.description}
                    </p>
                    <p className="text-xs text-yellow-600 mt-1">
                      {formatDate(badge.earnedAt, 'MM-DD HH:mm')}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Subject Performance */}
      {showDetailed && progress.subjects.length > 0 && (
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-lg">
              <BarChart3 className="h-5 w-5" />
              科目表现
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {progress.subjects.map((subject, index) => (
                <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                  <div className="flex-1">
                    <div className="flex items-center justify-between mb-2">
                      <h4 className="font-medium text-gray-900">{subject.name}</h4>
                      <div className="flex items-center gap-2">
                        <Badge variant="outline" className="text-xs">
                          #{subject.rank}
                        </Badge>
                        {getTrendIcon(subject.improvement)}
                      </div>
                    </div>
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-gray-600">
                        {subject.completedExams} 次考试
                      </span>
                      <span className={cn('font-medium', getScoreColor(subject.averageScore))}>
                        {subject.averageScore.toFixed(1)}%
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
};