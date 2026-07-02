import { Injectable, inject } from '@angular/core';
import { Observable, from, map } from 'rxjs';
import {
  Firestore,
  collection,
  doc,
  getDocs,
  addDoc,
  updateDoc,
  deleteDoc,
} from '@angular/fire/firestore';

@Injectable({
  providedIn: 'root',
})
export class OrderApiService {
  private firestore = inject(Firestore);
  private col = collection(this.firestore, 'orders');

  getOrders(): Observable<any> {
    return from(getDocs(this.col)).pipe(
      map((snap) => snap.docs.map((d) => ({ _id: d.id, ...(d.data() as any) })))
    );
  }

  createOrder(data: any): Observable<any> {
    const { _id, ...rest } = data;
    const record = { ...rest, createdAt: rest.createdAt || new Date().toISOString() };
    return from(addDoc(this.col, record)).pipe(map((ref) => ({ _id: ref.id, ...record })));
  }

  updateOrderStatus(id: string, status: string): Observable<any> {
    return from(updateDoc(doc(this.firestore, 'orders', id), { status })).pipe(
      map(() => ({ _id: id, status }))
    );
  }

  cancelOrder(id: string): Observable<any> {
    return from(updateDoc(doc(this.firestore, 'orders', id), { status: 'cancelled' })).pipe(
      map(() => ({ _id: id, status: 'cancelled' }))
    );
  }

  shipOrder(id: string, trackingCode: string): Observable<any> {
    return from(
      updateDoc(doc(this.firestore, 'orders', id), { status: 'shipped', trackingCode })
    ).pipe(map(() => ({ _id: id, status: 'shipped', trackingCode })));
  }

  deleteOrder(id: string): Observable<any> {
    return from(deleteDoc(doc(this.firestore, 'orders', id)));
  }

  updateShipping(id: string, shippingData: any): Observable<any> {
    const { _id, ...rest } = shippingData;
    return from(updateDoc(doc(this.firestore, 'orders', id), rest)).pipe(
      map(() => ({ _id: id, ...rest }))
    );
  }
}
