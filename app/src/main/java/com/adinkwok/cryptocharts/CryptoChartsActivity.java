package com.adinkwok.cryptocharts;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class CryptoChartsActivity extends AppCompatActivity {
    private static final String TAG = CryptoChartsActivity.class.getSimpleName();

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

        getSupportActionBar().getCustomView().findViewById(R.id.up_arrow)
                .setOnClickListener(v -> mScrollView.fullScroll(ScrollView.FOCUS_UP));
        getSupportActionBar().getCustomView().findViewById(R.id.down_arrow)
                .setOnClickListener(v -> mScrollView.fullScroll(ScrollView.FOCUS_DOWN));

        new GetCurrencyInfo().execute();
    }

    /**
     * Adds selected currency to the list.
     * If the list is in favourites, move it out of there.
     *
     * @param existingLL is an existing layout, used to prevent recreation.
     * @param name       of the currency.
     * @param price      of a single unit of the currency in CAD.
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
     * Move the currency out of the regular list and move it here.
     *
     * @param existingLL is an existing layout, used to prevent recreation.
     * @param name       of the currency.
     * @param price      of a single unit of the currency in CAD.
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
     *
     * @param layout of the currency entry to modify.
     */
    private void updateFavImage(LinearLayout layout) {
        if (layout != null) {
            ImageView favImage = layout.findViewById(R.id.fav_image);
            if (mFavLinearLayout.indexOfChild(layout) != -1) {
                favImage.setImageDrawable(getDrawable(R.drawable.ic_star_filled));
            } else if (mRegLinearLayout.indexOfChild(layout) != -1) {
                favImage.setImageDrawable(getDrawable(R.drawable.ic_star_open));
            }
        }
    }

    /**
     * Creates a new currency entry with the given name and price.
     *
     * @param name  of the currency.
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
        private final HashMap<String, String[]> currencyInfo = new HashMap<>();
        private final ProgressDialog progressDialog = new ProgressDialog(CryptoChartsActivity.this);
        private final HttpHandler httpHandler = new HttpHandler();


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
         * Only request data if it has not been previously recorded.
         */
        private String getUrlCoinList() {
            //  String urlCoinList = "https://www.cryptocompare.com/api/data/coinlist";
            String urlCoinList = "https://min-api.cryptocompare.com/data/all/coinlist";
            String response = httpHandler.makeServiceCall(urlCoinList);
            Log.e(TAG, "Response from url: " + response);
            return response;
        }

        /**
         * Since the CryptoCurrency API only allows fsyms with a constraint of 300 characters,
         * split requests like so: pausing one symbol before reaching the maximum then starting a
         * new request. If isPriceListDownloaded is true, don't request data again and use previous
         * results.
         */
        private ArrayList getPriceList() {
            ArrayList<String> prices = new ArrayList<>();
            StringBuilder urlConvertToCad = new StringBuilder();
            for (String symbol : currencyInfo.keySet()) {
                urlConvertToCad.append(symbol).append(",");
                if (urlConvertToCad.length() + symbol.length() + 1 >= 300) {
                    prices.add(requestPrices(urlConvertToCad.toString()));
                    urlConvertToCad = new StringBuilder();
                }
            }
            prices.add(requestPrices(urlConvertToCad.toString()));
            return prices;
        }

        /**
         * Requests and records prices of the given symbols.
         * @param symbols that are separated by commas.
         * @return a JSON string of the prices.
         */
        private String requestPrices(String symbols) {
            if (symbols == null) {
                return null;
            }
            String finalUrlConvert =
                    "https://min-api.cryptocompare.com/data/pricemulti?fsyms=" +
                            symbols +
                            "&tsyms=CAD";
            String jsonStr = httpHandler.makeServiceCall(finalUrlConvert);
            Log.e(TAG, "Response from url: " + jsonStr);
            return jsonStr;
        }

        /**
         * Extracts all currency names and symbols.
         *
         * @return true if parsing JSON data is without fail, false if otherwise.
         */
        private boolean parseCurrencyName(String coinListJson) {
            try {
                JSONObject jsonObj = new JSONObject(coinListJson);
                jsonObj = jsonObj.getJSONObject("Data");
                JSONArray keys = jsonObj.names();
                for (int i = 0; i < keys.length(); i++) {
                    JSONObject c = jsonObj.getJSONObject(keys.getString(i));
                    String[] fetched = {c.getString("FullName"), " N/a"};
                    currencyInfo.put(c.getString("Symbol"), fetched);
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
         *
         * @return true if nothing bad happens when parsing, false otherwise
         */
        private boolean parseCurrencyPrice(List prices) {
            for (int i = 0; i < prices.size(); i++) {
                String jsonStr = (String) prices.get(i);
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    Iterator<String> keys = jsonObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        if (jsonObj.get(key) instanceof JSONObject) {
                            String[] currency = currencyInfo.get(key);
                            assert currency != null;
                            currency[1] = ((JSONObject) jsonObj.get(key)).getString("CAD");
                            currencyInfo.put(key, currency);
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
            boolean shouldComplain = false;
            String coinList = getUrlCoinList();
            if (coinList != null) {
                shouldComplain = !parseCurrencyName(coinList);
                if (!shouldComplain) {
                    ArrayList prices = getPriceList();
                    if (prices != null) {
                        shouldComplain = !parseCurrencyPrice(prices);
                    } else {
                        shouldComplain = true;
                    }
                }
            }
            if (shouldComplain)
                failedToGetData();
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
            for (String[] fetchedData : currencyInfo.values()) {
                addRegCurrency(null, fetchedData[0], fetchedData[1]);
            }
            if (progressDialog.isShowing())
                progressDialog.dismiss();
        }
    }
}
