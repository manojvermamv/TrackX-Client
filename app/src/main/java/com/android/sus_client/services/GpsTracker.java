package com.android.sus_client.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;

import com.android.sus_client.utils.Utils;
import com.android.sus_client.utils.ViewUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Create this Class from tutorial :
 * http://www.androidhive.info/2012/07/android-gps-location-manager-tutorial
 * <p>
 * For Geocoder read this : http://stackoverflow.com/questions/472313/android-reverse-geocoding-getfromlocation
 */

public class GpsTracker implements LocationListener, Runnable {

    /**
     * Also check ---> <a href="https://github.com/mksantoki/LocationTracker/blob/master/app/src/main/java/com/mk/locationtracker/location/LocationManger.java">...</a>
     */
    // Get Class Name
    private static final String TAG = GpsTracker.class.getName();
    public static final int REQUEST_GPS_PERMISSION_CODE = 446;

    private final Context mContext;
    private LocationManager locationManager;
    private Location location;
    private double latitude;
    private double longitude;
    private double speed;
    private float accuracy;

    // Store LocationManager.GPS_PROVIDER or LocationManager.NETWORK_PROVIDER information
    private String providerInfo = LocationManager.NETWORK_PROVIDER;
    private LocationListener locationListener = null;
    private Thread locationThread = null;


    // How many Geocoder should return our GPSTracker
    private static final int geocoderMaxResults = 1;
    // The minimum distance to change updates in meters (Default 1 meters)
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;
    // The minimum time between updates in milliseconds (Default 2 seconds)
    private static final long MIN_TIME_BW_UPDATES = 1000 * 2;

    // flag for Gps, Network and Gps Tracking Status
    public boolean isGpsEnabled = false;
    public boolean isNetworkEnabled = false;
    public boolean isTrackingEnabled = false;


    public interface LocationListener {
        void onChange(JSONObject locationData);
    }

    private GpsTracker(Context context) {
        this.mContext = context;
        this.locationThread = new Thread(this);
        checkPermissions();
    }

    public static GpsTracker getInstance(Context context) {
        return new GpsTracker(context);
    }

