package com.example.qluvx;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class NearbyUserAdapter extends RecyclerView.Adapter<NearbyUserAdapter.ViewHolder> {

    private static final int[] AVATAR_DRAWABLES = new int[]{
            R.drawable.img_avatar_0,
            R.drawable.img_avatar_1,
            R.drawable.img_avatar_2,
            R.drawable.img_avatar_3,
            R.drawable.img_avatar_4,
            R.drawable.img_avatar_5
    };

    private final Context context;
    private final List<HomeFragment.NearbyUser> users;

    public NearbyUserAdapter(Context context, List<HomeFragment.NearbyUser> users) {
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_nearby_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HomeFragment.NearbyUser user = users.get(position);
        holder.tvNameAge.setText(user.name + ", " + user.age);
        holder.tvDistance.setText(user.distance);
        holder.tvRoleTag.setText(user.role);

        int fallbackRes = AVATAR_DRAWABLES[position % AVATAR_DRAWABLES.length];
        if (holder.ivUserPhoto != null) {
            if (user.avatarUrl != null && !user.avatarUrl.isEmpty()) {
                Glide.with(context)
                        .load(user.avatarUrl)
                        .placeholder(fallbackRes)
                        .error(fallbackRes)
                        .centerCrop()
                        .into(holder.ivUserPhoto);
            } else {
                holder.ivUserPhoto.setImageResource(fallbackRes);
            }
        }

        // Color code Dating Role Tag
        if ("Top".equalsIgnoreCase(user.role)) {
            holder.tvRoleTag.setBackgroundColor(Color.parseColor("#E91E63"));
        } else if ("Bottom".equalsIgnoreCase(user.role)) {
            holder.tvRoleTag.setBackgroundColor(Color.parseColor("#7C4DFF"));
        } else {
            holder.tvRoleTag.setBackgroundColor(Color.parseColor("#00BFA5"));
        }

        holder.viewOnline.setVisibility(user.isOnline ? View.VISIBLE : View.INVISIBLE);

        holder.itemView.setOnClickListener(v ->
                Toast.makeText(context, "Opening chat with " + user.name + " (" + user.distance + ")", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserPhoto;
        TextView tvNameAge, tvDistance, tvRoleTag;
        View viewOnline;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserPhoto = itemView.findViewById(R.id.ivUserPhoto);
            tvNameAge = itemView.findViewById(R.id.tvNameAge);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvRoleTag = itemView.findViewById(R.id.tvRoleTag);
            viewOnline = itemView.findViewById(R.id.viewOnlineIndicator);
        }
    }
}
