package com.abandiak.alerta.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.auth.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View logo = findViewById(R.id.logoImage);
        View welcome = findViewById(R.id.welcomeText);

        logo.setScaleX(0.6f);
        logo.setScaleY(0.6f);
        logo.setAlpha(0f);

        welcome.setAlpha(0f);
        welcome.setScaleX(0.85f);
        welcome.setScaleY(0.85f);

        logo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(700)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> showWelcome(welcome))
                .start();
    }

    private void showWelcome(View welcome) {
        welcome.animate()
                .alpha(1f)
                .scaleX(1.22f)
                .scaleY(1.22f)
                .setDuration(550)
                .setInterpolator(new OvershootInterpolator(2f))
                .withEndAction(() -> welcome.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .start()
                )
                .withEndAction(() ->
                        welcome.postDelayed(this::goToLogin, 500)
                )
                .start();
    }


    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}

