import { Ionicons } from '@expo/vector-icons';
import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { ConfirmationDialog } from '@/components/confirmation-dialog';
import { useAppTheme } from '@/hooks/use-app-theme';
import { getTaskScheduleLabel, Task, useTaskStore } from '@/hooks/use-task-store';

export function TaskRow({ task, onToggle, onMenu, onOpen, highlighted = false, requiresConfirmation = false }: { task: Task; onToggle: () => void; onMenu: () => void; onOpen?: () => void; highlighted?: boolean; requiresConfirmation?: boolean }) {
  const [confirming, setConfirming] = useState(false);
  const { colors } = useAppTheme();
  const { preferences } = useTaskStore();
  const requestToggle = () => requiresConfirmation ? setConfirming(true) : onToggle();
  return (
    <><View style={[styles.row, { borderBottomColor: colors.border, backgroundColor: highlighted ? colors.purpleSurface : colors.surface }, highlighted && styles.highlighted]}>
      <Pressable onPress={requestToggle} style={[styles.checkbox, { borderColor: colors.muted }, task.completed && { backgroundColor: colors.purple, borderColor: colors.purple }]} accessibilityLabel={task.completed ? 'Mark task not done' : 'Mark task done'}>
        {task.completed && <Ionicons name="checkmark" size={14} color="#fff" />}
      </Pressable>
      <Pressable style={styles.main} onPress={onOpen ?? onToggle}>
        <Text style={[styles.title, { color: colors.text }, task.completed && styles.titleDone]}>{task.title}</Text>
        <View style={styles.meta}>
          <Text style={[styles.date, { color: colors.muted }]}>{getTaskScheduleLabel(task, preferences.timeFormat)}</Text>
          <View style={[styles.dot, { backgroundColor: task.color }]} />
          <Text style={[styles.category, { color: colors.muted }]}>{task.category}</Text>
        </View>
      </Pressable>
      <Pressable onPress={onMenu} style={styles.menu} accessibilityLabel={`Actions for ${task.title}`} hitSlop={8}>
        <Ionicons name="ellipsis-vertical" size={20} color={colors.muted} />
      </Pressable>
    </View><ConfirmationDialog visible={confirming} title={task.completed ? 'Mark task as not done?' : 'Mark task as done?'} message={`${task.title} will be updated.`} confirmLabel="Confirm" onCancel={() => setConfirming(false)} onConfirm={() => { onToggle(); setConfirming(false); }} /></>
  );
}

const styles = StyleSheet.create({
  row: { minHeight: 72, flexDirection: 'row', alignItems: 'center', gap: 12, borderBottomWidth: 1, borderBottomColor: '#ECE9EE' },
  highlighted: { borderRadius: 14, paddingHorizontal: 10 },
  checkbox: { width: 24, height: 24, borderRadius: 8, borderWidth: 1.5, borderColor: '#C7C2CD', alignItems: 'center', justifyContent: 'center' },
  checkboxDone: { backgroundColor: '#6B4EFF', borderColor: '#6B4EFF' },
  main: { flex: 1, paddingVertical: 12 },
  title: { color: '#18151F', fontSize: 14, fontWeight: '800' },
  titleDone: { opacity: 0.55, textDecorationLine: 'line-through' },
  meta: { flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 6 },
  date: { color: '#77727F', fontSize: 11 },
  dot: { width: 7, height: 7, borderRadius: 4 },
  category: { color: '#77727F', fontSize: 11 },
  menu: { width: 30, alignItems: 'center' },
});
