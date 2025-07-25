import React, { useState } from 'react';
import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  X,
  Plus,
  Minus,
  HelpCircle,
  CheckCircle,
  Grid,
  FileText,
  Edit3,
  AlertCircle,
  Tag,
  BookOpen,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { adminApi } from '@/lib/api';

const optionSchema = z.object({
  content: z.string().min(1, '选项内容不能为空'),
  isCorrect: z.boolean(),
});

const questionCreateSchema = z.object({
  title: z.string().min(1, '题目标题不能为空').max(200, '标题不能超过200个字符'),
  content: z.string().min(1, '题目内容不能为空').max(2000, '内容不能超过2000个字符'),
  type: z.enum(['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'FILL_BLANK', 'ESSAY']),
  category: z.string().min(1, '请选择题目分类'),
  difficulty: z.enum(['EASY', 'MEDIUM', 'HARD']),
  points: z.number().min(1, '分值至少为1').max(100, '分值不能超过100'),
  tags: z.array(z.string()).default([]),
  options: z.array(optionSchema).optional(),
  correctAnswer: z.string().optional(),
  explanation: z.string().optional(),
});

type QuestionCreateForm = z.infer<typeof questionCreateSchema>;

interface QuestionCreateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export const QuestionCreateModal: React.FC<QuestionCreateModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [currentStep, setCurrentStep] = useState(1);
  const [newTag, setNewTag] = useState('');
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
    setValue,
    reset,
    control,
  } = useForm<QuestionCreateForm>({
    resolver: zodResolver(questionCreateSchema),
    defaultValues: {
      title: '',
      content: '',
      type: 'SINGLE_CHOICE',
      category: '',
      difficulty: 'MEDIUM',
      points: 5,
      tags: [],
      options: [
        { content: '', isCorrect: true },
        { content: '', isCorrect: false },
        { content: '', isCorrect: false },
        { content: '', isCorrect: false },
      ],
      correctAnswer: '',
      explanation: '',
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'options',
  });

  const questionType = watch('type');
  const tags = watch('tags');
  const options = watch('options');

  const createQuestionMutation = useMutation({
    mutationFn: (data: QuestionCreateForm) => adminApi.createQuestion(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminQuestions'] });
      onSuccess?.();
      handleClose();
    },
  });

  const handleClose = () => {
    reset();
    setCurrentStep(1);
    setNewTag('');
    onClose();
  };

  const onSubmit = (data: QuestionCreateForm) => {
    // Validate based on question type
    if (['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(data.type)) {
      const correctOptions = data.options?.filter(opt => opt.isCorrect) || [];
      if (data.type === 'SINGLE_CHOICE' && correctOptions.length !== 1) {
        alert('单选题必须有且仅有一个正确选项');
        return;
      }
      if (data.type === 'MULTIPLE_CHOICE' && correctOptions.length < 1) {
        alert('多选题至少需要一个正确选项');
        return;
      }
    }

    createQuestionMutation.mutate(data);
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

  const addTag = () => {
    if (newTag.trim() && !tags.includes(newTag.trim())) {
      setValue('tags', [...tags, newTag.trim()]);
      setNewTag('');
    }
  };

  const removeTag = (tagToRemove: string) => {
    setValue('tags', tags.filter(tag => tag !== tagToRemove));
  };

  const addOption = () => {
    append({ content: '', isCorrect: false });
  };

  const removeOption = (index: number) => {
    if (fields.length > 2) {
      remove(index);
    }
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'SINGLE_CHOICE': return <CheckCircle className="h-4 w-4" />;
      case 'MULTIPLE_CHOICE': return <Grid className="h-4 w-4" />;
      case 'TRUE_FALSE': return <HelpCircle className="h-4 w-4" />;
      case 'FILL_BLANK': return <Edit3 className="h-4 w-4" />;
      case 'ESSAY': return <FileText className="h-4 w-4" />;
      default: return <HelpCircle className="h-4 w-4" />;
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
      <div className="bg-white rounded-lg shadow-xl w-full max-w-3xl max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-slate-200 p-4 sm:p-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-bold text-slate-900">
                创建新题目
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
                  {step === 1 ? '基本信息' : step === 2 ? '题目内容' : '答案设置'}
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
                  label="题目标题"
                  placeholder="请输入题目标题"
                  {...register('title')}
                  error={errors.title?.message}
                  required
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    题目类型 <span className="text-red-500">*</span>
                  </label>
                  <select
                    {...register('type')}
                    className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500 focus:border-slate-500"
                  >
                    <option value="SINGLE_CHOICE">单选题</option>
                    <option value="MULTIPLE_CHOICE">多选题</option>
                    <option value="TRUE_FALSE">判断题</option>
                    <option value="FILL_BLANK">填空题</option>
                    <option value="ESSAY">问答题</option>
                  </select>
                  {errors.type && (
                    <p className="mt-1 text-sm text-red-600">
                      {errors.type.message}
                    </p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    题目分类 <span className="text-red-500">*</span>
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
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    难度等级 <span className="text-red-500">*</span>
                  </label>
                  <select
                    {...register('difficulty')}
                    className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500 focus:border-slate-500"
                  >
                    <option value="EASY">简单</option>
                    <option value="MEDIUM">中等</option>
                    <option value="HARD">困难</option>
                  </select>
                  {errors.difficulty && (
                    <p className="mt-1 text-sm text-red-600">
                      {errors.difficulty.message}
                    </p>
                  )}
                </div>

                <div>
                  <Input
                    label="分值"
                    type="number"
                    min="1"
                    max="100"
                    placeholder="5"
                    leftIcon={<BookOpen className="h-4 w-4" />}
                    {...register('points', { valueAsNumber: true })}
                    error={errors.points?.message}
                    required
                  />
                </div>
              </div>

              {/* Tags */}
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  标签
                </label>
                <div className="flex gap-2 mb-2">
                  <Input
                    placeholder="添加标签"
                    value={newTag}
                    onChange={(e) => setNewTag(e.target.value)}
                    onKeyPress={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        addTag();
                      }
                    }}
                  />
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={addTag}
                  >
                    <Plus className="h-4 w-4" />
                  </Button>
                </div>
                <div className="flex flex-wrap gap-2">
                  {tags.map((tag) => (
                    <Badge key={tag} variant="secondary" className="text-xs">
                      <Tag className="h-3 w-3 mr-1" />
                      {tag}
                      <button
                        type="button"
                        onClick={() => removeTag(tag)}
                        className="ml-1 hover:text-red-600"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </Badge>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Step 2: Question Content */}
          {currentStep === 2 && (
            <div className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  题目内容 <span className="text-red-500">*</span>
                </label>
                <textarea
                  {...register('content')}
                  placeholder="请输入题目内容..."
                  rows={6}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500 focus:border-slate-500"
                />
                {errors.content && (
                  <p className="mt-1 text-sm text-red-600">
                    {errors.content.message}
                  </p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  题目解析（可选）
                </label>
                <textarea
                  {...register('explanation')}
                  placeholder="请输入题目解析和答题思路..."
                  rows={4}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500 focus:border-slate-500"
                />
              </div>

              {/* Preview */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-base flex items-center gap-2">
                    {getTypeIcon(questionType)}
                    题目预览
                  </CardTitle>
                </CardHeader>
                <CardContent className="pt-0">
                  <div className="space-y-4">
                    <div className="p-4 bg-slate-50 rounded-lg">
                      <h4 className="font-medium text-slate-900 mb-2">
                        {watch('title') || '题目标题'}
                      </h4>
                      <p className="text-slate-700">
                        {watch('content') || '题目内容将在这里显示...'}
                      </p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          )}

          {/* Step 3: Answer Settings */}
          {currentStep === 3 && (
            <div className="space-y-6">
              {/* Single Choice & Multiple Choice Options */}
              {['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(questionType) && (
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <label className="block text-sm font-medium text-slate-700">
                      选项设置 <span className="text-red-500">*</span>
                    </label>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={addOption}
                    >
                      <Plus className="h-4 w-4 mr-1" />
                      添加选项
                    </Button>
                  </div>
                  
                  <div className="space-y-3">
                    {fields.map((field, index) => (
                      <div key={field.id} className="flex items-center gap-3 p-3 border border-slate-200 rounded-lg">
                        <input
                          type={questionType === 'SINGLE_CHOICE' ? 'radio' : 'checkbox'}
                          {...register(`options.${index}.isCorrect`)}
                          name={questionType === 'SINGLE_CHOICE' ? 'singleCorrect' : undefined}
                          className="rounded border-gray-300 text-slate-600 focus:ring-slate-500"
                        />
                        <Input
                          placeholder={`选项 ${String.fromCharCode(65 + index)}`}
                          {...register(`options.${index}.content`)}
                          error={errors.options?.[index]?.content?.message}
                          className="flex-1"
                        />
                        {fields.length > 2 && (
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => removeOption(index)}
                            className="text-red-600"
                          >
                            <Minus className="h-4 w-4" />
                          </Button>
                        )}
                      </div>
                    ))}
                  </div>
                  <p className="text-xs text-slate-500 mt-2">
                    {questionType === 'SINGLE_CHOICE' 
                      ? '请选择一个正确选项' 
                      : '请选择一个或多个正确选项'}
                  </p>
                </div>
              )}

              {/* True/False */}
              {questionType === 'TRUE_FALSE' && (
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    正确答案 <span className="text-red-500">*</span>
                  </label>
                  <div className="space-y-2">
                    <label className="flex items-center gap-2">
                      <input
                        type="radio"
                        value="true"
                        {...register('correctAnswer')}
                        className="rounded border-gray-300 text-slate-600 focus:ring-slate-500"
                      />
                      <span>正确</span>
                    </label>
                    <label className="flex items-center gap-2">
                      <input
                        type="radio"
                        value="false"
                        {...register('correctAnswer')}
                        className="rounded border-gray-300 text-slate-600 focus:ring-slate-500"
                      />
                      <span>错误</span>
                    </label>
                  </div>
                </div>
              )}

              {/* Fill in the Blank */}
              {questionType === 'FILL_BLANK' && (
                <div>
                  <Input
                    label="标准答案"
                    placeholder="请输入标准答案，多个答案用 | 分隔"
                    {...register('correctAnswer')}
                    error={errors.correctAnswer?.message}
                    required
                  />
                  <p className="text-xs text-slate-500 mt-1">
                    多个可接受答案请用竖线 | 分隔，例如：答案1|答案2|答案3
                  </p>
                </div>
              )}

              {/* Essay Question */}
              {questionType === 'ESSAY' && (
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    参考答案（可选）
                  </label>
                  <textarea
                    {...register('correctAnswer')}
                    placeholder="请输入参考答案要点..."
                    rows={4}
                    className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-slate-500 focus:border-slate-500"
                  />
                  <p className="text-xs text-slate-500 mt-1">
                    问答题需要人工评分，此处填写参考答案要点
                  </p>
                </div>
              )}
            </div>
          )}

          {/* Global Error */}
          {createQuestionMutation.error && (
            <div className="bg-red-50 border border-red-200 rounded-md p-3 mt-6">
              <div className="flex items-center gap-2">
                <AlertCircle className="h-4 w-4 text-red-500" />
                <p className="text-sm text-red-600">
                  创建失败：{(createQuestionMutation.error as any)?.response?.data?.message || '未知错误'}
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
                  loading={createQuestionMutation.isPending}
                  loadingText="创建中..."
                  className="bg-slate-700 hover:bg-slate-800"
                >
                  创建题目
                </Button>
              )}
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};