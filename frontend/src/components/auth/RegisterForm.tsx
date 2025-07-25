import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { User, Mail, Lock, CheckCircle, XCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuth } from '@/hooks/useAuth';
import { RegisterForm as RegisterFormData } from '@/types';

const registerSchema = z.object({
  username: z
    .string()
    .min(3, '用户名至少3个字符')
    .max(20, '用户名不能超过20个字符')
    .regex(/^[a-zA-Z0-9_]+$/, '用户名只能包含字母、数字和下划线'),
  email: z
    .string()
    .email('请输入有效的邮箱地址'),
  fullName: z
    .string()
    .min(2, '姓名至少2个字符')
    .max(50, '姓名不能超过50个字符'),
  password: z
    .string()
    .min(8, '密码至少8个字符')
    .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, '密码必须包含大小写字母和数字'),
  confirmPassword: z.string(),
  acceptTerms: z.boolean().refine(val => val === true, '请同意服务条款'),
}).refine((data) => data.password === data.confirmPassword, {
  message: '两次输入的密码不一致',
  path: ['confirmPassword'],
});

const PasswordStrengthIndicator: React.FC<{ password: string }> = ({ password }) => {
  const requirements = [
    { label: '至少8个字符', test: (pwd: string) => pwd.length >= 8 },
    { label: '包含小写字母', test: (pwd: string) => /[a-z]/.test(pwd) },
    { label: '包含大写字母', test: (pwd: string) => /[A-Z]/.test(pwd) },
    { label: '包含数字', test: (pwd: string) => /\d/.test(pwd) },
  ];

  return (
    <div className="mt-2 space-y-1">
      <p className="text-sm text-gray-600">密码强度要求：</p>
      {requirements.map((req, index) => {
        const isValid = req.test(password);
        return (
          <div key={index} className="flex items-center text-xs">
            {isValid ? (
              <CheckCircle className="w-3 h-3 text-green-500 mr-2" />
            ) : (
              <XCircle className="w-3 h-3 text-gray-300 mr-2" />
            )}
            <span className={isValid ? 'text-green-600' : 'text-gray-500'}>
              {req.label}
            </span>
          </div>
        );
      })}
    </div>
  );
};

