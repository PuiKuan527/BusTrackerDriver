package com.test.bustrackerdriver.Interface;


import com.test.bustrackerdriver.MyLatLng;
import com.test.bustrackerdriver.MyLatLng1;

import java.util.List;

public interface IOnLoadLocationListener {
    void OnLoadLocationSuccess(List<MyLatLng> latLngs);
    void OnLoadLocationFailed(String Message);
    void OnLoadLocationSuccess1(List<MyLatLng1> latLngs);

}
