import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AdminApiService } from '../admin-api.service';
import { UserApiService } from '../../user-api.service';
import { ProductApiService } from '../../product-api.service';

interface ChatMsg {
  from: 'bot' | 'user';
  text: string;
  time: Date;
}

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot.html',
  styleUrl: './chatbot.css',
})
export class Chatbot implements OnInit {
  messages: ChatMsg[] = [];
  input = '';
  ready = false;

  private orders: any[] = [];
  private users: any[] = [];
  private products: any[] = [];

  suggestions = [
    'Doanh thu hiện tại?',
    'Có bao nhiêu đơn hàng?',
    'Bao nhiêu người dùng?',
    'Đơn nào đang chờ xử lý?',
    'Sản phẩm sắp hết hàng',
  ];

  constructor(
    private adminApi: AdminApiService,
    private userApi: UserApiService,
    private productApi: ProductApiService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.push('bot', 'Xin chào 👋 Mình là trợ lý admin TiredCity. Bạn có thể hỏi mình về doanh thu, đơn hàng, người dùng, sản phẩm — hoặc tra cứu một đơn theo mã.');
    forkJoin({
      orders: this.adminApi.getOrders(),
      users: this.userApi.getUsers(),
      products: this.productApi.getProducts(),
    }).subscribe({
      next: ({ orders, users, products }) => {
        this.orders = orders || [];
        this.users = users || [];
        this.products = products || [];
        this.ready = true;
        this.cdr.markForCheck();
      },
      error: () => {
        this.ready = true;
        this.push('bot', '⚠️ Mình chưa tải được dữ liệu từ Firestore, một số câu trả lời có thể không chính xác.');
        this.cdr.markForCheck();
      },
    });
  }

  ask(q: string): void {
    const text = (q ?? this.input).trim();
    if (!text) return;
    this.push('user', text);
    this.input = '';
    const reply = this.answer(text.toLowerCase());
    this.push('bot', reply);
  }

  private push(from: 'bot' | 'user', text: string): void {
    this.messages.push({ from, text, time: new Date() });
  }

  private currency(n: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(n) || 0);
  }

  private revenue(): number {
    return this.orders.reduce((s, o) => s + (Number(o.totalPrice ?? o.total ?? o.amount ?? 0) || 0), 0);
  }

  private countStatus(status: string): number {
    return this.orders.filter((o) => (o.status || '').toLowerCase() === status).length;
  }

  private answer(q: string): string {
    if (!this.ready) return 'Mình đang tải dữ liệu, bạn đợi vài giây rồi hỏi lại nhé.';

    // Tra cứu đơn theo mã: "tra đơn ABC", "đơn #ABC", hoặc chứa mã
    const idMatch = q.match(/(?:đơn|order|mã)\s*#?\s*([a-z0-9_-]{4,})/i);
    if (idMatch && (q.includes('tra') || q.includes('tìm') || q.includes('#') || q.includes('mã'))) {
      const id = idMatch[1];
      const found = this.orders.find((o) =>
        [o.orderID, o._id, o.id].some((x) => String(x || '').toLowerCase().includes(id))
      );
      if (found) {
        return `Đơn ${found.orderID || found._id}: khách "${found.userName || 'Guest'}", tổng ${this.currency(found.totalPrice ?? found.total ?? 0)}, trạng thái "${found.status || '—'}", đặt ngày ${found.createdAt ? new Date(found.createdAt).toLocaleDateString('vi-VN') : '—'}.`;
      }
      return `Mình không tìm thấy đơn nào khớp mã "${id}".`;
    }

    // Doanh thu
    if (q.includes('doanh thu') || q.includes('revenue') || q.includes('tiền')) {
      return `Tổng doanh thu hiện tại là ${this.currency(this.revenue())} từ ${this.orders.length} đơn hàng.`;
    }

    // Đơn chờ xử lý
    if ((q.includes('chờ') || q.includes('pending') || q.includes('xử lý')) && q.includes('đơn')) {
      const n = this.countStatus('pending');
      return `Hiện có ${n} đơn đang chờ xử lý (pending)${n ? '. Bạn vào mục Đơn hàng để xác nhận nhé.' : '.'}`;
    }

    // Đơn hàng tổng quát
    if (q.includes('đơn') || q.includes('order')) {
      return `Tổng cộng ${this.orders.length} đơn hàng — ${this.countStatus('pending')} chờ xử lý, ${this.countStatus('processing')} đang chuẩn bị, ${this.countStatus('shipped')} đang giao, ${this.countStatus('delivered')} đã giao, ${this.countStatus('cancelled')} đã huỷ.`;
    }

    // Người dùng
    if (q.includes('người dùng') || q.includes('user') || q.includes('khách') || q.includes('tài khoản')) {
      const admins = this.users.filter((u) => ['admin', 'superadmin'].includes(u.role)).length;
      return `Có ${this.users.length} tài khoản (${admins} admin, ${this.users.length - admins} khách hàng).`;
    }

    // Sản phẩm sắp hết
    if (q.includes('hết hàng') || q.includes('sắp hết') || q.includes('tồn kho') || q.includes('low stock')) {
      const low = this.products.filter((p) => Number(p.stocked_quantity ?? p.stock ?? 0) <= 5);
      if (!low.length) return 'Không có sản phẩm nào sắp hết hàng (tồn kho ≤ 5). 👍';
      const names = low.slice(0, 5).map((p) => `${p.product_name || p.name} (${p.stocked_quantity ?? p.stock ?? 0})`).join(', ');
      return `Có ${low.length} sản phẩm tồn kho thấp: ${names}${low.length > 5 ? '…' : ''}.`;
    }

    // Sản phẩm tổng quát
    if (q.includes('sản phẩm') || q.includes('product')) {
      return `Cửa hàng đang có ${this.products.length} sản phẩm.`;
    }

    // Chào hỏi
    if (q.includes('chào') || q.includes('hello') || q.includes('hi') || q.includes('giúp')) {
      return 'Mình có thể trả lời: doanh thu, số đơn hàng & trạng thái, số người dùng, sản phẩm tồn kho thấp, và tra cứu đơn theo mã (VD: "tra đơn #ABC123").';
    }

    return 'Mình chưa hiểu câu hỏi này 🤔. Bạn thử hỏi về "doanh thu", "đơn hàng", "người dùng", "sản phẩm", hoặc "tra đơn #<mã>".';
  }
}
