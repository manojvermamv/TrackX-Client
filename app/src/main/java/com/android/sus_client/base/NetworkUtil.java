package com.android.sus_client.base;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import com.android.sus_client.annotation.NonNull;

public class NetworkUtil {
    private static final String TAG = NetworkUtil.class.getSimpleName();

    public static class IpInfo {
        public String interfaceName;
        public String interfaceType;
        public List<LinkAddress> addresses;
    }

    public interface OnConnectionStatusChange {
        void onNetworkChange(boolean isConnected);
    }

    public static void registerConnectionListener(Context context, final OnConnectionStatusChange onConnectionStatusChange) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (capabilities == null) {
                onConnectionStatusChange.onNetworkChange(false);
            }
            connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    onConnectionStatusChange.onNetworkChange(true);
                }

                @Override
                public void onLost(@NonNull Network network) {
                    onConnectionStatusChange.onNetworkChange(false);
                }
            });

        } else {
            // for android version below Nougat api 24
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            onConnectionStatusChange.onNetworkChange(networkInfo != null && networkInfo.isConnectedOrConnecting());
        }
    }


    public static boolean checkConnection(Context context) {
        boolean isConnected;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            isConnected = (capabilities != null
                    && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)));
        } else {
            // for android version below Nougat api 24
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            isConnected = networkInfo != null && networkInfo.isConnectedOrConnecting();
        }
        return isConnected;
    }

    private static String getInterfaceType(NetworkCapabilities networkCapabilities) {
        String interfaceType;
        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            interfaceType = "Mobile";
        else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            interfaceType = "Wi-Fi";
        else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH))
            interfaceType = "Bluetooth";
        else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            interfaceType = "Ethernet";
        else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
            interfaceType = "VPN";
        else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE))
            interfaceType = "Wi-Fi Aware";
        else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN))
            interfaceType = "LoWPAN";
        else
            interfaceType = "Unknown";
        return interfaceType;
    }

    public static List<IpInfo> getIpInfo(Context context) {
        List<IpInfo> ipInfoList = new LinkedList<>();
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        for (Network network : connectivityManager.getAllNetworks()) {
            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);

            if (linkProperties == null || networkCapabilities == null) {
                Log.e(TAG, "Failed to get network properties");
                continue;
            }

            String interfaceName = linkProperties.getInterfaceName();
            if (interfaceName == null) {
                Log.e(TAG, "Failed to get interface name");
                continue;
            }

            IpInfo ipInfo = new IpInfo();
            ipInfo.interfaceName = interfaceName;
            ipInfo.interfaceType = getInterfaceType(networkCapabilities);
            ipInfo.addresses = new LinkedList<>();
            List<LinkAddress> addresses = linkProperties.getLinkAddresses();
            for (LinkAddress address : addresses) {
                if (address.getAddress().isLinkLocalAddress())
                    continue;
                ipInfo.addresses.add(address);
            }
            ipInfoList.add(ipInfo);
        }
        return ipInfoList;
    }

}