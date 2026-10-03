import { DarkTheme, DefaultTheme, Stack, ThemeProvider, useRouter } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import * as Notifications from 'expo-notifications';
import { useEffect } from 'react';
import { Platform } from 'react-native';

import { AnimatedSplashOverlay } from '@/components/animated-icon';
import { AuthProvider, useAuth } from '@/hooks/use-auth';
import { TaskProvider, useTaskStore } from '@/hooks/use-task-store';
import { scheduleTaskSnooze, syncTaskNotifications } from '@/lib/task-notifications';

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  return <AuthProvider><TaskProvider><AppNavigation /></TaskProvider></AuthProvider>;
}

function AppNavigation() {
  const { isLoading, user } = useAuth();
  const { themeMode, tasks, preferences, toggleTask } = useTaskStore();
  const router = useRouter();
  useEffect(() => { void syncTaskNotifications(tasks, preferences.remindersEnabled, preferences.quietHoursEnabled); }, [tasks, preferences.remindersEnabled, preferences.quietHoursEnabled]);
  useEffect(() => {
    if (Platform.OS === 'web') return;
    const subscription = Notifications.addNotificationResponseReceivedListener((response) => {
      const data = response.notification.request.content.data as { type?: string; taskId?: string; dateKey?: string };
      if (data.type !== 'task-reminder' || !data.taskId || !data.dateKey) return;
      const task = tasks.find((item) => item.id === data.taskId);
      if (!task) return;
      if (response.actionIdentifier === 'done') toggleTask(task.id, data.dateKey);
      else if (response.actionIdentifier === 'snooze') void scheduleTaskSnooze(task, data.dateKey);
      else if (response.actionIdentifier === 'open' || response.actionIdentifier === Notifications.DEFAULT_ACTION_IDENTIFIER) router.push(`/task-detail?id=${task.id}&date=${data.dateKey}`);
    });
    return () => subscription.remove();
  }, [router, tasks, toggleTask]);
  if (isLoading) return <AnimatedSplashOverlay />;
  return (
    <ThemeProvider value={themeMode === 'dark' ? DarkTheme : DefaultTheme}>
      <AnimatedSplashOverlay />
      <Stack initialRouteName={user ? '(tabs)' : 'sign-in'} screenOptions={{ headerShown: true, headerTintColor: '#7C4DFF', headerTitleStyle: { fontWeight: '800' } }}>
        <Stack.Protected guard={Boolean(user)}>
          <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
          <Stack.Screen name="create-task" options={{ headerShown: false }} />
          <Stack.Screen name="task-detail" options={{ headerShown: false, presentation: 'formSheet', sheetGrabberVisible: true, sheetAllowedDetents: [0.82, 1] }} />
          <Stack.Screen name="profile" options={{ headerShown: false }} />
        </Stack.Protected>
        <Stack.Protected guard={!user}>
          <Stack.Screen name="sign-in" options={{ headerShown: false }} />
          <Stack.Screen name="sign-up" options={{ headerShown: false }} />
        </Stack.Protected>
      </Stack>
    </ThemeProvider>
  );
}
