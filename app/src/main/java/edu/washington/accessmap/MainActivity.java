package edu.washington.accessmap;

import android.app.Dialog;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.IntentSender;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.Polyline;
import com.mapbox.mapboxsdk.annotations.Annotation;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final int DATA_ZOOM_LEVEL = 15;
    public static final String TAG = MainActivity.class.getSimpleName();

    private static final double HIGH_GRADE = 0.0833;
    private static final double MID_GRADE = 0.05;

    private MapView mapView = null;
    private EditText addressText = null;
    private Button centerUserLocationButton = null;
    private ImageButton zoomOutButton = null;
    private ImageButton zoomInButton = null;
    private GoogleApiClient mGoogleApiClient = null;
    private LocationRequest mLocationRequest = null;
    private Marker userLocationMarker = null;
    private Location userLastLocation = null;
    private Marker lastSearchedAddressMarker = null;
    private Address lastSearchedAddress = null;
    private boolean handleAddressOnResume = false;
    private LatLng lastCenterLocation = null;
    private double lastZoomLevel = 17;
    private boolean isFirstConnection = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buildGoogleApiClient();
        buildLocationRequest();

        /** Create a mapView and give it some properties */
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setStyleUrl(Style.MAPBOX_STREETS);
        lastCenterLocation = new LatLng(47.6, -122.3);
        mapView.setCenterCoordinate(lastCenterLocation);
        mapView.setZoomLevel(17);
        mapView.setCompassGravity(Gravity.BOTTOM);
        mapView.setLogoGravity(Gravity.RIGHT);
        mapView.onCreate(savedInstanceState);

        mapView.addOnMapChangedListener(handleMapChange);

        addressText = (EditText) findViewById(R.id.address_text_bar);
        addressText.setOnClickListener(searchAddress);

        centerUserLocationButton = (Button) findViewById(R.id.center_user_location_button);
        centerUserLocationButton.setOnClickListener(centerOnUserLocationButtonOnClickListener);

        zoomInButton = (ImageButton) findViewById(R.id.zoom_in_button);
        zoomInButton.setOnClickListener(zoomInButtonOnClickListener);

        zoomOutButton = (ImageButton) findViewById(R.id.zoom_out_button);
        zoomOutButton.setOnClickListener(zoomOutButtonOnClickListener);
    }

    View.OnClickListener searchAddress = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            System.out.println("StartingNewActivity!!!");
            Intent searchAddress = new Intent(MainActivity.this, SearchAddressActivity.class);
            MainActivity.this.startActivityForResult(searchAddress, 1);
        }
    };

    MapView.OnMapChangedListener handleMapChange = new MapView.OnMapChangedListener() {
        @Override
        public void onMapChanged(int change) {
            System.out.println("change: " + change);
            double zoom = mapView.getZoomLevel();
            LatLng centerCoordinate = mapView.getCenterCoordinate();

            float computedDistance = computeDistance(centerCoordinate, lastCenterLocation);

            if (lastZoomLevel >= 16 && zoom < 16) {
                lastZoomLevel = zoom;
                clearMap();
            } else if (lastZoomLevel < 16 && zoom >= 16) {
                lastZoomLevel = zoom;
                loadData();
            } else if (computedDistance > 200 && zoom >= 16) {
                lastZoomLevel = zoom;
                lastCenterLocation = centerCoordinate;
                System.out.println("CHANGED REGION");

                clearMap();
                loadData();
            }
        }

    };

    // returns -1 on error otherwise meters between coordinates
    private float computeDistance(LatLng one, LatLng two) {
        float[] distanceArray = new float[1];
        try {
            Location.distanceBetween(one.getLatitude(), one.getLongitude(),
                    two.getLatitude(), two.getLongitude(), distanceArray);
            return distanceArray[0];
        } catch (IllegalArgumentException iae) {
            System.out.println("distanceBetween failed");
            return -1;
        }
    }

    // clears annotations from the map except searched and current location
    private void clearMap() {
        mapView.removeAllAnnotations();

        if (userLocationMarker != null) {
            mapView.addMarker(new MarkerOptions().position(userLocationMarker.getPosition()));
        }
        if (lastSearchedAddressMarker != null) {
            mapView.addMarker(new MarkerOptions().position(lastSearchedAddressMarker.getPosition()));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                Address selectedAddress = data.getExtras().getParcelable("SELECTED_ADDRESS");

                String textAddress = "";
                for (int i = 0; i < selectedAddress.getMaxAddressLineIndex(); i++) {
                    textAddress += selectedAddress.getAddressLine(i) + ", ";
                }
                addressText.setText(textAddress);
                lastSearchedAddress = selectedAddress;
                handleAddressOnResume = true;
                // onResume() is about to be called
            } else if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    } //onActivityResult

    View.OnClickListener zoomInButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            double currentZoomLevel = mapView.getZoomLevel();
            if (currentZoomLevel + 1 < MapView.MAXIMUM_ZOOM_LEVEL) {
                mapView.setZoomLevel(currentZoomLevel + 1, true);  // animated zoom in
            } else {
                mapView.setZoomLevel(MapView.MAXIMUM_ZOOM_LEVEL, true);
            }
            lastZoomLevel = mapView.getZoomLevel();
            if (lastZoomLevel >= 16) {
                loadData();
            }
        }
    };

    View.OnClickListener zoomOutButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            double currentZoomLevel = mapView.getZoomLevel();
            mapView.setZoomLevel(currentZoomLevel - 1, true);  // animated zoom out
            lastZoomLevel = mapView.getZoomLevel();
            if (lastZoomLevel < 16) {
                clearMap();
            }
        }
    };

    View.OnClickListener centerOnUserLocationButtonOnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View arg0) {
            handleNewLocation(userLastLocation, true);
        }
    };

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected synchronized void buildLocationRequest() {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Location services connected.");
        userLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (userLastLocation == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else if (isFirstConnection) {
            isFirstConnection = false;
            handleNewLocation(userLastLocation, true);
            loadData();
        }
    }

    @Override
    public void onLocationChanged(Location mLastLocation) {
        userLastLocation = mLastLocation;
        handleNewLocation(userLastLocation, false);
    }

    private void handleNewLocation(Location mLastLocation, boolean center) {
        Log.i(TAG, "handling new location");
        double currentLatitude = mLastLocation.getLatitude();
        double currentLongitude = mLastLocation.getLongitude();
        LatLng currentPosition = new LatLng(currentLatitude, currentLongitude);
        if (userLocationMarker != null) {
            mapView.removeAnnotation(userLocationMarker);
        }
        // new userLocationMarker
        userLocationMarker = mapView.addMarker(new MarkerOptions()
                .position(currentPosition));
        if (center) {
            mapView.setCenterCoordinate(currentPosition);
            lastCenterLocation = currentPosition;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
            int errorCode = connectionResult.getErrorCode();
            if (errorCode == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ||
                    errorCode == ConnectionResult.SERVICE_MISSING ||
                    errorCode == ConnectionResult.SERVICE_DISABLED) {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, 1);
                dialog.show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause()  {
        super.onPause();
        mapView.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        mGoogleApiClient.connect();

        if (handleAddressOnResume) {  // search address activity just finished
            try {
                if (lastSearchedAddressMarker != null) {
                    mapView.removeAnnotation(lastSearchedAddressMarker);
                }
                LatLng searchedPosition = new LatLng(lastSearchedAddress.getLatitude(), lastSearchedAddress.getLongitude());
                lastSearchedAddressMarker = mapView.addMarker(new MarkerOptions().position(searchedPosition));
                mapView.setCenterCoordinate(searchedPosition);
                System.out.println(mapView.getCenterCoordinate());
                System.out.println(searchedPosition);
                if (mapView.getCenterCoordinate() != searchedPosition) {
                    System.out.println("I DOT KNOW WHATS HAPPENING");
                }
                System.out.println("should have changed the map location");
            } catch (IllegalStateException ise) {
                Toast.makeText(getApplicationContext(),
                        "cannot resolve exact location of searched address",
                        Toast.LENGTH_LONG).show();
            }
            handleAddressOnResume = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    public void loadData() {
        // TODO handle deleting old annotations
        System.out.println("LOADING MORE DATA");
        System.out.println(mapView.getCenterCoordinate());
        String bounds = getDataBounds(mapView.getCenterCoordinate());
        String api_url = getString(R.string.api_url);
        System.out.println("About to load data");
        new CallAccessMapAPI().execute(api_url, "/curbs.geojson", bounds);
        new CallAccessMapAPI().execute(api_url, "/sidewalks.geojson", bounds);
    }

    // always get bounds for zoom level 15 and higher
    // format = botom left long, bottom left lat, top right long, top right lat
    public String getDataBounds(LatLng centerCoordinate) {
        double longitude = centerCoordinate.getLongitude();
        double latitude = centerCoordinate.getLatitude();
        return (longitude - .003) + "," + (latitude - .003) + "," + (longitude + .003) + "," + (latitude + .003);
        // +- .01 at Longitute
        // +- .01 Latitude
        // aproximate zoom level 15 bounds
    }

    private class CallAccessMapAPI extends AsyncTask<String, String, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... params) {
            Log.i(TAG, "DOING IN BACKGROUND");
            String urlString = params[0] + params[1];  // URL to call
            String bounds = params[2];
            JSONObject result = null;

            // HTTP Get
            try {

                String bbox = "-122.357879,47.646978,-122.348556,47.655709";
                System.out.println(bbox);
                System.out.println(bounds);
                String charset = "UTF-8";
                String query = String.format("bbox=%s", bounds); // URLEncoder.encode(bbox, charset));

                URL url = new URL(urlString + "?" + query);
                //URL url = new URL(urlString);
                System.out.println(urlString + "?" + query);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                if (urlConnection == null) {
                    System.out.println("we have a problem");
                }
                BufferedReader in = new BufferedReader( new InputStreamReader(urlConnection.getInputStream()));

                if (in == null) {
                    System.out.println("we have another problem");
                }
                System.out.println("here");

                String inputLine;
                StringBuffer response = new StringBuffer();

                System.out.println("wassup");
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                in.close();

                System.out.println(response.toString());
                Log.i(TAG, response.toString());

                result = new JSONObject(response.toString());
                result.put("class", params[1]);  // track data type

            } catch (Exception e ) {
                System.out.println(e.getMessage());
                return null;
            }

            return result;
        }

        protected void onPostExecute(JSONObject result) {
            if (result == null) {
                System.out.println("NULL!!!!");
                // do nothing
            } else {
                Log.i(TAG, "POST EXECUTE!!!");
                System.out.println("We made it!");
                try {
                    switch (result.getString("class")) {
                        case "/curbs.geojson":
                            drawCurbs(result);
                            break;
                        case "/sidewalks.geojson":
                            drawSidewalks(result);
                            break;
                    }
                } catch (JSONException je) {
                    Log.i(TAG, "JSON ERROR");
                }
            }
        }
    }  // end CallAccessMapAPI

    public void drawSidewalks(JSONObject sidewalkData) {
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

    public void drawCurbs(JSONObject curbData) {
        try {
            JSONArray features = curbData.getJSONArray("features");
            List<MarkerOptions> markers = new ArrayList<MarkerOptions>();
            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                JSONArray coordinates = geometry.getJSONArray("coordinates");
                LatLng latlng = new LatLng(coordinates.getDouble(1), coordinates.getDouble(0));
                markers.add(new MarkerOptions().position(latlng).sprite("circle-11"));  // "circle-11"
            }

            System.out.println("About to draw curbs");
            mapView.addMarkers(markers);
        } catch (JSONException je) {
            Log.i(TAG, "JSON ERROR");
        }
    }

    private LatLng convertToLatLng(JSONArray coordinates) throws JSONException {
        return new LatLng(coordinates.getDouble(1), coordinates.getDouble(0));
    }
}