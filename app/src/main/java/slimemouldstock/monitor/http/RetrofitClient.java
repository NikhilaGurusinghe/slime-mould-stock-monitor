package slimemouldstock.monitor.http;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

// Singleton class
// https://oxylabs.io/blog/java-http-request
// https://www.baeldung.com/java-http-request
// retrofit for android
// https://androidclarified.wordpress.com/2018/08/19/android-image-upload-example/
// https://nayosx.blogspot.com/2021/08/how-to-upload-file-from-form-data-post.html
// https://www.geeksforgeeks.org/android/how-to-post-data-to-api-using-retrofit-in-android/
// https://medium.com/@myofficework000/uploading-files-with-multipart-form-data-in-retrofit-android-f9a4fecba360
// https://medium.com/@mahbooberezaee68/upload-audio-file-with-post-multipart-form-data-using-retrofit-2-0-in-kotlin-6d050e293ac5
public class RetrofitClient {
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 5_000;
    private static final int WRITE_TIMEOUT_MS = 5_000;
    // TODO currently this is hardcoded in, but need a better API to specify this
    //  on init of singleton class, difficult to do cause this is a singleton
    // requires a trailing slash
    // if using http
    // ensure that android:usesCleartextTraffic="true" is added in the manifest's application
    private static final String DEFAULT_CONNECTION_URL = "http://192.168.0.119:3000/";
    private static volatile Retrofit instance;

    private RetrofitClient() {}

    public static Retrofit getInstance() {
        if (instance == null) {
            synchronized (RetrofitClient.class) {
                if (instance == null) {
                    final OkHttpClient httpClient = new OkHttpClient.Builder()
                            .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                            .writeTimeout(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                            .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                            .build();

                    instance = new Retrofit.Builder()
                            .client(httpClient)
                            .baseUrl(DEFAULT_CONNECTION_URL)
                            .build();
                }
            }
        }

        return instance;
    }
}
