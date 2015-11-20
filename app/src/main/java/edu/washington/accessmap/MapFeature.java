package edu.washington.accessmap;

/**
 * Created by samuelfelker on 11/19/15.
 */
public class MapFeature {
    private String name;
    private String url;
    private boolean state;

    public MapFeature(String name, String url, boolean state) {
        this.name = name;
        this.url = url;
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public boolean getState() {
        return state;
    }

    public void setState(boolean x) {
        state = x;
    }
}
