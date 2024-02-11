package com.android.sus_client.control.janus.server;

import com.android.sus_client.control.janus.json.JanusAttachRequest;
import com.android.sus_client.control.janus.json.JanusJsepRequest;
import com.android.sus_client.control.janus.json.JanusMessageRequest;
import com.android.sus_client.control.janus.json.JanusPollResponse;
import com.android.sus_client.control.janus.json.JanusRequest;
import com.android.sus_client.control.janus.json.JanusResponse;
import com.android.sus_client.control.janus.json.JanusStreamingCreateRequest;
import com.android.sus_client.control.janus.json.JanusStreamingCreateResponse;

public interface JanusServerApi {

//    @POST("/janus")
//    @Headers("Content-Type: application/json")
//    Call<JanusResponse> createSession(@Body JanusRequest request);
//
//    @POST("/janus/{session}")
//    @Headers("Content-Type: application/json")
//    Call<JanusResponse> attachPlugin(@Path("session") String sessionId, @Body JanusAttachRequest request);
//
//    @POST("/janus/{session}/{handle}")
//    @Headers("Content-Type: application/json")
//    Call<JanusResponse> sendJsep(@Path("session") String sessionId, @Path("handle") String handleId, @Body JanusJsepRequest request);
//
//    @POST("/janus/{session}/{handle}")
//    @Headers("Content-Type: application/json")
//    Call<JanusResponse> sendMessage(@Path("session") String sessionId, @Path("handle") String handleId, @Body JanusMessageRequest request);
//
//    @POST("/janus/{session}/{handle}")
//    @Headers("Content-Type: application/json")
//    Call<JanusStreamingCreateResponse> createStreaming(@Path("session") String sessionId, @Path("handle") String handleId, @Body JanusStreamingCreateRequest request);
//
//    @POST("/janus/{session}")
//    @Headers("Content-Type: application/json")
//    Call<JanusResponse> destroySession(@Path("session") String sessionId, @Body JanusRequest request);
//
//    @GET("/janus/{session}")
//    Call<JanusPollResponse> poll(@Path("session") String sessionId, @Query("apisecret") String secret);

}