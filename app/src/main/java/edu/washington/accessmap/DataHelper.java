package edu.washington.accessmap;

import android.location.Address;
import android.os.AsyncTask;
import android.util.Log;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by samuelfelker on 12/5/15.
 */
public class DataHelper {
    public static final String TAG = DataHelper.class.getSimpleName();
    public static final int MAX_ADDRESS_LENGTH = 50;

    public static String extractAddressText(Address address) {
        String textAddress = "";
        if (address.getMaxAddressLineIndex() != -1) {
            textAddress = address.getAddressLine(0);
        }
        for (int i = 1; i <= address.getMaxAddressLineIndex(); i++) {
            textAddress += ", " + address.getAddressLine(i);
        }
        if (textAddress.length() > MAX_ADDRESS_LENGTH) {
            return textAddress.substring(0, MAX_ADDRESS_LENGTH) + "...";
        } else {
            return textAddress;
        }
    }

    public static LatLng extractLatLng(Address address) {
        return new LatLng(address.getLatitude(), address.getLongitude());
    }

    public static String getRouteCoordinates(MapStateTracker mapTracker) {
        String result = "[";
        Address start = mapTracker.getCurrentRouteStart();
        Address end = mapTracker.getCurrentRouteEnd();
        result += start.getLatitude();
        result += "," + start.getLongitude();
        result += "," + end.getLatitude();
        result += "," + end.getLongitude() + "]";
        return result;
    }
}
