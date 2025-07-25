import React, { useState } from 'react';
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
  Plus,
  Minus,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { adminApi } from '@/lib/api';

const examCreateSchema = z.object({
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

type ExamCreateForm = z.infer<typeof examCreateSchema>;

interface ExamCreateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export const ExamCreateModal: React.FC<ExamCreateModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [currentStep, setCurrentStep] = useState(1);
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
    setValue,
    reset,
  } = useForm<ExamCreateForm>({
    resolver: zodResolver(examCreateSchema),
    defaultValues: {
      title: '',
      description: '',
      category: '',
      duration: 60,
      instructions: '',
      startDate: '',
      endDate: '',
      settings: {
        allowReview: true,
        showResults: true,
        timeLimit: true,
        randomizeQuestions: false,
        maxAttempts: 1,
        passScore: 60,
      },
    },
  });

  const settings = watch('settings');

  const createExamMutation = useMutation({
    mutationFn: (data: ExamCreateForm) => adminApi.createExam(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminExams'] });
      onSuccess?.();
      handleClose();
    },
  });

  const handleClose = () => {
    reset();
    setCurrentStep(1);
    onClose();
  };

  const onSubmit = (data: ExamCreateForm) => {
    createExamMutation.mutate(data);
  };

  const nextStep = () => {
    if (currentStep < 3) {
      setCurrentStep(currentStep + 1);
    }
  };

  const prevStep = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1);
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

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-slate-200 p-4 sm:p-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-bold text-slate-900">
                创建新试卷
              </h2>
              <p className="text-sm text-slate-600 mt-1">
                第 {currentStep} 步，共 3 步
              </p>
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleClose}
              className="text-slate-400 hover:text-slate-600"
            >
              <X className="h-5 w-5" />
            </Button>
          </div>

          {/* Progress Steps */}
          <div className="flex items-center gap-4 mt-4">
            {[1, 2, 3].map((step) => (
              <div key={step} className="flex items-center">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
                    step <= currentStep
                      ? 'bg-slate-700 text-white'
                      : 'bg-slate-200 text-slate-600'
                  }`}
                >
                  {step}
                </div>
                <span className={`ml-2 text-sm ${
                  step <= currentStep ? 'text-slate-900' : 'text-slate-500'
                }`}>
                  {step === 1 ? '基本信息' : step === 2 ? '时间设置' : '考试设置'}
                </span>
                {step < 3 && (
                  <div className={`w-8 h-0.5 ml-4 ${
                    step < currentStep ? 'bg-slate-700' : 'bg-slate-200'
                  }`} />
                )}
              </div>
            ))}
          </div>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="p-4 sm:p-6">
          {/* Step 1: Basic Information */}
          {currentStep === 1 && (
            <div className="space-y-6">
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
                  试卷描述 <span className="text-red-500">*</span>
                </label>
                <textarea
                  {...register('description')}
                  placeholder="请输入试卷描述，说明考试内容和要求"
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
                  placeholder="60"
                  leftIcon={<Clock className="h-4 w-4" />}
                  {...register('duration', { valueAsNumber: true })}
                  error={errors.duration?.message}
                  required
                />
                <p className="mt-1 text-xs text-slate-500">
                  建议设置为 30-120 分钟
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  考试说明（可选）
                </label>
                <textarea
                  {...register('instructions')}
                  placeholder="请输入考试说明和注意事项"
                  rows={3}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500 focus:border-slate-500"
                />
              </div>
            </div>
          )}

          {/* Step 2: Time Settings */}
          {currentStep === 2 && (
            <div className="space-y-6">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <Input
                    label="开始时间（可选）"
                    type="datetime-local"
                    leftIcon={<Calendar className="h-4 w-4" />}
                    {...register('startDate')}
                    error={errors.startDate?.message}
                  />
                  <p className="mt-1 text-xs text-slate-500">
                    不设置则立即可参加
                  </p>
                </div>

                <div>
                  <Input
                    label="结束时间（可选）"
                    type="datetime-local"
                    leftIcon={<Calendar className="h-4 w-4" />}
                    {...register('endDate')}
                    error={errors.endDate?.message}
                  />
                  <p className="mt-1 text-xs text-slate-500">
                    不设置则无时间限制
                  </p>
                </div>
              </div>

              <Card>
                <CardHeader>
                  <CardTitle className="text-base">时间设置说明</CardTitle>
                </CardHeader>
                <CardContent className="pt-0">
                  <div className="space-y-2 text-sm text-slate-600">
                    <p>• 开始时间：学生可以开始参加考试的时间</p>
                    <p>• 结束时间：考试截止时间，超过此时间无法参加</p>
                    <p>• 考试时长：学生开始考试后的答题时间限制</p>
                  </div>
                </CardContent>
              </Card>
            </div>
          )}

          {/* Step 3: Exam Settings */}
          {currentStep === 3 && (
            <div className="space-y-6">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <Input
                    label="最大尝试次数"
                    type="number"
                    min="1"
                    max="10"
                    placeholder="1"
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
                    placeholder="60"
                    leftIcon={<FileText className="h-4 w-4" />}
                    {...register('settings.passScore', { valueAsNumber: true })}
                    error={errors.settings?.passScore?.message}
                    required
                  />
                  <p className="mt-1 text-xs text-slate-500">
                    设置考试的及格分数线（0-100）
                  </p>
                </div>
              </div>

              <div className="space-y-4">
                <h3 className="text-lg font-medium text-slate-900">考试选项</h3>
                
                <div className="space-y-3">
                  <label className="flex items-center gap-3">
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

                  <label className="flex items-center gap-3">
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

                  <label className="flex items-center gap-3">
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

                  <label className="flex items-center gap-3">
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

              <Card>
                <CardHeader>
                  <CardTitle className="text-base flex items-center gap-2">
                    <Settings className="h-4 w-4" />
                    当前设置预览
                  </CardTitle>
                </CardHeader>
                <CardContent className="pt-0">
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <span className="text-slate-600">最大尝试：</span>
                      <Badge variant="outline" className="ml-2">
                        {settings.maxAttempts} 次
                      </Badge>
                    </div>
                    <div>
                      <span className="text-slate-600">及格分：</span>
                      <Badge variant="outline" className="ml-2">
                        {settings.passScore} 分
                      </Badge>
                    </div>
                    <div className="col-span-2 flex flex-wrap gap-2 mt-2">
                      {settings.timeLimit && (
                        <Badge variant="secondary">时间限制</Badge>
                      )}
                      {settings.allowReview && (
                        <Badge variant="secondary">可回顾</Badge>
                      )}
                      {settings.showResults && (
                        <Badge variant="secondary">显示结果</Badge>
                      )}
                      {settings.randomizeQuestions && (
                        <Badge variant="secondary">随机题目</Badge>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          )}

          {/* Global Error */}
          {createExamMutation.error && (
            <div className="bg-red-50 border border-red-200 rounded-md p-3 mt-6">
              <div className="flex items-center gap-2">
                <AlertCircle className="h-4 w-4 text-red-500" />
                <p className="text-sm text-red-600">
                  创建失败：{(createExamMutation.error as any)?.response?.data?.message || '未知错误'}
                </p>
              </div>
            </div>
          )}

          {/* Actions */}
          <div className="flex items-center justify-between pt-6 border-t border-slate-200">
            <Button
              type="button"
              variant="ghost"
              onClick={currentStep === 1 ? handleClose : prevStep}
            >
              {currentStep === 1 ? '取消' : '上一步'}
            </Button>

            <div className="flex items-center gap-2">
              {currentStep < 3 ? (
                <Button type="button" onClick={nextStep}>
                  下一步
                </Button>
              ) : (
                <Button
                  type="submit"
                  loading={createExamMutation.isPending}
                  loadingText="创建中..."
                  className="bg-slate-700 hover:bg-slate-800"
                >
                  创建试卷
                </Button>
              )}
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};