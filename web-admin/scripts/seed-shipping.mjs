/**
 * Seed cấu hình vận chuyển vào Firestore để màn Thanh toán (app khách) hiển thị
 * đúng như thiết kế Shopee: 2 gói — "Nhanh" MIỄN PHÍ (gạch giá gốc) + "Hỏa Tốc" trả phí,
 * pill xanh cho thời gian hỏa tốc.
 *
 * Ghi vào:
 *   shipping_configs/standard  → Nhanh: price 0, originalPrice 16500 (gạch ngang) → "Miễn Phí"
 *   shipping_configs/express   → Hỏa Tốc: price 92100, estimate "2 giờ" → pill xanh
 *   shipping_configs/economy   → tắt (isActive:false) để chỉ còn 2 thẻ như hình
 *   shipping_settings/general  → freeshipThreshold 0 (để Hỏa Tốc vẫn TÍNH PHÍ; nếu đặt >0
 *                                và đơn đạt ngưỡng thì MỌI gói đều miễn phí)
 *
 * Cách chạy (trong thư mục web-admin):
 *   node scripts/seed-shipping.mjs                                  # xem trước, không ghi
 *   FIREBASE_EMAIL=... FIREBASE_PASSWORD=... node scripts/seed-shipping.mjs --apply   # ghi thật
 *
 * Nếu Firestore rules yêu cầu đăng nhập, đặt FIREBASE_EMAIL/PASSWORD (tài khoản admin).
 */
import { initializeApp } from 'firebase/app';
import { getFirestore, doc, setDoc } from 'firebase/firestore';
import { getAuth, signInWithEmailAndPassword } from 'firebase/auth';

const firebaseConfig = {
  apiKey: 'AIzaSyAFKXern20RnlEdF_qbgp8n18q7KD2c4gc',
  authDomain: 'tiredcity-daf1e.firebaseapp.com',
  projectId: 'tiredcity-daf1e',
  storageBucket: 'tiredcity-daf1e.firebasestorage.app',
  messagingSenderId: '683649996737',
  appId: '1:683649996737:web:70d254876a071b444dc140',
};

const APPLY = process.argv.includes('--apply');

// id cố định economy/standard/express — app khách map id → tên hiển thị (Tiết kiệm/Nhanh/Hỏa Tốc).
const SHIPPING_CONFIGS = [
  { id: 'standard', name: 'SPX Cơ bản',  price: 0,     originalPrice: 16500, estimate: '1-2 ngày', isActive: true },
  { id: 'express',  name: 'SPX Hỏa tốc', price: 92100, originalPrice: 0,     estimate: '2 giờ',    isActive: true },
  { id: 'economy',  name: 'SPX Tiết kiệm', price: 15000, originalPrice: 0,   estimate: '3-5 ngày', isActive: false },
];

const SHIPPING_SETTINGS = { freeshipThreshold: 0 };

async function main() {
  const app = initializeApp(firebaseConfig);
  const db = getFirestore(app);

  const email = process.env.FIREBASE_EMAIL;
  const password = process.env.FIREBASE_PASSWORD;
  if (email && password) {
    await signInWithEmailAndPassword(getAuth(app), email, password);
    console.log(`✓ Đã đăng nhập: ${email}`);
  } else {
    console.log('! Chưa đặt FIREBASE_EMAIL/PASSWORD — thử chạy không đăng nhập (chỉ được nếu rules mở).');
  }

  console.log(`\n── KẾ HOẠCH ──`);
  SHIPPING_CONFIGS.forEach((c) =>
    console.log(`  shipping_configs/${c.id}  → ${JSON.stringify(c)}`));
  console.log(`  shipping_settings/general → ${JSON.stringify(SHIPPING_SETTINGS)}`);

  if (!APPLY) {
    console.log(`\n(DRY-RUN) Chưa ghi gì. Thêm cờ --apply để thực hiện.`);
    process.exit(0);
  }

  console.log(`\nĐang ghi vào Firestore...`);
  for (const { id, ...data } of SHIPPING_CONFIGS) {
    await setDoc(doc(db, 'shipping_configs', id), data, { merge: true });
    console.log(`  ~ set shipping_configs/${id}`);
  }
  await setDoc(doc(db, 'shipping_settings', 'general'), SHIPPING_SETTINGS, { merge: true });
  console.log(`  ~ set shipping_settings/general`);
  console.log(`\n✓ Xong.`);
  process.exit(0);
}

main().catch((e) => {
  console.error('LỖI:', e?.code || '', e?.message || e);
  process.exit(1);
});
