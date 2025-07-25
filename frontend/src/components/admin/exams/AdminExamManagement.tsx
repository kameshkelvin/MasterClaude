import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  FileText,
  Plus,
  Search,
  Filter,
  Edit3,
  Trash2,
  Eye,
  Play,
  Pause,
  Calendar,
  Clock,
  Users,
  MoreHorizontal,
  Copy,
  Download,
  Upload,
  Settings,
  TrendingUp,
  AlertCircle,
  CheckCircle,
  XCircle,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { adminApi } from '@/lib/api';
import { formatDate, cn } from '@/lib/utils';
import { ExamCreateModal } from './ExamCreateModal';
import { ExamEditModal } from './ExamEditModal';

interface Exam {
  id: string;
  title: string;
  description: string;
  status: 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'COMPLETED';
  category: string;
  duration: number; // in minutes
  totalQuestions: number;
  participantCount: number;
  averageScore: number;
  passRate: number;
  createdAt: string;
  updatedAt: string;
  startDate?: string;
  endDate?: string;
  instructions?: string;
  settings: {
    allowReview: boolean;
    showResults: boolean;
    timeLimit: boolean;
    randomizeQuestions: boolean;
    maxAttempts: number;
  };
}

interface ExamFilters {
  status: string;
  category: string;
  search: string;
  sortBy: 'title' | 'createdAt' | 'participantCount' | 'averageScore';
  sortOrder: 'asc' | 'desc';
}

