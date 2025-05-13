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
        
        holder.senderNameTextView.setText(transaction.getSenderName());
        holder.amountTextView.setText(transaction.getFormattedAmount());
        holder.dateTextView.setText(transaction.getFormattedDate());
        
        // Display bank and account number info
        String bankAccountInfo = transaction.getBankName() + " - " + 
                transaction.getAccountNumber();
        holder.bankAccountTextView.setText(bankAccountInfo);
        
        // Show message if exists
        if (transaction.getMessage() != null && !transaction.getMessage().isEmpty()) {
            holder.messageTextView.setText(transaction.getMessage());
            holder.messageTextView.setVisibility(View.VISIBLE);
        } else {
            holder.messageTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public void updateData(List<TransactionMessage> newTransactions) {
        this.transactionList = newTransactions;
        notifyDataSetChanged();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView senderNameTextView;
        TextView amountTextView;
        TextView dateTextView;
        TextView bankAccountTextView;
        TextView messageTextView;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            senderNameTextView = itemView.findViewById(R.id.senderNameTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            bankAccountTextView = itemView.findViewById(R.id.bankAccountTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }
} 