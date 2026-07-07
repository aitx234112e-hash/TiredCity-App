package com.tiredcity.admin.ui.seed;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SeedDataActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView tvLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        ScrollView scroll = new ScrollView(this);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        Button btnSeed = new Button(this);
        btnSeed.setText("NẠP TOÀN BỘ SẢN PHẨM & VẬN CHUYỂN");
        layout.addView(btnSeed);

        tvLog = new TextView(this);
        tvLog.setText("Trạng thái: Sẵn sàng...");
        layout.addView(tvLog);

        scroll.addView(layout);
        setContentView(scroll);

        btnSeed.setOnClickListener(v -> runFullSeed());
    }

    private void runFullSeed() {
        tvLog.setText("Đang nạp dữ liệu... vui lòng không đóng App.");
        
        // 1. Nạp phương thức vận chuyển
        seedShipping();

        // 2. Danh sách dữ liệu từ Sheet (Category, ID, Name, Price)
        String[][] products = {
            {"ÁO DÀI", "AD01", "Khói Trắng Kết Duyên", "2890000"},
            {"ÁO DÀI", "AD02", "Lam Lụa Cố Trạch", "1590000"},
            {"ÁO DÀI", "AD03", "Kim Vũ Phong Hoa", "1750000"},
            {"ÁO DÀI", "AD04", "Hồng Trần Mộc Dược", "1290000"},
            {"ÁO DÀI", "AD05", "Lục Thuỷ Hoàng Lan", "1450000"},
            {"NHẬT BÌNH", "NB01", "Xích Bào Đối Ấn", "3490000"},
            {"NHẬT BÌNH", "NB02", "Thạch Lam Hoàng Cung", "3290000"},
            {"NHẬT BÌNH", "NB03", "Lục Triều Tiểu Yến", "2890000"},
            {"NHẬT BÌNH", "NB04", "Hoàng Triều Kim Tuyến", "2190000"},
            {"NHẬT BÌNH", "NB05", "Vọng Nguyệt Lam Cung", "2690000"},
            {"ÁO TẤC", "AT01", "Lục Ngọc Vấn Khăn", "1590000"},
            {"ÁO TẤC", "AT02", "Ngọc Vũ Yên Sa", "1890000"},
            {"ÁO TẤC", "AT03", "Mộc Vân Thổ Xà", "1290000"},
            {"GIAO LĨNH", "GL01", "Bạch Sa Liên Vũ", "2290000"},
            {"GIAO LĨNH", "GL02", "Lam Ngọc Cổ Trấn", "2490000"},
            {"YẾM ĐÀO", "YD01", "Sương Mai Bạch Vũ", "2290000"},
            {"YẾM ĐÀO", "YD02", "Trúc Lục Khuê Phòng", "2690000"},
            {"PHỤ KIỆN", "PK01", "Vấn Nguyệt Bạch Vân Cẩm", "350000"},
            {"PHỤ KIỆN", "PK02", "Nón Dâu Cổ Phong", "80000"},
            {"PHỤ KIỆN", "PK03", "Quạt Xếp Khổng Tước", "420000"}
        };

        for (String[] row : products) {
            Map<String, Object> p = new HashMap<>();
            p.put("category", row[0]);
            p.put("id", row[1]);
            p.put("name", row[2]);
            p.put("price", Double.parseDouble(row[3]));
            p.put("stock", 20);
            p.put("description", "Việt Phục TiredCity - " + row[0] + " cao cấp.");
            p.put("image", "https://drive.google.com/uc?export=view&id=1XyZ_Placeholder"); 

            db.collection("products").document(row[1]).set(p);
        }

        tvLog.append("\n✅ Đã nạp xong sản phẩm!");
        Toast.makeText(this, "Thành công!", Toast.LENGTH_SHORT).show();
    }

    private void seedShipping() {
        Object[][] methods = {
            {"Giao hàng Hỏa tốc", 50000.0, "2 giờ"},
            {"Giao hàng Nhanh", 30000.0, "2-3 ngày"},
            {"Giao hàng Tiết kiệm", 15000.0, "4-6 ngày"}
        };

        for (Object[] m : methods) {
            Map<String, Object> ship = new HashMap<>();
            ship.put("name", m[0]);
            ship.put("fee", m[1]);
            ship.put("estimatedTime", m[2]);
            db.collection("shipping_methods").add(ship);
        }
        tvLog.append("\n✅ Đã nạp phương thức vận chuyển!");
    }
}