export const AdminExamManagement: React.FC = () => {
  const [selectedExams, setSelectedExams] = useState<string[]>([]);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedExam, setSelectedExam] = useState<Exam | null>(null);
  const [filters, setFilters] = useState<ExamFilters>({
    status: 'all',
    category: 'all',
    search: '',
    sortBy: 'createdAt',
    sortOrder: 'desc',
  });

  const queryClient = useQueryClient();

  // Fetch exams data
  const { data: examsData, isLoading, error } = useQuery({
    queryKey: ['adminExams', filters],
    queryFn: () => adminApi.getExams(filters),
    select: (response) => response.data.data as { exams: Exam[]; total: number; categories: string[] },
  });

  // Delete exam mutation
  const deleteExamMutation = useMutation({
    mutationFn: (examId: string) => adminApi.deleteExam(examId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminExams'] });
      setSelectedExams([]);
    },
  });

  // Publish exam mutation
  const publishExamMutation = useMutation({
    mutationFn: (examId: string) => adminApi.publishExam(examId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminExams'] });
    },
  });

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'bg-green-100 text-green-800 border-green-200';
      case 'DRAFT': return 'bg-gray-100 text-gray-800 border-gray-200';
      case 'PAUSED': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'COMPLETED': return 'bg-blue-100 text-blue-800 border-blue-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'ACTIVE': return <CheckCircle className="h-4 w-4" />;
      case 'DRAFT': return <Edit3 className="h-4 w-4" />;
      case 'PAUSED': return <Pause className="h-4 w-4" />;
      case 'COMPLETED': return <XCircle className="h-4 w-4" />;
      default: return <AlertCircle className="h-4 w-4" />;
    }
  };

  const handleDeleteExam = (examId: string) => {
    if (confirm('确定要删除这个考试吗？此操作不可撤销。')) {
      deleteExamMutation.mutate(examId);
    }
  };

  const handlePublishExam = (examId: string) => {
    if (confirm('确定要发布这个考试吗？发布后学生将可以参加考试。')) {
      publishExamMutation.mutate(examId);
    }
  };

  const handleBulkAction = (action: string) => {
    if (selectedExams.length === 0) return;
    
    switch (action) {
      case 'delete':
        if (confirm(`确定要删除选中的 ${selectedExams.length} 个考试吗？`)) {
          selectedExams.forEach(examId => deleteExamMutation.mutate(examId));
        }
        break;
      case 'publish':
        if (confirm(`确定要发布选中的 ${selectedExams.length} 个考试吗？`)) {
          selectedExams.forEach(examId => publishExamMutation.mutate(examId));
        }
        break;
    }
  };

  if (isLoading) {
    return (
      <div className="p-4 sm:p-6 space-y-6">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-slate-200 rounded w-1/3"></div>
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
            无法加载考试数据，请稍后重试
          </p>
        </div>
      </div>
    );
  }

  const { exams = [], total = 0, categories = [] } = examsData || {};

  return (
    <div className="p-4 sm:p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-slate-900">
            试卷管理
          </h1>
          <p className="text-slate-600 mt-1">
            创建、编辑和管理考试试卷，共 {total} 个试卷
          </p>
        </div>
        
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => {/* Handle import */}}
          >
            <Upload className="h-4 w-4 mr-1" />
            导入试卷
          </Button>
          <Button
            size="sm"
            onClick={() => setShowCreateModal(true)}
            className="bg-slate-700 hover:bg-slate-800"
          >
            <Plus className="h-4 w-4 mr-1" />
            创建试卷
          </Button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-blue-100 rounded-lg">
                <FileText className="h-5 w-5 text-blue-600" />
              </div>
              <div>
                <p className="text-sm text-slate-600">总试卷数</p>
                <p className="text-xl font-bold text-slate-900">{total}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-green-100 rounded-lg">
                <CheckCircle className="h-5 w-5 text-green-600" />
              </div>
              <div>
                <p className="text-sm text-slate-600">已发布</p>
                <p className="text-xl font-bold text-slate-900">
                  {exams.filter(e => e.status === 'ACTIVE').length}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-yellow-100 rounded-lg">
                <Edit3 className="h-5 w-5 text-yellow-600" />
              </div>
              <div>
                <p className="text-sm text-slate-600">草稿</p>
                <p className="text-xl font-bold text-slate-900">
                  {exams.filter(e => e.status === 'DRAFT').length}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-purple-100 rounded-lg">
                <TrendingUp className="h-5 w-5 text-purple-600" />
              </div>
              <div>
                <p className="text-sm text-slate-600">平均分</p>
                <p className="text-xl font-bold text-slate-900">
                  {exams.length > 0 ? 
                    (exams.reduce((acc, e) => acc + e.averageScore, 0) / exams.length).toFixed(1)
                    : '0.0'
                  }%
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Filters and Search */}
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-col sm:flex-row gap-4">
            {/* Search */}
            <div className="flex-1">
              <Input
                placeholder="搜索试卷名称、描述..."
                leftIcon={<Search className="h-4 w-4" />}
                value={filters.search}
                onChange={(e) => setFilters({ ...filters, search: e.target.value })}
              />
            </div>

            {/* Status Filter */}
            <select
              value={filters.status}
              onChange={(e) => setFilters({ ...filters, status: e.target.value })}
              className="px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500"
            >
              <option value="all">所有状态</option>
              <option value="DRAFT">草稿</option>
              <option value="ACTIVE">已发布</option>
              <option value="PAUSED">已暂停</option>
              <option value="COMPLETED">已完成</option>
            </select>

            {/* Category Filter */}
            <select
              value={filters.category}
              onChange={(e) => setFilters({ ...filters, category: e.target.value })}
              className="px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500"
            >
              <option value="all">所有分类</option>
              {categories.map(category => (
                <option key={category} value={category}>{category}</option>
              ))}
            </select>

            {/* Sort */}
            <select
              value={`${filters.sortBy}-${filters.sortOrder}`}
              onChange={(e) => {
                const [sortBy, sortOrder] = e.target.value.split('-') as [typeof filters.sortBy, typeof filters.sortOrder];
                setFilters({ ...filters, sortBy, sortOrder });
              }}
              className="px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500"
            >
              <option value="createdAt-desc">最新创建</option>
              <option value="createdAt-asc">最早创建</option>
              <option value="title-asc">名称 A-Z</option>
              <option value="title-desc">名称 Z-A</option>
              <option value="participantCount-desc">参与人数最多</option>
              <option value="averageScore-desc">平均分最高</option>
            </select>
          </div>
        </CardContent>
      </Card>

      {/* Bulk Actions */}
      {selectedExams.length > 0 && (
        <Card className="border-blue-200 bg-blue-50">
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <span className="text-sm text-blue-700">
                已选择 {selectedExams.length} 个试卷
              </span>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleBulkAction('publish')}
                  className="text-blue-700 border-blue-300"
                >
                  批量发布
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleBulkAction('delete')}
                  className="text-red-700 border-red-300"
                >
                  批量删除
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setSelectedExams([])}
                >
                  取消选择
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Exam List */}
      <div className="space-y-4">
        {exams.map((exam) => (
          <Card key={exam.id} className="hover:shadow-lg transition-shadow">
            <CardContent className="p-4 sm:p-6">
              <div className="flex items-start gap-4">
                {/* Checkbox */}
                <div className="mt-1">
                  <input
                    type="checkbox"
                    checked={selectedExams.includes(exam.id)}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedExams([...selectedExams, exam.id]);
                      } else {
                        setSelectedExams(selectedExams.filter(id => id !== exam.id));
                      }
                    }}
                    className="rounded border-gray-300 text-slate-600 focus:ring-slate-500"
                  />
                </div>

                {/* Exam Info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex-1">
                      <h3 className="text-lg font-semibold text-slate-900 mb-1">
                        {exam.title}
                      </h3>
                      <p className="text-sm text-slate-600 mb-2 line-clamp-2">
                        {exam.description}
                      </p>
                      <div className="flex items-center gap-4 text-sm text-slate-500">
                        <span className="flex items-center gap-1">
                          <Calendar className="h-4 w-4" />
                          {formatDate(exam.createdAt, 'MM-DD HH:mm')}
                        </span>
                        <span className="flex items-center gap-1">
                          <Clock className="h-4 w-4" />
                          {exam.duration} 分钟
                        </span>
                        <span className="flex items-center gap-1">
                          <FileText className="h-4 w-4" />
                          {exam.totalQuestions} 题
                        </span>
                        <span className="flex items-center gap-1">
                          <Users className="h-4 w-4" />
                          {exam.participantCount} 人参与
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 ml-4">
                      <Badge className={cn('text-xs', getStatusColor(exam.status))}>
                        <span className="flex items-center gap-1">
                          {getStatusIcon(exam.status)}
                          {exam.status === 'ACTIVE' ? '已发布' :
                           exam.status === 'DRAFT' ? '草稿' :
                           exam.status === 'PAUSED' ? '已暂停' : '已完成'}
                        </span>
                      </Badge>
                    </div>
                  </div>

                  {/* Stats */}
                  <div className="flex items-center gap-6 mb-4">
                    <div className="text-center">
                      <p className="text-lg font-semibold text-slate-900">
                        {exam.averageScore.toFixed(1)}%
                      </p>
                      <p className="text-xs text-slate-500">平均分</p>
                    </div>
                    <div className="text-center">
                      <p className="text-lg font-semibold text-slate-900">
                        {exam.passRate.toFixed(1)}%
                      </p>
                      <p className="text-xs text-slate-500">通过率</p>
                    </div>
                    <div className="text-center">
                      <p className="text-lg font-semibold text-slate-900">
                        {exam.participantCount}
                      </p>
                      <p className="text-xs text-slate-500">参与人数</p>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        setSelectedExam(exam);
                        setShowEditModal(true);
                      }}
                    >
                      <Edit3 className="h-4 w-4 mr-1" />
                      编辑
                    </Button>
                    
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => window.open(`/admin/exams/${exam.id}/preview`, '_blank')}
                    >
                      <Eye className="h-4 w-4 mr-1" />
                      预览
                    </Button>

                    {exam.status === 'DRAFT' && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handlePublishExam(exam.id)}
                        className="text-green-700 border-green-300"
                      >
                        <Play className="h-4 w-4 mr-1" />
                        发布
                      </Button>
                    )}

                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => window.open(`/admin/exams/${exam.id}/analytics`, '_blank')}
                    >
                      <TrendingUp className="h-4 w-4 mr-1" />
                      分析
                    </Button>

                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {/* Handle copy */}}
                    >
                      <Copy className="h-4 w-4 mr-1" />
                      复制
                    </Button>

                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleDeleteExam(exam.id)}
                      className="text-red-700 border-red-300"
                    >
                      <Trash2 className="h-4 w-4 mr-1" />
                      删除
                    </Button>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}

        {exams.length === 0 && (
          <Card>
            <CardContent className="p-12 text-center">
              <FileText className="h-12 w-12 text-slate-400 mx-auto mb-4" />
              <h3 className="text-lg font-semibold text-slate-900 mb-2">
                暂无试卷
              </h3>
              <p className="text-slate-600 mb-4">
                还没有创建任何试卷，点击下方按钮开始创建
              </p>
              <Button
                onClick={() => setShowCreateModal(true)}
                className="bg-slate-700 hover:bg-slate-800"
              >
                <Plus className="h-4 w-4 mr-1" />
                创建第一个试卷
              </Button>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Modals */}
      <ExamCreateModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSuccess={() => setShowCreateModal(false)}
      />

      <ExamEditModal
        isOpen={showEditModal}
        exam={selectedExam}
        onClose={() => {
          setShowEditModal(false);
          setSelectedExam(null);
        }}
        onSuccess={() => {
          setShowEditModal(false);
          setSelectedExam(null);
        }}
      />
    </div>
  );
};