package com.tpeterb.securechatclient.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.tpeterb.securechatclient.R;
import com.tpeterb.securechatclient.ui.listener.SearchListItemOnClickListener;

import java.util.Objects;

public class CustomSearchAdapter extends ArrayAdapter<String> {

    private final SearchListItemOnClickListener searchListItemOnClickListener;

    public CustomSearchAdapter(Context context, SearchListItemOnClickListener searchListItemOnClickListener) {
        super(context, android.R.layout.simple_list_item_1);
        this.searchListItemOnClickListener = searchListItemOnClickListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        TextView textView = view.findViewById(android.R.id.text1);

        view.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.chat_list_page_search_result_background));
        textView.setTextColor(ContextCompat.getColor(getContext(), R.color.chat_list_search_result_text));

        String username = getItem(position);

        textView.setOnClickListener(view1 -> {
            if (Objects.nonNull(searchListItemOnClickListener)) {
                searchListItemOnClickListener.onSearchListItemClick(username);
            }
        });

        return view;
    }
}
