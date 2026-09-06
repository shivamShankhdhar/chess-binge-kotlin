import { Ionicons } from '@expo/vector-icons';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAppTheme } from '@/hooks/use-app-theme';
import { taskCategories, TaskCategory, TaskDetails, TaskRange, toDateKey, useTaskStore } from '@/hooks/use-task-store';

const ranges: { value: TaskRange; label: string }[] = [{ value: 'date', label: 'Date' }, { value: 'today', label: 'Today' }, { value: '7days', label: 'Next 7 days' }, { value: 'month', label: 'This month' }];
const repeats: { value: NonNullable<TaskDetails['repeat']>; label: string }[] = [{ value: 'none', label: 'Does not repeat' }, { value: 'daily', label: 'Daily' }, { value: 'weekly', label: 'Weekly' }, { value: 'monthly', label: 'Monthly' }];
const priorities: { value: NonNullable<TaskDetails['priority']>; label: string; color: string }[] = [{ value: 'low', label: 'Low', color: '#62B9F5' }, { value: 'medium', label: 'Medium', color: '#FFC94A' }, { value: 'high', label: 'High', color: '#F26B6B' }];
const reminderOptions = [5, 15, 30, 60];

function parseTime(value: string) {
  const match = value.trim().match(/^(\d{1,2}):(\d{2})\s*(AM|PM)?$/i);
  if (!match) return null;
  let hour = Number(match[1]); const minute = Number(match[2]);
  if (minute > 59 || hour > 23) return null;
  const period = match[3]?.toUpperCase();
  if (period) { if (hour < 1 || hour > 12) return null; if (period === 'PM' && hour !== 12) hour += 12; if (period === 'AM' && hour === 12) hour = 0; }
  return hour * 60 + minute;
}

function isDateKey(value: string) {
  const parsed = new Date(`${value}T12:00:00`);
  return /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(parsed.getTime()) && toDateKey(parsed) === value;
}

