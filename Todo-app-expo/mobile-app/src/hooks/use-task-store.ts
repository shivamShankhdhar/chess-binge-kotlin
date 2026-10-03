import AsyncStorage from '@react-native-async-storage/async-storage';
import { createContext, createElement, PropsWithChildren, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { Appearance } from 'react-native';
import { useAuth } from './use-auth';
import { isTaskApiConfigured, taskApi } from '../lib/tasks-api';

export type Task = {
  id: string;
  title: string;
  category: TaskCategory;
  color: string;
  completed: boolean;
  completedDates: string[];
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
export type SyncStatus = 'offline' | 'syncing' | 'synced' | 'error';

export type AppPreferences = {
  remindersEnabled: boolean;
  weekStartsOn: 'monday' | 'sunday';
  timeFormat: '12h' | '24h';
  calendarView: 'week' | 'month';
  quietHoursEnabled: boolean;
  avatarColor: string;
};

const defaultPreferences: AppPreferences = {
  remindersEnabled: false,
  weekStartsOn: 'monday',
  timeFormat: '12h',
  calendarView: 'week',
  quietHoursEnabled: false,
  avatarColor: '#F2C2AE',
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

export function isTaskCompleted(task: Pick<Task, 'completed' | 'completedDates'>, dateKey = toDateKey(new Date())) {
  return Array.isArray(task.completedDates) ? task.completedDates.includes(dateKey) : task.completed;
}

export function isPastDate(dateKey: string) {
  return dateKey < toDateKey(new Date());
}

export function isTaskScheduledForDate(task: Pick<Task, 'date' | 'endDate' | 'repeat'>, dateKey: string) {
  if (task.date > dateKey || task.endDate < dateKey) return false;
  if (task.repeat === 'weekly') {
    return new Date(`${task.date}T12:00:00`).getDay() === new Date(`${dateKey}T12:00:00`).getDay();
  }
  if (task.repeat === 'monthly') {
    return new Date(`${task.date}T12:00:00`).getDate() === new Date(`${dateKey}T12:00:00`).getDate();
  }
  return true;
}

export function canActOnTaskDate(task: Pick<Task, 'date' | 'endDate' | 'repeat'>, dateKey: string) {
  return isTaskScheduledForDate(task, dateKey) && !isPastDate(dateKey);
}

export function formatDateLabel(dateKey: string) {
  const date = new Date(`${dateKey}T12:00:00`);
  const current = toDateKey(new Date());
  const tomorrow = toDateKey(addDays(new Date(), 1));
  if (dateKey === current) return 'Today';
  if (dateKey === tomorrow) return 'Tomorrow';
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

export function getTaskDateLabel(task: Pick<Task, 'date' | 'endDate' | 'range' | 'repeat'>) {
  if (task.range === 'today' && task.repeat === 'none') return 'Today';
  if (task.range === 'date' && task.repeat === 'none') return formatDateLabel(task.date);
  if (task.range === 'date') return `${formatDateLabel(task.date)} · ${task.repeat === 'daily' ? 'Daily' : task.repeat === 'weekly' ? 'Weekly' : 'Monthly'}`;
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

export function getTaskScheduleLabel(task: Pick<Task, 'date' | 'endDate' | 'range' | 'repeat' | 'startTime' | 'endTime'>, timeFormat: AppPreferences['timeFormat'] = '12h') {
  const time = formatTaskTime(task, timeFormat);
  return time ? `${getTaskDateLabel(task)} • ${time}` : getTaskDateLabel(task);
}

function normalizeDateKey(value: string | undefined) {
  if (!value) return today;
  const date = new Date(value.includes('T') ? value : `${value}T12:00:00`);
  return Number.isNaN(date.getTime()) ? today : toDateKey(date);
}

function getEndDate(startDate: string, range: TaskRange, repeat: Task['repeat'] = 'none'): string {
  const start = new Date(`${startDate}T12:00:00`);
  if (range === 'date') {
    if (repeat === 'daily') return toDateKey(addDays(start, 365));
    if (repeat === 'weekly') return toDateKey(addDays(start, 364));
    if (repeat === 'monthly') return toDateKey(new Date(start.getFullYear() + 1, start.getMonth(), start.getDate()));
    return startDate;
  }
  if (range === 'today') return repeat === 'none' ? startDate : getEndDate(startDate, 'date', repeat);
  if (range === '7days') return toDateKey(addDays(start, 6));
  return toDateKey(new Date(start.getFullYear(), start.getMonth() + 1, 0));
}

const today = toDateKey(new Date());
const seededAt = new Date().toISOString();
const initialTasks: Task[] = [
  { id: '1', title: 'Review product brief', category: 'Work', color: '#E7E1FF', completed: true, completedDates: [today], date: today, endDate: today, range: 'today', createdAt: seededAt, updatedAt: seededAt },
  { id: '2', title: 'Morning run', category: 'Health', color: '#D8F3E9', completed: false, completedDates: [], date: today, endDate: today, range: 'today', createdAt: seededAt, updatedAt: seededAt },
  { id: '3', title: 'Design sync with team', category: 'Work', color: '#FFE1D8', completed: false, completedDates: [], date: today, endDate: today, range: 'today', createdAt: seededAt, updatedAt: seededAt },
  { id: '4', title: 'Read 20 pages', category: 'Personal', color: '#FFF0C9', completed: false, completedDates: [], date: today, endDate: today, range: 'today', createdAt: seededAt, updatedAt: seededAt },
];

function normalizeTask(task: Task): Task {
  const completedDates = Array.isArray(task.completedDates) ? task.completedDates : task.completed ? [task.date] : [];
  return { ...task, completedDates, completed: completedDates.includes(toDateKey(new Date())) };
}

function taskPayload(task: Task): Omit<Task, 'id' | 'createdAt' | 'updatedAt'> {
  const { id: _id, createdAt: _createdAt, updatedAt: _updatedAt, ...payload } = task;
  return payload;
}

export type TaskCategory = 'Personal' | 'Work' | 'Health' | 'Important';
export const taskCategories: TaskCategory[] = ['Personal', 'Work', 'Health', 'Important'];
export type TaskDetails = { title: string; range: TaskRange; category: TaskCategory; startDate?: string; startTime?: string | null; endTime?: string | null; reminderEnabled?: boolean; reminderMinutesBefore?: number | null; repeat?: Task['repeat']; description?: string; priority?: Task['priority'] };
type TaskStore = {
  name: string;
  themeMode: 'light' | 'dark';
  preferences: AppPreferences;
  welcomeSeen: boolean | null;
  tasks: Task[];
  syncStatus: SyncStatus;
  isHydrating: boolean;
  lastDeletedTask: Task | null;
  lastTaskAction: { taskId: string; dateKey: string; previousCompleted: boolean } | null;
  updateName: (value: string) => void;
  setThemeMode: (value: 'light' | 'dark') => void;
  updatePreferences: (value: Partial<AppPreferences>) => void;
  completeWelcome: (value?: string) => void;
  toggleTask: (id: string, dateKey?: string) => void;
  addTask: (details: TaskDetails) => void;
  updateTask: (id: string, details: TaskDetails) => void;
  deleteTask: (id: string) => void;
  undoDelete: () => void;
  undoLastTaskAction: () => void;
  clearTasks: () => void;
  replaceTasks: (next: Task[]) => void;
  resetProfile: () => void;
};

const TaskStoreContext = createContext<TaskStore | null>(null);

export function TaskProvider({ children }: PropsWithChildren) {
  const { user } = useAuth();
  const accountKey = user?.id ?? 'anonymous';
  const nameStorageKey = `focus-name-${accountKey}`;
  const tasksStorageKey = `focus-tasks-${accountKey}`;
  const welcomeStorageKey = `focus-welcome-seen-${accountKey}`;
  const [name, setName] = useState(user?.name ?? 'User');
  const [themeMode, setThemeModeState] = useState<'light' | 'dark'>(Appearance.getColorScheme() === 'dark' ? 'dark' : 'light');
  const [welcomeSeen, setWelcomeSeen] = useState<boolean | null>(null);
  const [tasks, setTasks] = useState<Task[]>(initialTasks);
  const [syncStatus, setSyncStatus] = useState<SyncStatus>('offline');
  const [lastDeletedTask, setLastDeletedTask] = useState<Task | null>(null);
  const [lastTaskAction, setLastTaskAction] = useState<{ taskId: string; dateKey: string; previousCompleted: boolean } | null>(null);
  const [preferences, setPreferences] = useState<AppPreferences>(defaultPreferences);

  useEffect(() => {
    if (!user) return;
    Promise.all([AsyncStorage.getItem(nameStorageKey), AsyncStorage.getItem(tasksStorageKey), AsyncStorage.getItem(welcomeStorageKey), AsyncStorage.getItem('focus-theme'), AsyncStorage.getItem('focus-preferences')]).then(async ([savedName, savedTasks, savedWelcome, savedTheme, savedPreferences]) => {
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
            const completedDates = Array.isArray(task.completedDates) ? task.completedDates.filter((value): value is string => typeof value === 'string') : task.completed ? [date] : [];
            return { ...task, date, endDate: normalizeDateKey(task.endDate ?? task.date), range: task.range === '7days' || task.range === 'month' || task.range === 'date' ? task.range : 'today', category: task.category ?? 'Personal', startTime: task.startTime ?? null, endTime: task.endTime ?? null, reminderEnabled: task.reminderEnabled ?? false, reminderMinutesBefore: task.reminderMinutesBefore ?? null, repeat: task.repeat ?? 'none', description: task.description ?? '', priority: task.priority ?? 'medium', completedDates, completed: completedDates.includes(toDateKey(new Date())), createdAt: task.createdAt ?? now, updatedAt: task.updatedAt ?? now };
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
      if (user) {
        setName(user.name);
        setWelcomeSeen(true);
      }
      if (isTaskApiConfigured) {
        setSyncStatus('syncing');
        try {
          const remoteTasks = await taskApi.list();
          const normalizedRemoteTasks = remoteTasks.map(normalizeTask);
          setTasks(normalizedRemoteTasks);
          void AsyncStorage.setItem(tasksStorageKey, JSON.stringify(normalizedRemoteTasks));
          setSyncStatus('synced');
        } catch {
          setSyncStatus('error');
          // Keep the locally cached tasks available when the server is offline.
        }
      }
    });
  }, [nameStorageKey, tasksStorageKey, user, welcomeStorageKey]);

  const updateName = useCallback((value: string) => {
    const next = value.trim() || 'User';
    setName(next);
    void AsyncStorage.setItem(nameStorageKey, next);
  }, [nameStorageKey]);

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
    void AsyncStorage.multiSet([[nameStorageKey, next], [welcomeStorageKey, 'true']]);
  }, [nameStorageKey, welcomeStorageKey]);

  const persistTasks = useCallback((next: Task[]) => {
    setTasks(next);
    void AsyncStorage.setItem(tasksStorageKey, JSON.stringify(next));
  }, [tasksStorageKey]);

  const replaceWithServerTask = useCallback((localId: string, serverTask: Task) => {
    setTasks((current) => {
      const next = current.map((task) => task.id === localId ? normalizeTask(serverTask) : task);
      void AsyncStorage.setItem(tasksStorageKey, JSON.stringify(next));
      return next;
    });
  }, [tasksStorageKey]);

  const markSyncing = useCallback(() => setSyncStatus('syncing'), []);
  const markSynced = useCallback(() => setSyncStatus('synced'), []);
  const markSyncError = useCallback(() => setSyncStatus('error'), []);

  const addTask = useCallback((details: TaskDetails) => {
    const startDate = details.startDate ?? toDateKey(new Date());
    const now = new Date().toISOString();
    const task: Task = { id: Date.now().toString(), title: details.title.trim() || 'New task', date: startDate, endDate: getEndDate(startDate, details.range, details.repeat), range: details.range, category: details.category, color: details.category === 'Work' ? '#E7E1FF' : details.category === 'Health' ? '#D8F3E9' : details.category === 'Important' ? '#FFE1D8' : '#FFF0C9', completed: false, completedDates: [], startTime: details.startTime ?? null, endTime: details.endTime ?? null, reminderEnabled: details.reminderEnabled ?? false, reminderMinutesBefore: details.reminderMinutesBefore ?? null, repeat: details.repeat ?? 'none', description: details.description?.trim() ?? '', priority: details.priority ?? 'medium', createdAt: now, updatedAt: now };
    const next = [...tasks, task];
    persistTasks(next);
    if (isTaskApiConfigured) {
      markSyncing();
      void taskApi.create(taskPayload(task)).then((serverTask) => { replaceWithServerTask(task.id, serverTask); markSynced(); }).catch(markSyncError);
    }
  }, [markSyncError, markSynced, markSyncing, persistTasks, replaceWithServerTask, tasks]);

  const updateTask = useCallback((id: string, details: TaskDetails) => {
    const startDate = details.startDate ?? toDateKey(new Date());
    const updatedAt = new Date().toISOString();
    const next = tasks.map((task) => task.id === id ? { ...task, title: details.title.trim() || 'New task', category: details.category, date: startDate, endDate: getEndDate(startDate, details.range, details.repeat), range: details.range, startTime: details.startTime ?? null, endTime: details.endTime ?? null, reminderEnabled: details.reminderEnabled ?? false, reminderMinutesBefore: details.reminderMinutesBefore ?? null, repeat: details.repeat ?? 'none', description: details.description?.trim() ?? '', priority: details.priority ?? 'medium', updatedAt } : task);
    persistTasks(next);
    const updatedTask = next.find((task) => task.id === id);
    if (isTaskApiConfigured && updatedTask) {
      markSyncing();
      void taskApi.update(id, taskPayload(updatedTask)).then((serverTask) => { replaceWithServerTask(id, serverTask); markSynced(); }).catch(markSyncError);
    }
  }, [markSyncError, markSynced, markSyncing, persistTasks, replaceWithServerTask, tasks]);

  const toggleTask = useCallback((id: string, dateKey = toDateKey(new Date())) => {
    const target = tasks.find((task) => task.id === id);
    if (!target || !canActOnTaskDate(target, dateKey)) return;
    const completed = isTaskCompleted(target, dateKey);
    setLastTaskAction({ taskId: id, dateKey, previousCompleted: completed });
    const completedDates = completed ? target.completedDates.filter((value) => value !== dateKey) : [...target.completedDates, dateKey];
    const updatedAt = new Date().toISOString();
    const nextTask = { ...target, completedDates, completed: completedDates.includes(toDateKey(new Date())), updatedAt };
    persistTasks(tasks.map((task) => task.id === id ? nextTask : task));
    if (isTaskApiConfigured) {
      markSyncing();
      void taskApi.updateOccurrence(id, dateKey, !completed).then((serverTask) => { replaceWithServerTask(id, { ...serverTask, completedDates: serverTask.completedDates ?? completedDates, completed: isTaskCompleted({ ...serverTask, completedDates: serverTask.completedDates ?? completedDates }) }); markSynced(); }).catch(markSyncError);
    }
  }, [markSyncError, markSynced, markSyncing, persistTasks, replaceWithServerTask, tasks]);

  const deleteTask = useCallback((id: string) => {
    const target = tasks.find((task) => task.id === id);
    if (!target) return;
    setLastDeletedTask(target);
    setLastTaskAction(null);
    persistTasks(tasks.filter((task) => task.id !== id));
    if (isTaskApiConfigured) {
      markSyncing();
      void taskApi.remove(id).then(markSynced).catch(markSyncError);
    }
  }, [markSyncError, markSynced, markSyncing, persistTasks, tasks]);

  const undoDelete = useCallback(() => {
    if (!lastDeletedTask) return;
    const restored = lastDeletedTask;
    setLastDeletedTask(null);
    persistTasks([...tasks, restored].sort((a, b) => a.date.localeCompare(b.date) || a.createdAt.localeCompare(b.createdAt)));
    if (isTaskApiConfigured) {
      markSyncing();
      void taskApi.create(taskPayload(restored)).then((serverTask) => { replaceWithServerTask(restored.id, serverTask); markSynced(); }).catch(markSyncError);
    }
  }, [lastDeletedTask, markSyncError, markSynced, markSyncing, persistTasks, replaceWithServerTask, tasks]);

  const undoLastTaskAction = useCallback(() => {
    if (!lastTaskAction) return;
    const current = tasks.find((task) => task.id === lastTaskAction.taskId);
    if (!current || isTaskCompleted(current, lastTaskAction.dateKey) === lastTaskAction.previousCompleted) { setLastTaskAction(null); return; }
    toggleTask(lastTaskAction.taskId, lastTaskAction.dateKey);
    setLastTaskAction(null);
  }, [lastTaskAction, tasks, toggleTask]);

  const clearTasks = useCallback(() => {
    setLastTaskAction(null);
    persistTasks([]);
    if (isTaskApiConfigured) { markSyncing(); void taskApi.clear().then(markSynced).catch(markSyncError); }
  }, [markSyncError, markSynced, markSyncing, persistTasks]);

  const replaceTasks = useCallback((next: Task[]) => {
    persistTasks(next);
    if (isTaskApiConfigured) {
      void (async () => {
        await taskApi.clear();
        const restored = await Promise.all(next.map((task) => taskApi.create(taskPayload(task))));
        persistTasks(restored);
      })().catch(() => undefined);
    }
  }, [persistTasks]);
  const resetProfile = useCallback(() => {
    setName('User');
    setWelcomeSeen(false);
    void AsyncStorage.multiRemove([nameStorageKey, welcomeStorageKey]);
  }, [nameStorageKey, welcomeStorageKey]);

  const isHydrating = Boolean(user) && welcomeSeen === null;
  const value = useMemo(() => ({ name, themeMode, preferences, welcomeSeen, tasks, syncStatus, isHydrating, lastDeletedTask, lastTaskAction, updateName, setThemeMode, updatePreferences, completeWelcome, toggleTask, addTask, updateTask, deleteTask, undoDelete, undoLastTaskAction, clearTasks, replaceTasks, resetProfile }), [name, themeMode, preferences, welcomeSeen, tasks, syncStatus, isHydrating, lastDeletedTask, lastTaskAction, updateName, setThemeMode, updatePreferences, completeWelcome, toggleTask, addTask, updateTask, deleteTask, undoDelete, undoLastTaskAction, clearTasks, replaceTasks, resetProfile]);
  return createElement(TaskStoreContext.Provider, { value }, children);
}

export function useTaskStore() {
  const store = useContext(TaskStoreContext);
  if (!store) throw new Error('useTaskStore must be used inside TaskProvider');
  return store;
}
