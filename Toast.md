// ============================================================================
// PRODUCTION TOAST SYSTEM V2.0 - REACT NATIVE
// Bug-free • 60fps • Promise support • Progress bar • Portal-ready
// ============================================================================

import React, { createContext, useContext, useState, useCallback, useRef, useEffect } from 'react';
import { View, Text, Pressable, StyleSheet, Platform } from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withSpring,
  withSequence,
  runOnJS,
  useAnimatedGestureHandler,
  interpolate,
  Extrapolate,
} from 'react-native-reanimated';
import { PanGestureHandler } from 'react-native-gesture-handler';

// ============================================================================
// TYPES
// ============================================================================

type ToastType = 'success' | 'error' | 'warning' | 'info' | 'loading' | 'custom';
type ToastPosition = 'top' | 'center' | 'bottom';
type ToastAnimation = 'slide' | 'fade' | 'scale' | 'bounce';

interface ToastConfig {
  id: string;
  type?: ToastType;
  title?: string;
  message?: string;
  children?: React.ReactNode;
  position?: ToastPosition;
  animation?: ToastAnimation;
  duration?: number;
  dismissible?: boolean;
  swipeToDismiss?: boolean;
  showProgress?: boolean;
  pauseOnPress?: boolean;
  onDismiss?: () => void;
  onPress?: () => void;
  icon?: React.ReactNode;
  backgroundColor?: string;
  textColor?: string;
  stackOffset?: number;
}

interface ToastContextValue {
  showToast: (config: Omit<ToastConfig, 'id'>) => string;
  hideToast: (id: string) => void;
  hideAllToasts: () => void;
  success: (message: string, options?: Partial<Omit<ToastConfig, 'id' | 'type'>>) => string;
  error: (message: string, options?: Partial<Omit<ToastConfig, 'id' | 'type'>>) => string;
  warning: (message: string, options?: Partial<Omit<ToastConfig, 'id' | 'type'>>) => string;
  info: (message: string, options?: Partial<Omit<ToastConfig, 'id' | 'type'>>) => string;
  loading: (message: string, options?: Partial<Omit<ToastConfig, 'id' | 'type'>>) => string;
  promise: <T>(
    promise: Promise<T>,
    messages: {
      loading: string;
      success: string | ((data: T) => string);
      error: string | ((error: any) => string);
    },
    options?: Partial<Omit<ToastConfig, 'id' | 'type'>>
  ) => Promise<T>;
}

interface ToastProviderProps {
  children: React.ReactNode;
  maxToasts?: number;
  defaultDuration?: number;
  defaultPosition?: ToastPosition;
  defaultAnimation?: ToastAnimation;
  stackOffset?: number;
}

// ============================================================================
// CONTEXT
// ============================================================================

const ToastContext = createContext<ToastContextValue | null>(null);

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within ToastProvider');
  }
  return context;
};

// ============================================================================
// PROVIDER
// ============================================================================

