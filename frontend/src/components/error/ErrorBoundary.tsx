import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertCircle, RefreshCw, Home } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
    };
  }

  static getDerivedStateFromError(error: Error): Partial<State> {
    // Update state so the next render will show the fallback UI
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    // Log error details
    this.setState({
      error,
      errorInfo,
    });

    // You can also log the error to an error reporting service here
    console.error('Error caught by boundary:', error, errorInfo);
  }

  handleRefresh = () => {
    // Clear error state and reload the page
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
    });
    window.location.reload();
  };

  handleGoHome = () => {
    // Clear error state and navigate to home
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
    });
    window.location.href = '/';
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
          <Card className="w-full max-w-md">
            <CardContent className="p-6 text-center">
              <div className="mb-6">
                <AlertCircle className="h-16 w-16 text-red-500 mx-auto mb-4" />
                <h1 className="text-xl font-bold text-gray-900 mb-2">
                  哎呀，出错了！
                </h1>
                <p className="text-gray-600 mb-4">
                  应用程序遇到了意外错误。请尝试刷新页面或返回首页。
                </p>
              </div>

              <div className="space-y-3">
                <Button 
                  onClick={this.handleRefresh}
                  className="w-full btn-mobile"
                >
                  <RefreshCw className="h-4 w-4 mr-2" />
                  刷新页面
                </Button>
                
                <Button 
                  variant="outline"
                  onClick={this.handleGoHome}
                  className="w-full btn-mobile"
                >
                  <Home className="h-4 w-4 mr-2" />
                  返回首页
                </Button>
              </div>

              {/* Error details for development */}
              {process.env.NODE_ENV === 'development' && this.state.error && (
                <details className="mt-6 text-left">
                  <summary className="cursor-pointer text-sm font-medium text-gray-700 mb-2">
                    错误详情 (开发模式)
                  </summary>
                  <div className="bg-gray-100 p-3 rounded text-xs font-mono overflow-auto max-h-32">
                    <div className="text-red-600 font-bold mb-1">
                      {this.state.error.name}: {this.state.error.message}
                    </div>
                    <div className="text-gray-700 whitespace-pre-wrap">
                      {this.state.error.stack}
                    </div>
                    {this.state.errorInfo && (
                      <div className="mt-2 pt-2 border-t border-gray-300">
                        <div className="text-blue-600 font-bold mb-1">Component Stack:</div>
                        <div className="text-gray-700 whitespace-pre-wrap">
                          {this.state.errorInfo.componentStack}
                        </div>
                      </div>
                    )}
                  </div>
                </details>
              )}
            </CardContent>
          </Card>
        </div>
      );
    }

    return this.props.children;
  }
}