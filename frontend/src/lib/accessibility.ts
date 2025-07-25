// Accessibility utilities for WCAG compliance

export const KEYBOARD_KEYS = {
  ENTER: 'Enter',
  SPACE: ' ',
  ESCAPE: 'Escape',
  TAB: 'Tab',
  ARROW_UP: 'ArrowUp',
  ARROW_DOWN: 'ArrowDown',
  ARROW_LEFT: 'ArrowLeft',
  ARROW_RIGHT: 'ArrowRight',
  HOME: 'Home',
  END: 'End',
} as const;

export const ARIA_LABELS = {
  // Common labels
  CLOSE: '关闭',
  MENU: '菜单',
  LOADING: '正在加载',
  SEARCH: '搜索',
  FILTER: '筛选',
  SORT: '排序',
  
  // Exam specific
  NEXT_QUESTION: '下一题',
  PREVIOUS_QUESTION: '上一题',
  MARK_FOR_REVIEW: '标记复查',
  SUBMIT_EXAM: '提交考试',
  START_EXAM: '开始考试',
  EXAM_TIMER: '考试计时器',
  QUESTION_NAVIGATION: '题目导航',
  ANSWER_OPTION: '答案选项',
  
  // Navigation
  MAIN_NAVIGATION: '主导航',
  BREADCRUMB: '导航路径',
  PAGINATION: '分页导航',
  
  // Notifications
  NOTIFICATIONS: '通知中心',
  UNREAD_NOTIFICATIONS: '未读通知',
  MARK_AS_READ: '标记为已读',
  
  // Forms
  REQUIRED_FIELD: '必填项',
  OPTIONAL_FIELD: '可选项',
  FIELD_ERROR: '输入错误',
  PASSWORD_VISIBILITY: '切换密码可见性',
} as const;

export const ARIA_DESCRIBEDBY_IDS = {
  PASSWORD_REQUIREMENTS: 'password-requirements',
  FORM_ERROR: 'form-error',
  FIELD_HELP: 'field-help',
  EXAM_INSTRUCTIONS: 'exam-instructions',
  TIMER_WARNING: 'timer-warning',
} as const;

// Focus management utilities
export const focusManager = {
  /**
   * Set focus to an element and announce it to screen readers
   */
  focusElement: (element: HTMLElement | null, announce = true) => {
    if (!element) return;
    
    element.focus({ preventScroll: false });
    
    if (announce) {
      // Add temporary aria-live region for announcements
      const announcement = document.createElement('div');
      announcement.setAttribute('aria-live', 'polite');
      announcement.setAttribute('aria-atomic', 'true');
      announcement.className = 'sr-only';
      announcement.textContent = `焦点已移动到 ${element.getAttribute('aria-label') || element.textContent || '元素'}`;
      
      document.body.appendChild(announcement);
      setTimeout(() => document.body.removeChild(announcement), 1000);
    }
  },

  /**
   * Get all focusable elements within a container
   */
  getFocusableElements: (container: HTMLElement): HTMLElement[] => {
    const focusableSelectors = [
      'button:not([disabled])',
      'input:not([disabled])',
      'select:not([disabled])',
      'textarea:not([disabled])',
      'a[href]',
      '[tabindex]:not([tabindex="-1"])',
      '[contenteditable="true"]',
    ].join(', ');
    
    return Array.from(container.querySelectorAll(focusableSelectors))
      .filter(el => {
        const element = el as HTMLElement;
        return element.offsetParent !== null && // Not hidden
               getComputedStyle(element).visibility !== 'hidden';
      }) as HTMLElement[];
  },

  /**
   * Trap focus within a container (for modals, dropdowns)
   */
  trapFocus: (container: HTMLElement) => {
    const focusableElements = focusManager.getFocusableElements(container);
    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === KEYBOARD_KEYS.TAB) {
        if (e.shiftKey) {
          // Shift + Tab
          if (document.activeElement === firstElement) {
            e.preventDefault();
            lastElement?.focus();
          }
        } else {
          // Tab
          if (document.activeElement === lastElement) {
            e.preventDefault();
            firstElement?.focus();
          }
        }
      }
    };

    container.addEventListener('keydown', handleKeyDown);
    
    // Focus first element
    firstElement?.focus();

    // Return cleanup function
    return () => {
      container.removeEventListener('keydown', handleKeyDown);
    };
  },
};

// Screen reader utilities
export const screenReader = {
  /**
   * Announce a message to screen readers
   */
  announce: (message: string, priority: 'polite' | 'assertive' = 'polite') => {
    const announcement = document.createElement('div');
    announcement.setAttribute('aria-live', priority);
    announcement.setAttribute('aria-atomic', 'true');
    announcement.className = 'sr-only';
    announcement.textContent = message;
    
    document.body.appendChild(announcement);
    setTimeout(() => document.body.removeChild(announcement), 1000);
  },

  /**
   * Create a live region for dynamic content updates
   */
  createLiveRegion: (id: string, priority: 'polite' | 'assertive' = 'polite') => {
    let region = document.getElementById(id);
    if (!region) {
      region = document.createElement('div');
      region.id = id;
      region.setAttribute('aria-live', priority);
      region.setAttribute('aria-atomic', 'true');
      region.className = 'sr-only';
      document.body.appendChild(region);
    }
    return region;
  },

  /**
   * Update live region content
   */
  updateLiveRegion: (id: string, message: string) => {
    const region = document.getElementById(id);
    if (region) {
      region.textContent = message;
    }
  },
};

