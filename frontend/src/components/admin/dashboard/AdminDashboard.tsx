import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Users,
  FileText,
  BarChart3,
  TrendingUp,
  TrendingDown,
  Clock,
  CheckCircle,
  AlertTriangle,
  Calendar,
  BookOpen,
  Award,
  Activity,
  Eye,
  Download,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { adminApi } from '@/lib/api';
import { formatDate, cn } from '@/lib/utils';

interface DashboardStats {
  overview: {
    totalUsers: number;
    totalExams: number;
    totalQuestions: number;
    totalAttempts: number;
    activeExams: number;
    pendingGrades: number;
  };
  trends: {
    userGrowth: number;
    examGrowth: number;
    averageScore: number;
    completionRate: number;
  };
  recent: {
    newUsers: Array<{
      id: string;
      name: string;
      email: string;
      registeredAt: string;
    }>;
    recentExams: Array<{
      id: string;
      title: string;
      status: string;
      participants: number;
      createdAt: string;
    }>;
    pendingGrades: Array<{
      id: string;
      examTitle: string;
      studentName: string;
      submittedAt: string;
      status: string;
    }>;
  };
  systemHealth: {
    serverStatus: 'healthy' | 'warning' | 'error';
    databaseStatus: 'healthy' | 'warning' | 'error';
    lastBackup: string;
    diskUsage: number;
    memoryUsage: number;
  };
}

