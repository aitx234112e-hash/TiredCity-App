package com.tiredcity.app.data.mock;

import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.Review;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * Đánh giá khách hàng mẫu dùng khi backend REST chưa có dữ liệu review thật (offline/demo).
 * Mỗi sản phẩm nhận 7–10 đánh giá được chọn ngẫu nhiên NHƯNG có seed cố định theo id sản phẩm
 * ({@link Random#Random(long)}) — cùng 1 sản phẩm luôn hiện đúng 1 bộ đánh giá giống nhau giữa
 * các lần mở, không đổi lung tung mỗi lần vào lại màn Chi tiết sản phẩm.
 */
public final class MockReviewCatalog {

    private static final String[] REVIEWER_NAMES = {
            "Nguyễn Thu Hà", "Trần Minh Anh", "Lê Thị Ngọc", "Phạm Quốc Bảo", "Hoàng Yến Nhi",
            "Vũ Thảo My", "Đặng Gia Hân", "Bùi Thanh Tùng", "Đỗ Khánh Linh", "Ngô Bảo Trân",
            "Phan Thị Lan", "Trương Minh Đức", "Lý Hoài Thương", "Đinh Nhật Nam", "Mai Phương Uyên",
            "Cao Thị Bích", "Tô Gia Bảo", "Huỳnh Kim Ngân", "Vương Thảo Vy", "Đoàn Anh Thư",
            "Nguyễn Thị Diễm", "Trần Đức Anh", "Lê Bảo Châu", "Phạm Thị Hằng", "Hồ Minh Quân",
    };

    // Bình luận nhóm theo số sao — tỉ lệ nghiêng về 5⭐/4⭐, thỉnh thoảng có 3⭐ cho thực tế.
    private static final String[] COMMENTS_5_STAR = {
            "Vải mềm mịn, may rất chỉn chu, mặc lên tôn dáng hẳn. Sẽ ủng hộ shop dài dài.",
            "Sản phẩm đẹp hơn cả mong đợi, màu lên hình chuẩn không lệch tí nào. Đóng gói cẩn thận.",
            "Chất lượng vượt tầm giá, đường chỉ tỉ mỉ, mặc rất thoải mái. Giao hàng cũng nhanh.",
            "Mình đặt mặc đi chụp ảnh, ai cũng khen đẹp. Form chuẩn, không cần chỉnh sửa gì thêm.",
            "Lần đầu mua Việt phục online mà ưng ý vậy, chắc chắn quay lại ủng hộ shop.",
            "Vải dày dặn, không xuyên thấu, mặc mùa nào cũng hợp. Rất đáng tiền.",
            "Đặt size theo bảng hướng dẫn là vừa luôn, không cần đổi trả. Cảm ơn shop nhiều!",
            "Sản phẩm giống hệt hình, màu sắc tươi tắn, đường may chắc chắn.",
            "Đóng gói kỹ, có túi chống ẩm, mở ra thơm mùi vải mới. Rất hài lòng.",
            "Dịch vụ tư vấn nhiệt tình, hỗ trợ chọn size rất kỹ trước khi đặt.",
            "Chưa từng mặc Việt phục bao giờ nhưng nhờ shop tư vấn nên rất tự tin diện đi sự kiện.",
            "Chi tiết thêu tay đẹp không góc, xứng đáng 5 sao.",
    };

    private static final String[] COMMENTS_4_STAR = {
            "Chất lượng ổn, form đẹp, chỉ hơi tiếc màu thực tế nhạt hơn ảnh một chút.",
            "Vải đẹp, may kỹ nhưng giao hàng hơi chậm hơn dự kiến vài ngày.",
            "Mặc vừa vặn, thoải mái, chỉ mong shop có thêm nhiều màu để lựa chọn hơn.",
            "Sản phẩm tốt so với giá, đường may gọn gàng, sẽ ủng hộ thêm lần sau.",
            "Nhìn chung ưng ý, riêng phần cổ áo hơi rộng so với mong muốn của mình.",
            "Chất vải ổn, mặc mát, chỉ hơi nhăn nhẹ khi lấy ra khỏi hộp, ủi lại là đẹp ngay.",
            "Đẹp và đúng như mô tả, trừ điểm nhẹ vì đóng gói hơi đơn giản.",
    };

    private static final String[] COMMENTS_3_STAR = {
            "Sản phẩm tạm ổn, màu hơi khác so với hình một chút nhưng chất lượng chấp nhận được.",
            "Form áo hơi rộng so với size mình hay mặc, chắc lần sau nên đặt nhỏ hơn 1 size.",
            "Giao hàng hơi lâu, sản phẩm thì ổn, chưa có gì để chê nhiều.",
    };

    private MockReviewCatalog() {}

    /** Sinh 7–10 đánh giá mẫu cho 1 sản phẩm, seed cố định theo id để luôn ra cùng 1 kết quả. */
    public static List<Review> getReviewsForProduct(Product product) {
        if (product == null) return new ArrayList<>();
        String seedKey = product.getId() != null ? product.getId() : product.getName();
        Random rnd = new Random(seedKey.hashCode());

        int count = 7 + rnd.nextInt(4); // 7..10
        List<Review> reviews = new ArrayList<>(count);
        Calendar baseCal = Calendar.getInstance();

        for (int i = 0; i < count; i++) {
            int roll = rnd.nextInt(100);
            String[] pool;
            float rating;
            if (roll < 60)      { pool = COMMENTS_5_STAR; rating = 5f; }
            else if (roll < 90) { pool = COMMENTS_4_STAR; rating = 4f; }
            else                { pool = COMMENTS_3_STAR; rating = 3f; }

            Calendar reviewCal = (Calendar) baseCal.clone();
            reviewCal.add(Calendar.DAY_OF_YEAR, -rnd.nextInt(120)); // rải trong ~4 tháng qua

            Review r = new Review();
            r.setId(seedKey + "-rv" + i);
            r.setProductId(product.getId());
            r.setUserName(REVIEWER_NAMES[rnd.nextInt(REVIEWER_NAMES.length)]);
            r.setRating(rating);
            r.setComment(pool[rnd.nextInt(pool.length)]);
            r.setCreatedAt(reviewCal.getTime());
            reviews.add(r);
        }

        // Mới nhất hiển thị trước, giống hành vi quen thuộc trên các sàn TMĐT.
        reviews.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return reviews;
    }
}
