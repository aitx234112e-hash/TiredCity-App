package com.tiredcity.app.ui.settings;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import com.tiredcity.app.databinding.ActivityPinBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.Constants;

public class PinActivity extends BaseActivity {

    private ActivityPinBinding binding;
    private String mode; // "SETUP" or "VERIFY"
    private String firstPin; // For setup confirmation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPinBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mode = getIntent().getStringExtra("MODE");
        if (mode == null) mode = "VERIFY";

        setupUI();

        binding.btnConfirm.setVisibility(View.GONE); // Ẩn nút xác nhận, tự động kiểm tra khi đủ 4 số

        binding.etPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 4) {
                    handlePin();
                }
            }
        });

        binding.btnConfirm.setOnClickListener(v -> handlePin());
    }

    private void setupUI() {
        if ("SETUP".equals(mode)) {
            binding.tvPinTitle.setText("Thiết lập mã PIN");
            binding.tvPinSubtitle.setText("Nhập mã PIN 4 số để bảo mật ứng dụng");
        } else {
            binding.tvPinTitle.setText("Nhập mã PIN");
            binding.tvPinSubtitle.setText("Vui lòng nhập mã PIN để tiếp tục");
        }
    }

    private void handlePin() {
        String pin = binding.etPin.getText().toString();
        if (pin.length() < 4) {
            Toast.makeText(this, "Mã PIN phải có 4 chữ số", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("SETUP".equals(mode)) {
            if (firstPin == null) {
                firstPin = pin;
                binding.etPin.setText("");
                binding.tvPinTitle.setText("Xác nhận mã PIN");
                binding.tvPinSubtitle.setText("Nhập lại mã PIN một lần nữa");
            } else {
                if (pin.equals(firstPin)) {
                    preferenceManager.setPin(pin);
                    preferenceManager.setToggle(Constants.KEY_PIN_UNLOCK, true);
                    Toast.makeText(this, "Thiết lập mã PIN thành công", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "Mã PIN không khớp, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                    firstPin = null;
                    binding.etPin.setText("");
                    setupUI();
                }
            }
        } else {
            String savedPin = preferenceManager.getPin();
            if (pin.equals(savedPin)) {
                String purpose = getIntent().getStringExtra("PURPOSE");
                if ("TURN_OFF".equals(purpose)) {
                    preferenceManager.setToggle(Constants.KEY_PIN_UNLOCK, false);
                    preferenceManager.setPin("");
                    Toast.makeText(this, "Đã tắt mã PIN", Toast.LENGTH_SHORT).show();
                }
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Mã PIN không chính xác", Toast.LENGTH_SHORT).show();
                binding.etPin.setText("");
            }
        }
    }
}
