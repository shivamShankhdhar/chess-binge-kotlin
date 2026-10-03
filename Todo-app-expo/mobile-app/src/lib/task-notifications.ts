import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';

import { addDays, isTaskCompleted, isTaskScheduledForDate, Task, taskTimeToMinutes, toDateKey } from '@/hooks/use-task-store';

const fallbackHours = [10, 18, 22];
const horizonDays = 31;
const quietHoursStart = 22;
const quietHoursEnd = 7;

Notifications.setNotificationHandler({
  handleNotification: async () => ({ shouldShowBanner: true, shouldShowList: true, shouldPlaySound: true, shouldSetBadge: false }),
});

function dateAt(dateKey: string, minutes: number) {
  const [year, month, day] = dateKey.split('-').map(Number);
  return new Date(year, month - 1, day, Math.floor(minutes / 60), minutes % 60, 0, 0);
}

function occurrenceDates(task: Task) {
  const dates: string[] = [];
  let cursor = new Date(`${task.date}T12:00:00`);
  const end = new Date(`${task.endDate}T12:00:00`);
  for (let index = 0; index < horizonDays && cursor <= end; index += 1) {
    const dateKey = toDateKey(cursor);
    if (isTaskScheduledForDate(task, dateKey)) dates.push(dateKey);
    cursor = addDays(cursor, 1);
  }
  return dates;
}

async function prepareNotifications() {
  if (Platform.OS === 'web') return false;
  const permission = await Notifications.getPermissionsAsync();
  if (!permission.granted) {
    const requested = await Notifications.requestPermissionsAsync();
    if (!requested.granted) return false;
  }
  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('task-reminders', { name: 'Task reminders', importance: Notifications.AndroidImportance.DEFAULT, sound: 'default' });
  }
  await Notifications.setNotificationCategoryAsync('task-reminder', [
    { identifier: 'done', buttonTitle: 'Done', options: { opensAppToForeground: true } },
    { identifier: 'snooze', buttonTitle: 'Snooze 1h', options: { opensAppToForeground: false } },
    { identifier: 'open', buttonTitle: 'Open', options: { opensAppToForeground: true } },
  ]);
  return true;
}

async function cancelExistingTaskReminders() {
  if (Platform.OS === 'web') return;
  const scheduled = await Notifications.getAllScheduledNotificationsAsync();
  await Promise.all(scheduled.filter((item) => item.content.data?.type === 'task-reminder').map((item) => Notifications.cancelScheduledNotificationAsync(item.identifier)));
}

export async function syncTaskNotifications(tasks: Task[], enabled: boolean, quietHoursEnabled = false) {
  if (Platform.OS === 'web') return;
  await cancelExistingTaskReminders();
  if (!enabled || !(await prepareNotifications())) return;

  const now = Date.now();
  for (const task of tasks) {
    if (!task.reminderEnabled) continue;
    for (const dateKey of occurrenceDates(task)) {
      if (isTaskCompleted(task, dateKey)) continue;
      const triggerTimes: Date[] = [];
      if (task.startTime) {
        const startMinutes = taskTimeToMinutes(task.startTime);
        if (Number.isFinite(startMinutes)) triggerTimes.push(new Date(dateAt(dateKey, startMinutes).getTime() - (task.reminderMinutesBefore ?? 15) * 60_000));
      } else {
        for (const hour of fallbackHours) triggerTimes.push(dateAt(dateKey, hour * 60));
      }
      for (const triggerDate of triggerTimes) {
        if (triggerDate.getTime() <= now) continue;
        if (quietHoursEnabled && (triggerDate.getHours() >= quietHoursStart || triggerDate.getHours() < quietHoursEnd)) continue;
        await Notifications.scheduleNotificationAsync({
          content: { title: 'Task reminder', body: task.title, sound: 'default', categoryIdentifier: 'task-reminder', data: { type: 'task-reminder', taskId: task.id, dateKey } },
          trigger: { type: Notifications.SchedulableTriggerInputTypes.DATE, date: triggerDate, ...(Platform.OS === 'android' ? { channelId: 'task-reminders' } : {}) },
        });
      }
    }
  }
}

export async function scheduleTaskSnooze(task: Pick<Task, 'id' | 'title'>, dateKey: string) {
  if (Platform.OS === 'web') return;
  await Notifications.scheduleNotificationAsync({
    content: { title: 'Snoozed task', body: task.title, sound: 'default', categoryIdentifier: 'task-reminder', data: { type: 'task-reminder', taskId: task.id, dateKey } },
    trigger: { type: Notifications.SchedulableTriggerInputTypes.DATE, date: new Date(Date.now() + 60 * 60_000), ...(Platform.OS === 'android' ? { channelId: 'task-reminders' } : {}) },
  });
}
