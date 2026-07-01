export const formatVND = (value: number | string) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value));

export const formatDate = (iso: string) =>
  new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(iso));

// Mau badge theo trang thai don
export const ORDER_STATUS_LABEL: Record<string, { text: string; cls: string }> = {
  PENDING: { text: 'Chờ xác nhận', cls: 'bg-amber-100 text-amber-700' },
  CONFIRMED: { text: 'Đã xác nhận', cls: 'bg-blue-100 text-blue-700' },
  PACKING: { text: 'Đang đóng gói', cls: 'bg-indigo-100 text-indigo-700' },
  SHIPPING: { text: 'Đang giao', cls: 'bg-cyan-100 text-cyan-700' },
  DELIVERED: { text: 'Hoàn tất', cls: 'bg-green-100 text-green-700' },
  CANCELLED: { text: 'Đã hủy', cls: 'bg-gray-200 text-gray-600' },
  REFUNDED: { text: 'Hoàn tiền', cls: 'bg-rose-100 text-rose-700' },
};

export const PRODUCT_STATUS_LABEL: Record<string, string> = {
  DRAFT: 'Nháp',
  ACTIVE: 'Đang bán',
  SOLD_OUT: 'Hết hàng',
  ARCHIVED: 'Lưu trữ',
};
