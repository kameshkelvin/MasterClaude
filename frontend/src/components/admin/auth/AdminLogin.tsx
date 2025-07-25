import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate, useLocation } from 'react-router-dom';
import { Shield, Eye, EyeOff, Lock, User, AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuth } from '@/hooks/useAuth';
import { LoginForm as LoginFormData } from '@/types';

const adminLoginSchema = z.object({
  username: z.string().min(1, '用户名不能为空'),
  password: z.string().min(1, '密码不能为空'),
  rememberMe: z.boolean().default(false),
});

export const AdminLogin: React.FC = () => {
  const [showPassword, setShowPassword] = useState(false);
  const { login, isLoading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  
  const from = (location.state as any)?.from?.pathname || '/admin/dashboard';

  const {
    register,
    handleSubmit,
    formState: { errors },
    setError,
  } = useForm<LoginFormData>({
    resolver: zodResolver(adminLoginSchema),
    defaultValues: {
      username: '',
      password: '',
      rememberMe: false,
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      const user = await login(data);
      
      // Check if user has admin role
      if (user.role !== 'ADMIN' && user.role !== 'SUPER_ADMIN') {
        setError('root', {
          type: 'manual',
          message: '您没有管理员权限，无法访问后台',
        });
        return;
      }
      
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
          message: '账户已被锁定，请联系系统管理员',
        });
      } else {
        setError('root', {
          type: 'manual',
          message: error.response?.data?.message || '登录失败，请稍后重试',
        });
      }
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-50 to-slate-100 px-4 py-6 safe-area-top safe-area-bottom">
      <div className="w-full max-w-md spacing-responsive">
        {/* Logo and Header */}
        <div className="text-center mb-8">
          <div className="mx-auto h-16 w-16 sm:h-20 sm:w-20 bg-gradient-to-br from-slate-600 to-slate-800 rounded-full flex items-center justify-center mb-4 shadow-lg">
            <Shield className="h-8 w-8 sm:h-10 sm:w-10 text-white" />
          </div>
          <h1 className="text-responsive-xl font-bold text-slate-900 mb-2">
            管理员登录
          </h1>
          <p className="text-slate-600 text-sm sm:text-base">
            在线考试系统 - 后台管理
          </p>
        </div>

        {/* Login Form */}
        <Card className="shadow-2xl border-0 bg-white/80 backdrop-blur-sm">
          <CardHeader className="space-y-2 pb-4">
            <CardTitle className="text-responsive-lg text-center text-slate-800">
              管理员后台
            </CardTitle>
            <CardDescription className="text-center text-sm">
              请输入管理员账号和密码
            </CardDescription>
          </CardHeader>
          <CardContent className="p-4 sm:p-6">
            <form onSubmit={handleSubmit(onSubmit)} className="form-mobile">
              {/* Global Error */}
              {errors.root && (
                <div className="bg-red-50 border border-red-200 rounded-md p-3 mb-4">
                  <div className="flex items-center gap-2">
                    <AlertCircle className="h-4 w-4 text-red-500 flex-shrink-0" />
                    <p className="text-sm text-red-600">{errors.root.message}</p>
                  </div>
                </div>
              )}

              {/* Username Field */}
              <div className="form-field">
                <Input
                  id="username"
                  label="管理员账号"
                  type="text"
                  leftIcon={<User className="h-4 w-4" />}
                  {...register('username')}
                  error={errors.username?.message}
                  placeholder="请输入管理员账号"
                  autoComplete="username"
                  required
                />
              </div>

              {/* Password Field */}
              <div className="form-field">
                <Input
                  id="password"
                  label="密码"
                  type="password"
                  leftIcon={<Lock className="h-4 w-4" />}
                  showPasswordToggle
                  {...register('password')}
                  error={errors.password?.message}
                  placeholder="请输入密码"
                  autoComplete="current-password"
                  required
                />
              </div>

              {/* Remember Me */}
              <div className="flex items-center justify-between text-sm">
                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    {...register('rememberMe')}
                    className="rounded border-gray-300 text-slate-600 focus:ring-slate-500"
                  />
                  <span className="text-slate-600">记住我</span>
                </label>
                <button
                  type="button"
                  className="text-slate-600 hover:text-slate-800 transition-colors"
                  onClick={() => {
                    // Handle forgot password for admin
                    alert('请联系系统管理员重置密码');
                  }}
                >
                  忘记密码？
                </button>
              </div>

              {/* Submit Button */}
              <Button
                type="submit"
                className="w-full btn-mobile bg-slate-700 hover:bg-slate-800 focus:ring-slate-500"
                size="lg"
                loading={isLoading}
                loadingText="登录中..."
              >
                登录管理后台
              </Button>

              {/* Demo Admin Accounts */}
              <div className="space-y-2">
                <div className="relative">
                  <div className="absolute inset-0 flex items-center">
                    <span className="w-full border-t" />
                  </div>
                  <div className="relative flex justify-center text-xs uppercase">
                    <span className="bg-white px-2 text-slate-500">演示账户</span>
                  </div>
                </div>
                <div className="grid grid-cols-1 gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="btn-mobile text-xs"
                    onClick={() => {
                      const form = document.forms[0] as HTMLFormElement;
                      (form.elements.namedItem('username') as HTMLInputElement).value = 'admin001';
                      (form.elements.namedItem('password') as HTMLInputElement).value = 'admin123';
                    }}
                  >
                    👑 超级管理员 (admin001)
                  </Button>
                </div>
              </div>

              {/* Security Notice */}
              <div className="mt-6 p-3 bg-amber-50 border border-amber-200 rounded-lg">
                <div className="flex items-start gap-2">
                  <Shield className="h-4 w-4 text-amber-600 flex-shrink-0 mt-0.5" />
                  <div className="text-xs text-amber-800">
                    <p className="font-medium mb-1">安全提醒</p>
                    <p>
                      管理员后台包含敏感数据，请确保在安全环境下登录，
                      使用完毕后请及时退出登录。
                    </p>
                  </div>
                </div>
              </div>
            </form>
          </CardContent>
        </Card>

        {/* Footer */}
        <div className="text-center mt-6 text-xs text-slate-500">
          <p>© 2024 在线考试系统 - 管理员后台</p>
          <p className="mt-1">请妥善保管您的登录凭据</p>
        </div>
      </div>
    </div>
  );
};