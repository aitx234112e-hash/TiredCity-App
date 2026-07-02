import { PrismaClient, Role, Menh, ProductStatus, Size } from '@prisma/client';

const prisma = new PrismaClient();

async function main() {
  // ----- Collections -----
  const hoangThanh = await prisma.collection.upsert({
    where: { slug: 'hoang-thanh' },
    update: {},
    create: { name: 'Hoàng Thành', slug: 'hoang-thanh', description: 'BST cảm hứng kiến trúc cung đình' },
  });

  // ----- Categories -----
  const aoDai = await prisma.category.upsert({
    where: { slug: 'ao-dai' },
    update: {},
    create: { name: 'Áo dài cách tân', slug: 'ao-dai' },
  });

  // ----- Demo product + variants -----
  const product = await prisma.product.upsert({
    where: { slug: 'ao-dai-ngu-ho' },
    update: {},
    create: {
      name: 'Áo dài Ngũ Hổ Tướng Quân',
      slug: 'ao-dai-ngu-ho',
      description: 'Áo dài thêu họa tiết Ngũ Hổ.',
      culturalStory: 'Lấy cảm hứng từ tranh Hàng Trống thờ Ngũ Hổ.',
      basePrice: 890000,
      material: 'Lụa tơ tằm',
      menh: Menh.HOA,
      status: ProductStatus.ACTIVE,
      collectionId: hoangThanh.id,
      categoryId: aoDai.id,
      images: {
        create: [{ url: 'https://res.cloudinary.com/demo/sample.jpg', isPrimary: true }],
      },
      variants: {
        create: [
          { color: 'Đỏ', size: Size.M, sku: 'NGUHO-DO-M', price: 890000, stockQty: 12 },
          { color: 'Đỏ', size: Size.L, sku: 'NGUHO-DO-L', price: 890000, stockQty: 8 },
          { color: 'Đen', size: Size.M, sku: 'NGUHO-DEN-M', price: 920000, stockQty: 5 },
        ],
      },
    },
  });

  // ----- Admin user (cap quyen cho email trong env) -----
  const adminEmail = process.env.SEED_ADMIN_EMAIL ?? 'hoensune@gmail.com';
  await prisma.user.upsert({
    where: { email: adminEmail },
    update: { role: Role.ADMIN },
    create: { email: adminEmail, fullName: 'Super Admin', role: Role.ADMIN, menh: Menh.KIM },
  });

  console.log('Seed xong. Admin email:', adminEmail, '| Product:', product.slug);
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(() => prisma.$disconnect());
