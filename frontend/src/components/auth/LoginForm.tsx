import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { User, Lock, Eye, EyeOff } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuth } from '@/hooks/useAuth';
import { LoginForm as LoginFormData } from '@/types';

const loginSchema = z.object({
  username: z.string().min(1, '用户名不能为空'),
  password: z.string().min(1, '密码不能为空'),
  rememberMe: z.boolean().default(false),
});

export const LoginForm: React.FC = () => {
  const [showPassword, setShowPassword] = useState(false);
  const { login, isLoading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  
  const from = (location.state as any)?.from?.pathname || '/dashboard';

  const {
    register,
    handleSubmit,
    formState: { errors },
    setError,
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: '',
      password: '',
      rememberMe: false,
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      await login(data);
      navigate(from, { replace: true });
    } catch (error: any) {
      if (error.response?.status === 401) {
        setError('root', {
          type: 'manual',
          message: '用户名或密码错误',
        });
      } else if (error.response?.status === 423) {
        setError('root', {
          type: 'manual',
          message: '账户已被锁定，请联系管理员',
        });
      } else {
        setError('root', {
          type: 'manual',
          message: error.message || '登录失败，请稍后重试',
        });
      }
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-exam-50 to-exam-100 px-4 py-6 safe-area-top safe-area-bottom">
      <div className="w-full max-w-md spacing-responsive">
        {/* Logo and Header */}
        <div className="text-center">
          <div className="mx-auto h-12 w-12 sm:h-16 sm:w-16 bg-exam-500 rounded-full flex items-center justify-center mb-4">
            <span className="text-lg sm:text-2xl font-bold text-white">考</span>
          </div>
          <h1 className="text-responsive-xl font-bold text-gray-900">在线考试系统</h1>
          <p className="text-gray-600 mt-2 text-sm sm:text-base">请登录您的账户</p>
        </div>

        {/* Login Form */}
        <Card className="shadow-2xl border-0">
          <CardHeader className="space-y-2 pb-4">
            <CardTitle className="text-responsive-lg text-center">登录</CardTitle>
            <CardDescription className="text-center text-sm">
              输入您的用户名和密码以访问系统
            </CardDescription>
          </CardHeader>
          <CardContent className="p-4 sm:p-6">
            <form onSubmit={handleSubmit(onSubmit)} className="form-mobile">
              {/* Global Error */}
              {errors.root && (
                <div className="bg-red-50 border border-red-200 rounded-md p-3">
                  <p className="text-sm text-red-600">{errors.root.message}</p>
                </div>
              )}

              {/* Username Field */}
              <div>
                <Input
                  {...register('username')}
                  label="用户名"
                  placeholder="请输入用户名"
                  leftIcon={<User className="h-4 w-4" />}
                  error={errors.username?.message}
                  disabled={isLoading}
                  autoComplete="username"
                  autoFocus
                />
              </div>

              {/* Password Field */}
              <div>
                <Input
                  {...register('password')}
                  type="password"
                  label="密码"
                  placeholder="请输入密码"
                  leftIcon={<Lock className="h-4 w-4" />}
                  showPasswordToggle
                  error={errors.password?.message}
                  disabled={isLoading}
                  autoComplete="current-password"
                />
              </div>

              {/* Remember Me */}
              <div className="flex items-center justify-between">
                <label className="flex items-center">
                  <input
                    {...register('rememberMe')}
                    type="checkbox"
                    className="rounded border-gray-300 text-exam-600 focus:ring-exam-500"
                    disabled={isLoading}
                  />
                  <span className="ml-2 text-sm text-gray-600">记住我</span>
                </label>
                <Link
                  to="/forgot-password"
                  className="text-sm text-exam-600 hover:text-exam-500"
                >
                  忘记密码？
                </Link>
              </div>

              {/* Submit Button */}
              <Button
                type="submit"
                variant="exam"
                size="lg"
                className="w-full btn-mobile"
                loading={isLoading}
                loadingText="登录中..."
              >
                登录
              </Button>

              {/* Quick Login Options */}
              <div className="space-y-2">
                <div className="relative">
                  <div className="absolute inset-0 flex items-center">
                    <span className="w-full border-t" />
                  </div>
                  <div className="relative flex justify-center text-xs uppercase">
                    <span className="bg-white px-2 text-gray-500">演示账户</span>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="btn-mobile"
                    onClick={() => {
                      const form = document.forms[0] as HTMLFormElement;
                      (form.elements.namedItem('username') as HTMLInputElement).value = 'student001';
                      (form.elements.namedItem('password') as HTMLInputElement).value = 'password123';
                    }}
                    disabled={isLoading}
                  >
                    学生账户
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      const form = document.forms[0] as HTMLFormElement;
                      (form.elements.namedItem('username') as HTMLInputElement).value = 'teacher001';
                      (form.elements.namedItem('password') as HTMLInputElement).value = 'password123';
                    }}
                    disabled={isLoading}
                  >
                    教师账户
                  </Button>
                </div>
              </div>
            </form>
          </CardContent>
        </Card>

        {/* Register Link */}
        <div className="text-center">
          <p className="text-sm text-gray-600">
            还没有账户？{' '}
            <Link
              to="/register"
              className="font-medium text-exam-600 hover:text-exam-500"
            >
              立即注册
            </Link>
          </p>
        </div>

        {/* Footer */}
        <div className="text-center text-xs text-gray-500">
          <p>© 2024 在线考试系统. 保留所有权利.</p>
          <div className="mt-2 space-x-4">
            <Link to="/privacy" className="hover:text-gray-700">隐私政策</Link>
            <Link to="/terms" className="hover:text-gray-700">服务条款</Link>
            <Link to="/help" className="hover:text-gray-700">帮助中心</Link>
          </div>
        </div>
      </div>
    </div>
  );
};