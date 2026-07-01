'use client';

import { createContext, useContext, useEffect, useState } from 'react';
import {
  onAuthStateChanged, signInWithPopup, signOut as fbSignOut, type User,
} from 'firebase/auth';
import { doc, getDoc, setDoc, Timestamp } from 'firebase/firestore';
import { auth, db, googleProvider, ADMIN_EMAILS, firebaseReady } from '@/lib/firebase';
import type { Me, Role } from '@/lib/types';

type AuthState = {
  me: Me | null;
  loading: boolean;
  error: string | null;         // 'forbidden' | 'disabled' | ...
  signInGoogle: () => Promise<void>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

// Chi cac vai tro nay duoc vao trang admin
const ALLOWED: Role[] = ['ADMIN', 'STAFF', 'AUDITOR'];

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!firebaseReady) {
      setError('noconfig');
      setLoading(false);
      return;
    }
    return onAuthStateChanged(auth, async (user: User | null) => {
      setLoading(true);
      setError(null);
      if (!user) {
        setMe(null);
        setLoading(false);
        return;
      }
      try {
        const ref = doc(db, 'users', user.uid);
        const snap = await getDoc(ref);
        let data: any;
        if (!snap.exists()) {
          // Lan dau dang nhap -> tao ho so. Email trong ADMIN_EMAILS => ADMIN.
          const isBootstrapAdmin = ADMIN_EMAILS.includes((user.email ?? '').toLowerCase());
          data = {
            email: user.email ?? '',
            fullName: user.displayName ?? '',
            avatarUrl: user.photoURL ?? '',
            role: (isBootstrapAdmin ? 'ADMIN' : 'CUSTOMER') as Role,
            isActive: true,
            createdAt: Timestamp.now(),
          };
          await setDoc(ref, data);
        } else {
          data = snap.data();
        }

        if (data.isActive === false) {
          await fbSignOut(auth);
          setMe(null);
          setError('disabled');
        } else if (!ALLOWED.includes(data.role)) {
          await fbSignOut(auth);
          setMe(null);
          setError('forbidden');
        } else {
          setMe({
            id: user.uid,
            email: data.email,
            fullName: data.fullName,
            avatarUrl: data.avatarUrl,
            role: data.role,
            menh: data.menh,
          });
        }
      } catch (e: any) {
        setError(e?.message ?? 'error');
        setMe(null);
      } finally {
        setLoading(false);
      }
    });
  }, []);

  const signInGoogle = async () => {
    if (!firebaseReady) {
      setError('noconfig');
      return;
    }
    setError(null);
    await signInWithPopup(auth, googleProvider);
    // onAuthStateChanged se xu ly phan con lai
  };

  const signOut = async () => {
    await fbSignOut(auth);
    setMe(null);
  };

  return (
    <AuthContext.Provider value={{ me, loading, error, signInGoogle, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth phai dung trong <AuthProvider>');
  return ctx;
}
