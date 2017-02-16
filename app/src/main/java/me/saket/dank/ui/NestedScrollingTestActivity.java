package me.saket.dank.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.DankActivity;
import me.saket.dank.R;
import me.saket.dank.utils.RecyclerViewArrayAdapter;

public class NestedScrollingTestActivity extends DankActivity {

    @BindView(R.id.recyclerView) RecyclerView list;

    public static void start(Context context) {
        context.startActivity(new Intent(context, NestedScrollingTestActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nested_scrolling_test);
        ButterKnife.bind(this);
        findAndSetupToolbar(true);

        list.setLayoutManager(new LinearLayoutManager(this));

        List<String> strings = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            strings.add("Item #" + i);
        }
        list.setAdapter(new ListAdapter(strings));
    }

    static class ListAdapter extends RecyclerViewArrayAdapter<String, ListAdapter.ItemVH> {
        public ListAdapter(@Nullable List<String> items) {
            super(items);
        }

        @Override
        protected ItemVH onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
            return ItemVH.createVH(inflater, parent);
        }

        @Override
        public void onBindViewHolder(ItemVH holder, int position) {
            holder.bind(getItem(position));
        }

        static class ItemVH extends RecyclerView.ViewHolder{
            public static ItemVH createVH(LayoutInflater inflater, ViewGroup parent) {
                return new ItemVH(inflater.inflate(android.R.layout.simple_list_item_1, parent, false));
            }

            public ItemVH(View itemView) {
                super(itemView);
                //itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.blue_gray_600));
            }

            public void bind(String item) {
                ButterKnife.<TextView>findById(this.itemView, android.R.id.text1).setText(item);
            }
        }

    }

}
