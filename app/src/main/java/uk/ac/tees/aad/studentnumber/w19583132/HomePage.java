package uk.ac.tees.aad.studentnumber.w19583132;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import uk.ac.tees.aad.w19583132.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class HomePage extends AppCompatActivity {
    CardView userButton, adminButton;
    ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);


        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        userButton = findViewById(R.id.userbtn);
        adminButton = findViewById(R.id.adminbtn);
        imageView = findViewById(R.id.imageView);

        userButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(user==null){
                    Intent intent = new Intent(HomePage.this, Login.class);
                    intent.putExtra("type", "user");
                    startActivity(intent);
                }else {
                    String userEmail = user.getEmail();
                    if (userEmail != null){
                        if(!userEmail.equals("admin@gmail.com")){
                            // User is admin, redirect to the AdminHome page
                            Intent intent = new Intent(HomePage.this, userpage.class);
                            startActivity(intent);
                        }else{
                            Toast.makeText(getApplicationContext(),"Already LoggedIn as Admin", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });
        adminButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(user==null) {
                    Intent intent = new Intent(HomePage.this, Login.class);
                    intent.putExtra("type", "admin");
                    startActivity(intent);
                }else {
                    String userEmail = user.getEmail();
                    if (userEmail != null){
                        if(userEmail.equals("admin@gmail.com")){
                            // User is admin, redirect to the AdminHome page
                            Intent intent = new Intent(HomePage.this, adminPage.class);
                            startActivity(intent);
                        }else{
                            Toast.makeText(getApplicationContext(),"Already LoggedIn as User", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int orientation = newConfig.orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Landscape mode, hide profileImage
            imageView.setVisibility(View.GONE);
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Portrait mode, show profileImage
            imageView.setVisibility(View.VISIBLE);
        }
    }

}