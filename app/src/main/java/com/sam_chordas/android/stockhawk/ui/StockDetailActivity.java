package com.sam_chordas.android.stockhawk.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.LineChartView;
import com.db.chart.view.animation.Animation;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.Utils.ProgressBarUtil;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;


public class StockDetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = StockDetailActivity.class.getSimpleName();
    public static final String TAG_COMPANY_STOCK_SYMBOL = "COMPANY_STOCK_SYMBOL";
    public static final String SAVE_ALL_LABELS_LIST = "ALL_LABELS_LIST";
    public static final String SAVE_ALL_VALUES_LIST = "ALL_VALUES_LIST";

    private ProgressBarUtil mProgressBar;
    private String companySymbol;
    private ArrayList<String> mLabelsList;
    private ArrayList<Float> mValuesList;

    @BindView(R.id.tv_error_msg)
    TextView mTVErrorMsg;

    @BindView(R.id.linechart)
    LineChartView mLineChartView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);
        ButterKnife.bind(this);

        mProgressBar = new ProgressBarUtil(this);
        companySymbol = getIntent().getStringExtra(TAG_COMPANY_STOCK_SYMBOL);
        setTitle(companySymbol);


        if (savedInstanceState == null || !savedInstanceState.containsKey(SAVE_ALL_LABELS_LIST) ||
                !savedInstanceState.containsKey(SAVE_ALL_VALUES_LIST)) {
            Log.d(LOG_TAG, "savedInstanceState is null");
            mLabelsList = new ArrayList<String>();
            mValuesList = new ArrayList<Float>();

            mProgressBar.show();
            getStockDetailsFromAPI();

        } else {
            mLabelsList = savedInstanceState.getStringArrayList(SAVE_ALL_LABELS_LIST);

            mValuesList = new ArrayList<Float>();
            float[] floatValuesArray = savedInstanceState.getFloatArray(SAVE_ALL_VALUES_LIST);
            for (float f : floatValuesArray) {
                mValuesList.add(f);
            }
            showChart();
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mLabelsList != null && !mLabelsList.isEmpty()) {
            outState.putStringArrayList(SAVE_ALL_LABELS_LIST, mLabelsList);
        }
        if (mValuesList != null && !mValuesList.isEmpty()) {
            float[] floatValuesArray = new float[mValuesList.size()];
            int i = 0;

            for (Float f : mValuesList) {
                floatValuesArray[i++] = (f != null ? f : Float.NaN);
            }
            outState.putFloatArray(SAVE_ALL_VALUES_LIST, floatValuesArray);
        }
    }



    // Get stock historical data from yahoo api
    private void getStockDetailsFromAPI() {
        OkHttpClient client = new OkHttpClient();

        StringBuilder urlStringBuilder = new StringBuilder();
        try {
            // Base URL for the Yahoo query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol = " + "\"" + companySymbol + "\""
                    + " and startDate = " + "\"2015-01-10\"" + " and endDate = " + "\"2015-04-10\"", "UTF-8"));
            // finalize the URL for the API query.
            urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                    + "org%2Falltableswithkeys&callback=");
        } catch (UnsupportedEncodingException ex) {
            Log.e(LOG_TAG, "Exception ex : " + ex.getMessage());
        }
        String urlString = null;
        if (urlStringBuilder != null) {
            urlString = urlStringBuilder.toString();
        }

        Request request = new Request.Builder()
                .url(urlString)
                .build();
        Log.d("Request url : ", request.url().toString());


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Response response) throws IOException {
                if (response.code() == 200) {

                    try {
                        //Parse JSON
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        JSONObject queryObj = jsonResponse.getJSONObject("query");
                        JSONObject resultObj = queryObj.getJSONObject("results");
                        JSONArray quoteArray = resultObj.getJSONArray("quote");

                        for (int i = 0; i < quoteArray.length(); i++) {
                            JSONObject item = quoteArray.getJSONObject(i);
                            String date = item.getString("Date");
                            Float closeStockPrice = Float.parseFloat(item.getString("Close"));

                            mLabelsList.add(date);
                            mValuesList.add(closeStockPrice);
                        }

                        showChart();

                    } catch (Exception ex) {
                        onAPIFailure();
                        Log.e(LOG_TAG, "Exception ex : " + ex.getMessage());
                    }
                } else {
                    onAPIFailure();
                }
            }

            @Override
            public void onFailure(Request request, IOException e) {
                onAPIFailure();
            }
        });
    }

    private void showChart() {
        StockDetailActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.hide();
                mTVErrorMsg.setVisibility(View.GONE);

                float[] floatArray = new float[mValuesList.size()];
                int i = 0;

                for (Float f : mValuesList) {
                    floatArray[i++] = (f != null ? f : Float.NaN);
                }

                LineSet dataset = new LineSet(mLabelsList.toArray(new String[mLabelsList.size()]), floatArray);

                float minimumPrice = Collections.min(mValuesList);
                float maximumPrice = Collections.max(mValuesList);


                dataset.setColor(Color.parseColor("#758cbb"))
                        .setFill(Color.parseColor("#2d374c"))
                        .setDotsColor(Color.parseColor("#758cbb"))
                        .setThickness(4)
                        .setDashed(new float[]{10f, 10f});

                //To add dataset to Line Chart View
                mLineChartView.addData(dataset);


                // To plot the Chart, define min & max value for Y axis.
                mLineChartView.setBorderSpacing(Tools.fromDpToPx(15))
                        .setYLabels(AxisController.LabelPosition.OUTSIDE)
                        .setXLabels(AxisController.LabelPosition.NONE)
                        .setLabelsColor(Color.parseColor("#6a84c3"))
                        .setXAxis(false)
                        .setYAxis(false)
                        .setAxisBorderValues(Math.round(Math.max(0f, minimumPrice - 5f)), Math.round(maximumPrice + 5f));

                Animation anim = new Animation();

                if (dataset.size() > 1) {
                    mLineChartView.show(anim);
                } else {
                    Toast.makeText(StockDetailActivity.this, "No data to show the chart", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private void onAPIFailure() {
        StockDetailActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLineChartView.setVisibility(View.GONE);
                mProgressBar.hide();
                mTVErrorMsg.setVisibility(View.VISIBLE);
            }
        });
    }


}
