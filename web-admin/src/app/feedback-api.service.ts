import { Injectable, inject } from '@angular/core';
import { Observable, from, map } from 'rxjs';
import {
  Firestore,
  collection,
  doc,
  addDoc,
  deleteDoc,
  updateDoc,
  collectionData,
} from '@angular/fire/firestore';

@Injectable({
  providedIn: 'root',
})
export class FeedbackApiService {
  private firestore = inject(Firestore);
  private col = collection(this.firestore, 'feedbacks');

  sendFeedback(data: any): Observable<any> {
    const { _id, ...rest } = data;
    const record = { ...rest, createdAt: rest.createdAt || new Date().toISOString() };
    return from(addDoc(this.col, record)).pipe(map((ref) => ({ _id: ref.id, ...record })));
  }

  getFeedback(): Observable<any[]> {
    // Không dùng query orderBy để tránh lỗi thiếu Index trên Firestore
    return collectionData(this.col, { idField: '_id' }).pipe(
      map(list => list || [])
    );
  }

  deleteFeedback(id: string): Observable<any> {
    return from(deleteDoc(doc(this.firestore, 'feedbacks', id)));
  }

  updateFeedback(id: string, data: any): Observable<any> {
    const { _id, ...rest } = data;
    return from(updateDoc(doc(this.firestore, 'feedbacks', id), rest)).pipe(
      map(() => ({ _id: id, ...rest }))
    );
  }
}
