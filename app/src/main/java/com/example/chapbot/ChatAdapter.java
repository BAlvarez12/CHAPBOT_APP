package com.example.chapbot;

import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {
    public interface Listener { void onClick(String userId); }

    private final List<UserMenu> items;
    private final Listener listener;

    public ChatAdapter(List<UserMenu> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cuadrado_menu, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int pos) {
        UserMenu u = items.get(pos);
        holder.tvName.setText(u.name);
        byte[] decoded = Base64.decode(u.photoBase64, Base64.DEFAULT);
        holder.img.setImageBitmap(BitmapFactory.decodeByteArray(decoded, 0, decoded.length));
        holder.itemView.setOnClickListener(v -> listener.onClick(u.userId));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tvName;

        VH(View v) {
            super(v);
            img    = v.findViewById(R.id.imgQuickProfile);
            tvName = v.findViewById(R.id.tvQuickName);
        }
    }
}

class UserMenu {
    String userId, name, photoBase64;

    UserMenu(String id, String name, String photoBase64) {
        this.userId = id;
        this.name = name;
        this.photoBase64 = photoBase64;
    }
}
