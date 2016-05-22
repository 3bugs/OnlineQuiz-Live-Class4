package com.example.onlinequizliveclass4.net;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.onlinequizliveclass4.model.ResponseStatus;
import com.example.onlinequizliveclass4.model.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Promlert on 5/21/2016.
 */
public class WebServices {

    private static final String TAG = WebServices.class.getSimpleName();

    private static final String BASE_URL = "http://10.0.3.2/online_quiz/";
    public static final String USER_IMAGES_URL = BASE_URL + "user_images/";

    private static final String LOGIN_URL = BASE_URL + "login.php?username=%s&password=%s";
    private static final String ADD_USER_URL = BASE_URL + "add_user.php";

    private static final OkHttpClient mClient = new OkHttpClient();

    private static User mUser;
    private static ResponseStatus mResponseStatus;

    public interface LoginCallback {
        void onFailure(IOException e);
        void onResponse(ResponseStatus responseStatus, User user);
    }

    public interface AddUserCallback {
        void onFailure(IOException e);
        void onResponse(ResponseStatus responseStatus);
    }

    public static void login(String username,
                             String password,
                             final LoginCallback callback) {

        Request request = new Request.Builder()
                .url(String.format(LOGIN_URL, username, password))
                .build();

        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                new Handler(Looper.getMainLooper()).post(
                        new Runnable() {
                            @Override
                            public void run() {
                                callback.onFailure(e);
                            }
                        }
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonResult = response.body().string();

                delay(1);

                try {
                    JSONObject jsonObject = new JSONObject(jsonResult);
                    int success = jsonObject.getInt("success");

                    if (success == 1) {
                        mResponseStatus = new ResponseStatus(true, null);
                        // เชื่อมต่อ MySQL สำเร็จ
                        int loginSuccess = jsonObject.getInt("login_success");

                        if (loginSuccess == 1) {
                            // ล็อกอินสำเร็จ
                            int userId = jsonObject.getInt("user_id");
                            String name = jsonObject.getString("name");
                            String username = jsonObject.getString("username");
                            String picture = jsonObject.getString("picture");

                            mUser = new User(userId, name, username, picture);
                        } else if (loginSuccess == 0) {
                            // ล็อกอินไม่สำเร็จ
                            mUser = null;
                        }

                    } else if (success == 0) {
                        // เชื่อมต่อ MySQL ไม่สำเร็จ
                        mResponseStatus = new ResponseStatus(false, jsonObject.getString("message"));
                        mUser = null;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Error parsing JSON");
                }

                new Handler(Looper.getMainLooper()).post(
                        new Runnable() {
                            @Override
                            public void run() {
                                callback.onResponse(mResponseStatus, mUser);
                            }
                        }
                );

            }
        });
    }

    private static void delay(int second) {
        try {
            Thread.sleep(second * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpg");

    public static void addUser(String name, String email, String password, String pictureFilePath,
                               final AddUserCallback callback) {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("name", name)
                .addFormDataPart("username", email)
                .addFormDataPart("password", password)
                .addFormDataPart("picture", "picture.jpg",
                        RequestBody.create(MEDIA_TYPE_JPEG, new File(pictureFilePath)))
                .build();

        Request request = new Request.Builder()
                .url(ADD_USER_URL)
                .post(requestBody)
                .build();

        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                new Handler(Looper.getMainLooper()).post(
                        new Runnable() {
                            @Override
                            public void run() {
                                callback.onFailure(e);
                            }
                        }
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                delay(1);

                final String jsonResult = response.body().string();
                Log.d(TAG, jsonResult);

                try {
                    JSONObject jsonObject = new JSONObject(jsonResult);
                    int success = jsonObject.getInt("success");

                    if (success == 1) {
                        mResponseStatus = new ResponseStatus(true, jsonObject.getString("message"));
                    } else if (success == 0) {
                        mResponseStatus = new ResponseStatus(false, jsonObject.getString("message"));
                    }
                } catch (JSONException e) {
                    mResponseStatus = new ResponseStatus(false, "Error parsing JSON.");
                    Log.e(TAG, "Error parsing JSON.");
                    e.printStackTrace();
                }

                new Handler(Looper.getMainLooper()).post(
                        new Runnable() {
                            @Override
                            public void run() {
                                callback.onResponse(mResponseStatus);
                            }
                        }
                );
            }
        });
    }

}










