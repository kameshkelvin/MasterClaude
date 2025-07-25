import axios, { AxiosResponse, AxiosError } from 'axios';
import { storage } from '@/lib/utils';
import { ApiResponse, AuthUser, LoginForm, RegisterForm } from '@/types';

// Create axios instance
export const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const user = storage.get<AuthUser | null>('user', null);
    if (user?.accessToken) {
      config.headers.Authorization = `Bearer ${user.accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle errors and token refresh
api.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as any;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const user = storage.get<AuthUser | null>('user', null);
        if (user?.refreshToken) {
          const response = await api.post('/auth/refresh', {
            refreshToken: user.refreshToken,
          });

          const newUser = response.data.data;
          storage.set('user', newUser);

          // Retry original request with new token
          originalRequest.headers.Authorization = `Bearer ${newUser.accessToken}`;
          return api(originalRequest);
        }
      } catch (refreshError) {
        // Refresh failed, redirect to login
        storage.remove('user');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

// API endpoints
export const authApi = {
  login: (credentials: LoginForm): Promise<AxiosResponse<ApiResponse<AuthUser>>> =>
    api.post('/auth/login', credentials),

  register: (data: RegisterForm): Promise<AxiosResponse<ApiResponse<AuthUser>>> =>
    api.post('/auth/register', data),

  logout: (): Promise<AxiosResponse<ApiResponse<void>>> =>
    api.post('/auth/logout'),

  refreshToken: (refreshToken: string): Promise<AxiosResponse<ApiResponse<AuthUser>>> =>
    api.post('/auth/refresh', { refreshToken }),

  forgotPassword: (email: string): Promise<AxiosResponse<ApiResponse<void>>> =>
    api.post('/auth/forgot-password', { email }),

  resetPassword: (token: string, password: string): Promise<AxiosResponse<ApiResponse<void>>> =>
    api.post('/auth/reset-password', { token, password }),

  verifyEmail: (token: string): Promise<AxiosResponse<ApiResponse<void>>> =>
    api.post('/auth/verify-email', { token }),

  resendVerification: (email: string): Promise<AxiosResponse<ApiResponse<void>>> =>
    api.post('/auth/resend-verification', { email }),
};

export const examApi = {
  // Student exam endpoints
  getAvailableExams: (params?: any): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get('/student/exams/available', { params }),

  getExamInfo: (examId: string): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get(`/student/exams/${examId}`),

  startExam: (examId: string): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.post(`/student/exams/${examId}/start`),

  getExamQuestions: (examId: string, attemptId: string): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get(`/student/exams/${examId}/questions`, { params: { attemptId } }),

  submitAnswer: (examId: string, questionId: string, data: any): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.post(`/student/exams/${examId}/questions/${questionId}/answer`, data),

  submitAnswersBatch: (examId: string, data: any): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.post(`/student/exams/${examId}/answers/batch`, data),

  finishExam: (examId: string, data: any): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.post(`/student/exams/${examId}/finish`, data),

  getExamResult: (examId: string, attemptId: string): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get(`/student/exams/${examId}/result`, { params: { attemptId } }),

  getExamHistory: (params?: any): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get('/student/exams/history', { params }),

  getSessionStatus: (examId: string, attemptId: string): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get(`/student/exams/${examId}/session/${attemptId}/status`),

  getExamProgress: (examId: string, attemptId: string): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get(`/student/exams/${examId}/progress`, { params: { attemptId } }),
};

export const userApi = {
  getProfile: (): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get('/user/profile'),

  updateProfile: (data: any): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.put('/user/profile', data),

  changePassword: (data: any): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.post('/user/change-password', data),

  uploadAvatar: (file: File): Promise<AxiosResponse<ApiResponse<any>>> => {
    const formData = new FormData();
    formData.append('avatar', file);
    return api.post('/user/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  getNotifications: (params?: any): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get('/user/notifications', { params }),

  markNotificationRead: (notificationId: string): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.patch(`/user/notifications/${notificationId}/read`),

  markNotificationAsRead: (notificationId: string): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.put(`/user/notifications/${notificationId}/read`),

  markAllNotificationsAsRead: (): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.put('/user/notifications/read-all'),

  getStatistics: (): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get('/user/statistics'),

  // Progress tracking
  getProgressData: (timeRange: string): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get(`/user/progress?range=${timeRange}`),

  getDashboard: (): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get('/user/dashboard'),

  getRecentResults: (limit: number): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get(`/user/results/recent?limit=${limit}`),

  getExamHistory: (params: any): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get('/user/exam-history', { params }),
};

export const adminApi = {
  // Admin exam management
  getExams: (params?: any): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get('/admin/exams', { params }),

  createExam: (data: any): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.post('/admin/exams', data),

  updateExam: (examId: string, data: any): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.put(`/admin/exams/${examId}`, data),

  deleteExam: (examId: string): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.delete(`/admin/exams/${examId}`),

  publishExam: (examId: string): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.post(`/admin/exams/${examId}/publish`),

  // Question management
  getQuestions: (params?: any): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get('/admin/questions', { params }),

  createQuestion: (data: any): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.post('/admin/questions', data),

  updateQuestion: (questionId: string, data: any): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.put(`/admin/questions/${questionId}`, data),

  deleteQuestion: (questionId: string): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.delete(`/admin/questions/${questionId}`),

  // Statistics and reports
  getGradeStatistics: (examId: string): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get(`/admin/grade-statistics/exam/${examId}`),

  getUserStatistics: (userId: string): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get(`/admin/grade-statistics/user/${userId}`),

  getComprehensiveReport: (): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get('/admin/grade-statistics/comprehensive-report'),

  getDashboard: (): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.get('/admin/grade-statistics/dashboard'),

  exportStatistics: (examId: string, format: string): Promise<AxiosResponse<ApiResponse<any>>> =>
    api.post(`/admin/grade-statistics/export/${examId}`, { format }),
};

// Error handling utilities
export const handleApiError = (error: AxiosError): string => {
  if (error.response?.data) {
    const data = error.response.data as any;
    if (data.message) {
      return data.message;
    }
    if (data.errors && typeof data.errors === 'object') {
      return Object.values(data.errors).flat().join(', ') as string;
    }
  }

  if (error.message) {
    return error.message;
  }

  return '网络错误，请稍后重试';
};

// Upload progress callback type
export type UploadProgressCallback = (progress: number) => void;

// File upload utility
export const uploadFile = (
  file: File,
  endpoint: string,
  onProgress?: UploadProgressCallback
): Promise<AxiosResponse<ApiResponse<any>>> => {
  const formData = new FormData();
  formData.append('file', file);

  return api.post(endpoint, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        onProgress(progress);
      }
    },
  });
};

// Download file utility
export const downloadFile = async (url: string, filename?: string): Promise<void> => {
  try {
    const response = await api.get(url, {
      responseType: 'blob',
    });

    const blob = new Blob([response.data]);
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = filename || 'download';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(downloadUrl);
  } catch (error) {
    console.error('Download failed:', error);
    throw new Error('文件下载失败');
  }
};

export default api;