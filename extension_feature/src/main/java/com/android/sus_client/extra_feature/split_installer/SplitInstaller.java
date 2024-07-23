package com.android.sus_client.extra_feature.split_installer;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.android.sus_client.extra_feature.moduler.StorageFeature;
import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;

import java.util.ServiceLoader;
import java.util.logging.Logger;

public class SplitInstaller implements SplitInstallStateUpdatedListener {

    private Application application;
    private SplitInstallManager splitInstallManager;

    private int sessionId = 0;

    @Override
    public void onStateUpdate(SplitInstallSessionState state) {
        if (state.sessionId() == sessionId) {
            switch (state.status()) {
                case SplitInstallSessionStatus.FAILED:
                    Log.d("SplitInstaller", "Module installed failed.");
                    break;
                case SplitInstallSessionStatus.INSTALLED:
                    Log.d("SplitInstaller", "Module installed successfully.");
                    break;
                default:
                    Log.d("SplitInstaller", "Status: " + state.status());
                    break;
            }
        }
    }

    public void onStart(Application application) {
        this.application = application;
        if (splitInstallManager == null) {
            splitInstallManager = SplitInstallManagerFactory.create(application);
        }
        splitInstallManager.registerListener(this);
    }

    public void onStop() {
        if (splitInstallManager != null)
            splitInstallManager.unregisterListener(this);
    }

    public void requestModuleInstall(String module) {
        SplitInstallRequest request = SplitInstallRequest
                .newBuilder()
                .addModule(module)
                .build();

        splitInstallManager.startInstall(request).addOnSuccessListener(id -> {
            sessionId = id;
        }).addOnFailureListener(e -> {
            Log.e("SplitInstaller", "Error installing module: " + e.getMessage());
        });
    }

    public boolean isModuleInstalled(String moduleName) {
        return splitInstallManager.getInstalledModules().contains(moduleName);
    }

    public StorageFeature initializeModuleFeature(String moduleName) {
        if (!isModuleInstalled(moduleName)) {
            requestModuleInstall(moduleName);
            return null;
        }

        // We will need this to pass in dependencies to the StorageFeature.Provider
        StorageFeature.Dependencies dependencies = new StorageFeature.Dependencies() {
            @Override
            public Context getContext() {
                return application.getApplicationContext();
            }

            @Override
            public Logger getLogger() {
                return Logger.getGlobal();
            }
        };

        // Ask ServiceLoader for concrete implementations of StorageFeature.Provider
        // Explicitly use the 2-argument version of load to enable R8 optimization.
        ServiceLoader<StorageFeature.Provider> serviceLoader = ServiceLoader.load(
                StorageFeature.Provider.class,
                StorageFeature.Provider.class.getClassLoader());

        // Explicitly ONLY use the .iterator() method on the returned ServiceLoader to enable R8 optimization.
        // When these two conditions are met, R8 replaces ServiceLoader calls with direct object instantiation.
        StorageFeature storageModule = serviceLoader.iterator().next().get(dependencies);
        Log.d("SplitInstaller", "Loaded storage feature through ServiceLoader");
        return storageModule;
    }

}