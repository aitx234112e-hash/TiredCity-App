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
export class FeedbackApiService {
  private firestore = inject(Firestore);
  private col = collection(this.firestore, 'feedback');

  sendFeedback(data: any): Observable<any> {
    const { _id, ...rest } = data;
    const record = { ...rest, createdAt: rest.createdAt || new Date().toISOString() };
    return from(addDoc(this.col, record)).pipe(map((ref) => ({ _id: ref.id, ...record })));
  }

  getFeedback(): Observable<any> {
    return from(getDocs(this.col)).pipe(
      map((snap) => snap.docs.map((d) => ({ _id: d.id, ...(d.data() as any) })))
    );
  }

  deleteFeedback(id: string): Observable<any> {
    return from(deleteDoc(doc(this.firestore, 'feedback', id)));
  }

  updateFeedback(id: string, data: any): Observable<any> {
    const { _id, ...rest } = data;
    return from(updateDoc(doc(this.firestore, 'feedback', id), rest)).pipe(
      map(() => ({ _id: id, ...rest }))
    );
  }
}
