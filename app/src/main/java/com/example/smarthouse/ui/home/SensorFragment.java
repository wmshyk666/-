package com.example.smarthouse.ui.home;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.smarthouse.MainActivity;
import com.example.smarthouse.MqttClientManager;
import com.example.smarthouse.R;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;



import android.os.Handler;
import android.os.Looper;
import java.util.Timer;
import java.util.TimerTask;
/** @noinspection ALL*/
class Sensor {
    public int getSensorId() {
        return sensorId;
    }

    public String getSensorName() {
        return sensorName;
    }

    public String getSensorType() {
        return sensorType;
    }

    public String getLastReading() {
        return lastReading;
    }

    public Timestamp getReadingTime() {
        return readingTime;
    }

    public void setLastReading(String lastReading) {
        this.lastReading = lastReading;
    }

    private int sensorId;
    private String sensorName;
    private String sensorType;
    private String lastReading;

    public void setReadingTime(Timestamp readingTime) {
        this.readingTime = readingTime;
    }

    private Timestamp readingTime;
    public Sensor(JSONObject jsonObject) throws JSONException {
        this.sensorId = jsonObject.getInt("sensorId");
        this.sensorName = jsonObject.getString("sensorName");
        this.sensorType = jsonObject.getString("sensorType");
        this.lastReading = jsonObject.getString("lastReading");
        // 解析时间戳字符串为 Timestamp 对象
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        try {
            Date date = sdf.parse(jsonObject.getString("readingTime"));
            if (date != null) {
                this.readingTime = new Timestamp(date.getTime());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
class SensorNode {
    private String paramName;
    private int paramValue;

    // 构造函数
    public SensorNode(String paramName, int paramValue) {
        this.paramName = paramName;
        this.paramValue = paramValue;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public int getParamValue() {
        return paramValue;
    }

    public void setParamValue(int paramValue) {
        this.paramValue = paramValue;
    }
}
/** @noinspection ALL*/
public class SensorFragment extends Fragment implements MqttClientManager.MqttMessageListener {
    private Timer timer;
    private Handler handler;
    private ListView sensorListView;
    private List<Sensor> sensorList;
    private SensorListAdapter adapter;
    private String TOPIC; // 订阅主题
    private String deviceName;
    private  int deviceId;

    private MqttClientManager mqttClientManager = new MqttClientManager();
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensor, container, false);
        sensorListView = view.findViewById(R.id.sensorListView);
        sensorList = new ArrayList<>();
        adapter = new SensorListAdapter();
        sensorListView.setAdapter(adapter);
        Bundle bundle = getArguments();
        if (bundle != null) {
            // 从 Bundle 中获取设备名称
            deviceName = bundle.getString("deviceName");
            deviceId=bundle.getInt("deviceId");
        }

        String urlString = String.format("http://vipvip.xiaomiqiu.com:44468/sensor/selectByDeviceID/%d", deviceId);
        new SensorFragment.FetchDataTask().execute(urlString);

        // 从 MQTT 处获取数据
        TOPIC = String.format("device/%s", deviceName);
        // 连接到 MQTT Broker 并订阅主题
        mqttClientManager.connectToBroker(requireActivity(), TOPIC, null);
        mqttClientManager.setMqttMessageListener(this);

        return view;
    }


    private void sendMinMaxValuesToServer(String maxValue, String minValue) {
        // 构建API请求并将最大值和最小值发送到服务器

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    // 构建URL对象
                    URL url = new URL("YOUR_API_ENDPOINT");

                    // 创建HttpURLConnection对象
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);

                    // 构建JSON对象
                    JSONObject requestData = new JSONObject();
                    requestData.put("maxValue", maxValue);
                    requestData.put("minValue", minValue);

                    // 将JSON数据写入输出流
                    OutputStream outputStream = connection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                    writer.write(requestData.toString());
                    writer.flush();
                    writer.close();
                    outputStream.close();

                    // 获取响应码
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // 如果需要，可以在这里处理成功响应
                    } else {
                        // 如果需要，可以在这里处理错误响应
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                // 如果需要，处理API响应
                // 您可以根据响应显示Toast消息或更新UI
            }
        }.execute();
    }



    // 异步任务，从服务器获取数据
    private class FetchDataTask extends AsyncTask<String, Void, List<Sensor>> {

        @Override
        protected List<Sensor> doInBackground(String... params) {
            String serverUrl = params[0];
            List<Sensor> sensors = new ArrayList<>();
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

                    // 解析从服务器获取的数据
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    int code = jsonResponse.getInt("code");
                    if (code == 200) {
                        JSONArray dataArray = jsonResponse.getJSONArray("data");
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject jsonObject = dataArray.getJSONObject(i);
                            // 假设您的 Device 类有一个带 JSONObject 参数的构造函数
                            Sensor sensor = new Sensor(jsonObject);
                            sensors.add(sensor);
                        }
                    } else {
                        // 请求不成功的处理
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return sensors;
        }
        @Override
        protected void onPostExecute(List<Sensor> sensors) {
            super.onPostExecute(sensors);
            sensorList.clear();
            sensorList.addAll(sensors);
            adapter.notifyDataSetChanged();
        }
    }
    // 传感器列表适配器
    class SensorListAdapter extends ArrayAdapter<Sensor> {

