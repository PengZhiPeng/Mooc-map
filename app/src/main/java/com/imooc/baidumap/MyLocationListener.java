package com.imooc.baidumap;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

/**
 * Created by acer on 2015/10/26.
 */
//定位
public class MyLocationListener implements BDLocationListener {

    private BaiduMap mBaiduMap = null;
    private boolean isFirstIn = true;
    private double mLatitude;
    private double mLongtitude;

    private MyLocationListener(){}
    private static MyLocationListener instance = new MyLocationListener();
    public static MyLocationListener getInstance(){
        return instance;
    }

    @Override
    public void onReceiveLocation(BDLocation location) {
        MyLocationData data = new MyLocationData.Builder()
                .direction(100)//方向
                .accuracy(location.getRadius())//精度
                .latitude(location.getLatitude())//纬度
                .longitude(location.getLongitude()).build();//经度
        mBaiduMap.setMyLocationData(data);
        MyLocationConfiguration config =
                new MyLocationConfiguration(null, true, null);
        mBaiduMap.setMyLocationConfigeration(config);
        //更新经纬度
        mLatitude = location.getLatitude();
        mLongtitude = location.getLongitude();
        if (isFirstIn) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
            mBaiduMap.animateMapStatus(msu);
            isFirstIn = false;
        }
    }
}

