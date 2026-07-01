import { Injectable } from '@nestjs/common';
import { OrderStatus } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';

// Don da hoan tat moi tinh la doanh thu
const REVENUE_STATUSES: OrderStatus[] = ['DELIVERED'];

@Injectable()
export class DashboardService {
  constructor(private prisma: PrismaService) {}

  /** Cac chi so tong quan cho dau trang Dashboard. */
  async overview() {
    const [revenueAgg, totalOrders, pendingOrders, totalUsers, totalProducts, lowStock] =
      await this.prisma.$transaction([
        this.prisma.order.aggregate({
          _sum: { total: true },
          where: { status: { in: REVENUE_STATUSES } },
        }),
        this.prisma.order.count(),
        this.prisma.order.count({ where: { status: OrderStatus.PENDING } }),
        this.prisma.user.count({ where: { role: 'CUSTOMER' } }),
        this.prisma.product.count({ where: { status: 'ACTIVE' } }),
        this.prisma.productVariant.count({ where: { stockQty: { lte: 5 } } }),
      ]);

    return {
      revenue: Number(revenueAgg._sum.total ?? 0),
      totalOrders,
      pendingOrders,
      totalCustomers: totalUsers,
      activeProducts: totalProducts,
      lowStockVariants: lowStock,
    };
  }

  /** Doanh thu theo ngay trong N ngay gan nhat (cho line chart). */
  async revenueByDay(days = 14) {
    const since = new Date();
    since.setDate(since.getDate() - (days - 1));
    since.setHours(0, 0, 0, 0);

    // raw query gom theo ngay (Postgres)
    const rows = await this.prisma.$queryRaw<{ day: Date; revenue: number; orders: bigint }[]>`
      SELECT date_trunc('day', "created_at") AS day,
             COALESCE(SUM(total), 0)::float AS revenue,
             COUNT(*) AS orders
      FROM orders
      WHERE status = 'DELIVERED' AND "created_at" >= ${since}
      GROUP BY day
      ORDER BY day ASC
    `;

    // chuan hoa: lap day du moi ngay ke ca ngay khong co don
    const map = new Map(rows.map((r) => [r.day.toISOString().slice(0, 10), r]));
    const result: { date: string; revenue: number; orders: number }[] = [];
    for (let i = 0; i < days; i++) {
      const d = new Date(since);
      d.setDate(since.getDate() + i);
      const key = d.toISOString().slice(0, 10);
      const row = map.get(key);
      result.push({ date: key, revenue: row ? Number(row.revenue) : 0, orders: row ? Number(row.orders) : 0 });
    }
    return result;
  }

  /** Phan bo don theo trang thai (cho pie/bar chart). */
  async ordersByStatus() {
    const grouped = await this.prisma.order.groupBy({
      by: ['status'],
      _count: { _all: true },
    });
    return grouped.map((g) => ({ status: g.status, count: g._count._all }));
  }

  /** Top san pham ban chay theo so luong (join qua order_items -> variant -> product). */
  async topProducts(limit = 5) {
    const rows = await this.prisma.$queryRaw<
      { product_id: string; name: string; sold: bigint; revenue: number }[]
    >`
      SELECT p.id AS product_id, p.name,
             SUM(oi.quantity) AS sold,
             SUM(oi.line_total)::float AS revenue
      FROM order_items oi
      JOIN product_variants pv ON pv.id = oi.variant_id
      JOIN products p ON p.id = pv.product_id
      JOIN orders o ON o.id = oi.order_id
      WHERE o.status = 'DELIVERED'
      GROUP BY p.id, p.name
      ORDER BY sold DESC
      LIMIT ${limit}
    `;
    return rows.map((r) => ({
      productId: r.product_id,
      name: r.name,
      sold: Number(r.sold),
      revenue: Number(r.revenue),
    }));
  }
}
