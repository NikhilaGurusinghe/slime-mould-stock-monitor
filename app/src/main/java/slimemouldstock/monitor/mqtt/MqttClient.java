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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import info.mqtt.android.service.MqttAndroidClient;

// https://wildanmsyah.wordpress.com/2017/05/11/mqtt-android-client-tutorial/#dependencies
public class MqttClient {

    // debug
    private final String logCatTag = "slimemouldstock.monitor/MqttClient";

    private MqttAndroidClient client;
    private final String clientID = "43464f4b-e823-4ae4-8e17-fe0b1dde0153-slime-mould-stock-monitor";
    // something like "tcp://broker.emqx.io:1883"
    private final String brokerURL;

    // message queuing stuff
    private final Queue<PendingMessage> pendingMessages = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isEmptyingPendingMessages = new AtomicBoolean(false);

    public MqttClient(Context context, String brokerURL) throws MqttException {
        this.brokerURL = brokerURL;
        client = new MqttAndroidClient(context, brokerURL, clientID);
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                emptyPendingMessages();

                Log.d(logCatTag,
                        "MQTT client " + (reconnect ? "reconnected" : "connected") +
                                " successfully to " + serverURI);
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d(logCatTag,
                        "MQTT client disconnected: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(logCatTag,
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
                Log.d(logCatTag,
                        "MQTT client failed to connect to " + brokerURL + ": " +
                                exception.getMessage());
            }
        });

    }

    public void publishToTopic(String topic,
                               int qos,
                               Boolean retained,
                               String message) {
        pendingMessages.add(new PendingMessage(topic, qos, retained, message));

        // TODO might have to replace this with volatile boolean if .isConnected is not thread safe
        if (client.isConnected()) {
            emptyPendingMessages();
        }
    }

    private void emptyPendingMessages() {
        // TODO might have to replace this with volatile boolean if .isConnected is not thread safe
        if (!client.isConnected()) {
            return;
        }

        // stops race by only letting one call through
        if (!isEmptyingPendingMessages.compareAndSet(false, true)) {
            return;
        }

        try {
            PendingMessage pendingMessage;

            // whilst we're connected with messages in our pendingMessages queue
            while (client.isConnected() && (pendingMessage = pendingMessages.poll()) != null) {
                try {
                    MqttMessage publishMe = new MqttMessage(
                            pendingMessage.payload.getBytes(StandardCharsets.UTF_8)
                    );
                    publishMe.setQos(pendingMessage.qos);
                    if (pendingMessage.retained != null) {
                        publishMe.setRetained(pendingMessage.retained);
                    }

                    client.publish(pendingMessage.topic,
                            publishMe,
                            pendingMessage,
                            new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            Log.d(logCatTag, "Message delivered");
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            // requeue failed message for redelivery
                            // TODO this does not guarantee ordering, so this could have unintended
                            //  consequences
                            PendingMessage failedMessage =
                                    (PendingMessage) asyncActionToken.getUserContext();

                            pendingMessages.offer(failedMessage);

                            Log.e(logCatTag, "Message delivery failed; requeued", exception);
                        }
                    });
                } catch (Exception exception) {
                    // requeue failed message for redelivery
                    // TODO this does not guarantee ordering, so this could have unintended
                    pendingMessages.offer(pendingMessage);
                    Log.e(logCatTag, "Message delivery failed; requeued", exception);
                }
            }
        } finally {
            // queue should be empty here
            isEmptyingPendingMessages.set(false);

            // if not continue emptying the queue
            if (client.isConnected() && !pendingMessages.isEmpty()) {
                emptyPendingMessages();
            }
        }
    }

}
