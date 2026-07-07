import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, from, of, Observable, tap, map, switchMap } from 'rxjs';
import {
  Auth,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut,
  sendPasswordResetEmail,
} from '@angular/fire/auth';
import {
  Firestore,
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  addDoc,
  updateDoc,
  deleteDoc,
  collectionData,
} from '@angular/fire/firestore';
import { Account } from '../app/models/Account';

@Injectable({
  providedIn: 'root',
})
export class UserApiService {
  private auth = inject(Auth);
  private firestore = inject(Firestore);
  private col = collection(this.firestore, 'users');

  // ✅ State quản lý user
  private currentUser = new BehaviorSubject<Account | null>(null);
  currentUser$ = this.currentUser.asObservable();

  // =========================
  // AUTH
  // =========================

  /** Đăng nhập bằng Firebase Auth, lấy hồ sơ (role...) từ Firestore users/{uid} */
  login(data: any): Observable<any> {
    return from(signInWithEmailAndPassword(this.auth, data.email, data.password)).pipe(
      switchMap((cred) =>
        from(getDoc(doc(this.firestore, 'users', cred.user.uid))).pipe(
          map((snap) => {
            const profile: any = snap.exists() ? snap.data() : {};
            return {
              _id: cred.user.uid,
              email: cred.user.email,
              role: profile.role || 'user',
              ...profile,
            };
          })
        )
      ),
      tap((user: any) => {
        if (user?._id) this.setUser(user);
      })
    );
  }

  /** Đăng ký: tạo tài khoản Auth + hồ sơ Firestore. `data` cần có email & password */
  register(data: any): Observable<any> {
    return from(createUserWithEmailAndPassword(this.auth, data.email, data.password)).pipe(
      switchMap((cred) => {
        const { password, ...profile } = data;
        const record = { ...profile, role: profile.role || 'user' };
        return from(setDoc(doc(this.firestore, 'users', cred.user.uid), record)).pipe(
          map(() => ({ _id: cred.user.uid, ...record }))
        );
      })
    );
  }

  requestPasswordReset(email: string): Observable<{ message: string }> {
    return from(sendPasswordResetEmail(this.auth, email)).pipe(
      map(() => ({ message: 'Đã gửi email đặt lại mật khẩu' }))
    );
  }

  /** Firebase xử lý reset qua email link — giữ chữ ký cho tương thích */
  resetPassword(_token: string, _newPassword: string): Observable<{ message: string }> {
    return of({ message: 'Vui lòng đặt lại mật khẩu qua liên kết trong email' });
  }

  logout() {
    signOut(this.auth).catch(() => {});
    this.currentUser.next(null);
    if (typeof window !== 'undefined') {
      localStorage.removeItem('user');
    }
  }

  // =========================
  // USER API (Firestore)
  // =========================

  getUser(id: string): Observable<Account> {
    return from(getDoc(doc(this.firestore, 'users', id))).pipe(
      map((snap) => ({ _id: snap.id, ...(snap.data() as any) }) as Account)
    );
  }

  updateUser(id: string, data: any): Observable<Account> {
    const { _id, ...rest } = data;
    return from(updateDoc(doc(this.firestore, 'users', id), rest)).pipe(
      map(() => ({ _id: id, ...rest }) as Account),
      tap((updated) => {
          // Chỉ cập nhật currentUser nếu UID khớp với user đang đăng nhập
          if (this.currentUser.value && this.currentUser.value._id === id) {
              this.setUser(updated);
          }
      })
    );
  }

  getCurrentUser(): Observable<Account | null> {
    return this.currentUser$;
  }

  loadCurrentUser() {
    const u = this.auth.currentUser;
    if (!u) {
      this.currentUser.next(null);
      return;
    }
    this.getUser(u.uid).subscribe({
      next: (user) => this.setUser(user),
      error: () => this.currentUser.next(null),
    });
  }

  setUser(user: Account) {
    this.currentUser.next(user);
    if (typeof window !== 'undefined') {
      localStorage.setItem('user', JSON.stringify(user));
    }
  }

  /** fallback load từ local (khi reload trang) */
  loadUserFromLocal() {
    if (typeof window === 'undefined') return;
    const userRaw = localStorage.getItem('user');
    if (!userRaw) return;
    try {
      this.currentUser.next(JSON.parse(userRaw));
    } catch {
      this.currentUser.next(null);
    }
  }

  getUsers(): Observable<Account[]> {
    return collectionData(this.col, { idField: '_id' }).pipe(
      map(list => (list || []) as Account[])
    );
  }

  /** Thêm hồ sơ user vào Firestore (không tạo tài khoản Auth — dùng register() nếu cần đăng nhập) */
  addUser(data: any): Observable<Account> {
    const { _id, ...rest } = data;
    return from(addDoc(this.col, rest)).pipe(
      map((ref) => ({ _id: ref.id, ...rest }) as Account)
    );
  }

  deleteUser(id: string): Observable<void> {
    return from(deleteDoc(doc(this.firestore, 'users', id)));
  }
}
