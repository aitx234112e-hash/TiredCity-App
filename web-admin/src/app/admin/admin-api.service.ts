import { Injectable, inject } from '@angular/core';
import { Observable, from, map } from 'rxjs';
import { Firestore, collection, getDocs } from '@angular/fire/firestore';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private firestore = inject(Firestore);

  private count(name: string): Observable<number> {
    return from(getDocs(collection(this.firestore, name))).pipe(map((s) => s.size));
  }

  private ordersData(): Observable<any[]> {
    return from(getDocs(collection(this.firestore, 'orders'))).pipe(
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
}
