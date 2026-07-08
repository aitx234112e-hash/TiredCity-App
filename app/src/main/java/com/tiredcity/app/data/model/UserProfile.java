package com.tiredcity.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class UserProfile {

    @SerializedName("id")
    private String id;

    /** Firebase Auth UID (đăng nhập Google/email thật) — dùng để đồng bộ hồ sơ với Firestore. */
    @SerializedName("uid")
    private String uid;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("address")
    private String address;

    @SerializedName("province")
    private String province;

    /**
     * Quận/Huyện — cấp hành chính này đã bị bỏ từ 01/07/2025, không còn ô nhập nào ghi vào nữa.
     * Giữ lại field để hồ sơ CŨ đã lưu trên Firestore vẫn đọc được và {@link #getFullAddress()}
     * vẫn dựng đúng địa chỉ cũ; hồ sơ lưu mới luôn để rỗng.
     */
    @SerializedName("district")
    private String district;

    @SerializedName("ward")
    private String ward;

    @SerializedName("street")
    private String street;

    @SerializedName("avatar")
    private String avatar;

    @SerializedName("birth_date")
    private String birthDate;

    /** Ngũ Hành mệnh element (Kim/Mộc/Thủy/Hỏa/Thổ). */
    @SerializedName("menh")
    private String menh;

    /** Western zodiac name in Vietnamese (e.g. "Sư Tử"). */
    @SerializedName("zodiac")
    private String zodiac;

    /** Chinese zodiac animal in Vietnamese (e.g. "Thìn"). */
    @SerializedName("animal")
    private String animal;

    @SerializedName("style_prefs")
    private List<String> stylePrefs;

    public UserProfile() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()                       { return id; }
    public void setId(String id)                { this.id = id; }

    public String getUid()                      { return uid; }
    public void setUid(String uid)              { this.uid = uid; }

    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }

    public String getEmail()                    { return email; }
    public void setEmail(String email)          { this.email = email; }

    public String getPhone()                    { return phone; }
    public void setPhone(String phone)          { this.phone = phone; }

    public String getAddress()                  { return address; }
    public void setAddress(String address)      { this.address = address; }

    public String getProvince()                 { return province; }
    public void setProvince(String province)    { this.province = province; }

    public String getDistrict()                 { return district; }
    public void setDistrict(String district)    { this.district = district; }

    public String getWard()                     { return ward; }
    public void setWard(String ward)            { this.ward = ward; }

    public String getStreet()                   { return street; }
    public void setStreet(String street)        { this.street = street; }

    /** Gộp địa chỉ đầy đủ để hiển thị / giao hàng: "Số nhà đường, Phường, Quận, Tỉnh". */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, street);
        appendPart(sb, ward);
        appendPart(sb, district);
        appendPart(sb, province);
        return sb.length() > 0 ? sb.toString() : (address != null ? address : "");
    }

    private void appendPart(StringBuilder sb, String part) {
        if (part != null && !part.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(part.trim());
        }
    }

    public String getAvatar()                   { return avatar; }
    public void setAvatar(String avatar)        { this.avatar = avatar; }

    public String getBirthDate()                { return birthDate; }
    public void setBirthDate(String birthDate)  { this.birthDate = birthDate; }

    public String getMenh()                     { return menh; }
    public void setMenh(String menh)            { this.menh = menh; }

    public String getZodiac()                   { return zodiac; }
    public void setZodiac(String zodiac)        { this.zodiac = zodiac; }

    public String getAnimal()                   { return animal; }
    public void setAnimal(String animal)        { this.animal = animal; }

    public List<String> getStylePrefs()                  { return stylePrefs; }
    public void setStylePrefs(List<String> stylePrefs)   { this.stylePrefs = stylePrefs; }

    /** Returns display name, falling back to email. */
    public String getDisplayName() {
        return (name != null && !name.isEmpty()) ? name : email;
    }

    /** Parses birth year from ISO date string (yyyy-MM-dd). */
    public int getBirthYear() {
        if (birthDate == null || birthDate.length() < 4) return 0;
        try { return Integer.parseInt(birthDate.substring(0, 4)); }
        catch (NumberFormatException e) { return 0; }
    }
}
