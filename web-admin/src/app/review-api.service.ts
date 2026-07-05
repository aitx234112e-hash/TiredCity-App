import { Injectable, inject } from '@angular/core';
import { Observable, from, map } from 'rxjs';
import {
  Firestore,
  collection,
  doc,
  getDocs,
  deleteDoc,
  updateDoc,
  query,
  orderBy,
  collectionData
} from '@angular/fire/firestore';

@Injectable({
  providedIn: 'root',
})
export class ReviewApiService {
  private firestore = inject(Firestore);
  private col = collection(this.firestore, 'reviews');

  getReviews(): Observable<any[]> {
    const q = query(this.col, orderBy('createdAt', 'desc'));
    return collectionData(q, { idField: '_id' });
  }

  deleteReview(id: string): Observable<void> {
    return from(deleteDoc(doc(this.firestore, 'reviews', id)));
  }

  updateReview(id: string, data: any): Observable<void> {
    return from(updateDoc(doc(this.firestore, 'reviews', id), data));
  }
}
