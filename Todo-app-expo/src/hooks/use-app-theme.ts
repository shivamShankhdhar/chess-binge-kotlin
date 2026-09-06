import { Colors } from '@/constants/theme';
import { useTaskStore } from '@/hooks/use-task-store';

export function useAppTheme() {
  const { themeMode } = useTaskStore();
  const dark = themeMode === 'dark';
  return { dark, colors: Colors[dark ? 'dark' : 'light'] };
}
