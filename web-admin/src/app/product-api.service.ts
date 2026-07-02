import { Injectable, inject } from '@angular/core';
import { Observable, BehaviorSubject, from, map } from 'rxjs';
import {
  Firestore,
  collection,
  doc,
  getDoc,
  getDocs,
  addDoc,
  updateDoc,
  deleteDoc,
  query,
  where,
} from '@angular/fire/firestore';
import { Product } from './models/product';

@Injectable({
  providedIn: 'root',
})
export class ProductApiService {
  private firestore = inject(Firestore);
  private col = collection(this.firestore, 'products');

  private currentProduct = new BehaviorSubject<any>(null);
  currentProduct$ = this.currentProduct.asObservable();

  getProducts(): Observable<Product[]> {
    return from(getDocs(this.col)).pipe(
      map((snap) => snap.docs.map((d) => ({ _id: d.id, ...(d.data() as any) }) as Product))
    );
  }

  getProduct(id: string): Observable<Product> {
    return from(getDoc(doc(this.firestore, 'products', id))).pipe(
      map((snap) => ({ _id: snap.id, ...(snap.data() as any) }) as Product)
    );
  }

  getProductsByCategory(category: string): Observable<Product[]> {
    return from(getDocs(query(this.col, where('product_dept', '==', category)))).pipe(
      map((snap) => snap.docs.map((d) => ({ _id: d.id, ...(d.data() as any) }) as Product))
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

  /**
   * Chưa dùng Firebase Storage — mã hoá ảnh thành data URL (base64) lưu trực tiếp vào Firestore.
   * Nếu cần Storage thật, thay bằng uploadBytes + getDownloadURL của @angular/fire/storage.
   */
  uploadImage(file: File): Observable<{ fileName: string; imageUrl: string }> {
    return new Observable((observer) => {
      const reader = new FileReader();
      reader.onload = () => {
        observer.next({ fileName: file.name, imageUrl: reader.result as string });
        observer.complete();
      };
      reader.onerror = (err) => observer.error(err);
      reader.readAsDataURL(file);
    });
  }

  setCurrentProduct(product: any) {
    this.currentProduct.next(product);
  }

  getCurrentProduct() {
    return this.currentProduct$;
  }
}
