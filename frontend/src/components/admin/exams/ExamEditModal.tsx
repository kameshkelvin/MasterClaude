import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  X,
  Calendar,
  Clock,
  FileText,
  Settings,
  Users,
  AlertCircle,
  Save,
  Eye,
  Play,
  Pause,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { adminApi } from '@/lib/api';

const examEditSchema = z.object({
  title: z.string().min(1, '试卷标题不能为空').max(100, '标题不能超过100个字符'),
  description: z.string().min(1, '试卷描述不能为空').max(500, '描述不能超过500个字符'),
  category: z.string().min(1, '请选择试卷分类'),
  duration: z.number().min(1, '考试时长至少1分钟').max(480, '考试时长不能超过8小时'),
  instructions: z.string().optional(),
  startDate: z.string().optional(),
  endDate: z.string().optional(),
  settings: z.object({
    allowReview: z.boolean(),
    showResults: z.boolean(),
    timeLimit: z.boolean(),
    randomizeQuestions: z.boolean(),
    maxAttempts: z.number().min(1).max(10),
    passScore: z.number().min(0).max(100),
  }),
});

type ExamEditForm = z.infer<typeof examEditSchema>;

interface Exam {
  id: string;
  title: string;
  description: string;
  status: 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'COMPLETED';
  category: string;
  duration: number;
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
    passScore: number;
  };
}

interface ExamEditModalProps {
  isOpen: boolean;
  exam: Exam | null;
  onClose: () => void;
  onSuccess?: () => void;
}

