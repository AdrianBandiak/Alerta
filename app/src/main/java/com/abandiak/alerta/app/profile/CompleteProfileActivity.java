package com.abandiak.alerta.app.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.home.HomeActivity;
import com.abandiak.alerta.core.utils.ToastUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CompleteProfileActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_gray));
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);

        setContentView(R.layout.activity_complete_profile);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        EditText inputFirst = findViewById(R.id.inputFirstName);
        EditText inputLast = findViewById(R.id.inputLastName);
        EditText inputCountry = findViewById(R.id.inputCountry);
        EditText inputCity = findViewById(R.id.inputCity);
        EditText inputPostal = findViewById(R.id.inputPostalCode);
        EditText inputStreet = findViewById(R.id.inputStreet);
        EditText inputHome = findViewById(R.id.inputHomeNumber);
        AutoCompleteTextView inputGender = findViewById(R.id.inputGender);
        MaterialButton btnSave = findViewById(R.id.btnSave);

        String[] genders = {"Male", "Female", "Prefer not to say"};
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        inputGender.setAdapter(adapter);
        inputGender.setOnClickListener(v -> inputGender.showDropDown());

        btnSave.setOnClickListener(v -> {
            String first = inputFirst.getText().toString().trim();
            String last = inputLast.getText().toString().trim();
            String country = inputCountry.getText().toString().trim();
            String city = inputCity.getText().toString().trim();
            String postal = inputPostal.getText().toString().trim();
            String street = inputStreet.getText().toString().trim();
            String home = inputHome.getText().toString().trim();
            String gender = inputGender.getText().toString().trim();

            if (first.isEmpty() || last.isEmpty() || country.isEmpty() || city.isEmpty()) {
                ToastUtils.show(this, "Please fill all required fields.");
                return;
            }

            String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
            if (uid == null) {
                ToastUtils.show(this, "User not logged in.");
                return;
            }

            Map<String, Object> profile = new HashMap<>();
            profile.put("firstName", first);
            profile.put("lastName", last);
            profile.put("country", country);
            profile.put("city", city);
            profile.put("postalCode", postal);
            profile.put("street", street);
            profile.put("homeNumber", home);
            profile.put("gender", gender);
            profile.put("profileCompleted", true);

            db.collection("users").document(uid)
                    .update(profile)
                    .addOnSuccessListener(a -> {
                        ToastUtils.show(this, "Profile saved successfully!");
                        startActivity(new Intent(this, HomeActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        db.collection("users").document(uid)
                                .set(profile)
                                .addOnSuccessListener(a2 -> {
                                    ToastUtils.show(this, "Profile saved successfully!");
                                    startActivity(new Intent(this, HomeActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                                    finish();
                                })
                                .addOnFailureListener(err ->
                                        ToastUtils.show(this, "Failed to save profile. Try again."));
                    });
        });
    }
}
