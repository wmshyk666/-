package com.example.smarthouse.ui.control;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smarthouse.MqttClientManager;
import com.example.smarthouse.R;


import java.util.HashMap;
import java.util.Map;

/** @noinspection ALL*/
public class ControlFragment extends Fragment {
    String topic = "control/设备1";
    private ListView controlListView;
    private Map<String, String> deviceStatus;
    private MqttClientManager mqttClientManager = new MqttClientManager();
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_control, container, false);

        // 初始化设备状态
        deviceStatus = new HashMap<>();
        deviceStatus.put("fan", "0");
        deviceStatus.put("led", "0");
        deviceStatus.put("pump", "0");

        controlListView = view.findViewById(R.id.controlListView);

        // 创建设备列表的适配器，使用自定义的item_control布局
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), R.layout.item_control, deviceStatus.keySet().toArray(new String[0])) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                @SuppressLint("ViewHolder") View view = LayoutInflater.from(getContext()).inflate(R.layout.item_control, parent, false);
                TextView decivenameTextView = view.findViewById(R.id.decivenameTextView);
                Button closeButton = view.findViewById(R.id.closeButton);
                Button smallButton = view.findViewById(R.id.smallButton);
                Button middleButton = view.findViewById(R.id.middleButton);
                Button bigButton = view.findViewById(R.id.bigButton);
                ImageView fanImageView = view.findViewById(R.id.fanImageView);

                String deviceName = getItem(position);
                decivenameTextView.setText(deviceName);

                // 根据设备名称设置图片
                if (deviceName != null) {
                    switch (deviceName) {
                        case "fan":
                            fanImageView.setImageResource(R.drawable.fan);
                            decivenameTextView.setText("风扇");
                            break;
                        case "pump":
                            fanImageView.setImageResource(R.drawable.shuibeng);
                            decivenameTextView.setText("水泵");
                            break;
                        case "led":
                            fanImageView.setImageResource(R.drawable.light);
                            decivenameTextView.setText("补光灯");
                            break;
                    }
                }

                closeButton.setOnClickListener(v -> {
                    updateDeviceStatus(deviceName, "0");
                    showToast();
                });

                smallButton.setOnClickListener(v -> {
                    updateDeviceStatus(deviceName, "33");
                    showToast();
                });

                middleButton.setOnClickListener(v -> {
                    updateDeviceStatus(deviceName, "66");
                    showToast();
                });

                bigButton.setOnClickListener(v -> {
                    updateDeviceStatus(deviceName, "100");
                    showToast();
                });

                return view;
            }
        };
        controlListView.setAdapter(adapter);
        return view;
    }

    // 更新设备状态
    private void updateDeviceStatus(String deviceName, String status) {
        deviceStatus.put(deviceName, status);
        // 更新列表显示
        ((ArrayAdapter) controlListView.getAdapter()).notifyDataSetChanged();
    }

    // 显示Toast提示，并发送 MQTT 消息
    private void showToast() {
        // 构建发送的 JSON 数据
        StringBuilder message = new StringBuilder("{");
        for (Map.Entry<String, String> entry : deviceStatus.entrySet()) {
            message.append("\"").append(entry.getKey()).append("\":").append(entry.getValue()).append(",");
        }
        // 删除末尾的逗号
        if (message.length() > 1) {
            message.deleteCharAt(message.length() - 1);
        }
        message.append("}");

        Toast.makeText(requireContext(), message.toString(), Toast.LENGTH_SHORT).show();

        try {
            // 发送 MQTT 消息
            mqttClientManager.connectToBroker(requireActivity(), topic, message.toString());
        } catch (Exception e) {
            // 处理连接失败或其他异常情况
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to connect to MQTT broker", Toast.LENGTH_SHORT).show();
        }
    }

}
