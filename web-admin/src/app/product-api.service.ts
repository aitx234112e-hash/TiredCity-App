import { Injectable, inject } from '@angular/core';
import { from, map, Observable } from 'rxjs';
import {
  Firestore,
  collection,
  doc,
  addDoc,
  updateDoc,
  deleteDoc,
  collectionData,
} from '@angular/fire/firestore';
import { Product } from './models/product';

@Injectable({
  providedIn: 'root',
})
export class ProductApiService {
  private firestore = inject(Firestore);
  private col = collection(this.firestore, 'products');

  /** Lấy danh sách sản phẩm thời gian thực */
  getProducts(): Observable<Product[]> {
    return collectionData(this.col, { idField: '_id' }).pipe(
      map(list => (list || []) as Product[])
    );
  }

  addProduct(data: any): Observable<any> {
    const { _id, ...rest } = data;
    return from(addDoc(this.col, rest)).pipe(map((ref) => ({ _id: ref.id, ...rest })));
  }

  updateProduct(id: string, data: any): Observable<any> {
    const { _id, ...rest } = data;
    return from(updateDoc(doc(this.firestore, 'products', id), rest)).pipe(
      map(() => ({ _id: id, ...rest }))
    );
  }

  deleteProduct(id: string): Observable<void> {
    return from(deleteDoc(doc(this.firestore, 'products', id)));
  }

  uploadImage(file: File): Observable<{ fileName: string; imageUrl: string }> {
    return new Observable((observer) => {
      const reader = new FileReader();
      reader.onload = () => { observer.next({ fileName: file.name, imageUrl: reader.result as string }); observer.complete(); };
      reader.readAsDataURL(file);
    });
  }
}