export default function CreateTaskScreen() {
  const router = useRouter();
  const { edit, date: selectedDate } = useLocalSearchParams<{ edit?: string; date?: string }>();
  const { tasks, addTask, updateTask } = useTaskStore();
  const { colors } = useAppTheme();
  const existing = tasks.find((task) => task.id === edit);
  const initialDate = typeof selectedDate === 'string' ? selectedDate : existing?.date ?? toDateKey(new Date());
  const [title, setTitle] = useState(existing?.title ?? '');
  const [range, setRange] = useState<TaskRange>(existing?.range ?? 'date');
  const [date, setDate] = useState(initialDate);
  const [category, setCategory] = useState<TaskCategory>(existing?.category ?? 'Personal');
  const [hasTime, setHasTime] = useState(Boolean(existing?.startTime));
  const [startTime, setStartTime] = useState(existing?.startTime ?? '');
  const [endTime, setEndTime] = useState(existing?.endTime ?? '');
  const [priority, setPriority] = useState<NonNullable<TaskDetails['priority']>>(existing?.priority ?? 'medium');
  const [repeat, setRepeat] = useState<NonNullable<TaskDetails['repeat']>>(existing?.repeat ?? 'none');
  const [reminderEnabled, setReminderEnabled] = useState(existing?.reminderEnabled ?? false);
  const [reminderMinutesBefore, setReminderMinutesBefore] = useState(existing?.reminderMinutesBefore ?? 15);
  const [description, setDescription] = useState(existing?.description ?? '');
  const [error, setError] = useState('');

  // The edit route can resolve after AsyncStorage finishes hydrating the task store.
  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { if (existing) { setTitle(existing.title); setRange(existing.range); setDate(existing.date); setCategory(existing.category); setHasTime(Boolean(existing.startTime)); setStartTime(existing.startTime ?? ''); setEndTime(existing.endTime ?? ''); setPriority(existing.priority ?? 'medium'); setRepeat(existing.repeat ?? 'none'); setReminderEnabled(existing.reminderEnabled ?? false); setReminderMinutesBefore(existing.reminderMinutesBefore ?? 15); setDescription(existing.description ?? ''); } }, [existing]);
  const save = () => {
    const start = hasTime ? startTime.trim() || null : null; const end = hasTime ? endTime.trim() || null : null;
    const startMinutes = start ? parseTime(start) : null; const endMinutes = end ? parseTime(end) : null;
    if (!title.trim()) return setError('Add a task title first.');
    if (!isDateKey(date)) return setError('Use a valid date in YYYY-MM-DD format.');
    if (hasTime && !start) return setError('Add a start time or remove the time section.');
    if (end && !start) return setError('Add a start time before an end time.');
    if (start && startMinutes === null) return setError('Use a time such as 3:00 PM.');
    if (end && (endMinutes === null || (startMinutes !== null && endMinutes < startMinutes))) return setError('End time cannot be before start time.');
    const details: TaskDetails = { title, range, category, startDate: date, startTime: start, endTime: end, priority, repeat, reminderEnabled, reminderMinutesBefore: reminderEnabled ? reminderMinutesBefore : null, description };
    if (edit) updateTask(edit, details); else addTask(details); router.back();
  };
  const fieldStyle = [styles.input, { backgroundColor: colors.surface, color: colors.text, borderColor: colors.border }];
  const pillStyle = (active: boolean) => [styles.pill, { backgroundColor: active ? colors.purpleSurface : colors.surface, borderColor: active ? colors.purple : colors.border }];
  const categoryColor = (item: TaskCategory) => item === 'Work' ? colors.purple : item === 'Health' ? colors.green : item === 'Important' ? colors.danger : colors.warning;

  return <SafeAreaView style={[styles.screen, { backgroundColor: colors.background }]} edges={['top', 'bottom']}><ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled"><View style={styles.topBar}><Pressable accessibilityLabel="Go back" onPress={() => router.back()} style={styles.iconButton}><Ionicons name="arrow-back" size={22} color={colors.text} /></Pressable><Text style={[styles.topTitle, { color: colors.text }]}>{edit ? 'Edit Task' : 'Add New Task'}</Text><Pressable accessibilityLabel="Save task" onPress={save}><Text style={[styles.saveTop, { color: colors.purple }]}>Save</Text></Pressable></View><Text style={[styles.prompt, { color: colors.text }]}>What do you need to do?</Text><TextInput value={title} onChangeText={(value) => { setTitle(value); setError(''); }} placeholder="e.g. Finish the dashboard design" placeholderTextColor={colors.muted} style={fieldStyle} /><Text style={[styles.label, { color: colors.muted }]}>DATE</Text><TextInput value={date} onChangeText={(value) => { setDate(value); setError(''); }} placeholder="YYYY-MM-DD" placeholderTextColor={colors.muted} style={fieldStyle} autoCapitalize="none" /><View style={[styles.pillRow, styles.rangeRow]}>{ranges.map((item) => <Pressable key={item.value} onPress={() => { setRange(item.value); if (item.value === 'today') setDate(toDateKey(new Date())); }} style={pillStyle(range === item.value)}><Text style={[styles.pillText, { color: range === item.value ? colors.purple : colors.muted }]}>{item.label}</Text></Pressable>)}</View><Text style={[styles.label, { color: colors.muted }]}>TIME <Text style={styles.optional}>OPTIONAL</Text></Text>{hasTime ? <><View style={styles.timeRow}><TextInput value={startTime} onChangeText={setStartTime} placeholder="Start, e.g. 3:00 PM" placeholderTextColor={colors.muted} style={[fieldStyle, styles.timeInput]} /><Text style={[styles.toText, { color: colors.muted }]}>to</Text><TextInput value={endTime} onChangeText={setEndTime} placeholder="End (optional)" placeholderTextColor={colors.muted} style={[fieldStyle, styles.timeInput]} /></View><Pressable onPress={() => { setHasTime(false); setStartTime(''); setEndTime(''); }}><Text style={[styles.removeTime, { color: colors.purple }]}>Remove time</Text></Pressable></> : <Pressable onPress={() => setHasTime(true)} style={[styles.addTime, { backgroundColor: colors.elevated }]}><Ionicons name="time-outline" size={17} color={colors.purple} /><Text style={[styles.addTimeText, { color: colors.purple }]}>Add time</Text></Pressable>}<Text style={[styles.label, { color: colors.muted }]}>CATEGORY</Text><View style={styles.pillRow}>{taskCategories.map((item) => <Pressable key={item} onPress={() => setCategory(item)} style={pillStyle(category === item)}><View style={[styles.categoryDot, { backgroundColor: categoryColor(item) }]} /><Text style={[styles.pillText, { color: category === item ? colors.purple : colors.muted }]}>{item}</Text></Pressable>)}</View><Text style={[styles.label, { color: colors.muted }]}>PRIORITY</Text><View style={styles.pillRow}>{priorities.map((item) => <Pressable key={item.value} onPress={() => setPriority(item.value)} style={pillStyle(priority === item.value)}><View style={[styles.categoryDot, { backgroundColor: item.color }]} /><Text style={[styles.pillText, { color: priority === item.value ? colors.purple : colors.muted }]}>{item.label}</Text></Pressable>)}</View><Text style={[styles.label, { color: colors.muted }]}>REPEAT</Text><View style={styles.pillRow}>{repeats.map((item) => <Pressable key={item.value} onPress={() => setRepeat(item.value)} style={pillStyle(repeat === item.value)}><Text style={[styles.pillText, { color: repeat === item.value ? colors.purple : colors.muted }]}>{item.label}</Text></Pressable>)}</View><View style={[styles.reminderRow, { backgroundColor: colors.surface, borderColor: colors.border }]}><View style={styles.reminderCopy}><Ionicons name="notifications-outline" size={18} color={colors.purple} /><View><Text style={[styles.reminderTitle, { color: colors.text }]}>Reminder</Text><Text style={[styles.reminderText, { color: colors.muted }]}>{reminderEnabled ? `${reminderMinutesBefore} minutes before` : 'Off'}</Text></View></View><Pressable accessibilityRole="switch" accessibilityState={{ checked: reminderEnabled }} onPress={() => setReminderEnabled((value) => !value)} style={[styles.switch, { backgroundColor: reminderEnabled ? colors.purple : colors.muted }]}><View style={[styles.switchThumb, reminderEnabled && styles.switchOn]} /></Pressable></View>{reminderEnabled && <View style={[styles.pillRow, styles.reminderOptions]}>{reminderOptions.map((minutes) => <Pressable key={minutes} onPress={() => setReminderMinutesBefore(minutes)} style={pillStyle(reminderMinutesBefore === minutes)}><Text style={[styles.pillText, { color: reminderMinutesBefore === minutes ? colors.purple : colors.muted }]}>{minutes === 60 ? '1 hour' : `${minutes} min`}</Text></Pressable>)}</View>}<Text style={[styles.label, { color: colors.muted }]}>DESCRIPTION <Text style={styles.optional}>OPTIONAL</Text></Text><TextInput value={description} onChangeText={setDescription} multiline placeholder="Add more details about this task..." placeholderTextColor={colors.muted} style={[fieldStyle, styles.description]} />{error ? <Text style={[styles.error, { color: colors.danger }]}>{error}</Text> : null}<Pressable accessibilityRole="button" style={[styles.primaryButton, { backgroundColor: colors.purple }]} onPress={save}><Text style={[styles.primaryButtonText, { color: colors.onPurple }]}>{edit ? 'Save Changes' : 'Create Task'}</Text></Pressable></ScrollView></SafeAreaView>;
}

