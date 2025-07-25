import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Home, ArrowLeft, Search, BookOpen } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';

export const NotFound: React.FC = () => {
  const navigate = useNavigate();

  const handleGoBack = () => {
    if (window.history.length > 1) {
      navigate(-1);
    } else {
      navigate('/');
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
      <Card className="w-full max-w-lg">
        <CardContent className="p-6 sm:p-8 text-center">
          {/* 404 illustration */}
          <div className="mb-8">
            <div className="text-6xl sm:text-8xl font-bold text-gray-200 mb-4">
              404
            </div>
            <div className="relative">
              <Search className="h-16 w-16 sm:h-20 sm:w-20 text-gray-300 mx-auto" />
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="w-8 h-8 sm:w-10 sm:h-10 border-2 border-gray-400 rounded-full border-dashed animate-spin"></div>
              </div>
            </div>
          </div>

          {/* Error message */}
          <div className="mb-8">
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 mb-3">
              页面未找到
            </h1>
            <p className="text-gray-600 mb-2">
              抱歉，您访问的页面不存在或已被移动。
            </p>
            <p className="text-sm text-gray-500">
              请检查网址是否正确，或使用下方按钮导航到其他页面。
            </p>
          </div>

          {/* Navigation buttons */}
          <div className="space-y-3">
            <Button 
              onClick={handleGoBack}
              className="w-full btn-mobile"
            >
              <ArrowLeft className="h-4 w-4 mr-2" />
              返回上一页
            </Button>
            
            <Button 
              variant="outline"
              asChild
              className="w-full btn-mobile"
            >
              <Link to="/">
                <Home className="h-4 w-4 mr-2" />
                返回首页
              </Link>
            </Button>
            
            <Button 
              variant="outline"
              asChild
              className="w-full btn-mobile"
            >
              <Link to="/exams">
                <BookOpen className="h-4 w-4 mr-2" />
                浏览考试
              </Link>
            </Button>
          </div>

          {/* Help text */}
          <div className="mt-8 pt-6 border-t border-gray-200">
            <p className="text-xs text-gray-500">
              如果您认为这是一个错误，请联系系统管理员。
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};