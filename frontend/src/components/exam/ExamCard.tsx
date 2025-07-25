import React from 'react';
import { Link } from 'react-router-dom';
import { Clock, Users, BookOpen, Calendar, Timer, Trophy, Play } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Exam } from '@/types';
import { 
  formatDate, 
  formatDuration, 
  getExamStatusBadge, 
  getDifficultyBadge,
  getTimeUntilExam,
  isExamAvailable,
  cn
} from '@/lib/utils';

interface ExamCardProps {
  exam: Exam;
  showActions?: boolean;
  variant?: 'default' | 'compact' | 'featured';
  className?: string;
}

export const ExamCard: React.FC<ExamCardProps> = ({ 
  exam, 
  showActions = true, 
  variant = 'default',
  className 
}) => {
  const statusBadge = getExamStatusBadge(exam.status);
  const difficultyBadge = getDifficultyBadge(exam.difficulty);
  const { isStarted, timeRemaining } = getTimeUntilExam(exam.startTime);
  const isAvailable = isExamAvailable(exam);

  const cardClasses = cn(
    'transition-all duration-200 hover:shadow-exam-active group',
    variant === 'featured' && 'border-exam-200 shadow-exam-card',
    variant === 'compact' && 'p-4',
    className
  );

  const renderQuickStats = () => (
    <div className="flex items-center gap-4 text-sm text-gray-500">
      <div className="flex items-center gap-1">
        <BookOpen className="h-4 w-4" />
        <span>{exam.questionCount} 题</span>
      </div>
      <div className="flex items-center gap-1">
        <Clock className="h-4 w-4" />
        <span>{formatDuration(exam.duration)}</span>
      </div>
      <div className="flex items-center gap-1">
        <Trophy className="h-4 w-4" />
        <span>{exam.totalPoints} 分</span>
      </div>
    </div>
  );

  const renderTimeInfo = () => (
    <div className="space-y-2">
      <div className="flex items-center gap-2 text-sm">
        <Calendar className="h-4 w-4 text-gray-400" />
        <span className="text-gray-600">
          {formatDate(exam.startTime, 'yyyy-MM-dd HH:mm')} - {formatDate(exam.endTime, 'HH:mm')}
        </span>
      </div>
      {!isStarted && (
        <div className="flex items-center gap-2 text-sm">
          <Timer className="h-4 w-4 text-exam-500" />
          <span className="text-exam-600 font-medium">
            {timeRemaining}后开始
          </span>
        </div>
      )}
    </div>
  );

  const renderActions = () => {
    if (!showActions) return null;

    return (
      <div className="flex gap-2">
        <Button 
          asChild 
          variant="outline" 
          size="sm"
          className="flex-1"
        >
          <Link to={`/exam/${exam.id}`}>
            查看详情
          </Link>
        </Button>
        
        {isAvailable && (
          <Button 
            asChild 
            variant="exam" 
            size="sm"
            className="flex-1"
          >
            <Link to={`/exam/${exam.id}/start`}>
              <Play className="h-4 w-4 mr-1" />
              开始考试
            </Link>
          </Button>
        )}
        
        {exam.status === 'ENDED' && exam.showResults && (
          <Button 
            asChild 
            variant="secondary" 
            size="sm"
            className="flex-1"
          >
            <Link to={`/exam/${exam.id}/results`}>
              查看成绩
            </Link>
          </Button>
        )}
      </div>
    );
  };

  if (variant === 'compact') {
    return (
      <Card className={cardClasses}>
        <CardContent className="p-4">
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-2">
                <h3 className="font-semibold text-lg truncate">{exam.title}</h3>
                <Badge className={statusBadge.color}>{statusBadge.label}</Badge>
              </div>
              <p className="text-gray-600 text-sm line-clamp-2 mb-3">{exam.description}</p>
              {renderQuickStats()}
            </div>
            <div className="flex flex-col gap-2 shrink-0">
              <Badge variant="outline" className={difficultyBadge.color}>
                {difficultyBadge.label}
              </Badge>
              {showActions && isAvailable && (
                <Button asChild variant="exam" size="sm">
                  <Link to={`/exam/${exam.id}/start`}>
                    <Play className="h-4 w-4 mr-1" />
                    开始
                  </Link>
                </Button>
              )}
            </div>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (variant === 'featured') {
    return (
      <Card className={cardClasses}>
        <CardHeader className="pb-4">
          <div className="flex items-start justify-between">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-3 mb-2">
                <h2 className="text-xl font-bold text-gray-900 truncate">{exam.title}</h2>
                <Badge className={statusBadge.color}>{statusBadge.label}</Badge>
                <Badge variant="outline" className={difficultyBadge.color}>
                  {difficultyBadge.label}
                </Badge>
              </div>
              <p className="text-gray-600 mb-4">{exam.description}</p>
              {renderTimeInfo()}
            </div>
          </div>
        </CardHeader>
        <CardContent className="pt-0">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
            <div className="text-center p-3 bg-gray-50 rounded-lg">
              <BookOpen className="h-6 w-6 mx-auto text-exam-500 mb-1" />
              <p className="text-sm font-medium">{exam.questionCount}</p>
              <p className="text-xs text-gray-500">题目数</p>
            </div>
            <div className="text-center p-3 bg-gray-50 rounded-lg">
              <Clock className="h-6 w-6 mx-auto text-exam-500 mb-1" />
              <p className="text-sm font-medium">{formatDuration(exam.duration)}</p>
              <p className="text-xs text-gray-500">考试时长</p>
            </div>
            <div className="text-center p-3 bg-gray-50 rounded-lg">
              <Trophy className="h-6 w-6 mx-auto text-exam-500 mb-1" />
              <p className="text-sm font-medium">{exam.totalPoints}</p>
              <p className="text-xs text-gray-500">总分</p>
            </div>
            <div className="text-center p-3 bg-gray-50 rounded-lg">
              <Users className="h-6 w-6 mx-auto text-exam-500 mb-1" />
              <p className="text-sm font-medium">{exam.maxAttempts}</p>
              <p className="text-xs text-gray-500">尝试次数</p>
            </div>
          </div>
          
          {exam.tags && exam.tags.length > 0 && (
            <div className="flex flex-wrap gap-2 mb-4">
              {exam.tags.map((tag, index) => (
                <Badge key={index} variant="secondary" className="text-xs">
                  {tag}
                </Badge>
              ))}
            </div>
          )}
        </CardContent>
        <CardFooter className="pt-0">
          {renderActions()}
        </CardFooter>
      </Card>
    );
  }

  // Default variant
  return (
    <Card className={cardClasses}>
      <CardHeader className="pb-4">
        <div className="flex items-start justify-between">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-2">
              <h3 className="font-semibold text-lg truncate group-hover:text-exam-600">
                {exam.title}
              </h3>
              <Badge className={statusBadge.color}>{statusBadge.label}</Badge>
            </div>
            <p className="text-gray-600 text-sm line-clamp-2 mb-3">{exam.description}</p>
          </div>
          <Badge variant="outline" className={difficultyBadge.color}>
            {difficultyBadge.label}
          </Badge>
        </div>
      </CardHeader>
      
      <CardContent className="pt-0 pb-4">
        <div className="space-y-3">
          {renderTimeInfo()}
          {renderQuickStats()}
          
          {exam.tags && exam.tags.length > 0 && (
            <div className="flex flex-wrap gap-1">
              {exam.tags.slice(0, 3).map((tag, index) => (
                <Badge key={index} variant="secondary" className="text-xs">
                  {tag}
                </Badge>
              ))}
              {exam.tags.length > 3 && (
                <Badge variant="secondary" className="text-xs">
                  +{exam.tags.length - 3}
                </Badge>
              )}
            </div>
          )}
        </div>
      </CardContent>
      
      <CardFooter className="pt-0">
        {renderActions()}
      </CardFooter>
    </Card>
  );
};