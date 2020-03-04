package com.test.bustrackerdriver;

public class MyLatLng {
    private double latitude;
    private double longitude;

    public MyLatLng(){

    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
