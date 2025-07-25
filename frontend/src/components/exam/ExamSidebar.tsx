import React from 'react';
import { X, CheckCircle, Circle, Flag, Send, Eye } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Question } from '@/types';
import { cn } from '@/lib/utils';

interface ExamSidebarProps {
  isOpen: boolean;
  onClose: () => void;
  questions: Question[];
  currentIndex: number;
  answers: Map<string, string>;
  markedForReview: Set<string>;
  onQuestionSelect: (index: number) => void;
  onSubmitExam: () => void;
  className?: string;
}

export const ExamSidebar: React.FC<ExamSidebarProps> = ({
  isOpen,
  onClose,
  questions,
  currentIndex,
  answers,
  markedForReview,
  onQuestionSelect,
  onSubmitExam,
  className,
}) => {
  const getQuestionStatus = (question: Question, index: number) => {
    const hasAnswer = answers.has(question.id) && answers.get(question.id)?.trim() !== '';
    const isMarked = markedForReview.has(question.id);
    const isCurrent = index === currentIndex;

    if (isCurrent) return 'current';
    if (hasAnswer && isMarked) return 'answered-marked';
    if (hasAnswer) return 'answered';
    if (isMarked) return 'marked';
    return 'unanswered';
  };

  const getStatusStyles = (status: string) => {
    switch (status) {
      case 'current':
        return 'bg-exam-500 text-white border-exam-500';
      case 'answered-marked':
        return 'bg-yellow-100 text-yellow-800 border-yellow-300';
      case 'answered':
        return 'bg-green-100 text-green-800 border-green-300';
      case 'marked':
        return 'bg-yellow-50 text-yellow-700 border-yellow-200';
      default:
        return 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'current':
        return <Eye className="h-3 w-3" />;
      case 'answered-marked':
        return <Flag className="h-3 w-3 fill-current" />;
      case 'answered':
        return <CheckCircle className="h-3 w-3" />;
      case 'marked':
        return <Flag className="h-3 w-3" />;
      default:
        return <Circle className="h-3 w-3" />;
    }
  };

  const stats = {
    answered: questions.filter(q => answers.has(q.id) && answers.get(q.id)?.trim() !== '').length,
    marked: markedForReview.size,
    total: questions.length,
  };

  const unanswered = stats.total - stats.answered;

  if (!isOpen) return null;

  return (
    <>
      {/* Overlay */}
      <div 
        className="fixed inset-0 bg-black bg-opacity-25 z-40 lg:hidden"
        onClick={onClose}
      />
      
      {/* Sidebar */}
      <div className={cn(
        'fixed right-0 top-0 h-full w-80 bg-white border-l border-gray-200 shadow-lg z-50 overflow-hidden',
        'lg:relative lg:shadow-none lg:border-l-0 lg:w-80',
        className
      )}>
        <div className="flex flex-col h-full">
          {/* Header */}
          <div className="flex items-center justify-between p-4 border-b border-gray-200">
            <h3 className="font-semibold text-gray-900">题目导航</h3>
            <Button
              variant="ghost"
              size="sm"
              onClick={onClose}
              className="lg:hidden"
            >
              <X className="h-4 w-4" />
            </Button>
          </div>

          {/* Stats */}
          <div className="p-4 border-b border-gray-200">
            <div className="grid grid-cols-3 gap-3 text-center">
              <div className="p-2 bg-green-50 rounded-lg">
                <div className="text-lg font-bold text-green-600">{stats.answered}</div>
                <div className="text-xs text-green-600">已答</div>
              </div>
              <div className="p-2 bg-yellow-50 rounded-lg">
                <div className="text-lg font-bold text-yellow-600">{stats.marked}</div>
                <div className="text-xs text-yellow-600">标记</div>
              </div>
              <div className="p-2 bg-gray-50 rounded-lg">
                <div className="text-lg font-bold text-gray-600">{unanswered}</div>
                <div className="text-xs text-gray-600">未答</div>
              </div>
            </div>
          </div>

          {/* Question grid */}
          <div className="flex-1 overflow-y-auto p-4">
            <div className="grid grid-cols-5 gap-2">
              {questions.map((question, index) => {
                const status = getQuestionStatus(question, index);
                const statusStyles = getStatusStyles(status);
                const statusIcon = getStatusIcon(status);

                return (
                  <button
                    key={question.id}
                    onClick={() => {
                      onQuestionSelect(index);
                      onClose();
                    }}
                    className={cn(
                      'aspect-square flex flex-col items-center justify-center p-2 rounded-lg border-2 transition-all text-xs font-medium',
                      statusStyles
                    )}
                    title={`第 ${index + 1} 题${status === 'answered' ? ' (已答)' : ''}${status.includes('marked') ? ' (已标记)' : ''}`}
                  >
                    <div className="mb-1">{statusIcon}</div>
                    <div>{index + 1}</div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Legend */}
          <div className="p-4 border-t border-gray-200">
            <div className="space-y-2 text-xs">
              <h4 className="font-medium text-gray-700 mb-2">图例说明</h4>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-exam-500 rounded border"></div>
                <span className="text-gray-600">当前题目</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-green-100 border border-green-300 rounded flex items-center justify-center">
                  <CheckCircle className="h-2 w-2 text-green-600" />
                </div>
                <span className="text-gray-600">已答题目</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-yellow-100 border border-yellow-300 rounded flex items-center justify-center">
                  <Flag className="h-2 w-2 text-yellow-600" />
                </div>
                <span className="text-gray-600">标记复查</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-white border border-gray-200 rounded flex items-center justify-center">
                  <Circle className="h-2 w-2 text-gray-400" />
                </div>
                <span className="text-gray-600">未答题目</span>
              </div>
            </div>
          </div>

          {/* Submit button */}
          <div className="p-4 border-t border-gray-200">
            <Button
              onClick={onSubmitExam}
              variant="destructive"
              className="w-full"
              size="lg"
            >
              <Send className="h-4 w-4 mr-2" />
              提交考试
            </Button>
            
            {unanswered > 0 && (
              <p className="text-xs text-gray-500 mt-2 text-center">
                还有 {unanswered} 题未作答
              </p>
            )}
            
            {stats.marked > 0 && (
              <p className="text-xs text-yellow-600 mt-1 text-center">
                {stats.marked} 题标记待复查
              </p>
            )}
          </div>
        </div>
      </div>
    </>
  );
};