package com.abandiak.alerta.app.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.home.HomeActivity;
import com.abandiak.alerta.core.utils.BaseActivity;
import com.abandiak.alerta.core.utils.ToastUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class CompleteProfileActivity extends BaseActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private EditText inputFirst, inputLast, inputCountry, inputCity, inputPostal, inputStreet, inputHome;
    private AutoCompleteTextView inputGender;

    private boolean editMode = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        inputFirst = findViewById(R.id.inputFirstName);
        inputLast = findViewById(R.id.inputLastName);
        inputCountry = findViewById(R.id.inputCountry);
        inputCity = findViewById(R.id.inputCity);
        inputPostal = findViewById(R.id.inputPostalCode);
        inputStreet = findViewById(R.id.inputStreet);
        inputHome = findViewById(R.id.inputHomeNumber);
        inputGender = findViewById(R.id.inputGender);
        MaterialButton btnSave = findViewById(R.id.btnSave);

        editMode = getIntent().getBooleanExtra("edit_mode", false);

        if (editMode) {
            ((android.widget.TextView) findViewById(R.id.textTitle)).setText("Edit your profile");
            btnSave.setText("Save changes");
        }

        String[] genders = {"Male", "Female", "Prefer not to say"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.list_item_dropdown,
                genders
        );
        inputGender.setAdapter(adapter);
        inputGender.setDropDownBackgroundDrawable(
                ContextCompat.getDrawable(this, R.drawable.bg_dropdown_alerta)
        );

        if (editMode) loadExistingUserData();

        btnSave.setOnClickListener(v -> saveProfile());
    }


    private void loadExistingUserData() {
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        inputFirst.setText(doc.getString("firstName"));
                        inputLast.setText(doc.getString("lastName"));
                        inputCountry.setText(doc.getString("country"));
                        inputCity.setText(doc.getString("city"));
                        inputPostal.setText(doc.getString("postalCode"));
                        inputStreet.setText(doc.getString("street"));
                        inputHome.setText(doc.getString("homeNumber"));

                        String gender = doc.getString("gender");
                        if (gender != null) {
                            inputGender.setText(gender, false);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        ToastUtils.show(this, "Failed to load profile: " + e.getMessage()));
    }


    private void saveProfile() {

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

        String uid = auth.getCurrentUser().getUid();

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
                .set(profile, SetOptions.merge())
                .addOnSuccessListener(a -> {

                    ToastUtils.show(this, editMode ? "Profile updated!" : "Profile saved!");

                    if (editMode) {
                        finish();
                    } else {
                        startActivity(new Intent(this, HomeActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                        ToastUtils.show(this, "Failed: " + e.getMessage()));
    }
}
