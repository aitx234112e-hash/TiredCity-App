package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.ChatMessage;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ViewHolder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_BOT  = 1;

    private final List<ChatMessage> messages;

    public ChatMessageAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isFromUser() ? TYPE_USER : TYPE_BOT;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == TYPE_USER)
                ? R.layout.item_chat_user
                : R.layout.item_chat_bot;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage;
        private final int viewType;

        ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            tvMessage = itemView.findViewById(R.id.tv_message);
        }

        void bind(ChatMessage message) {
            if (tvMessage != null) {
                tvMessage.setText(message.getText());
            }
        }
    }
}
