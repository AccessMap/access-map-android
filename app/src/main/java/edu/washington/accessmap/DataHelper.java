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

    public static String extractAddressText(Address address) {
        String textAddress = "";
        for (int i = 0; i < address.getMaxAddressLineIndex() - 1; i++) {
            textAddress += address.getAddressLine(i) + ", ";
        }
        textAddress += address.getAddressLine(address.getMaxAddressLineIndex() - 1);
        return textAddress;
    }

    public static LatLng extractLatLng(Address address) {
        return new LatLng(address.getLatitude(), address.getLongitude());
    }

}
