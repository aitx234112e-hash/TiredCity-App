package com.tiredcity.app.ui.explore;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Article;
import com.tiredcity.app.data.model.Event;
import com.tiredcity.app.utils.DateUtils;

/**
 * Nội dung biên tập cho trang chi tiết bài Sự kiện / Tin tức.
 *
 * <p>Bài lấy từ Firestore dùng luôn mô tả/nội dung của bản ghi; bài mẫu offline
 * dùng phần nội dung biên tập sẵn bên dưới. Ảnh minh hoạ lấy từ kho ảnh sự kiện
 * của Tired City (drawable {@code tc_post_1..12}).
 */
public final class PostContent {

    /** Loại khối trong thân bài dạng khối. */
    public static final int BLOCK_TEXT = 0;
    public static final int BLOCK_HEADING = 1;
    public static final int BLOCK_IMAGE = 2;

    /** Kho ảnh minh hoạ dùng chung cho bài sự kiện & tin tức. */
    private static final int[] IMAGE_POOL = {
        R.drawable.tc_post_1, R.drawable.tc_post_2, R.drawable.tc_post_3,
        R.drawable.tc_post_4, R.drawable.tc_post_5, R.drawable.tc_post_6,
        R.drawable.tc_post_7, R.drawable.tc_post_8, R.drawable.tc_post_9,
        R.drawable.tc_post_10, R.drawable.tc_post_11, R.drawable.tc_post_12,
    };

    public String header;
    public String title;
    public String meta;
    /** Nhãn chuyên đề phía trên tiêu đề — chỉ bài đặc biệt mới có. */
    @Nullable public String kicker;
    @Nullable public String factTime;
    @Nullable public String factPlace;
    public String body;
    /**
     * Thân bài dạng khối, dùng thay cho {@link #body} khi bài có tiêu đề mục và ảnh
     * xen giữa các đoạn. Ba mảng song song, phần tử thứ i mô tả một khối:
     * {@code blockKinds[i]} là {@link #BLOCK_TEXT} / {@link #BLOCK_HEADING} /
     * {@link #BLOCK_IMAGE}; khối chữ đọc {@code blockTexts[i]}, khối ảnh đọc
     * {@code blockImages[i]} và lấy {@code blockTexts[i]} làm chú thích.
     */
    public int[] blockKinds = new int[0];
    public String[] blockTexts = new String[0];
    public int[] blockImages = new int[0];
    /** Câu trích dẫn nổi bật đặt sau phần nội dung. */
    @Nullable public String quote;
    /** Dòng ghi nguồn tư liệu ở cuối bài. */
    @Nullable public String credit;
    @DrawableRes public int heroRes;
    @Nullable public String heroUrl;
    public int[] gallery = new int[0];
    /** Chú thích cho từng ảnh; rỗng nghĩa là ảnh minh hoạ thường (không khung, cắt vuông). */
    public String[] galleryCaptions = new String[0];
    /** Nhãn của khối ảnh; {@code null} thì dùng mặc định "Hình ảnh". */
    @Nullable public String galleryLabel;

    private PostContent() {}

    // ── Sự kiện ──────────────────────────────────────────────────────────

    public static PostContent forEvent(Event event, String header) {
        PostContent c = new PostContent();
        c.header = header;
        c.title = safe(event.getTitle());
        c.factPlace = "📍  " + (event.isOnline() ? "Online" : safe(event.getLocation()));
        c.factTime = event.getEventDate() != null
                ? "🗓  " + DateUtils.formatDisplayDate(event.getEventDate())
                : null;
        c.meta = "Tired City" + (event.getEventDate() != null
                ? " · " + DateUtils.formatDisplayDate(event.getEventDate()) : "");

        String description = safe(event.getDescription());
        c.body = !description.isEmpty() ? description : eventBody(c.title);

        c.heroRes = event.getLocalImageRes() != 0 ? event.getLocalImageRes() : heroForTitle(c.title);
        if (event.getLocalImageRes() == 0 && notEmpty(event.getImageUrl())) {
            c.heroUrl = event.getImageUrl();
        }
        c.gallery = galleryForTitle(c.title, c.heroRes);
        return c;
    }