export const RegisterForm: React.FC = () => {
  const [currentStep, setCurrentStep] = useState(1);
  const [password, setPassword] = useState('');
  const { register: registerUser, isLoading } = useAuth();
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    formState: { errors },
    setError,
    watch,
    trigger,
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    mode: 'onChange',
    defaultValues: {
      username: '',
      email: '',
      fullName: '',
      password: '',
      confirmPassword: '',
      acceptTerms: false,
    },
  });

  const watchedPassword = watch('password');

  const onSubmit = async (data: RegisterFormData) => {
    try {
      await registerUser(data);
      navigate('/login', {
        state: { message: '注册成功！请使用您的账户登录。' }
      });
    } catch (error: any) {
      if (error.response?.status === 409) {
        const conflictField = error.response.data?.field;
        if (conflictField === 'username') {
          setError('username', {
            type: 'manual',
            message: '该用户名已被使用',
          });
        } else if (conflictField === 'email') {
          setError('email', {
            type: 'manual',
            message: '该邮箱已被注册',
          });
        } else {
          setError('root', {
            type: 'manual',
            message: '用户名或邮箱已存在',
          });
        }
      } else {
        setError('root', {
          type: 'manual',
          message: error.message || '注册失败，请稍后重试',
        });
      }
    }
  };

  const nextStep = async () => {
    const fieldsToValidate = currentStep === 1 
      ? ['username', 'email', 'fullName'] 
      : ['password', 'confirmPassword'];
    
    const isStepValid = await trigger(fieldsToValidate as any);
    if (isStepValid) {
      setCurrentStep(2);
    }
  };

  const previousStep = () => {
    setCurrentStep(1);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-exam-50 to-exam-100 px-4">
      <div className="w-full max-w-md space-y-8">
        {/* Logo and Header */}
        <div className="text-center">
          <div className="mx-auto h-16 w-16 bg-exam-500 rounded-full flex items-center justify-center mb-4">
            <span className="text-2xl font-bold text-white">考</span>
          </div>
          <h1 className="text-3xl font-bold text-gray-900">创建账户</h1>
          <p className="text-gray-600 mt-2">加入我们的在线考试平台</p>
        </div>

        {/* Progress Indicator */}
        <div className="flex justify-center">
          <div className="flex items-center space-x-4">
            <div className={`flex items-center justify-center w-8 h-8 rounded-full ${
              currentStep >= 1 ? 'bg-exam-500 text-white' : 'bg-gray-200 text-gray-500'
            }`}>
              1
            </div>
            <div className={`w-12 h-1 ${
              currentStep >= 2 ? 'bg-exam-500' : 'bg-gray-200'
            }`} />
            <div className={`flex items-center justify-center w-8 h-8 rounded-full ${
              currentStep >= 2 ? 'bg-exam-500 text-white' : 'bg-gray-200 text-gray-500'
            }`}>
              2
            </div>
          </div>
        </div>

        {/* Registration Form */}
        <Card className="shadow-2xl border-0">
          <CardHeader className="space-y-2 pb-4">
            <CardTitle className="text-2xl text-center">
              {currentStep === 1 ? '基本信息' : '设置密码'}
            </CardTitle>
            <CardDescription className="text-center">
              {currentStep === 1 
                ? '请填写您的基本信息' 
                : '请设置一个安全的密码'
              }
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              {/* Global Error */}
              {errors.root && (
                <div className="bg-red-50 border border-red-200 rounded-md p-3">
                  <p className="text-sm text-red-600">{errors.root.message}</p>
                </div>
              )}

              {/* Step 1: Basic Information */}
              {currentStep === 1 && (
                <>
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
                      helperText="3-20个字符，只能包含字母、数字和下划线"
                    />
                  </div>

                  <div>
                    <Input
                      {...register('email')}
                      type="email"
                      label="邮箱地址"
                      placeholder="请输入邮箱地址"
                      leftIcon={<Mail className="h-4 w-4" />}
                      error={errors.email?.message}
                      disabled={isLoading}
                      autoComplete="email"
                    />
                  </div>

                  <div>
                    <Input
                      {...register('fullName')}
                      label="真实姓名"
                      placeholder="请输入真实姓名"
                      leftIcon={<User className="h-4 w-4" />}
                      error={errors.fullName?.message}
                      disabled={isLoading}
                      autoComplete="name"
                    />
                  </div>

                  <Button
                    type="button"
                    variant="exam"
                    size="lg"
                    className="w-full"
                    onClick={nextStep}
                    disabled={isLoading}
                  >
                    下一步
                  </Button>
                </>
              )}

              {/* Step 2: Password Setup */}
              {currentStep === 2 && (
                <>
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
                      autoComplete="new-password"
                      onChange={(e) => setPassword(e.target.value)}
                    />
                    {watchedPassword && (
                      <PasswordStrengthIndicator password={watchedPassword} />
                    )}
                  </div>

                  <div>
                    <Input
                      {...register('confirmPassword')}
                      type="password"
                      label="确认密码"
                      placeholder="请再次输入密码"
                      leftIcon={<Lock className="h-4 w-4" />}
                      showPasswordToggle
                      error={errors.confirmPassword?.message}
                      disabled={isLoading}
                      autoComplete="new-password"
                    />
                  </div>

                  <div>
                    <label className="flex items-start space-x-2">
                      <input
                        {...register('acceptTerms')}
                        type="checkbox"
                        className="mt-1 rounded border-gray-300 text-exam-600 focus:ring-exam-500"
                        disabled={isLoading}
                      />
                      <span className="text-sm text-gray-600">
                        我已阅读并同意{' '}
                        <Link to="/terms" className="text-exam-600 hover:text-exam-500">
                          服务条款
                        </Link>{' '}
                        和{' '}
                        <Link to="/privacy" className="text-exam-600 hover:text-exam-500">
                          隐私政策
                        </Link>
                      </span>
                    </label>
                    {errors.acceptTerms && (
                      <p className="mt-1 text-sm text-red-600">{errors.acceptTerms.message}</p>
                    )}
                  </div>

                  <div className="flex space-x-3">
                    <Button
                      type="button"
                      variant="outline"
                      size="lg"
                      className="flex-1"
                      onClick={previousStep}
                      disabled={isLoading}
                    >
                      上一步
                    </Button>
                    <Button
                      type="submit"
                      variant="exam"
                      size="lg"
                      className="flex-1"
                      loading={isLoading}
                      loadingText="注册中..."
                    >
                      注册
                    </Button>
                  </div>
                </>
              )}
            </form>
          </CardContent>
        </Card>

        {/* Login Link */}
        <div className="text-center">
          <p className="text-sm text-gray-600">
            已有账户？{' '}
            <Link
              to="/login"
              className="font-medium text-exam-600 hover:text-exam-500"
            >
              立即登录
            </Link>
          </p>
        </div>

        {/* Footer */}
        <div className="text-center text-xs text-gray-500">
          <p>© 2024 在线考试系统. 保留所有权利.</p>
        </div>
      </div>
    </div>
  );
};