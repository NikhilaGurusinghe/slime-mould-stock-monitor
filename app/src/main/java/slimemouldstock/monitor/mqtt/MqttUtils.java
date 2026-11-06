package slimemouldstock.monitor.mqtt;

public class MqttUtils {
    public static void sendTurnLedOn(MqttClient mqttClient) {
        mqttClient.publishToTopic("slime-mould-stock-monitor/led", 2,
                true, "on");
    }

    public static void sendTurnLedOff(MqttClient mqttClient) {
        mqttClient.publishToTopic("slime-mould-stock-monitor/led", 2,
                true, "off");
    }
}
