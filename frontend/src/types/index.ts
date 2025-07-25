// User Types
export interface User {
  id: string;
  username: string;
  email: string;
  fullName: string;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
  avatar?: string;
  createdAt: string;
  lastLoginAt?: string;
}

export interface AuthUser extends User {
  accessToken: string;
  refreshToken: string;
}

// Exam Types
export interface Exam {
  id: string;
  title: string;
  description: string;
  startTime: string;
  endTime: string;
  duration: number; // in minutes
  maxAttempts: number;
  passingScore: number;
  status: 'SCHEDULED' | 'ACTIVE' | 'ENDED' | 'CANCELLED';
  instructions?: string;
  isRandomized: boolean;
  showResults: boolean;
  allowReview: boolean;
  questionCount: number;
  totalPoints: number;
  tags: string[];
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  estimatedTime: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

// Question Types
export interface Question {
  id: string;
  type: 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'FILL_BLANK' | 'ESSAY';
  content: string;
  options?: string[];
  correctAnswer?: string;
  points: number;
  explanation?: string;
  orderNumber: number;
  timeLimit?: number;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  tags: string[];
  attachments?: Attachment[];
}

export interface Attachment {
  id: string;
  name: string;
  url: string;
  type: 'IMAGE' | 'AUDIO' | 'VIDEO' | 'DOCUMENT';
  size: number;
}

// Exam Attempt Types
export interface ExamAttempt {
  id: string;
  examId: string;
  studentId: string;
  startTime: string;
  endTime: string;
  submitTime?: string;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'TIMEOUT' | 'CANCELLED';
  score?: number;
  attemptNumber: number;
  timeSpent: number;
  isGraded: boolean;
  securityScore?: number;
  violations: SecurityViolation[];
  answers: Answer[];
}

export interface Answer {
  id: string;
  questionId: string;
  studentAnswer: string;
  isCorrect?: boolean;
  score?: number;
  submitTime: string;
  timeSpent: number;
  isMarkedForReview: boolean;
}

// Security Types
export interface SecurityViolation {
  id: string;
  type: 'WINDOW_SWITCH' | 'TAB_SWITCH' | 'COPY_PASTE' | 'RIGHT_CLICK' | 'FULLSCREEN_EXIT' | 'SUSPICIOUS_TIMING';
  description: string;
  timestamp: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  metadata?: Record<string, any>;
}

// Exam Session Types
export interface ExamSession {
  attemptId: string;
  examId: string;
  studentId: string;
  startTime: string;
  endTime: string;
  currentQuestionIndex: number;
  totalQuestions: number;
  remainingTime: number;
  isFullscreen: boolean;
  isProctoringEnabled: boolean;
  securityLevel: 'RELAXED' | 'NORMAL' | 'STRICT';
  allowedActions: string[];
  sessionToken: string;
}

// Progress and Results
export interface ExamProgress {
  attemptId: string;
  totalQuestions: number;
  answeredQuestions: number;
  markedForReview: number;
  currentQuestionIndex: number;
  timeSpent: number;
  remainingTime: number;
  progressPercentage: number;
}

export interface ExamResult {
  attemptId: string;
  examTitle: string;
  score: number;
  maxScore: number;
  passingScore: number;
  isPassed: boolean;
  grade: string;
  percentile?: number;
  startTime: string;
  submitTime: string;
  totalTime: number;
  correctAnswers: number;
  totalQuestions: number;
  questionResults: QuestionResult[];
  feedback?: string;
  certificate?: Certificate;
}

export interface QuestionResult {
  questionId: string;
  question: string;
  studentAnswer: string;
  correctAnswer: string;
  isCorrect: boolean;
  score: number;
  maxScore: number;
  explanation?: string;
  timeSpent: number;
}

export interface Certificate {
  id: string;
  examTitle: string;
  studentName: string;
  score: number;
  grade: string;
  issueDate: string;
  verificationCode: string;
  isValid: boolean;
}

// UI State Types
export interface ExamState {
  currentExam: Exam | null;
  currentSession: ExamSession | null;
  currentQuestion: Question | null;
  questions: Question[];
  answers: Map<string, string>;
  markedForReview: Set<string>;
  progress: ExamProgress | null;
  isLoading: boolean;
  error: string | null;
  timeRemaining: number;
  isSubmitting: boolean;
  violations: SecurityViolation[];
}

export interface UIState {
  theme: 'light' | 'dark';
  sidebarOpen: boolean;
  notifications: Notification[];
  modals: {
    confirmSubmit: boolean;
    timeWarning: boolean;
    securityAlert: boolean;
  };
  currentRoute: string;
  isFullscreen: boolean;
  connectionStatus: 'connected' | 'disconnected' | 'reconnecting';
}

export interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  timestamp: string;
  duration?: number;
  actions?: NotificationAction[];
}

