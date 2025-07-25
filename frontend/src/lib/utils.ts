import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { format, formatDistanceToNow, isAfter, isBefore, addMinutes } from 'date-fns';
import { zhCN } from 'date-fns/locale';

/**
 * Utility function to merge Tailwind CSS classes
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Format date to readable string
 */
export function formatDate(date: string | Date, formatStr = 'yyyy-MM-dd HH:mm:ss'): string {
  const dateObj = typeof date === 'string' ? new Date(date) : date;
  return format(dateObj, formatStr, { locale: zhCN });
}

/**
 * Format date to relative time (e.g., "2 hours ago")
 */
export function formatRelativeTime(date: string | Date): string {
  const dateObj = typeof date === 'string' ? new Date(date) : date;
  return formatDistanceToNow(dateObj, { addSuffix: true, locale: zhCN });
}

/**
 * Format duration in minutes to readable string
 */
export function formatDuration(minutes: number): string {
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  
  if (hours === 0) {
    return `${mins}分钟`;
  }
  
  if (mins === 0) {
    return `${hours}小时`;
  }
  
  return `${hours}小时${mins}分钟`;
}

/**
 * Format time remaining in seconds to MM:SS format
 */
export function formatTimeRemaining(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
}

/**
 * Format score to percentage
 */
export function formatScore(score: number, maxScore: number): string {
  const percentage = Math.round((score / maxScore) * 100);
  return `${percentage}%`;
}

/**
 * Get grade based on score percentage
 */
export function getGrade(percentage: number): { grade: string; color: string } {
  if (percentage >= 95) return { grade: 'A+', color: 'text-green-600' };
  if (percentage >= 90) return { grade: 'A', color: 'text-green-600' };
  if (percentage >= 85) return { grade: 'A-', color: 'text-green-500' };
  if (percentage >= 80) return { grade: 'B+', color: 'text-blue-600' };
  if (percentage >= 75) return { grade: 'B', color: 'text-blue-600' };
  if (percentage >= 70) return { grade: 'B-', color: 'text-blue-500' };
  if (percentage >= 65) return { grade: 'C+', color: 'text-yellow-600' };
  if (percentage >= 60) return { grade: 'C', color: 'text-yellow-600' };
  if (percentage >= 55) return { grade: 'C-', color: 'text-orange-600' };
  if (percentage >= 50) return { grade: 'D', color: 'text-red-500' };
  return { grade: 'F', color: 'text-red-600' };
}

/**
 * Check if exam is available for taking
 */
export function isExamAvailable(exam: { startTime: string; endTime: string; status: string }): boolean {
  const now = new Date();
  const startTime = new Date(exam.startTime);
  const endTime = new Date(exam.endTime);
  
  return (
    exam.status === 'ACTIVE' &&
    !isBefore(now, startTime) &&
    !isAfter(now, endTime)
  );
}

/**
 * Get exam status badge configuration
 */
export function getExamStatusBadge(status: string): { label: string; variant: string; color: string } {
  const statusMap = {
    SCHEDULED: { label: '未开始', variant: 'secondary', color: 'bg-gray-100 text-gray-800' },
    ACTIVE: { label: '进行中', variant: 'success', color: 'bg-green-100 text-green-800' },
    ENDED: { label: '已结束', variant: 'destructive', color: 'bg-red-100 text-red-800' },
    CANCELLED: { label: '已取消', variant: 'destructive', color: 'bg-red-100 text-red-800' },
  };
  
  return statusMap[status as keyof typeof statusMap] || statusMap.SCHEDULED;
}

/**
 * Get difficulty badge configuration
 */
export function getDifficultyBadge(difficulty: string): { label: string; color: string } {
  const difficultyMap = {
    EASY: { label: '简单', color: 'bg-green-100 text-green-800' },
    MEDIUM: { label: '中等', color: 'bg-yellow-100 text-yellow-800' },
    HARD: { label: '困难', color: 'bg-red-100 text-red-800' },
  };
  
  return difficultyMap[difficulty as keyof typeof difficultyMap] || difficultyMap.MEDIUM;
}

/**
 * Calculate time until exam starts
 */
export function getTimeUntilExam(startTime: string): { isStarted: boolean; timeRemaining: string } {
  const now = new Date();
  const start = new Date(startTime);
  
  if (isBefore(now, start)) {
    return {
      isStarted: false,
      timeRemaining: formatDistanceToNow(start, { locale: zhCN }),
    };
  }
  
  return {
    isStarted: true,
    timeRemaining: '已开始',
  };
}

/**
 * Calculate progress percentage
 */