export const ExamEditModal: React.FC<ExamEditModalProps> = ({
  isOpen,
  exam,
  onClose,
  onSuccess,
}) => {
  const [activeTab, setActiveTab] = useState<'basic' | 'settings' | 'questions' | 'analytics'>('basic');
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
    watch,
    setValue,
    reset,
  } = useForm<ExamEditForm>({
    resolver: zodResolver(examEditSchema),
  });

  const settings = watch('settings');

  // Update exam mutation
  const updateExamMutation = useMutation({
    mutationFn: (data: ExamEditForm) => 
      adminApi.updateExam(exam!.id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminExams'] });
      onSuccess?.();
      onClose();
    },
  });

  // Publish exam mutation
  const publishExamMutation = useMutation({
    mutationFn: () => adminApi.publishExam(exam!.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminExams'] });
      onSuccess?.();
    },
  });

  // Load exam data when modal opens
  useEffect(() => {
    if (exam && isOpen) {
      reset({
        title: exam.title,
        description: exam.description,
        category: exam.category,
        duration: exam.duration,
        instructions: exam.instructions || '',
        startDate: exam.startDate || '',
        endDate: exam.endDate || '',
        settings: exam.settings,
      });
    }
  }, [exam, isOpen, reset]);

  const onSubmit = (data: ExamEditForm) => {
    updateExamMutation.mutate(data);
  };

  const handlePublish = () => {
    if (confirm('确定要发布这个考试吗？发布后学生将可以参加考试。')) {
      publishExamMutation.mutate();
    }
  };

  const categories = [
    '计算机科学',
    '数学',
    '英语',
    '物理',
    '化学',
    '生物',
    '历史',
    '地理',
    '政治',
    '语文',
    '其他',
  ];

  if (!isOpen || !exam) return null;

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'bg-green-100 text-green-800 border-green-200';
      case 'DRAFT': return 'bg-gray-100 text-gray-800 border-gray-200';
      case 'PAUSED': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'COMPLETED': return 'bg-blue-100 text-blue-800 border-blue-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'ACTIVE': return '已发布';
      case 'DRAFT': return '草稿';
      case 'PAUSED': return '已暂停';
      case 'COMPLETED': return '已完成';
      default: return '未知';
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-hidden">
        {/* Header */}
        <div className="bg-slate-50 border-b border-slate-200 p-4 sm:p-6">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-xl font-bold text-slate-900">
                编辑试卷
              </h2>
              <div className="flex items-center gap-3 mt-2">
                <p className="text-sm text-slate-600">
                  {exam.title}
                </p>
                <Badge className={getStatusColor(exam.status)}>
                  {getStatusText(exam.status)}
                </Badge>
              </div>
            </div>
            <div className="flex items-center gap-2">
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
                  onClick={handlePublish}
                  loading={publishExamMutation.isPending}
                  className="text-green-700 border-green-300"
                >
                  <Play className="h-4 w-4 mr-1" />
                  发布
                </Button>
              )}
              <Button
                variant="ghost"
                size="sm"
                onClick={onClose}
                className="text-slate-400 hover:text-slate-600"
              >
                <X className="h-5 w-5" />
              </Button>
            </div>
          </div>

          {/* Tabs */}
          <div className="flex space-x-1 bg-slate-100 p-1 rounded-lg">
            {[
              { id: 'basic', label: '基本信息', icon: FileText },
              { id: 'settings', label: '考试设置', icon: Settings },
              { id: 'questions', label: '题目管理', icon: FileText },
              { id: 'analytics', label: '数据分析', icon: Users },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id as any)}
                className={`flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-md transition-colors ${
                  activeTab === tab.id
                    ? 'bg-white text-slate-900 shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <tab.icon className="h-4 w-4" />
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        {/* Content */}
        <div className="p-4 sm:p-6 overflow-y-auto max-h-[calc(90vh-200px)]">
          {activeTab === 'basic' && (
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="space-y-4">
                  <div>
                    <Input
                      label="试卷标题"
                      placeholder="请输入试卷标题"
                      {...register('title')}
                      error={errors.title?.message}
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      试卷分类 <span className="text-red-500">*</span>
                    </label>
                    <select
                      {...register('category')}
                      className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500 focus:border-slate-500"
                    >
                      <option value="">请选择分类</option>
                      {categories.map((category) => (
                        <option key={category} value={category}>
                          {category}
                        </option>
                      ))}
                    </select>
                    {errors.category && (
                      <p className="mt-1 text-sm text-red-600">
                        {errors.category.message}
                      </p>
                    )}
                  </div>

                  <div>
                    <Input
                      label="考试时长（分钟）"
                      type="number"
                      min="1"
                      max="480"
                      leftIcon={<Clock className="h-4 w-4" />}
                      {...register('duration', { valueAsNumber: true })}
                      error={errors.duration?.message}
                      required
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Input
                        label="开始时间"
                        type="datetime-local"
                        leftIcon={<Calendar className="h-4 w-4" />}
                        {...register('startDate')}
                        error={errors.startDate?.message}
                      />
                    </div>

                    <div>
                      <Input
                        label="结束时间"
                        type="datetime-local"
                        leftIcon={<Calendar className="h-4 w-4" />}
                        {...register('endDate')}
                        error={errors.endDate?.message}
                      />
                    </div>
                  </div>
                </div>

                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      试卷描述 <span className="text-red-500">*</span>
                    </label>
                    <textarea
                      {...register('description')}
                      placeholder="请输入试卷描述"
                      rows={4}
                      className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500 focus:border-slate-500"
                    />
                    {errors.description && (
                      <p className="mt-1 text-sm text-red-600">
                        {errors.description.message}
                      </p>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      考试说明
                    </label>
                    <textarea
                      {...register('instructions')}
                      placeholder="请输入考试说明和注意事项"
                      rows={4}
                      className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500 focus:border-slate-500"
                    />
                  </div>

                  {/* Exam Stats */}
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-base">试卷统计</CardTitle>
                    </CardHeader>
                    <CardContent className="pt-0">
                      <div className="grid grid-cols-2 gap-4 text-sm">
                        <div>
                          <span className="text-slate-600">题目数量：</span>
                          <span className="font-medium">{exam.totalQuestions}</span>
                        </div>
                        <div>
                          <span className="text-slate-600">参与人数：</span>
                          <span className="font-medium">{exam.participantCount}</span>
                        </div>
                        <div>
                          <span className="text-slate-600">平均分：</span>
                          <span className="font-medium">{exam.averageScore.toFixed(1)}%</span>
                        </div>
                        <div>
                          <span className="text-slate-600">通过率：</span>
                          <span className="font-medium">{exam.passRate.toFixed(1)}%</span>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                </div>
              </div>

              {/* Error Display */}
              {updateExamMutation.error && (
                <div className="bg-red-50 border border-red-200 rounded-md p-3">
                  <div className="flex items-center gap-2">
                    <AlertCircle className="h-4 w-4 text-red-500" />
                    <p className="text-sm text-red-600">
                      更新失败：{(updateExamMutation.error as any)?.response?.data?.message || '未知错误'}
                    </p>
                  </div>
                </div>
              )}

              {/* Actions */}
              <div className="flex items-center justify-end gap-2 pt-4 border-t border-slate-200">
                <Button
                  type="button"
                  variant="ghost"
                  onClick={onClose}
                >
                  取消
                </Button>
                <Button
                  type="submit"
                  loading={updateExamMutation.isPending}
                  loadingText="保存中..."
                  disabled={!isDirty}
                  className="bg-slate-700 hover:bg-slate-800"
                >
                  <Save className="h-4 w-4 mr-1" />
                  保存更改
                </Button>
              </div>
            </form>
          )}

          {activeTab === 'settings' && (
            <div className="space-y-6">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <Input
                    label="最大尝试次数"
                    type="number"
                    min="1"
                    max="10"
                    leftIcon={<Users className="h-4 w-4" />}
                    {...register('settings.maxAttempts', { valueAsNumber: true })}
                    error={errors.settings?.maxAttempts?.message}
                    required
                  />
                </div>

                <div>
                  <Input
                    label="及格分数"
                    type="number"
                    min="0"
                    max="100"
                    leftIcon={<FileText className="h-4 w-4" />}
                    {...register('settings.passScore', { valueAsNumber: true })}
                    error={errors.settings?.passScore?.message}
                    required
                  />
                </div>
              </div>

              <div className="space-y-4">
                <h3 className="text-lg font-medium text-slate-900">考试选项</h3>
                
                <div className="space-y-3">
                  <label className="flex items-center gap-3 p-3 border border-slate-200 rounded-lg">
                    <input
                      type="checkbox"
                      {...register('settings.timeLimit')}
                      className="rounded border-gray-300 text-slate-600 focus:ring-slate-500"
                    />
                    <div>
                      <span className="text-sm font-medium text-slate-700">
                        启用时间限制
                      </span>
                      <p className="text-xs text-slate-500">
                        学生必须在规定时间内完成考试
                      </p>
                    </div>
                  </label>

                  <label className="flex items-center gap-3 p-3 border border-slate-200 rounded-lg">
                    <input
                      type="checkbox"
                      {...register('settings.allowReview')}
                      className="rounded border-gray-300 text-slate-600 focus:ring-slate-500"
                    />
                    <div>
                      <span className="text-sm font-medium text-slate-700">
                        允许回顾题目
                      </span>
                      <p className="text-xs text-slate-500">
                        学生可以返回查看和修改之前的答案
                      </p>
                    </div>
                  </label>

                  <label className="flex items-center gap-3 p-3 border border-slate-200 rounded-lg">
                    <input
                      type="checkbox"
                      {...register('settings.showResults')}
                      className="rounded border-gray-300 text-slate-600 focus:ring-slate-500"
                    />
                    <div>
                      <span className="text-sm font-medium text-slate-700">
                        显示考试结果
                      </span>
                      <p className="text-xs text-slate-500">
                        考试完成后向学生显示分数和正确答案
                      </p>
                    </div>
                  </label>

                  <label className="flex items-center gap-3 p-3 border border-slate-200 rounded-lg">
                    <input
                      type="checkbox"
                      {...register('settings.randomizeQuestions')}
                      className="rounded border-gray-300 text-slate-600 focus:ring-slate-500"
                    />
                    <div>
                      <span className="text-sm font-medium text-slate-700">
                        随机排列题目
                      </span>
                      <p className="text-xs text-slate-500">
                        为每个学生随机排列题目顺序
                      </p>
                    </div>
                  </label>
                </div>
              </div>

              <div className="flex items-center justify-end gap-2 pt-4 border-t border-slate-200">
                <Button
                  onClick={handleSubmit(onSubmit)}
                  loading={updateExamMutation.isPending}
                  loadingText="保存中..."
                  disabled={!isDirty}
                  className="bg-slate-700 hover:bg-slate-800"
                >
                  <Save className="h-4 w-4 mr-1" />
                  保存设置
                </Button>
              </div>
            </div>
          )}

          {activeTab === 'questions' && (
            <div className="text-center py-12">
              <FileText className="h-12 w-12 text-slate-400 mx-auto mb-4" />
              <h3 className="text-lg font-semibold text-slate-900 mb-2">
                题目管理
              </h3>
              <p className="text-slate-600 mb-4">
                题目管理功能将在题目管理模块中实现
              </p>
              <Button
                onClick={() => window.open(`/admin/questions?examId=${exam.id}`, '_blank')}
                className="bg-slate-700 hover:bg-slate-800"
              >
                管理题目
              </Button>
            </div>
          )}

          {activeTab === 'analytics' && (
            <div className="text-center py-12">
              <Users className="h-12 w-12 text-slate-400 mx-auto mb-4" />
              <h3 className="text-lg font-semibold text-slate-900 mb-2">
                数据分析
              </h3>
              <p className="text-slate-600 mb-4">
                详细的数据分析功能将在成绩查看模块中实现
              </p>
              <Button
                onClick={() => window.open(`/admin/grades?examId=${exam.id}`, '_blank')}
                className="bg-slate-700 hover:bg-slate-800"
              >
                查看分析
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};