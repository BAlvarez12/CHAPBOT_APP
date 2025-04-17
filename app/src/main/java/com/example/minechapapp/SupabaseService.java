package com.example.minechapapp;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
public interface SupabaseService {
    @Multipart
    @PUT("storage/v1/object/minechap/{fileName}")
    Call<ResponseBody> uploadFile(
            @Header("Authorization") String bearerToken,
            @Path("fileName") String fileName,
            @Part MultipartBody.Part file
    );
}
