package com.example.smarthouse;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NewSubActivity extends AppCompatActivity {

    private String serverUrl = "http://vipvip.xiaomiqiu.com:44468/deviceTopic/add"; // 服务器API的URL
    private EditText deviceNameEdit;
    private EditText topicContentEdit;
    private MqttClientManager mqttClientManager = new MqttClientManager();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newsub);

        deviceNameEdit = findViewById(R.id.device_name);
        topicContentEdit = findViewById(R.id.topic_content);
        Button subscribeButton = findViewById(R.id.subscribe_button);

        subscribeButton.setOnClickListener(v -> {
            String deviceName = deviceNameEdit.getText().toString();
            String topicContent = topicContentEdit.getText().toString();
            new SubscribeTask(this, deviceName, topicContent).execute(serverUrl, deviceName, topicContent);
        });
    }

    private class SubscribeTask extends AsyncTask<String, Void, JSONObject> {
        private final AppCompatActivity mActivity;
        private String deviceName;
        private String topicContent;

        SubscribeTask(AppCompatActivity activity, String deviceName, String topicContent) {
            this.mActivity = activity;
            this.deviceName = deviceName;
            this.topicContent = topicContent;
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            String serverUrl = params[0];
            try {
                URL urlObj = new URL(serverUrl);
                HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");

                // 将参数转换为JSON格式
                JSONObject jsonParams = new JSONObject();
                jsonParams.put("deviceName", this.deviceName);
                jsonParams.put("topic", this.topicContent);

                OutputStream os = connection.getOutputStream();
                os.write(jsonParams.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

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

                    // 根据服务器响应判断订阅是否成功
                    return new JSONObject(response.toString());
                } else {
                    return null; // 返回 null 表示发生错误
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                return null; // 返回 null 表示发生错误
            }
        }

        @Override
        protected void onPostExecute(JSONObject jsonResponse) {
            // 根据异步任务的结果更新UI
            try {
                if (jsonResponse != null && jsonResponse.has("code") && jsonResponse.getInt("code") == 200) {
                    // 订阅成功

                    mqttClientManager.subscribeToTopic(topicContent);
                    Toast.makeText(mActivity.getApplicationContext(), "订阅成功", Toast.LENGTH_SHORT).show();
                } else {
                    // 订阅失败
                    Toast.makeText(mActivity.getApplicationContext(), "订阅失败，请返回设备表检查您输入的设备名是否存在", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