    /** Bài mẫu offline: nội dung biên tập theo từng sự kiện. */
    private static String eventBody(String title) {
        String key = title.toLowerCase();
        if (key.contains("triển lãm")) {
            return "Triển lãm Việt Phục Xuân quy tụ hơn 60 tác phẩm minh hoạ và 20 bộ cổ phục "
                 + "được phục dựng công phu, kể lại dòng chảy y phục Việt từ thời Lý – Trần đến "
                 + "cuối triều Nguyễn.\n\n"
                 + "Không gian trưng bày được chia thành ba khu: khu tư liệu (bản vẽ, hoa văn, "
                 + "chất liệu gốc), khu trải nghiệm mặc thử cổ phục và khu minh hoạ đương đại — "
                 + "nơi các hoạ sĩ trẻ diễn giải lại di sản bằng ngôn ngữ thị giác của thế hệ mình.\n\n"
                 + "Khách tham quan được hướng dẫn viên đồng hành theo khung giờ cố định, có thể "
                 + "chụp ảnh tự do và nhận một bản in giới hạn khi hoàn thành lộ trình tham quan.\n\n"
                 + "Vào cửa tự do. Ban tổ chức khuyến khích bạn đến sớm 15 phút để nhận sơ đồ "
                 + "triển lãm và đăng ký khung giờ trải nghiệm mặc thử.";
        }
        if (key.contains("talkshow")) {
            return "Buổi trò chuyện xoay quanh câu hỏi: vì sao cổ phục Việt lại trở thành một "
                 + "hiện tượng văn hoá trong cộng đồng người trẻ những năm gần đây?\n\n"
                 + "Ba khách mời — một nhà nghiên cứu trang phục, một hoạ sĩ minh hoạ và một bạn "
                 + "trẻ đang vận hành cộng đồng cổ phục — sẽ cùng nhìn lại hành trình từ những "
                 + "bản vẽ tư liệu đầu tiên đến các bộ sưu tập ứng dụng ngày nay.\n\n"
                 + "Nội dung chính: nguồn gốc và quy chuẩn của Nhật Bình, Áo Tấc, Ngũ Thân; ranh "
                 + "giới giữa phục dựng và sáng tạo; cách kể chuyện di sản trên nền tảng số.\n\n"
                 + "Buổi talkshow diễn ra trực tuyến, có phần hỏi đáp mở cuối chương trình. Link "
                 + "tham dự được gửi qua email sau khi bạn đăng ký.";
        }
        if (key.contains("nhật bình")) {
            return "Đêm trình diễn tái hiện nghi thức và vẻ đẹp của Nhật Bình — thường phục dành "
                 + "cho bậc mệnh phụ trong cung đình triều Nguyễn.\n\n"
                 + "Chương trình mở đầu bằng phần diễn giải cấu trúc trang phục: cổ giao lĩnh, "
                 + "hoa văn phượng ổ, dải ngũ sắc nơi tay áo và những lớp thêu chỉ vàng đòi hỏi "
                 + "hàng trăm giờ thủ công.\n\n"
                 + "Tiếp đó là màn trình diễn của các người mẫu không chuyên trong không gian "
                 + "Văn Miếu về đêm, đi cùng nhạc cụ truyền thống và phần ngâm thơ.\n\n"
                 + "Số lượng chỗ ngồi có hạn. Ban tổ chức đề nghị khách tham dự mặc trang phục "
                 + "lịch sự, hạn chế dùng đèn flash trong suốt phần trình diễn.";
        }
        if (key.contains("chợ phiên")) {
            return "Chợ phiên cuối tuần là nơi các nhóm phục dựng, xưởng may thủ công và hoạ sĩ "
                 + "minh hoạ cùng bày biện trong một không gian mở giữa lòng phố đi bộ.\n\n"
                 + "Bạn có thể thử áo Ngũ Thân, đặt may theo số đo, xem nghệ nhân thêu tay tại "
                 + "chỗ, hoặc đơn giản là ngồi lại nghe một bản chèo giữa buổi chiều.\n\n"
                 + "Khu vực Tired City mang tới các bản in giới hạn, sổ tay và phụ kiện lấy cảm "
                 + "hứng từ hoa văn cổ — nhiều thiết kế chỉ phát hành trong ngày phiên chợ.\n\n"
                 + "Phiên chợ họp cả ngày, đông nhất vào khung 16h–20h. Vào cửa tự do.";
        }
        return "Tired City tổ chức chương trình này như một phần trong hành trình đưa di sản văn "
             + "hoá Việt đến gần hơn với người trẻ.\n\n"
             + "Chương trình gồm phần trưng bày, phần trò chuyện cùng khách mời và khu trải "
             + "nghiệm dành cho người tham dự.\n\n"
             + "Vào cửa tự do. Thông tin chi tiết về lịch trình sẽ được cập nhật trên các kênh "
             + "chính thức của Tired City trước ngày diễn ra.";
    }

