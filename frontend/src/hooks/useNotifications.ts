import { useState, useEffect, useCallback } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { userApi } from '@/lib/api';
import toast from 'react-hot-toast';

interface NotificationSettings {
  examReminders: boolean;
  resultNotifications: boolean;
  systemUpdates: boolean;
  soundEnabled: boolean;
  browserNotifications: boolean;
}

export const useNotifications = () => {
  const queryClient = useQueryClient();
  const [isOpen, setIsOpen] = useState(false);
  const [settings, setSettings] = useState<NotificationSettings>({
    examReminders: true,
    resultNotifications: true,
    systemUpdates: true,
    soundEnabled: true,
    browserNotifications: false,
  });

  // Fetch notifications
  const { data: notifications = [], isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: () => userApi.getNotifications(),
    select: (response) => response.data.data,
    refetchInterval: 30000, // Refresh every 30 seconds
  });

  // Get unread count
  const unreadCount = notifications.filter((n: any) => !n.isRead).length;

  // Request browser notification permission
  const requestNotificationPermission = useCallback(async () => {
    if ('Notification' in window) {
      const permission = await Notification.requestPermission();
      setSettings(prev => ({
        ...prev,
        browserNotifications: permission === 'granted',
      }));
      return permission === 'granted';
    }
    return false;
  }, []);

  // Show browser notification
  const showBrowserNotification = useCallback((title: string, body: string, icon?: string) => {
    if ('Notification' in window && Notification.permission === 'granted' && settings.browserNotifications) {
      const notification = new Notification(title, {
        body,
        icon: icon || '/favicon.ico',
        tag: 'exam-notification',
        requireInteraction: false,
      });

      // Auto close after 5 seconds
      setTimeout(() => {
        notification.close();
      }, 5000);

      return notification;
    }
    return null;
  }, [settings.browserNotifications]);

  // Play notification sound
  const playNotificationSound = useCallback(() => {
    if (settings.soundEnabled) {
      // Create a subtle notification sound
      const audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      const oscillator = audioContext.createOscillator();
      const gainNode = audioContext.createGain();

      oscillator.connect(gainNode);
      gainNode.connect(audioContext.destination);

      oscillator.frequency.setValueAtTime(800, audioContext.currentTime);
      oscillator.frequency.setValueAtTime(600, audioContext.currentTime + 0.1);
      
      gainNode.gain.setValueAtTime(0, audioContext.currentTime);
      gainNode.gain.linearRampToValueAtTime(0.1, audioContext.currentTime + 0.01);
      gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.3);

      oscillator.start(audioContext.currentTime);
      oscillator.stop(audioContext.currentTime + 0.3);
    }
  }, [settings.soundEnabled]);

  // Handle new notification
  const handleNewNotification = useCallback((notification: any) => {
    // Show toast notification
    const toastContent = (
      <div className="flex items-start gap-3">
        <div className="flex-1">
          <p className="font-medium text-sm">{notification.title}</p>
          <p className="text-sm text-gray-600">{notification.message}</p>
        </div>
      </div>
    );

    toast.custom(toastContent, {
      duration: 5000,
      position: 'top-right',
    });

    // Show browser notification
    showBrowserNotification(notification.title, notification.message);

    // Play sound
    playNotificationSound();

    // Invalidate notifications query to refresh
    queryClient.invalidateQueries({ queryKey: ['notifications'] });
  }, [showBrowserNotification, playNotificationSound, queryClient]);

  // Set up WebSocket connection for real-time notifications
  useEffect(() => {
    let ws: WebSocket | null = null;
    let reconnectTimer: NodeJS.Timeout | null = null;
    let heartbeatTimer: NodeJS.Timeout | null = null;

    const connect = () => {
      try {
        // Get auth token
        const token = localStorage.getItem('authToken');
        if (!token) return;

        // Create WebSocket connection
        const wsUrl = `${process.env.VITE_WS_URL || 'ws://localhost:3001'}/notifications?token=${token}`;
        ws = new WebSocket(wsUrl);

        ws.onopen = () => {
          console.log('Notifications WebSocket connected');
          
          // Start heartbeat
          heartbeatTimer = setInterval(() => {
            if (ws?.readyState === WebSocket.OPEN) {
              ws.send(JSON.stringify({ type: 'ping' }));
            }
          }, 30000);
        };

        ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            
            switch (data.type) {
              case 'notification':
                handleNewNotification(data.payload);
                break;
              case 'exam_reminder':
                if (settings.examReminders) {
                  handleNewNotification({
                    title: '考试提醒',
                    message: data.payload.message,
                    type: 'exam_reminder',
                  });
                }
                break;
              case 'result_ready':
                if (settings.resultNotifications) {
                  handleNewNotification({
                    title: '成绩已出',
                    message: data.payload.message,
                    type: 'result_ready',
                  });
                }
                break;
              case 'system_update':
                if (settings.systemUpdates) {
                  handleNewNotification({
                    title: '系统通知',
                    message: data.payload.message,
                    type: 'system_update',
                  });
                }
                break;
              case 'pong':
                // Heartbeat response
                break;
            }
          } catch (error) {
            console.error('Error parsing WebSocket message:', error);
          }
        };

        ws.onclose = () => {
          console.log('Notifications WebSocket disconnected');
          if (heartbeatTimer) {
            clearInterval(heartbeatTimer);
          }
          
          // Attempt to reconnect after 5 seconds
          reconnectTimer = setTimeout(connect, 5000);
        };

        ws.onerror = (error) => {
          console.error('WebSocket error:', error);
        };

      } catch (error) {
        console.error('Failed to connect to notifications WebSocket:', error);
        reconnectTimer = setTimeout(connect, 5000);
      }
    };

    connect();

    return () => {
      if (ws) {
        ws.close();
      }
      if (reconnectTimer) {
        clearTimeout(reconnectTimer);
      }
      if (heartbeatTimer) {
        clearInterval(heartbeatTimer);
      }
    };
  }, [handleNewNotification, settings]);

  // Load settings from localStorage
  useEffect(() => {
    const savedSettings = localStorage.getItem('notificationSettings');
    if (savedSettings) {
      try {
        setSettings(JSON.parse(savedSettings));
      } catch (error) {
        console.error('Error loading notification settings:', error);
      }
    }
  }, []);

  // Save settings to localStorage
  const updateSettings = useCallback((newSettings: Partial<NotificationSettings>) => {
    setSettings(prev => {
      const updated = { ...prev, ...newSettings };
      localStorage.setItem('notificationSettings', JSON.stringify(updated));
      return updated;
    });
  }, []);

  // Mark notification as read
  const markAsRead = useCallback(async (notificationId: string) => {
    try {
      await userApi.markNotificationAsRead(notificationId);
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    } catch (error) {
      toast.error('标记失败');
    }
  }, [queryClient]);

  // Mark all notifications as read
  const markAllAsRead = useCallback(async () => {
    try {
      await userApi.markAllNotificationsAsRead();
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      toast.success('已标记所有通知为已读');
    } catch (error) {
      toast.error('操作失败');
    }
  }, [queryClient]);

  return {
    notifications,
    unreadCount,
    isLoading,
    isOpen,
    setIsOpen,
    settings,
    updateSettings,
    markAsRead,
    markAllAsRead,
    requestNotificationPermission,
    showBrowserNotification,
  };
};