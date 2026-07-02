import { Injectable, inject, Injector, runInInjectionContext } from '@angular/core';
import { Observable, from, map, of, catchError } from 'rxjs';
import { Firestore, collection, getDocs, addDoc } from '@angular/fire/firestore';

/** Ghi & đọc nhật ký hoạt động (collection 'auditLogs') */
@Injectable({ providedIn: 'root' })
export class AuditService {
  private firestore = inject(Firestore);
  private injector = inject(Injector);

  private inCtx<T>(fn: () => Promise<T>): Observable<T> {
    return from(runInInjectionContext(this.injector, fn));
  }

  private currentActor(): string {
    if (typeof window === 'undefined') return 'system';
    try {
      const raw = localStorage.getItem('user');
      if (!raw) return 'unknown';
      const u = JSON.parse(raw);
      return u.profileName || u.email || u._id || 'admin';
    } catch {
      return 'admin';
    }
  }

  /**
   * Ghi 1 log. Không chặn luồng chính: nếu lỗi thì nuốt lỗi (subscribe an toàn).
   * @param action mã hành động, VD: 'login', 'order.status', 'voucher.create'
   * @param target đối tượng bị tác động (mã đơn / tên voucher / user...)
   * @param detail mô tả thêm
   */
  log(action: string, target: string = '', detail: string = ''): void {
    const entry = {
      action,
      target,
      detail,
      actor: this.currentActor(),
      timestamp: new Date().toISOString(),
    };
    this.inCtx(() => addDoc(collection(this.firestore, 'auditLogs'), entry))
      .pipe(catchError(() => of(null)))
      .subscribe();
  }

  getLogs(): Observable<any[]> {
    return this.inCtx(() => getDocs(collection(this.firestore, 'auditLogs'))).pipe(
      map((snap) =>
        snap.docs
          .map((d) => ({ _id: d.id, ...(d.data() as any) }))
          .sort((a, b) => String(b.timestamp || '').localeCompare(String(a.timestamp || '')))
      ),
      catchError(() => of([]))
    );
  }
}