    // ── Tin tức ──────────────────────────────────────────────────────────

    public static PostContent forArticle(Article article, String header) {
        PostContent c = new PostContent();
        c.header = header;
        String title = article.getTitleVi() != null ? article.getTitleVi() : article.getTitle();
        c.title = safe(title);
        String author = notEmpty(article.getAuthor()) ? article.getAuthor() : "Tired City";
        c.meta = author + (article.getPublishedDate() != null
                ? " · " + DateUtils.formatDisplayDate(article.getPublishedDate()) : "");

        c.heroRes = article.getLocalImageRes() != 0 ? article.getLocalImageRes() : heroForTitle(c.title);
        if (article.getLocalImageRes() == 0 && notEmpty(article.getImageUrl())) {
            c.heroUrl = article.getImageUrl();
        }

        // Bài hát bội dùng nội dung & bộ trang tạp chí biên tập sẵn, bỏ qua nội dung
        // từ Firestore (vốn không khớp tiêu đề). Ảnh bìa vẫn giữ nguyên như trên.
        if (isHatBoi(c.title)) {
            applyHatBoiFeature(c);
            return c;
        }

        String content = safe(article.getContent());
        if (content.isEmpty()) content = safe(article.getExcerpt());
        c.body = !content.isEmpty() ? content : articleBody(c.title);

        c.gallery = galleryForTitle(c.title, c.heroRes);
        return c;
    }

    // ── Chuyên đề Hát Bội ────────────────────────────────────────────────

    /** Bài viết về hát bội (tuồng) — nhận cả cách viết hoa/thường khác nhau. */
    private static boolean isHatBoi(String title) {
        String key = safe(title).toLowerCase();
        return key.contains("hát bội") || key.contains("hát tuồng");
    }

