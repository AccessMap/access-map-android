package edu.washington.accessmap;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Telephony;
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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
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
    public static final int DATA_ZOOM_LEVEL = 16;
    public static final float CHANGE_DATA_DISTANCE = 200;
    public static final String TAG = MainActivity.class.getSimpleName();

    // map variables
    public MapView mapView = null;
    private MapStateTracker mapTracker = null;
    private static MapFeature[] mapFeatureState = null;

    // ui elements
    private EditText addressText = null;
    private FloatingActionButton closeSearchDisplayButton = null;
    private ImageButton centerUserLocationButton = null;
    private ImageButton zoomOutButton = null;
    private ImageButton zoomInButton = null;
    private ImageButton adjustFeaturesButton = null;
    private ImageButton routeButton = null;
    private ListView routeListView = null;
    private FloatingActionButton closeRouteView = null;

    // google services variables
    private GoogleApiClient mGoogleApiClient = null;
    private LocationRequest mLocationRequest = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buildGoogleApiClient();
        buildLocationRequest();

        /** Instanciate MapView and properties */
        mapView = (MapView) findViewById(R.id.mapview);
        setUpMapProperties(savedInstanceState);
        mapTracker = new MapStateTracker();

        // Instanciate Map Feature Tracker
        Resources res = getResources();
        String[] featureResources = res.getStringArray(R.array.feature_array);
        mapFeatureState = new MapFeature[featureResources.length];
        instanciateFeatureTracker(featureResources);

        // User Interface Listeners
        buildUserInterfaceListeners();

        // Alert User if no Network Connection
        checkWifi();
    }

    public void setUpMapProperties(Bundle savedInstanceState) {
        mapView.setStyleUrl(Style.MAPBOX_STREETS);
        mapTracker.setLastCenterLocation(new LatLng(47.6, -122.3));
        mapView.setCenterCoordinate(mapTracker.getLastCenterLocation());
        mapView.setZoomLevel(DATA_ZOOM_LEVEL);
        mapView.setCompassGravity(Gravity.BOTTOM);
        mapView.setLogoGravity(Gravity.RIGHT);
        mapView.onCreate(savedInstanceState);
    }

    // Pulls feature information from res
    public void instanciateFeatureTracker(String[] featureResources) {
        for (int i = 0; i < featureResources.length; i++) {
            System.out.println(featureResources[i]);
            String[] results = featureResources[i].split("\\|");
            System.out.println(results[0] + " " + results[1] + " " + results[2]);
            mapFeatureState[i] = new MapFeature(results[0], results[1], Boolean.valueOf(results[2]));
        }
    }

    public void buildUserInterfaceListeners() {
        mapView.addOnMapChangedListener(handleMapChange);

        addressText = (EditText) findViewById(R.id.address_text_bar);
        addressText.setOnClickListener(searchAddress);

        closeSearchDisplayButton = (FloatingActionButton) findViewById(R.id.close_search_display);
        closeSearchDisplayButton.setOnClickListener(closeSearchDisplayButtonOnCLickListener);

        centerUserLocationButton = (ImageButton) findViewById(R.id.center_user_location_button);
        centerUserLocationButton.setOnClickListener(centerOnUserLocationButtonOnClickListener);

        zoomInButton = (ImageButton) findViewById(R.id.zoom_in_button);
        zoomInButton.setOnClickListener(zoomInButtonOnClickListener);

        zoomOutButton = (ImageButton) findViewById(R.id.zoom_out_button);
        zoomOutButton.setOnClickListener(zoomOutButtonOnClickListener);

        adjustFeaturesButton = (ImageButton) findViewById(R.id.adjust_features_button);
        adjustFeaturesButton.setOnClickListener(adjustFeaturesButtonOnClickListener);

        routeButton = (ImageButton) findViewById(R.id.route_button);
        routeButton.setOnClickListener(routeButtonOnClickListener);

        routeListView = (ListView) findViewById(R.id.list_view);
        routeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent routeSearch = new Intent(MainActivity.this, Routing.class);
                routeSearch.putExtra("FROM_ADDRESS", mapTracker.getCurrentRouteStart());
                routeSearch.putExtra("USER_LOCATION", false);
                routeSearch.putExtra("TO_ADDRESS", mapTracker.getCurrentRouteEnd());
                MainActivity.this.startActivityForResult(routeSearch, 2);
            }
        });

        closeRouteView = (FloatingActionButton) findViewById(R.id.close_routing_display);
        closeRouteView.setOnClickListener(closeRouteViewOnCLickListener);
    }

    View.OnClickListener searchAddress = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            Intent searchAddress = new Intent(MainActivity.this, SearchAddressActivity.class);
            searchAddress.putExtra("PREVIOUS_SEARCH", mapTracker.getLastSearchedAddress());
            MainActivity.this.startActivityForResult(searchAddress, 1);
        }
    };

    MapView.OnMapChangedListener handleMapChange = new MapView.OnMapChangedListener() {
        @Override
        public void onMapChanged(int change) {
            double zoom = mapView.getZoomLevel();
            LatLng centerCoordinate = mapView.getCenterCoordinate();
            float computedDistance = computeDistance(centerCoordinate, mapTracker.getLastCenterLocation());

            if (mapTracker.getLastZoomLevel() >= DATA_ZOOM_LEVEL && zoom < DATA_ZOOM_LEVEL) {
                mapTracker.setLastZoomLevel(zoom);
                MapArtist.clearMap(mapView, mapTracker);
            } else if (mapTracker.getLastZoomLevel() < DATA_ZOOM_LEVEL && zoom >= DATA_ZOOM_LEVEL) {
                mapTracker.setLastZoomLevel(zoom);
                loadData();
            } else if (computedDistance > CHANGE_DATA_DISTANCE && zoom >= DATA_ZOOM_LEVEL) {
                mapTracker.setLastZoomLevel(zoom);
                mapTracker.setLastCenterLocation(centerCoordinate);
                System.out.println("CHANGED REGION");
                refreshMap();
            }
        }
    };

    View.OnClickListener zoomInButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            double currentZoomLevel = mapView.getZoomLevel();
            if (currentZoomLevel + 1 < MapView.MAXIMUM_ZOOM_LEVEL) {
                mapView.setZoomLevel(currentZoomLevel + 1, true);  // animated zoom in
            } else {
                mapView.setZoomLevel(MapView.MAXIMUM_ZOOM_LEVEL, true);
            }
            mapTracker.setLastZoomLevel(currentZoomLevel);
            if (currentZoomLevel >= DATA_ZOOM_LEVEL) {
                loadData();
            }
        }
    };

    View.OnClickListener zoomOutButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            double currentZoomLevel = mapView.getZoomLevel();
            mapView.setZoomLevel(currentZoomLevel - 1, true);  // animated zoom out
            mapTracker.setLastZoomLevel(currentZoomLevel);
            if (currentZoomLevel < DATA_ZOOM_LEVEL) {
                MapArtist.clearMap(mapView, mapTracker);
            }
        }
    };

    View.OnClickListener adjustFeaturesButtonOnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View arg0) {
            DialogFragment featureAdjuster = new FeatureAdjuster() {
                @Override
                public void onDismiss(final DialogInterface dialog) {
                    //Fragment dialog had been dismissed
                    refreshMap();  // makes users preferences load when dialog closses
                }
            };
            Bundle args = new Bundle();
            args.putParcelableArray("MAP_FEATURES_STATE", mapFeatureState);
            featureAdjuster.setArguments(args);
            featureAdjuster.show(getFragmentManager(), "adjust_features");
        }
    };

    View.OnClickListener routeButtonOnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View arg0) {
            // temporary bypassing of interface and api call
            closeSearchDisplay();  // could be active here if routing after a search
            Intent routeSearch = new Intent(MainActivity.this, Routing.class);
            routeSearch.putExtra("USER_LOCATION", true);  // no from address needed if true
            routeSearch.putExtra("TO_ADDRESS", mapTracker.getLastSearchedAddress());
            MainActivity.this.startActivityForResult(routeSearch, 2);
        }
    };

    View.OnClickListener closeRouteViewOnCLickListener = new View.OnClickListener(){
        @Override
        public void onClick(View arg0) {
            closeRouteView();
        }
    };

    View.OnClickListener closeSearchDisplayButtonOnCLickListener = new View.OnClickListener(){
        @Override
        public void onClick(View arg0) {
            closeSearchDisplay();
        }
    };

    public static void setMapFeatures(MapFeature[] newFeatureSelection) {
        mapFeatureState = newFeatureSelection;
    }

    View.OnClickListener centerOnUserLocationButtonOnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View arg0) {
            handleNewLocation(mapTracker.getUserLastLocation(), true);
        }
    };


    // GOOGLE API LOCATION SERVICE METHODS

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
        mapTracker.setUserLastLocation(LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient));
        if (mapTracker.getUserLastLocation() == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else if (mapTracker.isFirstConnection()) {
            mapTracker.setIsFirstConnection(false);
            handleNewLocation(mapTracker.getUserLastLocation(), true);
            loadData();
        }
    }

    @Override
    public void onLocationChanged(Location mLastLocation) {
        mapTracker.setUserLastLocation(mLastLocation);
        handleNewLocation(mapTracker.getUserLastLocation(), false);
    }

    // new user location detected
    private void handleNewLocation(Location mLastLocation, boolean center) {
        Log.i(TAG, "handling new location");
        double currentLatitude = mLastLocation.getLatitude();
        double currentLongitude = mLastLocation.getLongitude();
        LatLng currentPosition = new LatLng(currentLatitude, currentLongitude);
        if (mapTracker.getUserLocationMarker() != null) {
            System.out.println("removing annotation");
            mapView.removeAnnotation(mapTracker.getUserLocationMarker());
        }
        // new userLocationMarker
        mapTracker.setUserLocationMarker(mapView.addMarker(new MarkerOptions()
                .position(currentPosition)));
        if (center) {
            mapView.setCenterCoordinate(currentPosition);
            mapTracker.setLastCenterLocation(currentPosition);
            if (mapView.getZoomLevel() >= DATA_ZOOM_LEVEL) {
                refreshMap();
            }
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

    // ANDROID ACTIVTY FUNCTIONS

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

        if (mapTracker.isHandleAddressOnResume()) {  // search address activity just finished
            try {
                if (mapTracker.getLastSearchedAddressMarker() != null) {
                    mapView.removeAnnotation(mapTracker.getLastSearchedAddressMarker());
                }
                Address lastSearchedAddress = mapTracker.getLastSearchedAddress();
                LatLng searchedPosition = DataHelper.extractLatLng(lastSearchedAddress);
                String textAddress = DataHelper.extractAddressText(lastSearchedAddress);
                addressText.setText(textAddress);

                mapTracker.setLastSearchedAddressMarker(mapView.addMarker(new MarkerOptions()
                        .position(searchedPosition)
                        .title("Searched Address:")
                        .snippet(textAddress)));

                enterSearchDisplay();

                mapView.setCenterCoordinate(searchedPosition);
            } catch (IllegalStateException ise) {
                Toast.makeText(getApplicationContext(),
                        "cannot resolve exact location of searched address",
                        Toast.LENGTH_LONG).show();
            }
            mapTracker.setHandleAddressOnResume(false);
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

    // Handles result of other activity execution like address searching
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                Address selectedAddress = data.getExtras().getParcelable("SELECTED_ADDRESS");

                mapTracker.setLastSearchedAddress(selectedAddress);
                mapTracker.setHandleAddressOnResume(true);
                // onResume() is about to be called and searched address added to map
            } else if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        } else if (requestCode == 2) {
            if (resultCode == Activity.RESULT_OK) {
                System.out.println(data.getExtras().getBoolean("USER_LATLNG"));
                if (data.getExtras().getBoolean("USER_LATLNG")) {  // use user current location
                    double currentLatitude = mapTracker.getUserLastLocation().getLatitude();
                    double currentLongitude = mapTracker.getUserLastLocation().getLongitude();
                    Address x = new Address(Locale.US);
                    x.setAddressLine(0, "Your Current Location");
                    x.setLatitude(currentLatitude);
                    x.setLongitude(currentLongitude);
                    mapTracker.setCurrentRouteStart(x);
                    System.out.println(mapTracker.getCurrentRouteStart());
                } else {
                    mapTracker.setCurrentRouteStart((Address) data.getExtras().getParcelable("FROM_ADDRESS"));
                }

                mapTracker.setCurrentRouteEnd((Address) data.getExtras().getParcelable("TO_ADDRESS"));
                String mobilitySelection = data.getExtras().getString("MOBILITY_SELECTION");

                System.out.println(mapTracker.getCurrentRouteEnd());
                System.out.println(mapTracker.getCurrentRouteStart());

                if (mapTracker.getCurrentRouteStart() == null || mapTracker.getCurrentRouteEnd() == null) {
                    Toast.makeText(getApplicationContext(),
                            "you must select start and end destination for routing", Toast.LENGTH_LONG).show();
                } else {
                    System.out.println("THIS IS A BIG DEAL!!!!");
                    System.out.println(mobilitySelection);
                    String coordinates = DataHelper.getRouteCoordinates(mapTracker);
                    System.out.println(coordinates);
                    new CallAccessMapAPI().execute(getString(R.string.routing_url), getString(R.string.routing_endpoint), coordinates);
                    mapTracker.setRoutingDialog(ProgressDialog.show(this, "", "Finding Route. Please wait...", true));
                    enterRouteView();
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    } //onActivityResult

    public void checkWifi() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(getApplicationContext(),
                    "Access Map needs Wifi or Mobile Network connection",
                    Toast.LENGTH_LONG).show();
        }
    }

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

    private void enterRouteView() {
        addressText.setVisibility(View.GONE);
        routeButton.setVisibility(View.GONE);
        routeListView.setVisibility(View.VISIBLE);
        closeRouteView.setVisibility(View.VISIBLE);

        RelativeLayout.LayoutParams culb = (RelativeLayout.LayoutParams) centerUserLocationButton.getLayoutParams();
        culb.addRule(RelativeLayout.BELOW, R.id.list_view);

        RelativeLayout.LayoutParams zib = (RelativeLayout.LayoutParams) zoomInButton.getLayoutParams();
        zib.addRule(RelativeLayout.BELOW, R.id.adjust_features_button);

        // visbility gone of address search bar
        // visibility gone of route button
        // visibility visbile on listView
        // load to and from address into listview
        // make listview editable, which should load route screen again
        // make floating action button visible
        // change id of relative layouts
    }

    private void closeRouteView() {
        addressText.setVisibility(View.VISIBLE);
        routeButton.setVisibility(View.VISIBLE);
        routeListView.setVisibility(View.GONE);
        closeRouteView.setVisibility(View.GONE);

        RelativeLayout.LayoutParams culb = (RelativeLayout.LayoutParams) centerUserLocationButton.getLayoutParams();
        culb.addRule(RelativeLayout.BELOW, R.id.address_text_bar);

        RelativeLayout.LayoutParams zib = (RelativeLayout.LayoutParams) zoomInButton.getLayoutParams();
        zib.addRule(RelativeLayout.BELOW, R.id.route_button);

        MapArtist.clearRoute(mapView, mapTracker);
        refreshMap();
    }

    private void enterSearchDisplay() {
        closeSearchDisplayButton.setVisibility(View.VISIBLE);
    }

    private void closeSearchDisplay() {
        closeSearchDisplayButton.setVisibility(View.GONE);
        addressText.setText("");
        mapTracker.setLastSearchedAddressMarker(null); // to prevent drawing
        refreshMap();
    }

    // DATA HANDLING FUNCTIONS BELOW

    public void refreshMap() {
        MapArtist.clearMap(mapView, mapTracker);
        loadData();
    }

    public void loadData() {
        System.out.println("LOADING MORE DATA");
        System.out.println(mapView.getCenterCoordinate());
        String bounds = MapArtist.getDataBounds(mapView.getCenterCoordinate());
        System.out.println("About to load data");
        String api_url = getString(R.string.api_url);
        for (MapFeature mapFeature : mapFeatureState) {
            if (mapFeature.isVisible()) {
                new CallAccessMapAPI().execute(api_url, mapFeature.getUrl(), bounds);
            }
        }
    }

    private class CallAccessMapAPI extends AsyncTask<String, String, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... params) {
            Log.i(TAG, "DOING IN BACKGROUND");
            String urlString = params[0] + params[1];  // URL to call
            String coordinates = params[2];
            JSONObject result = null;

            // HTTP Get
            try {
                String query = "";
                if (params[1].equals("route.json")) {
                    query = String.format("waypoints=%s", coordinates);
                } else {
                    query = String.format("bbox=%s", coordinates);
                }
                URL url = new URL(urlString + "?" + query);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                if (urlConnection == null) {
                    System.out.println("we have a problem");
                }
                BufferedReader in = new BufferedReader( new InputStreamReader(urlConnection.getInputStream()));

                if (in == null) {
                    System.out.println("we have another problem");
                }

                String inputLine;
                StringBuffer response = new StringBuffer();

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
                mapTracker.getRoutingDialog().cancel();
                closeRouteView();
                Toast.makeText(getApplicationContext(), "A Network error occurred, or no possible route avaliable, please check Network connection", Toast.LENGTH_LONG).show();
            } else {
                Log.i(TAG, "POST EXECUTE!!!");
                System.out.println("We made it!");
                try {
                    switch (result.getString("class")) {
                        case "/curbs.geojson":
                            MapArtist.drawCurbs(mapView, result);
                            break;
                        case "/sidewalks.geojson":
                            MapArtist.drawSidewalks(mapView, result);
                            break;
                        case "/permits.geojson":
                            MapArtist.drawPermits(mapView, result);
                            break;
                        case "route.json":
                            System.out.println("here");
                            mapTracker.setCurrentRoute(MapArtist.extractRoute(result));
                            List<String> routeInfo = new ArrayList<String>();
                            routeInfo.add("From: " + DataHelper.extractAddressText(mapTracker.getCurrentRouteStart()));
                            routeInfo.add("To: " + DataHelper.extractAddressText(mapTracker.getCurrentRouteEnd()));
                            ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(),
                                    R.layout.custom_list_element, R.id.list_content, routeInfo);
                            routeListView.setAdapter(adapter);
                            MapArtist.drawRoute(mapView, mapTracker, true);
                            mapTracker.getRoutingDialog().cancel();
                            break;
                    }
                } catch (JSONException je) {
                    Log.i(TAG, "JSON ERROR");
                }
            }
        }
    }  // end CallAccessMapAPI
}