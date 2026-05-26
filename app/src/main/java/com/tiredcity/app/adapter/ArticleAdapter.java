package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Article;
import com.tiredcity.app.utils.DateUtils;
import java.util.List;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ViewHolder> {
    private List<Article> articles;
    private OnArticleClickListener listener;

    public interface OnArticleClickListener {
        void onArticleClick(Article article);
    }

    public ArticleAdapter(List<Article> articles) {
        this.articles = articles;
    }

    public void setOnArticleClickListener(OnArticleClickListener listener) {
        this.listener = listener;
    }

    public void updateArticles(List<Article> newArticles) {
        this.articles = newArticles;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_article, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Article article = articles.get(position);
        String title = article.getTitleVi() != null ? article.getTitleVi() : article.getTitle();
        holder.tvTitle.setText(title);
        holder.tvAuthor.setText(article.getAuthor() != null ? article.getAuthor() : "Tired City");
        holder.tvDate.setText(article.getPublishedAt() != null
                ? DateUtils.formatDisplayDate(article.getPublishedAt()) : "");

        if (article.getImageUrl() != null && !article.getImageUrl().isEmpty()) {
            Glide.with(holder.ivImage.getContext())
                    .load(article.getImageUrl())
                    .centerCrop()
                    .placeholder(R.color.bg_subtle)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.color.bg_subtle);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onArticleClick(article);
        });
    }

    @Override
    public int getItemCount() {
        return articles != null ? articles.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle, tvAuthor, tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage  = itemView.findViewById(R.id.iv_article_image);
            tvTitle  = itemView.findViewById(R.id.tv_article_title);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvDate   = itemView.findViewById(R.id.tv_date);
        }
    }
}
