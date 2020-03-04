package com.test.bustrackerdriver.Interface;



import com.test.bustrackerdriver.MyLatLng;

import java.util.List;

public interface IOnLoadLocationListener {
    void OnLoadLocationSuccess(List<MyLatLng> latLngs);
    void OnLoadLocationFailed(String Message);
}
