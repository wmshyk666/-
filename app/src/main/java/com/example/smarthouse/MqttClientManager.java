package com.example.smarthouse;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttClientManager {

    private MqttAndroidClient mqttAndroidClient;
    private MqttConnectOptions mqttConnectOptions;
    private boolean isConnected = false;
    private String SERVER_URI = "tcp://123.56.191.147:37926"; // MQTT Broker地址
    private MqttMessageListener mqttMessageListener; // Listener for MQTT messages

    public void connectToBroker(Activity activity, String topic, String control) {
        if (!isConnected) { // 如果尚未连接
            mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setCleanSession(true);
            mqttConnectOptions.setAutomaticReconnect(true);
            String clientId = "mqttx_hyk";
            mqttAndroidClient = new MqttAndroidClient(activity.getApplicationContext(), SERVER_URI, clientId);

            try {
                mqttAndroidClient.connect(mqttConnectOptions, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        isConnected = true; // 连接成功后标记为已连接
                        subscribeToTopic(topic);
                        if (control != null) {
                            publishMessage(topic, control);
                        }
                        setMqttCallback();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        exception.printStackTrace();
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else { // 如果已经连接
            subscribeToTopic(topic);
            if (control != null) {
                publishMessage(topic, control);
            }
        }
    }

    // Set MQTT client callback
    private void setMqttCallback() {
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                isConnected = false; // 处理连接丢失
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                if (mqttMessageListener != null) {
                    mqttMessageListener.onMqttMessageReceived(topic, message);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Handle message delivery completion
            }
        });
    }

    // Set MQTT message listener
    public void setMqttMessageListener(MqttMessageListener listener) {
        this.mqttMessageListener = listener;
    }

    // Subscribe to a topic
    public void subscribeToTopic(String topic) {
        try {
            mqttAndroidClient.subscribe(topic, 2);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // Publish a message
    public void publishMessage(String topic, String message) {
        try {
            mqttAndroidClient.publish(topic, message.getBytes(), 1, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // Unsubscribe from a topic
    public void unsubscribeToTopic(String topic) {
        try {
            mqttAndroidClient.unsubscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public interface MqttMessageListener {
        void onMqttMessageReceived(String topic, MqttMessage message);
    }
}
