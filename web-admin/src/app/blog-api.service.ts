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
export class BlogApiService {
  private firestore = inject(Firestore);
  private col = collection(this.firestore, 'blogs');

  /** Chuẩn hoá Date -> ISO string để đồng nhất với model (string dates) */
  private normalize(data: any): any {
    const out: any = {};
    for (const [k, v] of Object.entries(data)) {
      if (v === undefined) continue;
      out[k] = v instanceof Date ? v.toISOString() : v;
    }
    return out;
  }

  getBlogs(): Observable<any[]> {
    return from(getDocs(this.col)).pipe(
      map((snap) => snap.docs.map((d) => ({ _id: d.id, ...(d.data() as any) })))
    );
  }

  addBlog(data: any): Observable<any> {
    const record = this.normalize({
      ...data,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    });
    return from(addDoc(this.col, record)).pipe(map((ref) => ({ _id: ref.id, ...record })));
  }

  updateBlog(id: string, data: any): Observable<any> {
    const { _id, ...rest } = data;
    const record = this.normalize({ ...rest, updatedAt: new Date().toISOString() });
    return from(updateDoc(doc(this.firestore, 'blogs', id), record)).pipe(
      map(() => ({ _id: id, ...record }))
    );
  }

  deleteBlog(id: string): Observable<any> {
    return from(deleteDoc(doc(this.firestore, 'blogs', id)));
  }

  /** Chưa dùng Storage — mã hoá ảnh thành data URL (base64) */
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
}
