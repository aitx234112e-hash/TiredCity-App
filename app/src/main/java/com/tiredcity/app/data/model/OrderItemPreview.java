package com.tiredcity.app.data.model;

/**
 * Bản rút gọn của một dòng sản phẩm trong đơn hàng — chỉ dữ liệu cần để hiển thị
 * ở thẻ "Đơn hàng đã mua" (ảnh, tên, biến thể, số lượng, thành tiền).
 */
public class OrderItemPreview {
    private final String name;
    private final String image;
    private final String size;
    private final String color;
    private final int quantity;
    private final double lineTotal;

    public OrderItemPreview(String name, String image, String size, String color,
                            int quantity, double lineTotal) {
        this.name = name;
        this.image = image;
        this.size = size;
        this.color = color;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
    }

    public String getName() { return name; }
    public String getImage() { return image; }
    public String getSize() { return size; }
    public String getColor() { return color; }
    public int getQuantity() { return quantity; }
    public double getLineTotal() { return lineTotal; }

    /** Chuỗi biến thể gọn: "Size M • Đỏ" (bỏ phần trống). */
    public String variantLabel() {
        StringBuilder sb = new StringBuilder();
        if (size != null && !size.trim().isEmpty()) sb.append(size.trim());
        if (color != null && !color.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(color.trim());
        }
        return sb.toString();
    }
}
