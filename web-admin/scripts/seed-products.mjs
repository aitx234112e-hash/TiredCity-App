/**
 * Seed / đồng bộ 44 sản phẩm trang phục (danh mục + màu) vào Firestore collection `products`.
 *
 * Nguồn dữ liệu: sheet "[MBAPP] LIST TRANG PHỤC.xlsx" (đã đối chiếu khớp MockProductCatalog).
 * Cơ chế: KHỚP THEO product_name (chuẩn hoá) — sản phẩm đã có thì chỉ cập nhật
 * product_dept + color (+ giá nếu đang trống); sản phẩm chưa có thì thêm mới. KHÔNG xoá gì.
 *
 * Cách chạy (trong thư mục web-admin):
 *   # xem trước, không ghi:
 *   node scripts/seed-products.mjs
 *   # ghi thật:
 *   FIREBASE_EMAIL=... FIREBASE_PASSWORD=... node scripts/seed-products.mjs --apply
 *
 * Nếu Firestore rules yêu cầu đăng nhập (thường có), phải đặt FIREBASE_EMAIL/PASSWORD
 * (tài khoản admin). Nếu rules mở thì có thể chạy không cần đăng nhập.
 */
import { initializeApp } from 'firebase/app';
import {
  getFirestore, collection, getDocs, doc, updateDoc, addDoc,
} from 'firebase/firestore';
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

// dept = mã danh mục Firestore (khớp categoryLabel trong app khách + dropdown web-admin).
// color = MÀU THÔ theo sheet: app dùng ColorTaxonomy.normalize() để quy về nhóm lọc, nên
// giữ nguyên chuỗi gốc vẫn khớp đúng (kể cả màu ghép như "Xanh - Hồng" → cả Xanh lẫn Hồng).
// Với phụ kiện, color = LOẠI phụ kiện (khớp trực tiếp, không qua nhóm màu).
const MASTER = [
  // ── ÁO DÀI ──
  ['ao-dai', 'clothing', 'Khói Trắng Kết Duyên', 'Trắng', 2890000],
  ['ao-dai', 'clothing', 'Lam Lụa Cố Trạch', 'Xanh lam', 1590000],
  ['ao-dai', 'clothing', 'Kim Vũ Phong Hoa', 'Vàng', 1750000],
  ['ao-dai', 'clothing', 'Hồng Trần Mộc Dược', 'Hồng', 1290000],
  ['ao-dai', 'clothing', 'Lục Thuỷ Hoàng Lan', 'Xanh - Hồng', 1450000],
  ['ao-dai', 'clothing', 'Nguyệt Cầm Phấn Hồng', 'Hồng', 1350000],
  ['ao-dai', 'clothing', 'Phấn Hoa Cổ Điển', 'Trắng', 1200000], // giá sheet trống → theo mock
  // ── NHẬT BÌNH ──
  ['nhat-binh', 'clothing', 'Xích Bào Đối Ấn', 'Trắng', 3490000],
  ['nhat-binh', 'clothing', 'Thạch Lam Hoàng Cung', 'Xanh lá', 3290000],
  ['nhat-binh', 'clothing', 'Lục Triều Tiểu Yến', 'Xanh lá', 2890000],
  ['nhat-binh', 'clothing', 'Hoàng Triều Kim Tuyến', 'Xanh lam đậm', 2190000],
  ['nhat-binh', 'clothing', 'Vọng Nguyệt Lam Cung', 'Đỏ', 2690000],
  ['nhat-binh', 'clothing', 'Nhật Bình Lam Vũ', 'Xanh lam đậm', 2990000],
  ['nhat-binh', 'clothing', 'Lục Yên Lam Bửu', 'Xanh lá', 2750000],
  ['nhat-binh', 'clothing', 'Tử Vân Yên Thảo', 'Vàng', 2450000],
  ['nhat-binh', 'clothing', 'Trầm Hồng Cổ Các', 'Vàng', 2150000],
  ['nhat-binh', 'clothing', 'Kim Trần Mộc Dược', 'Vàng', 1890000],
  // ── ÁO TẤC ──
  ['ao-tac', 'clothing', 'Lục Ngọc Vấn Khăn', 'Xanh lam', 1590000],
  ['ao-tac', 'clothing', 'Ngọc Vũ Yên Sa', 'Trắng', 1890000],
  ['ao-tac', 'clothing', 'Mộc Vân Thổ Xà', 'Cam', 1290000],
  ['ao-tac', 'clothing', 'Thanh Long Cổ Trấn', 'Xanh lá', 1450000],
  ['ao-tac', 'clothing', 'Lục Y Phù Quạt', 'Xanh lam', 1690000],
  ['ao-tac', 'clothing', 'Tơ Ngà Vấn Nguyệt', 'Trắng', 1750000],
  // ── GIAO LĨNH ──
  ['giao-linh', 'clothing', 'Bạch Sa Liên Vũ', 'Xanh lục bảo', 2290000],
  ['giao-linh', 'clothing', 'Lam Ngọc Cổ Trấn', 'Xanh rêu', 2490000],
  ['giao-linh', 'clothing', 'Lục Trúc Vân Khúc', 'Xanh bạc hà', 2450000],
  ['giao-linh', 'clothing', 'Kim Sắc Hoàng Triều', 'Vàng kim', 2290000],
  ['giao-linh', 'clothing', 'Cam Giao Lĩnh Bào', 'Đỏ thẫm hoa văn vàng kim', 1890000],
  ['giao-linh', 'clothing', 'Hắc Kim Mẫu Đơn', 'Đỏ đô thẫm', 2690000],
  // ── YẾM ĐÀO ──
  ['yem-dao', 'clothing', 'Sương Mai Bạch Vũ', 'Đỏ đậm', 2290000],
  ['yem-dao', 'clothing', 'Trúc Lục Khuê Phòng', 'Xanh lá', 2690000],
  ['yem-dao', 'clothing', 'Yên Hoa Bạch Liên', 'Vàng đỏ', 1990000],
  ['yem-dao', 'clothing', 'Thanh Lam Trì Liên', 'Xanh lá ngọc', 2350000],
  ['yem-dao', 'clothing', 'Bích Lam Cẩm Tú', 'Vàng kem', 2450000],
  ['yem-dao', 'clothing', 'Dạ Kim Mẫu Đơn', 'Hồng xanh ngọc', 2890000],
  // ── PHỤ KIỆN (color = loại phụ kiện) ──
  ['phu-kien', 'accessory', 'Vấn Nguyệt Bạch Vân Cẩm', 'Khăn đội đầu', 350000],
  ['phu-kien', 'accessory', 'Nón Dâu Cổ Phong', 'Mũ đội đầu', 80000],
  ['phu-kien', 'accessory', 'Quạt Xếp Khổng Tước Khai Bình', 'Quạt', 420000],
  ['phu-kien', 'accessory', 'Lọng Tán Trường An', 'Ô che', 650000],
  ['phu-kien', 'accessory', 'Khăn Vấn Thổ Mộc', 'Khăn đội đầu', 890000],
  ['phu-kien', 'accessory', 'Vân Kiên Đám Mây', 'Ô che', 120000],
  ['phu-kien', 'accessory', 'Quan Phượng Vũ Triều Thiên', 'Mũ đội đầu', 990000],
  ['phu-kien', 'accessory', 'Ngự Lạp Kim Sa', 'Mũ đội đầu', 750000], // giá sheet trống → theo mock
  ['phu-kien', 'accessory', 'Lọng Ngự Tán Kim Vân', 'Ô che', 550000], // giá sheet trống → theo mock
].map(([dept, kind, name, color, price]) => ({ dept, kind, name, color, price }));

