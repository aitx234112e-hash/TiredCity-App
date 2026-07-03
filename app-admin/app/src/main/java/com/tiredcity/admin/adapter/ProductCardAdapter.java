package com.tiredcity.admin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.tiredcity.admin.R;
import com.tiredcity.admin.utils.DocUtils;
import com.tiredcity.admin.utils.ImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Grid the san pham cho man hinh PRODUCTS — mirror giao dien card ben web-admin
 * (product-management). Binh truc tiep tu DocumentSnapshot cua Firestore 'products'
 * de tan dung anh / mang sizes / rating ma Row (dang text) khong giu.
 */
public class ProductCardAdapter extends RecyclerView.Adapter<ProductCardAdapter.VH> {

    public interface OnCardClick {
        void onClick(int position);
    }

    private final List<DocumentSnapshot> items = new ArrayList<>();
    private final OnCardClick listener;

    public ProductCardAdapter(OnCardClick listener) {
        this.listener = listener;
    }

    public void submit(List<DocumentSnapshot> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DocumentSnapshot d = items.get(position);

        if (listener != null) {
            h.itemView.setOnClickListener(v -> {
                int pos = h.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onClick(pos);
            });
        }

        // Ten
        String name = DocUtils.str(d, "product_name");
        h.name.setText(name.isEmpty() ? "(Chưa đặt tên)" : name);

        // Badge danh muc
        String dept = DocUtils.str(d, "product_dept");
        if (dept.isEmpty()) {
            h.category.setVisibility(View.GONE);
        } else {
            h.category.setVisibility(View.VISIBLE);
            h.category.setText(categoryLabel(dept));
        }

        // Mau / chat lieu (dong phu duoi ten)
        String subtitle = DocUtils.str(d, "color");
        if (subtitle.isEmpty()) subtitle = DocUtils.str(d, "material");
        if (subtitle.isEmpty()) {
            h.subtitle.setVisibility(View.GONE);
        } else {
            h.subtitle.setVisibility(View.VISIBLE);
            h.subtitle.setText(subtitle);
        }

        // Gia + badge giam gia
        h.price.setText(DocUtils.money(DocUtils.num(d, "unit_price")));
        double discount = DocUtils.num(d, "discount");
        if (discount > 0) {
            h.discount.setVisibility(View.VISIBLE);
            h.discount.setText("-" + (long) discount + "%");
        } else {
            h.discount.setVisibility(View.GONE);
        }

        // Danh gia
        double rating = DocUtils.num(d, "rating");
        if (rating > 0) {
            h.rating.setVisibility(View.VISIBLE);
            h.rating.setText("★ " + trimRating(rating));
        } else {
            h.rating.setVisibility(View.GONE);
        }

        // Anh
        ImageLoader.loadProduct(h.image, firstImage(d));

        // Chip ton kho theo size (hoac tong ton cho phu kien)
        bindSizeChips(h.sizeContainer, d, dept);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ----------------------------------------------------------------- helpers

    private static void bindSizeChips(LinearLayout container, DocumentSnapshot d, String dept) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(container.getContext());

        List<Map<?, ?>> sizes = sizeList(d);
        if (!sizes.isEmpty()) {
            for (Map<?, ?> s : sizes) {
                String label = String.valueOf(s.get("size"));
                long qty = asLong(s.get("quantity"));
                if (label.isEmpty() || "null".equals(label)) continue;
                addChip(inflater, container, label + ": " + qty);
            }
            if (container.getChildCount() > 0) {
                container.setVisibility(View.VISIBLE);
                return;
            }
        }

        // Phu kien / khong co sizes -> hien tong ton kho
        double stock = DocUtils.num(d, "stocked_quantity", "stock");
        addChip(inflater, container, "Tồn: " + (long) stock);
        container.setVisibility(View.VISIBLE);
    }

    private static void addChip(LayoutInflater inflater, LinearLayout container, String text) {
        TextView chip = (TextView) inflater.inflate(R.layout.item_size_chip, container, false);
        chip.setText(text);
        container.addView(chip);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<?, ?>> sizeList(DocumentSnapshot d) {
        List<Map<?, ?>> out = new ArrayList<>();
        Object o = d.get("sizes");
        if (o instanceof List) {
            for (Object it : (List<Object>) o) {
                if (it instanceof Map) out.add((Map<?, ?>) it);
            }
        }
        return out;
    }

    private static long asLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        if (o instanceof String) {
            try {
                return (long) Double.parseDouble(((String) o).replaceAll("[^0-9.\\-]", ""));
            } catch (NumberFormatException ignored) {}
        }
        return 0L;
    }

    @SuppressWarnings("unchecked")
    private static String firstImage(DocumentSnapshot d) {
        Object o = d.get("images");
        if (o instanceof List && !((List<Object>) o).isEmpty()) {
            Object f = ((List<Object>) o).get(0);
            if (f != null) return String.valueOf(f);
        }
        return DocUtils.str(d, "image", "thumbnail");
    }

    private static String categoryLabel(String dept) {
        switch (dept) {
            case "ao-dai":   return "ÁO DÀI";
            case "viet-phuc": return "VIỆT PHỤC";
            case "ao-tac":   return "ÁO TẤC";
            case "yem-dao":  return "YẾM ĐÀO";
            case "phu-kien": return "PHỤ KIỆN";
            default:         return dept.toUpperCase(Locale.ROOT);
        }
    }

    private static String trimRating(double r) {
        if (r == Math.floor(r)) return String.valueOf((long) r);
        return String.format(Locale.US, "%.1f", r);
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView category, discount, name, subtitle, price, rating;
        final LinearLayout sizeContainer;

        VH(@NonNull View v) {
            super(v);
            image = v.findViewById(R.id.ivImage);
            category = v.findViewById(R.id.tvCategory);
            discount = v.findViewById(R.id.tvDiscount);
            name = v.findViewById(R.id.tvName);
            subtitle = v.findViewById(R.id.tvSubtitle);
            price = v.findViewById(R.id.tvPrice);
            rating = v.findViewById(R.id.tvRating);
            sizeContainer = v.findViewById(R.id.sizeContainer);
        }
    }
}