export function calculateProgress(answered: number, total: number): number {
  if (total === 0) return 0;
  return Math.round((answered / total) * 100);
}

/**
 * Validate email format
 */
export function isValidEmail(email: string): boolean {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

/**
 * Generate random string for IDs
 */
export function generateId(length = 8): string {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

/**
 * Debounce function
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: NodeJS.Timeout;
  return (...args: Parameters<T>) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
}

/**
 * Throttle function
 */
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  limit: number
): (...args: Parameters<T>) => void {
  let inThrottle: boolean;
  return (...args: Parameters<T>) => {
    if (!inThrottle) {
      func(...args);
      inThrottle = true;
      setTimeout(() => (inThrottle = false), limit);
    }
  };
}

/**
 * Deep clone object
 */
export function deepClone<T>(obj: T): T {
  if (obj === null || typeof obj !== 'object') return obj;
  if (obj instanceof Date) return new Date(obj.getTime()) as T;
  if (obj instanceof Array) return obj.map((item) => deepClone(item)) as T;
  if (typeof obj === 'object') {
    const cloned = {} as T;
    Object.keys(obj).forEach((key) => {
      (cloned as any)[key] = deepClone((obj as any)[key]);
    });
    return cloned;
  }
  return obj;
}

/**
 * Safe JSON parse
 */
export function safeJsonParse<T>(jsonString: string, defaultValue: T): T {
  try {
    return JSON.parse(jsonString);
  } catch {
    return defaultValue;
  }
}

/**
 * Local storage helpers
 */
export const storage = {
  get: <T>(key: string, defaultValue: T): T => {
    if (typeof window === 'undefined') return defaultValue;
    const item = localStorage.getItem(key);
    return item ? safeJsonParse(item, defaultValue) : defaultValue;
  },
  
  set: <T>(key: string, value: T): void => {
    if (typeof window === 'undefined') return;
    localStorage.setItem(key, JSON.stringify(value));
  },
  
  remove: (key: string): void => {
    if (typeof window === 'undefined') return;
    localStorage.removeItem(key);
  },
  
  clear: (): void => {
    if (typeof window === 'undefined') return;
    localStorage.clear();
  },
};

/**
 * Session storage helpers
 */
export const sessionStorage = {
  get: <T>(key: string, defaultValue: T): T => {
    if (typeof window === 'undefined') return defaultValue;
    const item = window.sessionStorage.getItem(key);
    return item ? safeJsonParse(item, defaultValue) : defaultValue;
  },
  
  set: <T>(key: string, value: T): void => {
    if (typeof window === 'undefined') return;
    window.sessionStorage.setItem(key, JSON.stringify(value));
  },
  
  remove: (key: string): void => {
    if (typeof window === 'undefined') return;
    window.sessionStorage.removeItem(key);
  },
};

/**
 * URL helpers
 */
export const url = {
  addParams: (baseUrl: string, params: Record<string, any>): string => {
    const url = new URL(baseUrl, window.location.origin);
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        url.searchParams.set(key, String(value));
      }
    });
    return url.toString();
  },
  
  getParam: (param: string): string | null => {
    if (typeof window === 'undefined') return null;
    return new URLSearchParams(window.location.search).get(param);
  },
};

/**
 * Array helpers
 */
export const array = {
  unique: <T>(arr: T[]): T[] => [...new Set(arr)],
  
  chunk: <T>(arr: T[], size: number): T[][] => {
    const chunks: T[][] = [];
    for (let i = 0; i < arr.length; i += size) {
      chunks.push(arr.slice(i, i + size));
    }
    return chunks;
  },
  
  shuffle: <T>(arr: T[]): T[] => {
    const shuffled = [...arr];
    for (let i = shuffled.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
    }
    return shuffled;
  },
};

/**
 * Color helpers
 */
export function getColorFromScore(score: number): string {
  if (score >= 90) return 'text-green-600';
  if (score >= 80) return 'text-blue-600';
  if (score >= 70) return 'text-yellow-600';
  if (score >= 60) return 'text-orange-600';
  return 'text-red-600';
}

/**
 * File size formatter
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes';
  
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

/**
 * Copy to clipboard
 */
export async function copyToClipboard(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    // Fallback for older browsers
    const textArea = document.createElement('textarea');
    textArea.value = text;
    document.body.appendChild(textArea);
    textArea.select();
    document.execCommand('copy');
    document.body.removeChild(textArea);
    return true;
  }
}

/**
 * Download file
 */
export function downloadFile(data: Blob | string, filename: string, type = 'text/plain'): void {
  const blob = data instanceof Blob ? data : new Blob([data], { type });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}