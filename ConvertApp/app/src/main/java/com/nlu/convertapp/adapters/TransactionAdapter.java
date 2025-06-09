package com.nlu.convertapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nlu.convertapp.R;
import com.nlu.convertapp.models.TransactionMessage;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<TransactionMessage> transactionList;
    private Context context;

    public TransactionAdapter(Context context, List<TransactionMessage> transactionList) {
        this.context = context;
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionMessage transaction = transactionList.get(position);
        holder.messageTextView.setText(transaction.getMessage());
    }

    @Override
    public int getItemCount() {
        return transactionList != null ? transactionList.size() : 0;
    }

    public void updateData(List<TransactionMessage> newTransactions) {
        this.transactionList = newTransactions;
        notifyDataSetChanged();
    }

    public void addTransaction(TransactionMessage transaction) {
        this.transactionList.add(0, transaction);
        notifyItemInserted(0);
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        TransactionViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }
} 