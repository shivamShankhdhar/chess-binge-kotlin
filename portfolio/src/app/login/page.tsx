'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

export default function LoginPage() {
  const [step, setStep] = useState<'email' | 'otp'>('email');
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [devOtp, setDevOtp] = useState(''); // For development
  const router = useRouter();

  const handleSendOTP = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await fetch('/api/auth', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, action: 'sendOTP' }),
      });

      const data = await response.json();

      if (!response.ok) throw new Error(data.error);

      if (data.otp) setDevOtp(data.otp); // For development
      setStep('otp');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to send OTP');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOTP = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await fetch('/api/auth', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, otp, action: 'verifyOTP' }),
      });

      const data = await response.json();

      if (!response.ok) throw new Error(data.error);

      localStorage.setItem('adminToken', data.token);
      localStorage.setItem('adminEmail', email);
      router.push('/admin');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid OTP');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 to-slate-800 px-4">
      <div className="w-full max-w-md">
        <div className="bg-white dark:bg-slate-950 rounded-2xl shadow-2xl p-8 border border-orange-500/20">
          <div className="mb-8 text-center">
            <div className="inline-flex h-16 w-16 items-center justify-center rounded-xl bg-gradient-to-br from-orange-500 to-orange-600 mb-4">
              <span className="text-2xl font-bold text-white">SS</span>
            </div>
            <h1 className="text-3xl font-bold text-slate-900 dark:text-white">Admin Login</h1>
            <p className="mt-2 text-slate-600 dark:text-slate-400">Shivam's Portfolio</p>
          </div>

          {error && (
            <div className="mb-4 rounded-lg bg-red-100 dark:bg-red-900/30 p-4 text-sm text-red-700 dark:text-red-300">
              {error}
            </div>
          )}

          {step === 'email' ? (
            <form onSubmit={handleSendOTP} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                  Email Address
                </label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="w-full px-4 py-3 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
                  placeholder="your@email.com"
                />
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full px-4 py-3 rounded-lg bg-gradient-to-r from-orange-500 to-orange-600 text-white font-semibold transition-all hover:shadow-lg disabled:opacity-50"
              >
                {loading ? 'Sending OTP...' : 'Send OTP'}
              </button>
            </form>
          ) : (
            <form onSubmit={handleVerifyOTP} className="space-y-4">
              <p className="text-sm text-slate-600 dark:text-slate-400 text-center">
                Enter the 6-digit OTP sent to <strong>{email}</strong>
              </p>

              {devOtp && (
                <div className="rounded-lg bg-blue-100 dark:bg-blue-900/30 p-3 text-center">
                  <p className="text-xs text-blue-600 dark:text-blue-300">Development OTP: <strong>{devOtp}</strong></p>
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                  Enter OTP
                </label>
                <input
                  type="text"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  required
                  maxLength={6}
                  className="w-full px-4 py-3 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-center text-2xl tracking-widest focus:outline-none focus:ring-2 focus:ring-orange-500"
                  placeholder="000000"
                />
              </div>

              <button
                type="submit"
                disabled={loading || otp.length !== 6}
                className="w-full px-4 py-3 rounded-lg bg-gradient-to-r from-orange-500 to-orange-600 text-white font-semibold transition-all hover:shadow-lg disabled:opacity-50"
              >
                {loading ? 'Verifying...' : 'Verify OTP'}
              </button>

              <button
                type="button"
                onClick={() => {
                  setStep('email');
                  setOtp('');
                  setError('');
                }}
                className="w-full px-4 py-2 rounded-lg border-2 border-slate-300 dark:border-slate-600 text-slate-700 dark:text-slate-300 font-semibold transition-all hover:bg-slate-100 dark:hover:bg-slate-900"
              >
                Change Email
              </button>
            </form>
          )}

          <div className="mt-6 text-center">
            <Link
              href="/"
              className="text-sm text-orange-600 dark:text-orange-400 hover:underline"
            >
              Back to Portfolio
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
