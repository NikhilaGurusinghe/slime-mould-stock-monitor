package slimemouldstock.monitor.mqtt;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnect;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

// https://wildanmsyah.wordpress.com/2017/05/11/mqtt-android-client-tutorial/#dependencies
// TODO clean up logging
public class MqttClient {
    private MqttAndroidClient client;
    private final String clientID = UUID.randomUUID().toString() + "-slime-mould-stock-monitor";
    private final String brokerURL;

    public MqttClient(Context context, String brokerURL) throws MqttException {
        this.brokerURL = brokerURL;
        client = new MqttAndroidClient(context, brokerURL, clientID);
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d("SlimeMouldStockMonitor/MqttClient",
                        "MQTT client " + (reconnect ? "reconnected" : "connected") +
                                " successfully to " + serverURI);
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d("SlimeMouldStockMonitor/MqttClient",
                        "MQTT client disconnected: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        connectToBroker();
    }

    private void connectToBroker() throws MqttException {
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setCleanSession(false);

        client.connect(connectOptions, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                disconnectedBufferOptions.setBufferEnabled(true);
                disconnectedBufferOptions.setBufferSize(100);
                disconnectedBufferOptions.setPersistBuffer(false);
                disconnectedBufferOptions.setDeleteOldestMessages(false);
                client.setBufferOpts(disconnectedBufferOptions);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.d("SlimeMouldStockMonitor/MqttClient",
                        "MQTT client failed to connect to " + brokerURL + ": " +
                                exception.getMessage());
            }
        });

    }

    public void publishToTopic(String topic, int qos, Boolean retained, String message) throws MqttException {
        MqttMessage publishMe = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));
        publishMe.setQos(qos);
        if (retained != null) {
            publishMe.setRetained(retained);
        }
        client.publish(topic, publishMe);
    }
}
