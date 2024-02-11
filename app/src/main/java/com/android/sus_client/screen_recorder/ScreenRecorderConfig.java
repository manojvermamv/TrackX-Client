package com.android.sus_client.screen_recorder;

import static com.android.sus_client.utils.Constants.NO_SPECIFIED_MAX_SIZE;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * @author：luck
 * @date：2021/5/18 7:30 PM
 * @describe：MediaExtraInfo
 */
public class ScreenRecorderConfig implements Serializable {

    private int mScreenWidth = 360;
    private int mScreenHeight = 640;
    private int mScreenDensity;

    private boolean isAudioEnabled = true;
    private boolean isVideoHDEnabled = true;
    private String outputPath;
    private String fileName;
    private int audioBitrate = 0;
    private int audioSamplingRate = 0;

    private String audioSource = "MIC";
    private String videoEncoder = "DEFAULT";
    private boolean enableCustomSettings = false;
    private int videoFrameRate = 30;
    private int videoBitrate = 4000000;
    private String outputFormat = "DEFAULT";
    private int orientation;
    private long maxFileSize = NO_SPECIFIED_MAX_SIZE; // Default no max size

    private int maxDuration = 0;

    public int getScreenWidth() {
        return mScreenWidth;
    }

    public void setScreenWidth(int mScreenWidth) {
        this.mScreenWidth = mScreenWidth;
    }

    public int getScreenHeight() {
        return mScreenHeight;
    }

    public void setScreenHeight(int mScreenHeight) {
        this.mScreenHeight = mScreenHeight;
    }

    public int getScreenDensity() {
        return mScreenDensity;
    }

    public void setScreenDensity(int mScreenDensity) {
        this.mScreenDensity = mScreenDensity;
    }

    /*Enable/Disable audio*/
    public boolean isAudioEnabled() {
        return isAudioEnabled;
    }

    public void setAudioEnabled(boolean audioEnabled) {
        isAudioEnabled = audioEnabled;
    }

    public boolean isVideoHDEnabled() {
        return isVideoHDEnabled;
    }

    public void setVideoHDEnabled(boolean videoHDEnabled) {
        isVideoHDEnabled = videoHDEnabled;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getAudioBitrate() {
        return audioBitrate;
    }

    public void setAudioBitrate(int audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    public int getAudioSamplingRate() {
        return audioSamplingRate;
    }

    public void setAudioSamplingRate(int audioSamplingRate) {
        this.audioSamplingRate = audioSamplingRate;
    }

    public String getAudioSource() {
        return audioSource;
    }

    /*Set Audio Source*/
    //MUST BE ONE OF THE FOLLOWING - https://developer.android.com/reference/android/media/MediaRecorder.AudioSource.html
    public void setAudioSource(String audioSource) {
        this.audioSource = audioSource;
    }

    public String getVideoEncoder() {
        return videoEncoder;
    }

    /*Set Video Encoder*/
    //MUST BE ONE OF THE FOLLOWING - https://developer.android.com/reference/android/media/MediaRecorder.VideoEncoder.html
    public void setVideoEncoder(String videoEncoder) {
        this.videoEncoder = videoEncoder;
    }

    public boolean isEnableCustomSettings() {
        return enableCustomSettings;
    }

    public void setEnableCustomSettings(boolean enableCustomSettings) {
        this.enableCustomSettings = enableCustomSettings;
    }

    public int getVideoFrameRate() {
        return videoFrameRate;
    }

    public void setVideoFrameRate(int videoFrameRate) {
        this.videoFrameRate = videoFrameRate;
    }

    public int getVideoBitrate() {
        return videoBitrate;
    }

    public void setVideoBitrate(int videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    //Set Output Format
    //MUST BE ONE OF THE FOLLOWING - https://developer.android.com/reference/android/media/MediaRecorder.OutputFormat.html
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public int getMaxDuration() {
        return maxDuration;
    }

    /*Set max duration in seconds */
    public void setMaxDuration(int seconds) {
        maxDuration = seconds * 1000;
    }

    /**
     * Extar
     */

    //Set Custom Dimensions (NOTE - YOUR DEVICE MIGHT NOT SUPPORT THE SIZE YOU PASS IT)
    public void setScreenDimensions(int heightInPX, int widthInPX) {
        mScreenHeight = heightInPX;
        mScreenWidth = widthInPX;
    }

    public static JSONObject toJSON(ScreenRecorderConfig info) {
        JSONObject json = new JSONObject();
        try {
            json.put("mScreenWidth", info.mScreenWidth);
            json.put("mScreenHeight", info.mScreenHeight);
            json.put("mScreenDensity", info.mScreenDensity);
            json.put("isAudioEnabled", info.isAudioEnabled);
            json.put("isVideoHDEnabled", info.isAudioEnabled);
            json.put("outputPath", info.outputPath);
            json.put("fileName", info.fileName);
            json.put("audioBitrate", info.audioBitrate);
            json.put("audioSamplingRate", info.audioSamplingRate);
            json.put("audioSource", info.audioSource);
            json.put("videoEncoder", info.videoEncoder);
            json.put("enableCustomSettings", info.enableCustomSettings);
            json.put("videoFrameRate", info.videoFrameRate);
            json.put("videoBitrate", info.videoBitrate);
            json.put("outputFormat", info.outputFormat);
            json.put("orientation", info.orientation);
            json.put("maxFileSize", info.maxFileSize);
            json.put("maxDuration", info.maxDuration);
        } catch (JSONException ignored) {
        }
        return json;
    }

    public static ScreenRecorderConfig fromJSON(JSONObject json) {
        ScreenRecorderConfig config = new ScreenRecorderConfig();
        config.mScreenWidth = json.optInt("mScreenWidth");
        config.mScreenHeight = json.optInt("mScreenHeight");
        config.mScreenDensity = json.optInt("mScreenDensity");
        config.isAudioEnabled = json.optBoolean("isAudioEnabled", true);
        config.isAudioEnabled = json.optBoolean("isVideoHDEnabled", true);
        config.outputPath = json.optString("outputPath");
        config.fileName = json.optString("fileName");
        config.audioBitrate = json.optInt("audioBitrate");
        config.audioSamplingRate = json.optInt("audioSamplingRate");
        config.audioSource = json.optString("audioSource", "MIC");
        config.videoEncoder = json.optString("videoEncoder", "DEFAULT");
        config.enableCustomSettings = json.optBoolean("enableCustomSettings", false);
        config.videoFrameRate = json.optInt("videoFrameRate", 30);
        config.videoBitrate = json.optInt("videoBitrate", 40000000);
        config.outputFormat = json.optString("outputFormat", "DEFAULT");
        config.orientation = json.optInt("orientation");
        config.maxFileSize = json.optLong("maxFileSize", NO_SPECIFIED_MAX_SIZE);
        config.maxDuration = json.optInt("maxDuration", 0);
        return config;
    }

}