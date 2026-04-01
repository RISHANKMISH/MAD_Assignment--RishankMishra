package com.example.photo_gallery;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.io.File;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private Context context;
    private List<ImageItem> imageList;
    private OnImageClickListener listener;

    public interface OnImageClickListener {
        void onImageClick(ImageItem imageItem, int position);
    }

    public ImageAdapter(Context context, List<ImageItem> imageList, OnImageClickListener listener) {
        this.context = context;
        this.imageList = imageList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageItem imageItem = imageList.get(position);

        Object loadSource;
        String path = imageItem.getPath();
        if (path.startsWith("content://")) {
            loadSource = Uri.parse(path);
        } else {
            loadSource = new File(path);
        }

        Glide.with(context)
                .load(loadSource)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.ic_gallery)
                .error(R.drawable.ic_gallery)
                .centerCrop()
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(imageItem, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageList != null ? imageList.size() : 0;
    }

    public void updateImageList(List<ImageItem> newList) {
        this.imageList = newList;
        notifyDataSetChanged();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivImage);
        }
    }
}
