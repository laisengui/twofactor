package cn.lsg.twofactor;


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private List<Item> mItemList;
    private Context mContext;

    public ItemAdapter(List<Item> itemList) {
        mItemList = itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = mItemList.get(position);
        holder.nameTextView.setText(item.getName());
        holder.userTextView.setText(item.getUser());


        holder.itemView.setOnClickListener(v->{
            Intent intent = new Intent(mContext, DetailActivity.class);
            // 传递数据到详情页
            intent.putExtra("id", item.getId());
            intent.putExtra("name", item.getName());
            intent.putExtra("user", item.getUser());
            intent.putExtra("secretKey", item.getSecretKey());
            intent.putExtra("position", position);
            mContext.startActivity(intent);
        });
    }

    public void updateData(List<Item> newItems) {
        mItemList.clear();
        mItemList.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView userTextView;
        Button detailButton;

        ViewHolder(View view) {
            super(view);
            nameTextView = view.findViewById(R.id.tv_name);
            userTextView = view.findViewById(R.id.tv_user);
        }
    }
}
