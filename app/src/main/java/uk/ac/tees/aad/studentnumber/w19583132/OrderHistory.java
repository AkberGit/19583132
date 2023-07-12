package uk.ac.tees.aad.studentnumber.w19583132;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import uk.ac.tees.aad.w19583132.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class OrderHistory extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OrderAdapter orderAdapter;
    private FirebaseFirestore fStore;
    private TextToSpeech textToSpeech;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);
        recyclerView = findViewById(R.id.recycler_view_order_history);

        fStore = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Query the Firestore database for the user's order history
        CollectionReference orderHistoryRef = fStore.collection("users").document(uid).collection("order_history");

        orderHistoryRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    QuerySnapshot querySnapshot = task.getResult();
                    if (querySnapshot != null) {
                        List<Order> orderHistory = new ArrayList<>();

                        for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                            try {
                                List<OrderItem> orderItems = new ArrayList<>();
                                Map<String, Object> foodMap = documentSnapshot.getData();
                                for (Map.Entry<String, Object> entry : foodMap.entrySet()) {
                                    String foodName = entry.getKey();
                                    Long quantityLong = (Long) entry.getValue();
                                    int quantity = quantityLong.intValue();
                                    OrderItem orderItem = new OrderItem(foodName, quantity);
                                    orderItems.add(orderItem);
                                }
                                String timestamp = documentSnapshot.getId();
                                Order order = new Order(orderItems, timestamp);
                                orderHistory.add(order);
                            }catch (Exception e){
                                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        // Create the adapter and set it to the RecyclerView
                        orderAdapter = new OrderAdapter(orderHistory);
                        recyclerView.setLayoutManager(new LinearLayoutManager(OrderHistory.this));
                        recyclerView.setAdapter(orderAdapter);
                    }
                } else {
                    Toast.makeText(OrderHistory.this, "Failed to retrieve order history", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }



    // Define the Order class
    public class Order {
        private List<OrderItem> orderItems;
        String timestamp;

        public Order(){}
        public Order(List<OrderItem> orderItems, String timestamp) {
            this.timestamp = timestamp;
            this.orderItems = orderItems;
        }

        public List<OrderItem> getOrderItems() {
            return orderItems;
        }
    }

    // Define the OrderItem class
    public class OrderItem {
        private String foodName;
        private int quantity;

        public OrderItem(String foodName, int quantity) {
            this.foodName = foodName;
            this.quantity = quantity;
        }

        public OrderItem(){}

        public String getFoodName() {
            return foodName;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    // Define the OrderAdapter
    public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

        private List<Order> orderHistory;

        public OrderAdapter(List<Order> orderHistory) {
            this.orderHistory = orderHistory;
        }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_card, parent, false);
            return new OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            Order order = orderHistory.get(position);
            String time = order.timestamp;
            holder.orderIdTextView.setText("Order Date: "+ time.substring(0,time.length()-9));
            holder.orderIdTextView.setTextSize(18);
            holder.complete.setVisibility(View.GONE);
            holder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                DocumentReference docRef = fStore.collection("users").document(uid).collection("order_history").document(order.timestamp);
                docRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Remove the deleted order from the orderHistory list
                        orderHistory.remove(holder.getAdapterPosition());

                        // Notify the adapter that an item has been removed at the specified position
                        notifyItemRemoved(holder.getAdapterPosition());
                        // Display a toast message if the delete operation is successful
                        Toast.makeText(getApplicationContext(), "Order deleted Successfully!", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Display a toast message if the delete operation fails
                        Toast.makeText(getApplicationContext(), "Failed to delete order: "+e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                }
            });


            holder.speaker.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String orderText = getOrderText(order);
                    speakOrder(orderText);
                }
            });

            holder.share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String orderText = getOrderText(order);
                    shareOrder(orderText);
                }
            });



            List<OrderItem> orderItems = order.getOrderItems();
            OrderItemAdapter orderItemAdapter = new OrderItemAdapter(orderItems);
            holder.orderItemRecyclerView.setAdapter(orderItemAdapter);
        }

        // Add the following method to the OrderAdapter class
        private void shareOrder(String text) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(intent, "Share Order"));
        }

        // Add the following methods to the OrderAdapter class
        private String getOrderText(Order order) {
            StringBuilder stringBuilder = new StringBuilder();
            for (OrderItem orderItem : order.getOrderItems()) {
                stringBuilder.append(orderItem.getFoodName()).append(", quantity: ").append(orderItem.getQuantity()).append(".\n");
            }
            return stringBuilder.toString();
        }

        private void speakOrder(String text) {
            if (textToSpeech != null) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        }

        @Override
        public int getItemCount() {
            return orderHistory.size();
        }

        public class OrderViewHolder extends RecyclerView.ViewHolder {
            public TextView orderIdTextView;
            public RecyclerView orderItemRecyclerView;
            public Button complete;
            public ImageView share, delete, speaker;

            public OrderViewHolder(View itemView) {
                super(itemView);
                orderIdTextView = itemView.findViewById(R.id.order_tableId);
                orderItemRecyclerView = itemView.findViewById(R.id.table_order_recycler_view);
                complete = itemView.findViewById(R.id.order_complete);
                share = itemView.findViewById(R.id.share_icon);
                delete = itemView.findViewById(R.id.delete_icon);
                speaker = itemView.findViewById(R.id.speaker_icon);
                orderItemRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));

                textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            // TextToSpeech initialization successful
                            int result = textToSpeech.setLanguage(Locale.US);
                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                // Language data is missing or the language is not supported
//                                Toast.makeText(getApplicationContext(),"TTS: Language not supported", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // TextToSpeech initialization failed
                            Toast.makeText(getApplicationContext(),"TTS: Initialization failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        }
    }

    // Define the OrderItemAdapter
    public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder> {

        private List<OrderItem> orderItems;

        public OrderItemAdapter(List<OrderItem> orderItems) {
            this.orderItems = orderItems;
        }

        @NonNull
        @Override
        public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_list_item, parent, false);
            return new OrderItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
            OrderItem orderItem = orderItems.get(position);

            holder.foodNameTextView.setText(orderItem.getFoodName());
            holder.quantityTextView.setText("Qty: " + orderItem.getQuantity());
        }

        @Override
        public int getItemCount() {
            return orderItems.size();
        }

        public class OrderItemViewHolder extends RecyclerView.ViewHolder {
            public TextView foodNameTextView;
            public TextView quantityTextView;

            public OrderItemViewHolder(View itemView) {
                super(itemView);
                foodNameTextView = itemView.findViewById(R.id.order_foodName);
                quantityTextView = itemView.findViewById(R.id.order_quantity);
            }
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Stop the text-to-speech when the back button is pressed and activity is finished
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }
}
