package com.example.recyclerviewsimple;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;

public class FlowerAdapter extends RecyclerView.Adapter<FlowerAdapter.FlowerViewHolder> {
    static class FlowerViewHolder extends RecyclerView.ViewHolder {
        TextView        flowerTextView;
        public FlowerViewHolder(@NonNull View aView) {
            super(aView);
            flowerTextView = aView.findViewById(R.id.flower_text);
        }

    }

    private final List<String>    flowerList = Arrays.asList(
            "Lily",
            "Poppy",
            "Sunflower",
            "Freesia",
            "Daisy",
            "Rose",
            "Daffodil",
            "Lavender",
            "Peony",
            "Lilac",
            "Dahlia",
            "Tulip",
            "Dandelion"
    );
//    private final List<Integer> mSeekersResIdList = Arrays.asList(
//            R.drawable.num0, R.drawable.num1, R.drawable.num2, R.drawable.num3,
//            R.drawable.num4, R.drawable.num5, R.drawable.num6, R.drawable.num7,
//            R.drawable.num8, R.drawable.num9);

    @NonNull
    @Override
    public FlowerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.flower_item, parent, false);
        return new FlowerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FlowerViewHolder holder, int position) {
        holder.flowerTextView.setText(flowerList.get(position));
    }

    @Override
    public int getItemCount() {
        return flowerList.size();
    }
}
