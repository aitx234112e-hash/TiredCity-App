import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { OrderStatus, Prisma } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { QueryOrderDto } from './dto/query-order.dto';
import { BulkUpdateStatusDto, UpdateStatusDto } from './dto/update-status.dto';

// State machine: tu trang thai X chi duoc chuyen sang cac trang thai cho phep
const TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['PACKING', 'CANCELLED'],
  PACKING: ['SHIPPING', 'CANCELLED'],
  SHIPPING: ['DELIVERED', 'REFUNDED'],
  DELIVERED: ['REFUNDED'],
  CANCELLED: [],
  REFUNDED: [],
};

@Injectable()
export class OrdersService {
  constructor(private prisma: PrismaService) {}

  // ---------- LIST ----------
  async findAll(query: QueryOrderDto) {
    const { search, status, from, to, page = 1, limit = 20 } = query;

    const where: Prisma.OrderWhereInput = {
      ...(status && { status }),
      ...(search && {
        OR: [
          { orderCode: { contains: search, mode: 'insensitive' } },
          { user: { email: { contains: search, mode: 'insensitive' } } },
        ],
      }),
      ...((from || to) && {
        createdAt: {
          ...(from && { gte: new Date(from) }),
          ...(to && { lte: new Date(to) }),
        },
      }),
    };

    const [items, total] = await this.prisma.$transaction([
      this.prisma.order.findMany({
        where,
        skip: (page - 1) * limit,
        take: limit,
        orderBy: { createdAt: 'desc' },
        include: {
          user: { select: { fullName: true, email: true } },
          _count: { select: { items: true } },
        },
      }),
      this.prisma.order.count({ where }),
    ]);

    return { data: items, meta: { total, page, limit, totalPages: Math.ceil(total / limit) } };
  }

  // ---------- DETAIL ----------
  async findOne(id: string) {
    const order = await this.prisma.order.findUnique({
      where: { id },
      include: {
        user: { select: { fullName: true, email: true, avatarUrl: true } },
        voucher: { select: { code: true, type: true, value: true } },
        items: { include: { variant: { include: { product: { select: { slug: true } } } } } },
      },
    });
    if (!order) throw new NotFoundException('Khong tim thay don hang');
    return order;
  }

  // ---------- UPDATE STATUS (1 don, co kiem tra transition) ----------
  async updateStatus(id: string, dto: UpdateStatusDto) {
    const order = await this.prisma.order.findUnique({ where: { id } });
    if (!order) throw new NotFoundException('Khong tim thay don hang');

    this.assertTransition(order.status, dto.status);

    return this.prisma.order.update({
      where: { id },
      data: { status: dto.status, ...(dto.note && { note: dto.note }) },
    });
  }

  // ---------- BULK UPDATE STATUS ----------
  async bulkUpdateStatus(dto: BulkUpdateStatusDto) {
    const orders = await this.prisma.order.findMany({ where: { id: { in: dto.orderIds } } });

    const skipped: { id: string; reason: string }[] = [];
    const validIds: string[] = [];
    for (const o of orders) {
      if (TRANSITIONS[o.status].includes(dto.status)) validIds.push(o.id);
      else skipped.push({ id: o.id, reason: `Khong the chuyen ${o.status} -> ${dto.status}` });
    }

    const result = await this.prisma.order.updateMany({
      where: { id: { in: validIds } },
      data: { status: dto.status },
    });

    return { updated: result.count, skipped };
  }

  private assertTransition(current: OrderStatus, next: OrderStatus) {
    if (current === next) {
      throw new BadRequestException(`Don hang da o trang thai ${current}`);
    }
    if (!TRANSITIONS[current].includes(next)) {
      throw new BadRequestException(
        `Khong the chuyen tu ${current} sang ${next}. Cho phep: ${TRANSITIONS[current].join(', ') || 'khong co'}`,
      );
    }
  }
}
