package com.tiredcity.admin.ui.shipping;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.tiredcity.admin.R;
import com.tiredcity.admin.adapter.ShippingConfigAdapter;
import com.tiredcity.admin.data.model.PickupPoint;
import com.tiredcity.admin.data.model.ShippingConfig;
import com.tiredcity.admin.data.model.ShippingSettings;
import com.tiredcity.admin.databinding.ActivityShippingConfigBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Man hinh Van chuyen gom 3 phan:
 *  1. Dia chi lay hang  — 2 kho co dinh, chon kho dang hoat dong.
 *  2. Don vi van chuyen — 3 goi cuoc SPX Express (economy / standard / express).
 *  3. Thiet lap chung   — nguong freeship + ghi chu shipper.
 * Cac goi SPX va thiet lap chung tu tao ban ghi mau neu Firestore chua co.
 */
public class ShippingConfigActivity extends AppCompatActivity {

    private static final String COLLECTION = "shipping_configs";
    private static final String[] FIXED_IDS = {"economy", "standard", "express"};

    private ActivityShippingConfigBinding binding;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ShippingConfigAdapter adapter;

    private ShippingSettings settings = ShippingSettings.defaults();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShippingConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new ShippingConfigAdapter(this::openEdit);
        binding.rvShippingConfigs.setLayoutManager(new LinearLayoutManager(this));
        binding.rvShippingConfigs.setAdapter(adapter);

        buildPickupCards();
        binding.btnSaveGeneral.setOnClickListener(v -> saveGeneral());

        binding.swipeRefresh.setOnRefreshListener(() -> {
            loadSpx();
            loadSettings();
        });

        loadSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Chi tai lai SPX (bottom sheet co the vua sua) — tranh ghi de o dang nhap thiet lap chung.
        loadSpx();
    }

    // ─────────────────────────── Section 2: SPX ───────────────────────────

    private void loadSpx() {
        binding.swipeRefresh.setRefreshing(true);
        db.collection(COLLECTION).get()
                .addOnSuccessListener(this::onSpxLoaded)
                .addOnFailureListener(e -> {
                    binding.swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, getString(R.string.load_error, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private void onSpxLoaded(QuerySnapshot snap) {
        Map<String, DocumentSnapshot> byId = new HashMap<>();
        for (DocumentSnapshot d : snap.getDocuments()) byId.put(d.getId(), d);

        List<ShippingConfig> configs = new ArrayList<>();
        List<ShippingConfig> missing = new ArrayList<>();

        for (String id : FIXED_IDS) {
            DocumentSnapshot d = byId.get(id);
            if (d == null || !d.exists()) {
                ShippingConfig def = ShippingConfig.defaultFor(id);
                configs.add(def);
                missing.add(def);
            } else {
                String name = d.getString("name");
                Double price = d.getDouble("price");
                String estimate = d.getString("estimate");
                Boolean active = d.getBoolean("isActive");
                configs.add(new ShippingConfig(
                        id,
                        name != null ? name : ShippingConfig.defaultFor(id).name,
                        price != null ? price : 0,
                        estimate != null ? estimate : "",
                        active == null || active));
            }
        }

        adapter.submit(configs);
        binding.swipeRefresh.setRefreshing(false);

        if (!missing.isEmpty()) seedMissing(missing);
    }

    /** Tu tao ban ghi mau cho cac goi chua ton tai tren Firestore. */
    private void seedMissing(List<ShippingConfig> missing) {
        WriteBatch batch = db.batch();
        for (ShippingConfig c : missing) {
            batch.set(db.collection(COLLECTION).document(c.id), c.toMap(), SetOptions.merge());
        }
        batch.commit();
    }

    private void openEdit(ShippingConfig config) {
        ShippingConfigEditBottomSheet sheet = ShippingConfigEditBottomSheet.newInstance(config);
        sheet.setOnSavedListener(this::loadSpx);
        sheet.show(getSupportFragmentManager(), "shipping_config_edit");
    }

    // ─────────────────────── Section 1: Kho lay hang ──────────────────────

    /** Dung 2 the kho co dinh, gan id lam tag de xu ly chon. */
    private void buildPickupCards() {
        binding.containerPickup.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (PickupPoint p : PickupPoint.defaults(this)) {
            View card = inflater.inflate(R.layout.item_pickup_point, binding.containerPickup, false);
            ((TextView) card.findViewById(R.id.tvPickupName)).setText(p.name);
            ((TextView) card.findViewById(R.id.tvPickupAddress)).setText(p.address);
            card.findViewById(R.id.tvPickupDefault).setVisibility(
                    p.id.equals(PickupPoint.defaultId()) ? View.VISIBLE : View.GONE);
            card.setTag(p.id);
            card.setOnClickListener(v -> onPickupSelected((String) v.getTag()));
            binding.containerPickup.addView(card);
        }
        renderPickupSelection();
    }

    private void onPickupSelected(String id) {
        if (id == null || id.equals(settings.activePickupId)) return;
        settings.activePickupId = id;
        renderPickupSelection();
        saveSettings(false);
    }

    /** Danh dau kho dang chon: viền đỏ + icon check; kho mac dinh giu nhan "Mặc định". */
    private void renderPickupSelection() {
        String activeId = settings.activePickupId;
        for (int i = 0; i < binding.containerPickup.getChildCount(); i++) {
            View card = binding.containerPickup.getChildAt(i);
            boolean active = activeId != null && activeId.equals(card.getTag());
            card.findViewById(R.id.ivPickupCheck).setVisibility(active ? View.VISIBLE : View.GONE);
            if (card instanceof MaterialCardView) {
                MaterialCardView mc = (MaterialCardView) card;
                mc.setStrokeColor(ContextCompat.getColor(this, active ? R.color.tc_brand : R.color.tc_stroke));
                mc.setStrokeWidth(dp(active ? 2 : 1));
            }
        }
    }

    // ─────────────────────── Section 3: Thiet lap chung ───────────────────

    private void loadSettings() {
        db.collection(ShippingSettings.COLLECTION).document(ShippingSettings.DOC_ID).get()
                .addOnSuccessListener(d -> {
                    settings = ShippingSettings.from(d);
                    bindSettings();
                    if (d == null || !d.exists()) saveSettings(false); // seed ban ghi mau
                })
                .addOnFailureListener(e -> {
                    settings = ShippingSettings.defaults();
                    bindSettings();
                });
    }

    private void bindSettings() {
        renderPickupSelection();
        binding.etFreeship.setText(settings.freeshipThreshold > 0
                ? String.valueOf((long) settings.freeshipThreshold) : "");
        binding.etShipperNote.setText(settings.shipperNote);
    }

    private void saveGeneral() {
        settings.freeshipThreshold = parseAmount(binding.etFreeship.getText());
        settings.shipperNote = binding.etShipperNote.getText() != null
                ? binding.etShipperNote.getText().toString().trim() : "";
        saveSettings(true);
    }

    private void saveSettings(boolean showToast) {
        db.collection(ShippingSettings.COLLECTION).document(ShippingSettings.DOC_ID)
                .set(settings.toMap(), SetOptions.merge())
                .addOnSuccessListener(x -> {
                    if (showToast) Toast.makeText(this, R.string.scfg_general_saved, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.scfg_save_error, e.getMessage()), Toast.LENGTH_LONG).show());
    }

    private static double parseAmount(CharSequence text) {
        if (TextUtils.isEmpty(text)) return 0;
        String digits = text.toString().replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try {
            return Double.parseDouble(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