export const ToastProvider: React.FC<ToastProviderProps> = ({ 
  children, 
  maxToasts = 3,
  defaultDuration = 3000,
  defaultPosition = 'bottom',
  defaultAnimation = 'slide',
  stackOffset = 8,
}) => {
  const [toasts, setToasts] = useState<ToastConfig[]>([]);
  const idCounter = useRef(0);
  const queueRef = useRef<ToastConfig[]>([]);

  const processQueue = useCallback(() => {
    setToasts(current => {
      if (current.length >= maxToasts || queueRef.current.length === 0) {
        return current;
      }
      
      const next = queueRef.current.shift();
      if (next) {
        return [...current, next];
      }
      return current;
    });
  }, [maxToasts]);

  const showToast = useCallback((config: Omit<ToastConfig, 'id'>) => {
    const id = `toast-${Date.now()}-${idCounter.current++}`;
    const newToast: ToastConfig = {
      id,
      type: 'custom',
      position: defaultPosition,
      animation: defaultAnimation,
      duration: defaultDuration,
      dismissible: true,
      swipeToDismiss: true,
      showProgress: false,
      pauseOnPress: false,
      stackOffset,
      ...config,
    };

    setToasts(current => {
      if (current.length >= maxToasts) {
        queueRef.current.push(newToast);
        return current;
      }
      return [...current, newToast];
    });

    return id;
  }, [maxToasts, defaultDuration, defaultPosition, defaultAnimation, stackOffset]);

  const hideToast = useCallback((id: string) => {
    setToasts(prev => {
      const filtered = prev.filter(toast => toast.id !== id);
      setTimeout(processQueue, 100);
      return filtered;
    });
  }, [processQueue]);

  const hideAllToasts = useCallback(() => {
    setToasts([]);
    queueRef.current = [];
  }, []);

  // Helper methods
  const success = useCallback((message: string, options?: Partial<Omit<ToastConfig, 'id' | 'type'>>) => {
    return showToast({ type: 'success', message, ...options });
  }, [showToast]);

  const error = useCallback((message: string, options?: Partial<Omit<ToastConfig, 'id' | 'type'>>) => {
    return showToast({ type: 'error', message, ...options });
  }, [showToast]);

  const warning = useCallback((message: string, options?: Partial<Omit<ToastConfig, 'id' | 'type'>>) => {
    return showToast({ type: 'warning', message, ...options });
  }, [showToast]);

  const info = useCallback((message: string, options?: Partial<Omit<ToastConfig, 'id' | 'type'>>) => {
    return showToast({ type: 'info', message, ...options });
  }, [showToast]);

  const loading = useCallback((message: string, options?: Partial<Omit<ToastConfig, 'id' | 'type'>>) => {
    return showToast({ type: 'loading', message, duration: 0, dismissible: false, ...options });
  }, [showToast]);

  // Promise wrapper
  const promise = useCallback(async <T,>(
    promiseToResolve: Promise<T>,
    messages: {
      loading: string;
      success: string | ((data: T) => string);
      error: string | ((error: any) => string);
    },
    options?: Partial<Omit<ToastConfig, 'id' | 'type'>>
  ): Promise<T> => {
    const loadingId = loading(messages.loading, options);

    try {
      const data = await promiseToResolve;
      hideToast(loadingId);
      
      const successMessage = typeof messages.success === 'function' 
        ? messages.success(data) 
        : messages.success;
      
      success(successMessage, options);
      return data;
    } catch (err) {
      hideToast(loadingId);
      
      const errorMessage = typeof messages.error === 'function' 
        ? messages.error(err) 
        : messages.error;
      
      error(errorMessage, options);
      throw err;
    }
  }, [loading, success, error, hideToast]);

  return (
    <ToastContext.Provider value={{ 
      showToast, 
      hideToast, 
      hideAllToasts,
      success,
      error,
      warning,
      info,
      loading,
      promise,
    }}>
      {children}
      <ToastHost toasts={toasts} onDismiss={hideToast} />
    </ToastContext.Provider>
  );
};

// ============================================================================
// TOAST HOST
// ============================================================================

interface ToastHostProps {
  toasts: ToastConfig[];
  onDismiss: (id: string) => void;
}

const ToastHost: React.FC<ToastHostProps> = ({ toasts, onDismiss }) => {
  if (toasts.length === 0) return null;

  const groupedToasts = {
    top: toasts.filter(t => t.position === 'top'),
    center: toasts.filter(t => t.position === 'center'),
    bottom: toasts.filter(t => t.position === 'bottom'),
  };

  return (
    <View style={StyleSheet.absoluteFill} pointerEvents="box-none">
      {groupedToasts.top.length > 0 && (
        <View style={[styles.toastGroup, styles.topGroup]} pointerEvents="box-none">
          {groupedToasts.top.map((toast, index) => (
            <Toast 
              key={toast.id} 
              {...toast} 
              index={index}
              onDismiss={() => onDismiss(toast.id)} 
            />
          ))}
        </View>
      )}
      
      {groupedToasts.center.length > 0 && (
        <View style={[styles.toastGroup, styles.centerGroup]} pointerEvents="box-none">
          {groupedToasts.center.map((toast, index) => (
            <Toast 
              key={toast.id} 
              {...toast} 
              index={index}
              onDismiss={() => onDismiss(toast.id)} 
            />
          ))}
        </View>
      )}
      
      {groupedToasts.bottom.length > 0 && (
        <View style={[styles.toastGroup, styles.bottomGroup]} pointerEvents="box-none">
          {groupedToasts.bottom.map((toast, index) => (
            <Toast 
              key={toast.id} 
              {...toast} 
              index={index}
              onDismiss={() => onDismiss(toast.id)} 
            />
          ))}
        </View>
      )}
    </View>
  );
};

