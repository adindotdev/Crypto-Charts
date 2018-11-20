package com.adinkwok.cryptocharts;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;

public class CryptoChartsActivity extends AppCompatActivity {
    private static final String TAG = CryptoChartsActivity.class.getSimpleName();
    private static final int FAV_LIST = 0;

    private ScrollView mScrollView;
    private LinearLayout mFavLinearLayout;
    private LinearLayout mRegLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crypto_charts);

        mScrollView = findViewById(R.id.currency_scroll);
        mFavLinearLayout = findViewById(R.id.fav_currency_layout);
        mRegLinearLayout = findViewById(R.id.reg_currency_layout);

        Objects.requireNonNull(getSupportActionBar())
                .setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.abs_layout);
        new GetCurrencyInfo().execute();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.btn_star_big_off);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, FAV_LIST, 0, R.string.app_name)
                .setIcon(android.R.drawable.arrow_up_float)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case FAV_LIST:
                mScrollView.fullScroll(ScrollView.FOCUS_UP);
                return true;
            case android.R.id.home:
                clearFavourites();
                return true;
            default:
                return false;
        }
    }

    /**
     * Removes all currency entries in the favourites list.
     */
    private void clearFavourites() {
        Toast.makeText(this,
                "Cleared favourites.",
                Toast.LENGTH_SHORT).show();
        for (int i = mFavLinearLayout.getChildCount() - 1; i >= 0; i--) {
            addRegCurrency((LinearLayout) mFavLinearLayout.getChildAt(i), null, null);
        }
    }

    /**
     * Adds selected currency to the list.
     * If the list is in favourites, move it out of there.
     * @param existingLL is an existing layout, used to prevent recreation.
     * @param name of the currency.
     * @param price of a single unit of the currency in CAD.
     */
    private void addRegCurrency(LinearLayout existingLL, String name, String price) {
        if (existingLL != null && mFavLinearLayout.indexOfChild(existingLL) != -1) {
            mFavLinearLayout.removeView(existingLL);
            mRegLinearLayout.addView(existingLL, 0);
            updateFavImage(existingLL);
        } else {
            LinearLayout newLL = createNewCurrencyEntry(name, price);
            mRegLinearLayout.addView(newLL);
            updateFavImage(newLL);
        }
    }

    /**
     * Adds selected currency to the favourites list.
     * Move the list out of the regular list and move it here.
     * @param existingLL is an existing layout, used to prevent recreation.
     * @param name of the currency.
     * @param price of a single unit of the currency in CAD.
     */
    private void addFavCurrency(LinearLayout existingLL, String name, String price) {
        Toast.makeText(getApplicationContext(),
                "Added " + name + " to favourites.",
                Toast.LENGTH_LONG)
                .show();
        if (existingLL != null && mRegLinearLayout.indexOfChild(existingLL) != -1) {
            mRegLinearLayout.removeView(existingLL);
            mFavLinearLayout.addView(existingLL, 0);
            updateFavImage(existingLL);
        } else {
            existingLL = createNewCurrencyEntry(name, price);
            mFavLinearLayout.addView(existingLL);
            updateFavImage(existingLL);
        }
        Log.e(TAG, "Added " + name + " to favourites.");
    }

    /**
     * Updates given currency's star image depending on the list they reside in.
     * @param layout of the currency entry to modify.
     */
    private void updateFavImage(LinearLayout layout) {
        if (layout != null) {
            ImageView favImage = layout.findViewById(R.id.fav_image);
            if (mFavLinearLayout.indexOfChild(layout) != -1) {
                favImage.setImageDrawable(getDrawable(android.R.drawable.btn_star_big_on));
            } else if (mRegLinearLayout.indexOfChild(layout) != -1) {
                favImage.setImageDrawable(getDrawable(android.R.drawable.btn_star_big_off));
            }
        }
    }

    /**
     * Creates a new currency entry with the given name and price.
     * @param name of the currency.
     * @param price of a single unit of the currency in CAD.
     * @return the new currency entry.
     */
    @SuppressLint("InflateParams")
    private LinearLayout createNewCurrencyEntry(String name, String price) {
        LinearLayout currencyItem = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.currency_item_layout, null);
        TextView currencyName = currencyItem.findViewById(R.id.currency_name);
        currencyName.setText(name);
        TextView currencyPrice = currencyItem.findViewById(R.id.currency_price_in_cad);
        String formattedPrice = getString(R.string.currency_price);
        currencyPrice.setText(String.format(formattedPrice, price));
        ImageView favImage = currencyItem.findViewById(R.id.fav_image);
        favImage.setOnClickListener(v -> {
            if (mRegLinearLayout.indexOfChild(currencyItem) != -1) {
                addFavCurrency(currencyItem, name, price);
            } else if (mFavLinearLayout.indexOfChild(currencyItem) != -1) {
                addRegCurrency(currencyItem, name, price);
            }
        });
        return currencyItem;
    }

    @SuppressLint("StaticFieldLeak")
    private class GetCurrencyInfo extends AsyncTask<Void, Void, Void> {
        private final ProgressDialog progressDialog = new ProgressDialog(CryptoChartsActivity.this);
        private final HttpHandler httpHandler = new HttpHandler();

        private final LinkedList<String[]> currencyInfo = new LinkedList<>();
        private final ArrayList<String> priceJson = new ArrayList<>();
        private final ArrayList<Integer> max200Index = new ArrayList<>();
        private boolean isPriceListDownloaded = false;
        private String coinListJson;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Just hold on...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        /**
         * Changed coin list API path as recommended by CryptoCompare.
         * Original link: https://www.cryptocompare.com/api/data/coinlist
         *
         * Only request data if it has not been previously recorded.
         */
        private void getUrlCoinList() {
            if (coinListJson == null) {
                //  String urlCoinList = "https://www.cryptocompare.com/api/data/coinlist";
                String urlCoinList = "https://min-api.cryptocompare.com/data/all/coinlist";
                coinListJson = httpHandler.makeServiceCall(urlCoinList);
                Log.e(TAG, "Response from url: " + coinListJson);
            }
        }

        /**
         * Since the CryptoCurrency API only allows fsyms with a constraint of 300 characters,
         * split requests like so: pausing one symbol before reaching the maximum then starting a
         * new request.
         *
         * If isPriceListDownloaded is true, don't request data again and use previous results.
         */
        private void getPriceList() {
            if (!isPriceListDownloaded) {
                max200Index.add(0);
                StringBuilder urlConvertToCad = new StringBuilder();
                for (int i = 0; i < currencyInfo.size(); i++) {
                    if (urlConvertToCad.length() + currencyInfo.get(i)[0].length() + 1 < 300) {
                        urlConvertToCad.append(currencyInfo.get(i)[0]).append(",");
                    } else {
                        String finalUrlConvert =
                                "https://min-api.cryptocompare.com/data/pricemulti?fsyms=" +
                                        urlConvertToCad.toString() +
                                        "&tsyms=CAD";
                        String jsonStr = httpHandler.makeServiceCall(finalUrlConvert);
                        max200Index.add(i);
                        priceJson.add(jsonStr);
                        urlConvertToCad = new StringBuilder();
                        urlConvertToCad.append(currencyInfo.get(i)[0]).append(",");
                        Log.e(TAG, "Response from url: " + jsonStr);
                    }
                }
                isPriceListDownloaded = true;
            }
        }

        /**
         * Extracts all currency names and symbols.
         * @return true if parsing JSON data is without fail, false if otherwise.
         */
        private boolean parseCurrencyName() {
            try {
                JSONObject jsonObj = new JSONObject(coinListJson);
                jsonObj = jsonObj.getJSONObject("Data");
                JSONArray keys = jsonObj.names();
                for (int i = 0; i < keys.length(); i++) {
                    JSONObject c = jsonObj.getJSONObject(keys.getString(i));
                    String[] fetched = {c.getString("Symbol"),
                            c.getString("FullName"), ""};
                    currencyInfo.add(fetched);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Json parsing error: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        "Json parsing error: " + e.getMessage(),
                        Toast.LENGTH_LONG)
                        .show());
                return false;
            }
            return true;
        }

        /**
         * Extracts the Canadian dollar conversion of currencies.
         * If price information is unavailable, show "N/a".
         * @return true if nothing bad happens when parsing, false otherwise
         */
        private boolean parseCurrencyPrice() {
            for (int i = 0; i < priceJson.size(); i++) {
                String jsonStr = priceJson.get(i);
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    for (int k = max200Index.get(i); k < max200Index.get(i + 1); k++) {
                        if (k < currencyInfo.size()) {
                            if (jsonObj.has(currencyInfo.get(k)[0])) {
                                JSONObject c = jsonObj.getJSONObject(currencyInfo.get(k)[0]);
                                currencyInfo.get(k)[2] = c.getString("CAD");
                            } else {
                                currencyInfo.get(k)[2] = " N/a";
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                            "Json parsing error: " + e.getMessage(),
                            Toast.LENGTH_LONG)
                            .show());
                    return false;
                }
            }
            return true;
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            getUrlCoinList();
            if (coinListJson != null && parseCurrencyName()) {
                getPriceList();
                if (!isPriceListDownloaded || !parseCurrencyPrice()) {
                    failedToGetData();
                }
            } else {
                failedToGetData();
            }
            return null;
        }

        private void failedToGetData() {
            Log.e(TAG, "Couldn't get data from server.");
            runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                    "Couldn't get data from server.",
                    Toast.LENGTH_LONG)
                    .show());
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            for (String[] fetchedData : currencyInfo) {
                addRegCurrency(null, fetchedData[1], fetchedData[2]);
            }
            if (progressDialog.isShowing())
                progressDialog.dismiss();
        }
    }
}
