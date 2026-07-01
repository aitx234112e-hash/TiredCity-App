'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';

const ERRORS: Record<string, string> = {
  forbidden: 'Tài khoản này không có quyền truy cập Admin.',
  disabled: 'Tài khoản đã bị vô hiệu hóa.',
  noconfig: 'Chưa cấu hình Firebase. Điền khóa vào web-admin/.env.local rồi khởi động lại.',
};

export default function LoginPage() {
  const router = useRouter();
  const { me, loading, error, signInGoogle } = useAuth();

  useEffect(() => {
    if (me) router.replace('/dashboard');
  }, [me, router]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-brand-soft px-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow-lg">
        <h1 className="text-2xl font-bold text-brand">TiredCity Admin</h1>
        <p className="mt-1 text-sm text-gray-500">Đăng nhập để quản lý hệ thống</p>

        {error && (
          <div className="mt-4 rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {ERRORS[error] ?? 'Đăng nhập thất bại.'}
          </div>
        )}

        <button
          onClick={() => signInGoogle().catch((e) => alert(e?.message ?? 'Đăng nhập thất bại'))}
          disabled={loading}
          className="mt-6 flex w-full items-center justify-center gap-3 rounded-lg border border-gray-300 bg-white px-4 py-2.5 font-medium text-gray-700 transition hover:bg-gray-50 disabled:opacity-50"
        >
          <svg width="18" height="18" viewBox="0 0 24 24">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.27-4.74 3.27-8.1z" />
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84A11 11 0 0 0 12 23z" />
            <path fill="#FBBC05" d="M5.84 14.1a6.6 6.6 0 0 1 0-4.2V7.06H2.18a11 11 0 0 0 0 9.88l3.66-2.84z" />
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1A11 11 0 0 0 2.18 7.06l3.66 2.84C6.71 7.31 9.14 5.38 12 5.38z" />
          </svg>
          Đăng nhập với Google
        </button>

        <p className="mt-6 text-center text-xs text-gray-400">
          Chỉ tài khoản ADMIN / STAFF mới truy cập được.
        </p>
      </div>
    </div>
  );
}