export const AdminDashboard: React.FC = () => {
  const [timeRange, setTimeRange] = useState<'7d' | '30d' | '90d'>('30d');

  // Fetch dashboard data
  const { data: dashboardData, isLoading, error } = useQuery({
    queryKey: ['adminDashboard', timeRange],
    queryFn: () => adminApi.getDashboard(),
    select: (response) => response.data.data as DashboardStats,
    refetchInterval: 60000, // Refresh every minute
  });

  if (isLoading) {
    return (
      <div className="p-6 space-y-6">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-slate-200 rounded w-1/3"></div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="h-32 bg-slate-200 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (error || !dashboardData) {
    return (
      <div className="p-6">
        <div className="text-center py-12">
          <AlertTriangle className="h-12 w-12 text-red-500 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-slate-900 mb-2">
            数据加载失败
          </h3>
          <p className="text-slate-600">
            无法加载仪表板数据，请稍后重试
          </p>
        </div>
      </div>
    );
  }

  const { overview, trends, recent, systemHealth } = dashboardData;

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'healthy': return 'text-green-500';
      case 'warning': return 'text-yellow-500';
      case 'error': return 'text-red-500';
      default: return 'text-slate-500';
    }
  };

  const getTrendIcon = (value: number) => {
    if (value > 0) return <TrendingUp className="h-4 w-4 text-green-500" />;
    if (value < 0) return <TrendingDown className="h-4 w-4 text-red-500" />;
    return <Activity className="h-4 w-4 text-slate-500" />;
  };

  return (
    <div className="p-4 sm:p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-slate-900">
            管理员仪表板
          </h1>
          <p className="text-slate-600 mt-1">
            系统概览和关键指标
          </p>
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
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        <Card>
          <CardContent className="p-4 sm:p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-600">总用户数</p>
                <p className="text-2xl font-bold text-slate-900">
                  {overview.totalUsers.toLocaleString()}
                </p>
                <div className="flex items-center gap-1 mt-1">
                  {getTrendIcon(trends.userGrowth)}
                  <span className={cn(
                    'text-sm',
                    trends.userGrowth > 0 ? 'text-green-600' : 
                    trends.userGrowth < 0 ? 'text-red-600' : 'text-slate-600'
                  )}>
                    {trends.userGrowth > 0 ? '+' : ''}{trends.userGrowth}%
                  </span>
                </div>
              </div>
              <div className="p-3 bg-blue-100 rounded-full">
                <Users className="h-6 w-6 text-blue-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 sm:p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-600">考试总数</p>
                <p className="text-2xl font-bold text-slate-900">
                  {overview.totalExams}
                </p>
                <div className="flex items-center gap-1 mt-1">
                  {getTrendIcon(trends.examGrowth)}
                  <span className={cn(
                    'text-sm',
                    trends.examGrowth > 0 ? 'text-green-600' : 
                    trends.examGrowth < 0 ? 'text-red-600' : 'text-slate-600'
                  )}>
                    {trends.examGrowth > 0 ? '+' : ''}{trends.examGrowth}%
                  </span>
                </div>
              </div>
              <div className="p-3 bg-green-100 rounded-full">
                <FileText className="h-6 w-6 text-green-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 sm:p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-600">题目总数</p>
                <p className="text-2xl font-bold text-slate-900">
                  {overview.totalQuestions.toLocaleString()}
                </p>
                <div className="flex items-center gap-1 mt-1">
                  <BookOpen className="h-4 w-4 text-slate-500" />
                  <span className="text-sm text-slate-600">
                    题库资源
                  </span>
                </div>
              </div>
              <div className="p-3 bg-purple-100 rounded-full">
                <BookOpen className="h-6 w-6 text-purple-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 sm:p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-600">平均分</p>
                <p className="text-2xl font-bold text-slate-900">
                  {trends.averageScore.toFixed(1)}%
                </p>
                <div className="flex items-center gap-1 mt-1">
                  <Award className="h-4 w-4 text-slate-500" />
                  <span className="text-sm text-slate-600">
                    完成率 {trends.completionRate.toFixed(1)}%
                  </span>
                </div>
              </div>
              <div className="p-3 bg-yellow-100 rounded-full">
                <BarChart3 className="h-6 w-6 text-yellow-600" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* System Health */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Activity className="h-5 w-5" />
            系统状态
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="flex items-center justify-between p-3 bg-slate-50 rounded-lg">
              <div>
                <p className="text-sm font-medium text-slate-600">服务器状态</p>
                <p className={cn('font-semibold', getStatusColor(systemHealth.serverStatus))}>
                  {systemHealth.serverStatus === 'healthy' ? '正常' : 
                   systemHealth.serverStatus === 'warning' ? '警告' : '错误'}
                </p>
              </div>
              <div className={cn('w-3 h-3 rounded-full', 
                systemHealth.serverStatus === 'healthy' ? 'bg-green-500' :
                systemHealth.serverStatus === 'warning' ? 'bg-yellow-500' : 'bg-red-500'
              )} />
            </div>

            <div className="flex items-center justify-between p-3 bg-slate-50 rounded-lg">
              <div>
                <p className="text-sm font-medium text-slate-600">数据库状态</p>
                <p className={cn('font-semibold', getStatusColor(systemHealth.databaseStatus))}>
                  {systemHealth.databaseStatus === 'healthy' ? '正常' : 
                   systemHealth.databaseStatus === 'warning' ? '警告' : '错误'}
                </p>
              </div>
              <div className={cn('w-3 h-3 rounded-full',
                systemHealth.databaseStatus === 'healthy' ? 'bg-green-500' :
                systemHealth.databaseStatus === 'warning' ? 'bg-yellow-500' : 'bg-red-500'
              )} />
            </div>

            <div className="flex items-center justify-between p-3 bg-slate-50 rounded-lg">
              <div>
                <p className="text-sm font-medium text-slate-600">磁盘使用率</p>
                <p className="font-semibold text-slate-900">
                  {systemHealth.diskUsage}%
                </p>
              </div>
              <div className="w-12 h-2 bg-slate-200 rounded-full overflow-hidden">
                <div 
                  className={cn(
                    'h-full transition-all',
                    systemHealth.diskUsage < 70 ? 'bg-green-500' :
                    systemHealth.diskUsage < 85 ? 'bg-yellow-500' : 'bg-red-500'
                  )}
                  style={{ width: `${systemHealth.diskUsage}%` }}
                />
              </div>
            </div>

            <div className="flex items-center justify-between p-3 bg-slate-50 rounded-lg">
              <div>
                <p className="text-sm font-medium text-slate-600">内存使用率</p>
                <p className="font-semibold text-slate-900">
                  {systemHealth.memoryUsage}%
                </p>
              </div>
              <div className="w-12 h-2 bg-slate-200 rounded-full overflow-hidden">
                <div 
                  className={cn(
                    'h-full transition-all',
                    systemHealth.memoryUsage < 70 ? 'bg-green-500' :
                    systemHealth.memoryUsage < 85 ? 'bg-yellow-500' : 'bg-red-500'
                  )}
                  style={{ width: `${systemHealth.memoryUsage}%` }}
                />
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Recent Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Exams */}
        <Card>
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2 text-lg">
                <FileText className="h-5 w-5" />
                最近考试
              </CardTitle>
              <Button variant="ghost" size="sm">
                <Eye className="h-4 w-4 mr-1" />
                查看全部
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {recent.recentExams.map((exam) => (
                <div key={exam.id} className="flex items-center justify-between p-3 bg-slate-50 rounded-lg">
                  <div className="flex-1 min-w-0">
                    <h4 className="font-medium text-slate-900 truncate">
                      {exam.title}
                    </h4>
                    <div className="flex items-center gap-3 mt-1 text-sm text-slate-500">
                      <span>{exam.participants} 人参与</span>
                      <span>{formatDate(exam.createdAt, 'MM-DD HH:mm')}</span>
                    </div>
                  </div>
                  <Badge 
                    variant={exam.status === 'ACTIVE' ? 'success' : 'outline'}
                    className="text-xs"
                  >
                    {exam.status === 'ACTIVE' ? '进行中' : 
                     exam.status === 'DRAFT' ? '草稿' : '已结束'}
                  </Badge>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* New Users */}
        <Card>
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2 text-lg">
                <Users className="h-5 w-5" />
                新注册用户
              </CardTitle>
              <Button variant="ghost" size="sm">
                <Eye className="h-4 w-4 mr-1" />
                查看全部
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {recent.newUsers.map((user) => (
                <div key={user.id} className="flex items-center gap-3 p-3 bg-slate-50 rounded-lg">
                  <div className="w-10 h-10 bg-slate-200 rounded-full flex items-center justify-center">
                    <span className="text-sm font-medium text-slate-700">
                      {user.name.charAt(0)}
                    </span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <h4 className="font-medium text-slate-900 truncate">
                      {user.name}
                    </h4>
                    <div className="flex items-center gap-3 mt-1 text-sm text-slate-500">
                      <span className="truncate">{user.email}</span>
                      <span>{formatDate(user.registeredAt, 'MM-DD')}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle>快速操作</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <Button 
              variant="outline" 
              className="h-auto p-4 flex-col gap-2"
              onClick={() => window.open('/admin/exams/new', '_blank')}
            >
              <FileText className="h-6 w-6" />
              <span className="text-sm">创建考试</span>
            </Button>
            
            <Button 
              variant="outline" 
              className="h-auto p-4 flex-col gap-2"
              onClick={() => window.open('/admin/questions/new', '_blank')}
            >
              <BookOpen className="h-6 w-6" />
              <span className="text-sm">添加题目</span>
            </Button>
            
            <Button 
              variant="outline" 
              className="h-auto p-4 flex-col gap-2"
              onClick={() => window.open('/admin/grades', '_blank')}
            >
              <BarChart3 className="h-6 w-6" />
              <span className="text-sm">查看成绩</span>
            </Button>
            
            <Button 
              variant="outline" 
              className="h-auto p-4 flex-col gap-2"
              onClick={() => {
                // Export functionality
                console.log('Export system report');
              }}
            >
              <Download className="h-6 w-6" />
              <span className="text-sm">导出报告</span>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};