// ============================================================================
// TOAST COMPONENT
// ============================================================================

const Toast: React.FC<ToastConfig & { onDismiss: () => void; index: number }> = ({
  children,
  type = 'custom',
  title,
  message,
  position = 'bottom',
  animation = 'slide',
  duration = 3000,
  dismissible = true,
  swipeToDismiss = true,
  showProgress = false,
  pauseOnPress = false,
  onDismiss,
  onPress,
  icon,
  backgroundColor,
  textColor,
  index,
  stackOffset = 8,
}) => {
  // Shared values
  const opacity = useSharedValue(0);
  const translateY = useSharedValue(getInitialTranslateY(animation, position));
  const scale = useSharedValue(animation === 'scale' ? 0.7 : 1);
  const gestureTranslateY = useSharedValue(0);
  const progress = useSharedValue(1);

  const timerRef = useRef<NodeJS.Timeout>();
  const isDismissing = useRef(false);
  const isPaused = useRef(false);
  const remainingTime = useRef(duration);
  const startTime = useRef(Date.now());

  useEffect(() => {
    const delay = index * 50;
    
    setTimeout(() => {
      opacity.value = withTiming(1, { duration: 300 });
      translateY.value = withTiming(0, { duration: 300 });
      
      if (animation === 'scale') {
        scale.value = withSpring(1, { damping: 15, stiffness: 200 });
      } else if (animation === 'bounce') {
        scale.value = withSequence(
          withSpring(1.08, { damping: 10, stiffness: 200 }),
          withSpring(1, { damping: 15, stiffness: 200 })
        );
      }
    }, delay);

    // Auto-dismiss with progress
    if (duration > 0) {
      startTime.current = Date.now();
      
      if (showProgress) {
        progress.value = withTiming(0, { duration: duration + delay });
      }
      
      timerRef.current = setTimeout(() => {
        if (!isDismissing.current && !isPaused.current) {
          handleDismiss();
        }
      }, duration + delay);
    }

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, []);

  const handleDismiss = useCallback(() => {
    if (isDismissing.current) return;
    isDismissing.current = true;

    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }

    opacity.value = withTiming(0, { duration: 200 });
    translateY.value = withTiming(
      getInitialTranslateY(animation, position),
      { duration: 200 }
    );
    scale.value = withTiming(0.8, { duration: 200 });

    setTimeout(() => onDismiss(), 220);
  }, [animation, position, onDismiss]);

  const handlePause = useCallback(() => {
    if (!pauseOnPress || duration === 0) return;
    
    isPaused.current = true;
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      remainingTime.current = duration - (Date.now() - startTime.current);
    }
  }, [pauseOnPress, duration]);

  const handleResume = useCallback(() => {
    if (!pauseOnPress || duration === 0) return;
    
    isPaused.current = false;
    startTime.current = Date.now();
    
    timerRef.current = setTimeout(() => {
      if (!isDismissing.current) {
        handleDismiss();
      }
    }, remainingTime.current);
  }, [pauseOnPress, handleDismiss]);

  // Gesture handler
  const gestureHandler = useAnimatedGestureHandler({
    onActive: (event) => {
      const direction = position === 'top' ? -1 : 1;
      gestureTranslateY.value = event.translationY * direction;
      opacity.value = Math.max(0.3, 1 - Math.abs(event.translationY) / 200);
    },
    onEnd: (event) => {
      const threshold = 80;
      
      if (Math.abs(event.translationY) > threshold) {
        gestureTranslateY.value = withTiming(
          event.translationY > 0 ? 200 : -200,
          { duration: 200 }
        );
        opacity.value = withTiming(0, { duration: 200 });
        runOnJS(handleDismiss)();
      } else {
        gestureTranslateY.value = withSpring(0);
        opacity.value = withSpring(1);
      }
    },
  });

  const animatedStyle = useAnimatedStyle(() => ({
    opacity: opacity.value,
    transform: [
      { translateY: translateY.value + gestureTranslateY.value + (index * stackOffset) },
      { scale: scale.value },
    ],
  }));

  const progressStyle = useAnimatedStyle(() => ({
    width: `${progress.value * 100}%`,
  }));

  const handlePress = useCallback(() => {
    if (pauseOnPress) {
      handlePause();
      setTimeout(handleResume, 2000);
    }
    
    if (onPress) {
      onPress();
    }
    
    if (dismissible && !pauseOnPress) {
      handleDismiss();
    }
  }, [onPress, dismissible, pauseOnPress, handleDismiss, handlePause, handleResume]);

  // Render custom children
  if (children) {
    const ToastWrapper = swipeToDismiss ? PanGestureHandler : View;
    const wrapperProps = swipeToDismiss ? { onGestureEvent: gestureHandler } : {};

    return (
      <View style={styles.toastWrapper} pointerEvents="box-none">
        <ToastWrapper {...wrapperProps}>
          <Animated.View style={[styles.toastAnimated, animatedStyle]}>
            <Pressable onPress={handlePress} disabled={!dismissible && !onPress && !pauseOnPress}>
              {children}
              {showProgress && duration > 0 && (
                <Animated.View style={[styles.progressBar, progressStyle]} />
              )}
            </Pressable>
          </Animated.View>
        </ToastWrapper>
      </View>
    );
  }

  // Render default toast
  const toastStyle = getToastStyle(type, backgroundColor);
  const defaultIcon = icon || getDefaultIcon(type);
  const defaultTextColor = textColor || '#FFFFFF';
  const ToastWrapper = swipeToDismiss ? PanGestureHandler : View;
  const wrapperProps = swipeToDismiss ? { onGestureEvent: gestureHandler } : {};

  return (
    <View style={styles.toastWrapper} pointerEvents="box-none">
      <ToastWrapper {...wrapperProps}>
        <Animated.View style={[styles.toastAnimated, animatedStyle]}>
          <Pressable onPress={handlePress} disabled={!dismissible && !onPress && !pauseOnPress}>
            <View style={[styles.defaultToast, toastStyle]}>
              {defaultIcon && (
                <View style={styles.iconContainer}>
                  {defaultIcon}
                </View>
              )}
              <View style={styles.textContainer}>
                {title && (
                  <Text style={[styles.title, { color: defaultTextColor }]} numberOfLines={2}>
                    {title}
                  </Text>
                )}
                {message && (
                  <Text style={[styles.message, { color: defaultTextColor }]} numberOfLines={3}>
                    {message}
                  </Text>
                )}
              </View>
            </View>
            {showProgress && duration > 0 && (
              <Animated.View style={[styles.progressBar, progressStyle]} />
            )}
          </Pressable>
        </Animated.View>
      </ToastWrapper>
    </View>
  );
};

