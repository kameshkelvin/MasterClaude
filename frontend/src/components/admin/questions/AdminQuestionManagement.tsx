import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  HelpCircle,
  Plus,
  Search,
  Filter,
  Edit3,
  Trash2,
  Eye,
  Copy,
  Download,
  Upload,
  BookOpen,
  Tag,
  Clock,
  TrendingUp,
  AlertCircle,
  CheckCircle,
  MoreHorizontal,
  FileText,
  Grid,
  List,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { adminApi } from '@/lib/api';
import { formatDate, cn } from '@/lib/utils';
import { QuestionCreateModal } from './QuestionCreateModal';

interface Question {
  id: string;
  title: string;
  content: string;
  type: 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'FILL_BLANK' | 'ESSAY';
  category: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  points: number;
  tags: string[];
  options?: {
    id: string;
    content: string;
    isCorrect: boolean;
  }[];
  correctAnswer?: string;
  explanation?: string;
  usageCount: number;
  averageScore: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

interface QuestionFilters {
  type: string;
  category: string;
  difficulty: string;
  tags: string[];
  search: string;
  sortBy: 'title' | 'createdAt' | 'usageCount' | 'averageScore' | 'difficulty';
  sortOrder: 'asc' | 'desc';
}

export const AdminQuestionManagement: React.FC = () => {
  const [selectedQuestions, setSelectedQuestions] = useState<string[]>([]);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedQuestion, setSelectedQuestion] = useState<Question | null>(null);
  const [viewMode, setViewMode] = useState<'list' | 'grid'>('list');
  const [filters, setFilters] = useState<QuestionFilters>({
    type: 'all',
    category: 'all',
    difficulty: 'all',
    tags: [],
    search: '',
    sortBy: 'createdAt',
    sortOrder: 'desc',
  });

  const queryClient = useQueryClient();

  // Fetch questions data
  const { data: questionsData, isLoading, error } = useQuery({
    queryKey: ['adminQuestions', filters],
    queryFn: () => adminApi.getQuestions(filters),
    select: (response) => response.data.data as { 
      questions: Question[]; 
      total: number; 
      categories: string[]; 
      tags: string[];
      stats: {
        totalQuestions: number;
        easyQuestions: number;
        mediumQuestions: number;
        hardQuestions: number;
        avgUsage: number;
      };
    },
  });

