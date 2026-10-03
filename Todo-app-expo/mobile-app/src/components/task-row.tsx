import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { useState } from 'react';
import { Animated, Easing, Pressable, StyleSheet, Text, View } from 'react-native';

import { ConfirmationDialog } from '@/components/confirmation-dialog';
import { useAppTheme } from '@/hooks/use-app-theme';
import { getTaskScheduleLabel, isTaskCompleted, Task, useTaskStore } from '@/hooks/use-task-store';

export function TaskRow({ task, onToggle, onMenu, onOpen, highlighted = false, requiresConfirmation = false, dateKey, disabled = false }: { task: Task; onToggle: () => void; onMenu: () => void; onOpen?: () => void; highlighted?: boolean; requiresConfirmation?: boolean; dateKey?: string; disabled?: boolean }) {
  const [confirming, setConfirming] = useState(false);
  const { colors } = useAppTheme();
  const { preferences } = useTaskStore();
  const [checkScale] = useState(() => new Animated.Value(1));
  const completed = isTaskCompleted(task, dateKey);
  const animateToggle = () => {
    Animated.sequence([
      Animated.timing(checkScale, { toValue: 0.78, duration: 90, easing: Easing.out(Easing.quad), useNativeDriver: true }),
      Animated.spring(checkScale, { toValue: 1, friction: 5, tension: 180, useNativeDriver: true }),
    ]).start();
  };
  const requestToggle = () => {
    if (disabled) return;
    void Haptics.selectionAsync();
    animateToggle();
    if (requiresConfirmation) setConfirming(true); else onToggle();
  };
  return (
    <><View style={[styles.row, { borderBottomColor: colors.border, backgroundColor: highlighted ? colors.purpleSurface : colors.surface }, highlighted && styles.highlighted, disabled && styles.disabledRow]}>
      <Animated.View style={{ transform: [{ scale: checkScale }] }}>
        <Pressable disabled={disabled} onPress={requestToggle} style={[styles.checkbox, { borderColor: colors.muted }, completed && { backgroundColor: colors.purple, borderColor: colors.purple }]} accessibilityLabel={completed ? 'Mark task not done' : 'Mark task done'} accessibilityState={{ disabled }}>
          {completed && <Ionicons name="checkmark" size={14} color="#fff" />}
        </Pressable>
      </Animated.View>
      <Pressable style={styles.main} onPress={onOpen ?? onToggle}>
        <Text style={[styles.title, { color: colors.text }, completed && styles.titleDone]}>{task.title}</Text>
        <View style={styles.meta}>
          <Text style={[styles.date, { color: colors.muted }]}>{getTaskScheduleLabel(task, preferences.timeFormat)}</Text>
          <View style={[styles.dot, { backgroundColor: task.color }]} />
          <Text style={[styles.category, { color: colors.muted }]}>{task.category}</Text>
          {task.priority === 'high' && <Ionicons name="flag" size={12} color={colors.danger} />}
          {task.reminderEnabled && <Ionicons name="notifications-outline" size={12} color={colors.purple} />}
        </View>
      </Pressable>
      <Pressable disabled={disabled} onPress={disabled ? undefined : onMenu} style={styles.menu} accessibilityLabel={`Actions for ${task.title}`} accessibilityState={{ disabled }} hitSlop={8}>
        <Ionicons name="ellipsis-vertical" size={20} color={colors.muted} />
      </Pressable>
    </View><ConfirmationDialog visible={confirming} title={completed ? 'Mark task as not done?' : 'Mark task as done?'} message={`${task.title} will be updated.`} confirmLabel="Confirm" onCancel={() => setConfirming(false)} onConfirm={() => { void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success); animateToggle(); onToggle(); setConfirming(false); }} /></>
  );
}

const styles = StyleSheet.create({
  row: { minHeight: 68, flexDirection: 'row', alignItems: 'center', gap: 12, borderBottomWidth: 1, borderBottomColor: '#ECE9EE' },
  highlighted: { borderRadius: 14, paddingHorizontal: 10 },
  disabledRow: { opacity: 0.52 },
  checkbox: { width: 26, height: 26, borderRadius: 9, borderWidth: 1.5, borderColor: '#C7C2CD', alignItems: 'center', justifyContent: 'center' },
  checkboxDone: { backgroundColor: '#6B4EFF', borderColor: '#6B4EFF' },
  main: { flex: 1, paddingVertical: 10 },
  title: { color: '#18151F', fontSize: 14, fontWeight: '800' },
  titleDone: { opacity: 0.55, textDecorationLine: 'line-through' },
  meta: { flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 6 },
  date: { color: '#77727F', fontSize: 11 },
  dot: { width: 7, height: 7, borderRadius: 4 },
  category: { color: '#77727F', fontSize: 11 },
  menu: { width: 30, alignItems: 'center' },
});
