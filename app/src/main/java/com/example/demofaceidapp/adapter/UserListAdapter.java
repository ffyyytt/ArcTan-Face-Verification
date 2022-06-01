package com.example.demofaceidapp.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demofaceidapp.R;
import com.example.demofaceidapp.data.User;
import com.example.demofaceidapp.databinding.ViewUserListItemBinding;

import java.util.List;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> {

    private List<User> data;
    private Listener listener;

    public UserListAdapter(List<User> data, Listener listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ViewUserListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = data.get(position);
        holder.binding.tvName.setText(user.name);
        if (user.faces == null || user.faces.isEmpty()) {
            holder.binding.tvStatus.setText("Face data: Not ready");
            holder.binding.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
        } else {
            holder.binding.tvStatus.setText("Face data: Available");
            holder.binding.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.green));
        }
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ViewUserListItemBinding binding;

        public ViewHolder(ViewUserListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            this.binding.btnAdd.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAdd(data.get(getAdapterPosition()).id);
                }
            });


            this.binding.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(data.get(getAdapterPosition()).id);
                    notifyItemRemoved(getAdapterPosition());
                }
            });
        }


    }

    public interface Listener {
        void onAdd(int userId);
        void onDelete(int userId);
    }

}
