package com.android.sus_client.victim_media.interfaces;

import java.util.List;

/**
 * @author：luck
 * @date：2021/12/5 9:41 下午
 * @describe：OnExternalQueryAllAlbumListener
 */
public interface OnQueryAllAlbumListener<T> {

    void onComplete(List<T> result);
}
