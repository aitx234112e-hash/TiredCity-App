package com.tiredcity.app.ui.support;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tiredcity.app.databinding.ActivityContactBinding;
import com.tiredcity.app.ui.base.BaseActivity;

import java.util.HashMap;
import java.util.Map;

public class ContactActivity extends BaseActivity {

    private ActivityContactBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContactBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        binding.layoutStore1.setOnClickListener(v ->
                openMaps("TiredCity 37 Hàng Hành, Hà Nội"));
        binding.layoutStore2.setOnClickListener(v ->
                openMaps("TiredCity 97 Hàng Gai, Hà Nội"));

        binding.btnSendMessage.setOnClickListener(v -> sendContactForm());
    }

    private void sendContactForm() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String message = binding.etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            binding.etName.setError("Nhập tên");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Nhập email");
            return;
        }
        if (TextUtils.isEmpty(message)) {
            binding.etMessage.setError("Nhập nội dung");
            return;
        }

        binding.btnSendMessage.setEnabled(false);

        Map<String, Object> contact = new HashMap<>();
        contact.put("name", name);
        contact.put("email", email);
        contact.put("message", message);
        contact.put("createdAt", com.google.firebase.Timestamp.now());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) contact.put("userId", user.getUid());

        FirebaseFirestore.getInstance().collection("contacts")
                .add(contact)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Đã gửi tin nhắn thành công!", Toast.LENGTH_SHORT).show();
                    binding.etMessage.setText("");
                    binding.btnSendMessage.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    binding.btnSendMessage.setEnabled(true);
                    Toast.makeText(this, "Lỗi khi gửi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openMaps(String query) {
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query)));
            startActivity(browserIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