// Color contrast utilities
export const colorContrast = {
  /**
   * Calculate relative luminance of a color
   */
  getLuminance: (color: string): number => {
    // Convert color to RGB values
    const rgb = colorContrast.hexToRgb(color);
    if (!rgb) return 0;

    // Convert to relative luminance
    const [r, g, b] = [rgb.r, rgb.g, rgb.b].map(c => {
      c = c / 255;
      return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    });

    return 0.2126 * r + 0.7152 * g + 0.0722 * b;
  },

  /**
   * Convert hex color to RGB
   */
  hexToRgb: (hex: string): { r: number; g: number; b: number } | null => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
      r: parseInt(result[1], 16),
      g: parseInt(result[2], 16),
      b: parseInt(result[3], 16),
    } : null;
  },

  /**
   * Calculate contrast ratio between two colors
   */
  getContrastRatio: (color1: string, color2: string): number => {
    const lum1 = colorContrast.getLuminance(color1);
    const lum2 = colorContrast.getLuminance(color2);
    const brightest = Math.max(lum1, lum2);
    const darkest = Math.min(lum1, lum2);
    return (brightest + 0.05) / (darkest + 0.05);
  },

  /**
   * Check if colors meet WCAG contrast requirements
   */
  meetsWCAG: (foreground: string, background: string, level: 'AA' | 'AAA' = 'AA', size: 'normal' | 'large' = 'normal'): boolean => {
    const ratio = colorContrast.getContrastRatio(foreground, background);
    
    if (level === 'AAA') {
      return size === 'large' ? ratio >= 4.5 : ratio >= 7;
    } else {
      return size === 'large' ? ratio >= 3 : ratio >= 4.5;
    }
  },
};

// Keyboard navigation utilities
export const keyboardNavigation = {
  /**
   * Handle arrow key navigation in a list
   */
  handleArrowNavigation: (
    event: KeyboardEvent,
    items: HTMLElement[],
    currentIndex: number,
    onChange: (newIndex: number) => void,
    options: {
      wrap?: boolean;
      horizontal?: boolean;
    } = {}
  ) => {
    const { wrap = true, horizontal = false } = options;
    const { key } = event;
    
    let newIndex = currentIndex;
    
    if (horizontal) {
      if (key === KEYBOARD_KEYS.ARROW_LEFT) {
        newIndex = currentIndex > 0 ? currentIndex - 1 : (wrap ? items.length - 1 : 0);
      } else if (key === KEYBOARD_KEYS.ARROW_RIGHT) {
        newIndex = currentIndex < items.length - 1 ? currentIndex + 1 : (wrap ? 0 : items.length - 1);
      }
    } else {
      if (key === KEYBOARD_KEYS.ARROW_UP) {
        newIndex = currentIndex > 0 ? currentIndex - 1 : (wrap ? items.length - 1 : 0);
      } else if (key === KEYBOARD_KEYS.ARROW_DOWN) {
        newIndex = currentIndex < items.length - 1 ? currentIndex + 1 : (wrap ? 0 : items.length - 1);
      }
    }
    
    if (key === KEYBOARD_KEYS.HOME) {
      newIndex = 0;
    } else if (key === KEYBOARD_KEYS.END) {
      newIndex = items.length - 1;
    }
    
    if (newIndex !== currentIndex) {
      event.preventDefault();
      onChange(newIndex);
      items[newIndex]?.focus();
    }
  },
};

// Form accessibility utilities
export const formAccessibility = {
  /**
   * Generate unique ID for form fields
   */
  generateFieldId: (name: string, suffix?: string): string => {
    const id = `field-${name}`;
    return suffix ? `${id}-${suffix}` : id;
  },

  /**
   * Create accessible field description
   */
  createFieldDescription: (fieldId: string, description: string): string => {
    return `${fieldId}-description`;
  },

  /**
   * Create accessible error message
   */
  createErrorMessage: (fieldId: string, error: string): string => {
    return `${fieldId}-error`;
  },

  /**
   * Get ARIA attributes for form fields
   */
  getFieldAriaAttributes: (field: {
    id: string;
    required?: boolean;
    invalid?: boolean;
    description?: string;
    error?: string;
  }) => {
    const attributes: Record<string, string | boolean> = {};
    
    if (field.required) {
      attributes['aria-required'] = true;
    }
    
    if (field.invalid) {
      attributes['aria-invalid'] = true;
    }
    
    const describedBy = [];
    if (field.description) {
      describedBy.push(`${field.id}-description`);
    }
    if (field.error) {
      describedBy.push(`${field.id}-error`);
    }
    
    if (describedBy.length > 0) {
      attributes['aria-describedby'] = describedBy.join(' ');
    }
    
    return attributes;
  },
};

// Skip link utility
export const skipLinks = {
  /**
   * Create skip to main content link
   */
  createSkipLink: (): HTMLElement => {
    const skipLink = document.createElement('a');
    skipLink.href = '#main-content';
    skipLink.textContent = '跳转到主要内容';
    skipLink.className = 'sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:bg-white focus:text-black focus:px-4 focus:py-2 focus:rounded focus:shadow-lg';
    
    return skipLink;
  },

  /**
   * Add skip links to page
   */
  addSkipLinks: () => {
    const skipLink = skipLinks.createSkipLink();
    document.body.insertBefore(skipLink, document.body.firstChild);
  },
};