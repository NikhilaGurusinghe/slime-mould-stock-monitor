package slimemouldstock.monitor.http.api;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import slimemouldstock.monitor.http.RetrofitClient;
import slimemouldstock.monitor.http.api.status.SlimeMouldDecision;
import slimemouldstock.monitor.http.api.status.SlimeMouldState;
import slimemouldstock.monitor.http.api.status.StatusService;

public class Communication {
    public static void sendStatusUpdate(SlimeMouldState state) {
        Retrofit retrofitClient = RetrofitClient.getInstance();

        StatusService statusAPI = retrofitClient.create(StatusService.class);

        RequestBody slimeMouldStateRequestBody =
                RequestBody.create(state.name(), MediaType.parse("text/plain"));

        Call<ResponseBody> call = statusAPI.POSTDefault(slimeMouldStateRequestBody);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
    }

    public static void sendStatusUpdate(SlimeMouldState state,
                                        SlimeMouldDecision decision,
                                        File image) {

    }
}
