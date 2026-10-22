package slimemouldstock.monitor.http.api;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
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

public class APIUtils {
    public static void sendStatusUpdate(SlimeMouldState state) {
        Retrofit retrofitClient = RetrofitClient.getInstance();

        StatusService statusAPI = retrofitClient.create(StatusService.class);

        RequestBody slimeMouldStateRequestBody =
                RequestBody.create(state.name(), MediaType.parse("text/plain"));

        Call<ResponseBody> postRequest = statusAPI.POSTDefault(slimeMouldStateRequestBody);

        postRequest.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
    }

    // image must be a jpeg encoded image held in memory
    // use something like Imgcodecs.imencode to do this (for an openCV mat)
    // imageName must end with .jpg
    public static void sendStatusUpdate(SlimeMouldState state,
                                        SlimeMouldDecision decision,
                                        byte[] jpegImage,
                                        String imageName) {
        Retrofit retrofitClient = RetrofitClient.getInstance();

        StatusService statusAPI = retrofitClient.create(StatusService.class);

        RequestBody slimeMouldJPEGRequestBody =
                RequestBody.create(jpegImage, MediaType.parse("image/jpeg"));

        // the name here is the field name of the formData field,
        // it does NOT correspond to the filename sent over the wire
        MultipartBody.Part imagePart =
                MultipartBody.Part.createFormData("image", imageName, slimeMouldJPEGRequestBody);

        RequestBody slimeMouldStateRequestBody =
                RequestBody.create(state.name(), MediaType.parse("text/plain"));

        RequestBody slimeMouldDecisionRequestBody =
                RequestBody.create(decision.name(), MediaType.parse("text/plain"));

        Call<ResponseBody> postRequest = statusAPI.POSTDecision(slimeMouldStateRequestBody,
                                                                slimeMouldDecisionRequestBody,
                                                                imagePart);

        postRequest.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
    }
}
