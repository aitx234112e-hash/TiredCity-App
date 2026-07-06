import { Injectable, OnModuleInit } from '@nestjs/common';
import { initializeApp, cert, getApps } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import * as path from 'path';

@Injectable()
export class FirebaseService implements OnModuleInit {
  onModuleInit() {
    if (!getApps().length) {
      const serviceAccount = require(path.resolve(process.cwd(), 'serviceAccountKey.json'));
      initializeApp({
        credential: cert(serviceAccount)
      });
      console.log('🔥 Firebase Admin SDK initialized');
    }
  }

  getAuth() {
    return getAuth();
  }
}