        public SensorListAdapter() {
            super(getActivity(), R.layout.item_sensor, sensorList);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_sensor, parent, false);
            }

            Sensor currentSensor = sensorList.get(position);

            // 找到 item_sensor.xml 中的视图
           // TextView sensorIdTextView = itemView.findViewById(R.id.sensorIdTextView);
           // TextView sensorNameTextView = itemView.findViewById(R.id.sensorNameTextView);
            TextView sensorTypeTextView = itemView.findViewById(R.id.sensorTypeTextView);
            TextView lastReadingTextView = itemView.findViewById(R.id.lastReadingTextView);
            TextView readingTimeTextView = itemView.findViewById(R.id.readingTimeTextView);
            Button historyButton = itemView.findViewById(R.id.historyButton);
            EditText maxEditText = itemView.findViewById(R.id.maxEditText);
            EditText minEditText = itemView.findViewById(R.id.minEditText);
            Button saveButton = itemView.findViewById(R.id.saveButton);
            // 将传感器信息设置到视图中
            //sensorIdTextView.setText("传感器ID: " + currentSensor.getSensorId());
           // sensorNameTextView.setText("传感器名称: " + currentSensor.getSensorName());
            sensorTypeTextView.setText("传感器类型: " + currentSensor.getSensorType());

            switch(currentSensor.getSensorType()){
                case"环境温度传感器":
                case"土壤温度传感器":{
                    lastReadingTextView.setText("最后读数: " + currentSensor.getLastReading()+"°C");
                    break;
                }
                case"环境湿度传感器":
                case"土壤湿度传感器":{
                    lastReadingTextView.setText("最后读数: " + currentSensor.getLastReading()+"%");
                    break;
                }
                case"光照传感器":{
                    lastReadingTextView.setText("最后读数: " + currentSensor.getLastReading()+"lux");
                    break;
                }
                case"二氧化碳浓度传感器":{
                    lastReadingTextView.setText("最后读数: " + currentSensor.getLastReading()+"ppm");
                    break;
                }
                default:
                    lastReadingTextView.setText("最后读数: " + currentSensor.getLastReading());
            }


            readingTimeTextView.setText("最后读数时间: " + currentSensor.getReadingTime());

            historyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 获取点击的设备
                    Sensor selectedSensor = sensorList.get(position);

                    HistoryFragment historyfragment = new HistoryFragment();

                    // 创建 Bundle 对象，用于传递参数
                    Bundle bundle = new Bundle();
                    bundle.putInt("sensorId", selectedSensor.getSensorId());

                    // 将参数设置到 Fragment 中
                    historyfragment.setArguments(bundle);
                    sensorListView.setVisibility(View.GONE);
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, historyfragment)
                            .addToBackStack(null)
                            .commit();
                }
            });

            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String maxValue = maxEditText.getText().toString();
                    String minValue = minEditText.getText().toString();
                    sendMinMaxValuesToServer(maxValue, minValue);
                }
            });
            return itemView;
        }
    }

    // 处理从 MqttClientManager 返回的数据
    @Override
    public void onMqttMessageReceived(String topic, MqttMessage message) {
        if (topic != null && topic.equals(TOPIC)) {
            updateUIWithMessage(new String(message.getPayload()));
        }
    }
    static class SensorDataManager {
        public static void addJsonToSensorList(String jsonString, LinkedList<SensorNode> sensorDataArray) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                // 获取所有的参数名
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String paramName = keys.next();
                    if (!paramName.equals("deviceName")) {
                        int paramValue = jsonObject.getInt(paramName);
                        sensorDataArray.add(new SensorNode(paramName, paramValue));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    // 更新 UI 显示收到的消息
    private void updateUIWithMessage(String message) {

        LinkedList<SensorNode> sensorDataArray = new LinkedList<>();
        SensorDataManager.addJsonToSensorList(message, sensorDataArray);
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        // 遍历 sensorList 中的每个传感器节点
        for (Sensor sensor : sensorList) {
            // 是否找到匹配的传感器数据
            boolean found = false;

            // 在 sensorDataArray 中查找相同 paramName 的节点
            for (SensorNode sensorNode : sensorDataArray) {
                if (sensor.getSensorName().equals(sensorNode.getParamName())) {
                    // 将 paramValue 转换为 String 类型并赋给 lastReading
                    sensor.setLastReading(String.valueOf(sensorNode.getParamValue()));
                    sensor.setReadingTime(currentTime);
                    // 找到匹配节点后设置 found 为 true，继续查找后续的传感器数据
                    found = true;
                    break; // 找到匹配节点后退出循环，因为一个传感器只能有一个参数值
                }
            }

            // 如果找到匹配的传感器数据，则继续处理下一个传感器
            if (found) {
                continue;
            }
        }
        // 通知适配器更新 UI
        adapter.notifyDataSetChanged();
    }
}
