package com.example.smarthouse.ui.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.smarthouse.MainActivity;
import com.example.smarthouse.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/** @noinspection ALL*/
class Device {
    public int getDeviceId() {
        return deviceId;
    }
    public String getDeviceName() {
        return deviceName;
    }
    public String getDeviceType() {
        return deviceType;
    }
    public int getIsOnline() {
        return isOnline;
    }
    public String getLocation() {
        return location;
    }
    public Timestamp getLastOnline() {
        return lastOnline;
    }
    private int deviceId;
    private String deviceName;
    private String deviceType;
    private int isOnline ; // 默认为0，表示离线
    private String location;
    private Timestamp lastOnline = new Timestamp(System.currentTimeMillis()); // 默认为null
    public Device(JSONObject jsonObject) throws JSONException {
        this.deviceId = jsonObject.getInt("deviceId");
        this.deviceName = jsonObject.getString("deviceName");
        this.deviceType = jsonObject.getString("deviceType");
        this.isOnline = jsonObject.getInt("isOnline");
        this.location = jsonObject.getString("location");
        // 如果 lastOnline 是字符串类型的时间戳，则需要进行适当的转换
        String lastOnlineStr = jsonObject.getString("lastOnline");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        try {
            Date lastOnlineDate = sdf.parse(lastOnlineStr);
            if (lastOnlineDate != null) {
                this.lastOnline = new Timestamp(lastOnlineDate.getTime());
            }
        } catch (ParseException e) {
            e.printStackTrace();
            // 处理日期解析异常，例如使用默认日期或者设置为 null
            this.lastOnline = null; // 设置为 null 或者其他默认值
        }
    }
}

/** @noinspection ALL*/
public class HomeFragment extends Fragment {

    private ListView deviceListView;
    private List<Device> deviceList;
    private DeviceListAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        deviceListView = root.findViewById(R.id.deviceListView);
        deviceList = new ArrayList<>();
        adapter = new DeviceListAdapter();
        deviceListView.setAdapter(adapter);

        // 在这里调用从服务器获取数据的方法
        new FetchDataTask().execute("http://vipvip.xiaomiqiu.com:44468/device/selectAll");
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 获取点击的设备
                Device selectedDevice = deviceList.get(position);
                // 创建 SensorFragment 实例
                SensorFragment sensorFragment = new SensorFragment();
                // 创建 Bundle 对象，用于传递参数
                Bundle bundle = new Bundle();
                bundle.putString("deviceName", selectedDevice.getDeviceName());
                int deviceId=selectedDevice.getDeviceId();
                bundle.putInt("deviceId",deviceId);
                // 将参数设置到 Fragment 中
                sensorFragment.setArguments(bundle);
                // 隐藏 deviceListView
                deviceListView.setVisibility(View.GONE);
                // 切换到 SensorFragment
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, sensorFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        return root;
    }

    // 异步任务，从服务器获取数据
    private class FetchDataTask extends AsyncTask<String, Void, List<Device>> {

        private int maxAttempts = 3; // 最大重试次数
        private int currentAttempt = 0; // 当前重试次数
        private int connectionTimeout = 5000; // 连接超时时间，单位：毫秒
        private String serverUrl;

        @Override
        protected List<Device> doInBackground(String... params) {
            serverUrl = params[0];
            return fetchDataFromServer();
        }

        private List<Device> fetchDataFromServer() {
            List<Device> devices = new ArrayList<>();
            boolean connected = false;
            while (!connected && currentAttempt < maxAttempts) {
                currentAttempt++;
                try {
                    URL url = new URL(serverUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(connectionTimeout); // 设置连接超时时间

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        connected = true;
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
                                Device device = new Device(jsonObject);
                                devices.add(device);
                            }
                        } else {
                            // 请求不成功的处理
                        }
                    } else {
                        // 请求不成功的处理
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    // 处理连接超时的情况
                    if (e instanceof SocketTimeoutException) {
                        // 显示连接超时的消息
                        Activity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(requireContext(), "连接服务器超时，请重试", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }
                if (!connected && currentAttempt < maxAttempts) {
                    // 等待一段时间后重试
                    try {
                        Thread.sleep(1000); // 等待1秒后重试
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            return devices;
        }

        @Override
        protected void onPostExecute(List<Device> devices) {
            super.onPostExecute(devices);
            if (!devices.isEmpty()) {
                deviceList.clear();
                deviceList.addAll(devices);
                adapter.notifyDataSetChanged();
            } else {
                // 显示连接失败的消息并允许用户重试
                Toast.makeText(requireContext(), "连接服务器失败，请重试", Toast.LENGTH_SHORT).show();
                // 提示连接失败后，尝试重新连接
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setMessage("连接服务器失败，是否重试？")
                        .setCancelable(false)
                        .setPositiveButton("重试", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // 重新连接
                                currentAttempt = 0; // 重置重试次数
                                new FetchDataTask().execute(serverUrl);
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        }
    }


    // 设备列表适配器
    private class DeviceListAdapter extends ArrayAdapter<Device> {

        public DeviceListAdapter() {
            super(requireActivity(), R.layout.item_device, deviceList);
        }

        @SuppressLint("SetTextI18n")
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_device, parent, false);
            }

            Device currentDevice = deviceList.get(position);

            // 找到 item_device.xml 中的视图
            //TextView deviceIdTextView = itemView.findViewById(R.id.deviceIdTextView);
            TextView deviceNameTextView = itemView.findViewById(R.id.deviceNameTextView);
            TextView deviceTypeTextView = itemView.findViewById(R.id.deviceTypeTextView);
            TextView isOnlineTextView = itemView.findViewById(R.id.isOnlineTextView);
            TextView locationTextView = itemView.findViewById(R.id.locationTextView);
            TextView lastOnlineTextView = itemView.findViewById(R.id.lastOnlineTextView);
            TextView massageTextView = itemView.findViewById(R.id.massageTextView);
            // 将设备信息设置到视图中
            //deviceIdTextView.setText("设备ID: " + currentDevice.getDeviceId());
            deviceNameTextView.setText("设备名称: " + currentDevice.getDeviceName());
            deviceTypeTextView.setText("设备类型: " + currentDevice.getDeviceType());
            isOnlineTextView.setText("在线状态: " + (currentDevice.getIsOnline() == 1 ? "在线" : "离线"));
            locationTextView.setText("位置: " + currentDevice.getLocation());
            lastOnlineTextView.setText("最后在线时间: " + currentDevice.getLastOnline());
            massageTextView.setText("设备情况："+(currentDevice.getIsOnline() == 1 ? "在线" : "离线"));
            return itemView;
        }
    }
}
