import React, { useState } from 'react';
import { Outlet, Link, useLocation } from 'react-router-dom';
import {
  Home,
  BookOpen,
  User,
  History,
  Menu,
  X,
  LogOut,
  Settings,
  Bell,
  Award,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useAuth } from '@/hooks/useAuth';
import { useNotifications } from '@/hooks/useNotifications';
import { NotificationCenter } from '@/components/notifications/NotificationCenter';
import { cn } from '@/lib/utils';

export const MainLayout: React.FC = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { user, logout } = useAuth();
  const { unreadCount, isOpen: notificationOpen, setIsOpen: setNotificationOpen } = useNotifications();
  const location = useLocation();

  const navigation = [
    { name: '首页', href: '/dashboard', icon: Home },
    { name: '考试', href: '/exams', icon: BookOpen },
    { name: '历史', href: '/history', icon: History },
    { name: '个人', href: '/profile', icon: User },
  ];

  const isActive = (href: string) => {
    return location.pathname === href || location.pathname.startsWith(href + '/');
  };

  const handleLogout = () => {
    logout();
    setSidebarOpen(false);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Mobile header */}
      <header className="lg:hidden bg-white border-b border-gray-200 px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setSidebarOpen(true)}
            className="touch-target"
          >
            <Menu className="h-5 w-5" />
          </Button>
          <div className="flex items-center gap-2">
            <Award className="h-6 w-6 text-exam-600" />
            <span className="font-bold text-gray-900">在线考试</span>
          </div>
        </div>
        
        <div className="flex items-center gap-2">
          <div className="relative">
            <Button 
              variant="ghost" 
              size="sm" 
              className="touch-target"
              onClick={() => setNotificationOpen(true)}
            >
              <Bell className="h-5 w-5" />
            </Button>
            {unreadCount > 0 && (
              <Badge 
                variant="destructive" 
                className="absolute -top-1 -right-1 h-5 w-5 rounded-full p-0 flex items-center justify-center text-xs"
              >
                {unreadCount > 99 ? '99+' : unreadCount}
              </Badge>
            )}
          </div>
          <div className="w-8 h-8 bg-exam-100 rounded-full flex items-center justify-center">
            <span className="text-sm font-medium text-exam-700">
              {user?.name?.charAt(0) || 'U'}
            </span>
          </div>
        </div>
      </header>

      {/* Desktop sidebar */}
      <div className="hidden lg:fixed lg:inset-y-0 lg:flex lg:w-64 lg:flex-col">
        <div className="flex flex-col flex-grow bg-white border-r border-gray-200 pt-5 pb-4 overflow-y-auto">
          {/* Logo */}
          <div className="flex items-center flex-shrink-0 px-4 mb-8">
            <Award className="h-8 w-8 text-exam-600 mr-3" />
            <span className="text-xl font-bold text-gray-900">在线考试系统</span>
          </div>

          {/* Navigation */}
          <nav className="mt-5 flex-1 px-2 space-y-1">
            {navigation.map((item) => (
              <Link
                key={item.name}
                to={item.href}
                className={cn(
                  'group flex items-center px-2 py-2 text-sm font-medium rounded-md transition-colors',
                  isActive(item.href)
                    ? 'bg-exam-100 text-exam-900'
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                )}
              >
                <item.icon
                  className={cn(
                    'mr-3 flex-shrink-0 h-6 w-6',
                    isActive(item.href) ? 'text-exam-500' : 'text-gray-400 group-hover:text-gray-500'
                  )}
                />
                {item.name}
              </Link>
            ))}
          </nav>

          {/* User section */}
          <div className="flex-shrink-0 px-4 py-4 border-t border-gray-200">
            <div className="flex items-center">
              <div className="w-10 h-10 bg-exam-100 rounded-full flex items-center justify-center mr-3">
                <span className="text-sm font-medium text-exam-700">
                  {user?.name?.charAt(0) || 'U'}
                </span>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">
                  {user?.name || '用户'}
                </p>
                <p className="text-xs text-gray-500 truncate">
                  {user?.email}
                </p>
              </div>
            </div>
            <div className="mt-3 flex gap-2">
              <Button variant="ghost" size="sm" className="flex-1" asChild>
                <Link to="/profile">
                  <Settings className="h-4 w-4 mr-2" />
                  设置
                </Link>
              </Button>
              <Button variant="ghost" size="sm" onClick={handleLogout}>
                <LogOut className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Mobile sidebar */}
      {sidebarOpen && (
        <>
          <div
            className="fixed inset-0 bg-black bg-opacity-25 z-40 lg:hidden"
            onClick={() => setSidebarOpen(false)}
          />
          <div className="fixed inset-y-0 left-0 flex flex-col w-80 max-w-xs bg-white border-r border-gray-200 z-50 lg:hidden">
            {/* Header */}
            <div className="flex items-center justify-between p-4 border-b border-gray-200">
              <div className="flex items-center gap-2">
                <Award className="h-6 w-6 text-exam-600" />
                <span className="font-bold text-gray-900">在线考试</span>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setSidebarOpen(false)}
                className="touch-target"
              >
                <X className="h-5 w-5" />
              </Button>
            </div>

            {/* User info */}
            <div className="p-4 border-b border-gray-200">
              <div className="flex items-center">
                <div className="w-12 h-12 bg-exam-100 rounded-full flex items-center justify-center mr-3">
                  <span className="text-lg font-medium text-exam-700">
                    {user?.name?.charAt(0) || 'U'}
                  </span>
                </div>
                <div className="flex-1">
                  <p className="text-base font-medium text-gray-900">
                    {user?.name || '用户'}
                  </p>
                  <p className="text-sm text-gray-500">
                    {user?.email}
                  </p>
                </div>
              </div>
            </div>

            {/* Navigation */}
            <nav className="flex-1 px-4 py-4 space-y-2">
              {navigation.map((item) => (
                <Link
                  key={item.name}
                  to={item.href}
                  onClick={() => setSidebarOpen(false)}
                  className={cn(
                    'flex items-center px-4 py-3 text-base font-medium rounded-lg transition-colors touch-target',
                    isActive(item.href)
                      ? 'bg-exam-100 text-exam-900'
                      : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                  )}
                >
                  <item.icon
                    className={cn(
                      'mr-4 flex-shrink-0 h-6 w-6',
                      isActive(item.href) ? 'text-exam-500' : 'text-gray-400'
                    )}
                  />
                  {item.name}
                </Link>
              ))}
            </nav>

            {/* Footer actions */}
            <div className="p-4 border-t border-gray-200 space-y-2">
              <Button variant="ghost" className="w-full justify-start" asChild>
                <Link to="/profile" onClick={() => setSidebarOpen(false)}>
                  <Settings className="h-5 w-5 mr-3" />
                  设置
                </Link>
              </Button>
              <Button
                variant="ghost"
                className="w-full justify-start text-red-600 hover:text-red-700"
                onClick={handleLogout}
              >
                <LogOut className="h-5 w-5 mr-3" />
                退出登录
              </Button>
            </div>
          </div>
        </>
      )}

      {/* Main content */}
      <div className="lg:pl-64 flex flex-col flex-1">
        <main id="main-content" className="flex-1" role="main" aria-label="主要内容">
          <div className="pb-20 lg:pb-0"> {/* Space for mobile navigation */}
            <Outlet />
          </div>
        </main>
      </div>

      {/* Mobile bottom navigation */}
      <nav className="nav-mobile" role="navigation" aria-label="移动端主导航">
        {navigation.map((item) => (
          <Link
            key={item.name}
            to={item.href}
            className={cn(
              'nav-mobile-item',
              isActive(item.href) && 'active'
            )}
            aria-current={isActive(item.href) ? 'page' : undefined}
          >
            <item.icon className="h-5 w-5 mb-1" aria-hidden="true" />
            <span className="text-xs">{item.name}</span>
          </Link>
        ))}
      </nav>

      {/* Notification Center */}
      <NotificationCenter 
        isOpen={notificationOpen} 
        onClose={() => setNotificationOpen(false)} 
      />
    </div>
  );
};