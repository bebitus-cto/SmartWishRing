package com.manridy.sdkdemo_mrd2019.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.manridy.sdkdemo_mrd2019.R;

import java.util.ArrayList;

public class ParseDataAdapter extends RecyclerView.Adapter<ParseDataAdapter.ParseDataViewHolder> {

    private ArrayList<String> parseList = new ArrayList<>();

    @NonNull
    @Override
    public ParseDataViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ParseDataViewHolder(
                LayoutInflater.from(
                        parent.getContext()).inflate(
                        R.layout.item_content,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ParseDataViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return parseList.size();
    }

    public void emit(ArrayList<String> data) {
        parseList.clear();
        parseList.addAll(data);
        notifyDataSetChanged();
    }

    public void emit(String body) {
        parseList.add(0,body);
        notifyDataSetChanged();
    }

    public void clear(){
        parseList.clear();
        notifyDataSetChanged();
    }

    class ParseDataViewHolder extends RecyclerView.ViewHolder {

        private TextView tv_body;

        public ParseDataViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_body = itemView.findViewById(R.id.tv_body);
        }

        void bind(int position) {
            Log.i("MrdRead", "adapter body is " + parseList.get(position));
            tv_body.setText(parseList.get(position));
        }
    }
}
