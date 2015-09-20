package com.imooc.baidumap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Outline;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.PoiOverlay;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;


public class MainActivity extends Activity {

    private MapView mMapView;
    private BaiduMap mBaiduMap = null;

    private Context context;

    //定位相关
    private LocationClient mlocationClient;
    private MyLocationListener mLocationListener;
    private boolean isFirstIn = true;
    private double mLatitude;
    private double mLongtitude;

    //自定义定位图标
//    private BitmapDescriptor mIconLocation;
    private MyOrientationListener myOrientationListener;
    private float mCurrentX;
    private MyLocationConfiguration.LocationMode mLocationMode;

    //POI检索
    private com.baidu.mapapi.search.poi.PoiSearch mPoiSearch = null;
    private SuggestionSearch mSuggestionSearch = null;
    //搜索关键字输入窗口
    private AutoCompleteTextView keyWorldsView = null;
    private ArrayAdapter<String> sugAdapter = null;
    private int load_Index = 0;

    private View fabView;
    private Button requestLocButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        this.context = this;
        initView();
        initLocation();
        initSearch();
        setOnClickListener();
    }

    private void initSearch() {
        // 初始化搜索模块，注册搜索事件监听
        mPoiSearch = com.baidu.mapapi.search.poi.PoiSearch.newInstance();
        mSuggestionSearch = SuggestionSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(poiListener);
        mSuggestionSearch.setOnGetSuggestionResultListener(sugListener);
        keyWorldsView = (AutoCompleteTextView) findViewById(R.id.searchkey);
        sugAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line);
        keyWorldsView.setAdapter(sugAdapter);
        //当输入关键字变化时，动态更新建议列表
        keyWorldsView.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2,
                                      int arg3) {
                if (cs.length() <= 0) {
                    return;
                }
                String city = ((EditText) findViewById(R.id.city)).getText()
                        .toString();
                //使用建议搜索服务获取建议列表，结果在onSuggestionResult()中更新
                mSuggestionSearch
                        .requestSuggestion((new SuggestionSearchOption())
                                .keyword(cs.toString()).city(city));
            }
        });

    }

    private void initLocation() {

        mlocationClient = new LocationClient(this);
        mLocationListener = new MyLocationListener();
        mlocationClient.registerLocationListener(mLocationListener);

        LocationClientOption option = new LocationClientOption();
        option.setCoorType("bd09ll");
        option.setIsNeedAddress(true);
        option.setOpenGps(true);
        option.setScanSpan(1000);
        mlocationClient.setLocOption(option);
//        初始化图标
//        mIconLocation = BitmapDescriptorFactory.fromResource(R.drawable.navi_map_gps_locked);

        myOrientationListener = new MyOrientationListener(context);

        myOrientationListener.setOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mCurrentX = x;
            }
        });
    }

    private void initView() {
        findViewById(R.id.PoiSearchLayout).setVisibility(View.GONE);
        mMapView = (MapView) findViewById(R.id.id_bmapView);
        mBaiduMap = mMapView.getMap();
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(15.0f);
        mBaiduMap.setMapStatus(msu);

        mLocationMode = MyLocationConfiguration.LocationMode.NORMAL;
        requestLocButton = (Button) findViewById(R.id.map_mode);
        requestLocButton.setText("普通模式");

        //隐藏缩放控件和百度logo
        int childCount = mMapView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mMapView.getChildAt(i);
            if (child instanceof ZoomControls || child instanceof ImageView) {
                child.setVisibility(View.GONE);
            }
        }

        ViewOutlineProvider viewOutlineProvider = new ViewOutlineProvider() {

            @Override
            public void getOutline(View view, Outline outline) {
                // 获取按钮的尺寸
                int fabSize = getResources().getDimensionPixelSize(
                        R.dimen.fab_size);
                // 设置轮廓的尺寸
                outline.setOval(-4, -4, fabSize + 2, fabSize + 2);
            }
        };
        // 获得右下角圆形按钮对象
        fabView = findViewById(R.id.fab_add);
        fabView.setOutlineProvider(viewOutlineProvider);

    }

    private void setOnClickListener() {
        findViewById(R.id.ib_locaMyPosition).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                centerToMyLocation();
            }
        });
        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                findViewById(R.id.PoiSearchLayout).setVisibility(View.GONE);
                mBaiduMap.hideInfoWindow();
            }

            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                return false;
            }
        });

        requestLocButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mLocationMode) {
                    case NORMAL:
                        mLocationMode = MyLocationConfiguration.LocationMode.FOLLOWING;
                        requestLocButton.setText("跟随模式");
                        break;
                    case FOLLOWING:
                        mLocationMode = MyLocationConfiguration.LocationMode.COMPASS;
                        requestLocButton.setText("罗盘模式");
                        break;
                    case COMPASS:
                        mLocationMode = MyLocationConfiguration.LocationMode.NORMAL;
                        requestLocButton.setText("普通模式");
                        break;
                }
            }
        });

        fabView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });
    }

    private void showDialog() {
        final String[] items = getResources().getStringArray(R.array.more_feature);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("更多功能");
        builder.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                switch (which) {
                    case 0:

                        break;
                    case 1:
                        findViewById(R.id.PoiSearchLayout).setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        Intent intent = new Intent(MainActivity.this,RoutePlan.class);
                        startActivity(intent);
                        break;
                    case 3:

                        break;
                    case 4:

                        break;

                }

            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        }).create().show();
    }

    //点击EditText文本框之外任何地方隐藏键盘
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (isShouldHideInput(v, ev)) {

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
            return super.dispatchTouchEvent(ev);
        }
        // 必不可少，否则所有的组件都不会有TouchEvent了
        if (getWindow().superDispatchTouchEvent(ev)) {
            return true;
        }
        return onTouchEvent(ev);
    }

    public boolean isShouldHideInput(View v, MotionEvent event) {
        if (v != null && (v instanceof EditText)) {
            int[] leftTop = {0, 0};
            //获取输入框当前的location位置
            v.getLocationInWindow(leftTop);
            int left = leftTop[0];
            int top = leftTop[1];
            int bottom = top + v.getHeight();
            int right = left + v.getWidth();
            if (event.getX() > left && event.getX() < right
                    && event.getY() > top && event.getY() < bottom) {
                // 点击的是输入框区域，保留点击EditText的事件
                return false;
            } else {
                return true;
            }
        }
        return false;
    }


    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //开始定位
        mBaiduMap.setMyLocationEnabled(true);
        if (!mlocationClient.isStarted())
            mlocationClient.start();
        //开始方向传感器
        myOrientationListener.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        mPoiSearch.destroy();
        mSuggestionSearch.destroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //停止定位
        mBaiduMap.setMyLocationEnabled(false);
        mlocationClient.stop();
        //关闭方向传感器
        myOrientationListener.stop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    //设置底图显示模式
    public void setMapMode(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.normal:
                if (checked)
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                break;
            case R.id.statellite:
                if (checked)
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                break;
        }
    }

    //设置是否显示交通图
    public void setTraffic(View view) {
        mBaiduMap.setTrafficEnabled(((CheckBox) view).isChecked());
    }

    //设置是否显示百度热力图
    public void setBaiduHeatMap(View view) {
        mBaiduMap.setBaiduHeatMapEnabled(((CheckBox) view).isChecked());
    }


    //定位到我的位置
    private void centerToMyLocation() {
        LatLng latLng = new LatLng(mLatitude, mLongtitude);
        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
        mBaiduMap.animateMapStatus(msu);
    }

    private class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            MyLocationData data = new MyLocationData.Builder()
                    .direction(mCurrentX)//更新当前方向
                    .accuracy(location.getRadius())//精度
                    .latitude(location.getLatitude())//纬度
                    .longitude(location.getLongitude()).build();//经度
            mBaiduMap.setMyLocationData(data);
            //可在第三个参数设置自定义图标
            MyLocationConfiguration config =
                    new MyLocationConfiguration(mLocationMode, true, null);
            mBaiduMap.setMyLocationConfigeration(config);
            //更新经纬度
            mLatitude = location.getLatitude();
            mLongtitude = location.getLongitude();

            if (isFirstIn) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
                mBaiduMap.animateMapStatus(msu);
                isFirstIn = false;

                Toast.makeText(context, location.getAddrStr(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    //影响搜索按钮点击事件
    public void searchButtonProcess(View v) {
        EditText editCity = (EditText) findViewById(R.id.city);
        EditText editSearchKey = (EditText) findViewById(R.id.searchkey);
        mPoiSearch.searchInCity((new PoiCitySearchOption())
                .city(editCity.getText().toString())
                .keyword(editSearchKey.getText().toString())
                .pageNum(load_Index));
    }

    public void goToNextPage(View v) {
        load_Index++;
        searchButtonProcess(null);
    }

    public OnGetPoiSearchResultListener poiListener = new OnGetPoiSearchResultListener() {

        @Override
        public void onGetPoiResult(PoiResult result) {
            if (result == null
                    || result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                Toast.makeText(MainActivity.this, "未找到结果", Toast.LENGTH_LONG)
                        .show();
                return;
            }
            if (result.error == SearchResult.ERRORNO.NO_ERROR) {
                mBaiduMap.clear();
                PoiOverlay overlay = new MyPoiOverlay(mBaiduMap);
                mBaiduMap.setOnMarkerClickListener(overlay);
                overlay.setData(result);
                overlay.addToMap();
                overlay.zoomToSpan();
            }
        }

        @Override
        public void onGetPoiDetailResult(PoiDetailResult result) {
            if (result.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(MainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
                        .show();
            } else {
                Toast.makeText(MainActivity.this, result.getName() + ": " + result.getAddress(), Toast.LENGTH_SHORT)
                        .show();
            }
            ShowInfoWindow(result);
        }
    };

    //显示infowindow，显示点击的POI的名称。
    private void ShowInfoWindow(PoiDetailResult result) {
        TextView tv = new TextView(context);
        tv.setBackgroundResource(R.drawable.popup);
        tv.setPadding(30, 20, 30, 50);
        tv.setGravity(Gravity.CENTER);
        tv.setText(result.getName());
        LatLng ll = new LatLng(result.getLocation().latitude, result.getLocation().longitude);
        InfoWindow infoWindow = new InfoWindow(tv, ll, -47);
        mBaiduMap.showInfoWindow(infoWindow);
    }

    private OnGetSuggestionResultListener sugListener = new OnGetSuggestionResultListener() {

        @Override
        public void onGetSuggestionResult(SuggestionResult res) {
            if (res == null || res.getAllSuggestions() == null) {
                return;
            }
            sugAdapter.clear();
            for (SuggestionResult.SuggestionInfo info : res.getAllSuggestions()) {
                if (info.key != null)
                    sugAdapter.add(info.key);
            }
            sugAdapter.notifyDataSetChanged();
        }
    };

    private class MyPoiOverlay extends PoiOverlay {

        public MyPoiOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public boolean onPoiClick(int index) {

            super.onPoiClick(index);
            PoiInfo poi = getPoiResult().getAllPoi().get(index);
            // if (poi.hasCaterDetails) {
            mPoiSearch.searchPoiDetail((new PoiDetailSearchOption())
                    .poiUid(poi.uid));
            // }
            return true;

        }
    }

}
