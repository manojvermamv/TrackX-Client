/*
 * Headwind Remote: Open Source Remote Access Software for Android
 * https://headwind-remote.com
 *
 * Copyright (C) 2022 headwind-remote.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sus_client.control;

public class Const {
    public static final String LOG_TAG = "com.hmdm.Control";

    public static final int REQUEST_SETTINGS = 1000;
    public static final int REQUEST_PERMISSION_AUDIO = 1001;
    public static final int REQUEST_SCREEN_SHARE = 1002;
    public static final int REQUEST_FROM_NOTIFICATION = 1003;
    public static final int REQUEST_PERMISSION_OVERLAY = 1004;
    public static final int RESULT_DIRTY = 2000;

    public static final int SCREEN_RECORD_REQUEST_CODE = 777;
    public static final int DEVICE_ADMIN_REQUEST_CODE = 778;
    public static final int REQUEST_CODE_ACCESSIBILITY_SERVICE = 779;
    public static final int REQUEST_CODE_MEDIA_PROJECTION = 780;


    public static final int DEFAULT_BITRATE = 256000;
    public static final int DEFAULT_FRAME_RATE = 10;
    public static final int DEFAULT_IDLE_TIMEOUT = 120;
    public static final int DEFAULT_PING_TIMEOUT = 20;


    public static final int MAX_SHARED_SCREEN_WIDTH = 800;
    public static final int MAX_SHARED_SCREEN_HEIGHT = 800;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_SHARING = 3;
    public static final int STATE_DISCONNECTING = 4;

    public static final int CONNECTION_TIMEOUT = 60000;

    public static final String EXTRA_EVENT = "event";
    public static final String EXTRA_SESSION = "session";
    public static final String EXTRA_WEBRTCUP = "webrtcup";
    public static final String EXTRA_MESSAGE = "message";

    public static final int SUCCESS = 0;
    public static final int NETWORK_ERROR = 1;
    public static final int SERVER_ERROR = 2;
    public static final int INTERNAL_ERROR = 3;

    public static final String ACTION_JANUS_SESSION_POLL = "janus_session_poll";

    public static final String JANUS_PLUGIN_TEXTROOM = "janus.plugin.textroom";
    public static final String JANUS_PLUGIN_STREAMING = "janus.plugin.streaming";

    public static final int TEST_RTP_PORT = 1234;

    public static final String ACTION_SCREEN_SHARING_START = "SCREEN_SHARING_START";
    public static final String ACTION_SCREEN_SHARING_STOP = "SCREEN_SHARING_STOP";
    public static final String ACTION_SCREEN_SHARING_PERMISSION_NEEDED = "SCREEN_SHARING_PERMISSION_NEEDED";
    public static final String ACTION_SCREEN_SHARING_FAILED = "SCREEN_SHARING_FAILED";
    public static final String ACTION_CONNECTION_FAILURE = "CONNECTION_FAILURE";
    public static final String ACTION_GESTURE = "GESTURE";

    public static final int DEFAULT_SERVER_PORT = 9443;
    public static final String DEFAULT_SERVER_PATH = "/rtp-web-admin/";

    public static final String ERROR_ICE_FAILED = "iceFailed";


    // WEB INTERFACE VERSION CONTROL
    public static final String OLD_DIR = "web_screen_control_v1_0";
    public static final String NEW_DIR = "web_screen_control_v2_0";
    public static final String WEB_INTERFACE_DIR = "web-admin-screen-control";
}