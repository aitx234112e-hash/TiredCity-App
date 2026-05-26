package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Review;
import com.tiredcity.app.utils.DateUtils;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {
    private List<Review> reviews;

    public ReviewAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    public void updateReviews(List<Review> newReviews) {
        this.reviews = newReviews;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.tvUserName.setText(review.getUserName() != null ? review.getUserName() : "Ẩn danh");
        holder.tvComment.setText(review.getComment() != null ? review.getComment() : "");
        holder.rbRating.setRating(review.getRating());
        holder.tvDate.setText(review.getCreatedAt() != null
                ? DateUtils.formatDisplayDate(review.getCreatedAt()) : "");

        if (review.getUserAvatarUrl() != null && !review.getUserAvatarUrl().isEmpty()) {
            Glide.with(holder.civAvatar.getContext())
                    .load(review.getUserAvatarUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_person_placeholder)
                    .into(holder.civAvatar);
        } else {
            holder.civAvatar.setImageResource(R.drawable.ic_person_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return reviews != null ? reviews.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView civAvatar;
        TextView tvUserName, tvComment, tvDate;
        RatingBar rbRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            civAvatar  = itemView.findViewById(R.id.civ_avatar);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            rbRating   = itemView.findViewById(R.id.rb_rating);
            tvComment  = itemView.findViewById(R.id.tv_comment);
            tvDate     = itemView.findViewById(R.id.tv_date);
        }
    }
}
