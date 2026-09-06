import AsyncStorage from '@react-native-async-storage/async-storage';
import { createContext, createElement, PropsWithChildren, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { Appearance } from 'react-native';

export type Task = {
  id: string;
  title: string;
  category: TaskCategory;
  color: string;
  completed: boolean;
  date: string;
  endDate: string;
  range: 'today' | '7days' | 'month' | 'date';
  startTime?: string | null;
  endTime?: string | null;
  reminderEnabled?: boolean;
  reminderMinutesBefore?: number | null;
  repeat?: 'none' | 'daily' | 'weekly' | 'monthly';
  description?: string;
  priority?: 'low' | 'medium' | 'high';
  createdAt: string;
  updatedAt: string;
};

export type TaskRange = Task['range'];
export type AppPreferences = {
  remindersEnabled: boolean;
  weekStartsOn: 'monday' | 'sunday';
  timeFormat: '12h' | '24h';
  calendarView: 'week' | 'month';
};

const defaultPreferences: AppPreferences = {
  remindersEnabled: false,
  weekStartsOn: 'monday',
  timeFormat: '12h',
  calendarView: 'week',
};

export function toDateKey(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function addDays(date: Date, days: number) {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

export function formatDateLabel(dateKey: string) {
  const date = new Date(`${dateKey}T12:00:00`);
  const current = toDateKey(new Date());
  const tomorrow = toDateKey(addDays(new Date(), 1));
  if (dateKey === current) return 'Today';
  if (dateKey === tomorrow) return 'Tomorrow';
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

export function getTaskDateLabel(task: Pick<Task, 'date' | 'endDate' | 'range'>) {
  if (task.range === 'today') return 'Today';
  if (task.range === 'date') return formatDateLabel(task.date);
  return `${formatDateLabel(task.date)} - ${formatDateLabel(task.endDate)}`;
}

function formatTime(value: string, timeFormat: AppPreferences['timeFormat']) {
  const minutes = taskTimeToMinutes(value);
  if (!Number.isFinite(minutes)) return value;
  const hour = Math.floor(minutes / 60);
  const minute = minutes % 60;
  if (timeFormat === '24h') return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
  const displayHour = hour % 12 || 12;
  return `${displayHour}:${String(minute).padStart(2, '0')} ${hour >= 12 ? 'PM' : 'AM'}`;
}

export function formatTaskTime(task: Pick<Task, 'startTime' | 'endTime'>, timeFormat: AppPreferences['timeFormat'] = '12h') {
  if (!task.startTime) return '';
  const start = formatTime(task.startTime, timeFormat);
  return task.endTime ? `${start} – ${formatTime(task.endTime, timeFormat)}` : start;
}

export function taskTimeToMinutes(value?: string | null) {
  if (!value) return Number.POSITIVE_INFINITY;
  const match = value.trim().match(/^(\d{1,2}):(\d{2})\s*(AM|PM)?$/i);
  if (!match) return Number.POSITIVE_INFINITY;
  let hour = Number(match[1]);
  const minute = Number(match[2]);
  if (minute > 59 || hour > 23) return Number.POSITIVE_INFINITY;
  const period = match[3]?.toUpperCase();
  if (period) {
    if (hour < 1 || hour > 12) return Number.POSITIVE_INFINITY;
    if (period === 'PM' && hour !== 12) hour += 12;
    if (period === 'AM' && hour === 12) hour = 0;
  }
  return hour * 60 + minute;
}

export function getTaskScheduleLabel(task: Pick<Task, 'date' | 'endDate' | 'range' | 'startTime' | 'endTime'>, timeFormat: AppPreferences['timeFormat'] = '12h') {
  const time = formatTaskTime(task, timeFormat);
  return time ? `${getTaskDateLabel(task)} • ${time}` : getTaskDateLabel(task);
}

function normalizeDateKey(value: string | undefined) {
  if (!value) return today;
  const date = new Date(value.includes('T') ? value : `${value}T12:00:00`);
  return Number.isNaN(date.getTime()) ? today : toDateKey(date);
}

function getEndDate(startDate: string, range: TaskRange) {
  const start = new Date(`${startDate}T12:00:00`);
  if (range === 'today' || range === 'date') return startDate;
  if (range === '7days') return toDateKey(addDays(start, 6));
  return toDateKey(new Date(start.getFullYear(), start.getMonth() + 1, 0));
}

const today = toDateKey(new Date());
const seededAt = new Date().toISOString();
const initialTasks: Task[] = [
  { id: '1', title: 'Review product brief', category: 'Work', color: '#E7E1FF', completed: true, date: today, endDate: today, range: 'today', createdAt: seededAt, updatedAt: seededAt },
  { id: '2', title: 'Morning run', category: 'Health', color: '#D8F3E9', completed: false, date: today, endDate: today, range: 'today', createdAt: seededAt, updatedAt: seededAt },
  { id: '3', title: 'Design sync with team', category: 'Work', color: '#FFE1D8', completed: false, date: today, endDate: today, range: 'today', createdAt: seededAt, updatedAt: seededAt },
  { id: '4', title: 'Read 20 pages', category: 'Personal', color: '#FFF0C9', completed: false, date: today, endDate: today, range: 'today', createdAt: seededAt, updatedAt: seededAt },
];

export type TaskCategory = 'Personal' | 'Work' | 'Health' | 'Important';
export const taskCategories: TaskCategory[] = ['Personal', 'Work', 'Health', 'Important'];
export type TaskDetails = { title: string; range: TaskRange; category: TaskCategory; startDate?: string; startTime?: string | null; endTime?: string | null; reminderEnabled?: boolean; reminderMinutesBefore?: number | null; repeat?: Task['repeat']; description?: string; priority?: Task['priority'] };
type TaskStore = {
  name: string;
  themeMode: 'light' | 'dark';
  preferences: AppPreferences;
  welcomeSeen: boolean | null;
  tasks: Task[];
  updateName: (value: string) => void;
  setThemeMode: (value: 'light' | 'dark') => void;
  updatePreferences: (value: Partial<AppPreferences>) => void;
  completeWelcome: (value?: string) => void;
  toggleTask: (id: string) => void;
  addTask: (details: TaskDetails) => void;
  updateTask: (id: string, details: TaskDetails) => void;
  deleteTask: (id: string) => void;
  clearTasks: () => void;
  replaceTasks: (next: Task[]) => void;
  resetProfile: () => void;
};

const TaskStoreContext = createContext<TaskStore | null>(null);

export function TaskProvider({ children }: PropsWithChildren) {
  const [name, setName] = useState('User');
  const [themeMode, setThemeModeState] = useState<'light' | 'dark'>(Appearance.getColorScheme() === 'dark' ? 'dark' : 'light');
  const [welcomeSeen, setWelcomeSeen] = useState<boolean | null>(null);
  const [tasks, setTasks] = useState<Task[]>(initialTasks);
  const [preferences, setPreferences] = useState<AppPreferences>(defaultPreferences);

  useEffect(() => {
    Promise.all([AsyncStorage.getItem('focus-name'), AsyncStorage.getItem('focus-tasks'), AsyncStorage.getItem('focus-welcome-seen'), AsyncStorage.getItem('focus-theme'), AsyncStorage.getItem('focus-preferences')]).then(([savedName, savedTasks, savedWelcome, savedTheme, savedPreferences]) => {
      const legacyDefaultName = savedName === 'Alex';
      if (savedName && !legacyDefaultName) setName(savedName);
      setWelcomeSeen(savedWelcome === 'true' || (Boolean(savedName) && !legacyDefaultName));
      if (savedTheme === 'light' || savedTheme === 'dark') {
        setThemeModeState(savedTheme);
        Appearance.setColorScheme(savedTheme);
      }
      if (savedTasks) {
        try {
          const parsed = JSON.parse(savedTasks) as (Partial<Task> & { time?: string; repeat?: string })[];
          setTasks(parsed.map((task) => {
            const date = normalizeDateKey(task.date);
            const now = new Date().toISOString();
            return { ...task, date, endDate: normalizeDateKey(task.endDate ?? task.date), range: task.range === '7days' || task.range === 'month' || task.range === 'date' ? task.range : 'today', category: task.category ?? 'Personal', startTime: task.startTime ?? null, endTime: task.endTime ?? null, reminderEnabled: task.reminderEnabled ?? false, reminderMinutesBefore: task.reminderMinutesBefore ?? null, repeat: task.repeat ?? 'none', description: task.description ?? '', priority: task.priority ?? 'medium', createdAt: task.createdAt ?? now, updatedAt: task.updatedAt ?? now };
          }) as Task[]);
        } catch {
          setTasks(initialTasks);
        }
      }
      if (savedPreferences) {
        try {
          const parsed = JSON.parse(savedPreferences) as Partial<AppPreferences>;
          setPreferences({ ...defaultPreferences, ...parsed });
        } catch {
          setPreferences(defaultPreferences);
        }
      }
    });
  }, []);

  const updateName = useCallback((value: string) => {
    const next = value.trim() || 'User';
    setName(next);
    void AsyncStorage.setItem('focus-name', next);
  }, []);

  const setThemeMode = useCallback((value: 'light' | 'dark') => {
    setThemeModeState(value);
    Appearance.setColorScheme(value);
    void AsyncStorage.setItem('focus-theme', value);
  }, []);

  const updatePreferences = useCallback((value: Partial<AppPreferences>) => {
    const next = { ...preferences, ...value };
    setPreferences(next);
    void AsyncStorage.setItem('focus-preferences', JSON.stringify(next));
  }, [preferences]);

  const completeWelcome = useCallback((value?: string) => {
    const next = value?.trim() || 'User';
    setName(next);
    setWelcomeSeen(true);
    void AsyncStorage.multiSet([['focus-name', next], ['focus-welcome-seen', 'true']]);
  }, []);

  const toggleTask = useCallback((id: string) => {
    const next = tasks.map((task) => task.id === id ? { ...task, completed: !task.completed } : task);
    setTasks(next);
    void AsyncStorage.setItem('focus-tasks', JSON.stringify(next));
  }, [tasks]);

  const persistTasks = useCallback((next: Task[]) => {
    setTasks(next);
    void AsyncStorage.setItem('focus-tasks', JSON.stringify(next));
  }, []);

  const addTask = useCallback((details: TaskDetails) => {
    const startDate = details.startDate ?? toDateKey(new Date());
    const now = new Date().toISOString();
    const next = [...tasks, { id: Date.now().toString(), title: details.title.trim() || 'New task', date: startDate, endDate: getEndDate(startDate, details.range), range: details.range, category: details.category, color: details.category === 'Work' ? '#E7E1FF' : details.category === 'Health' ? '#D8F3E9' : details.category === 'Important' ? '#FFE1D8' : '#FFF0C9', completed: false, startTime: details.startTime ?? null, endTime: details.endTime ?? null, reminderEnabled: details.reminderEnabled ?? false, reminderMinutesBefore: details.reminderMinutesBefore ?? null, repeat: details.repeat ?? 'none', description: details.description?.trim() ?? '', priority: details.priority ?? 'medium', createdAt: now, updatedAt: now }];
    persistTasks(next);
  }, [persistTasks, tasks]);

  const updateTask = useCallback((id: string, details: TaskDetails) => {
    const startDate = details.startDate ?? toDateKey(new Date());
    const updatedAt = new Date().toISOString();
    persistTasks(tasks.map((task) => task.id === id ? { ...task, title: details.title.trim() || 'New task', category: details.category, date: startDate, endDate: getEndDate(startDate, details.range), range: details.range, startTime: details.startTime ?? null, endTime: details.endTime ?? null, reminderEnabled: details.reminderEnabled ?? false, reminderMinutesBefore: details.reminderMinutesBefore ?? null, repeat: details.repeat ?? 'none', description: details.description?.trim() ?? '', priority: details.priority ?? 'medium', updatedAt } : task));
  }, [persistTasks, tasks]);

  const deleteTask = useCallback((id: string) => persistTasks(tasks.filter((task) => task.id !== id)), [persistTasks, tasks]);
  const clearTasks = useCallback(() => persistTasks([]), [persistTasks]);
  const replaceTasks = useCallback((next: Task[]) => persistTasks(next), [persistTasks]);
  const resetProfile = useCallback(() => {
    setName('User');
    setWelcomeSeen(false);
    void AsyncStorage.multiRemove(['focus-name', 'focus-welcome-seen']);
  }, []);

  const value = useMemo(() => ({ name, themeMode, preferences, welcomeSeen, tasks, updateName, setThemeMode, updatePreferences, completeWelcome, toggleTask, addTask, updateTask, deleteTask, clearTasks, replaceTasks, resetProfile }), [name, themeMode, preferences, welcomeSeen, tasks, updateName, setThemeMode, updatePreferences, completeWelcome, toggleTask, addTask, updateTask, deleteTask, clearTasks, replaceTasks, resetProfile]);
  return createElement(TaskStoreContext.Provider, { value }, children);
}

export function useTaskStore() {
  const store = useContext(TaskStoreContext);
  if (!store) throw new Error('useTaskStore must be used inside TaskProvider');
  return store;
}
