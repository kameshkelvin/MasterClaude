import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { Toaster } from 'react-hot-toast';

// Layout components
import { ProtectedRoute } from './components/auth/ProtectedRoute';
import { PublicRoute } from './components/auth/PublicRoute';
import { MainLayout } from './components/layout/MainLayout';

// Page components
import { LoginForm } from './components/auth/LoginForm';
import { RegisterForm } from './components/auth/RegisterForm';
import { ExamList } from './components/exam/ExamList';
import { ExamInterface } from './components/exam/ExamInterface';
import { ExamResults } from './components/exam/ExamResults';
import { Dashboard } from './components/dashboard/Dashboard';
import { Profile } from './components/user/Profile';
import { ExamHistory } from './components/user/ExamHistory';

// Error components
import { NotFound } from './components/error/NotFound';
import { ErrorBoundary } from './components/error/ErrorBoundary';

// Styles
import './index.css';

// Create a client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      gcTime: 1000 * 60 * 10, // 10 minutes
      retry: (failureCount, error: any) => {
        // Don't retry on 401, 403, or 404 errors
        if (error?.response?.status === 401 || 
            error?.response?.status === 403 || 
            error?.response?.status === 404) {
          return false;
        }
        // Retry up to 3 times for other errors
        return failureCount < 3;
      },
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
});

function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <Router>
          {/* Skip to main content link */}
          <a 
            href="#main-content"
            className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:bg-white focus:text-black focus:px-4 focus:py-2 focus:rounded focus:shadow-lg focus:ring-2 focus:ring-exam-500"
          >
            跳转到主要内容
          </a>
          <div className="App">
            <Routes>
              {/* Public routes (accessible when not authenticated) */}
              <Route path="/login" element={
                <PublicRoute>
                  <LoginForm />
                </PublicRoute>
              } />
              <Route path="/register" element={
                <PublicRoute>
                  <RegisterForm />
                </PublicRoute>
              } />

              {/* Protected routes (require authentication) */}
              <Route path="/" element={
                <ProtectedRoute>
                  <MainLayout />
                </ProtectedRoute>
              }>
                {/* Dashboard */}
                <Route index element={<Navigate to="/dashboard" replace />} />
                <Route path="dashboard" element={<Dashboard />} />

                {/* Exam routes */}
                <Route path="exams" element={<ExamList />} />
                <Route path="exam/:examId" element={<ExamInterface />} />
                <Route path="exam/:examId/start" element={<ExamInterface />} />
                <Route path="exam/:examId/result" element={<ExamResults />} />

                {/* User routes */}
                <Route path="profile" element={<Profile />} />
                <Route path="history" element={<ExamHistory />} />

                {/* Admin routes (will be protected by role) */}
                <Route path="admin/*" element={<div>Admin Panel (Coming Soon)</div>} />
              </Route>

              {/* 404 route */}
              <Route path="*" element={<NotFound />} />
            </Routes>
          </div>
        </Router>

        {/* Global components */}
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 4000,
            style: {
              background: '#363636',
              color: '#fff',
            },
            success: {
              duration: 3000,
              iconTheme: {
                primary: '#10b981',
                secondary: '#fff',
              },
            },
            error: {
              duration: 5000,
              iconTheme: {
                primary: '#ef4444',
                secondary: '#fff',
              },
            },
          }}
        />

        {/* React Query Devtools */}
        {process.env.NODE_ENV === 'development' && (
          <ReactQueryDevtools initialIsOpen={false} />
        )}
      </QueryClientProvider>
    </ErrorBoundary>
  );
}

export default App;