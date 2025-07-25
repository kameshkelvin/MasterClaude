import React, { useState, useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Bell,
  X,
  Clock,
  AlertCircle,
  CheckCircle,
  Info,
  Trophy,
  Calendar,
  BookOpen,
  User,
  Settings,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { formatDate, cn } from '@/lib/utils';
import { userApi } from '@/lib/api';
import toast from 'react-hot-toast';

interface Notification {
  id: string;
  type: 'exam_reminder' | 'result_ready' | 'system_update' | 'achievement' | 'general';
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
  actionUrl?: string;
  priority: 'low' | 'medium' | 'high';
  metadata?: {
    examId?: string;
    examTitle?: string;
    score?: number;
    badge?: string;
  };
}

interface NotificationCenterProps {
  isOpen: boolean;
  onClose: () => void;
  className?: string;
}

export const NotificationCenter: React.FC<NotificationCenterProps> = ({
  isOpen,
  onClose,
  className,
}) => {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<'all' | 'unread'>('all');

  // Fetch notifications
  const { data: notifications = [], isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: () => userApi.getNotifications(),
    select: (response) => response.data.data,
    refetchInterval: 30000, // Refresh every 30 seconds
    enabled: isOpen,
  });

  // Mark notification as read
  const markAsRead = async (notificationId: string) => {
    try {
      await userApi.markNotificationAsRead(notificationId);
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    } catch (error) {
      toast.error('标记失败');
    }
  };

  // Mark all as read
  const markAllAsRead = async () => {
    try {
      await userApi.markAllNotificationsAsRead();
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      toast.success('已标记所有通知为已读');
    } catch (error) {
      toast.error('操作失败');
    }
  };

  // Get notification icon
  const getNotificationIcon = (type: string, priority: string) => {
    const baseClasses = "h-5 w-5";
    const iconProps = { className: baseClasses };

    switch (type) {
      case 'exam_reminder':
        return <Clock {...iconProps} className={cn(baseClasses, 'text-blue-500')} />;
      case 'result_ready':
        return <Trophy {...iconProps} className={cn(baseClasses, 'text-yellow-500')} />;
      case 'achievement':
        return <Trophy {...iconProps} className={cn(baseClasses, 'text-purple-500')} />;
      case 'system_update':
        return <Settings {...iconProps} className={cn(baseClasses, 'text-gray-500')} />;
      default:
        return priority === 'high' ? 
          <AlertCircle {...iconProps} className={cn(baseClasses, 'text-red-500')} /> :
          <Info {...iconProps} className={cn(baseClasses, 'text-blue-500')} />;
    }
  };

  // Get priority color
  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'high': return 'border-l-red-500';
      case 'medium': return 'border-l-yellow-500';
      default: return 'border-l-blue-500';
    }
  };

  // Filter notifications
  const filteredNotifications = notifications.filter(notification => {
    if (filter === 'unread') return !notification.isRead;
    return true;
  });

  const unreadCount = notifications.filter(n => !n.isRead).length;

  if (!isOpen) return null;

  return (
    <>
      {/* Overlay */}
      <div 
        className="fixed inset-0 bg-black bg-opacity-25 z-40 lg:hidden"
        onClick={onClose}
      />
      
      {/* Notification Panel */}
      <div className={cn(
        'fixed right-0 top-0 h-full w-96 max-w-[90vw] bg-white border-l border-gray-200 shadow-lg z-50 overflow-hidden',
        'lg:relative lg:shadow-none lg:border-l-0',
        className
      )}>
        <div className="flex flex-col h-full">
          {/* Header */}
          <div className="flex items-center justify-between p-4 border-b border-gray-200">
            <div className="flex items-center gap-2">
              <Bell className="h-5 w-5 text-gray-600" />
              <h3 className="font-semibold text-gray-900">
                通知中心
              </h3>
              {unreadCount > 0 && (
                <Badge variant="destructive" className="text-xs">
                  {unreadCount}
                </Badge>
              )}
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={onClose}
              className="touch-target"
            >
              <X className="h-4 w-4" />
            </Button>
          </div>

          {/* Filter tabs */}
          <div className="flex p-4 pb-2 gap-2">
            <Button
              variant={filter === 'all' ? 'default' : 'ghost'}
              size="sm"
              onClick={() => setFilter('all')}
              className="flex-1"
            >
              全部 ({notifications.length})
            </Button>
            <Button
              variant={filter === 'unread' ? 'default' : 'ghost'}
              size="sm"
              onClick={() => setFilter('unread')}
              className="flex-1"
            >
              未读 ({unreadCount})
            </Button>
          </div>

          {/* Actions */}
          {unreadCount > 0 && (
            <div className="px-4 pb-2">
              <Button
                variant="outline"
                size="sm"
                onClick={markAllAsRead}
                className="w-full"
              >
                全部标为已读
              </Button>
            </div>
          )}

          {/* Notifications List */}
          <div className="flex-1 overflow-y-auto p-4 pt-2 space-y-3">
            {isLoading ? (
              <div className="space-y-3">
                {Array.from({ length: 5 }).map((_, i) => (
                  <div key={i} className="animate-pulse">
                    <div className="h-20 bg-gray-200 rounded"></div>
                  </div>
                ))}
              </div>
            ) : filteredNotifications.length === 0 ? (
              <div className="text-center py-8">
                <Bell className="h-12 w-12 text-gray-400 mx-auto mb-3" />
                <p className="text-gray-500 mb-2">
                  {filter === 'unread' ? '没有未读通知' : '暂无通知'}
                </p>
                <p className="text-sm text-gray-400">
                  {filter === 'unread' ? '所有通知都已查看' : '您的通知将在这里显示'}
                </p>
              </div>
            ) : (
              filteredNotifications.map((notification) => (
                <Card
                  key={notification.id}
                  className={cn(
                    'cursor-pointer transition-all hover:shadow-md border-l-4',
                    getPriorityColor(notification.priority),
                    !notification.isRead && 'bg-blue-50'
                  )}
                  onClick={() => {
                    if (!notification.isRead) {
                      markAsRead(notification.id);
                    }
                    if (notification.actionUrl) {
                      window.open(notification.actionUrl, '_blank');
                    }
                  }}
                >
                  <CardContent className="p-4">
                    <div className="flex items-start gap-3">
                      <div className="flex-shrink-0 mt-1">
                        {getNotificationIcon(notification.type, notification.priority)}
                      </div>
                      
                      <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between gap-2">
                          <h4 className={cn(
                            'font-medium text-sm truncate',
                            !notification.isRead ? 'text-gray-900' : 'text-gray-700'
                          )}>
                            {notification.title}
                          </h4>
                          <div className="flex items-center gap-1 flex-shrink-0">
                            {!notification.isRead && (
                              <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                            )}
                            <span className="text-xs text-gray-500">
                              {formatDate(notification.createdAt, 'MM-DD HH:mm')}
                            </span>
                          </div>
                        </div>
                        
                        <p className="text-sm text-gray-600 mt-1 line-clamp-2">
                          {notification.message}
                        </p>

                        {/* Metadata */}
                        {notification.metadata && (
                          <div className="mt-2 flex items-center gap-2 text-xs text-gray-500">
                            {notification.metadata.examTitle && (
                              <div className="flex items-center gap-1">
                                <BookOpen className="h-3 w-3" />
                                {notification.metadata.examTitle}
                              </div>
                            )}
                            {notification.metadata.score !== undefined && (
                              <div className="flex items-center gap-1">
                                <Trophy className="h-3 w-3" />
                                {notification.metadata.score}%
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))
            )}
          </div>

          {/* Footer */}
          <div className="p-4 border-t border-gray-200 text-center">
            <Button
              variant="ghost"
              size="sm"
              className="text-sm text-gray-500"
              onClick={() => {
                // Navigate to full notifications page
                window.location.href = '/notifications';
              }}
            >
              查看所有通知
            </Button>
          </div>
        </div>
      </div>
    </>
  );
};