package edu.washington.accessmap;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by samuelfelker on 11/19/15.
 */
public class MapFeature implements Parcelable {
    private String name;
    private String url;
    private boolean visible;

    public MapFeature(String name, String url, boolean visible) {
        this.name = name;
        this.url = url;
        this.visible = visible;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisibility(boolean x) {
        visible = x;
    }

    // Parcelling part
    public MapFeature(Parcel in){
        String[] data = new String[3];

        in.readStringArray(data);
        this.name = data[0];
        this.url = data[1];
        this.visible = Boolean.parseBoolean(data[2]);
    }

    @Override
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {this.name,
                this.url,
                Boolean.toString(this.visible)});
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public MapFeature createFromParcel(Parcel in) {
            return new MapFeature(in);
        }

        public MapFeature[] newArray(int size) {
            return new MapFeature[size];
        }
    };
}
