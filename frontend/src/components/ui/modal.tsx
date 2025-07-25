import React, { useEffect, useRef, ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import { Button } from './button';
import { cn } from '@/lib/utils';
import { focusManager, screenReader, KEYBOARD_KEYS, ARIA_LABELS } from '@/lib/accessibility';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full';
  closeOnOverlayClick?: boolean;
  closeOnEscape?: boolean;
  showCloseButton?: boolean;
  className?: string;
  overlayClassName?: string;
  initialFocus?: React.RefObject<HTMLElement>;
  ariaDescribedBy?: string;
}

export const Modal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  title,
  children,
  size = 'md',
  closeOnOverlayClick = true,
  closeOnEscape = true,
  showCloseButton = true,
  className,
  overlayClassName,
  initialFocus,
  ariaDescribedBy,
}) => {
  const modalRef = useRef<HTMLDivElement>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);
  const trapFocusCleanup = useRef<(() => void) | null>(null);

  // Size classes
  const sizeClasses = {
    sm: 'max-w-sm',
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-xl',
    full: 'max-w-full mx-4',
  };

  // Handle escape key
  useEffect(() => {
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === KEYBOARD_KEYS.ESCAPE && closeOnEscape) {
        onClose();
      }
    };

    if (isOpen) {
      document.addEventListener('keydown', handleEscape);
      return () => document.removeEventListener('keydown', handleEscape);
    }
  }, [isOpen, closeOnEscape, onClose]);

  // Handle focus management
  useEffect(() => {
    if (isOpen) {
      // Store previous focus
      previousFocusRef.current = document.activeElement as HTMLElement;
      
      // Prevent background scrolling
      document.body.style.overflow = 'hidden';
      
      // Set up focus trap
      setTimeout(() => {
        const modal = modalRef.current;
        if (modal) {
          trapFocusCleanup.current = focusManager.trapFocus(modal);
          
          // Focus initial element or first focusable element
          if (initialFocus?.current) {
            focusManager.focusElement(initialFocus.current);
          } else {
            const focusableElements = focusManager.getFocusableElements(modal);
            if (focusableElements.length > 0) {
              focusManager.focusElement(focusableElements[0]);
            }
          }
        }
      }, 100);
      
      // Announce modal opening
      screenReader.announce(`对话框已打开：${title}`);
    }

    return () => {
      // Cleanup
      if (trapFocusCleanup.current) {
        trapFocusCleanup.current();
        trapFocusCleanup.current = null;
      }
      
      // Restore focus and scrolling
      if (previousFocusRef.current) {
        focusManager.focusElement(previousFocusRef.current, false);
      }
      document.body.style.overflow = '';
      
      // Announce modal closing
      if (isOpen) {
        screenReader.announce('对话框已关闭');
      }
    };
  }, [isOpen, title, initialFocus]);

  // Handle overlay click
  const handleOverlayClick = (event: React.MouseEvent) => {
    if (event.target === event.currentTarget && closeOnOverlayClick) {
      onClose();
    }
  };

  if (!isOpen) return null;

  const modalContent = (
    <div
      className={cn(
        'fixed inset-0 z-50 flex items-center justify-center',
        overlayClassName
      )}
      onClick={handleOverlayClick}
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
      aria-describedby={ariaDescribedBy}
    >
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-black bg-opacity-50 transition-opacity"
        aria-hidden="true"
      />
      
      {/* Modal */}
      <div
        ref={modalRef}
        className={cn(
          'relative bg-white rounded-lg shadow-xl w-full transform transition-all',
          'max-h-[90vh] overflow-hidden flex flex-col',
          sizeClasses[size],
          className
        )}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-4 sm:p-6 border-b border-gray-200">
          <h2 
            id="modal-title"
            className="text-lg font-semibold text-gray-900"
          >
            {title}
          </h2>
          
          {showCloseButton && (
            <Button
              variant="ghost"
              size="sm"
              onClick={onClose}
              className="touch-target -mr-2"
              aria-label={ARIA_LABELS.CLOSE}
            >
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>
        
        {/* Content */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-6">
          {children}
        </div>
      </div>
    </div>
  );

  // Render modal in portal
  return createPortal(modalContent, document.body);
};

// Confirmation Modal with accessible buttons
interface ConfirmationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  variant?: 'default' | 'destructive';
  isLoading?: boolean;
}

export const ConfirmationModal: React.FC<ConfirmationModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmText = '确认',
  cancelText = '取消',
  variant = 'default',
  isLoading = false,
}) => {
  const confirmButtonRef = useRef<HTMLButtonElement>(null);

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={title}
      size="sm"
      initialFocus={confirmButtonRef}
      ariaDescribedBy="confirmation-message"
    >
      <div className="space-y-4">
        <p id="confirmation-message" className="text-gray-600">
          {message}
        </p>
        
        <div className="flex gap-3 justify-end">
          <Button
            variant="outline"
            onClick={onClose}
            disabled={isLoading}
            className="btn-mobile"
          >
            {cancelText}
          </Button>
          
          <Button
            ref={confirmButtonRef}
            variant={variant === 'destructive' ? 'destructive' : 'default'}
            onClick={onConfirm}
            disabled={isLoading}
            className="btn-mobile"
            aria-describedby="confirmation-message"
          >
            {isLoading ? '处理中...' : confirmText}
          </Button>
        </div>
      </div>
    </Modal>
  );
};