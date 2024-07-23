package com.android.sus_client.extra_feature.moduler;


import android.content.Context;

import java.util.logging.Logger;

/**
 * This is the interface that needs
 * to be implemented by our dynamically loaded module.
 * <p>
 * Once we load the module through whichever mechanism we choose,
 * we will always refer to it from the app module using this interface.
 */
public interface StorageFeature {
    void saveCounter(int counter);

    int loadCounter();

    /**
     * StorageFeature can be instantiated in whatever way the implementer chooses,
     * we just want to have a simple method to get() an instance of it.
     */
    interface Provider {
        StorageFeature get(Dependencies dependencies);
    }

    /**
     * Dependencies from the main app module that are required by the StorageFeature.
     */
    interface Dependencies {
        Context getContext();

        Logger getLogger();
    }

}