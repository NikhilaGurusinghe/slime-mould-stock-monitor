package slimemouldstock.monitor.mqtt;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

import info.mqtt.android.service.MqttAndroidClient;

// https://wildanmsyah.wordpress.com/2017/05/11/mqtt-android-client-tutorial/#dependencies
// TODO clean up logging
public class MqttClient {
    private MqttAndroidClient client;
    private final String clientID = "43464f4b-e823-4ae4-8e17-fe0b1dde0153-slime-mould-stock-monitor";
    // something like "tcp://broker.emqx.io:1883"
    private final String brokerURL;

    public MqttClient(Context context, String brokerURL) throws MqttException {
        this.brokerURL = brokerURL;
        client = new MqttAndroidClient(context, brokerURL, clientID);
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d("slimemouldstock.monitor/MqttClient",
                        "MQTT client " + (reconnect ? "reconnected" : "connected") +
                                " successfully to " + serverURI);
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d("slimemouldstock.monitor/MqttClient",
                        "MQTT client disconnected: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("slimemouldstock.monitor/MqttClient",
                        "MQTT client completed delivery");
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
                Log.d("slimemouldstock.monitor/MqttClient",
                        "MQTT client failed to connect to " + brokerURL + ": " +
                                exception.getMessage());
            }
        });

    }

    public void publishToTopic(String topic,
                               int qos,
                               Boolean retained,
                               String message) throws MqttException {
        if (!client.isConnected()) {
            // add to queue
        }

        // otherwise we can publish immediately
        MqttMessage publishMe = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));
        publishMe.setQos(qos);
        if (retained != null) {
            publishMe.setRetained(retained);
        }
        client.publish(topic, publishMe);
    }

    public boolean isClientConnected() {
        return client.isConnected();
    }
}
