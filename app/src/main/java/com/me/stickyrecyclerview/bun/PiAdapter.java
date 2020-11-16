package com.me.stickyrecyclerview.bun;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.me.stickyrecyclerview.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by hvngoc on 11/14/20
 */
public class PiAdapter extends RecyclerView.Adapter<PiAdapter.MyViewHolder> implements StickyHeaders, StickyHeaders.ViewSetup {

    private static final String[] DICT = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
    };
    private static final int HEADER_ITEM = 123;

    private List<String> listData = new ArrayList<>();

    public PiAdapter() {
        for (int i = 65; i < 26 + 65; i++) {
            listData.add((char) i + " == ADS");
            for (int j = 0; j < 10; j++) {
                String itemText = getItemText((char) i);
                listData.add(itemText);
            }
        }
    }

    private String getItemText(char prefix) {
        int length = createRandom(10);
        StringBuilder builder = new StringBuilder();
        builder.append(prefix);
        for (int i = 0; i < length; i++) {
            int random = createRandom(51);
            builder.append(DICT[random]);
        }
        return builder.toString();
    }

    private int createRandom(int max) {
        Random random = new Random();
        return random.nextInt(max) % (max + 1);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
        return new MyViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        String item = listData.get(position);
        TextView textView = holder.itemView.findViewById(R.id.tvTitle);
        textView.setText(item);
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position % 11 == 0 && position > 0 ? HEADER_ITEM : super.getItemViewType(position);
    }

    @Override
    public boolean isStickyHeader(int position) {
        return getItemViewType(position) == HEADER_ITEM;
    }

    @Override
    public void setupStickyHeaderView(View stickyHeader) {
        ViewCompat.setElevation(stickyHeader, 10);
    }

    @Override
    public void teardownStickyHeaderView(View stickyHeader) {
        ViewCompat.setElevation(stickyHeader, 0);
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {

        public MyViewHolder(View itemView) {
            super(itemView);
        }
    }
}