    private static List<String> getRequiredPermissions(Context context) {
        String[] requiredPermissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE};
        List<String> permissionsList = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ViewUtils.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                permissionsList.add(permission);
        }
        return permissionsList;
    }

    public void checkPermissions() {
        // check permissions
        if (!getRequiredPermissions(mContext).isEmpty()) {
//            String[] requiredPermissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE};
//            ViewUtils.requestPermissions((Activity) context, permissions.toArray(new String[permissions.size()]), REQUEST_GPS_PERMISSION_CODE);
//            Permissions.request(mContext, requiredPermissions, new PermissionHandler() {
//                @Override
//                public void onGranted(ArrayList<String> grantedPermissions) {
//                    checkPermissions();
//                }
//            });
        } else {
            LocationManager manager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                //PermissionsUtils.turnOnGPS(mContext);
            }
            locationThread.start();
        }
    }


    /**
     * Try to get my current location by GPS or Network Provider
     */
    @Override
    public void run() {
        try {
            Looper.prepare();
            locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

            //getting GPS status
            isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            //getting network status
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            // Try to get location if you GPS Service is enabled
            if (isGpsEnabled) {
                this.isTrackingEnabled = true;
                Utils.getLog(TAG, "Application use GPS Service");

                /*
                 * This provider determines location using
                 * satellites. Depending on conditions, this provider may take a while to return
                 * a location fix.
                 */
                providerInfo = LocationManager.GPS_PROVIDER;

            } else if (isNetworkEnabled) { // Try to get location if you Network Service is enabled
                this.isTrackingEnabled = true;
                Utils.getLog(TAG, "Application use Network State to get GPS coordinates");

                /*
                 * This provider determines location based on
                 * availability of cell tower and WiFi access points. Results are retrieved
                 * by means of a network lookup.
                 */
                providerInfo = LocationManager.NETWORK_PROVIDER;
            }

            // Application can use GPS or Network Provider
            if (!providerInfo.isEmpty()) {
                locationManager.requestLocationUpdates(providerInfo, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                if (locationManager != null) {
                    location = locationManager.getLastKnownLocation(providerInfo);
                    updateGPSCoordinates();
                }
            }
            Looper.loop();
        } catch (Exception e) {
            this.isTrackingEnabled = false;
            Utils.getLog(TAG, "Impossible to connect to LocationManager : " + e);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        updateGPSCoordinates();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    /**
     * Update GPSTracker latitude and longitude
     */
    private void updateGPSCoordinates() {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            accuracy = location.getAccuracy();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                float mSpeed = location.getSpeedAccuracyMetersPerSecond();
                speed = (mSpeed > 0.0) ? mSpeed : location.getSpeed();
            } else {
                speed = location.getSpeed();
            }

            if (locationListener != null) {
                try {
                    JSONObject locationData = getLocationData(this);
                    locationListener.onChange(locationData);
                } catch (JSONException ignored) {
                }
            }
        }
    }

    public void setLocationListener(LocationListener listener) {
        this.locationListener = listener;
        if (locationListener != null) {
            try {
                JSONObject locationData = getLocationData(this);
                locationListener.onChange(locationData);
            } catch (JSONException ignored) {
            }
        }
    }

    public boolean trackingEnabled() {
        return isTrackingEnabled && getLatitude() != 0.0 && getLongitude() != 0.0;
    }

    /**
     * GPSTracker latitude getter and setter
     *
     * @return latitude
     */
    public double getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
        }
        return latitude;
    }

    /**
     * GPSTracker longitude getter and setter
     *
     * @return
     */
    public double getLongitude() {
        if (location != null) {
            longitude = location.getLongitude();
        }
        return longitude;
    }

    /**
     * Stop using GPS listener
     * Calling this method will stop using GPS in your app
     */
    public void stopUsingGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(GpsTracker.this);
        }
        locationListener = null;
    }

    public void stopUsingGPS(int minutes) {
        try {
            minutes = (minutes < 1) ? 1 : (Math.min(minutes, 60));
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    updateGPSCoordinates();
                    stopUsingGPS();
                }
            }, (long) minutes * 60 * 1000);
        } catch (Exception ignored) {
        }
    }

    /**
     * Get list of address by latitude and longitude
     *
     * @return null or List<Address>
     */
    private List<Address> getGeocoderAddress() {
        if (location != null) {
            Geocoder geocoder = new Geocoder(mContext, Locale.ENGLISH);
            try {
                /**
                 * Geocoder.getFromLocation - Returns an array of Addresses
                 * that are known to describe the area immediately surrounding the given latitude and longitude.
                 */
                return geocoder.getFromLocation(latitude, longitude, geocoderMaxResults);
            } catch (IOException e) {
                //e.printStackTrace();
                Utils.getLog(TAG, "Impossible to connect to Geocoder " + e);
            }
        }
        return null;
    }

    /**
     * Try to get AddressLine
     *
     * @return null or addressLine
     */
    public String getAddressLine() {
        List<Address> addresses = getGeocoderAddress();
        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            String addressLine = address.getAddressLine(0);
            return addressLine;
        } else {
            return null;
        }
    }

    /**
     * Try to get Locality
     *
     * @return null or locality
     */
    public String getLocality() {
        List<Address> addresses = getGeocoderAddress();
        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            String locality = address.getLocality();
            return locality;
        } else {
            return null;
        }
    }

    /**
     * Try to get Postal Code
     *
     * @return null or postalCode
     */
    public String getPostalCode() {
        List<Address> addresses = getGeocoderAddress();
        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            return address.getPostalCode();
        } else {
            return null;
        }
    }

    /**
     * Try to get CountryName
     *
     * @return null or postalCode
     */
    public String getCountryName() {
        List<Address> addresses = getGeocoderAddress();
        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            return address.getCountryName();
        } else {
            return null;
        }
    }

    /**
     * Get google map position by latitude and longitude
     *
     * @return String
     */
    public String getGoogleMapLink() {
        return "https://www.google.lk/maps/search/" + getLatitude() + ",+" + getLongitude();
    }

    public static JSONObject getLocationData(GpsTracker gpsTracker) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("gp", gpsTracker.isGpsEnabled);
        json.put("ne", gpsTracker.isNetworkEnabled);
        json.put("tr", gpsTracker.isTrackingEnabled);
        json.put("ac", new DecimalFormat("0.00").format(gpsTracker.accuracy));
        json.put("sp", getSpeedKmph(gpsTracker.speed));
        json.put("la", gpsTracker.getLatitude());
        json.put("lo", gpsTracker.getLongitude());
        json.put("ti", Calendar.getInstance().getTimeInMillis());
        /*json.put("googleMapPosition", gpsTracker.getGoogleMapLink());
        json.put("addressLine", gpsTracker.getAddressLine());
        json.put("locality", gpsTracker.getLocality());
        json.put("postalCode", gpsTracker.getPostalCode());
        json.put("countryName", gpsTracker.getCountryName());*/
        return json;
    }

    private static String getSpeedKmph(double speed) {
        try {
            double speedKmph = ((speed * 3600) / 1000);
            return new DecimalFormat("#0.00").format(speedKmph);
        } catch (Exception e) {
            return "0.00";
        }
    }

}