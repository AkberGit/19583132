package uk.ac.tees.aad.studentnumber.w19583132;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import uk.ac.tees.aad.w19583132.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class showOrders extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TableAdapter tableAdapter;
    private FirebaseFirestore fStore;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_orders);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);

        recyclerView = findViewById(R.id.recycler_view_orders);

        // Initialize the RecyclerView and the FirebaseFirestore and FirebaseAuth instances
        fStore= FirebaseFirestore.getInstance();

        CollectionReference tablesRef = fStore.collection("tables");
        tablesRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    List<Table> tablesList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Table table = document.toObject(Table.class);
                        tablesList.add(table);
                    }
                    tableAdapter = new TableAdapter(tablesList);
                    recyclerView.setLayoutManager(new GridLayoutManager(getApplicationContext(), 1));
                    recyclerView.setAdapter(tableAdapter);
                } else {
                    Log.d("showOrders", "Error getting tables: ", task.getException());
                }
            }
        });
    }


    public class TableAdapter extends RecyclerView.Adapter<OrderViewHolder>{
        private List<Table> allTables;

        public TableAdapter(List<Table> allTables){
            this.allTables = allTables;
        }

        @Override
        public OrderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_card, parent, false);
            return new OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(OrderViewHolder holder, int position) {
            Table table = allTables.get(position);
            if(!table.isAvailable()) {
                holder.tableId.setText(table.tableId);
                holder.complete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        updateUserData(table);
                    }
                });
                // Create an instance of the ItemAdapter
                ItemAdapter itemAdapter = new ItemAdapter(table.orders);
                // Set the ItemAdapter as the adapter for the ListView
                holder.cardRecyclerView.setAdapter(itemAdapter);
            }else
                holder.itemView.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return allTables.size();
        }
    }


    private void updateUserData(Table table){
        CollectionReference currTableRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(table.currentUId)
                .collection("curr_table");

        currTableRef.document("current")
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        updateTable(table.tableId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Failed to complete order", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void updateTable(String tableId) {
        DocumentReference tableRef = FirebaseFirestore.getInstance().collection("tables")
                .document(tableId);
        Map<String, Object> table = new HashMap<>();
        table.put("tableId", tableId);
        table.put("available", true);
        table.put("currentUId", "");
        table.put("orders", new HashMap<String, Integer>());

        tableRef.update(table).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(getApplicationContext(), "Order Completed Successfully!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), adminPage.class));
                finishAffinity();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Error: "+e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public class OrderViewHolder extends RecyclerView.ViewHolder {
        public TextView tableId;
        public RecyclerView cardRecyclerView;
        public Button complete;
        public ImageView share, delete, speaker;

        public OrderViewHolder(View itemView) {
            super(itemView);
            tableId = itemView.findViewById(R.id.order_tableId);
            cardRecyclerView = itemView.findViewById(R.id.table_order_recycler_view);
            complete = itemView.findViewById(R.id.order_complete);
            share = itemView.findViewById(R.id.share_icon);
            delete = itemView.findViewById(R.id.delete_icon);
            speaker = itemView.findViewById(R.id.speaker_icon);
            cardRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            cardRecyclerView.addItemDecoration(new DividerItemDecoration(getApplicationContext(),
                    DividerItemDecoration.VERTICAL));
            share.setVisibility(View.GONE);
            delete.setVisibility(View.GONE);
            speaker.setVisibility(View.GONE);
        }
    }


    public static class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
        private HashMap<String, Integer> itemsList;

        public ItemAdapter(HashMap<String, Integer> itemsList) {
            this.itemsList = itemsList;
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_list_item, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            String itemName = (String) itemsList.keySet().toArray()[position];
            int quantity = itemsList.get(itemName);

            holder.itemNameTextView.setText(itemName);
            holder.quantityTextView.setText("Qty: "+quantity);
        }

        @Override
        public int getItemCount() {
            return itemsList.size();
        }

        public class ItemViewHolder extends RecyclerView.ViewHolder {
            public TextView itemNameTextView;
            public TextView quantityTextView;

            public ItemViewHolder(View itemView) {
                super(itemView);
                itemNameTextView = itemView.findViewById(R.id.order_foodName);
                quantityTextView = itemView.findViewById(R.id.order_quantity);
            }
        }
    }

}