const styles = StyleSheet.create({ screen: { flex: 1 }, content: { padding: 22, paddingBottom: 45 }, topBar: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 25 }, iconButton: { width: 44, height: 44, justifyContent: 'center', alignItems: 'center' }, topTitle: { fontSize: 16, fontWeight: '800' }, saveTop: { fontSize: 13, fontWeight: '800' }, prompt: { fontSize: 24, fontWeight: '800', marginBottom: 14 }, input: { borderRadius: 15, borderWidth: 1, paddingHorizontal: 15, paddingVertical: 15, minHeight: 54, fontSize: 14 }, label: { fontSize: 10, fontWeight: '800', letterSpacing: 1.1, marginTop: 23, marginBottom: 9 }, optional: { fontWeight: '500', letterSpacing: 0 }, pillRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 }, rangeRow: { marginTop: 9 }, pill: { minHeight: 40, borderRadius: 13, borderWidth: 1, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', gap: 6 }, pillText: { fontSize: 11, fontWeight: '800' }, categoryDot: { width: 8, height: 8, borderRadius: 4 }, timeRow: { flexDirection: 'row', alignItems: 'center', gap: 8 }, timeInput: { flex: 1, minWidth: 0, paddingHorizontal: 11 }, toText: { fontSize: 12 }, addTime: { minHeight: 48, alignSelf: 'flex-start', borderRadius: 13, paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center', gap: 7 }, addTimeText: { fontSize: 12, fontWeight: '800' }, removeTime: { fontSize: 12, fontWeight: '800', marginTop: 10 }, reminderRow: { minHeight: 67, borderRadius: 16, borderWidth: 1, marginTop: 22, paddingHorizontal: 15, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, reminderCopy: { flexDirection: 'row', alignItems: 'center', gap: 11 }, reminderTitle: { fontSize: 13, fontWeight: '800' }, reminderText: { fontSize: 11, marginTop: 3 }, switch: { width: 44, height: 25, borderRadius: 13, padding: 3, justifyContent: 'center' }, switchThumb: { width: 19, height: 19, borderRadius: 10, backgroundColor: '#fff' }, switchOn: { alignSelf: 'flex-end' }, reminderOptions: { marginTop: 10 }, description: { minHeight: 100, textAlignVertical: 'top' }, error: { fontSize: 12, fontWeight: '700', marginTop: 10 }, primaryButton: { borderRadius: 15, minHeight: 52, alignItems: 'center', justifyContent: 'center', marginTop: 24 }, primaryButtonText: { fontSize: 14, fontWeight: '800' } });