  // Delete question mutation
  const deleteQuestionMutation = useMutation({
    mutationFn: (questionId: string) => adminApi.deleteQuestion(questionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminQuestions'] });
      setSelectedQuestions([]);
    },
  });

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'SINGLE_CHOICE': return <CheckCircle className="h-4 w-4" />;
      case 'MULTIPLE_CHOICE': return <Grid className="h-4 w-4" />;
      case 'TRUE_FALSE': return <MoreHorizontal className="h-4 w-4" />;
      case 'FILL_BLANK': return <Edit3 className="h-4 w-4" />;
      case 'ESSAY': return <FileText className="h-4 w-4" />;
      default: return <HelpCircle className="h-4 w-4" />;
    }
  };

  const getTypeText = (type: string) => {
    switch (type) {
      case 'SINGLE_CHOICE': return '单选题';
      case 'MULTIPLE_CHOICE': return '多选题';
      case 'TRUE_FALSE': return '判断题';
      case 'FILL_BLANK': return '填空题';
      case 'ESSAY': return '问答题';
      default: return '未知';
    }
  };

  const getDifficultyColor = (difficulty: string) => {
    switch (difficulty) {
      case 'EASY': return 'bg-green-100 text-green-800 border-green-200';
      case 'MEDIUM': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'HARD': return 'bg-red-100 text-red-800 border-red-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getDifficultyText = (difficulty: string) => {
    switch (difficulty) {
      case 'EASY': return '简单';
      case 'MEDIUM': return '中等';
      case 'HARD': return '困难';
      default: return '未知';
    }
  };

  const handleDeleteQuestion = (questionId: string) => {
    if (confirm('确定要删除这个题目吗？此操作不可撤销。')) {
      deleteQuestionMutation.mutate(questionId);
    }
  };

  const handleBulkAction = (action: string) => {
    if (selectedQuestions.length === 0) return;
    
    switch (action) {
      case 'delete':
        if (confirm(`确定要删除选中的 ${selectedQuestions.length} 个题目吗？`)) {
          selectedQuestions.forEach(questionId => deleteQuestionMutation.mutate(questionId));
        }
        break;
      case 'export':
        // Handle export functionality
        console.log('Exporting questions:', selectedQuestions);
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
              <div key={i} className="h-32 bg-slate-200 rounded"></div>
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
            无法加载题目数据，请稍后重试
          </p>
        </div>
      </div>
    );
  }

  const { questions = [], total = 0, categories = [], tags = [], stats } = questionsData || {};

  return (
    <div className="p-4 sm:p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-slate-900">
            题目管理
          </h1>
          <p className="text-slate-600 mt-1">
            管理题库中的所有题目，共 {total} 道题目
          </p>
        </div>
        
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => {/* Handle import */}}
          >
            <Upload className="h-4 w-4 mr-1" />
            导入题目
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {/* Handle export */}}
          >
            <Download className="h-4 w-4 mr-1" />
            导出题目
          </Button>
          <Button
            size="sm"
            onClick={() => setShowCreateModal(true)}
            className="bg-slate-700 hover:bg-slate-800"
          >
            <Plus className="h-4 w-4 mr-1" />
            创建题目
          </Button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-blue-100 rounded-lg">
                <HelpCircle className="h-5 w-5 text-blue-600" />
              </div>
              <div>
                <p className="text-sm text-slate-600">总题目数</p>
                <p className="text-xl font-bold text-slate-900">{stats?.totalQuestions || total}</p>
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
                <p className="text-sm text-slate-600">简单</p>
                <p className="text-xl font-bold text-slate-900">{stats?.easyQuestions || 0}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-yellow-100 rounded-lg">
                <AlertCircle className="h-5 w-5 text-yellow-600" />
              </div>
              <div>
                <p className="text-sm text-slate-600">中等</p>
                <p className="text-xl font-bold text-slate-900">{stats?.mediumQuestions || 0}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-red-100 rounded-lg">
                <TrendingUp className="h-5 w-5 text-red-600" />
              </div>
              <div>
                <p className="text-sm text-slate-600">困难</p>
                <p className="text-xl font-bold text-slate-900">{stats?.hardQuestions || 0}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-purple-100 rounded-lg">
                <BookOpen className="h-5 w-5 text-purple-600" />
              </div>
              <div>
                <p className="text-sm text-slate-600">平均使用</p>
                <p className="text-xl font-bold text-slate-900">
                  {stats?.avgUsage?.toFixed(1) || '0.0'}
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
                placeholder="搜索题目标题、内容..."
                leftIcon={<Search className="h-4 w-4" />}
                value={filters.search}
                onChange={(e) => setFilters({ ...filters, search: e.target.value })}
              />
            </div>

            {/* Type Filter */}
            <select
              value={filters.type}
              onChange={(e) => setFilters({ ...filters, type: e.target.value })}
              className="px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500"
            >
              <option value="all">所有类型</option>
              <option value="SINGLE_CHOICE">单选题</option>
              <option value="MULTIPLE_CHOICE">多选题</option>
              <option value="TRUE_FALSE">判断题</option>
              <option value="FILL_BLANK">填空题</option>
              <option value="ESSAY">问答题</option>
            </select>

            {/* Difficulty Filter */}
            <select
              value={filters.difficulty}
              onChange={(e) => setFilters({ ...filters, difficulty: e.target.value })}
              className="px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500"
            >
              <option value="all">所有难度</option>
              <option value="EASY">简单</option>
              <option value="MEDIUM">中等</option>
              <option value="HARD">困难</option>
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

            {/* View Mode Toggle */}
            <div className="flex border border-slate-300 rounded-md">
              <button
                onClick={() => setViewMode('list')}
                className={cn(
                  'px-3 py-2 text-sm',
                  viewMode === 'list' 
                    ? 'bg-slate-100 text-slate-900' 
                    : 'text-slate-600 hover:text-slate-900'
                )}
              >
                <List className="h-4 w-4" />
              </button>
              <button
                onClick={() => setViewMode('grid')}
                className={cn(
                  'px-3 py-2 text-sm border-l border-slate-300',
                  viewMode === 'grid' 
                    ? 'bg-slate-100 text-slate-900' 
                    : 'text-slate-600 hover:text-slate-900'
                )}
              >
                <Grid className="h-4 w-4" />
              </button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Bulk Actions */}
      {selectedQuestions.length > 0 && (
        <Card className="border-blue-200 bg-blue-50">
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <span className="text-sm text-blue-700">
                已选择 {selectedQuestions.length} 道题目
              </span>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleBulkAction('export')}
                  className="text-blue-700 border-blue-300"
                >
                  批量导出
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
                  onClick={() => setSelectedQuestions([])}
                >
                  取消选择
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Question List */}
      <div className={cn(
        'gap-4',
        viewMode === 'grid' 
          ? 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3' 
          : 'space-y-4'
      )}>
        {questions.map((question) => (
          <Card key={question.id} className="hover:shadow-lg transition-shadow">
            <CardContent className="p-4 sm:p-6">
              <div className="flex items-start gap-4">
                {/* Checkbox */}
                <div className="mt-1">
                  <input
                    type="checkbox"
                    checked={selectedQuestions.includes(question.id)}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedQuestions([...selectedQuestions, question.id]);
                      } else {
                        setSelectedQuestions(selectedQuestions.filter(id => id !== question.id));
                      }
                    }}
                    className="rounded border-gray-300 text-slate-600 focus:ring-slate-500"
                  />
                </div>

                {/* Question Info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex-1">
                      <h3 className="text-lg font-semibold text-slate-900 mb-1">
                        {question.title}
                      </h3>
                      <p className="text-sm text-slate-600 mb-2 line-clamp-2">
                        {question.content.length > 100 
                          ? `${question.content.substring(0, 100)}...` 
                          : question.content}
                      </p>
                      <div className="flex items-center gap-4 text-sm text-slate-500">
                        <span className="flex items-center gap-1">
                          {getTypeIcon(question.type)}
                          {getTypeText(question.type)}
                        </span>
                        <span className="flex items-center gap-1">
                          <BookOpen className="h-4 w-4" />
                          {question.category}
                        </span>
                        <span className="flex items-center gap-1">
                          <TrendingUp className="h-4 w-4" />
                          使用 {question.usageCount} 次
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 ml-4">
                      <Badge className={cn('text-xs', getDifficultyColor(question.difficulty))}>
                        {getDifficultyText(question.difficulty)}
                      </Badge>
                      <Badge variant="outline" className="text-xs">
                        {question.points} 分
                      </Badge>
                    </div>
                  </div>

                  {/* Tags */}
                  {question.tags.length > 0 && (
                    <div className="flex flex-wrap gap-1 mb-4">
                      {question.tags.slice(0, 3).map((tag) => (
                        <Badge key={tag} variant="secondary" className="text-xs">
                          <Tag className="h-3 w-3 mr-1" />
                          {tag}
                        </Badge>
                      ))}
                      {question.tags.length > 3 && (
                        <Badge variant="secondary" className="text-xs">
                          +{question.tags.length - 3}
                        </Badge>
                      )}
                    </div>
                  )}

                  {/* Stats */}
                  <div className="flex items-center gap-6 mb-4">
                    <div className="text-center">
                      <p className="text-lg font-semibold text-slate-900">
                        {question.averageScore.toFixed(1)}%
                      </p>
                      <p className="text-xs text-slate-500">平均得分</p>
                    </div>
                    <div className="text-center">
                      <p className="text-lg font-semibold text-slate-900">
                        {question.usageCount}
                      </p>
                      <p className="text-xs text-slate-500">使用次数</p>
                    </div>
                    <div className="text-center">
                      <p className="text-lg font-semibold text-slate-900">
                        {question.points}
                      </p>
                      <p className="text-xs text-slate-500">分值</p>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        setSelectedQuestion(question);
                        setShowEditModal(true);
                      }}
                    >
                      <Edit3 className="h-4 w-4 mr-1" />
                      编辑
                    </Button>
                    
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => window.open(`/admin/questions/${question.id}/preview`, '_blank')}
                    >
                      <Eye className="h-4 w-4 mr-1" />
                      预览
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
                      onClick={() => handleDeleteQuestion(question.id)}
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

        {questions.length === 0 && (
          <Card className="col-span-full">
            <CardContent className="p-12 text-center">
              <HelpCircle className="h-12 w-12 text-slate-400 mx-auto mb-4" />
              <h3 className="text-lg font-semibold text-slate-900 mb-2">
                暂无題目
              </h3>
              <p className="text-slate-600 mb-4">
                还没有创建任何题目，点击下方按钮开始创建
              </p>
              <Button
                onClick={() => setShowCreateModal(true)}
                className="bg-slate-700 hover:bg-slate-800"
              >
                <Plus className="h-4 w-4 mr-1" />
                创建第一道题目
              </Button>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Modals */}
      <QuestionCreateModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSuccess={() => setShowCreateModal(false)}
      />

      {/* TODO: Add QuestionEditModal component */}
    </div>
  );
};