/**
 * Seed 4 mã giảm giá demo vào Firestore collection `vouchers` — khớp với danh sách
 * hiển thị ở app khách ("Mã khả dụng (4)") và schema quản lý bên admin (ModuleForm.VOUCHERS).
 *
 * Doc id = mã voucher (SALE10, SALE20, GIAM50K, FREESHIP) → chạy lại KHÔNG tạo trùng.
 *
 * Field theo admin: code, type (percent|fixed|freeship), value, minOrder,
 *   expiry (yyyy-MM-dd), description, image, createdAt. Thêm title/maxDiscount cho đầy đủ.
 *
 * Cách chạy (trong thư mục web-admin):
 *   node scripts/seed-vouchers.mjs                                  # xem trước, không ghi
 *   FIREBASE_EMAIL=... FIREBASE_PASSWORD=... node scripts/seed-vouchers.mjs --apply
 *
 * Rules yêu cầu đăng nhập admin để ghi → đặt FIREBASE_EMAIL/PASSWORD (tài khoản admin).
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
const EXPIRY = '2026-12-31'; // Hạn dùng chung cho các mã demo

const VOUCHERS = [
  {
    id: 'SALE10', code: 'SALE10', title: 'Giảm 10% (tối đa 100K)',
    type: 'percent', value: 10, minOrder: 0, maxDiscount: 100000,
    description: 'Áp dụng cho toàn bộ đơn hàng', expiry: EXPIRY, image: '',
  },
  {
    id: 'SALE20', code: 'SALE20', title: 'Giảm 20% (tối đa 200K)',
    type: 'percent', value: 20, minOrder: 1000000, maxDiscount: 200000,
    description: 'Cho đơn hàng từ 1.000.000đ', expiry: EXPIRY, image: '',
  },
  {
    id: 'GIAM50K', code: 'GIAM50K', title: 'Giảm 50.000đ',
    type: 'fixed', value: 50000, minOrder: 0, maxDiscount: 0,
    description: 'Áp dụng cho mọi sản phẩm', expiry: EXPIRY, image: '',
  },
  {
    id: 'FREESHIP', code: 'FREESHIP', title: 'Miễn phí vận chuyển',
    type: 'freeship', value: 0, minOrder: 0, maxDiscount: 0,
    description: 'Cho mọi đơn hàng, mọi gói giao', expiry: EXPIRY, image: '',
  },
];

function nowIso() {
  return new Date().toISOString();
}

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

  console.log(`\n── KẾ HOẠCH (vouchers) ──`);
  VOUCHERS.forEach((v) =>
    console.log(`  vouchers/${v.id}  → ${v.type} ${v.value}${v.type === 'percent' ? '%' : ''} — ${v.description}`));

  if (!APPLY) {
    console.log(`\n(DRY-RUN) Chưa ghi gì. Thêm cờ --apply để thực hiện.`);
    process.exit(0);
  }

  console.log(`\nĐang ghi vào Firestore...`);
  for (const { id, ...data } of VOUCHERS) {
    await setDoc(doc(db, 'vouchers', id), { ...data, createdAt: nowIso() }, { merge: true });
    console.log(`  ~ set vouchers/${id}`);
  }
  console.log(`\n✓ Xong ${VOUCHERS.length} voucher.`);
  process.exit(0);
}

main().catch((e) => {
  console.error('LỖI:', e?.code || '', e?.message || e);
  process.exit(1);
});
