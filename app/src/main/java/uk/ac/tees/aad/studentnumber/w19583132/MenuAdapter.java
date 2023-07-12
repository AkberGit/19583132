package uk.ac.tees.aad.studentnumber.w19583132;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import uk.ac.tees.aad.w19583132.R;

import java.util.HashMap;
import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {
    private List<FoodItem> foodItems;
    private Context context;
    HashMap<String, Integer> all_order;

    public MenuAdapter(List<FoodItem> foodItems, HashMap<String, Integer> all_order, Context context){
        this.foodItems = foodItems;
        this.context = context;
        this.all_order = all_order;
    }
    @Override
    public MenuAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_food_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MenuAdapter.ViewHolder holder, int position) {
        FoodItem foodItem = foodItems.get(position);
        String image = foodItem.getImage();
        holder.itemName.setText(foodItem.getName());
        holder.price.setText("Price: $"+foodItem.getPrice());
        holder.itemDescription.setText(foodItem.getDescription()+" "+foodItem.getDescription());
        int imageResId = context.getResources().getIdentifier("@drawable/"+
                        image.substring(0,image.length()-4), "drawable",
                context.getPackageName());
        Glide.with(context).load(imageResId).into(holder.itemImage);

        // Set the quantity for each item
        holder.curr_quantity = all_order.containsKey(foodItem.getName()) ? all_order.get(foodItem.getName()) : 0;
        holder.quantity.setText(String.valueOf(holder.curr_quantity));
        // Minus button click listener
        holder.minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.curr_quantity > 0) {
                    holder.curr_quantity--;
                    holder.quantity.setText(String.valueOf(holder.curr_quantity));
                    all_order.put(foodItem.getName(), holder.curr_quantity);
                    if(holder.curr_quantity==0) all_order.remove(foodItem.getName());
                }
            }
        });

        // Plus button click listener
        holder.plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.curr_quantity++;
                holder.quantity.setText(String.valueOf(holder.curr_quantity));
                all_order.put(foodItem.getName(), holder.curr_quantity);
            }
        });

    }

    @Override
    public int getItemCount() {
        return foodItems.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder{
        private ImageView itemImage;
        private TextView itemName, itemDescription, quantity, price;
        private TextView minusButton, plusButton;
        int curr_quantity =0;

        public ViewHolder(View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.item_image);
            itemName = itemView.findViewById(R.id.item_name);
            itemDescription = itemView.findViewById(R.id.item_description);
            price = itemView.findViewById(R.id.item_price);
            quantity = itemView.findViewById(R.id.quantity_text);
            minusButton = itemView.findViewById(R.id.minus_button);
            plusButton = itemView.findViewById(R.id.plus_button);
        }

    }
}
