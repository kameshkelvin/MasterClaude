import React from 'react';
import { Flag, CheckCircle2, Circle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Question } from '@/types';
import { cn } from '@/lib/utils';

interface QuestionRendererProps {
  question: Question;
  answer: string;
  onChange: (answer: string) => void;
  isMarkedForReview?: boolean;
  onToggleMarkForReview?: () => void;
  showCorrectAnswer?: boolean;
  isReadOnly?: boolean;
  className?: string;
}

export const QuestionRenderer: React.FC<QuestionRendererProps> = ({
  question,
  answer,
  onChange,
  isMarkedForReview = false,
  onToggleMarkForReview,
  showCorrectAnswer = false,
  isReadOnly = false,
  className,
}) => {
  const renderSingleChoice = () => {
    const options = question.options || [];
    const selectedOption = answer;

    return (
      <div className="exam-options-mobile">
        {options.map((option, index) => {
          const optionLabel = String.fromCharCode(65 + index); // A, B, C, D...
          const isSelected = selectedOption === optionLabel;
          const isCorrect = showCorrectAnswer && question.correctAnswer === optionLabel;
          const isWrong = showCorrectAnswer && isSelected && question.correctAnswer !== optionLabel;

          return (
            <label
              key={index}
              className={cn(
                'exam-option-mobile',
                'flex items-start gap-3 p-3 sm:p-4 rounded-lg border cursor-pointer transition-all',
                isSelected && !showCorrectAnswer && 'bg-exam-50 border-exam-200',
                isCorrect && 'bg-green-50 border-green-200',
                isWrong && 'bg-red-50 border-red-200',
                isReadOnly && 'cursor-default',
                !isSelected && !showCorrectAnswer && 'hover:bg-gray-50'
              )}
            >
              <input
                type="radio"
                name={`question-${question.id}`}
                value={optionLabel}
                checked={isSelected}
                onChange={(e) => !isReadOnly && onChange(e.target.value)}
                disabled={isReadOnly}
                className="mt-1 text-exam-600 focus:ring-exam-500"
              />
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <span className="font-medium text-gray-700">{optionLabel}.</span>
                  <span className="text-gray-900">{option}</span>
                  {isCorrect && (
                    <Badge variant="success" className="ml-auto">
                      正确答案
                    </Badge>
                  )}
                  {isWrong && (
                    <Badge variant="error" className="ml-auto">
                      错误
                    </Badge>
                  )}
                </div>
              </div>
            </label>
          );
        })}
      </div>
    );
  };

  const renderMultipleChoice = () => {
    const options = question.options || [];
    const selectedOptions = answer ? answer.split(',') : [];

    return (
      <div className="space-y-3">
        <p className="text-sm text-gray-600 mb-4">
          * 多选题，请选择所有正确答案
        </p>
        {options.map((option, index) => {
          const optionLabel = String.fromCharCode(65 + index); // A, B, C, D...
          const isSelected = selectedOptions.includes(optionLabel);
          const correctOptions = showCorrectAnswer ? (question.correctAnswer?.split(',') || []) : [];
          const isCorrect = showCorrectAnswer && correctOptions.includes(optionLabel);
          const isWrong = showCorrectAnswer && isSelected && !correctOptions.includes(optionLabel);

          return (
            <label
              key={index}
              className={cn(
                'flex items-start gap-3 p-4 rounded-lg border cursor-pointer transition-all',
                isSelected && !showCorrectAnswer && 'bg-exam-50 border-exam-200',
                isCorrect && 'bg-green-50 border-green-200',
                isWrong && 'bg-red-50 border-red-200',
                isReadOnly && 'cursor-default',
                !isSelected && !showCorrectAnswer && 'hover:bg-gray-50'
              )}
            >
              <input
                type="checkbox"
                value={optionLabel}
                checked={isSelected}
                onChange={(e) => {
                  if (isReadOnly) return;
                  const newSelectedOptions = e.target.checked
                    ? [...selectedOptions, optionLabel]
                    : selectedOptions.filter(opt => opt !== optionLabel);
                  onChange(newSelectedOptions.join(','));
                }}
                disabled={isReadOnly}
                className="mt-1 text-exam-600 focus:ring-exam-500 rounded"
              />
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <span className="font-medium text-gray-700">{optionLabel}.</span>
                  <span className="text-gray-900">{option}</span>
                  {isCorrect && (
                    <Badge variant="success" className="ml-auto">
                      正确选项
                    </Badge>
                  )}
                  {isWrong && (
                    <Badge variant="error" className="ml-auto">
                      多选
                    </Badge>
                  )}
                </div>
              </div>
            </label>
          );
        })}
      </div>
    );
  };

  const renderTrueFalse = () => {
    const isTrue = answer === 'true';
    const isFalse = answer === 'false';
    const correctAnswer = question.correctAnswer === 'true';

    return (
      <div className="space-y-3">
        <label
          className={cn(
            'flex items-center gap-3 p-4 rounded-lg border cursor-pointer transition-all',
            isTrue && !showCorrectAnswer && 'bg-exam-50 border-exam-200',
            showCorrectAnswer && correctAnswer && 'bg-green-50 border-green-200',
            showCorrectAnswer && isTrue && !correctAnswer && 'bg-red-50 border-red-200',
            isReadOnly && 'cursor-default',
            !isTrue && !showCorrectAnswer && 'hover:bg-gray-50'
          )}
        >
          <input
            type="radio"
            name={`question-${question.id}`}
            value="true"
            checked={isTrue}
            onChange={(e) => !isReadOnly && onChange('true')}
            disabled={isReadOnly}
            className="text-exam-600 focus:ring-exam-500"
          />
          <span className="text-gray-900">正确</span>
          {showCorrectAnswer && correctAnswer && (
            <Badge variant="success" className="ml-auto">
              正确答案
            </Badge>
          )}
        </label>

        <label
          className={cn(
            'flex items-center gap-3 p-4 rounded-lg border cursor-pointer transition-all',
            isFalse && !showCorrectAnswer && 'bg-exam-50 border-exam-200',
            showCorrectAnswer && !correctAnswer && 'bg-green-50 border-green-200',
            showCorrectAnswer && isFalse && correctAnswer && 'bg-red-50 border-red-200',
            isReadOnly && 'cursor-default',
            !isFalse && !showCorrectAnswer && 'hover:bg-gray-50'
          )}
        >
          <input
            type="radio"
            name={`question-${question.id}`}
            value="false"
            checked={isFalse}
            onChange={(e) => !isReadOnly && onChange('false')}
            disabled={isReadOnly}
            className="text-exam-600 focus:ring-exam-500"
          />
          <span className="text-gray-900">错误</span>
          {showCorrectAnswer && !correctAnswer && (
            <Badge variant="success" className="ml-auto">
              正确答案
            </Badge>
          )}
        </label>
      </div>
    );
  };

  const renderFillBlank = () => {
    const isCorrect = showCorrectAnswer && question.correctAnswer === answer;
    const isWrong = showCorrectAnswer && answer && question.correctAnswer !== answer;

    return (
      <div className="space-y-4">
        <Input
          value={answer}
          onChange={(e) => !isReadOnly && onChange(e.target.value)}
          placeholder="请输入答案..."
          disabled={isReadOnly}
          className={cn(
            isCorrect && 'border-green-500 bg-green-50',
            isWrong && 'border-red-500 bg-red-50'
          )}
        />
        {showCorrectAnswer && (
          <div className="p-3 bg-gray-50 rounded-lg">
            <p className="text-sm font-medium text-gray-700 mb-1">参考答案：</p>
            <p className="text-gray-900">{question.correctAnswer}</p>
          </div>
        )}
      </div>
    );
  };

  const renderEssay = () => {
    return (
      <div className="space-y-4">
        <textarea
          value={answer}
          onChange={(e) => !isReadOnly && onChange(e.target.value)}
          placeholder="请输入您的答案..."
          disabled={isReadOnly}
          rows={8}
          className={cn(
            'w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-exam-500 focus:border-exam-500 resize-vertical',
            isReadOnly && 'bg-gray-50'
          )}
        />
        <div className="flex justify-between text-sm text-gray-500">
          <span>字数统计：{answer.length} 字符</span>
          <span>建议答题时间：{question.timeLimit || 15} 分钟</span>
        </div>
        {showCorrectAnswer && question.explanation && (
          <div className="p-4 bg-blue-50 rounded-lg">
            <p className="text-sm font-medium text-blue-700 mb-2">答题要点：</p>
            <p className="text-blue-900 whitespace-pre-wrap">{question.explanation}</p>
          </div>
        )}
      </div>
    );
  };

  const renderQuestionType = () => {
    switch (question.type) {
      case 'SINGLE_CHOICE':
        return renderSingleChoice();
      case 'MULTIPLE_CHOICE':
        return renderMultipleChoice();
      case 'TRUE_FALSE':
        return renderTrueFalse();
      case 'FILL_BLANK':
        return renderFillBlank();
      case 'ESSAY':
        return renderEssay();
      default:
        return <div className="text-gray-500">不支持的题目类型</div>;
    }
  };

  return (
    <div className={cn('spacing-responsive', className)}>
      {/* Question content */}
      <div className="space-y-4">
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1">
            <div className="prose prose-sm max-w-none">
              <p className="text-gray-900 text-responsive-base leading-relaxed whitespace-pre-wrap">
                {question.content}
              </p>
            </div>
          </div>
          {onToggleMarkForReview && (
            <Button
              variant="ghost"
              size="sm"
              onClick={onToggleMarkForReview}
              className={cn(
                'shrink-0 touch-target',
                isMarkedForReview && 'text-yellow-600 bg-yellow-50'
              )}
            >
              <Flag className={cn(
                'h-4 w-4',
                isMarkedForReview && 'fill-current'
              )} />
            </Button>
          )}
        </div>

        {/* Question attachments */}
        {question.attachments && question.attachments.length > 0 && (
          <div className="space-y-2">
            {question.attachments.map((attachment, index) => (
              <Card key={index} className="p-3">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-gray-100 rounded">
                    <span className="text-xs font-medium text-gray-600">
                      {attachment.type}
                    </span>
                  </div>
                  <div className="flex-1">
                    <p className="font-medium text-sm">{attachment.name}</p>
                    <p className="text-xs text-gray-500">
                      {(attachment.size / 1024 / 1024).toFixed(2)} MB
                    </p>
                  </div>
                  <Button variant="outline" size="sm" asChild>
                    <a href={attachment.url} target="_blank" rel="noopener noreferrer">
                      查看
                    </a>
                  </Button>
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Answer area */}
      <div className="space-y-4">
        <div className="flex items-center gap-2">
          <h4 className="font-medium text-gray-900">答案</h4>
          <Badge variant="outline">
            {getQuestionTypeLabel(question.type)}
          </Badge>
        </div>
        {renderQuestionType()}
      </div>

      {/* Explanation (shown after submission) */}
      {showCorrectAnswer && question.explanation && question.type !== 'ESSAY' && (
        <div className="p-4 bg-blue-50 rounded-lg">
          <h5 className="font-medium text-blue-900 mb-2">题目解析</h5>
          <p className="text-blue-800 whitespace-pre-wrap">{question.explanation}</p>
        </div>
      )}
    </div>
  );
};

// Helper function
const getQuestionTypeLabel = (type: string): string => {
  const typeLabels: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    ESSAY: '主观题',
  };
  return typeLabels[type] || '未知';
};