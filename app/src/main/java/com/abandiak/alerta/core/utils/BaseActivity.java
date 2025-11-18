package com.abandiak.alerta.core.utils;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.abandiak.alerta.R;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(
                R.anim.fade_in,
                R.anim.fade_out
        );
    }

    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        super.startActivity(intent, options);
        overridePendingTransition(
                R.anim.fade_in,
                R.anim.fade_out
        );
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(
                R.anim.fade_in,
                R.anim.fade_out
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        overridePendingTransition(
                R.anim.fade_in,
                R.anim.fade_out
        );
    }

    public void finishWithAnimation() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
