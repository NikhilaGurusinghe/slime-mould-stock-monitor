package slimemouldstock.monitor.mqtt;

public class PendingMessage {
    public final String topic;
    public final int qos;
    public final Boolean retained;
    public final String payload;

    public PendingMessage(String topic, int qos, Boolean retained, String payload) {
        this.topic = topic;
        this.qos = qos;
        this.retained = retained;
        this.payload = payload;
    }
}