const norm = (s) => (s || '').toString().trim().toLowerCase().replace(/\s+/g, ' ');

function newDocFor(m) {
  const base = {
    product_name: m.name,
    product_dept: m.dept,
    color: m.color,
    unit_price: m.price,
    description: `${m.color}.`, // câu đầu = màu → tương thích cả cách đọc màu cũ
    material: m.kind === 'accessory' ? 'Thêu tay' : 'Lụa tơ tằm',
    origin: 'Việt Nam',
    rating: 4.7,
    discount: 0,
    images: [],
  };
  if (m.kind === 'accessory') {
    return { ...base, stock: 20, sizes: [] };
  }
  return {
    ...base,
    stock: 0,
    sizes: [
      { size: 'S', quantity: 5 },
      { size: 'M', quantity: 5 },
      { size: 'L', quantity: 5 },
      { size: 'XL', quantity: 5 },
    ],
  };
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

  const snap = await getDocs(collection(db, 'products'));
  const existing = new Map();
  snap.forEach((d) => existing.set(norm(d.data().product_name), { id: d.id, data: d.data() }));
  console.log(`Firestore hiện có ${snap.size} sản phẩm.\n`);

  const plan = { update: [], add: [], unchanged: [] };
  for (const m of MASTER) {
    const hit = existing.get(norm(m.name));
    if (hit) {
      const patch = {};
      if (hit.data.product_dept !== m.dept) patch.product_dept = m.dept;
      if (hit.data.color !== m.color) patch.color = m.color;
      if (!hit.data.unit_price) patch.unit_price = m.price; // chỉ điền nếu đang trống/0
      if (Object.keys(patch).length) plan.update.push({ id: hit.id, name: m.name, patch });
      else plan.unchanged.push(m.name);
    } else {
      plan.add.push(m);
    }
  }

  console.log(`── KẾ HOẠCH ──`);
  console.log(`Cập nhật: ${plan.update.length} | Thêm mới: ${plan.add.length} | Giữ nguyên: ${plan.unchanged.length}\n`);
  plan.update.forEach((u) => console.log(`  ~ [update] ${u.name} → ${JSON.stringify(u.patch)}`));
  plan.add.forEach((m) => console.log(`  + [add]    ${m.name}  (${m.dept} / ${m.color} / ${m.price})`));

  if (!APPLY) {
    console.log(`\n(DRY-RUN) Chưa ghi gì. Thêm cờ --apply để thực hiện.`);
    process.exit(0);
  }

  console.log(`\nĐang ghi vào Firestore...`);
  for (const u of plan.update) {
    await updateDoc(doc(db, 'products', u.id), u.patch);
    console.log(`  ~ updated ${u.name}`);
  }
  for (const m of plan.add) {
    const ref = await addDoc(collection(db, 'products'), newDocFor(m));
    console.log(`  + added   ${m.name} (${ref.id})`);
  }
  console.log(`\n✓ Xong. Cập nhật ${plan.update.length}, thêm ${plan.add.length}.`);
  process.exit(0);
}

main().catch((e) => {
  console.error('LỖI:', e?.code || '', e?.message || e);
  process.exit(1);
});
