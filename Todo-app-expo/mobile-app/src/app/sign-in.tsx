import { useRouter } from 'expo-router';

import { AuthForm } from '@/components/auth-form';

export default function SignInScreen() {
  const router = useRouter();
  return <AuthForm mode="sign-in" onSwitch={() => router.replace('/sign-up')} />;
}
