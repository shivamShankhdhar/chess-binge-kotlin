import { DarkTheme, DefaultTheme, Stack, ThemeProvider } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';

import { AnimatedSplashOverlay } from '@/components/animated-icon';
import { TaskProvider, useTaskStore } from '@/hooks/use-task-store';

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  return <TaskProvider><AppNavigation /></TaskProvider>;
}

function AppNavigation() {
  const { themeMode } = useTaskStore();
  return (
    <ThemeProvider value={themeMode === 'dark' ? DarkTheme : DefaultTheme}>
      <AnimatedSplashOverlay />
      <Stack screenOptions={{ headerShown: true, headerTintColor: '#7C4DFF', headerTitleStyle: { fontWeight: '800' } }}>
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen name="create-task" options={{ headerShown: false }} />
        <Stack.Screen name="tasks" options={{ headerShown: false }} />
        <Stack.Screen name="task-detail" options={{ headerShown: false, presentation: 'formSheet', sheetGrabberVisible: true, sheetAllowedDetents: [0.82, 1] }} />
      </Stack>
    </ThemeProvider>
  );
}
