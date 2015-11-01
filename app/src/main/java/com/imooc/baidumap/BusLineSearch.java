package com.imooc.baidumap;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.SupportMapFragment;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.BusLineOverlay;
import com.baidu.mapapi.search.busline.BusLineResult;
import com.baidu.mapapi.search.busline.BusLineSearchOption;
import com.baidu.mapapi.search.busline.OnGetBusLineSearchResultListener;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;

import java.util.ArrayList;
import java.util.List;

/**
 * 此demo用来展示如何进行公交线路详情检索，并使用RouteOverlay在地图上绘制,
 * 同时展示如何浏览路线节点并弹出泡泡
 */
public class BusLineSearch extends FragmentActivity implements
		OnGetPoiSearchResultListener, OnGetBusLineSearchResultListener,
		BaiduMap.OnMapClickListener,View.OnClickListener {
	private Button mBtnPre = null;// 上一个节点
	private Button mBtnNext = null;// 下一个节点
	private int nodeIndex = -2;// 节点索引,供浏览节点时使用
	private BusLineResult route = null;// 保存驾车/步行路线数据的变量，供浏览节点时使用
	private List<String> busLineIDList = null;
	private int busLineIndex = 0;
	// 搜索相关
	private PoiSearch mSearch = null; // 搜索模块，也可去掉地图模块独立使用
	private com.baidu.mapapi.search.busline.BusLineSearch mBusLineSearch = null;
	private BaiduMap mBaiduMap = null;
	BusLineOverlay overlay;//公交路线绘制对象
	private boolean isShow = true;
	private View busSearchView;
	//定位相关
	private LocationClient mlocationClient;
	private MyLocationListener mLocationListener;
	private boolean isFirstIn = true;
	private double mLatitude;
	private double mLongtitude;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_busline);
		CharSequence titleLable = "公交线路查询功能";
		setTitle(titleLable);
		mSearch = PoiSearch.newInstance();
		mSearch.setOnGetPoiSearchResultListener(this);
		mBusLineSearch = com.baidu.mapapi.search.busline.BusLineSearch.newInstance();
		mBusLineSearch.setOnGetBusLineSearchResultListener(this);
		busLineIDList = new ArrayList<String>();
        initView();
        setOnClickListener();
	}

    private void setOnClickListener() {
        overlay = new BusLineOverlay(mBaiduMap);
        mBaiduMap.setOnMarkerClickListener(overlay);
        mBaiduMap.setOnMapClickListener(this);
		findViewById(R.id.jump2route).setOnClickListener(this);
		findViewById(R.id.jump2bus).setOnClickListener(this);
		findViewById(R.id.jump2pano).setOnClickListener(this);
		findViewById(R.id.id_more).setOnClickListener(this);
		findViewById(R.id.id_zooomAdd).setOnClickListener(this);
		findViewById(R.id.id_zoomSub).setOnClickListener(this);
		findViewById(R.id.ib_locaMyPosition).setOnClickListener(this);
    }

	@Override
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.id_more:
				Intent toSettings = new Intent(BusLineSearch.this, DrawerActivity.class);
				startActivity(toSettings);
				finish();
				break;
			case R.id.ib_locaMyPosition:
				centerToMyLocation();
				break;
			case R.id.id_zooomAdd:
				MapStatusUpdate zoomIn = MapStatusUpdateFactory.zoomIn();
				mBaiduMap.setMapStatus(zoomIn);
				break;
			case R.id.id_zoomSub:
				MapStatusUpdate zoomOut = MapStatusUpdateFactory.zoomOut();
				mBaiduMap.setMapStatus(zoomOut);
				break;
			case R.id.jump2route:
				Intent toRoute = new Intent(BusLineSearch.this, RoutePlan.class);
				startActivity(toRoute);
				finish();
				break;
			case R.id.jump2bus:
				if (isShow) {
					busSearchView.setVisibility(View.GONE);
					isShow = false;
				} else {
					busSearchView.setVisibility(View.VISIBLE);
					isShow = true;
				}
				break;
			case R.id.jump2pano:
				Intent toPano = new Intent(BusLineSearch.this, PanoMain.class);
				startActivity(toPano);
				finish();
				break;
		}
	}

    private void initView() {
        mBtnPre = (Button) findViewById(R.id.pre);
        mBtnNext = (Button) findViewById(R.id.next);
        mBtnPre.setVisibility(View.INVISIBLE);
        mBtnNext.setVisibility(View.INVISIBLE);
		busSearchView = findViewById(R.id.bus_searchView);
        mBaiduMap = ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.bmapView)).getBaiduMap();
		MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(16.0f);//缩放等级16=200m
		mBaiduMap.setMapStatus(msu);
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
	//定位到我的位置
	private void centerToMyLocation() {
		LatLng latLng = new LatLng(mLatitude, mLongtitude);
		MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
		mBaiduMap.animateMapStatus(msu);
	}
    //发起检索
	public void searchButtonProcess(View v) {
		busLineIDList.clear();
		busLineIndex = 0;
		mBtnPre.setVisibility(View.INVISIBLE);
		mBtnNext.setVisibility(View.INVISIBLE);
		EditText editCity = (EditText) findViewById(R.id.city);
		EditText editSearchKey = (EditText) findViewById(R.id.searchkey);
		// 发起poi检索，从得到所有poi中找到公交线路类型的poi，再使用该poi的uid进行公交详情搜索
		mSearch.searchInCity((new PoiCitySearchOption()).city(
				editCity.getText().toString()).keyword(
				editSearchKey.getText().toString()));
	}

	public void SearchNextBusline(View v) {
		if (busLineIndex >= busLineIDList.size()) {
			busLineIndex = 0;
		}
		if (busLineIndex >= 0 && busLineIndex < busLineIDList.size()
				&& busLineIDList.size() > 0) {
			mBusLineSearch.searchBusLine((new BusLineSearchOption()
					.city(((EditText) findViewById(R.id.city)).getText()
							.toString()).uid(busLineIDList.get(busLineIndex))));

			busLineIndex++;
		}

	}
	//节点浏览示例
	public void nodeClick(View v) {

		if (nodeIndex < -1 || route == null
				|| nodeIndex >= route.getStations().size())
			return;
		TextView popupText = new TextView(this);
		popupText.setBackgroundResource(R.drawable.popup);
		popupText.setTextColor(0xff000000);
		// 上一个节点
		if (mBtnPre.equals(v) && nodeIndex > 0) {
			// 索引减
			nodeIndex--;
		}
		// 下一个节点
		if (mBtnNext.equals(v) && nodeIndex < (route.getStations().size() - 1)) {
			// 索引加
			nodeIndex++;
		}
		if(nodeIndex >= 0){
			// 移动到指定索引的坐标
			mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(route
					.getStations().get(nodeIndex).getLocation()));
			// 弹出泡泡
			popupText.setText(route.getStations().get(nodeIndex).getTitle());
			popupText.setGravity(Gravity.CENTER);
			mBaiduMap.showInfoWindow(new InfoWindow(popupText, route.getStations()
					.get(nodeIndex).getLocation(), 0));
		}
	}
    //设置地图显示模式
    public void setMapMode(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.normal:
                if (checked)
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);//普通
                break;
            case R.id.statellite:
                if (checked)
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);//卫星
                break;
        }
    }

    //设置是否显示交通图
    public void setTraffic(View view) {
        mBaiduMap.setTrafficEnabled(((CheckBox) view).isChecked());
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
	protected void onStart() {
		super.onStart();
		//开始定位
		mBaiduMap.setMyLocationEnabled(true);
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
		mBaiduMap.setMyLocationEnabled(false);
		mlocationClient.stop();
	}
	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		mSearch.destroy();
		mBusLineSearch.destroy();
		super.onDestroy();
	}

	@Override
	public void onGetBusLineResult(BusLineResult result) {
		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
			Toast.makeText(BusLineSearch.this, "抱歉，未找到结果",
					Toast.LENGTH_LONG).show();
			return;
		}
		mBaiduMap.clear();
		route = result;
		nodeIndex = -1;
		overlay.removeFromMap();
		overlay.setData(result);
		overlay.addToMap();
		overlay.zoomToSpan();
		mBtnPre.setVisibility(View.VISIBLE);
		mBtnNext.setVisibility(View.VISIBLE);
		Toast.makeText(BusLineSearch.this, result.getBusLineName(),
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onGetPoiResult(PoiResult result) {

		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
			Toast.makeText(BusLineSearch.this, "抱歉，未找到结果",
					Toast.LENGTH_LONG).show();
			return;
		}
		// 遍历所有poi，找到类型为公交线路的poi
		busLineIDList.clear();
		for (PoiInfo poi : result.getAllPoi()) {
			if (poi.type == PoiInfo.POITYPE.BUS_LINE
					|| poi.type == PoiInfo.POITYPE.SUBWAY_LINE) {
				busLineIDList.add(poi.uid);
			}
		}
		SearchNextBusline(null);
		route = null;
	}

	@Override
	public void onGetPoiDetailResult(PoiDetailResult result) {}

	@Override
	public void onMapClick(LatLng point) {
		mBaiduMap.hideInfoWindow();
	}

	@Override
	public boolean onMapPoiClick(MapPoi poi) {
		return false;
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
	//定位
	public class MyLocationListener implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			MyLocationData data = new MyLocationData.Builder()
					.accuracy(location.getRadius())//精度
					.latitude(location.getLatitude())//纬度
					.longitude(location.getLongitude()).build();//经度
			mBaiduMap.setMyLocationData(data);
			//可在第三个参数设置自定义图标
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
}
