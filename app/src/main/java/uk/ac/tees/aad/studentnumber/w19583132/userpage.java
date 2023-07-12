package uk.ac.tees.aad.studentnumber.w19583132;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import uk.ac.tees.aad.w19583132.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.Map;

public class userpage extends AppCompatActivity {
    MaterialCardView logout, order, order_history, locate;
    String tableId;
    ImageView profileImage;
    private FirebaseFirestore db;
    private static final int PICK_IMAGE_REQUEST = 101;
    private StorageReference storageReference;
    private static final String ACTION_SET_PROFILE_IMAGE = "com.example.myapplication.SET_PROFILE_IMAGE";

    // BroadcastReceiver to handle shared image
    private BroadcastReceiver profileImageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userpage);

        // Hide the ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);


        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        logout =  findViewById(R.id.logoutBtn);
        order = findViewById(R.id.order);
        locate = findViewById(R.id.locate);
        order_history = findViewById(R.id.order_history);
        profileImage = findViewById(R.id.myProfile);
        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        profileImageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_SET_PROFILE_IMAGE)) {
                    Uri sharedImageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (sharedImageUri != null) {
                        // Check if the user is logged in
                        FirebaseAuth mAuth = FirebaseAuth.getInstance();
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null && currentUser.getEmail()!=null &&!currentUser.getEmail().equals("admin@gmail.com")) {
                            // User is logged in, upload the profile image
                            uploadProfileImage(sharedImageUri);
                        } else {
                            // User is not logged in, redirect to login page
                            Toast.makeText(getApplicationContext(), "Please Create an Account!", Toast.LENGTH_SHORT).show();
                            Intent i = new Intent(getApplicationContext(), HomePage.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                            finish();                         }
                    }
                }
            }
        };

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(ACTION_SET_PROFILE_IMAGE);
        registerReceiver(profileImageReceiver, filter);

        handleSharedImageIntent(getIntent());



        order.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    if (isOfflineMode()) {
                        Intent intent = new Intent(getApplicationContext(), menuPage.class);
                        intent.putExtra("tableId", "offline");
                        startActivity(intent);
                        Toast.makeText(getApplicationContext(), "You are currently offline. Please connect to the internet to place an order.", Toast.LENGTH_SHORT).show();
                    } else {
                        if (tableId.equals("empty")) {
                            startActivity(new Intent(userpage.this, tablesPage.class));
                        } else {
                            Intent intent = new Intent(getApplicationContext(), menuPage.class);
                            intent.putExtra("tableId", tableId);
                            startActivity(intent);
                        }
                    }
                }catch (Exception e){
                    Toast.makeText(getApplicationContext(), "Please Wait!, fetching details...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        order_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(userpage.this, OrderHistory.class));
            }
        });

        locate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(userpage.this, LocateMe.class));
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.signOut();
                Toast.makeText(userpage.this, "Logout successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(userpage.this, HomePage.class));
                finishAffinity();
            }
        });
        try {
            loadImage();
        }catch (Exception e){
        }

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(userpage.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PICK_IMAGE_REQUEST);
                } else {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, PICK_IMAGE_REQUEST);
                }
            }
        });
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        fetchCurrentTable(userId, new OnFetchCurrentTableListener() {
            @Override
            public void onFetchSuccess(String currentTableId) {
                if (currentTableId.isEmpty()) {
                    tableId="empty";
                } else {
                    tableId = currentTableId;
                }
            }

            @Override
            public void onFetchFailure(String errorMessage) {
                // Failed to fetch current table
            }
        });

    }

    private boolean isOfflineMode() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo == null || !networkInfo.isConnected();
    }



    private void handleSharedImageIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null && type.startsWith("image/")) {
            Uri sharedImageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (sharedImageUri != null) {
                // Set the shared image as the profile image
                uploadProfileImage(sharedImageUri);
            }
        }
    }

    @Override
    protected void onDestroy() {
        // Unregister the BroadcastReceiver
        if (profileImageReceiver != null) {
            unregisterReceiver(profileImageReceiver);
        }
        super.onDestroy();
    }


    public void fetchCurrentTable(String userId, OnFetchCurrentTableListener listener) {
        // Get the reference to the curr_table document for the user
        DocumentReference currTableRef = db.collection("users").document(userId).collection("curr_table").document("current");

        currTableRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String currentTableId = document.getString("tableId");
                    listener.onFetchSuccess(currentTableId);
                } else {
                    // Document doesn't exist or no current table available
                    listener.onFetchSuccess(""); // Return an empty string or null value
                }
            } else {
                // Error occurred while fetching current table
                listener.onFetchFailure(task.getException().getMessage());
            }
        });
    }

    public interface OnFetchCurrentTableListener {
        void onFetchSuccess(String currentTableId);
        void onFetchFailure(String errorMessage);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PICK_IMAGE_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Launch gallery intent
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            } else {
                Toast.makeText(getApplicationContext(), "Permission denied. Unable to update profile picture.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadProfileImage(imageUri);
        }
    }

    private void uploadProfileImage(Uri imageUri) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();
        FirebaseStorage mStorage = FirebaseStorage.getInstance();

        String userId = mAuth.getCurrentUser().getUid();
        StorageReference imageRef = mStorage.getReference().child("profile_images/" + userId + ".jpg");

        // Check if the image already exists
//        imageRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
//            @Override
//            public void onSuccess(StorageMetadata metadata) {
//                // The image already exists, update the existing image
//                updateExistingImage(imageRef, imageUri);
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                // The image does not exist, upload the new image
                uploadNewImage(imageRef, imageUri);
//            }
//        });
    }

    private void updateProfileWithImage(Uri downloadUrl) {
        FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> user = new HashMap<>();
        user.put("profileImageUrl", downloadUrl.toString());

        mFirestore.collection("users").document(userId)
                .set(user, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Load the new profile image using Glide
                        Glide.with(getApplicationContext())
                                .load(downloadUrl)
                                .into(profileImage);
                        Toast.makeText(getApplicationContext(), "Successfully Updated!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadNewImage(StorageReference imageRef, Uri imageUri) {
        UploadTask uploadTask = imageRef.putFile(imageUri);
        Toast.makeText(getApplicationContext(), "Uploading...", Toast.LENGTH_SHORT).show();

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Get the download URL for the uploaded image
                imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri downloadUrl) {
                        // Update the profile with the new image URL
                        updateProfileWithImage(downloadUrl);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void loadImage(){
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();
        FirebaseStorage mStorage = FirebaseStorage.getInstance();

        String userId = mAuth.getCurrentUser().getUid();
        StorageReference imageRef = mStorage.getReference().child("profile_images/" + userId + ".jpg");

        // Check if the image already exists
        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri existingImageUri) {
                Glide.with(getApplicationContext())
                        .load(existingImageUri)
                        .into(profileImage);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
            }
        });
    }

}