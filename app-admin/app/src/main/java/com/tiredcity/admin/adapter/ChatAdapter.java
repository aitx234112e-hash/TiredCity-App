package com.tiredcity.admin.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tiredcity.admin.R;
import com.tiredcity.admin.data.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Hien thi khung chat: bong bot ben trai, bong nguoi dung ben phai. */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    private final List<ChatMessage> items = new ArrayList<>();
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public void add(ChatMessage m) {
        items.add(m);
        notifyItemInserted(items.size() - 1);
    }

    public int size() {
        return items.size();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatMessage m = items.get(position);
        boolean user = m.from == ChatMessage.FROM_USER;

        h.text.setText(m.text);
        h.time.setText(timeFmt.format(new Date(m.time)));

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) h.bubble.getLayoutParams();
        lp.gravity = user ? Gravity.END : Gravity.START;
        h.bubble.setLayoutParams(lp);

        if (user) {
            h.bubble.setBackgroundResource(R.drawable.bg_bubble_user);
            h.text.setTextColor(ContextCompat.getColor(h.text.getContext(), R.color.white));
            h.time.setTextColor(0xCCFFFFFF);
        } else {
            h.bubble.setBackgroundResource(R.drawable.bg_bubble_bot);
            h.text.setTextColor(ContextCompat.getColor(h.text.getContext(), R.color.tc_text_primary));
            h.time.setTextColor(ContextCompat.getColor(h.time.getContext(), R.color.tc_text_secondary));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final LinearLayout bubble;
        final TextView text, time;

        VH(@NonNull View v) {
            super(v);
            bubble = v.findViewById(R.id.bubble);
            text = v.findViewById(R.id.tvText);
            time = v.findViewById(R.id.tvTime);
        }
    }
}
