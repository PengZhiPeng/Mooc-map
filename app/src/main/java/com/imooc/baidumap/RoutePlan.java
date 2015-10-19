package com.imooc.baidumap;


import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Outline;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.InputMethodManager;
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
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.DrivingRouteOverlay;
import com.baidu.mapapi.overlayutil.TransitRouteOverlay;
import com.baidu.mapapi.overlayutil.WalkingRouteOverlay;
import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.DrivingRouteLine;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.search.route.TransitRoutePlanOption;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteLine;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;

/**
 * 此activity用来展示如何进行驾车、步行、公交路线搜索并在地图使用RouteOverlay、TransitOverlay绘制
 * 同时展示如何进行节点浏览并弹出泡泡
 */
public class RoutePlan extends Activity implements BaiduMap.OnMapClickListener,
        OnGetRoutePlanResultListener {
    //浏览路线节点相关
    private Button mBtnPre = null;//上一个节点
    private Button mBtnNext = null;//下一个节点
    private int nodeIndex = -1;//节点索引,供浏览节点时使用
    private RouteLine route = null;
    private TextView popupText = null;//泡泡view

    //地图相关，使用继承MapView的MyRouteMapView目的是重写touch事件实现泡泡处理
    //如果不处理touch事件，则无需继承，直接使用MapView即可
    private MapView mMapView = null;    // 地图View
    private BaiduMap mBaidumap = null;
    //搜索相关
    private RoutePlanSearch mSearch = null;    // 搜索模块，也可去掉地图模块独立使用
    private EditText startCityText;
    private EditText endCityText;

    private View fabView;
    private View layoutRoutePlan;
    //定位相关
    private LocationClient mlocationClient;
    private MyLocationListener mLocationListener;
    private boolean isFirstIn = true;
    private double mLatitude;
    private double mLongtitude;

    private boolean isShow = true;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routeplan);
        CharSequence titleLable = "路线规划功能";
        setTitle(titleLable);
        initMapView();
        initLocation();
        setOnClickListener();
        // 初始化搜索模块，注册事件监听
        mSearch = RoutePlanSearch.newInstance();
        mSearch.setOnGetRoutePlanResultListener(this);
    }

    private void setOnClickListener() {
        //地图点击事件处理
        mBaidumap.setOnMapClickListener(this);
        //定位按钮
        findViewById(R.id.ib_loca_routeplan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                centerToMyLocation();
            }
        });

        fabView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });
        layoutRoutePlan = findViewById(R.id.layout_routeplan);
        final View layoutJumpBtn = findViewById(R.id.layout_jumpBtn);
        final float curTranslationY = layoutRoutePlan.getTranslationY();
        final float curTranslationY2 = layoutJumpBtn.getTranslationY();
        //点击按钮显示垂直上移动画，再按则下移
        findViewById(R.id.jump2route).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isShow) {
                    ObjectAnimator animator = ObjectAnimator
                            .ofFloat(layoutRoutePlan, "translationY", curTranslationY, -410f);
                    animator.setDuration(500).start();
                    ObjectAnimator animator2 = ObjectAnimator
                            .ofFloat(layoutJumpBtn, "translationY", curTranslationY2, -410f);
                    animator.setDuration(500).start();
                    animator2.setDuration(500).start();
                    isShow = false;
                } else {
                    ObjectAnimator animator = ObjectAnimator
                            .ofFloat(layoutRoutePlan, "translationY", -410f, curTranslationY);
                    ObjectAnimator animator2 = ObjectAnimator
                            .ofFloat(layoutJumpBtn, "translationY", -410f, curTranslationY2);
                    animator.setDuration(500).start();
                    animator2.setDuration(500).start();
                    isShow = true;
                }
            }
        });
        findViewById(R.id.jump2bus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toBus = new Intent(RoutePlan.this, BusLineSearch.class);
                startActivity(toBus);
                finish();
            }
        });
        findViewById(R.id.jump2pano).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toPano = new Intent(RoutePlan.this, PanoMain.class);
                startActivity(toPano);
                finish();
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
                        Intent intent2Main = new Intent(RoutePlan.this,MainActivity.class);
                        startActivity(intent2Main);
                        finish();
                        break;
                    case 1://POI
                        Intent intent2Poi = new Intent(RoutePlan.this,PoiSearch.class);
                        startActivity(intent2Poi);
                        finish();
                        break;
                    case 2://routePlan
                        break;
                    case 3://BusLine
                        Intent intent2Bus = new Intent(RoutePlan.this,BusLineSearch.class);
                        startActivity(intent2Bus);
                        finish();
                        break;
                    case 4://panorama
                        Intent intent2Pano = new Intent(RoutePlan.this,PanoMain.class);
                        startActivity(intent2Pano);
                        finish();
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
    }

    private void initMapView() {
        //初始化地图
        mMapView = (MapView) findViewById(R.id.map);
        mBaidumap = mMapView.getMap();
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(15.0f);
        mBaidumap.setMapStatus(msu);

        mBtnPre = (Button) findViewById(R.id.pre);
        mBtnNext = (Button) findViewById(R.id.next);
        mBtnPre.setVisibility(View.INVISIBLE);
        mBtnNext.setVisibility(View.INVISIBLE);
        //隐藏缩放控件和百度logo
        int childCount = mMapView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mMapView.getChildAt(i);
            if (child instanceof ZoomControls || child instanceof ImageView) {
                child.setVisibility(View.GONE);
            }
        }
        //改变比例尺的位置
        final int screenheight = this.getWindowManager().getDefaultDisplay().getHeight();
        mBaidumap.setOnMapLoadedCallback(new BaiduMap.OnMapLoadedCallback() {

            @Override
            public void onMapLoaded() {
                mMapView.setScaleControlPosition(new Point(200, screenheight-307));
            }
        });

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
        fabView = findViewById(R.id.fab_add_routeplan);
        fabView.setOutlineProvider(viewOutlineProvider);
    }
    //发起路线规划搜索示例
    public void SearchButtonProcess(View v) {
        //重置浏览节点的路线数据
        route = null;
        mBtnPre.setVisibility(View.INVISIBLE);
        mBtnNext.setVisibility(View.INVISIBLE);
        mBaidumap.clear();
        // 处理搜索按钮响应
        EditText editSt = (EditText) findViewById(R.id.start);
        EditText editEn = (EditText) findViewById(R.id.end);
        startCityText = (EditText) findViewById(R.id.et_startcity);
        endCityText = (EditText) findViewById(R.id.et_endcity);

        String startCity = startCityText.getText().toString();
        String endCity = endCityText.getText().toString();
        String startPlace = editSt.getText().toString();
        String endPlace = editEn.getText().toString();
        //设置起终点信息
        PlanNode stNode = PlanNode.withCityNameAndPlaceName(startCity, startPlace);
        PlanNode enNode = PlanNode.withCityNameAndPlaceName(endCity, endPlace);

        if (v.getId() == R.id.drive) {
            mSearch.drivingSearch((new DrivingRoutePlanOption())
                    .from(stNode)
                    .to(enNode));
        } else if (v.getId() == R.id.transit) {
            mSearch.transitSearch((new TransitRoutePlanOption())
                    .from(stNode)
                    .city(startCity)
                    .to(enNode));
        } else if (v.getId() == R.id.walk) {
            mSearch.walkingSearch((new WalkingRoutePlanOption())
                    .from(stNode)
                    .to(enNode));
        }
    }
    //节点浏览示例
    public void nodeClick(View v) {
        if (route == null ||
                route.getAllStep() == null) {
            return;
        }
        if (nodeIndex == -1 && v.getId() == R.id.pre) {
            return;
        }
        //设置节点索引
        if (v.getId() == R.id.next) {
            if (nodeIndex < route.getAllStep().size() - 1) {
                nodeIndex++;
            } else {
                return;
            }
        } else if (v.getId() == R.id.pre) {
            if (nodeIndex > 0) {
                nodeIndex--;
            } else {
                return;
            }
        }
        //获取节结果信息
        LatLng nodeLocation = null;
        String nodeTitle = null;
        Object step = route.getAllStep().get(nodeIndex);
        if (step instanceof DrivingRouteLine.DrivingStep) {
            nodeLocation = ((DrivingRouteLine.DrivingStep) step).getEntrance().getLocation();
            nodeTitle = ((DrivingRouteLine.DrivingStep) step).getInstructions();
        } else if (step instanceof WalkingRouteLine.WalkingStep) {
            nodeLocation = ((WalkingRouteLine.WalkingStep) step).getEntrance().getLocation();
            nodeTitle = ((WalkingRouteLine.WalkingStep) step).getInstructions();
        } else if (step instanceof TransitRouteLine.TransitStep) {
            nodeLocation = ((TransitRouteLine.TransitStep) step).getEntrance().getLocation();
            nodeTitle = ((TransitRouteLine.TransitStep) step).getInstructions();
        }

        if (nodeLocation == null || nodeTitle == null) {
            return;
        }
        //移动节点至中心
        mBaidumap.setMapStatus(MapStatusUpdateFactory.newLatLng(nodeLocation));
        // show popup
        popupText = new TextView(RoutePlan.this);
        popupText.setBackgroundResource(R.drawable.popup);
        popupText.setTextColor(0xFF000000);
        popupText.setText(nodeTitle);
        mBaidumap.showInfoWindow(new InfoWindow(popupText, nodeLocation, 0));
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onGetWalkingRouteResult(WalkingRouteResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(RoutePlan.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            //起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            //result.getSuggestAddrInfo()
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            nodeIndex = -1;
            mBtnPre.setVisibility(View.VISIBLE);
            mBtnNext.setVisibility(View.VISIBLE);
            route = result.getRouteLines().get(0);
            WalkingRouteOverlay overlay = new WalkingRouteOverlay(mBaidumap);
            mBaidumap.setOnMarkerClickListener(overlay);
            overlay.setData(result.getRouteLines().get(0));
            overlay.addToMap();
            overlay.zoomToSpan();
        }
    }

    @Override
    public void onGetTransitRouteResult(TransitRouteResult result) {

        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(RoutePlan.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            //起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            //result.getSuggestAddrInfo()
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            nodeIndex = -1;
            mBtnPre.setVisibility(View.VISIBLE);
            mBtnNext.setVisibility(View.VISIBLE);
            route = result.getRouteLines().get(0);
            TransitRouteOverlay overlay = new TransitRouteOverlay(mBaidumap);
            mBaidumap.setOnMarkerClickListener(overlay);
            overlay.setData(result.getRouteLines().get(0));
            overlay.addToMap();
            overlay.zoomToSpan();
        }
    }

    @Override
    public void onGetDrivingRouteResult(DrivingRouteResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(RoutePlan.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            //起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            //result.getSuggestAddrInfo()
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            nodeIndex = -1;
            mBtnPre.setVisibility(View.VISIBLE);
            mBtnNext.setVisibility(View.VISIBLE);
            route = result.getRouteLines().get(0);
            DrivingRouteOverlay overlay = new DrivingRouteOverlay(mBaidumap);
            mBaidumap.setOnMarkerClickListener(overlay);
            overlay.setData(result.getRouteLines().get(0));
            overlay.addToMap();
            overlay.zoomToSpan();
        }
    }
    //设置地图显示模式
    public void setMapMode(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.normal:
                if (checked)
                    mBaidumap.setMapType(BaiduMap.MAP_TYPE_NORMAL);//普通
                break;
            case R.id.statellite:
                if (checked)
                    mBaidumap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);//卫星
                break;
        }
    }
    //设置是否显示交通图
    public void setTraffic(View view) {
        mBaidumap.setTrafficEnabled(((CheckBox) view).isChecked());
    }
    //设置是否显示百度热力图
    public void setBaiduHeatMap(View view) {
        mBaidumap.setBaiduHeatMapEnabled(((CheckBox) view).isChecked());
    }
    //定位到我的位置
    private void centerToMyLocation() {
        LatLng latLng = new LatLng(mLatitude, mLongtitude);
        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
        mBaidumap.animateMapStatus(msu);
    }

    private class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            MyLocationData data2 = new MyLocationData.Builder()
                    .accuracy(location.getRadius())//精度
                    .latitude(location.getLatitude())//纬度
                    .longitude(location.getLongitude()).build();//经度
            mBaidumap.setMyLocationData(data2);
            //可在第三个参数设置自定义图标
//            MyLocationConfiguration config =
//                    new MyLocationConfiguration(mLocationMode, true, null);
//            mBaidumap.setMyLocationConfigeration(config);
            //更新经纬度
            mLatitude = location.getLatitude();
            mLongtitude = location.getLongitude();
            if (isFirstIn) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
                mBaidumap.animateMapStatus(msu);
                isFirstIn = false;
            }
        }
    }

    @Override
    public void onMapClick(LatLng point) {
        mBaidumap.hideInfoWindow();
    }

    @Override
    public boolean onMapPoiClick(MapPoi poi) {
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        //开始定位
        mBaidumap.setMyLocationEnabled(true);
        if (!mlocationClient.isStarted())
            mlocationClient.start();
        ActionBar actionBar = this.getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //停止定位
        mBaidumap.setMyLocationEnabled(false);
        mlocationClient.stop();
    }
    
    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mSearch.destroy();
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home: //点击actionbar中的应用图标返回mainactivity
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//跳转后为栈顶且清除原该activity栈之上的activity
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);//栈里有则不创建新的
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
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
}