    /**
     * Nội dung biên tập cho bài hát bội, rút từ chuyên đề "Hát Bội – Tinh hoa Đất Việt"
     * (Tạp chí Văn Hóa số 27): lịch sử, tuyến nhân vật, nghệ thuật hoá trang, các vở
     * tuồng để đời và luồng gió đương đại.
     *
     * <p>Thân bài dựng bằng khối: tiêu đề mục in hoa vàng kim, đoạn văn có từ khoá bọc
     * trong dấu {@code **…**} để in đậm, và bốn trang tạp chí xen ngay dưới đoạn nói về
     * chúng. Hai trang còn lại nằm ở khối ảnh cuối bài.
     */
    private static void applyHatBoiFeature(PostContent c) {
        c.kicker = "CHUYÊN ĐỀ · TẠP CHÍ VĂN HÓA SỐ 27";
        c.body = "";

        addText(c, "**Hát bội** — hay **hát tuồng** — là loại hình **sân khấu cổ truyền** đặc "
                + "sắc bậc nhất của Việt Nam, nơi **ca hát, vũ đạo và diễn xuất** hoà làm một. "
                + "Đậm nét nhất ở **miền Nam**, mỗi vở tuồng là một lát cắt sử thi: chuyện vua "
                + "tôi, cha con, anh hùng và kẻ thù, được kể bằng thứ **ngôn ngữ ước lệ** mà "
                + "bất kỳ ai cũng đọc được.");

        addImage(c, R.drawable.hb_contents,
                "Trang 03–04 · Mục lục — sáu chương: Lịch sử, Nhân vật, Mặt nạ, Tác phẩm để đời, "
                        + "Hát bội với thời trang, Hát bội với giới trẻ.");

        addHeading(c, "Từ sân đình ra sân khấu");
        addText(c, "Hát bội bắt nguồn từ **hát múa dân gian của người Việt cổ**, giao thoa cùng "
                + "**hí kịch Trung Hoa**. Buổi đầu, nó phục vụ **nghi lễ ở đình, đền, chùa** — "
                + "hát để tế lễ, để thờ cúng. **Cuối thế kỷ 19 và đầu thế kỷ 20**, hát bội vươn "
                + "lên thành hình thức biểu diễn phổ biến khắp các lễ hội, đi cùng **đàn tranh, "
                + "đàn nguyệt, đàn bầu** và tiếng trống chầu. Rồi **thập niên 30–40**, khi **cải "
                + "lương** lên ngôi, hát bội lặng lẽ nhường bước — bắt đầu một chặng **thăng "
                + "trầm** kéo dài đến hôm nay.");
        addImage(c, R.drawable.hb_history,
                "Trang 05–06 · Lịch sử Hát Bội — từ hát múa dân gian Việt cổ và hí kịch Trung Hoa, "
                        + "qua nghi lễ đình – đền – chùa, đến thời hưng thịnh rồi thăng trầm.");

        addHeading(c, "Kép, Lão, Đào và Mụ");
        addText(c, "Nhân vật hát bội không được xây dựng để giống người thật, mà để trở thành "
                + "**biểu tượng**. **Kép** là tuyến vai chính trực, thường gánh phần đối lập "
                + "mạnh mẽ trong vở. **Lão** dựng mạch truyện và giữ chiều sâu các mối quan hệ. "
                + "**Đào** là vai nữ dịu dàng, hiền thục. **Mụ** cũng là nữ nhưng mang cái xấu, "
                + "cái xảo quyệt, mưu mô — có thể là một bà lão, một mẹ kế. Bốn tuyến vai ấy "
                + "dựng nên mâu thuẫn **thiện – ác**, và qua đó là những **bài học nhân sinh**.");
        addImage(c, R.drawable.hb_characters,
                "Trang 07–08 · Nhân vật — Kép chính trực, Lão dựng mạch truyện, Đào hiền thục, "
                        + "Mụ gian ngoa: bốn tuyến vai, bốn biểu tượng phẩm hạnh.");

        addHeading(c, "Mỗi nét vẽ một thông điệp");
        addText(c, "**Hoá trang** là đỉnh cao nghệ thuật của hát bội. **Đỏ, vàng, xanh, trắng, "
                + "đen** — mỗi màu tượng trưng cho một tính cách. **Mắt** được tô đậm, viền kéo "
                + "dài ra ngoài để tạo thần thái; **môi** thường đỏ hoặc màu đậm để tương phản "
                + "với khuôn mặt; **lông mày** vẽ dài, cong hoặc vuông tuỳ vai. Người xem chỉ "
                + "cần nhìn **mặt nạ** đã biết mình đang đối diện **trung thần** hay **gian "
                + "thần**.");
        addImage(c, R.drawable.hb_makeup,
                "Trang 09–10 · Trang điểm Hát Bội — màu sắc, vẽ mặt, mắt, môi và lông mày, mỗi chi "
                        + "tiết là một thông điệp về tính cách và vai trò nhân vật.");

        addHeading(c, "Những vở tuồng để đời");
        addText(c, "**“Đào Tam Xuân”** kể về nữ tướng tài sắc vẹn toàn mà bạc phận — hình ảnh "
                + "người phụ nữ **chịu đựng, hy sinh** cho gia đình và danh dự. **“Lôi Vũ”** là "
                + "câu chuyện của một vị tướng tài ba mang số phận bất hạnh, thể hiện sức mạnh "
                + "của **lòng trung thành**. **“Ngọc Hân công chúa”** đậm chất **bi kịch**, "
                + "đồng thời là lời ngợi ca một người phụ nữ mạnh mẽ.");

        addHeading(c, "Luồng gió đương đại");
        addText(c, "Năm **2020**, MV **“Chân Ái”** (Orange × Khói × Châu Đăng Khoa) lấy bối cảnh "
                + "sân khấu kịch **thập niên 60**, với đào kép hoạ mặt cầu kỳ và phục trang lộng "
                + "lẫy, trộn **rap, hip-hop và ballad**. Cùng lúc đó, các **show thời trang** lấy "
                + "cảm hứng từ hoa văn, mũ mão và bảng màu hát bội. **Di sản không nằm yên trong "
                + "bảo tàng**: nó đi vào MV, vào sàn diễn, vào tủ đồ của người trẻ.");

        addText(c, "Hát bội vẫn ở đó — trong **tiếng trống chầu**, trong **nét cọ** trên gương "
                + "mặt người nghệ sĩ trước giờ diễn. Điều nó cần không phải sự thương cảm, mà là "
                + "những **cách kể mới** để tiếp tục được nghe.");

        c.quote = "“Mỗi nét vẽ và màu sắc đều có ý nghĩa biểu tượng,\n"
                + "giúp khắc sâu thêm câu chuyện mà vở hát bội\nmuốn truyền tải.”";

        c.galleryLabel = "Trang tạp chí";
        c.gallery = new int[] {
            R.drawable.hb_masterpiece,
            R.drawable.hb_modern,
        };
        c.galleryCaptions = new String[] {
            "Trang 11–12 · Tác phẩm để đời — nữ tướng Đào Tam Xuân, cùng “Lôi Vũ” và “Ngọc Hân "
                    + "công chúa”, kết tinh chất bi hùng của sân khấu tuồng cổ.",
            "Trang 13–14 · Giao thoa cũ và mới — “Chân Ái” đưa hoá trang hát bội vào rap, "
                    + "hip-hop và ballad, mở lại câu hỏi về cách bảo tồn di sản.",
        };

        c.credit = "Tư liệu & hình ảnh: Chuyên đề Hát Bội · Tạp chí Văn Hóa số 27\n"
                 + "Biên tập: Tired City";
    }

