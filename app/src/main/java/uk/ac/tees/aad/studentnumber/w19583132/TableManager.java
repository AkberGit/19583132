package uk.ac.tees.aad.studentnumber.w19583132;

import android.content.Context;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class TableManager {
    private FirebaseFirestore db;
    private Context context;

    public TableManager(Context context) {
        this.context = context;
        // Initialize Firestore instance
        db = FirebaseFirestore.getInstance();
    }

    public void showAddTableDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Add Table");
        // Set up the input
        final EditText input = new EditText(context);
        builder.setView(input);
        // Set up the buttons
        builder.setPositiveButton("Add", (dialog, which) -> {
            String tableName = input.getText().toString().trim();
            if (!TextUtils.isEmpty(tableName)) {
                checkAndCreateTable(tableName);
            }
        }).setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        // Show the dialog
        builder.show();
    }

    public void showRemoveTableDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Remove Table");

        // Set up the input
        final EditText input = new EditText(context);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Remove", (dialog, which) -> {
            String tableName = input.getText().toString().trim();
            if (!TextUtils.isEmpty(tableName)) {
                deleteTable(tableName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        // Show the dialog
        builder.show();
    }

    public void checkAndCreateTable(String tableName) {
        // Check if the table name is unique
        db.collection("tables").document(tableName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (!document.exists()) {
                            // Table name is unique, create the table
                            createTable(tableName);
                        } else {
                            // Table name already exists
                            showToastMessage("Table name already exists. Please choose a different name.");
                        }
                    } else {
                        // Error occurred while checking table name
                        showToastMessage("Error checking table name: " + task.getException().getMessage());
                    }
                });
    }

    public void createTable(String tableName) {
        // Create a new table document in Firestore
        db.collection("tables").document(tableName)
                .set(new Table(tableName, true, "", new HashMap<>()))
                .addOnSuccessListener(aVoid -> {
                    // Table created successfully
                })
                .addOnFailureListener(e -> {
                    // Failed to create table
                    showToastMessage("Error creating table: " + e.getMessage());
                });
    }

    public void deleteTable(String tableName) {
        // Delete the table document from Firestore
        db.collection("tables").document(tableName)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Table deleted successfully
                })
                .addOnFailureListener(e -> {
                    // Failed to delete table
                    showToastMessage("Error deleting table: " + e.getMessage());
                });
    }



    private void showToastMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

}
