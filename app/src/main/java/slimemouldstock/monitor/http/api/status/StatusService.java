package slimemouldstock.monitor.http.api.status;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface StatusService {
    // does not need prepended slash
    String API_ENDPOINT = "v1/status";

    @Multipart
    @POST(API_ENDPOINT)
    Call<ResponseBody> POSTDefault(@Part("status") RequestBody status);

    @Multipart
    @POST(API_ENDPOINT)
    Call<ResponseBody> POSTDecision(@Part("status") RequestBody status,
                                    @Part("decision") RequestBody decision,
                                    @Part MultipartBody.Part image);
}