    // ── Dựng thân bài dạng khối ──────────────────────────────────────────

    private static void addText(PostContent c, String text) { addBlock(c, BLOCK_TEXT, text, 0); }

    private static void addHeading(PostContent c, String text) { addBlock(c, BLOCK_HEADING, text, 0); }

    private static void addImage(PostContent c, @DrawableRes int res, String caption) {
        addBlock(c, BLOCK_IMAGE, caption, res);
    }

    private static void addBlock(PostContent c, int kind, String text, @DrawableRes int res) {
        int n = c.blockKinds.length;
        c.blockKinds = java.util.Arrays.copyOf(c.blockKinds, n + 1);
        c.blockTexts = java.util.Arrays.copyOf(c.blockTexts, n + 1);
        c.blockImages = java.util.Arrays.copyOf(c.blockImages, n + 1);
        c.blockKinds[n] = kind;
        c.blockTexts[n] = text;
        c.blockImages[n] = res;
    }

    /** Bài mẫu offline: nội dung biên tập theo từng bài viết. */
    private static String articleBody(String title) {
        String key = title.toLowerCase();
        if (key.contains("áo tấc")) {
            return "Áo Tấc — còn gọi là áo lễ Ngũ Thân tay thụng — là dạng lễ phục phổ biến bậc "
                 + "nhất của người Việt từ thế kỷ XVIII đến đầu thế kỷ XX.\n\n"
                 + "Cái tên \"tấc\" đến từ phần cổ áo rộng đúng một tấc. Áo gồm năm thân, tượng "
                 + "trưng cho tứ thân phụ mẫu và bản thân người mặc; năm chiếc khuy cài tượng "
                 + "trưng cho nhân, nghĩa, lễ, trí, tín.\n\n"
                 + "Vẻ đẹp của Áo Tấc nằm ở sự tối giản: không thêu kim tuyến, không hoa văn "
                 + "rườm rà, mọi hiệu quả thị giác đều đến từ cách chất liệu đổ nếp và cách tay "
                 + "áo thụng buông xuống khi người mặc chắp tay hành lễ.\n\n"
                 + "Ngày nay, Áo Tấc trở lại trong lễ cưới, lễ tế và cả những buổi chụp ảnh "
                 + "thường ngày của người trẻ — một minh chứng cho thấy di sản không cần bị đóng "
                 + "khung trong bảo tàng để tiếp tục sống.";
        }
        if (key.contains("ngũ hành") || key.contains("phối màu")) {
            return "Ngũ Hành — Kim, Mộc, Thuỷ, Hoả, Thổ — không chỉ là hệ triết học phương Đông "
                 + "mà còn là một bảng màu đã được cha ông sử dụng hàng trăm năm.\n\n"
                 + "Mệnh Kim hợp trắng, ghi, ánh kim; mệnh Mộc hợp xanh lá, xanh rêu; mệnh Thuỷ "
                 + "hợp xanh dương, đen; mệnh Hoả hợp đỏ, cam, hồng; mệnh Thổ hợp vàng đất, nâu.\n\n"
                 + "Nguyên tắc phối màu dựa trên quan hệ tương sinh: chọn màu bản mệnh làm nền, "
                 + "thêm màu tương sinh làm điểm nhấn, và tiết chế màu tương khắc xuống dưới một "
                 + "phần mười tổng thể trang phục.\n\n"
                 + "Điều thú vị là bảng màu Ngũ Hành gần như trùng khớp với các cặp màu bổ trợ "
                 + "trong lý thuyết màu hiện đại — bằng chứng cho thấy thẩm mỹ truyền thống có "
                 + "cơ sở thị giác rất vững.";
        }
        if (key.contains("phục dựng")) {
            return "Phục dựng cổ phục Việt là một công việc bắt đầu từ thư viện, không phải từ "
                 + "xưởng may.\n\n"
                 + "Nhóm nghiên cứu phải đối chiếu tranh vẽ, tượng thờ, châu bản triều Nguyễn và "
                 + "các hiện vật còn sót lại — thường chỉ là vài mảnh vải đã mục — để dựng lại "
                 + "một tấm áo hoàn chỉnh.\n\n"
                 + "Khó nhất là chất liệu. Nhiều loại lụa, gấm, đoạn trong tư liệu nay không còn "
                 + "ai dệt; các nhóm phục dựng phải đặt hàng riêng từ làng nghề, đôi khi mất "
                 + "hàng năm chỉ để có đúng một loại vải.\n\n"
                 + "Sau hơn một thập kỷ, cộng đồng phục dựng đã đưa Nhật Bình, Giao Lĩnh, Viên "
                 + "Lĩnh, Ngũ Thân trở lại đời sống. Điều họ khôi phục không chỉ là những tấm "
                 + "áo, mà là một cách nhìn về bản sắc.";
        }
        if (key.contains("lụa")) {
            return "Lụa tơ tằm Việt Nam từng là mặt hàng xuất khẩu đi khắp Đông Á, và cho tới "
                 + "nay vẫn là chất liệu được chọn cho những bộ cổ phục quan trọng nhất.\n\n"
                 + "Một mét lụa cần khoảng 3.000 con tằm và gần một tháng chăm sóc. Sợi tơ được "
                 + "kéo trong nước nóng, dệt trên khung gỗ rồi nhuộm bằng củ nâu, lá bàng, vỏ "
                 + "cây — những gam màu trầm rất khó bắt chước bằng thuốc nhuộm công nghiệp.\n\n"
                 + "Điều khiến lụa vượt thời gian nằm ở cấu trúc sợi: tam giác, phản xạ ánh sáng "
                 + "theo nhiều hướng, khiến bề mặt vải đổi màu nhẹ theo góc nhìn. Đó là lý do "
                 + "một tấm áo lụa trơn vẫn \"sống\" hơn nhiều loại vải in hoa.\n\n"
                 + "Các làng nghề Vạn Phúc, Nha Xá, Tân Châu hiện vẫn giữ nghề, dù số khung dệt "
                 + "thủ công còn lại chỉ đếm trên đầu ngón tay.";
        }
        return "Bài viết nằm trong chuyên mục văn hoá của Tired City — nơi chúng tôi kể lại "
             + "những câu chuyện về di sản, thủ công và mỹ thuật Việt.\n\n"
             + "Mỗi bài viết là kết quả của quá trình tìm hiểu tư liệu và trò chuyện cùng các "
             + "nghệ nhân, nhà nghiên cứu đang trực tiếp gìn giữ di sản.\n\n"
             + "Nếu bạn quan tâm tới chủ đề này, hãy theo dõi mục Tin tức để đón đọc các bài "
             + "viết tiếp theo.";
    }

