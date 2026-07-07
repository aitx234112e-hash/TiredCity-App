import { Injectable, inject, NgZone, Injector, runInInjectionContext } from '@angular/core';
import { Observable, from, map } from 'rxjs';
import { Firestore, collection, collectionData, query, orderBy, limit, doc, addDoc, updateDoc, deleteDoc, getDoc, setDoc, onSnapshot } from '@angular/fire/firestore';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private firestore = inject(Firestore);
  private zone = inject(NgZone);
  private injector = inject(Injector);

  private inCtx<T>(fn: () => Promise<T>): Observable<T> {
    return from(runInInjectionContext(this.injector, fn));
  }

  /** Luồng dữ liệu đơn hàng thời gian thực */
  getOrders(): Observable<any[]> {
    const col = collection(this.firestore, 'orders');
    return collectionData(col, { idField: '_id' }).pipe(map(list => list || []));
  }

  getOrdersRealtime(callback: (data: any[]) => void) {
    return onSnapshot(collection(this.firestore, 'orders'), (snap) => {
      this.zone.run(() => {
        const list = snap.docs.map(d => ({ _id: d.id, ...d.data() }));
        callback(list);
      });
    });
  }

  getCollectionCount(path: string, callback: (count: number) => void) {
    return onSnapshot(collection(this.firestore, path), (snap) => {
      this.zone.run(() => callback(snap.size));
    });
  }

  getUserCount(): Observable<{ count: number }> {
    return collectionData(collection(this.firestore, 'users')).pipe(map(s => ({ count: s.length })));
  }

  getOrderCount(): Observable<{ count: number }> {
    return this.getOrders().pipe(map(s => ({ count: s.length })));
  }

  getProductCount(): Observable<{ count: number }> {
    return collectionData(collection(this.firestore, 'products')).pipe(map(s => ({ count: s.length })));
  }

  getRevenue(): Observable<{ revenue: number }> {
    return this.getOrders().pipe(
      map(orders => ({
        revenue: orders
          .filter(o => o.status === 'DELIVERED' || o.isPaid)
          .reduce((sum, o) => sum + (Number(o.totalPrice || 0)), 0)
      }))
    );
  }

  getActivities(): Observable<Array<any>> {
    return this.getOrders().pipe(
      map(list => list.slice(0, 6).map(o => ({
        timestamp: o.createdAt || new Date().toISOString(),
        description: `Đơn hàng ${o.orderCode || o._id} — Trạng thái: ${o.status}`
      })))
    );
  }

  getRecentOrders(): Observable<Array<any>> {
    const q = query(collection(this.firestore, 'orders'), orderBy('createdAt', 'desc'), limit(8));
    return collectionData(q, { idField: '_id' });
  }

  getRecentOrdersRealtime(callback: (data: any[]) => void) {
    const q = query(collection(this.firestore, 'orders'), orderBy('createdAt', 'desc'), limit(8));
    return onSnapshot(q, (snap) => {
      this.zone.run(() => {
        const list = snap.docs.map(d => ({ _id: d.id, ...d.data() }));
        callback(list);
      });
    });
  }

  /** Khôi phục các hàm lấy danh sách User và Product cho Dashboard */
  getUsers(): Observable<any[]> {
    return collectionData(collection(this.firestore, 'users'), { idField: '_id' });
  }

  getProducts(): Observable<any[]> {
    return collectionData(collection(this.firestore, 'products'), { idField: '_id' });
  }

  // --- Khôi phục các hàm bị thiếu ---
  getEvents(): Observable<any[]> {
    return collectionData(collection(this.firestore, 'events'), { idField: '_id' });
  }
  addEvent(data: any): Observable<string> {
    return from(addDoc(collection(this.firestore, 'events'), data)).pipe(map(ref => ref.id));
  }
  updateEvent(id: string, data: any): Observable<void> {
    return from(updateDoc(doc(this.firestore, 'events', id), data));
  }
  deleteEvent(id: string): Observable<void> {
    return from(deleteDoc(doc(this.firestore, 'events', id)));
  }
  getVouchers(): Observable<any[]> {
    return collectionData(collection(this.firestore, 'vouchers'), { idField: '_id' });
  }
  addVoucher(data: any): Observable<string> {
    return from(addDoc(collection(this.firestore, 'vouchers'), data)).pipe(map(ref => ref.id));
  }
  updateVoucher(id: string, data: any): Observable<void> {
    return from(updateDoc(doc(this.firestore, 'vouchers', id), data));
  }
  deleteVoucher(id: string): Observable<void> {
    return from(deleteDoc(doc(this.firestore, 'vouchers', id)));
  }
  getShippingConfigs(): Observable<any[]> {
    return collectionData(collection(this.firestore, 'shipping_configs'), { idField: '_id' });
  }
  saveShippingConfig(id: string, data: any): Observable<void> {
    return from(setDoc(doc(this.firestore, 'shipping_configs', id), data, { merge: true }));
  }
  getShippingSettings(): Observable<any | null> {
    return from(getDoc(doc(this.firestore, 'shipping_settings', 'general'))).pipe(map(s => s.exists() ? s.data() : null));
  }
  saveShippingSettings(data: any): Observable<void> {
    return from(setDoc(doc(this.firestore, 'shipping_settings', 'general'), data, { merge: true }));
  }
  uploadImage(file: File): Observable<{ imageUrl: string }> {
    return new Observable((observer) => {
      const reader = new FileReader();
      reader.onload = () => { observer.next({ imageUrl: reader.result as string }); observer.complete(); };
      reader.readAsDataURL(file);
    });
  }
}
