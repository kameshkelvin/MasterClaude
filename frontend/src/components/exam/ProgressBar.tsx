import React from 'react';
import { CheckCircle, Flag, Circle } from 'lucide-react';
import { cn } from '@/lib/utils';

interface ProgressBarProps {
  current: number;
  total: number;
  markedForReview?: number;
  className?: string;
}

export const ProgressBar: React.FC<ProgressBarProps> = ({
  current,
  total,
  markedForReview = 0,
  className,
}) => {
  const percentage = total > 0 ? (current / total) * 100 : 0;
  const unanswered = total - current;

  return (
    <div className={cn('space-y-3', className)}>
      {/* Progress bar */}
      <div className="relative">
        <div className="w-full bg-gray-200 rounded-full h-3">
          <div
            className="bg-exam-500 h-3 rounded-full transition-all duration-300 ease-out"
            style={{ width: `${Math.min(percentage, 100)}%` }}
          />
        </div>
        <div className="absolute inset-y-0 right-0 flex items-center pr-2">
          <span className="text-xs font-medium text-gray-600">
            {Math.round(percentage)}%
          </span>
        </div>
      </div>

      {/* Stats */}
      <div className="flex items-center justify-between text-sm">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-1">
            <CheckCircle className="h-4 w-4 text-green-500" />
            <span className="text-gray-600">
              已答：<span className="font-medium text-green-600">{current}</span>
            </span>
          </div>
          
          <div className="flex items-center gap-1">
            <Circle className="h-4 w-4 text-gray-400" />
            <span className="text-gray-600">
              未答：<span className="font-medium text-gray-500">{unanswered}</span>
            </span>
          </div>
          
          {markedForReview > 0 && (
            <div className="flex items-center gap-1">
              <Flag className="h-4 w-4 text-yellow-500" />
              <span className="text-gray-600">
                标记：<span className="font-medium text-yellow-600">{markedForReview}</span>
              </span>
            </div>
          )}
        </div>
        
        <div className="text-gray-500">
          总计：{total} 题
        </div>
      </div>
    </div>
  );
};