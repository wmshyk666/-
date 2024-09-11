package com.example.smarthouse.ui.topic;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.smarthouse.MqttClientManager;
import com.example.smarthouse.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/** @noinspection ALL*/
class Topic {
    private String topicid;
    private String deviceId;
    private String topic;
    private  String deviceName;
    public Topic(JSONObject jsonObject) throws JSONException {
        this.topicid = jsonObject.getString("topicId");
        this.deviceId = jsonObject.getString("deviceId");
        this.deviceName = jsonObject.getString("deviceName");
        this.topic = jsonObject.getString("topic");
    }

    public String getId() {
        return topicid;
    }
    public String getDeviceId() {
        return deviceId;
    }
    public String getDeviceName() {
        return deviceName;
    }
    public String getTopic() {
        return topic;
    }
}

/** @noinspection ALL*/
public class TopicFragment extends Fragment {

    private ListView topicListView;
    private List<Topic> topicList;
    private TopicListAdapter adapter;
    private MqttClientManager mqttClientManager = new MqttClientManager();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_topic, container, false);


        topicListView = root.findViewById(R.id.topicListView);
        topicList = new ArrayList<>();
        adapter = new TopicListAdapter();
        topicListView.setAdapter(adapter);

        // 在这里调用从服务器获取数据的方法
        new FetchDataTask().execute("http://vipvip.xiaomiqiu.com:44468/deviceTopic/selectAll");
        return root;
    }

    // 异步任务，从服务器获取数据
    private class FetchDataTask extends AsyncTask<String, Void, List<Topic>> {

        @Override
        protected List<Topic> doInBackground(String... params) {
            String serverUrl = params[0];
            List<Topic> topics = new ArrayList<>();
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
                            Topic topic = new Topic(jsonObject);
                            String newtopic=jsonObject.getString("topic");
                            topics.add(topic);
                            mqttClientManager.connectToBroker(requireActivity(), jsonObject.getString("topic"), null);
                        }
                    } else {
                        // 请求不成功的处理
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return topics;
        }

        @Override
        protected void onPostExecute(List<Topic> topics) {
            super.onPostExecute(topics);
            topicList.clear();
            topicList.addAll(topics);
            adapter.notifyDataSetChanged();
        }
    }


    private class DeleteTopicTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            String topicId = params[0];
            try {
                URL url = new URL(String.format("http://vipvip.xiaomiqiu.com:44468/deviceTopic/delete/%s", topicId));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("DELETE");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return true; // Topic deleted successfully
                } else {
                    return false; // Unsuccessful deletion
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success) {
                showToast("取消订阅成功");
            } else {
                showToast("取消订阅失败");
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }


    // 设备列表适配器
    private class TopicListAdapter extends ArrayAdapter<Topic> {

        public TopicListAdapter() {
            super(requireActivity(), R.layout.item_device, topicList);
        }

        @SuppressLint("SetTextI18n")
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_topic, parent, false);
            }

            Topic currenttopic = topicList.get(position);
            TextView topicnameTextView = itemView.findViewById(R.id.TopicnameTextView);
            Button unsubscribeButton = itemView.findViewById(R.id.unsubscribeButton);

            unsubscribeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new DeleteTopicTask().execute(currenttopic.getId());
                    mqttClientManager.unsubscribeToTopic(currenttopic.getTopic());
                    topicList.remove(currenttopic);
                    adapter.notifyDataSetChanged();
                }
            });
            // 将设备信息设置到视图中
            topicnameTextView.setText("主题: " + currenttopic.getTopic());

            return itemView;
        }
    }
}