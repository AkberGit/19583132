package uk.ac.tees.aad.studentnumber.w19583132;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import uk.ac.tees.aad.w19583132.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class menuPage extends AppCompatActivity implements DataCallback{

    private RecyclerView recyclerView;
    private Button orderButton;
    private List<FoodItem> foodItems;
    ProgressBar progressBar;
    HashMap<String, Integer> all_order;
    private FirebaseFirestore db;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_page);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);
        progressBar = findViewById(R.id.menu_progressBar);
        recyclerView = findViewById(R.id.menu_recycler_view);
        orderButton = findViewById(R.id.order_button);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        all_order = new HashMap<>();
        getFoodData(this);

        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        Intent intent = getIntent();
        String tableId = intent.getStringExtra("tableId");
        if(tableId.equals("offline")) orderButton.setVisibility(View.GONE);
        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the current user's UID
                if (user != null) {
                    String uid = user.getUid();
                    // Create a batch write instance
                    WriteBatch batch = db.batch();
                    progressBar.setVisibility(View.VISIBLE);
                    // Get the current timestamp
                    String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    // Store data into order history collection
                    CollectionReference orderHistoryRef = db.collection("users").document(uid).collection("order_history");
                    batch.set(orderHistoryRef.document(timeStamp), all_order, SetOptions.merge());

                    // Update the current table information
                    CollectionReference currTableRef = db.collection("users").document(uid).collection("curr_table");
                    batch.set(currTableRef.document("current"), Collections.singletonMap("tableId", tableId));

                    // Commit the batch write
                    batch.commit()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // Batch write successful
                                    updateTable(tableId, all_order);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Failed to execute batch write
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(getApplicationContext(), "Error storing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        });
    }


    /*
    * users -> uid -> add order_history, override curr_table
    * tables -> tableId -> update table
    * */
    private void updateTable(String tableId, HashMap<String, Integer> orders) {
        DocumentReference tableRef = db.collection("tables").document(tableId);
        Map<String, Object> table = new HashMap<>();
        table.put("tableId", tableId);
        table.put("available", false);
        table.put("currentUId", user.getUid());
        table.put("orders", orders);

        tableRef.update(table).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(getApplicationContext(), "Order Placed Successfully!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), userpage.class));
                finishAffinity();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "Error: "+e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onDataReceived(ArrayList<FoodItem> foodItems) {
        this.foodItems = foodItems;
        try{
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            MenuAdapter menuAdapter = new MenuAdapter(foodItems, all_order, getApplicationContext());
            recyclerView.setAdapter(menuAdapter);
        }catch (Exception e){
            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onError(String errorMessage) {
//        Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
    }



    public void getFoodData(DataCallback callback) {
        String url = "https://api.jsonbin.io/v3/b/6499c1e49d312622a375f850/latest";

        // Create a new RequestQueue with a custom cache and network stack
        Cache cache = new DiskBasedCache(getApplicationContext().getCacheDir(), 1024 * 1024); // 1MB cache size
        HttpStack httpStack = new HurlStack();
        Network network = new BasicNetwork(httpStack);
        RequestQueue queue = new RequestQueue(cache, network);
        queue.start();

        // Check if the cached response is available
        Cache.Entry cachedEntry = queue.getCache().get(url);
        if (cachedEntry != null && cachedEntry.data != null) {
            try {
                String cachedResponse = new String(cachedEntry.data, "UTF-8");
                // Parse the cached response and retrieve food items
                // ...
                JSONObject responseJson = new JSONObject(cachedResponse);
                JSONArray recordArray = responseJson.getJSONArray("record");
                ArrayList<FoodItem> foodItems = new ArrayList<>();
                for (int i = 0; i < recordArray.length(); i++) {
                    JSONObject foodObj = recordArray.getJSONObject(i);
                    String id = foodObj.getString("id");
                    String name = foodObj.getString("name");
                    String ingredients = foodObj.getString("ingredients");
                    String image = foodObj.getString("image");
                    double price = foodObj.getDouble("price");
                    FoodItem foodItem = new FoodItem(name, ingredients, id, image, price);
                    foodItems.add(foodItem);
                }
                callback.onDataReceived(foodItems);
            } catch (Exception e) {
                e.printStackTrace();
                callback.onError(e.getLocalizedMessage());
            }
        } else {
            // No cached response available, make a network request
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray recordArray = response.getJSONArray("record");
                                ArrayList<FoodItem> foodItems = new ArrayList<>();
                                for (int i = 0; i < recordArray.length(); i++) {
                                    JSONObject foodObj = recordArray.getJSONObject(i);
                                    String id = foodObj.getString("id");
                                    String name = foodObj.getString("name");
                                    String ingredients = foodObj.getString("ingredients");
                                    String image = foodObj.getString("image");
                                    double price = foodObj.getDouble("price");
                                    FoodItem foodItem = new FoodItem(name, ingredients, id, image, price);
                                    foodItems.add(foodItem);
                                }
                                callback.onDataReceived(foodItems);
                            } catch (Exception e) {
                                e.printStackTrace();
                                callback.onError(e.getLocalizedMessage());
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            callback.onError(error.getLocalizedMessage());
                        }
                    });

            // Set cache control headers to enable caching
            jsonObjectRequest.setShouldCache(true);

            // Add the request to the RequestQueue
            queue.add(jsonObjectRequest);
        }
    }

}