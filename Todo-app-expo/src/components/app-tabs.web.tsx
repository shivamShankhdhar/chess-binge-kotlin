import { Ionicons } from '@expo/vector-icons';
import { Tabs, TabList, TabSlot, TabTrigger, TabTriggerSlotProps } from 'expo-router/ui';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/hooks/use-app-theme';

export default function AppTabs() {
  const { colors } = useAppTheme();
  return <Tabs><TabSlot style={styles.slot} /><TabList asChild><View style={[styles.bar, { backgroundColor: colors.surface, borderTopColor: colors.border }]}><TabTrigger name="index" href="/" asChild><TabButton label="Today" icon="home-outline" activeIcon="home" /></TabTrigger><TabTrigger name="calendar" href="/calendar" asChild><TabButton label="Calendar" icon="calendar-outline" activeIcon="calendar" /></TabTrigger><TabTrigger name="analytics" href="/analytics" asChild><TabButton label="Analytics" icon="bar-chart-outline" activeIcon="bar-chart" /></TabTrigger><TabTrigger name="settings" href="/settings" asChild><TabButton label="Settings" icon="settings-outline" activeIcon="settings" /></TabTrigger></View></TabList></Tabs>;
}

function TabButton({ label, icon, activeIcon, isFocused, ...props }: TabTriggerSlotProps & { label: string; icon: keyof typeof Ionicons.glyphMap; activeIcon: keyof typeof Ionicons.glyphMap }) {
  const { colors } = useAppTheme();
  return <Pressable {...props} accessibilityRole="tab" style={({ pressed }) => [styles.tab, pressed && styles.pressed]}><Ionicons name={isFocused ? activeIcon : icon} size={20} color={isFocused ? colors.purple : colors.muted} /><Text style={[styles.label, { color: isFocused ? colors.purple : colors.muted }]}>{label}</Text></Pressable>;
}

const styles = StyleSheet.create({ slot: { flex: 1 }, bar: { minHeight: 66, borderTopWidth: 1, flexDirection: 'row', justifyContent: 'space-around', alignItems: 'center', paddingHorizontal: 18 }, tab: { minWidth: 65, minHeight: 48, alignItems: 'center', justifyContent: 'center', gap: 3 }, label: { fontSize: 10, fontWeight: '700' }, pressed: { opacity: 0.65 } });
