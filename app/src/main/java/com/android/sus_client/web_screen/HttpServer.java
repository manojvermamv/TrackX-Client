//package com.android.sus_client.web_screen;
//
//import android.content.Context;
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//import android.widget.Toast;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.security.KeyStore;
//import java.util.Timer;
//import java.util.TimerTask;
//
//import javax.net.ssl.KeyManagerFactory;
//
//
//public class HttpServer {
//    private static final String TAG = HttpServer.class.getSimpleName();
//
//    private static final String HTML_DIR = "html/";
//    private static final String INDEX_HTML = "index.html";
//    private static final String MIME_IMAGE_SVG = "image/svg+xml";
//    private static final String MIME_JS = "text/javascript";
//    private static final String MIME_TEXT_PLAIN_JS = "text/plain";
//    private static final String MIME_TEXT_CSS = "text/css";
//    private static final String TYPE_PARAM = "type";
//    private static final String TYPE_VALUE_MOUSE_UP = "mouse_up";
//    private static final String TYPE_VALUE_MOUSE_MOVE = "mouse_move";
//    private static final String TYPE_VALUE_MOUSE_DOWN = "mouse_down";
//    private static final String TYPE_VALUE_MOUSE_ZOOM_IN = "mouse_zoom_in";
//    private static final String TYPE_VALUE_MOUSE_ZOOM_OUT = "mouse_zoom_out";
//    private static final String TYPE_VALUE_BUTTON_BACK = "button_back";
//    private static final String TYPE_VALUE_BUTTON_HOME = "button_home";
//    private static final String TYPE_VALUE_BUTTON_RECENT = "button_recent";
//    private static final String TYPE_VALUE_BUTTON_POWER = "button_power";
//    private static final String TYPE_VALUE_BUTTON_LOCK = "button_lock";
//    private static final String TYPE_VALUE_JOIN = "join";
//    private static final String TYPE_VALUE_SDP = "sdp";
//    private static final String TYPE_VALUE_ICE = "ice";
//    private static final String TYPE_VALUE_BYE = "bye";
//
//    private Context context;
//    Ws webSocket = null;
//
//    private HttpServerInterface httpServerInterface;
//
//    public HttpServer(Context context, HttpServerInterface httpServerInterface) {
//        this.context = context;
//        this.httpServerInterface = httpServerInterface;
//    }
//
//    public interface HttpServerInterface {
//        void onMouseDown(JSONObject message);
//        void onMouseMove(JSONObject message);
//        void onMouseUp(JSONObject message);
//        void onMouseZoomIn(JSONObject message);
//        void onMouseZoomOut(JSONObject message);
//        void onButtonBack();
//        void onButtonHome();
//        void onButtonRecent();
//        void onButtonPower();
//        void onButtonLock();
//        void onJoin(HttpServer server);
//        void onSdp(JSONObject message);
//        void onIceCandidate(JSONObject message);
//        void onBye();
//    }
//
//    public void send(String message) throws IOException {
//        webSocket.send(message);
//    }
//
//    public void handleRequest(JSONObject request) {
//        String type;
//        try {
//            type = request.getString(TYPE_PARAM);
//        } catch (JSONException e) {
//            e.printStackTrace();
//            return;
//        }
//
//        switch (type) {
//            case TYPE_VALUE_MOUSE_DOWN:
//                httpServerInterface.onMouseDown(request);
//                break;
//            case TYPE_VALUE_MOUSE_MOVE:
//                httpServerInterface.onMouseMove(request);
//                break;
//            case TYPE_VALUE_MOUSE_UP:
//                httpServerInterface.onMouseUp(request);
//                break;
//            case TYPE_VALUE_MOUSE_ZOOM_IN:
//                httpServerInterface.onMouseZoomIn(request);
//                break;
//            case TYPE_VALUE_MOUSE_ZOOM_OUT:
//                httpServerInterface.onMouseZoomOut(request);
//                break;
//            case TYPE_VALUE_BUTTON_BACK:
//                httpServerInterface.onButtonBack();
//                break;
//            case TYPE_VALUE_BUTTON_HOME:
//                httpServerInterface.onButtonHome();
//                break;
//            case TYPE_VALUE_BUTTON_RECENT:
//                httpServerInterface.onButtonRecent();
//                break;
//            case TYPE_VALUE_BUTTON_POWER:
//                httpServerInterface.onButtonPower();
//                break;
//            case TYPE_VALUE_BUTTON_LOCK:
//                httpServerInterface.onButtonLock();
//                break;
//            case TYPE_VALUE_JOIN:
//                httpServerInterface.onJoin(this);
//                break;
//            case TYPE_VALUE_SDP:
//                httpServerInterface.onSdp(request);
//                break;
//            case TYPE_VALUE_ICE:
//                httpServerInterface.onIceCandidate(request);
//                break;
//            case TYPE_VALUE_BYE:
//                httpServerInterface.onBye();
//                break;
//        }
//    }
//
//}
