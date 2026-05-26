package com.tiredcity.app.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.model.Order;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.OrderRepository;
import com.tiredcity.app.databinding.ActivityPaymentBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.PriceUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

public class PaymentActivity extends BaseActivity {

    private ActivityPaymentBinding binding;
    private OrderRepository orderRepository;
    private CartLocalStore cartLocalStore;
    private static final double SHIPPING_FEE = 30_000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        orderRepository = new OrderRepository(ApiClient.getApiService(preferenceManager.getToken()));
        cartLocalStore  = new CartLocalStore(this);

        setupOrderSummary();

        binding.btnPlaceOrder.setOnClickListener(v -> {
            String address = binding.tvRecipientAddress.getText().toString();
            String method  = getSelectedPaymentMethod();
            placeOrder(address, method);
        });
    }

    private void setupOrderSummary() {
        List<CartItem> items = cartLocalStore.getCartItems();

        double subtotal = 0;
        for (CartItem item : items) subtotal += item.getSubtotal();
        double total = subtotal + SHIPPING_FEE;

        binding.tvSubtotal.setText(PriceUtils.format(subtotal));
        binding.tvShippingFee.setText(PriceUtils.format(SHIPPING_FEE));
        binding.tvTotal.setText(PriceUtils.format(total));

        // Prefill recipient info from saved profile
        com.tiredcity.app.data.model.UserProfile user = preferenceManager.getUser();
        if (user != null) {
            binding.tvRecipientName.setText(user.getName());
            binding.tvRecipientPhone.setText(user.getPhone());
            binding.tvRecipientAddress.setText(user.getAddress());
        }
    }

    private String getSelectedPaymentMethod() {
        int id = binding.rgPaymentMethod.getCheckedRadioButtonId();
        if (id == binding.rbBankTransfer.getId()) return "BANK_TRANSFER";
        if (id == binding.rbMomo.getId())         return "MOMO";
        return "COD";
    }

    private void placeOrder(String address, String paymentMethod) {
        binding.btnPlaceOrder.setEnabled(false);
        List<CartItem> items = cartLocalStore.getCartItems();

        Order order = new Order();
        order.setItems(items);
        order.setShippingAddress(address);
        order.setPaymentMethod(paymentMethod);

        double total = 0;
        for (CartItem item : items) total += item.getSubtotal();
        order.setTotalPrice(total + SHIPPING_FEE);

        orderRepository.createOrder(order).enqueue(new Callback<ApiResponse<Order>>() {
            @Override
            public void onResponse(Call<ApiResponse<Order>> call, Response<ApiResponse<Order>> response) {
                binding.btnPlaceOrder.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    cartLocalStore.clearCart();
                    Intent intent = new Intent(PaymentActivity.this, OrderSuccessActivity.class);
                    intent.putExtra("order_id", response.body().getData().getId());
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(PaymentActivity.this, "Đặt hàng thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Order>> call, Throwable t) {
                binding.btnPlaceOrder.setEnabled(true);
                Toast.makeText(PaymentActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
