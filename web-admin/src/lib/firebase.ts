// Khoi tao Firebase cho web-admin.
// Cac gia tri NEXT_PUBLIC_* lay tu Firebase Console (Project settings -> Your apps -> Web).
import { initializeApp, getApps, getApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import { getStorage } from 'firebase/storage';

const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID,
};

// Da cau hinh chua? (tranh crash SSR khi .env.local con trong)
export const firebaseReady = !!firebaseConfig.apiKey;

// Tranh khoi tao trung khi Next.js hot-reload; chi init khi da co config
const app = firebaseReady
  ? (getApps().length ? getApp() : initializeApp(firebaseConfig))
  : undefined;

// Khi chua co config, export undefined (ep kieu) — code chi chay sau khi dang nhap,
// ma dang nhap thi da yeu cau firebaseReady === true.
export const auth = (app ? getAuth(app) : undefined) as ReturnType<typeof getAuth>;
export const db = (app ? getFirestore(app) : undefined) as ReturnType<typeof getFirestore>;
export const storage = (app ? getStorage(app) : undefined) as ReturnType<typeof getStorage>;
export const googleProvider = new GoogleAuthProvider();

// Danh sach email duoc coi la ADMIN khi dang nhap lan dau (bootstrap).
// Vd: NEXT_PUBLIC_ADMIN_EMAILS="a@gmail.com,b@gmail.com"
export const ADMIN_EMAILS = (process.env.NEXT_PUBLIC_ADMIN_EMAILS ?? '')
  .split(',')
  .map((e) => e.trim().toLowerCase())
  .filter(Boolean);
