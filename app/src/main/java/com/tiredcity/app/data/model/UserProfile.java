package com.tiredcity.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class UserProfile {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("address")
    private String address;

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

    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }

    public String getEmail()                    { return email; }
    public void setEmail(String email)          { this.email = email; }

    public String getPhone()                    { return phone; }
    public void setPhone(String phone)          { this.phone = phone; }

    public String getAddress()                  { return address; }
    public void setAddress(String address)      { this.address = address; }

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
