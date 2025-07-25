import React, { useState } from 'react';
import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  FileText,
  HelpCircle,
  BarChart3,
  Users,
  Settings,
  LogOut,
  Menu,
  X,
  Bell,
  Shield,
  Search,
  Moon,
  Sun,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { useAuth } from '@/hooks/useAuth';
import { useNotifications } from '@/hooks/useNotifications';
import { NotificationCenter } from '@/components/notifications/NotificationCenter';
import { cn } from '@/lib/utils';

export const AdminLayout: React.FC = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [darkMode, setDarkMode] = useState(false);
  const { user, logout } = useAuth();
  const { unreadCount, isOpen: notificationOpen, setIsOpen: setNotificationOpen } = useNotifications();
  const location = useLocation();
  const navigate = useNavigate();

  const navigation = [
    { 
      name: '仪表板', 
      href: '/admin/dashboard', 
      icon: LayoutDashboard,
      description: '系统概览和统计'
    },
    { 
      name: '试卷管理', 
      href: '/admin/exams', 
      icon: FileText,
      description: '创建和管理考试试卷'
    },
    { 
      name: '题目管理', 
      href: '/admin/questions', 
      icon: HelpCircle,
      description: '题库管理和题目编辑'
    },
    { 
      name: '成绩查看', 
      href: '/admin/grades', 
      icon: BarChart3,
      description: '查看和分析考试成绩'
    },
    { 
      name: '用户管理', 
      href: '/admin/users', 
      icon: Users,
      description: '管理学生和教师账户'
    },
    { 
      name: '系统设置', 
      href: '/admin/settings', 
      icon: Settings,
      description: '系统配置和参数设置'
    },
  ];

  const isActive = (href: string) => {
    return location.pathname === href || location.pathname.startsWith(href + '/');
  };

  const handleLogout = () => {
    logout();
    navigate('/admin/login');
    setSidebarOpen(false);
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      // Implement global search functionality
      console.log('Searching for:', searchQuery);
    }
  };

  return (
    <div className={cn('min-h-screen bg-slate-50', darkMode && 'dark')}>
      {/* Mobile header */}
      <header className="lg:hidden bg-white border-b border-slate-200 px-4 py-3 shadow-sm">
        <div className="flex items-center justify-between">
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
              <Shield className="h-6 w-6 text-slate-600" />
              <span className="font-bold text-slate-900">管理后台</span>
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
            <div className="w-8 h-8 bg-slate-100 rounded-full flex items-center justify-center">
              <span className="text-sm font-medium text-slate-700">
                {user?.name?.charAt(0) || 'A'}
              </span>
            </div>
          </div>
        </div>
      </header>

      {/* Desktop sidebar */}
      <div className="hidden lg:fixed lg:inset-y-0 lg:flex lg:w-72 lg:flex-col">
        <div className="flex flex-col flex-grow bg-white border-r border-slate-200 shadow-sm">
          {/* Logo */}
          <div className="flex items-center flex-shrink-0 px-6 py-6 border-b border-slate-200">
            <Shield className="h-8 w-8 text-slate-600 mr-3" />
            <div>
              <h1 className="text-xl font-bold text-slate-900">管理后台</h1>
              <p className="text-sm text-slate-600">在线考试系统</p>
            </div>
          </div>

          {/* Search */}
          <div className="px-6 py-4 border-b border-slate-200">
            <form onSubmit={handleSearch}>
              <Input
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="搜索功能、数据..."
                leftIcon={<Search className="h-4 w-4" />}
                className="bg-slate-50"
              />
            </form>
          </div>

          {/* Navigation */}
          <nav className="flex-1 px-4 py-4 space-y-2 overflow-y-auto">
            {navigation.map((item) => (
              <Link
                key={item.name}
                to={item.href}
                className={cn(
                  'group flex items-center px-3 py-3 text-sm font-medium rounded-lg transition-all duration-200',
                  isActive(item.href)
                    ? 'bg-slate-100 text-slate-900 shadow-sm'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                )}
                title={item.description}
              >
                <item.icon
                  className={cn(
                    'mr-3 flex-shrink-0 h-5 w-5',
                    isActive(item.href) ? 'text-slate-700' : 'text-slate-400 group-hover:text-slate-600'
                  )}
                />
                <div className="flex-1">
                  <div>{item.name}</div>
                  <div className="text-xs text-slate-500 mt-0.5 hidden group-hover:block">
                    {item.description}
                  </div>
                </div>
              </Link>
            ))}
          </nav>

          {/* User section */}
          <div className="flex-shrink-0 px-4 py-4 border-t border-slate-200">
            <div className="flex items-center mb-3">
              <div className="w-10 h-10 bg-slate-100 rounded-full flex items-center justify-center mr-3">
                <span className="text-sm font-medium text-slate-700">
                  {user?.name?.charAt(0) || 'A'}
                </span>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-slate-900 truncate">
                  {user?.name || '管理员'}
                </p>
                <div className="flex items-center gap-2">
                  <Badge variant="outline" className="text-xs">
                    {user?.role === 'SUPER_ADMIN' ? '超级管理员' : '管理员'}
                  </Badge>
                  <div className="relative">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setNotificationOpen(true)}
                    >
                      <Bell className="h-4 w-4" />
                    </Button>
                    {unreadCount > 0 && (
                      <Badge 
                        variant="destructive" 
                        className="absolute -top-1 -right-1 h-4 w-4 rounded-full p-0 flex items-center justify-center text-xs"
                      >
                        {unreadCount > 9 ? '9+' : unreadCount}
                      </Badge>
                    )}
                  </div>
                </div>
              </div>
            </div>
            <div className="flex gap-2">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setDarkMode(!darkMode)}
                className="flex-1"
                title={darkMode ? '切换到亮色模式' : '切换到暗色模式'}
              >
                {darkMode ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={handleLogout}
                className="flex-1 text-red-600 hover:text-red-700 hover:bg-red-50"
                title="退出登录"
              >
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
          <div className="fixed inset-y-0 left-0 flex flex-col w-80 max-w-xs bg-white border-r border-slate-200 z-50 lg:hidden">
            {/* Header */}
            <div className="flex items-center justify-between p-4 border-b border-slate-200">
              <div className="flex items-center gap-2">
                <Shield className="h-6 w-6 text-slate-600" />
                <span className="font-bold text-slate-900">管理后台</span>
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
            <div className="p-4 border-b border-slate-200">
              <div className="flex items-center">
                <div className="w-12 h-12 bg-slate-100 rounded-full flex items-center justify-center mr-3">
                  <span className="text-lg font-medium text-slate-700">
                    {user?.name?.charAt(0) || 'A'}
                  </span>
                </div>
                <div className="flex-1">
                  <p className="text-base font-medium text-slate-900">
                    {user?.name || '管理员'}
                  </p>
                  <Badge variant="outline" className="text-xs">
                    {user?.role === 'SUPER_ADMIN' ? '超级管理员' : '管理员'}
                  </Badge>
                </div>
              </div>
            </div>

            {/* Search */}
            <div className="p-4 border-b border-slate-200">
              <form onSubmit={handleSearch}>
                <Input
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="搜索功能、数据..."
                  leftIcon={<Search className="h-4 w-4" />}
                  className="bg-slate-50"
                />
              </form>
            </div>

            {/* Navigation */}
            <nav className="flex-1 px-4 py-4 space-y-2 overflow-y-auto">
              {navigation.map((item) => (
                <Link
                  key={item.name}
                  to={item.href}
                  onClick={() => setSidebarOpen(false)}
                  className={cn(
                    'flex items-center px-4 py-3 text-base font-medium rounded-lg transition-colors touch-target',
                    isActive(item.href)
                      ? 'bg-slate-100 text-slate-900'
                      : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                  )}
                >
                  <item.icon
                    className={cn(
                      'mr-4 flex-shrink-0 h-6 w-6',
                      isActive(item.href) ? 'text-slate-700' : 'text-slate-400'
                    )}
                  />
                  <div>
                    <div>{item.name}</div>
                    <div className="text-xs text-slate-500 mt-0.5">
                      {item.description}
                    </div>
                  </div>
                </Link>
              ))}
            </nav>

            {/* Footer actions */}
            <div className="p-4 border-t border-slate-200 space-y-2">
              <Button
                variant="ghost"
                onClick={() => setDarkMode(!darkMode)}
                className="w-full justify-start"
              >
                {darkMode ? <Sun className="h-5 w-5 mr-3" /> : <Moon className="h-5 w-5 mr-3" />}
                {darkMode ? '亮色模式' : '暗色模式'}
              </Button>
              <Button
                variant="ghost"
                className="w-full justify-start text-red-600 hover:text-red-700 hover:bg-red-50"
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
      <div className="lg:pl-72 flex flex-col flex-1">
        <main id="admin-main-content" className="flex-1" role="main" aria-label="管理员主要内容">
          <Outlet />
        </main>
      </div>

      {/* Notification Center */}
      <NotificationCenter 
        isOpen={notificationOpen} 
        onClose={() => setNotificationOpen(false)} 
      />
    </div>
  );
};