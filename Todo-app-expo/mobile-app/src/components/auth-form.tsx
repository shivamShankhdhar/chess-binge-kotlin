import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAuth } from '@/hooks/use-auth';
import { ApiError } from '@/lib/tasks-api';

type AuthFormProps = { mode: 'sign-in' | 'sign-up'; onSwitch: () => void };

const authColors = {
  background: '#0D0B15',
  surface: '#191523',
  border: '#342C46',
  text: '#F8F5FF',
  muted: '#AAA2B8',
  purple: '#A78BFA',
  purpleDeep: '#7C5CE7',
  danger: '#FF8BA1',
};

export function AuthForm({ mode, onSwitch }: AuthFormProps) {
  const { signIn, signUp } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const isSignUp = mode === 'sign-up';

  const submit = async () => {
    setError('');
    if (isSignUp && name.trim().length < 2) return setError('Enter your name.');
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) return setError('Enter a valid email address.');
    if (password.length < 8) return setError('Use at least 8 characters for your password.');

    setSubmitting(true);
    try {
      if (isSignUp) await signUp(name.trim(), email.trim(), password);
      else await signIn(email.trim(), password);
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : 'Unable to continue. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return <SafeAreaView style={styles.screen} edges={['top', 'bottom']}><KeyboardAvoidingView style={styles.keyboard} behavior={Platform.OS === 'ios' ? 'padding' : undefined}><View style={styles.content}>
    <View style={styles.brand}><Image source={require('@/assets/app-icon/app-icon.png')} contentFit="contain" style={styles.brandIcon} /><Text style={styles.brandName}>Focus</Text></View>
    <View style={styles.hero}><LinearGradient colors={['#30244E', '#171220']} style={styles.iconHalo}><Image source={require('@/assets/app-icon/app-icon.png')} contentFit="contain" style={styles.heroIcon} /></LinearGradient><Text style={styles.eyebrow}>{isSignUp ? 'CREATE YOUR ACCOUNT' : 'WELCOME BACK'}</Text><Text style={styles.title}>{isSignUp ? 'Plan with more clarity.' : 'Your day, in focus.'}</Text><Text style={styles.copy}>{isSignUp ? 'Create an account to keep your tasks private and available across sessions.' : 'Sign in to continue with your personal task space.'}</Text></View>
    <View style={styles.form}>
      {isSignUp ? <Field icon="person-outline" value={name} onChangeText={setName} placeholder="Your name" autoCapitalize="words" /> : null}
      <Field icon="mail-outline" value={email} onChangeText={setEmail} placeholder="Email address" autoCapitalize="none" keyboardType="email-address" />
      <View style={styles.field}><Ionicons name="lock-closed-outline" size={19} color={authColors.muted} /><TextInput value={password} onChangeText={setPassword} placeholder="Password" placeholderTextColor={authColors.muted} selectionColor={authColors.purple} secureTextEntry={!showPassword} autoCapitalize="none" autoCorrect={false} style={styles.input} /><Pressable accessibilityLabel={showPassword ? 'Hide password' : 'Show password'} onPress={() => setShowPassword((value) => !value)} hitSlop={10}><Ionicons name={showPassword ? 'eye-off-outline' : 'eye-outline'} size={20} color={authColors.muted} /></Pressable></View>
      {error ? <Text accessibilityRole="alert" style={styles.error}>{error}</Text> : null}
      <Pressable accessibilityRole="button" accessibilityState={{ disabled: submitting }} disabled={submitting} onPress={submit} style={[styles.submit, submitting && styles.submitDisabled]}><Text style={styles.submitText}>{submitting ? 'Please wait…' : isSignUp ? 'Create account' : 'Sign in'}</Text><Ionicons name="arrow-forward" size={18} color="#fff" /></Pressable>
    </View>
    <View style={styles.switchRow}><Text style={styles.switchCopy}>{isSignUp ? 'Already have an account?' : 'New to Focus?'}</Text><Pressable onPress={onSwitch} hitSlop={10}><Text style={styles.switchAction}>{isSignUp ? 'Sign in' : 'Create account'}</Text></Pressable></View>
  </View></KeyboardAvoidingView></SafeAreaView>;
}

function Field({ icon, ...props }: { icon: keyof typeof Ionicons.glyphMap } & React.ComponentProps<typeof TextInput>) {
  return <View style={styles.field}><Ionicons name={icon} size={19} color={authColors.muted} /><TextInput placeholderTextColor={authColors.muted} selectionColor={authColors.purple} autoCorrect={false} style={styles.input} {...props} /></View>;
}

const styles = StyleSheet.create({ screen: { flex: 1, backgroundColor: authColors.background }, keyboard: { flex: 1 }, content: { flex: 1, paddingHorizontal: 26, paddingTop: 18, paddingBottom: 22, justifyContent: 'center' }, brand: { flexDirection: 'row', alignItems: 'center', gap: 8, position: 'absolute', top: 18, left: 26 }, brandIcon: { width: 37, height: 37 }, brandName: { fontSize: 18, fontWeight: '800', color: authColors.text }, hero: { marginTop: 72, alignItems: 'center' }, iconHalo: { width: 142, height: 142, borderRadius: 44, borderWidth: 1, borderColor: '#4C3A72', alignItems: 'center', justifyContent: 'center', marginBottom: 20, boxShadow: '0 16px 34px rgba(0, 0, 0, 0.36)' }, heroIcon: { width: 130, height: 130 }, eyebrow: { color: authColors.purple, fontSize: 10, fontWeight: '800', letterSpacing: 1.5, alignSelf: 'stretch' }, title: { color: authColors.text, fontSize: 34, lineHeight: 39, fontWeight: '800', marginTop: 10, alignSelf: 'stretch' }, copy: { color: authColors.muted, fontSize: 14, lineHeight: 21, marginTop: 12, alignSelf: 'stretch' }, form: { marginTop: 28, gap: 12 }, field: { minHeight: 56, borderRadius: 16, backgroundColor: authColors.surface, borderWidth: 1, borderColor: authColors.border, paddingHorizontal: 16, flexDirection: 'row', alignItems: 'center', gap: 11 }, input: { flex: 1, color: authColors.text, fontSize: 15, minHeight: 52 }, error: { color: authColors.danger, fontSize: 12, fontWeight: '600', marginHorizontal: 4 }, submit: { minHeight: 55, borderRadius: 16, backgroundColor: authColors.purpleDeep, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, marginTop: 4, boxShadow: '0 10px 18px rgba(124, 92, 231, 0.35)' }, submitDisabled: { opacity: 0.65 }, submitText: { color: '#fff', fontSize: 15, fontWeight: '800' }, switchRow: { flexDirection: 'row', justifyContent: 'center', gap: 6, marginTop: 25 }, switchCopy: { color: authColors.muted, fontSize: 13 }, switchAction: { color: authColors.purple, fontSize: 13, fontWeight: '800' } });