export interface NotificationAction {
  label: string;
  action: () => void;
  variant?: 'default' | 'destructive';
}

// API Response Types
export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
  errors?: Record<string, string[]>;
  pagination?: {
    currentPage: number;
    totalPages: number;
    totalElements: number;
    pageSize: number;
    hasNext: boolean;
    hasPrevious: boolean;
  };
}

export interface PaginatedResponse<T> {
  content: T[];
  pagination: {
    currentPage: number;
    totalPages: number;
    totalElements: number;
    pageSize: number;
    hasNext: boolean;
    hasPrevious: boolean;
  };
}

// Form Types
export interface LoginForm {
  username: string;
  password: string;
  rememberMe: boolean;
}

export interface RegisterForm {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  fullName: string;
  acceptTerms: boolean;
}

export interface ExamFilters {
  status?: string[];
  difficulty?: string[];
  tags?: string[];
  search?: string;
  sortBy?: 'title' | 'startTime' | 'difficulty' | 'duration';
  sortOrder?: 'asc' | 'desc';
  page?: number;
  size?: number;
}

// WebSocket Types
export interface WebSocketMessage {
  type: 'heartbeat' | 'time_warning' | 'security_alert' | 'exam_ended' | 'connection_status';
  payload: any;
  timestamp: string;
}

export interface RealTimeEvent {
  eventType: 'STUDENT_ENTERED' | 'STUDENT_EXITED' | 'ANSWER_SUBMITTED' | 'VIOLATION_DETECTED';
  studentId: string;
  examId: string;
  attemptId: string;
  timestamp: string;
  data: any;
}

// Error Types
export interface AppError {
  code: string;
  message: string;
  details?: any;
  timestamp: string;
  requestId?: string;
}

// Component Props Types
export interface BaseComponentProps {
  className?: string;
  children?: React.ReactNode;
}

export interface ButtonProps extends BaseComponentProps {
  variant?: 'default' | 'destructive' | 'outline' | 'secondary' | 'ghost' | 'link';
  size?: 'default' | 'sm' | 'lg' | 'icon';
  disabled?: boolean;
  loading?: boolean;
  onClick?: () => void;
  type?: 'button' | 'submit' | 'reset';
}

export interface InputProps extends BaseComponentProps {
  type?: string;
  placeholder?: string;
  value?: string;
  onChange?: (value: string) => void;
  disabled?: boolean;
  error?: string;
  label?: string;
  required?: boolean;
}

// Utility Types
export type ExamStatus = Exam['status'];
export type QuestionType = Question['type'];
export type UserRole = User['role'];
export type SecurityLevel = SecurityViolation['severity'];
export type NotificationType = Notification['type'];

// Hook Return Types
export interface UseExamReturn {
  exam: Exam | null;
  session: ExamSession | null;
  questions: Question[];
  currentQuestion: Question | null;
  progress: ExamProgress | null;
  isLoading: boolean;
  error: string | null;
  startExam: () => Promise<void>;
  submitAnswer: (questionId: string, answer: string) => Promise<void>;
  markForReview: (questionId: string) => void;
  navigateToQuestion: (index: number) => void;
  submitExam: () => Promise<void>;
  nextQuestion: () => void;
  previousQuestion: () => void;
}

export interface UseAuthReturn {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginForm) => Promise<void>;
  register: (data: RegisterForm) => Promise<void>;
  logout: () => void;
  refreshToken: () => Promise<void>;
}

// Constants
export const QUESTION_TYPES = {
  SINGLE_CHOICE: 'SINGLE_CHOICE',
  MULTIPLE_CHOICE: 'MULTIPLE_CHOICE',
  TRUE_FALSE: 'TRUE_FALSE',
  FILL_BLANK: 'FILL_BLANK',
  ESSAY: 'ESSAY',
} as const;

export const EXAM_STATUS = {
  SCHEDULED: 'SCHEDULED',
  ACTIVE: 'ACTIVE',
  ENDED: 'ENDED',
  CANCELLED: 'CANCELLED',
} as const;

export const USER_ROLES = {
  STUDENT: 'STUDENT',
  TEACHER: 'TEACHER',
  ADMIN: 'ADMIN',
} as const;