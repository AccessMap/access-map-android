package edu.washington.accessmap;

/**
 * Created by samuelfelker on 11/19/15.
 */
public class MapFeatureState {
    MapFeature[] features;

    public MapFeatureState(String[] featureResources) {
        features = new MapFeature[featureResources.length];
        for (int i = 0; i < featureResources.length; i++) {
            String[] results = featureResources[i].split("|");
            features[i] = new MapFeature(results[0], results[1], Boolean.valueOf(results[2]));
        }
    }

    public MapFeature[] getFeatures() {
        return features;
    }
}
