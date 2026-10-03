import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAppTheme } from '@/hooks/use-app-theme';
import { useAuth } from '@/hooks/use-auth';
import { useTaskStore } from '@/hooks/use-task-store';

export default function ProfileScreen() {
  const router = useRouter();
  const { user, updateProfile } = useAuth();
  const { name, preferences, updatePreferences, updateName } = useTaskStore();
  const { colors } = useAppTheme();
  const [draftName, setDraftName] = useState(name === 'User' ? '' : name);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const avatarColors = ['#F2C2AE', '#C8B8FF', '#A9E2C4', '#9ED6F5', '#FFD884'];

  const save = async () => {
    const nextName = draftName.trim();
    if (nextName.length < 2) {
      setError('Use at least 2 characters for your name.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      await updateProfile(nextName);
      updateName(nextName);
      router.back();
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : 'Could not save your profile.');
    } finally {
      setSaving(false);
    }
  };

  return <SafeAreaView style={[styles.screen, { backgroundColor: colors.background }]} edges={['top', 'bottom']}><View style={styles.content}>
    <View style={styles.topBar}><Pressable accessibilityLabel="Go back" onPress={() => router.back()} style={styles.iconButton}><Ionicons name="arrow-back" size={22} color={colors.text} /></Pressable><Text style={[styles.title, { color: colors.text }]}>Profile</Text><View style={styles.iconButton} /></View>
    <View style={[styles.hero, { backgroundColor: colors.surface, borderColor: colors.border }]}><View style={[styles.avatar, { backgroundColor: preferences.avatarColor }]}><Text style={styles.avatarText}>{(draftName || 'U').slice(0, 1).toUpperCase()}</Text></View><Text style={[styles.heroTitle, { color: colors.text }]}>Make Focus yours</Text><Text style={[styles.heroCopy, { color: colors.muted }]}>Your name appears on your daily dashboard and reminders.</Text></View>
    <Text style={[styles.heading, { color: colors.text }]}>Account details</Text><Text style={[styles.label, { color: colors.muted }]}>AVATAR COLOR</Text><View style={styles.colorRow}>{avatarColors.map((color) => <Pressable key={color} accessibilityLabel={`Choose avatar color ${color}`} onPress={() => updatePreferences({ avatarColor: color })} style={[styles.colorSwatch, { backgroundColor: color }, preferences.avatarColor === color && { borderColor: colors.purple, borderWidth: 3 }]} />)}</View>
    <Text style={[styles.label, { color: colors.muted }]}>NAME</Text>
    <TextInput value={draftName} onChangeText={(value) => { setDraftName(value); setError(''); }} placeholder="Your name" placeholderTextColor={colors.muted} style={[styles.input, { color: colors.text, backgroundColor: colors.surface, borderColor: colors.border }]} autoCapitalize="words" returnKeyType="done" onSubmitEditing={() => { void save(); }} />
    <Text style={[styles.label, { color: colors.muted }]}>EMAIL</Text>
    <View style={[styles.readOnly, { backgroundColor: colors.elevated, borderColor: colors.border }]}><Ionicons name="mail-outline" size={18} color={colors.muted} /><Text style={[styles.email, { color: colors.muted }]} numberOfLines={1}>{user?.email ?? 'No email'}</Text><Ionicons name="lock-closed-outline" size={15} color={colors.muted} /></View>
    {error ? <Text style={[styles.error, { color: colors.danger }]}>{error}</Text> : null}
    <Pressable disabled={saving} onPress={() => { void save(); }} style={[styles.saveButton, { backgroundColor: colors.purple }, saving && styles.disabled]}><Ionicons name="checkmark-circle-outline" size={18} color={colors.onPurple} /><Text style={[styles.saveText, { color: colors.onPurple }]}>{saving ? 'Saving...' : 'Save profile'}</Text></Pressable>
    <Pressable disabled={saving} onPress={() => router.back()} style={styles.cancel}><Text style={[styles.cancelText, { color: colors.muted }]}>Cancel</Text></Pressable>
  </View></SafeAreaView>;
}

const styles = StyleSheet.create({ screen: { flex: 1 }, content: { flex: 1, padding: 22 }, topBar: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, iconButton: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' }, title: { fontSize: 17, fontWeight: '800' }, hero: { borderRadius: 24, borderWidth: 1, padding: 20, alignItems: 'center', marginTop: 26 }, avatar: { width: 82, height: 82, borderRadius: 41, alignItems: 'center', justifyContent: 'center' }, heroTitle: { fontSize: 18, fontWeight: '800', marginTop: 14 }, heroCopy: { fontSize: 11, lineHeight: 16, textAlign: 'center', marginTop: 5, maxWidth: 250 }, avatarText: { color: '#493700', fontSize: 34, fontWeight: '800' }, heading: { fontSize: 20, fontWeight: '800', marginTop: 26 }, colorRow: { flexDirection: 'row', gap: 11, marginTop: 3 }, colorSwatch: { width: 30, height: 30, borderRadius: 15, borderWidth: 1, borderColor: 'transparent' }, label: { fontSize: 10, fontWeight: '800', letterSpacing: 1.1, marginTop: 24, marginBottom: 9 }, input: { minHeight: 54, borderRadius: 15, borderWidth: 1, paddingHorizontal: 15, fontSize: 15 }, readOnly: { minHeight: 54, borderRadius: 15, borderWidth: 1, paddingHorizontal: 15, flexDirection: 'row', alignItems: 'center', gap: 10 }, email: { flex: 1, fontSize: 14 }, error: { fontSize: 12, fontWeight: '700', marginTop: 12 }, saveButton: { minHeight: 52, borderRadius: 15, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, marginTop: 28 }, saveText: { fontSize: 14, fontWeight: '800' }, cancel: { alignItems: 'center', paddingVertical: 17 }, cancelText: { fontSize: 13, fontWeight: '700' }, disabled: { opacity: 0.55 } });
