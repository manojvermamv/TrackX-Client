//package com.android.sus_client.storage_feature;
//
//import android.content.Context;
//
//import com.android.sus_client.extension_feature.moduler.StorageFeature;
//
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
//public class StorageFeatureImpl implements StorageFeature {
//
//    private final Logger logger;
//
//    StorageFeatureImpl(Context context, Logger logger) {
//        this.logger = logger;
//    }
//
//    @Override
//    public void saveCounter(int counter) {
//        logger.log(Level.WARNING, "StorageFeatureImpl: Saved to storage " + counter);
//    }
//
//    @Override
//    public int loadCounter() {
//        int counter = 2323;
//        logger.log(Level.WARNING, "StorageFeatureImpl: Loaded from storage " + counter);
//        return counter;
//    }
//
//}