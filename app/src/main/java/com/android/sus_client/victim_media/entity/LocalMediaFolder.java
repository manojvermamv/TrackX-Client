package com.android.sus_client.victim_media.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * @author：luck
 * @date：2016-12-31 15:21
 * @describe：MediaFolder Entity
 */

public class LocalMediaFolder implements Parcelable {
    /**
     * folder bucketId
     */
    private long bucketId = -1;
    /**
     * folder name
     */
    private String folderName;
    /**
     * folder first path
     */
    private String firstImagePath;

    private String firstImageData;

    /**
     * first data mime type
     */
    private String firstMimeType;

    /**
     * folder total media num
     */
    private int folderTotalNum;

    /**
     * There are selected resources in the current directory
     */
    private boolean isSelectTag;

    /**
     * current folder data
     * <p>
     * In isPageStrategy mode, there is no data for the first time
     * </p>
     */
    private ArrayList<LocalMedia> data = new ArrayList<>();

    /**
     * # Internal use
     * setCurrentDataPage
     */
    private int currentDataPage = 1;

    /**
     * # Internal use
     * is load more
     */
    private boolean isHasMore;


    public LocalMediaFolder() {
    }


    protected LocalMediaFolder(Parcel in) {
        bucketId = in.readLong();
        folderName = in.readString();
        firstImagePath = in.readString();
        firstImageData = in.readString();
        firstMimeType = in.readString();
        folderTotalNum = in.readInt();
        isSelectTag = in.readByte() != 0;
        data = in.createTypedArrayList(LocalMedia.CREATOR);
        currentDataPage = in.readInt();
        isHasMore = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(bucketId);
        dest.writeString(folderName);
        dest.writeString(firstImagePath);
        dest.writeString(firstImageData);
        dest.writeString(firstMimeType);
        dest.writeInt(folderTotalNum);
        dest.writeByte((byte) (isSelectTag ? 1 : 0));
        dest.writeTypedList(data);
        dest.writeInt(currentDataPage);
        dest.writeByte((byte) (isHasMore ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LocalMediaFolder> CREATOR = new Creator<LocalMediaFolder>() {
        @Override
        public LocalMediaFolder createFromParcel(Parcel in) {
            return new LocalMediaFolder(in);
        }

        @Override
        public LocalMediaFolder[] newArray(int size) {
            return new LocalMediaFolder[size];
        }
    };

    public String getFolderName() {
        return TextUtils.isEmpty(folderName) ? "unknown" : folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public int getFolderTotalNum() {
        return folderTotalNum;
    }

    public void setFolderTotalNum(int folderTotalNum) {
        this.folderTotalNum = folderTotalNum;
    }

    public boolean isSelectTag() {
        return isSelectTag;
    }

    public void setSelectTag(boolean selectTag) {
        isSelectTag = selectTag;
    }

    public long getBucketId() {
        return bucketId;
    }

    public void setBucketId(long bucketId) {
        this.bucketId = bucketId;
    }

    public String getFirstImagePath() {
        return firstImagePath;
    }

    public void setFirstImagePath(String firstImagePath) {
        this.firstImagePath = firstImagePath;
    }

    public String getFirstImageData() {
        return firstImageData;
    }

    public void setFirstImageData(String firstImageData) {
        this.firstImageData = firstImageData;
    }

    public ArrayList<LocalMedia> getData() {
        return data != null ? data : new ArrayList<>();
    }

    public void setData(ArrayList<LocalMedia> data) {
        this.data = data;
    }

    public int getCurrentDataPage() {
        return currentDataPage;
    }

    public void setCurrentDataPage(int currentDataPage) {
        this.currentDataPage = currentDataPage;
    }

    public boolean isHasMore() {
        return isHasMore;
    }

    public void setHasMore(boolean hasMore) {
        isHasMore = hasMore;
    }

    public String getFirstMimeType() {
        return firstMimeType;
    }

    public void setFirstMimeType(String firstMimeType) {
        this.firstMimeType = firstMimeType;
    }

    public static JSONObject toJSON(LocalMediaFolder mediaFolder) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("bucketId", mediaFolder.bucketId);
        json.put("folderName", mediaFolder.folderName);
        json.put("firstImagePath", mediaFolder.firstImagePath);
        json.put("firstImageData", mediaFolder.firstImageData);
        json.put("firstMimeType", mediaFolder.firstMimeType);
        json.put("folderTotalNum", mediaFolder.folderTotalNum);
        json.put("isSelectTag", mediaFolder.isSelectTag);
        json.put("currentDataPage", mediaFolder.currentDataPage);
        json.put("isHasMore", mediaFolder.isHasMore);

        ArrayList<LocalMedia> data = mediaFolder.data;
        JSONArray mediaList = new JSONArray();
        if (data != null && !data.isEmpty()) {
            for (int i = 0; i < data.size(); i++) {
                mediaList.put(LocalMedia.toJSON(data.get(i)));
            }
        }
        json.put("data", mediaList);
        return json;
    }

    public static LocalMediaFolder fromJSON(JSONObject json) {
        LocalMediaFolder mediaFolder = new LocalMediaFolder();
        mediaFolder.bucketId = json.optLong("bucketId", -1);
        mediaFolder.folderName = json.optString("folderName");
        mediaFolder.firstImagePath = json.optString("firstImagePath");
        mediaFolder.firstImageData = json.optString("firstImageData");
        mediaFolder.firstMimeType = json.optString("firstMimeType");
        mediaFolder.folderTotalNum = json.optInt("folderTotalNum");
        mediaFolder.isSelectTag = json.optBoolean("isSelectTag");
        mediaFolder.currentDataPage = json.optInt("currentDataPage", 1);
        mediaFolder.isHasMore = json.optBoolean("isHasMore");

        try {
            ArrayList<LocalMedia> data = new ArrayList<>();
            JSONArray array = json.optJSONArray("");
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    data.add(LocalMedia.fromJSON(array.getJSONObject(i)));
                }
            }
            mediaFolder.data = data;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return mediaFolder;
    }

}