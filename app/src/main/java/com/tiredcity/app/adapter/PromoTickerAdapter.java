package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.tiredcity.app.R;
import java.util.List;

/** ViewPager2 adapter for the sliding yellow promo ticker bar. */
public class PromoTickerAdapter extends RecyclerView.Adapter<PromoTickerAdapter.VH> {

    private final List<String> messages;

    public PromoTickerAdapter(List<String> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_promo_ticker, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        h.tvText.setText(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvText;

        VH(@NonNull View v) {
            super(v);
            tvText = v.findViewById(R.id.tv_ticker_text);
        }
    }
}
