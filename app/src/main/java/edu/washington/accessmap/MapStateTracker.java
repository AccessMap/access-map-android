package edu.washington.accessmap;

import android.app.ProgressDialog;
import android.location.Address;
import android.location.Location;

import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;

/**
 * Created by samuelfelker on 12/4/15.
 */
public class MapStateTracker {
    private Marker userLocationMarker = null;
    private Location userLastLocation = null;
    private Marker lastSearchedAddressMarker = null;
    private Address lastSearchedAddress = null;
    private ArrayList<LatLng> currentRoute = null;
    private Address currentRouteStart = null;
    private Address currentRouteEnd = null;
    private boolean handleAddressOnResume = false;
    private LatLng lastCenterLocation = null;
    private double lastZoomLevel = MainActivity.DATA_ZOOM_LEVEL;
    private boolean isFirstConnection = true;
    private ProgressDialog routingDialog = null;
    private LatLng userLatLng = null;

    public MapStateTracker() {}

    // GETTERS AND SETTERS BELOW

    public LatLng getUserLatLng() {
        return userLatLng;
    }

    public void setUserLatLng(LatLng userLatLng) {
        this.userLatLng = userLatLng;
    }

    public ProgressDialog getRoutingDialog() {
        return routingDialog;
    }

    public void setRoutingDialog(ProgressDialog routingDialog) {
        this.routingDialog = routingDialog;
    }

    public Marker getLastSearchedAddressMarker() {
        return lastSearchedAddressMarker;
    }

    public void setLastSearchedAddressMarker(Marker lastSearchedAddressMarker) {
        this.lastSearchedAddressMarker = lastSearchedAddressMarker;
    }

    public Location getUserLastLocation() {
        return userLastLocation;
    }

    public void setUserLastLocation(Location userLastLocation) {
        this.userLastLocation = userLastLocation;
    }

    public Marker getUserLocationMarker() {
        return userLocationMarker;
    }

    public void setUserLocationMarker(Marker userLocationMarker) {
        this.userLocationMarker = userLocationMarker;
    }

    public Address getLastSearchedAddress() {
        return lastSearchedAddress;
    }

    public void setLastSearchedAddress(Address lastSearchedAddress) {
        this.lastSearchedAddress = lastSearchedAddress;
    }

    public ArrayList<LatLng> getCurrentRoute() {
        return currentRoute;
    }

    public void setCurrentRoute(ArrayList<LatLng> currentRoute) {
        this.currentRoute = currentRoute;
    }

    public boolean isHandleAddressOnResume() {
        return handleAddressOnResume;
    }

    public void setHandleAddressOnResume(boolean handleAddressOnResume) {
        this.handleAddressOnResume = handleAddressOnResume;
    }

    public LatLng getLastCenterLocation() {
        return lastCenterLocation;
    }

    public void setLastCenterLocation(LatLng lastCenterLocation) {
        this.lastCenterLocation = lastCenterLocation;
    }

    public double getLastZoomLevel() {
        return lastZoomLevel;
    }

    public void setLastZoomLevel(double lastZoomLevel) {
        this.lastZoomLevel = lastZoomLevel;
    }

    public boolean isFirstConnection() {
        return isFirstConnection;
    }

    public void setIsFirstConnection(boolean isFirstConnection) {
        this.isFirstConnection = isFirstConnection;
    }

    public Address getCurrentRouteEnd() {
        return currentRouteEnd;
    }

    public void setCurrentRouteEnd(Address currentRouteEnd) {
        this.currentRouteEnd = currentRouteEnd;
    }

    public Address getCurrentRouteStart() {
        return currentRouteStart;
    }

    public void setCurrentRouteStart(Address currentRouteStart) {
        this.currentRouteStart = currentRouteStart;
    }
}
