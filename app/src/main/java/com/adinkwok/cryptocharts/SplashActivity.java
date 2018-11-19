package com.adinkwok.cryptocharts;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

public class SplashActivity extends AppCompatActivity {
    private Context mContext;
    private boolean mCancelled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);
        mContext = this;
        mCancelled = false;
        new Handler().postDelayed(() -> {
            if (!mCancelled) {
                goToMainMenu();
            }
        }, 2000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCancelled = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCancelled) {
            goToMainMenu();
        }
    }

    private void goToMainMenu() {
        Intent intent = new Intent(mContext, CryptoChartsActivity.class);
        startActivity(intent);
        finish();
    }
}
