
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderApiService } from '../../order-api.service';
import { AddressService } from '../../address.service';
import { AuditService } from '../audit.service';
import { ChangeDetectorRef } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-order-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './order-management.html',
  styleUrls: ['./order-management.css']
})
export class OrderManagement implements OnInit {

  orders: any[] = [];
  filteredOrders: any[] = [];
  searchText: string = '';
  selectedStatus: string = '';
  filterOption = 'orderId';

  startDate: string = '';
  endDate: string = '';

  currentPage = 1;
  pageSize = 5;

  totalOrders = 0;
  totalPages = 1;
  totalRevenue = 0;

  loading = true;
  successMsg = '';
  errorMsg = '';

  statusOptions = [
    { value: 'PENDING', label: 'Chờ xác nhận' },
    { value: 'CONFIRMED', label: 'Đang chuẩn bị' },
    { value: 'SHIPPING', label: 'Đang giao' },
    { value: 'DELIVERED', label: 'Đã giao' },
    { value: 'CANCELLED', label: 'Đã hủy' }
  ];

  // --- Bulk update (A3) ---
  selectedIds = new Set<string>();
  bulkStatus = '';
  bulkProcessing = false;

  constructor(
    private orderService: OrderApiService,
    private addressService: AddressService,
    private audit: AuditService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  countByStatus(status: string): number {
    if (!this.orders) return 0;
    return this.orders.filter(o =>
      (o.status || '').toString().toUpperCase() === status.toUpperCase()
    ).length;
  }

  private clearMsg() {
    setTimeout(() => { this.successMsg = ''; this.errorMsg = ''; }, 3000);
  }

  loadOrders(): void {
    this.orderService.getOrders().subscribe({
      next: (data: any[]) => {
        // Sort by date descending
        this.orders = data.sort((a, b) => {
          const dateA = new Date(a.createdAt || 0).getTime();
          const dateB = new Date(b.createdAt || 0).getTime();
          return dateB - dateA;
        });

        this.filteredOrders = [...this.orders];
        this.totalOrders = this.orders.length;
        this.totalPages = Math.ceil(this.totalOrders / this.pageSize);

        // compute subtotal, total and total revenue
        this.totalRevenue = 0;
        this.orders.forEach(o => {
          try {
            const items = o.orderItems || (o as any).items || [];
            const sub = items.reduce((acc: number, it: any) => {
              const p = Number(it.price || it.unit_price || 0) || 0;
              const q = Number(it.qty || it.quantity || it.qtyOrdered || 0) || 0;
              return acc + p * q;
            }, 0);
            if (o.subTotal == null) o.subTotal = sub;
            if (o.shippingFee == null && (o as any).shipping_fee != null) o.shippingFee = (o as any).shipping_fee;
            if (o.totalPrice == null) o.totalPrice = (Number(o.subTotal || 0) || 0) + (Number(o.shippingFee || 0) || 0);

            const s = (o.status || '').toString().toUpperCase();
            if (s === 'DELIVERED') {
              this.totalRevenue += Number(o.totalPrice || 0);
            }
          } catch (e) {}
        });

      },
      error: (err) => {
        console.error('Lỗi tải đơn hàng:', err);
        this.loading = false;
      }
    });
    this.loading = false;
  }

  setStatusFilter(status: string): void {
    this.selectedStatus = status;
    this.filterOrders();
  }

  filterOrders(): void {

    let result = [...this.orders];

    // search
    if (this.searchText) {

      const text = this.searchText.toLowerCase();

      if (this.filterOption === 'orderId') {

        result = result.filter(order =>
          order._id?.toLowerCase().includes(text)
        );

      } else {

        result = result.filter(order =>
          order.userName?.toLowerCase().includes(text) ||
          order.customerInfo?.phone?.toLowerCase().includes(text) ||
          order.customerInfo?.email?.toLowerCase().includes(text)
        );

      }

    }

    // filter status
    if (this.selectedStatus) {
      const filterS = this.selectedStatus.toUpperCase();

      result = result.filter(order => {
        const orderS = (order.status || '').toString().toUpperCase();

        // Logic lọc thông minh: Gộp các trạng thái tương đương
        if (filterS === 'PROCESSING_GROUP') {
          return orderS === 'PENDING' || orderS === 'CONFIRMED' || orderS === 'PROCESSING';
        }
        if (filterS === 'SHIPPING_GROUP') {
          return orderS === 'SHIPPING' || orderS === 'SHIPPED';
        }

        if (filterS === 'CONFIRMED' || filterS === 'PROCESSING') {
          return orderS === 'CONFIRMED' || orderS === 'PROCESSING';
        }
        if (filterS === 'SHIPPING' || filterS === 'SHIPPED') {
          return orderS === 'SHIPPING' || orderS === 'SHIPPED';
        }

        return orderS === filterS;
      });
    }

    // filter date
    if (this.startDate) {
      const start = new Date(this.startDate).getTime();
      result = result.filter(order => {
        const orderTime = new Date(order.createdAt).getTime();
        return orderTime >= start;
      });
    }

    if (this.endDate) {
      const end = new Date(this.endDate);
      end.setHours(23, 59, 59, 999);
      const endTime = end.getTime();
      result = result.filter(order => {
        const orderTime = new Date(order.createdAt).getTime();
        return orderTime <= endTime;
      });
    }

    this.filteredOrders = result;

    this.totalOrders = this.filteredOrders.length;
    this.totalPages = Math.ceil(this.totalOrders / this.pageSize);

    this.currentPage = 1;

  }

  private getCurrentActorName(): string {
    try {
      const raw = localStorage.getItem('user');
      if (!raw) return 'Admin';
      const u = JSON.parse(raw);
      return u.profileName || u.email || 'Admin';
    } catch {
      return 'Admin';
    }
  }

  updateOrderStatus(order: any, status: string): void {
    const actor = this.getCurrentActorName();
    this.orderService.updateOrderStatus(order._id, status, '', actor)
      .subscribe({

        next: (updated: any) => {
          // Use returned order to keep local state in sync (including isPaid/paidAt)
          if (updated) {
            order.status = updated.status;
            order.isPaid = updated.isPaid;
            order.paidAt = updated.paidAt;
          } else {
            order.status = status;
          }

        },

        error: (err) => {

          console.error("Lỗi cập nhật trạng thái:", err);

        }

      });

  }

  togglePaid(order: any, paid: boolean) {
    this.orderService.updateOrderStatus(order._id, order.status).subscribe({
      next: () => {
        order.isPaid = paid;
      },
      error: () => {
        this.errorMsg = 'Cập nhật trạng thái thanh toán thất bại!';
        this.clearMsg();
      }
    });
  }



  confirmCancelId: string | null = null;

  cancelOrder(order: any): void {
    if (order.status === 'cancelled') return;
    const actor = this.getCurrentActorName();
    this.orderService.cancelOrder(order._id, 'Admin hủy', actor).subscribe({
      next: () => {
        order.status = 'cancelled';
        this.audit.log('order.cancel', order.orderID || order._id, '');
        this.successMsg = 'Đã huỷ đơn hàng.';
        this.clearMsg();
      },
      error: () => {
        this.errorMsg = 'Huỷ đơn thất bại.';
        this.clearMsg();
      }
    });
  }

  // ================= BULK UPDATE (A3) =================
  isSelected(order: any): boolean {
    return this.selectedIds.has(order._id);
  }

  toggleSelect(order: any): void {
    if (this.selectedIds.has(order._id)) this.selectedIds.delete(order._id);
    else this.selectedIds.add(order._id);
  }

  get allOnPageSelected(): boolean {
    const page = this.paginatedOrders;
    return page.length > 0 && page.every((o) => this.selectedIds.has(o._id));
  }

  toggleSelectAll(): void {
    const page = this.paginatedOrders;
    if (this.allOnPageSelected) {
      page.forEach((o) => this.selectedIds.delete(o._id));
    } else {
      page.forEach((o) => this.selectedIds.add(o._id));
    }
  }

  clearSelection(): void {
    this.selectedIds.clear();
    this.bulkStatus = '';
  }

  applyBulkStatus(): void {
    if (!this.bulkStatus || this.selectedIds.size === 0) return;
    const ids = Array.from(this.selectedIds);
    if (!confirm(`Cập nhật ${ids.length} đơn sang "${this.getStatusLabel(this.bulkStatus)}"?`)) return;

    this.bulkProcessing = true;
    const actor = this.getCurrentActorName();
    const calls = ids.map((id) =>
      this.orderService.updateOrderStatus(id, this.bulkStatus, 'Cập nhật hàng loạt', actor).pipe(catchError(() => of(null)))
    );

    forkJoin(calls).subscribe({
      next: (results) => {
        let ok = 0;
        results.forEach((updated: any, i) => {
          const order = this.orders.find((o) => o._id === ids[i]);
          if (order) {
            order.status = updated?.status || this.bulkStatus;
            if (updated?.isPaid != null) order.isPaid = updated.isPaid;
            ok++;
          }
        });
        this.audit.log('order.status', `${ok} đơn`, `Bulk → ${this.getStatusLabel(this.bulkStatus)}`);
        this.bulkProcessing = false;
        this.successMsg = `Đã cập nhật ${ok}/${ids.length} đơn hàng.`;
        this.clearSelection();
        this.filterOrders();
        this.clearMsg();
      },
      error: () => {
        this.bulkProcessing = false;
        this.errorMsg = 'Cập nhật hàng loạt thất bại.';
        this.clearMsg();
      },
    });
  }

  // --- Các hàm modal chi tiết đơn hàng ---
  selectedOrder: any = null;

  // Shipping form — SPX tracking code
  trackingCodeInput = '';
  savingShipping = false;

  showOrderDetail(order: any) {
    // clone to avoid mutating list view state
    this.selectedOrder = { ...order };
    this.trackingCodeInput = order?.shipping?.trackingCode || '';

    // Try to attach saved address object for display (address, ward, city)
    const uid = order?.user || order?.userId || (typeof localStorage !== 'undefined' && JSON.parse(localStorage.getItem('user') || '{}')._id) || '';
    if (!uid) return;

    this.addressService.getAddressByUser(uid).subscribe({
      next: (addr: any) => {
        // normalize various address shapes
        let addrString = '';
        if (!addr) addrString = '';
        else if (typeof addr === 'string') addrString = addr;
        else if (addr.address) addrString = addr.address;
        else if (addr.fullAddress) addrString = addr.fullAddress;
        else {
          const parts: string[] = [];
          if (addr.houseNumber) parts.push(addr.houseNumber);
          if (addr.street) parts.push(addr.street);
          if (addr.address) parts.push(addr.address);
          if (addr.ward) parts.push(addr.ward);
          if (addr.district) parts.push(addr.district);
          if (addr.city) parts.push(addr.city);
          if (addr.province) parts.push(addr.province);
          addrString = parts.filter(Boolean).join(', ');
        }

        if (this.selectedOrder) {
          this.selectedOrder.shippingAddress = this.selectedOrder.shippingAddress || addrString;
          if (addr && typeof addr === 'object') this.selectedOrder.shippingAddressObj = addr;
          if (!this.selectedOrder.customerInfo) this.selectedOrder.customerInfo = {};
          this.selectedOrder.customerInfo.address = this.selectedOrder.customerInfo.address || addrString;
          // compute subtotal if missing
          try {
            const items = this.selectedOrder.orderItems || (this.selectedOrder as any).items || [];
            const sub = items.reduce((acc: number, it: any) => {
              const p = Number(it.price || it.unit_price || 0) || 0;
              const q = Number(it.qty || it.quantity || it.qtyOrdered || 0) || 0;
              return acc + p * q;
            }, 0);
            if (this.selectedOrder.subTotal == null) this.selectedOrder.subTotal = sub;
          } catch (e) {}
          // compute shipping fee if missing
          if (this.selectedOrder.shippingFee == null) {
            if ((this.selectedOrder as any).shipping_fee != null) this.selectedOrder.shippingFee = (this.selectedOrder as any).shipping_fee;
            else {
              const cityCandidate = (addr && typeof addr === 'object' ? addr.city : '') || addrString || this.selectedOrder.customerInfo?.address;
              const fee = this.computeShippingFeeFromLocation(cityCandidate);
              this.selectedOrder.shippingFee = fee;
              console.log('admin computed shipping fee for selectedOrder', this.selectedOrder._id, 'cityCandidate=', cityCandidate, 'fee=', fee);
            }
          }
          // compute total
          this.selectedOrder.totalPrice = (Number(this.selectedOrder.subTotal || 0) || 0) + (Number(this.selectedOrder.shippingFee || 0) || 0);
          console.log('admin selectedOrder totals:', { orderId: this.selectedOrder._id, subTotal: this.selectedOrder.subTotal, shippingFee: this.selectedOrder.shippingFee, totalPrice: this.selectedOrder.totalPrice });
        }

        // ensure modal updates
        try { this.cdr.detectChanges(); } catch (e) {}
      },
      error: () => {
        // ignore missing address
      }
    });
  }

  closeOrderDetail() {
    this.selectedOrder = null;
  }

  private loadShippingForm(order: any) {}

  private toDateTimeLocal(dateStr: string): string { return ''; }

  // Xác nhận đơn hàng: pending → processing
  confirmOrder() {
    if (!this.selectedOrder) return;
    const actor = this.getCurrentActorName();
    this.orderService.updateOrderStatus(this.selectedOrder._id, 'CONFIRMED', '', actor).subscribe({
      next: (updated: any) => {
        if (updated) {
          this.selectedOrder.status = updated.status;
          const idx = this.orders.findIndex((o: any) => o._id === this.selectedOrder._id);
          if (idx >= 0) this.orders[idx].status = updated.status;
        }
        this.audit.log('order.status', this.selectedOrder.orderID || this.selectedOrder._id, '→ Đang chuẩn bị');
        this.successMsg = 'Đã xác nhận đơn hàng!';
        this.clearMsg();
      },
      error: () => {
        this.errorMsg = 'Xác nhận đơn hàng thất bại!';
        this.clearMsg();
      }
    });
  }

  // Gửi hàng: processing → shipped + tracking code
  shipOrder() {
    if (!this.selectedOrder || !this.trackingCodeInput.trim()) return;
    this.savingShipping = true;
    this.orderService.shipOrder(this.selectedOrder._id, this.trackingCodeInput.trim()).subscribe({
      next: (updated: any) => {
        this.savingShipping = false;
        if (updated) {
          this.selectedOrder.status = updated.status;
          this.selectedOrder.shipping = updated.shipping;
          const idx = this.orders.findIndex((o: any) => o._id === this.selectedOrder._id);
          if (idx >= 0) {
            this.orders[idx].status = updated.status;
            this.orders[idx].shipping = updated.shipping;
          }
        }
        this.audit.log('order.status', this.selectedOrder.orderID || this.selectedOrder._id, `Gửi hàng — ${this.trackingCodeInput.trim()}`);
        this.successMsg = 'Đã gửi hàng thành công!';
        this.clearMsg();
      },
      error: () => {
        this.savingShipping = false;
        this.errorMsg = 'Gửi hàng thất bại!';
        this.clearMsg();
      }
    });
  }

  // Xác nhận đã giao: shipped → delivered
  markDelivered() {
    if (!this.selectedOrder) return;
    const actor = this.getCurrentActorName();
    this.orderService.updateOrderStatus(this.selectedOrder._id, 'DELIVERED', '', actor).subscribe({
      next: (updated: any) => {
        if (updated) {
          this.selectedOrder.status = updated.status;
          this.selectedOrder.isPaid = updated.isPaid;
          const idx = this.orders.findIndex((o: any) => o._id === this.selectedOrder._id);
          if (idx >= 0) {
            this.orders[idx].status = updated.status;
            this.orders[idx].isPaid = updated.isPaid;
          }
        }
        this.audit.log('order.status', this.selectedOrder.orderID || this.selectedOrder._id, '→ Đã giao');
        this.successMsg = 'Đã xác nhận giao hàng!';
        this.clearMsg();
      },
      error: () => {
        this.errorMsg = 'Cập nhật thất bại!';
        this.clearMsg();
      }
    });
  }

  saveShipping() {}

  getStatusLabel(status: string): string {
    const s = (status || '').toString().toUpperCase();
    const map: {[k: string]: string} = {
      'PENDING': 'Chờ xác nhận',
      'PROCESSING': 'Đang chuẩn bị hàng',
      'CONFIRMED': 'Đang chuẩn bị hàng',
      'SHIPPED': 'Đang giao hàng',
      'SHIPPING': 'Đang giao hàng',
      'DELIVERED': 'Đã giao hàng',
      'CANCELLED': 'Đã hủy'
    };
    return map[s] || status;
  }

  exportInvoice(order: any) {
    this.successMsg = 'Đang tạo hóa đơn...';
    // Logic tạo file CSV đơn giản cho một đơn
    this.downloadCSV([order], `HoaDon_${order.orderCode || order._id}.csv`);
    this.clearMsg();
  }

  exportToExcel() {
    if (this.filteredOrders.length === 0) return;
    this.successMsg = `Đang xuất ${this.filteredOrders.length} đơn hàng...`;
    this.downloadCSV(this.filteredOrders, `DanhSachDonHang_${new Date().toLocaleDateString()}.csv`);
    this.clearMsg();
  }

  private downloadCSV(data: any[], fileName: string) {
    const headers = ['Ma Don', 'Khach Hang', 'SDT', 'Email', 'Tong Tien', 'Trang Thai', 'Ngay Dat', 'Thanh Toan', 'Dia Chi'];
    const rows = data.map(o => [
      o.orderCode || o._id,
      o.userName || 'Guest',
      o.phone || o.customerInfo?.phone || '',
      o.userEmail || o.customerInfo?.email || '',
      o.totalPrice,
      this.getStatusLabel(o.status),
      new Date(o.createdAt).toLocaleString('vi-VN'),
      o.isPaid ? 'Da thanh toan' : 'Chua thanh toan',
      (o.shippingAddress || '').replace(/,/g, '-')
    ]);

    const csvContent = "\uFEFF" + [headers, ...rows].map(e => e.join(",")).join("\n");
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a");
    const url = URL.createObjectURL(blob);
    link.setAttribute("href", url);
    link.setAttribute("download", fileName);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  // Return true when order payment method indicates COD/cash
  isCod(order: any): boolean {
    if (!order) return false;
    const pm = (order.paymentMethod || order.payment || '').toString().toLowerCase();
    return pm.includes('cod') || pm.includes('cash') || pm === 'cod' || pm === 'cash';
  }

  // Display label for payment column: 'COD' for cash-on-delivery, otherwise 'Đã thanh toán'
  getPaymentLabel(order: any): string {
    if (!order) return '';
    if (order.isPaid) return 'Đã thanh toán';
    // Not paid yet
    if (this.isCod(order)) return 'Chưa TT';
    // For non-COD (online) orders, assume paid (server should set isPaid)
    return 'Đã thanh toán';
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  get paginatedOrders() {
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    return this.filteredOrders.slice(start, end);
  }

  // Compute shipping fee based on city/location: HCM city => 25000, others => 35000
  private computeShippingFeeFromLocation(loc?: string | null): number {
    const raw = (loc || '').toString().toLowerCase().trim();
    if (!raw) return 35000;
    let normalized = raw.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    normalized = normalized.replace(/\s|\.|\-/g, '');
    if (normalized.includes('hochiminh') || normalized.includes('tphcm') || normalized.includes('hcm')) return 25000;
    return 35000;
  }

}
