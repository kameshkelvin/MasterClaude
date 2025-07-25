import React, { useState, useEffect } from 'react';
import { Clock, AlertTriangle } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { cn, formatTimeRemaining } from '@/lib/utils';

interface ExamTimerProps {
  endTime: string;
  onTimeWarning?: (remainingMinutes: number) => void;
  onTimeExpired?: () => void;
  className?: string;
}

export const ExamTimer: React.FC<ExamTimerProps> = ({
  endTime,
  onTimeWarning,
  onTimeExpired,
  className,
}) => {
  const [timeRemaining, setTimeRemaining] = useState(0);
  const [hasWarned15, setHasWarned15] = useState(false);
  const [hasWarned5, setHasWarned5] = useState(false);
  const [hasExpired, setHasExpired] = useState(false);

  useEffect(() => {
    const calculateTimeRemaining = () => {
      const now = new Date().getTime();
      const end = new Date(endTime).getTime();
      const remaining = Math.max(0, Math.floor((end - now) / 1000));
      
      setTimeRemaining(remaining);
      
      const remainingMinutes = Math.floor(remaining / 60);
      
      // Trigger warnings
      if (remainingMinutes <= 15 && remainingMinutes > 5 && !hasWarned15) {
        setHasWarned15(true);
        onTimeWarning?.(remainingMinutes);
      } else if (remainingMinutes <= 5 && remainingMinutes > 0 && !hasWarned5) {
        setHasWarned5(true);
        onTimeWarning?.(remainingMinutes);
      } else if (remaining === 0 && !hasExpired) {
        setHasExpired(true);
        onTimeExpired?.();
      }
      
      return remaining;
    };

    // Calculate immediately
    calculateTimeRemaining();

    // Update every second
    const interval = setInterval(() => {
      calculateTimeRemaining();
    }, 1000);

    return () => clearInterval(interval);
  }, [endTime, onTimeWarning, onTimeExpired, hasWarned15, hasWarned5, hasExpired]);

  const minutes = Math.floor(timeRemaining / 60);
  const seconds = timeRemaining % 60;
  const totalMinutes = Math.floor(timeRemaining / 60);

  // Determine timer state
  const getTimerState = () => {
    if (timeRemaining === 0) return 'expired';
    if (totalMinutes <= 5) return 'critical';
    if (totalMinutes <= 15) return 'warning';
    return 'normal';
  };

  const timerState = getTimerState();

  const getTimerStyles = () => {
    switch (timerState) {
      case 'expired':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'critical':
        return 'bg-red-50 text-red-700 border-red-200 animate-timer-warning';
      case 'warning':
        return 'bg-yellow-50 text-yellow-700 border-yellow-200';
      default:
        return 'bg-exam-50 text-exam-700 border-exam-200';
    }
  };

  const formatTime = (totalSeconds: number) => {
    const hours = Math.floor(totalSeconds / 3600);
    const mins = Math.floor((totalSeconds % 3600) / 60);
    const secs = totalSeconds % 60;

    if (hours > 0) {
      return `${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className={cn('flex items-center gap-3', className)}>
      {/* Timer display */}
      <div className={cn(
        'flex items-center gap-2 px-3 py-2 rounded-lg border font-mono text-lg font-bold transition-all',
        getTimerStyles()
      )}>
        <Clock className="h-5 w-5" />
        <span>{formatTime(timeRemaining)}</span>
      </div>

      {/* Warning indicators */}
      {timerState === 'critical' && (
        <Badge variant="error" className="animate-pulse">
          <AlertTriangle className="h-3 w-3 mr-1" />
          时间紧急
        </Badge>
      )}
      
      {timerState === 'warning' && (
        <Badge variant="warning">
          <AlertTriangle className="h-3 w-3 mr-1" />
          注意时间
        </Badge>
      )}
      
      {timerState === 'expired' && (
        <Badge variant="error">
          时间到
        </Badge>
      )}
    </div>
  );
};