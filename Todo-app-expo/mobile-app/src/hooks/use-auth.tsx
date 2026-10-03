import AsyncStorage from '@react-native-async-storage/async-storage';
import * as SecureStore from 'expo-secure-store';
import { createContext, PropsWithChildren, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { Platform } from 'react-native';

import { authApi, AuthUser, isTaskApiConfigured, setAccessToken, setUnauthorizedHandler } from '@/lib/tasks-api';

const SESSION_KEY = 'focus-auth-session';
type StoredSession = { token: string; user: AuthUser };
type AuthStore = {
  isLoading: boolean;
  user: AuthUser | null;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (name: string, email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
  updateProfile: (name: string) => Promise<void>;
  deleteAccount: () => Promise<void>;
};

const AuthContext = createContext<AuthStore | null>(null);

async function readSession() {
  return Platform.OS === 'web' ? AsyncStorage.getItem(SESSION_KEY) : SecureStore.getItemAsync(SESSION_KEY);
}

async function saveSession(session: StoredSession) {
  const value = JSON.stringify(session);
  if (Platform.OS === 'web') await AsyncStorage.setItem(SESSION_KEY, value);
  else await SecureStore.setItemAsync(SESSION_KEY, value);
}

async function removeSession() {
  if (Platform.OS === 'web') await AsyncStorage.removeItem(SESSION_KEY);
  else await SecureStore.deleteItemAsync(SESSION_KEY);
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [isLoading, setIsLoading] = useState(true);
  const [user, setUser] = useState<AuthUser | null>(null);

  useEffect(() => {
    void (async () => {
      try {
        const value = await readSession();
        if (!value) return;
        const session = JSON.parse(value) as StoredSession;
        if (!session.token || !session.user?.id) return;
        setAccessToken(session.token);
        const response = await authApi.me();
        setUser(response.user);
        await saveSession({ token: session.token, user: response.user });
      } catch {
        setAccessToken(null);
        await removeSession();
      } finally {
        setIsLoading(false);
      }
    })();
  }, []);

  const completeAuth = useCallback(async (session: StoredSession) => {
    setAccessToken(session.token);
    await saveSession(session);
    setUser(session.user);
  }, []);

  const signIn = useCallback(async (email: string, password: string) => completeAuth(await authApi.signIn(email, password)), [completeAuth]);
  const signUp = useCallback(async (name: string, email: string, password: string) => completeAuth(await authApi.signUp(name, email, password)), [completeAuth]);
  const signOut = useCallback(async () => {
    setAccessToken(null);
    setUser(null);
    await removeSession();
  }, []);
  const updateProfile = useCallback(async (name: string) => {
    const nextName = name.trim();
    if (!isTaskApiConfigured) {
      setUser((current) => current ? { ...current, name: nextName } : current);
      const value = await readSession();
      if (value) {
        const session = JSON.parse(value) as StoredSession;
        await saveSession({ token: session.token, user: { ...session.user, name: nextName } });
      }
      return;
    }
    const response = await authApi.updateProfile({ name: nextName });
    setUser(response.user);
    const value = await readSession();
    if (value) {
      const session = JSON.parse(value) as StoredSession;
      await saveSession({ token: session.token, user: response.user });
    }
  }, []);
  const deleteAccount = useCallback(async () => {
    if (isTaskApiConfigured) await authApi.deleteAccount();
    await signOut();
  }, [signOut]);

  useEffect(() => {
    setUnauthorizedHandler(() => { void signOut(); });
    return () => setUnauthorizedHandler(null);
  }, [signOut]);

  const value = useMemo(() => ({ isLoading, user, signIn, signUp, signOut, updateProfile, deleteAccount }), [isLoading, user, signIn, signUp, signOut, updateProfile, deleteAccount]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const store = useContext(AuthContext);
  if (!store) throw new Error('useAuth must be used inside AuthProvider');
  return store;
}
