import { Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { Prisma } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { CloudinaryService } from '../cloudinary/cloudinary.service';
import { CreateProductDto } from './dto/create-product.dto';
import { UpdateProductDto } from './dto/update-product.dto';
import { QueryProductDto } from './dto/query-product.dto';

@Injectable()
export class ProductsService {
  constructor(
    private prisma: PrismaService,
    private cloudinary: CloudinaryService,
  ) {}

  // Bo dau tieng Viet + tao slug
  private slugify(text: string): string {
    return text
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '') // bo dau tieng Viet
      .replace(/đ/g, 'd')
      .replace(/Đ/g, 'd')
      .toLowerCase()
      .trim()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');
  }

  private async uniqueSlug(base: string, excludeId?: string): Promise<string> {
    let slug = base || `sp-${Date.now()}`;
    let i = 1;
    while (true) {
      const found = await this.prisma.product.findUnique({ where: { slug } });
      if (!found || found.id === excludeId) return slug;
      slug = `${base}-${i++}`;
    }
  }

  // ---------- CREATE ----------
  async create(dto: CreateProductDto) {
    const slug = await this.uniqueSlug(dto.slug ? this.slugify(dto.slug) : this.slugify(dto.name));
    await this.assertSkuUnique(dto.variants.map((v) => v.sku));

    return this.prisma.product.create({
      data: {
        name: dto.name,
        slug,
        description: dto.description,
        culturalStory: dto.culturalStory,
        basePrice: dto.basePrice,
        salePrice: dto.salePrice,
        material: dto.material,
        menh: dto.menh,
        status: dto.status,
        collectionId: dto.collectionId,
        categoryId: dto.categoryId,
        variants: {
          create: dto.variants.map((v) => ({
            color: v.color,
            size: v.size,
            sku: v.sku,
            price: v.price,
            stockQty: v.stockQty,
          })),
        },
      },
      include: { variants: true, images: true, collection: true, category: true },
    });
  }

  // ---------- LIST (filter + pagination) ----------
  async findAll(query: QueryProductDto) {
    const { search, status, collectionId, categoryId, page = 1, limit = 20 } = query;

    const where: Prisma.ProductWhereInput = {
      ...(status && { status }),
      ...(collectionId && { collectionId }),
      ...(categoryId && { categoryId }),
      ...(search && {
        OR: [
          { name: { contains: search, mode: 'insensitive' } },
          { slug: { contains: search, mode: 'insensitive' } },
        ],
      }),
    };

    const [items, total] = await this.prisma.$transaction([
      this.prisma.product.findMany({
        where,
        skip: (page - 1) * limit,
        take: limit,
        orderBy: { createdAt: 'desc' },
        include: {
          collection: { select: { name: true } },
          category: { select: { name: true } },
          images: { where: { isPrimary: true }, take: 1 },
          variants: { select: { stockQty: true } },
        },
      }),
      this.prisma.product.count({ where }),
    ]);

    // them tong ton kho cho moi san pham
    const data = items.map((p) => ({
      ...p,
      totalStock: p.variants.reduce((s, v) => s + v.stockQty, 0),
    }));

    return { data, meta: { total, page, limit, totalPages: Math.ceil(total / limit) } };
  }

  // ---------- DETAIL ----------
  async findOne(id: string) {
    const product = await this.prisma.product.findUnique({
      where: { id },
      include: { variants: true, images: { orderBy: { sortOrder: 'asc' } }, collection: true, category: true },
    });
    if (!product) throw new NotFoundException('Khong tim thay san pham');
    return product;
  }

  // ---------- UPDATE (thay toan bo variant neu co gui kem) ----------
  async update(id: string, dto: UpdateProductDto) {
    await this.findOne(id);

    const data: Prisma.ProductUpdateInput = {
      name: dto.name,
      description: dto.description,
      culturalStory: dto.culturalStory,
      basePrice: dto.basePrice,
      salePrice: dto.salePrice,
      material: dto.material,
      menh: dto.menh,
      status: dto.status,
      ...(dto.collectionId !== undefined && { collection: dto.collectionId ? { connect: { id: dto.collectionId } } : { disconnect: true } }),
      ...(dto.categoryId !== undefined && { category: dto.categoryId ? { connect: { id: dto.categoryId } } : { disconnect: true } }),
    };
    if (dto.slug) data.slug = await this.uniqueSlug(this.slugify(dto.slug), id);

    // Neu gui variants -> dong bo (xoa cu, tao moi). Don gian, du cho do an.
    if (dto.variants) {
      await this.assertSkuUnique(dto.variants.map((v) => v.sku), id);
      data.variants = {
        deleteMany: {},
        create: dto.variants.map((v) => ({
          color: v.color, size: v.size, sku: v.sku, price: v.price, stockQty: v.stockQty,
        })),
      };
    }

    return this.prisma.product.update({
      where: { id },
      data,
      include: { variants: true, images: true },
    });
  }

  // ---------- DELETE ----------
  async remove(id: string) {
    const product = await this.prisma.product.findUnique({ where: { id }, include: { images: true } });
    if (!product) throw new NotFoundException('Khong tim thay san pham');

    // xoa anh tren cloudinary truoc
    await Promise.all(product.images.filter((i) => i.publicId).map((i) => this.cloudinary.deleteImage(i.publicId!)));
    await this.prisma.product.delete({ where: { id } }); // cascade variant + image
    return { message: 'Da xoa san pham' };
  }

  // ---------- UPLOAD IMAGES ----------
  async addImages(productId: string, files: Express.Multer.File[]) {
    await this.findOne(productId);
    const existingCount = await this.prisma.productImage.count({ where: { productId } });

    const uploaded = await Promise.all(files.map((f) => this.cloudinary.uploadImage(f)));
    await this.prisma.productImage.createMany({
      data: uploaded.map((u, idx) => ({
        productId,
        url: u.secure_url,
        publicId: u.public_id,
        isPrimary: existingCount === 0 && idx === 0, // anh dau tien la primary
        sortOrder: existingCount + idx,
      })),
    });
    return this.prisma.productImage.findMany({ where: { productId }, orderBy: { sortOrder: 'asc' } });
  }

  async removeImage(imageId: string) {
    const img = await this.prisma.productImage.findUnique({ where: { id: imageId } });
    if (!img) throw new NotFoundException('Khong tim thay anh');
    if (img.publicId) await this.cloudinary.deleteImage(img.publicId);
    await this.prisma.productImage.delete({ where: { id: imageId } });
    return { message: 'Da xoa anh' };
  }

  // ---------- helper: chong trung SKU ----------
  private async assertSkuUnique(skus: string[], excludeProductId?: string) {
    const dup = skus.filter((s, i) => skus.indexOf(s) !== i);
    if (dup.length) throw new ConflictException(`SKU bi trung trong form: ${[...new Set(dup)].join(', ')}`);

    const existing = await this.prisma.productVariant.findMany({
      where: { sku: { in: skus }, ...(excludeProductId && { productId: { not: excludeProductId } }) },
      select: { sku: true },
    });
    if (existing.length) throw new ConflictException(`SKU da ton tai: ${existing.map((e) => e.sku).join(', ')}`);
  }
}
