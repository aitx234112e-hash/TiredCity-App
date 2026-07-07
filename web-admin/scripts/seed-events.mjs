/**
 * Seed danh sách sự kiện demo vào Firestore collection `events` — khớp trang "Sự kiện"
 * ở app khách và schema quản lý bên admin (ModuleForm.EVENTS).
 *
 * Doc id = slug cố định → chạy lại KHÔNG tạo trùng.
 * Field theo admin: title, date (yyyy-MM-dd), location, description, image, createdAt.
 *
 * Cách chạy (trong thư mục web-admin):
 *   node scripts/seed-events.mjs                                    # xem trước, không ghi
 *   FIREBASE_EMAIL=... FIREBASE_PASSWORD=... node scripts/seed-events.mjs --apply
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

const EVENTS = [
  {
    id: 'trien-lam-viet-phuc-xuan-2025',
    title: 'Triển lãm Việt Phục Xuân 2025',
    date: '2026-07-14',
    location: 'Nhà Văn hoá Thanh Niên, TP.HCM',
    description: 'Trưng bày các bộ Việt phục phục dựng công phu cùng hoạt động trải nghiệm mặc thử, chụp ảnh cổ phục.',
    image: '',
  },
  {
    id: 'workshop-phoi-do-theo-menh',
    title: 'Workshop phối đồ theo mệnh',
    date: '2026-07-21',
    location: 'Tired City Store, Hà Nội',
    description: 'Hướng dẫn phối màu trang phục theo Ngũ Hành mệnh, cùng chuyên gia tư vấn phong cách cá nhân.',
    image: '',
  },
  {
    id: 'talkshow-co-phuc-gen-z',
    title: 'Talkshow: Cổ phục & Gen Z',
    date: '2026-07-10',
    location: 'Online qua Zoom',
    description: 'Trò chuyện về hành trình đưa cổ phục Việt đến gần hơn với giới trẻ và đời sống hiện đại.',
    image: '',
  },
  {
    id: 'dem-trinh-dien-nhat-binh',
    title: 'Đêm trình diễn Nhật Bình',
    date: '2026-07-28',
    location: 'Văn Miếu – Quốc Tử Giám, Hà Nội',
    description: 'Show trình diễn áo Nhật Bình cung đình với ánh sáng và âm nhạc truyền thống.',
    image: '',
  },
  {
    id: 'cho-phien-viet-phuc-cuoi-tuan',
    title: 'Chợ phiên Việt phục cuối tuần',
    date: '2026-07-17',
    location: 'Phố đi bộ Hồ Gươm, Hà Nội',
    description: 'Gian hàng Việt phục, phụ kiện thủ công và không gian chụp ảnh check-in cuối tuần.',
    image: '',
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

  console.log(`\n── KẾ HOẠCH (events) ──`);
  EVENTS.forEach((e) => console.log(`  events/${e.id}  → ${e.date} — ${e.title} @ ${e.location}`));

  if (!APPLY) {
    console.log(`\n(DRY-RUN) Chưa ghi gì. Thêm cờ --apply để thực hiện.`);
    process.exit(0);
  }

  console.log(`\nĐang ghi vào Firestore...`);
  for (const { id, ...data } of EVENTS) {
    await setDoc(doc(db, 'events', id), { ...data, createdAt: nowIso() }, { merge: true });
    console.log(`  ~ set events/${id}`);
  }
  console.log(`\n✓ Xong ${EVENTS.length} sự kiện.`);
  process.exit(0);
}

main().catch((e) => {
  console.error('LỖI:', e?.code || '', e?.message || e);
  process.exit(1);
});
