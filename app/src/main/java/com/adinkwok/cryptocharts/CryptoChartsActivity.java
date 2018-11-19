package com.adinkwok.cryptocharts;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Objects;

public class CryptoChartsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crypto_charts);

        LinearLayout parent = findViewById(R.id.currency_layout);
        for (int i = 0; i < 12; i++) {
            parent.addView(makeCurrencyItem());
        }

        Objects.requireNonNull(getSupportActionBar())
                .setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.abs_layout);
    }

    @SuppressLint("InflateParams")
    View makeCurrencyItem() {
        LinearLayout currencyItem = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.currency_item_layout, null);
        TextView currencyName = currencyItem.findViewById(R.id.currency_name);
        TextView currencyPrice = currencyItem.findViewById(R.id.currency_price_in_cad);
        String formattedPrice = getString(R.string.currency_price);
        currencyName.setText(R.string.currency_test_name);
        currencyPrice.setText(String.format(formattedPrice, getString(R.string.currency_test_price)));
        return currencyItem;
    }
}
