package edu.washington.accessmap;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;

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

    public static void clearMap(MapboxMap mapboxMap, MapStateTracker mapTracker) {
        System.out.println("Removing all annotations");
        mapboxMap.removeAnnotations();

        if (mapTracker.getUserLocationMarker() != null) {
            mapboxMap.addMarker(new MarkerOptions()
                    .position(mapTracker.getUserLocationMarker().getPosition())
                    .title("Your Location"));
        }
        if (mapTracker.getLastSearchedAddressMarker() != null) {
            mapboxMap.addMarker(new MarkerOptions()
                    .position(mapTracker.getLastSearchedAddressMarker().getPosition())
                    .title("Searched Address:")
                    .snippet(DataHelper.extractAddressText(mapTracker.getLastSearchedAddress())));
        }
//            if (mapTracker.getCurrentRoute() != null) {
//                new DrawRouteTask(mapboxMap, mapTracker, false);
//            }
    }

    // clears annotations from the map except searched and current location
    static public class ClearMapTask implements Runnable{

        private final MapboxMap mapboxMap;
        private final MapStateTracker mapTracker;

        public ClearMapTask(MapboxMap mapboxMap, MapStateTracker mapTracker) {
            this.mapboxMap = mapboxMap;
            this.mapTracker = mapTracker;
        }

        @Override
        public void run() {
            System.out.println("Removing all annotations");
            mapboxMap.removeAnnotations();

            if (mapTracker.getUserLocationMarker() != null) {
                mapboxMap.addMarker(new MarkerOptions()
                        .position(mapTracker.getUserLocationMarker().getPosition())
                        .title("Your Location"));
            }
            if (mapTracker.getLastSearchedAddressMarker() != null) {
                mapboxMap.addMarker(new MarkerOptions()
                        .position(mapTracker.getLastSearchedAddressMarker().getPosition())
                        .title("Searched Address:")
                        .snippet(DataHelper.extractAddressText(mapTracker.getLastSearchedAddress())));
            }
//            if (mapTracker.getCurrentRoute() != null) {
//                new DrawRouteTask(mapboxMap, mapTracker, false);
//            }
        }
    }


    // format = bottom left long, bottom left lat, top right long, top right lat

    // a list of bounds for each tile
    public static List<String> getDataBounds(LatLng centerCoordinate) {
        double longitude = centerCoordinate.getLongitude();
        double latitude = centerCoordinate.getLatitude();
        List<String> bounds = new ArrayList<>();
        double largeBoundSideLength = 0.015;
        double minLat = latitude - largeBoundSideLength / 2;
        double minLng = longitude - largeBoundSideLength / 2;
        int numTilesEachSide = 1;
        double tileSideLength = largeBoundSideLength / numTilesEachSide;
        for (int x = 0; x < numTilesEachSide; x++) {
            for (int y = 0; y < numTilesEachSide; y++) {
                double lowerLng = minLng + x * tileSideLength;
                double lowerLat = minLat + y * tileSideLength;
                double higherLng = lowerLng + tileSideLength;
                double higherLat = lowerLat + tileSideLength;
                bounds.add(lowerLng + "," + lowerLat + "," + higherLng + "," + higherLat);
            }
        }
        return bounds;
    }

    public static void drawSideWalk(MapboxMap mapboxMap, JSONObject sidewalkData) {
        try {
            JSONArray features = sidewalkData.getJSONArray("features");
            System.out.println("About to draw sidewalks");
            List<PolylineOptions> polylines = new ArrayList<>();
            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                JSONArray coordinates = geometry.getJSONArray("coordinates");
                LatLng latlngStart = convertToLatLng(coordinates.getJSONArray(0));
                LatLng latlngEnd = convertToLatLng(coordinates.getJSONArray(1));
                double grade = feature.getJSONObject("properties").getDouble("grade");
                String colorStr;
                if (grade > HIGH_GRADE) {
                    colorStr = "#F65A56";
                } else if (grade > MID_GRADE) {
                    colorStr = "#F6F356";
                } else {
                    colorStr = "#5DF356";
                }
                polylines.add(new PolylineOptions().add(latlngStart, latlngEnd).width(3)
                        .alpha((float) 0.8).color(Color.parseColor(colorStr)));
                if (polylines.size() == 500) {
                    mapboxMap.addPolylines(polylines);
                    polylines.clear();
                }
            }
            if (polylines.size() > 0) {
                mapboxMap.addPolylines(polylines);
            }
        } catch (JSONException je) {
            Log.i(TAG, "JSON ERROR");
        }
    }

    static public class DrawSidewalksTask implements Runnable {
        private final MapboxMap mapboxMap;
        private final JSONObject sidewalkData;

        DrawSidewalksTask(MapboxMap mapboxMap, JSONObject sidewalkData) {
            this.mapboxMap = mapboxMap;
            this.sidewalkData = sidewalkData;
        }

        @Override
        public void run() {
            try {
                JSONArray features = sidewalkData.getJSONArray("features");
                System.out.println("About to draw sidewalks");
                List<PolylineOptions> polylines = new ArrayList<>();
                for (int i = 0; i < features.length(); i++) {
                    JSONObject feature = features.getJSONObject(i);
                    JSONObject geometry = feature.getJSONObject("geometry");
                    JSONArray coordinates = geometry.getJSONArray("coordinates");
                    LatLng latlngStart = convertToLatLng(coordinates.getJSONArray(0));
                    LatLng latlngEnd = convertToLatLng(coordinates.getJSONArray(1));
                    double grade = feature.getJSONObject("properties").getDouble("grade");
                    String colorStr;
                    if (grade > HIGH_GRADE) {
                        colorStr = "#012900";
                    } else if (grade > MID_GRADE) {
                        colorStr = "#029400";
                    } else {
                        colorStr = "#06f902";
                    }
                    polylines.add(new PolylineOptions().add(latlngStart, latlngEnd).width(2).color(Color.parseColor(colorStr)));
                    if (polylines.size() == 500) {
                        mapboxMap.addPolylines(polylines);
                        polylines.clear();
                    }
                }
                if (polylines.size() > 0) {
                    mapboxMap.addPolylines(polylines);
                }
            } catch (JSONException je) {
                Log.i(TAG, "JSON ERROR");
            }
        }
    }

    public static void drawCurbs(MapboxMap mapboxMap, JSONObject curbData) {
//        try {
//            JSONArray features = curbData.getJSONArray("features");
//            List<MarkerOptions> markers = new ArrayList<MarkerOptions>();
//
//            for (int i = 0; i < features.length(); i++) {
//                JSONObject feature = features.getJSONObject(i);
//                JSONObject geometry = feature.getJSONObject("geometry");
//                JSONArray coordinates = geometry.getJSONArray("coordinates");
//                LatLng latlng = new LatLng(coordinates.getDouble(1), coordinates.getDouble(0));
//                // TODO: icon
//                markers.add(new MarkerOptions().position(latlng).title("Curb Ramp"));  // "circle-11"
//            }
//
//            System.out.println("About to draw curbs");
//            mapboxMap.addMarkers(markers);
//        } catch (JSONException je) {
//            Log.i(TAG, "JSON ERROR");
//        }
    }

    public static void drawPermits(MapboxMap mapboxMap, JSONObject permitData) {
//        try {
//            JSONArray features = permitData.getJSONArray("features");
//            List<MarkerOptions> markers = new ArrayList<MarkerOptions>();
//            for (int i = 0; i < features.length(); i++) {
//                JSONObject feature = features.getJSONObject(i);
//                JSONObject geometry = feature.getJSONObject("geometry");
//                JSONArray coordinates = geometry.getJSONArray("coordinates");
//                JSONObject properties = feature.getJSONObject("properties");
//                LatLng latlng = new LatLng(coordinates.getDouble(1), coordinates.getDouble(0));
//                markers.add(new MarkerOptions()
//                        .position(latlng)
////                        .sprite("zoo-15")
//                        .title("Construction Permit")
//                        .snippet("Permit no. " + properties.getInt("permit_no") + "\n"
//                                + "Mobility Impact: " + properties.getString("mobility_impact_text")));
//            }
//
//            System.out.println("About to draw permits");
//            mapboxMap.addMarkers(markers);
//        } catch (JSONException je) {
//            Log.i(TAG, "JSON ERROR");
//        }
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

//    class DrawRouteTask implements Runnable {
//        private final MapboxMap mapboxMap;
//        private final MapStateTracker mapTracker;
//        private final boolean center;
//
//        drawRouteTask(MapboxMap mapboxMap, MapStateTracker mapTracker, boolean center) {
//            this.mapboxMap = mapboxMap;
//            this.mapTracker = mapTracker;
//            this.center = center;
//        }
//
//        @Override
//        public void run() {
//            ArrayList<LatLng> currentRoute = mapTracker.getCurrentRoute();
//            if (currentRoute != null && currentRoute.size() > 0) {
//                LatLng[] pointsArray = currentRoute.toArray(new LatLng[currentRoute.size()]);
//
//                // Draw Points on MapView
//                mapboxMap.addPolyline(new PolylineOptions()
//                        .add(pointsArray)
//                        .color(Color.parseColor("#0000ff"))
//                        .width(4));
//            }
//
//            Address start = mapTracker.getCurrentRouteStart();
//            Address end = mapTracker.getCurrentRouteEnd();
//
//            LatLng startLatLng = DataHelper.extractLatLng(start);
//            LatLng endLatLng = DataHelper.extractLatLng(end);
//
//            if (center) {
//                CameraPosition position = new CameraPosition.Builder()
//                        .target(startLatLng) // Sets the new camera position
//                        .zoom(MainActivity.DATA_ZOOM_LEVEL) // Sets the zoom
//                        .build(); // Creates a CameraPosition from the builder
//                mapboxMap.animateCamera(CameraUpdateFactory
//                        .newCameraPosition(position), 7000);
//            }
//
//            mapboxMap.addMarker(new MarkerOptions()
//                    .position(startLatLng)
//                    .title("Start Address:")
//                    .snippet(DataHelper.extractAddressText(start)));
//            mapboxMap.addMarker(new MarkerOptions()
//                    .position(endLatLng)
//                    .title("End Address:")
//                    .snippet(DataHelper.extractAddressText(end);
//        }
//
//    }

    public static void clearRoute(MapStateTracker mapTracker) {
        mapTracker.setCurrentRoute(null);
        mapTracker.setCurrentRouteEnd(null);
        mapTracker.setCurrentRouteStart(null);
    }

    private static LatLng convertToLatLng(JSONArray coordinates) throws JSONException {
        return new LatLng(coordinates.getDouble(1), coordinates.getDouble(0));
    }
}
