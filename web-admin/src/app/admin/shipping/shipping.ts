import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';
import { AuditService } from '../audit.service';

interface ShippingConfig {
  id: string;
  name: string;
  price: number;
  estimate: string;
  isActive: boolean;
}

interface PickupPoint {
  id: string;
  name: string;
  address: string;
}

interface ShippingSettings {
  activePickupId: string;
  freeshipThreshold: number;
  shipperNote: string;
}

/** 3 gói cước cố định của SPX Express, đồng bộ collection 'shipping_configs' với app Android. */
const DEFAULTS: ShippingConfig[] = [
  { id: 'economy', name: 'SPX Tiết kiệm', price: 15000, estimate: '3-5 ngày', isActive: true },
  { id: 'standard', name: 'SPX Cơ bản', price: 30000, estimate: '1-2 ngày', isActive: true },
  { id: 'express', name: 'SPX Hỏa tốc', price: 50000, estimate: '2 giờ', isActive: true },
];

/** 2 kho lấy hàng cố định — kho đầu tiên là kho mặc định. Đồng bộ với app Android. */
const PICKUP_POINTS: PickupPoint[] = [
  { id: 'hanghanh', name: 'TiredCity 37 Hàng Hành', address: '37 Hàng Hành, Hoàn Kiếm, Hà Nội' },
  { id: 'hanggai', name: 'TiredCity 97 Hàng Gai', address: '97 Hàng Gai, Hoàn Kiếm, Hà Nội' },
];
const DEFAULT_PICKUP_ID = PICKUP_POINTS[0].id;
const SETTINGS_DEFAULTS: ShippingSettings = { activePickupId: DEFAULT_PICKUP_ID, freeshipThreshold: 0, shipperNote: '' };

@Component({
  selector: 'app-shipping',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './shipping.html',
  styleUrl: './shipping.css',
})
export class Shipping implements OnInit {
  configs: ShippingConfig[] = [];
  loading = true;

  showForm = false;
  saving = false;
  errorMsg = '';
  editingId: string | null = null;

  form = { name: '', price: 0, estimate: '', isActive: true };

  // Section 1 + 3
  pickupPoints = PICKUP_POINTS;
  defaultPickupId = DEFAULT_PICKUP_ID;
  settings: ShippingSettings = { ...SETTINGS_DEFAULTS };
  savingSettings = false;
  settingsSaved = false;

  constructor(private adminApi: AdminApiService, private audit: AuditService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
    this.loadSettings();
  }

  load(): void {
    this.loading = true;
    this.adminApi.getShippingConfigs().subscribe({
      next: (list) => {
        const byId = new Map<string, any>((list || []).map((c) => [c._id, c]));
        const missing: ShippingConfig[] = [];

        this.configs = DEFAULTS.map((def) => {
          const found = byId.get(def.id);
          if (!found) {
            missing.push(def);
            return { ...def };
          }
          return {
            id: def.id,
            name: found.name ?? def.name,
            price: Number(found.price) || 0,
            estimate: found.estimate ?? '',
            isActive: found.isActive !== false,
          };
        });

        this.loading = false;
        this.cdr.markForCheck();

        // Empty state: tự tạo 3 bản ghi mẫu để admin bắt đầu chỉnh sửa.
        if (missing.length > 0) this.seed(missing);
      },
      error: () => {
        this.configs = DEFAULTS.map((d) => ({ ...d }));
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  private seed(missing: ShippingConfig[]): void {
    missing.forEach((c) => {
      this.adminApi.saveShippingConfig(c.id, this.toPayload(c)).subscribe({ error: () => {} });
    });
  }

  openEdit(c: ShippingConfig): void {
    this.editingId = c.id;
    this.form = { name: c.name, price: Number(c.price) || 0, estimate: c.estimate || '', isActive: c.isActive };
    this.errorMsg = '';
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
  }

  save(): void {
    if (!this.editingId) return;
    if (this.form.price < 0) {
      this.errorMsg = 'Phí vận chuyển không hợp lệ.';
      return;
    }
    this.saving = true;
    this.errorMsg = '';

    const id = this.editingId;
    const existing = this.configs.find((c) => c.id === id);
    const payload = {
      name: existing?.name ?? this.form.name,
      price: Number(this.form.price) || 0,
      estimate: this.form.estimate.trim(),
      isActive: this.form.isActive,
    };

    this.adminApi.saveShippingConfig(id, payload).subscribe({
      next: () => {
        const idx = this.configs.findIndex((c) => c.id === id);
        if (idx >= 0) this.configs[idx] = { ...this.configs[idx], ...payload };
        this.audit.log('shipping_config.update', payload.name, this.formatCurrency(payload.price));
        this.saving = false;
        this.showForm = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.saving = false;
        this.errorMsg = 'Không thể lưu (kiểm tra quyền Firestore).';
        this.cdr.markForCheck();
      },
    });
  }

  private toPayload(c: ShippingConfig) {
    return { name: c.name, price: c.price, estimate: c.estimate, isActive: c.isActive };
  }

  // ─── Section 1: kho lấy hàng + Section 3: thiết lập chung ───

  loadSettings(): void {
    this.adminApi.getShippingSettings().subscribe({
      next: (s) => {
        if (s) {
          this.settings = {
            activePickupId: s.activePickupId ?? DEFAULT_PICKUP_ID,
            freeshipThreshold: Number(s.freeshipThreshold) || 0,
            shipperNote: s.shipperNote ?? '',
          };
        } else {
          this.settings = { ...SETTINGS_DEFAULTS };
          this.persistSettings(); // seed bản ghi mẫu
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.settings = { ...SETTINGS_DEFAULTS };
        this.cdr.markForCheck();
      },
    });
  }

  selectPickup(p: PickupPoint): void {
    if (this.settings.activePickupId === p.id) return;
    this.settings.activePickupId = p.id;
    this.persistSettings();
  }

  saveSettings(): void {
    if (this.settings.freeshipThreshold < 0) {
      this.settings.freeshipThreshold = 0;
    }
    this.savingSettings = true;
    this.settingsSaved = false;
    this.persistSettings(() => {
      this.audit.log('shipping_settings.update', 'Thiết lập chung', this.formatCurrency(this.settings.freeshipThreshold));
      this.settingsSaved = true;
      setTimeout(() => {
        this.settingsSaved = false;
        this.cdr.markForCheck();
      }, 2500);
    });
  }

  private persistSettings(onDone?: () => void): void {
    const payload = {
      activePickupId: this.settings.activePickupId,
      freeshipThreshold: Number(this.settings.freeshipThreshold) || 0,
      shipperNote: (this.settings.shipperNote || '').trim(),
    };
    this.adminApi.saveShippingSettings(payload).subscribe({
      next: () => {
        this.savingSettings = false;
        if (onDone) onDone();
        this.cdr.markForCheck();
      },
      error: () => {
        this.savingSettings = false;
        this.errorMsg = 'Không thể lưu thiết lập (kiểm tra quyền Firestore).';
        this.cdr.markForCheck();
      },
    });
  }

  formatCurrency(n: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(n) || 0);
  }
}