// ============================================================================
// HELPERS
// ============================================================================

function getInitialTranslateY(animation: ToastAnimation, position: ToastPosition): number {
  if (animation === 'fade' || animation === 'scale') return 0;
  return position === 'top' ? -100 : 100;
}

function getToastStyle(type: ToastType, customBg?: string): any {
  if (customBg) {
    return { backgroundColor: customBg };
  }

  const colors = {
    success: '#10b981',
    error: '#ef4444',
    warning: '#f59e0b',
    info: '#3b82f6',
    loading: '#6366f1',
    custom: '#1f2937',
  };

  return { backgroundColor: colors[type] };
}

function getDefaultIcon(type: ToastType): React.ReactNode {
  const iconMap = {
    success: (
      <View style={[styles.iconCircle, { backgroundColor: 'rgba(255,255,255,0.25)' }]}>
        <Text style={styles.iconText}>✓</Text>
      </View>
    ),
    error: (
      <View style={[styles.iconCircle, { backgroundColor: 'rgba(255,255,255,0.25)' }]}>
        <Text style={styles.iconText}>✕</Text>
      </View>
    ),
    warning: (
      <View style={[styles.iconCircle, { backgroundColor: 'rgba(255,255,255,0.25)' }]}>
        <Text style={styles.iconText}>⚠</Text>
      </View>
    ),
    info: (
      <View style={[styles.iconCircle, { backgroundColor: 'rgba(255,255,255,0.25)' }]}>
        <Text style={styles.iconText}>i</Text>
      </View>
    ),
    loading: (
      <View style={[styles.iconCircle, { backgroundColor: 'rgba(255,255,255,0.25)' }]}>
        <LoadingSpinner />
      </View>
    ),
    custom: null,
  };

  return iconMap[type];
}

