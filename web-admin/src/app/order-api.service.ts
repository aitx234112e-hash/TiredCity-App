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

  updateOrderStatus(id: string, status: string, note: string = '', actor: string = 'Admin'): Observable<any> {
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
              const pData = pSnap.data();
              const qty = Number(item.quantity || item.qty || 0);
              const pUpdates: any = {
                stock: (pData['stock'] || 0) + qty
              };

              // Hoàn kho theo size
              const selectedSize = item.size || item.selected_size;
              if (selectedSize) {
                // 1. Kiểm tra trong các field 0, 1, 2... (Map size)
                let sizeFound = false;
                for (let i = 0; i <= 10; i++) {
                  const field = i.toString();
                  const sizeInfo = pData[field];
                  if (sizeInfo && typeof sizeInfo === 'object' && sizeInfo.size?.toString().toLowerCase() === selectedSize.toLowerCase()) {
                    pUpdates[field] = {
                      ...sizeInfo,
                      quantity: (Number(sizeInfo.quantity) || 0) + qty
                    };
                    sizeFound = true;
                    break;
                  }
                }

                // 2. Nếu không thấy, kiểm tra trong mảng "sizes"
                if (!sizeFound && Array.isArray(pData['sizes'])) {
                  pUpdates['sizes'] = pData['sizes'].map((s: any) => {
                    if (s.size?.toString().toLowerCase() === selectedSize.toLowerCase()) {
                      return { ...s, quantity: (Number(s.quantity) || 0) + qty };
                    }
                    return s;
                  });
                }
              }

              transaction.update(pRef, pUpdates);
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
        actor,
        note: note || `Chuyển trạng thái từ ${oldStatus} sang ${status}`
      };

      const history = order['history'] || [];
      history.push(logEntry);
      updateData.history = history;

      transaction.update(orderRef, updateData);
      return { _id: id, ...updateData };
    }));
  }

  cancelOrder(id: string, reason: string = 'Admin hủy', actor: string = 'Admin'): Observable<any> {
    return this.updateOrderStatus(id, 'CANCELLED', reason, actor);
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
