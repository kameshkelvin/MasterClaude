import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  User,
  Mail,
  Phone,
  Calendar,
  Edit3,
  Save,
  X,
  Shield,
  Bell,
  Palette,
  Globe,
  Camera,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { useAuth } from '@/hooks/useAuth';
import { userApi } from '@/lib/api';
import { formatDate, cn } from '@/lib/utils';
import toast from 'react-hot-toast';

interface ProfileFormData {
  name: string;
  email: string;
  phone?: string;
  bio?: string;
}

export const Profile: React.FC = () => {
  const { user, updateUser } = useAuth();
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState<ProfileFormData>({
    name: user?.name || '',
    email: user?.email || '',
    phone: user?.phone || '',
    bio: user?.bio || '',
  });

  // Fetch user profile details
  const { data: profile, isLoading } = useQuery({
    queryKey: ['profile', user?.id],
    queryFn: () => userApi.getProfile(),
    select: (response) => response.data.data,
    enabled: !!user?.id,
  });

  // Update profile mutation
  const updateProfileMutation = useMutation({
    mutationFn: (data: ProfileFormData) => userApi.updateProfile(data),
    onSuccess: (response) => {
      const updatedUser = response.data.data;
      updateUser(updatedUser);
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      setIsEditing(false);
      toast.success('个人资料更新成功');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || '更新失败');
    },
  });

  const handleSave = () => {
    if (!formData.name.trim() || !formData.email.trim()) {
      toast.error('姓名和邮箱不能为空');
      return;
    }
    updateProfileMutation.mutate(formData);
  };

  const handleCancel = () => {
    setFormData({
      name: user?.name || '',
      email: user?.email || '',
      phone: user?.phone || '',
      bio: user?.bio || '',
    });
    setIsEditing(false);
  };

  if (isLoading) {
    return (
      <div className="container-responsive py-6">
        <div className="animate-pulse space-y-6">
          <div className="h-8 bg-gray-200 rounded w-1/3"></div>
          <div className="h-64 bg-gray-200 rounded"></div>
        </div>
      </div>
    );
  }

  const stats = profile?.stats || {
    totalExams: 0,
    completedExams: 0,
    averageScore: 0,
    rank: null,
  };

  return (
    <div className="container-responsive py-4 sm:py-6 spacing-responsive">
      {/* Header */}
      <div className="flex items-center justify-between mb-6 sm:mb-8">
        <h1 className="text-responsive-xl font-bold text-gray-900">个人资料</h1>
        {!isEditing && (
          <Button
            onClick={() => setIsEditing(true)}
            className="btn-mobile"
          >
            <Edit3 className="h-4 w-4 mr-2" />
            编辑
          </Button>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main profile card */}
        <div className="lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <User className="h-5 w-5" />
                基本信息
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* Avatar section */}
              <div className="flex items-center gap-4">
                <div className="relative">
                  <div className="w-16 h-16 sm:w-20 sm:h-20 bg-exam-100 rounded-full flex items-center justify-center">
                    <span className="text-xl sm:text-2xl font-bold text-exam-700">
                      {user?.name?.charAt(0) || 'U'}
                    </span>
                  </div>
                  {isEditing && (
                    <button className="absolute -bottom-1 -right-1 w-8 h-8 bg-exam-500 rounded-full flex items-center justify-center text-white hover:bg-exam-600 transition-colors">
                      <Camera className="h-4 w-4" />
                    </button>
                  )}
                </div>
                <div className="flex-1">
                  <h3 className="text-lg sm:text-xl font-semibold text-gray-900">
                    {user?.name}
                  </h3>
                  <div className="flex items-center gap-2 mt-1">
                    <Badge variant="outline">{user?.role === 'ADMIN' ? '管理员' : '学生'}</Badge>
                    <span className="text-sm text-gray-500">
                      注册于 {formatDate(user?.createdAt, 'YYYY年MM月')}
                    </span>
                  </div>
                </div>
              </div>

              {/* Form fields */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="form-field">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    姓名 *
                  </label>
                  {isEditing ? (
                    <Input
                      value={formData.name}
                      onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
                      className="form-mobile"
                      placeholder="请输入姓名"
                    />
                  ) : (
                    <div className="p-3 bg-gray-50 rounded-lg text-gray-900">
                      {user?.name || '未设置'}
                    </div>
                  )}
                </div>

                <div className="form-field">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    邮箱 *
                  </label>
                  {isEditing ? (
                    <Input
                      type="email"
                      value={formData.email}
                      onChange={(e) => setFormData(prev => ({ ...prev, email: e.target.value }))}
                      className="form-mobile"
                      placeholder="请输入邮箱"
                    />
                  ) : (
                    <div className="p-3 bg-gray-50 rounded-lg text-gray-900 flex items-center gap-2">
                      <Mail className="h-4 w-4 text-gray-500" />
                      {user?.email}
                    </div>
                  )}
                </div>

                <div className="form-field">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    手机号
                  </label>
                  {isEditing ? (
                    <Input
                      type="tel"
                      value={formData.phone}
                      onChange={(e) => setFormData(prev => ({ ...prev, phone: e.target.value }))}
                      className="form-mobile"
                      placeholder="请输入手机号"
                    />
                  ) : (
                    <div className="p-3 bg-gray-50 rounded-lg text-gray-900 flex items-center gap-2">
                      <Phone className="h-4 w-4 text-gray-500" />
                      {user?.phone || '未设置'}
                    </div>
                  )}
                </div>

                <div className="form-field">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    注册时间
                  </label>
                  <div className="p-3 bg-gray-50 rounded-lg text-gray-900 flex items-center gap-2">
                    <Calendar className="h-4 w-4 text-gray-500" />
                    {formatDate(user?.createdAt, 'YYYY-MM-DD')}
                  </div>
                </div>
              </div>

              <div className="form-field">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  个人简介
                </label>
                {isEditing ? (
                  <textarea
                    value={formData.bio}
                    onChange={(e) => setFormData(prev => ({ ...prev, bio: e.target.value }))}
                    className="w-full p-3 border border-gray-300 rounded-lg resize-none text-base"
                    rows={3}
                    placeholder="介绍一下自己吧..."
                  />
                ) : (
                  <div className="p-3 bg-gray-50 rounded-lg text-gray-900 min-h-[80px]">
                    {user?.bio || '这个人很懒，什么都没有留下...'}
                  </div>
                )}
              </div>

              {/* Action buttons */}
              {isEditing && (
                <div className="flex gap-3 pt-4 border-t border-gray-200">
                  <Button
                    onClick={handleSave}
                    disabled={updateProfileMutation.isPending}
                    className="flex-1 btn-mobile"
                  >
                    <Save className="h-4 w-4 mr-2" />
                    {updateProfileMutation.isPending ? '保存中...' : '保存'}
                  </Button>
                  <Button
                    variant="outline"
                    onClick={handleCancel}
                    className="flex-1 btn-mobile"
                  >
                    <X className="h-4 w-4 mr-2" />
                    取消
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Stats card */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">考试统计</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="text-center">
                <div className="text-2xl font-bold text-exam-600 mb-1">
                  {stats.averageScore?.toFixed(1) || '0'}%
                </div>
                <div className="text-sm text-gray-500">平均分</div>
              </div>
              
              <div className="grid grid-cols-2 gap-4 text-center">
                <div>
                  <div className="text-xl font-bold text-blue-600">
                    {stats.totalExams}
                  </div>
                  <div className="text-xs text-gray-500">参加考试</div>
                </div>
                <div>
                  <div className="text-xl font-bold text-green-600">
                    {stats.completedExams}
                  </div>
                  <div className="text-xs text-gray-500">已完成</div>
                </div>
              </div>

              {stats.rank && (
                <div className="text-center p-3 bg-yellow-50 rounded-lg border border-yellow-200">
                  <div className="text-lg font-bold text-yellow-700">
                    #{stats.rank}
                  </div>
                  <div className="text-xs text-yellow-600">当前排名</div>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Settings shortcuts */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">设置</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <button className="w-full flex items-center gap-3 p-3 text-left hover:bg-gray-50 rounded-lg transition-colors">
                <Shield className="h-5 w-5 text-gray-500" />
                <div>
                  <div className="font-medium text-gray-900">安全设置</div>
                  <div className="text-sm text-gray-500">密码和安全</div>
                </div>
              </button>
              
              <button className="w-full flex items-center gap-3 p-3 text-left hover:bg-gray-50 rounded-lg transition-colors">
                <Bell className="h-5 w-5 text-gray-500" />
                <div>
                  <div className="font-medium text-gray-900">通知设置</div>
                  <div className="text-sm text-gray-500">消息和提醒</div>
                </div>
              </button>
              
              <button className="w-full flex items-center gap-3 p-3 text-left hover:bg-gray-50 rounded-lg transition-colors">
                <Palette className="h-5 w-5 text-gray-500" />
                <div>
                  <div className="font-medium text-gray-900">外观设置</div>
                  <div className="text-sm text-gray-500">主题和显示</div>
                </div>
              </button>
              
              <button className="w-full flex items-center gap-3 p-3 text-left hover:bg-gray-50 rounded-lg transition-colors">
                <Globe className="h-5 w-5 text-gray-500" />
                <div>
                  <div className="font-medium text-gray-900">语言设置</div>
                  <div className="text-sm text-gray-500">简体中文</div>
                </div>
              </button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};