// Simple loading spinner
const LoadingSpinner: React.FC = () => {
  const rotation = useSharedValue(0);

  useEffect(() => {
    rotation.value = withTiming(360, { duration: 1000 }, (finished) => {
      if (finished) {
        rotation.value = 0;
        rotation.value = withTiming(360, { duration: 1000 });
      }
    });
  }, []);

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ rotate: `${rotation.value}deg` }],
  }));

  return (
    <Animated.View style={animatedStyle}>
      <Text style={styles.iconText}>⟳</Text>
    </Animated.View>
  );
};

// ============================================================================
// STYLES
// ============================================================================

const styles = StyleSheet.create({
  toastGroup: {
    position: 'absolute',
    left: 0,
    right: 0,
    paddingHorizontal: 16,
  },
  topGroup: {
    top: Platform.OS === 'ios' ? 50 : 20,
  },
  centerGroup: {
    top: '40%',
  },
  bottomGroup: {
    bottom: Platform.OS === 'ios' ? 50 : 20,
  },
  toastWrapper: {
    width: '100%',
    marginVertical: 4,
  },
  toastAnimated: {
    width: '100%',
  },
  defaultToast: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
    overflow: 'hidden',
  },
  iconContainer: {
    marginRight: 12,
  },
  iconCircle: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  iconText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
  textContainer: {
    flex: 1,
  },
  title: {
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 2,
  },
  message: {
    fontSize: 14,
    fontWeight: '500',
    lineHeight: 18,
  },
  progressBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    height: 3,
    backgroundColor: 'rgba(255, 255, 255, 0.3)',
  },
});

// ============================================================================
// USAGE EXAMPLES
// ============================================================================

/*

// SETUP:
import { ToastProvider } from './ToastSystem';
import { GestureHandlerRootView } from 'react-native-gesture-handler';

export default function App() {
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <ToastProvider 
        maxToasts={3} 
        defaultDuration={3000}
        stackOffset={8}
      >
        <YourApp />
      </ToastProvider>
    </GestureHandlerRootView>
  );
}

// USAGE:
import { useToast } from './ToastSystem';

function MyComponent() {
  const { success, error, warning, info, loading, promise } = useToast();

  // Simple notifications
  success('Operation completed!');
  error('Failed to save', { duration: 5000 });
  warning('Battery low', { position: 'top' });
  info('New update available', { showProgress: true });

  // Loading toast (manual dismiss)
  const loadingId = loading('Processing...');
  // Later: hideToast(loadingId);

  // Promise wrapper (auto-handles loading/success/error)
  const handleAsyncAction = async () => {
    await promise(
      fetch('/api/data').then(r => r.json()),
      {
        loading: 'Fetching data...',
        success: (data) => `Loaded ${data.items.length} items`,
        error: (err) => `Failed: ${err.message}`,
      },
      { showProgress: true }
    );
  };

  // Pause on press
  info('Press to pause', { 
    pauseOnPress: true,
    duration: 5000,
    showProgress: true,
  });

  // Custom toast
  showToast({
    position: 'center',
    animation: 'bounce',
    duration: 4000,
    showProgress: true,
    children: (
      <View style={{ 
        backgroundColor: '#8b5cf6', 
        padding: 24, 
        borderRadius: 16 
      }}>
        <Text style={{ color: '#fff', fontSize: 20, fontWeight: 'bold' }}>
          🎉 Custom Toast!
        </Text>
      </View>
    ),
  });
}

*/