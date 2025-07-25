import React from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  BookOpen,
  Trophy,
  Clock,
  TrendingUp,
  Calendar,
  ArrowRight,
  Award,
  Target,
  Users,
  BarChart3,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { ExamCard } from '@/components/exam/ExamCard';
import { ProgressTracker } from '@/components/progress/ProgressTracker';
import { examApi, userApi } from '@/lib/api';
import { useAuth } from '@/hooks/useAuth';
import { formatDate, cn } from '@/lib/utils';

export const Dashboard: React.FC = () => {
  const { user } = useAuth();

  // Fetch dashboard data
  const { data: dashboardData, isLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => userApi.getDashboard(),
    select: (response) => response.data.data,
  });

  const { data: upcomingExams } = useQuery({
    queryKey: ['exams', 'upcoming'],
    queryFn: () => examApi.getExams({ status: 'SCHEDULED', limit: 3 }),
    select: (response) => response.data.data.exams,
  });

  const { data: recentResults } = useQuery({
    queryKey: ['results', 'recent'],
    queryFn: () => userApi.getRecentResults(3),
    select: (response) => response.data.data,
  });

  if (isLoading) {
    return (
      <div className="container-responsive py-6 space-y-6">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-gray-200 rounded w-1/3"></div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="h-24 bg-gray-200 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  const stats = dashboardData?.stats || {
    totalExams: 0,
    completedExams: 0,
    averageScore: 0,
    totalTime: 0,
  };

  return (
    <div className="container-responsive py-4 sm:py-6 spacing-responsive">
      {/* Welcome section */}
      <div className="mb-6 sm:mb-8">
        <h1 className="text-responsive-xl font-bold text-gray-900 mb-2">
          欢迎回来，{user?.name || '同学'}！
        </h1>
        <p className="text-gray-600">
          今天是 {formatDate(new Date(), 'YYYY年MM月DD日')}，准备好新的挑战了吗？
        </p>
      </div>

      {/* Stats grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-6 mb-6 sm:mb-8">
        <Card className="card-responsive">
          <CardContent className="p-4 sm:p-6 text-center">
            <div className="mb-2 sm:mb-3">
              <BookOpen className="h-6 w-6 sm:h-8 sm:w-8 text-blue-500 mx-auto" />
            </div>
            <div className="text-xl sm:text-2xl font-bold text-blue-600 mb-1">
              {stats.totalExams}
            </div>
            <div className="text-xs sm:text-sm text-gray-500">参加考试</div>
          </CardContent>
        </Card>

        <Card className="card-responsive">
          <CardContent className="p-4 sm:p-6 text-center">
            <div className="mb-2 sm:mb-3">
              <Trophy className="h-6 w-6 sm:h-8 sm:w-8 text-yellow-500 mx-auto" />
            </div>
            <div className="text-xl sm:text-2xl font-bold text-yellow-600 mb-1">
              {stats.completedExams}
            </div>
            <div className="text-xs sm:text-sm text-gray-500">已完成</div>
          </CardContent>
        </Card>

        <Card className="card-responsive">
          <CardContent className="p-4 sm:p-6 text-center">
            <div className="mb-2 sm:mb-3">
              <Target className="h-6 w-6 sm:h-8 sm:w-8 text-green-500 mx-auto" />
            </div>
            <div className="text-xl sm:text-2xl font-bold text-green-600 mb-1">
              {stats.averageScore?.toFixed(1) || '0'}%
            </div>
            <div className="text-xs sm:text-sm text-gray-500">平均分</div>
          </CardContent>
        </Card>

        <Card className="card-responsive">
          <CardContent className="p-4 sm:p-6 text-center">
            <div className="mb-2 sm:mb-3">
              <Clock className="h-6 w-6 sm:h-8 sm:w-8 text-purple-500 mx-auto" />
            </div>
            <div className="text-xl sm:text-2xl font-bold text-purple-600 mb-1">
              {Math.round((stats.totalTime || 0) / 60)}
            </div>
            <div className="text-xs sm:text-sm text-gray-500">总时长(分)</div>
          </CardContent>
        </Card>
      </div>

      {/* Quick actions - mobile optimized */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6 sm:mb-8">
        <Button asChild className="btn-mobile h-auto p-4 flex-col">
          <Link to="/exams">
            <BookOpen className="h-6 w-6 mb-2" />
            <span className="text-sm">开始考试</span>
          </Link>
        </Button>
        
        <Button asChild variant="outline" className="btn-mobile h-auto p-4 flex-col">
          <Link to="/history">
            <BarChart3 className="h-6 w-6 mb-2" />
            <span className="text-sm">查看历史</span>
          </Link>
        </Button>
        
        <Button asChild variant="outline" className="btn-mobile h-auto p-4 flex-col">
          <Link to="/profile">
            <Users className="h-6 w-6 mb-2" />
            <span className="text-sm">个人资料</span>
          </Link>
        </Button>
        
        <Button asChild variant="outline" className="btn-mobile h-auto p-4 flex-col">
          <Link to="/exams?filter=practice">
            <Award className="h-6 w-6 mb-2" />
            <span className="text-sm">练习模式</span>
          </Link>
        </Button>
      </div>

      {/* Upcoming exams */}
      {upcomingExams && upcomingExams.length > 0 && (
        <Card className="mb-6 sm:mb-8">
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2 text-responsive-lg">
                <Calendar className="h-5 w-5" />
                即将开始的考试
              </CardTitle>
              <Button variant="ghost" size="sm" asChild>
                <Link to="/exams" className="flex items-center gap-1">
                  查看全部
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </Button>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {upcomingExams.map((exam) => (
              <ExamCard
                key={exam.id}
                exam={exam}
                variant="compact"
                className="border border-gray-100"
              />
            ))}
          </CardContent>
        </Card>
      )}

      {/* Recent results */}
      {recentResults && recentResults.length > 0 && (
        <Card className="mb-6 sm:mb-8">
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2 text-responsive-lg">
                <TrendingUp className="h-5 w-5" />
                最近成绩
              </CardTitle>
              <Button variant="ghost" size="sm" asChild>
                <Link to="/history" className="flex items-center gap-1">
                  查看全部
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {recentResults.map((result) => (
                <div
                  key={result.id}
                  className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                >
                  <div className="flex-1 min-w-0">
                    <h4 className="font-medium text-gray-900 truncate">
                      {result.examTitle}
                    </h4>
                    <p className="text-sm text-gray-500">
                      {formatDate(result.submitTime, 'YYYY-MM-DD HH:mm')}
                    </p>
                  </div>
                  <div className="text-right ml-4">
                    <div className={cn(
                      'text-lg font-bold',
                      result.score >= 80 ? 'text-green-600' :
                      result.score >= 60 ? 'text-yellow-600' : 'text-red-600'
                    )}>
                      {result.score}%
                    </div>
                    <Badge 
                      variant={result.isPassed ? 'success' : 'error'}
                      className="text-xs"
                    >
                      {result.isPassed ? '通过' : '未通过'}
                    </Badge>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Progress Tracker */}
      <ProgressTracker showDetailed={false} />

      {/* No data state */}
      {(!upcomingExams || upcomingExams.length === 0) && 
       (!recentResults || recentResults.length === 0) && (
        <Card className="text-center py-8 sm:py-12">
          <CardContent>
            <BookOpen className="h-12 w-12 sm:h-16 sm:w-16 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg sm:text-xl font-semibold text-gray-900 mb-2">
              开始您的学习之旅
            </h3>
            <p className="text-gray-600 mb-6 max-w-md mx-auto">
              这里还没有考试记录。立即开始您的第一次考试，开启学习之旅！
            </p>
            <Button size="lg" asChild className="btn-mobile">
              <Link to="/exams">
                <BookOpen className="h-5 w-5 mr-2" />
                浏览考试
              </Link>
            </Button>
          </CardContent>
        </Card>
      )}
    </div>
  );
};