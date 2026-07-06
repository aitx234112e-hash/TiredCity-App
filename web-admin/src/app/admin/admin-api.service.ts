import { Injectable, inject, Injector, runInInjectionContext } from '@angular/core';
import { Observable, from, map } from 'rxjs';
import { Firestore, collection, getDocs, getDoc, addDoc, deleteDoc, updateDoc, doc, setDoc } from '@angular/fire/firestore';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private firestore = inject(Firestore);
  private injector = inject(Injector);

  /** Chạy lời gọi Firestore trong injection context để tránh destabilize change-detection */
  private inCtx<T>(fn: () => Promise<T>): Observable<T> {
    return from(runInInjectionContext(this.injector, fn));
  }

  private count(name: string): Observable<number> {
    return this.inCtx(() => getDocs(collection(this.firestore, name))).pipe(map((s) => s.size));
  }

  private ordersData(): Observable<any[]> {
    return this.inCtx(() => getDocs(collection(this.firestore, 'orders'))).pipe(
      map((snap) => snap.docs.map((d) => ({ _id: d.id, ...(d.data() as any) })))
    );
  }

  getUserCount(): Observable<{ count: number }> {
    return this.count('users').pipe(map((count) => ({ count })));
  }

  getOrderCount(): Observable<{ count: number }> {
    return this.count('orders').pipe(map((count) => ({ count })));
  }

  getProductCount(): Observable<{ count: number }> {
    return this.count('products').pipe(map((count) => ({ count })));
  }

  getRevenue(): Observable<{ revenue: number }> {
    return this.ordersData().pipe(
      map((orders) => ({
        revenue: orders.reduce(
          (sum, o) => sum + (Number(o.total ?? o.totalPrice ?? o.amount ?? 0) || 0),
          0
        ),
      }))
    );
  }

  getActivities(): Observable<Array<any>> {
    return this.ordersData().pipe(
      map((list) =>
        list.slice(0, 6).map((o) => ({
          timestamp: o.createdAt || o.created_at || new Date(),
          description: `Order ${o._id || ''} status: ${o.status || '—'}`,
        }))
      )
    );
  }

  getRecentOrders(): Observable<Array<any>> {
    return this.ordersData().pipe(map((list) => list.slice(0, 8)));
  }

  /** Toàn bộ đơn hàng (cho trang Doanh thu) */
  getOrders(): Observable<any[]> {
    return this.ordersData();
  }

  /** Danh sách sự kiện từ collection 'events' */
  getEvents(): Observable<any[]> {
    return this.inCtx(() => getDocs(collection(this.firestore, 'events'))).pipe(
      map((snap) => snap.docs.map((d) => ({ _id: d.id, ...(d.data() as any) })))
    );
  }

  addEvent(data: any): Observable<string> {
    return this.inCtx(() => addDoc(collection(this.firestore, 'events'), data)).pipe(
      map((ref) => ref.id)
    );
  }

  deleteEvent(id: string): Observable<void> {
    return this.inCtx(() => deleteDoc(doc(this.firestore, 'events', id)));
  }

  /** Voucher / mã giảm giá — collection 'vouchers' */
  getVouchers(): Observable<any[]> {
    return this.inCtx(() => getDocs(collection(this.firestore, 'vouchers'))).pipe(
      map((snap) => snap.docs.map((d) => ({ _id: d.id, ...(d.data() as any) })))
    );
  }

  addVoucher(data: any): Observable<string> {
    return this.inCtx(() => addDoc(collection(this.firestore, 'vouchers'), data)).pipe(
      map((ref) => ref.id)
    );
  }

  updateVoucher(id: string, data: any): Observable<void> {
    return this.inCtx(() => updateDoc(doc(this.firestore, 'vouchers', id), data));
  }

  deleteVoucher(id: string): Observable<void> {
    return this.inCtx(() => deleteDoc(doc(this.firestore, 'vouchers', id)));
  }

  /** Chưa dùng Storage — mã hoá ảnh thành data URL (base64) lưu trực tiếp vào Firestore */
  uploadImage(file: File): Observable<{ imageUrl: string }> {
    return new Observable((observer) => {
      const reader = new FileReader();
      reader.onload = () => {
        observer.next({ imageUrl: reader.result as string });
        observer.complete();
      };
      reader.onerror = (err) => observer.error(err);
      reader.readAsDataURL(file);
    });
  }

  /** Phương thức vận chuyển — collection 'shipping' */
  getShippingMethods(): Observable<any[]> {
    return this.inCtx(() => getDocs(collection(this.firestore, 'shipping'))).pipe(
      map((snap) => snap.docs.map((d) => ({ _id: d.id, ...(d.data() as any) })))
    );
  }

  addShippingMethod(data: any): Observable<string> {
    return this.inCtx(() => addDoc(collection(this.firestore, 'shipping'), data)).pipe(
      map((ref) => ref.id)
    );
  }

  updateShippingMethod(id: string, data: any): Observable<void> {
    return this.inCtx(() => updateDoc(doc(this.firestore, 'shipping', id), data));
  }

  deleteShippingMethod(id: string): Observable<void> {
    return this.inCtx(() => deleteDoc(doc(this.firestore, 'shipping', id)));
  }

  /** Cấu hình SPX Express — collection 'shipping_configs', 3 doc id cố định: economy / standard / express */
  getShippingConfigs(): Observable<any[]> {
    return this.inCtx(() => getDocs(collection(this.firestore, 'shipping_configs'))).pipe(
      map((snap) => snap.docs.map((d) => ({ _id: d.id, ...(d.data() as any) })))
    );
  }

  /** Ghi đè 1 gói cước theo id cố định (merge để không xoá trường khác). */
  saveShippingConfig(id: string, data: any): Observable<void> {
    return this.inCtx(() => setDoc(doc(this.firestore, 'shipping_configs', id), data, { merge: true }));
  }

  /** Thiết lập chung vận chuyển — doc cố định 'shipping_settings/general' (kho lấy hàng, freeship, ghi chú shipper). */
  getShippingSettings(): Observable<any | null> {
    return this.inCtx(() => getDoc(doc(this.firestore, 'shipping_settings', 'general'))).pipe(
      map((snap) => (snap.exists() ? { ...(snap.data() as any) } : null))
    );
  }

  saveShippingSettings(data: any): Observable<void> {
    return this.inCtx(() => setDoc(doc(this.firestore, 'shipping_settings', 'general'), data, { merge: true }));
  }
}
