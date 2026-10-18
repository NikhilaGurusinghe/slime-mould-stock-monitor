package slimemouldstock.monitor.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;

// https://oxylabs.io/blog/java-http-request
// https://www.baeldung.com/java-http-request
public class HttpClient {
    private final int CONNECT_TIMEOUT_MS = 5_000;
    private final int READ_TIMEOUT_MS = 5_000;
    private final URL CONNECTION_URL;
    private final URL DEFAULT_CONNECTION_URL = new URL("http://localhost:3000");
    private HttpURLConnection connection;

    public HttpClient(String connectionURL) throws MalformedURLException {
        this.CONNECTION_URL = connectionURL == null || connectionURL.isBlank() ?
                DEFAULT_CONNECTION_URL : new URL(connectionURL);
    }

    public boolean initConnection() {
        try {
            connection = (HttpURLConnection) CONNECTION_URL.openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        return true;
    }

    public boolean sendHttpRequest(HttpRequestMethods requestMethod,
                               HashMap<String, String> requestHeaders) {
        try {
            this.connection.setRequestMethod(requestMethod.name());
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        }

        // set request headers
        if (requestHeaders != null && !requestHeaders.isEmpty()) {
           requestHeaders.forEach((headerProperty, headerValue) -> {
               if (headerProperty != null && !headerProperty.isBlank() &&
                       headerValue != null && !headerValue.isBlank()) {
                   connection.setRequestProperty(headerProperty, headerValue);
               }
           });
        }



        return true;
    }

    public enum HttpRequestMethods {
        POST

    }
}
