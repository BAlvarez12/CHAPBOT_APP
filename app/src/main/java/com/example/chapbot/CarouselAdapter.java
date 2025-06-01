// CarouselAdapter.java
package com.example.chapbot.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chapbot.R;

import java.util.List;

public class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder> {

    private final List<String> listaBase64;

    public CarouselAdapter(List<String> listaBase64) {
        this.listaBase64 = listaBase64;
    }

    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carousel, parent, false);
        return new CarouselViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        String base64 = listaBase64.get(position);
        byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
        Bitmap bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        holder.imgCarouselItem.setImageBitmap(bmp);
    }

    @Override
    public int getItemCount() {
        return listaBase64.size();
    }

    static class CarouselViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCarouselItem;

        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCarouselItem = itemView.findViewById(R.id.imgCarouselItem);
        }
    }
}
