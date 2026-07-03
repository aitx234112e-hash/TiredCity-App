package com.tiredcity.admin.data;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.tiredcity.admin.R;

/**
 * Danh muc cac chuc nang admin — mirror dieu huong ben web-admin (nav.html + admin-routing).
 * Moi module mo mot man hinh:
 *  - {@code collection != null}  -> ModuleListActivity (danh sach doc tu Firestore)
 *  - REVENUE / REPORTS           -> man hinh rieng (tinh toan tong hop)
 */
public enum AdminModule {
    PRODUCTS (R.string.m_products, R.string.m_products_desc, R.drawable.ic_products, R.color.m_products, "products"),
    ORDERS   (R.string.m_orders,   R.string.m_orders_desc,   R.drawable.ic_orders,   R.color.m_orders,   "orders"),
    USERS    (R.string.m_users,    R.string.m_users_desc,    R.drawable.ic_users,    R.color.m_users,    "users"),
    VOUCHERS (R.string.m_vouchers, R.string.m_vouchers_desc, R.drawable.ic_vouchers, R.color.m_vouchers, "vouchers"),
    EVENTS   (R.string.m_events,   R.string.m_events_desc,   R.drawable.ic_events,   R.color.m_events,   "events"),
    SHIPPING (R.string.m_shipping, R.string.m_shipping_desc, R.drawable.ic_shipping, R.color.m_shipping, "shipping"),
    FEEDBACK (R.string.m_feedback, R.string.m_feedback_desc, R.drawable.ic_feedback, R.color.m_feedback, "feedback"),
    BLOGS    (R.string.m_blogs,    R.string.m_blogs_desc,    R.drawable.ic_blogs,    R.color.m_blogs,    "blogs"),
    AUDIT    (R.string.m_audit,    R.string.m_audit_desc,    R.drawable.ic_audit,    R.color.m_audit,    "auditLogs"),
    REVENUE  (R.string.m_revenue,  R.string.m_revenue_desc,  R.drawable.ic_revenue,  R.color.m_revenue,  null),
    REPORTS  (R.string.m_reports,  R.string.m_reports_desc,  R.drawable.ic_reports,  R.color.m_reports,  null),
    CHATBOT  (R.string.m_chatbot,  R.string.m_chatbot_desc,  R.drawable.ic_chatbot,  R.color.m_chatbot,  null);

    @StringRes public final int title;
    @StringRes public final int desc;
    @DrawableRes public final int icon;
    public final int colorRes;
    /** Ten collection Firestore; null neu la man hinh tong hop rieng. */
    public final String collection;

    AdminModule(@StringRes int title, @StringRes int desc, @DrawableRes int icon, int colorRes, String collection) {
        this.title = title;
        this.desc = desc;
        this.icon = icon;
        this.colorRes = colorRes;
        this.collection = collection;
    }
}
