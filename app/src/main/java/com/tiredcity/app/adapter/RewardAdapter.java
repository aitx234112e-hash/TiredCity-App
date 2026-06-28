package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Reward;
import java.util.List;

/** Adapter cho danh sách voucher ở trang "Ưu đãi". */
public class RewardAdapter extends RecyclerView.Adapter<RewardAdapter.ViewHolder> {

    public interface OnRewardClickListener {
        void onUseNow(Reward reward);
    }

    private final List<Reward> rewards;
    private OnRewardClickListener listener;

    public RewardAdapter(List<Reward> rewards) {
        this.rewards = rewards;
    }

    public void setOnRewardClickListener(OnRewardClickListener listener) {
        this.listener = listener;
    }

    public void updateRewards(List<Reward> newRewards) {
        rewards.clear();
        if (newRewards != null) rewards.addAll(newRewards);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reward, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reward reward = rewards.get(position);
        holder.ivBanner.setImageResource(reward.getBannerRes());
        holder.tvTitle.setText(reward.getTitle());
        holder.tvSubtitle.setText(reward.getSubtitle());
        holder.tvPoints.setText(String.valueOf(reward.getPoints()));
        holder.btnUseNow.setOnClickListener(v -> {
            if (listener != null) listener.onUseNow(reward);
        });
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUseNow(reward);
        });
    }

    @Override
    public int getItemCount() {
        return rewards != null ? rewards.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivBanner;
        final TextView tvTitle, tvSubtitle, tvPoints;
        final Button btnUseNow;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBanner   = itemView.findViewById(R.id.iv_reward_banner);
            tvTitle    = itemView.findViewById(R.id.tv_reward_title);
            tvSubtitle = itemView.findViewById(R.id.tv_reward_subtitle);
            tvPoints   = itemView.findViewById(R.id.tv_reward_points);
            btnUseNow  = itemView.findViewById(R.id.btn_use_now);
        }
    }
}