    // ── Ảnh minh hoạ ─────────────────────────────────────────────────────

    /** Ảnh bìa ổn định theo tiêu đề: cùng một bài luôn nhận cùng một ảnh. */
    @DrawableRes
    public static int heroForTitle(String title) {
        String key = safe(title).toLowerCase();
        if (isHatBoi(key)) return R.drawable.hb_cover;
        if (key.contains("triển lãm")) return R.drawable.tc_post_1;
        if (key.contains("talkshow")) return R.drawable.tc_post_8;
        if (key.contains("nhật bình")) return R.drawable.tc_post_4;
        if (key.contains("chợ phiên")) return R.drawable.tc_post_2;
        if (key.contains("áo tấc")) return R.drawable.tc_post_11;
        if (key.contains("ngũ hành") || key.contains("phối màu")) return R.drawable.tc_post_6;
        if (key.contains("phục dựng")) return R.drawable.tc_post_5;
        if (key.contains("lụa")) return R.drawable.tc_post_10;
        return IMAGE_POOL[Math.abs(key.hashCode()) % IMAGE_POOL.length];
    }

    /** Hai ảnh phụ, khác ảnh bìa, cũng ổn định theo tiêu đề. */
    private static int[] galleryForTitle(String title, @DrawableRes int heroRes) {
        int seed = Math.abs(safe(title).hashCode());
        int[] picked = new int[2];
        int found = 0;
        for (int step = 1; found < 2 && step <= IMAGE_POOL.length; step++) {
            int candidate = IMAGE_POOL[(seed + step * 5) % IMAGE_POOL.length];
            if (candidate == heroRes) continue;
            if (found == 1 && candidate == picked[0]) continue;
            picked[found++] = candidate;
        }
        return picked;
    }

    private static String safe(@Nullable String s) { return s != null ? s : ""; }

    private static boolean notEmpty(@Nullable String s) { return s != null && !s.isEmpty(); }
}
