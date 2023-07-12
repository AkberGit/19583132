package uk.ac.tees.aad.studentnumber.w19583132;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import uk.ac.tees.aad.w19583132.R;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;

import org.checkerframework.checker.nullness.qual.NonNull;

public class tablesPage extends AppCompatActivity{
    RecyclerView recyclerView;
    private FirestoreRecyclerAdapter<Table, TableViewHolder> tableAdapter;
    private FirebaseFirestore fStore;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tables_page);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));


        // Initialize the RecyclerView and the FirebaseFirestore and FirebaseAuth instances
        fStore= FirebaseFirestore.getInstance();
        // Query the Firestore database for the user's notes, ordered by title in descending order
        Query query= fStore.collection("tables");
        // Create a FirestoreRecyclerOptions object to display the notes in the RecyclerView
        FirestoreRecyclerOptions<Table> allTables=new FirestoreRecyclerOptions.Builder<Table>()
                .setQuery(query,Table.class)
                .build();

        // Declare a FirestoreRecyclerAdapter to display notes in the RecyclerView
        tableAdapter = create_adapter(allTables);
        // Set the layout manager for the RecyclerView to a StaggeredGridLayoutManager with 2 columns
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        // Set the adapter for the RecyclerView to the FirestoreRecyclerAdapter
        recyclerView.setAdapter(tableAdapter);
        tableAdapter.startListening();
    }

    public FirestoreRecyclerAdapter<Table, TableViewHolder> create_adapter(FirestoreRecyclerOptions<Table> allTables){
        tableAdapter= new FirestoreRecyclerAdapter<Table, TableViewHolder>(allTables) {
            @Override
            protected void onBindViewHolder(@NonNull TableViewHolder holder, int i, @NonNull final Table table) {
                holder.tableId.setText(table.tableId);
                if(table.isAvailable()){
                    holder.tableStatus.setText("Available");
                    holder.tableStatus.setTextColor(Color.GREEN);
                }
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(table.isAvailable()){
                            Intent intent = new Intent(getApplicationContext(), menuPage.class);
                            intent.putExtra("tableId", table.tableId);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
            }

            @Override
            public TableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.table, parent, false);
                return new TableViewHolder(view);
            }
        };
        return tableAdapter;
    }

    public class TableViewHolder extends RecyclerView.ViewHolder {
        private TextView tableId, tableStatus;

        public TableViewHolder(View itemView) {
            super(itemView);
            tableId = itemView.findViewById(R.id.tableId);
            tableStatus = itemView.findViewById(R.id.tableStatus);
        }

    }
    @Override
    protected void onStop() {
        super.onStop();
        if(tableAdapter!=null)
            tableAdapter.stopListening(); // Stop listening for Firestore updates
    }

}