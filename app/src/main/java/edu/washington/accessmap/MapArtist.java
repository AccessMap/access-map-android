package edu.washington.accessmap;

import android.graphics.Color;
import android.location.Address;
import android.text.TextUtils;

import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
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
        if (mapTracker.getCurrentRoute() != null) {
            drawRoute(mapboxMap, mapTracker, false);
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

    public static void drawRoute(MapboxMap mapboxMap, MapStateTracker mapTracker, boolean center) {
        ArrayList<LatLng> currentRoute = mapTracker.getCurrentRoute();
        if (currentRoute != null && currentRoute.size() > 0) {
            LatLng[] pointsArray = currentRoute.toArray(new LatLng[currentRoute.size()]);

            // Draw Points on MapView
            mapboxMap.addPolyline(new PolylineOptions()
                    .add(pointsArray)
                    .color(Color.parseColor("#00B3FD"))
                    .width(5));
        }

        Address start = mapTracker.getCurrentRouteStart();
        Address end = mapTracker.getCurrentRouteEnd();

        LatLng startLatLng = DataHelper.extractLatLng(start);
        LatLng endLatLng = DataHelper.extractLatLng(end);

        if (center) {
            CameraPosition position = new CameraPosition.Builder()
                    .target(startLatLng) // Sets the new camera position
                    .zoom(MainActivity.DATA_ZOOM_LEVEL) // Sets the zoom
                    .build(); // Creates a CameraPosition from the builder
            mapboxMap.animateCamera(CameraUpdateFactory
                    .newCameraPosition(position), 1000);
        }

        mapboxMap.addMarker(new MarkerOptions()
                .position(startLatLng)
                .title("Start Address:")
                .snippet(DataHelper.extractAddressText(start)));
        mapboxMap.addMarker(new MarkerOptions()
                .position(endLatLng)
                .title("End Address:")
                .snippet(DataHelper.extractAddressText(end)));
    }

    public static void clearRoute(MapStateTracker mapTracker) {
        mapTracker.setCurrentRoute(null);
        mapTracker.setCurrentRouteEnd(null);
        mapTracker.setCurrentRouteStart(null);
    }

}
