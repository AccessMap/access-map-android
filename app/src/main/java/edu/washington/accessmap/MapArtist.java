package edu.washington.accessmap;

import android.graphics.Color;
import android.location.Address;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by samuelfelker on 12/5/15.
 */

public class MapArtist {
    private static final double HIGH_GRADE = 0.0833;
    private static final double MID_GRADE = 0.05;
    public static final String TAG = MapArtist.class.getSimpleName();

    // clears annotations from the map except searched and current location
    public static void clearMap(MapView mapView, MapStateTracker mapTracker) {
        mapView.removeAllAnnotations();

        if (mapTracker.getUserLocationMarker() != null) {
            mapView.addMarker(new MarkerOptions()
                    .position(mapTracker.getUserLocationMarker().getPosition())
                    .title("Your Location"));
        }
        if (mapTracker.getLastSearchedAddressMarker() != null) {
            mapView.addMarker(new MarkerOptions()
                    .position(mapTracker.getLastSearchedAddressMarker().getPosition())
                    .title("Searched Address:")
                    .snippet(DataHelper.extractAddressText(mapTracker.getLastSearchedAddress())));
        }
        if (mapTracker.getCurrentRoute() != null) {
            MapArtist.drawRoute(mapView, mapTracker, false);
        }
    }


    // format = botom left long, bottom left lat, top right long, top right lat
    public static String getDataBounds(LatLng centerCoordinate) {
        double longitude = centerCoordinate.getLongitude();
        double latitude = centerCoordinate.getLatitude();
        return (longitude - .003) + "," + (latitude - .003) + "," + (longitude + .003) + "," + (latitude + .003);
    }

    public static void drawSidewalks(MapView mapView, JSONObject sidewalkData) {
        try {
            JSONArray features = sidewalkData.getJSONArray("features");
            List<PolylineOptions> polylines = new ArrayList<PolylineOptions>();
            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                JSONArray coordinates = geometry.getJSONArray("coordinates");
                LatLng latlngStart = convertToLatLng(coordinates.getJSONArray(0));
                LatLng latlngEnd = convertToLatLng(coordinates.getJSONArray(1));
                double grade = feature.getJSONObject("properties").getDouble("grade");
                if (grade > HIGH_GRADE) {
                    polylines.add(new PolylineOptions().add(latlngStart, latlngEnd).width(2).color(Color.parseColor("#012900")));
                } else if (grade > MID_GRADE) {
                    polylines.add(new PolylineOptions().add(latlngStart, latlngEnd).width(2).color(Color.parseColor("#029400")));
                } else {  // LOW_GRADE
                    polylines.add(new PolylineOptions().add(latlngStart, latlngEnd).width(2).color(Color.parseColor("#06f902")));
                }
            }

            System.out.println("About to draw sidewalks");
            mapView.addPolylines(polylines);
        } catch (JSONException je) {
            Log.i(TAG, "JSON ERROR");
        }
    }

    public static void drawCurbs(MapView mapView, JSONObject curbData) {
        try {
            JSONArray features = curbData.getJSONArray("features");
            List<MarkerOptions> markers = new ArrayList<MarkerOptions>();
            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                JSONArray coordinates = geometry.getJSONArray("coordinates");
                LatLng latlng = new LatLng(coordinates.getDouble(1), coordinates.getDouble(0));
                markers.add(new MarkerOptions().position(latlng).sprite("circle-11").title("Curb Ramp"));  // "circle-11"
            }

            System.out.println("About to draw curbs");
            mapView.addMarkers(markers);
        } catch (JSONException je) {
            Log.i(TAG, "JSON ERROR");
        }
    }

    public static void drawPermits(MapView mapView, JSONObject permitData) {
        try {
            JSONArray features = permitData.getJSONArray("features");
            List<MarkerOptions> markers = new ArrayList<MarkerOptions>();
            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                JSONArray coordinates = geometry.getJSONArray("coordinates");
                JSONObject properties = feature.getJSONObject("properties");
                LatLng latlng = new LatLng(coordinates.getDouble(1), coordinates.getDouble(0));
                markers.add(new MarkerOptions()
                        .position(latlng)
                        .sprite("zoo-15")
                        .title("Construction Permit")
                        .snippet("Permit no. " + properties.getInt("permit_no") + "\n"
                                + "Mobility Impact: " + properties.getString("mobility_impact_text")));
            }

            System.out.println("About to draw permits");
            mapView.addMarkers(markers);
        } catch (JSONException je) {
            Log.i(TAG, "JSON ERROR");
        }
    }

    public static ArrayList<LatLng> extractRoute(JSONObject routeData) {
        ArrayList<LatLng> points = new ArrayList<LatLng>();

        // Parse JSON
        try {
            JSONArray features = routeData.getJSONArray("routes");
            JSONObject feature = features.getJSONObject(0);
            JSONObject geometry = feature.getJSONObject("geometry");
            if (geometry != null) {
                String type = geometry.getString("type");

                // Our GeoJSON only has one feature: a line string
                if (!TextUtils.isEmpty(type) && type.equalsIgnoreCase("LineString")) {

                    // Get the Coordinates
                    JSONArray coords = geometry.getJSONArray("coordinates");
                    for (int lc = 0; lc < coords.length(); lc++) {
                        JSONArray coord = coords.getJSONArray(lc);
                        LatLng latLng = new LatLng(coord.getDouble(1), coord.getDouble(0));
                        points.add(latLng);
                    }
                }
            }
        } catch (JSONException je) {
            System.out.println("error in JSON extraction");
            return null;
        }

        return points;
    }

    public static void drawRoute(MapView mapView, MapStateTracker mapTracker, boolean center) {
        ArrayList<LatLng> currentRoute = mapTracker.getCurrentRoute();
        if (currentRoute != null && currentRoute.size() > 0) {
            LatLng[] pointsArray = currentRoute.toArray(new LatLng[currentRoute.size()]);

            // Draw Points on MapView
            mapView.addPolyline(new PolylineOptions()
                    .add(pointsArray)
                    .color(Color.parseColor("#0000ff"))
                    .width(4));
        }

        Address start = mapTracker.getCurrentRouteStart();
        Address end = mapTracker.getCurrentRouteEnd();

        LatLng startLatLng = DataHelper.extractLatLng(start);
        LatLng endLatLng = DataHelper.extractLatLng(end);

        if (center) {
            mapView.setCenterCoordinate(startLatLng);
            mapView.setZoomLevel(MainActivity.DATA_ZOOM_LEVEL);
        }

        mapView.addMarker(new MarkerOptions()
                .position(startLatLng)
                .title("Start Address:")
                .snippet(DataHelper.extractAddressText(start)));
        mapView.addMarker(new MarkerOptions()
                .position(endLatLng)
                .title("End Address:")
                .snippet(DataHelper.extractAddressText(end)));
    }

    public static void clearRoute(MapView mapView, MapStateTracker mapTracker) {
        mapTracker.setCurrentRoute(null);
        mapTracker.setCurrentRouteEnd(null);
        mapTracker.setCurrentRouteStart(null);
    }

    private static LatLng convertToLatLng(JSONArray coordinates) throws JSONException {
        return new LatLng(coordinates.getDouble(1), coordinates.getDouble(0));
    }
}
