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
  collectionData,
  runTransaction,
} from '@angular/fire/firestore';

@Injectable({
  providedIn: 'root',
})
export class OrderApiService {
  private firestore = inject(Firestore);
  private col = collection(this.firestore, 'orders');

  getOrders(): Observable<any> {
    return collectionData(this.col, { idField: '_id' });
  }

  createOrder(data: any): Observable<any> {
    const { _id, ...rest } = data;
    const record = { ...rest, createdAt: rest.createdAt || new Date().toISOString() };
    return from(addDoc(this.col, record)).pipe(map((ref) => ({ _id: ref.id, ...record })));
  }

  updateOrderStatus(id: string, status: string, note: string = ''): Observable<any> {
    const orderRef = doc(this.firestore, 'orders', id);

    return from(runTransaction(this.firestore, async (transaction) => {
      const snap = await transaction.get(orderRef);
      if (!snap.exists()) throw new Error('Đơn hàng không tồn tại');

      const order = snap.data();
      const oldStatus = order['status'];
      const items = order['orderItems'] || order['items'] || [];

      // Logic hoàn tồn kho nếu huỷ đơn (chỉ hoàn nếu đơn chưa giao/huỷ trước đó)
      if (status === 'CANCELLED' && oldStatus !== 'CANCELLED') {
        for (const item of items) {
          if (item.productId) {
            const pRef = doc(this.firestore, 'products', item.productId);
            const pSnap = await transaction.get(pRef);
            if (pSnap.exists()) {
              const currentStock = pSnap.data()['stock'] || 0;
              transaction.update(pRef, { stock: currentStock + (item.quantity || 0) });
            }
          }
        }
      }

      const updateData: any = {
        status,
        updatedAt: new Date().toISOString()
      };

      if (status === 'DELIVERED') {
        updateData.isPaid = true;
        updateData.paidAt = new Date().toISOString();
      }

      // Ghi log lịch sử
      const logEntry = {
        status,
        time: new Date().toISOString(),
        note: note || `Chuyển trạng thái từ ${oldStatus} sang ${status}`
      };

      const history = order['history'] || [];
      history.push(logEntry);
      updateData.history = history;

      transaction.update(orderRef, updateData);
      return { _id: id, ...updateData };
    }));
  }

  cancelOrder(id: string, reason: string = 'Admin hủy'): Observable<any> {
    return this.updateOrderStatus(id, 'CANCELLED', reason);
  }

  shipOrder(id: string, trackingCode: string): Observable<any> {
    const shipping = {
      trackingCode,
      shippedAt: new Date().toISOString(),
      carrier: 'SPX Express'
    };

    const orderRef = doc(this.firestore, 'orders', id);
    return from(runTransaction(this.firestore, async (transaction) => {
      const snap = await transaction.get(orderRef);
      const order = snap.data();
      const history = order?.['history'] || [];
      history.push({ status: 'SHIPPING', time: new Date().toISOString(), note: `Gửi hàng - Mã: ${trackingCode}` });

      transaction.update(orderRef, {
        status: 'SHIPPING',
        shipping,
        history,
        updatedAt: new Date().toISOString()
      });
      return { _id: id, status: 'SHIPPING', shipping };
    }));
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
