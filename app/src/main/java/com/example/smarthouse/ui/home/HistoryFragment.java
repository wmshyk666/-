package com.example.smarthouse.ui.home;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.smarthouse.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HistoryFragment extends Fragment {

    private LineChart lineChart;
    private int sensorId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // 强制横屏
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        lineChart = view.findViewById(R.id.lineChart);

        Bundle args = getArguments();
        if (args != null) {
            sensorId = args.getInt("sensorId", -1);
            String urlString = String.format("http://vipvip.xiaomiqiu.com:44468/sensorHistory/select/%d", sensorId);
            new FetchSensorHistoryTask().execute(urlString);
        }

        setupLineChart();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 恢复为默认方向
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void setupLineChart() {
        lineChart.setBackgroundColor(Color.WHITE);

        Description description = new Description();
        description.setText("");
        lineChart.setDescription(description);

        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(false);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        Legend legend = lineChart.getLegend();
        legend.setTextColor(Color.BLACK);
    }

    private class FetchSensorHistoryTask extends AsyncTask<String, Void, List<SensorHistory>> {
        @Override
        protected List<SensorHistory> doInBackground(String... params) {
            String serverUrl = params[0];
            List<SensorHistory> historyList = new ArrayList<>();
            try {
                URL url = new URL(serverUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    int code = jsonResponse.getInt("code");
                    if (code == 200) {
                        JSONArray dataArray = jsonResponse.getJSONArray("data");
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject jsonObject = dataArray.getJSONObject(i);
                            SensorHistory history = new SensorHistory(jsonObject);
                            if (Float.parseFloat(history.getValue()) != 0) {
                                historyList.add(history);
                            }
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return historyList;
        }

        @Override
        protected void onPostExecute(List<SensorHistory> historyList) {
            super.onPostExecute(historyList);
            if (historyList != null && !historyList.isEmpty()) {
                List<Entry> entries = new ArrayList<>();
                for (SensorHistory history : historyList) {
                    entries.add(new Entry(history.getId(), Float.parseFloat(history.getValue())));
                }
                LineDataSet dataSet = new LineDataSet(entries, "Sensor Data");
                dataSet.setColor(Color.BLACK);
                dataSet.setValueTextColor(Color.RED);
                dataSet.setCircleColor(Color.RED);
                dataSet.setLineWidth(2f);

                dataSet.setValueTextSize(6f); // 设置显示数值的字体大小

                LineData lineData = new LineData(dataSet);
                lineChart.setData(lineData);
                lineChart.invalidate(); // 刷新图表
            }
        }
    }
}

class SensorHistory {
    private int id;
    private int sensorId;
    private String value;
    private Timestamp timestamp;

    public SensorHistory(JSONObject jsonObject) throws JSONException {
        this.id = jsonObject.getInt("id");
        this.sensorId = jsonObject.getInt("sensorId");
        this.value = jsonObject.getString("value");

        String timestampStr = jsonObject.getString("timestamp");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        try {
            Date timestampDate = sdf.parse(timestampStr);
            if (timestampDate != null) {
                this.timestamp = new Timestamp(timestampDate.getTime());
            }
        } catch (ParseException e) {
            e.printStackTrace();
            this.timestamp = null;
        }
    }

    public int getId() {
        return id;
    }

    public int getSensorId() {
        return sensorId;
    }

    public String getValue() {
        return value;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}