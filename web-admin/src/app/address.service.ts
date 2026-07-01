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
  query,
  where,
} from '@angular/fire/firestore';

@Injectable({
  providedIn: 'root',
})
export class AddressService {
  private firestore = inject(Firestore);
  private col = collection(this.firestore, 'addresses');

  /** Lấy địa chỉ theo user (trả về object đầu tiên hoặc null) */
  getAddressByUser(userId: any): Observable<any> {
    return from(getDocs(query(this.col, where('userId', '==', userId)))).pipe(
      map((snap) => {
        const list = snap.docs.map((d) => ({ _id: d.id, ...(d.data() as any) }));
        return list.length ? list[0] : null;
      })
    );
  }

  createAddress(data: any): Observable<any> {
    const { _id, ...rest } = data;
    return from(addDoc(this.col, rest)).pipe(map((ref) => ({ _id: ref.id, ...rest })));
  }

  updateAddress(id: any, data: any): Observable<any> {
    const { _id, ...rest } = data;
    return from(updateDoc(doc(this.firestore, 'addresses', id), rest)).pipe(
      map(() => ({ _id: id, ...rest }))
    );
  }

  deleteAddress(id: any): Observable<any> {
    return from(deleteDoc(doc(this.firestore, 'addresses', id)));
  }

  // Format an address value into the string: "address, ward, city"
  // Accepts either a string or an object with { address, ward, city }.
  formatAddress(addr: any): string {
    if (!addr && addr !== '') return '';
    if (typeof addr === 'string') return addr;
    try {
      const parts: string[] = [];
      const a = (addr.address || addr.addr || addr.street || '').toString().trim();
      const w = (addr.ward || addr.wards || '').toString().trim();
      const c = (addr.city || addr.province || '').toString().trim();
      if (a) parts.push(a);
      if (w) parts.push(w);
      if (c) parts.push(c);
      return parts.join(', ');
    } catch (e) {
      return '';
    }
  }
}
