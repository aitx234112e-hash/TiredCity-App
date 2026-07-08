/**
 * Seed danh sách bài Tin tức demo vào Firestore collection `blogs` — khớp trang "Tin tức"
 * ở app khách và schema quản lý bài viết bên admin (ModuleForm.BLOGS).
 *
 * Doc id = slug cố định → chạy lại KHÔNG tạo trùng.
 * Field theo admin: title, excerpt, content, thumbnail, authorName, status,
 *   publishedAt, updatedAt, createdAt. Tất cả để status 'published'.
 *
 * Cách chạy (trong thư mục web-admin):
 *   node scripts/seed-news.mjs                                      # xem trước, không ghi
 *   FIREBASE_EMAIL=... FIREBASE_PASSWORD=... node scripts/seed-news.mjs --apply
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
const AUTHOR = 'Tired City';

const NEWS = [
  {
    id: 'nghe-thuat-theu-tay-nhat-binh-cung-dinh',
    title: 'Hát Bội – Tinh hoa Đất Việt',
    date: '2026-07-05',
    excerpt: 'Chuyên đề Tạp chí Văn Hóa số 27: hành trình Hát Bội từ cội nguồn, nhân vật, nghệ thuật hóa trang đến những vở tuồng để đời.',
    content: 'Hát Bội (hay hát tuồng) là một trong những loại hình sân khấu cổ truyền đặc sắc bậc nhất của Việt Nam — nơi ca, múa và diễn xuất hòa quyện để kể lại những câu chuyện lịch sử, thần thoại và đạo lý làm người. Số 27 của Tạp chí Văn Hóa đưa bạn đi trọn hành trình ấy: từ cội nguồn, nhân vật, nghệ thuật hóa trang, những vở tuồng để đời, cho tới cuộc giao thoa của Hát Bội với đời sống hôm nay.',
  },
  {
    id: 'ao-tac-ve-dep-toi-gian',
    title: 'Áo Tấc – vẻ đẹp tối giản của Việt phục',
    date: '2026-07-01',
    excerpt: 'Áo Tấc chinh phục người mặc bằng sự thanh lịch, kín đáo và đường nét tối giản.',
    content: 'Áo Tấc (áo ngũ thân tay thụng) là lễ phục truyền thống với phom dáng rộng rãi, trang nhã. Không cầu kỳ hoa văn, áo Tấc đề cao sự cân đối và chất liệu, phù hợp cả nghi lễ lẫn đời thường hiện đại. Cùng tìm hiểu cách chọn và phối áo Tấc cho từng dịp.',
  },
  {
    id: 'phoi-mau-trang-phuc-theo-ngu-hanh-menh',
    title: 'Phối màu trang phục theo Ngũ Hành mệnh',
    date: '2026-06-26',
    excerpt: 'Chọn màu trang phục hợp mệnh Kim – Mộc – Thuỷ – Hoả – Thổ để tôn lên vẻ đẹp và may mắn.',
    content: 'Theo thuyết Ngũ Hành, mỗi mệnh tương hợp với những gam màu riêng. Mệnh Kim hợp trắng, vàng ánh kim; mệnh Mộc hợp xanh lá; mệnh Thuỷ hợp xanh dương, đen; mệnh Hoả hợp đỏ, hồng; mệnh Thổ hợp nâu, vàng đất. Bài viết gợi ý cách phối Việt phục theo mệnh vừa hài hoà vừa hợp phong thuỷ.',
  },
  {
    id: 'hanh-trinh-phuc-dung-co-phuc-viet',
    title: 'Hành trình phục dựng cổ phục Việt',
    date: '2026-06-17',
    excerpt: 'Câu chuyện của những người trẻ dày công nghiên cứu, tái hiện cổ phục Việt qua các triều đại.',
    content: 'Phục dựng cổ phục là hành trình khảo cứu tư liệu, tranh tượng và hiện vật để tái hiện trang phục các triều Lý, Trần, Lê, Nguyễn. Đằng sau mỗi bộ cổ phục là nỗ lực bảo tồn di sản và lan toả tình yêu văn hoá Việt tới thế hệ hôm nay.',
  },
  {
    id: 'lua-to-tam-chat-lieu-vuot-thoi-gian',
    title: 'Lụa tơ tằm: chất liệu vượt thời gian',
    date: '2026-06-07',
    excerpt: 'Lụa tơ tằm mềm mại, thoáng mát và bền đẹp – linh hồn của nhiều bộ Việt phục.',
    content: 'Từ nghề ươm tơ dệt lụa truyền thống, lụa tơ tằm mang lại độ rủ mềm mại, thoáng mát và ánh óng đặc trưng. Chất liệu này không chỉ tôn dáng mà còn thân thiện với làn da, là lựa chọn hàng đầu cho áo dài và cổ phục cao cấp. Bài viết chia sẻ cách nhận biết và bảo quản lụa tơ tằm.',
  },
];

function isoAt(date) {
  // '2026-07-05' → ISO datetime (giữa ngày) để publishedAt là chuỗi ISO hợp lệ
  return new Date(`${date}T09:00:00.000Z`).toISOString();
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

  console.log(`\n── KẾ HOẠCH (blogs / Tin tức) ──`);
  NEWS.forEach((n) => console.log(`  blogs/${n.id}  → ${n.date} — ${n.title}`));

  if (!APPLY) {
    console.log(`\n(DRY-RUN) Chưa ghi gì. Thêm cờ --apply để thực hiện.`);
    process.exit(0);
  }

  console.log(`\nĐang ghi vào Firestore...`);
  for (const { id, date, ...rest } of NEWS) {
    const publishedAt = isoAt(date);
    const data = {
      title: rest.title,
      excerpt: rest.excerpt,
      content: rest.content,
      thumbnail: '',
      authorName: AUTHOR,
      status: 'published',
      publishedAt,
      updatedAt: publishedAt,
      createdAt: publishedAt,
    };
    await setDoc(doc(db, 'blogs', id), data, { merge: true });
    console.log(`  ~ set blogs/${id}`);
  }
  console.log(`\n✓ Xong ${NEWS.length} bài tin tức.`);
  process.exit(0);
}

main().catch((e) => {
  console.error('LỖI:', e?.code || '', e?.message || e);
  process.exit(1);
});
