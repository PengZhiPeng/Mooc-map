package com.imooc.baidumap;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Outline;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.baidu.mapapi.overlayutil.PoiOverlay;
import com.baidu.mapapi.search.core.CityInfo;
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

public class PoiSearch extends FragmentActivity implements
		OnGetPoiSearchResultListener, OnGetSuggestionResultListener {

	private com.baidu.mapapi.search.poi.PoiSearch mPoiSearch = null;
	private SuggestionSearch mSuggestionSearch = null;
	private BaiduMap mBaiduMap = null;
	private MapView mMapView = null;
	//定位相关
	private LocationClient mlocationClient;
	private MyLocationListener mLocationListener;
	private double mLatitude;
	private double mLongtitude;
	private boolean isFirstIn = true;
	//搜索关键字输入窗口
	private AutoCompleteTextView keyWorldsView = null;
	private ArrayAdapter<String> sugAdapter = null;
	private int load_Index = 0;

	private View fabView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_poisearch);
		initMapView();
		initLocation();
		initListener();
	}

	private void initMapView() {
		//初始化地图
		mMapView = (MapView) findViewById(R.id.map);
		mBaiduMap = mMapView.getMap();
		MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(15.0f);
		mBaiduMap.setMapStatus(msu);

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
		fabView = findViewById(R.id.fab_add_poi);
		fabView.setOutlineProvider(viewOutlineProvider);

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
		mBaiduMap.setOnMapLoadedCallback(new BaiduMap.OnMapLoadedCallback() {

			@Override
			public void onMapLoaded() {
				mMapView.setScaleControlPosition(new Point(200, screenheight-307));
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
	}

	private void initListener() {
		// 初始化搜索模块，注册搜索事件监听
		mPoiSearch = com.baidu.mapapi.search.poi.PoiSearch.newInstance();
		mPoiSearch.setOnGetPoiSearchResultListener(this);
		mSuggestionSearch = SuggestionSearch.newInstance();
		mSuggestionSearch.setOnGetSuggestionResultListener(this);
		keyWorldsView = (AutoCompleteTextView) findViewById(R.id.searchkey);
		sugAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line);
		keyWorldsView.setAdapter(sugAdapter);
		//当输入关键字变化时，动态更新建议列表
		keyWorldsView.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable arg0) {}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
										  int arg2, int arg3) {}

			@Override
			public void onTextChanged(CharSequence cs, int arg1, int arg2,
									  int arg3) {
				if (cs.length() <= 0) {
					return;
				}
				String city = ((EditText) findViewById(R.id.city)).getText()
						.toString();
				/**
				 * 使用建议搜索服务获取建议列表，结果在onSuggestionResult()中更新
				 */
				mSuggestionSearch
						.requestSuggestion((new SuggestionSearchOption())
								.keyword(cs.toString()).city(city));
			}
		});

		mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
			@Override
			public void onMapClick(LatLng latLng) {
				mBaiduMap.hideInfoWindow();
			}

			@Override
			public boolean onMapPoiClick(MapPoi mapPoi) {
				return false;
			}
		});

		fabView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog();
			}
		});

		findViewById(R.id.ib_loca_poi).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				centerToMyLocation();
			}
		});
		findViewById(R.id.jump2route).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent toRoute = new Intent(PoiSearch.this, RoutePlan.class);
				startActivity(toRoute);
				finish();
			}
		});
		findViewById(R.id.jump2bus).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent toBus = new Intent(PoiSearch.this, BusLineSearch.class);
				startActivity(toBus);
				finish();
			}
		});
		findViewById(R.id.jump2pano).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent toPano = new Intent(PoiSearch.this, PanoMain.class);
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
						Intent intent2Main = new Intent(PoiSearch.this,MainActivity.class);
						startActivity(intent2Main);
						finish();
						break;
					case 1://POI
						break;
					case 2://routePlan
						Intent intent2Route = new Intent(PoiSearch.this,RoutePlan.class);
						startActivity(intent2Route);
						finish();
						break;
					case 3://BusLine
						Intent intent2Bus = new Intent(PoiSearch.this,BusLineSearch.class);
						startActivity(intent2Bus);
						finish();
						break;
					case 4://panorama
						Intent intent2Pano = new Intent(PoiSearch.this,PanoMain.class);
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

	@Override
	protected void onStart() {
		//开始定位
		mBaiduMap.setMyLocationEnabled(true);
		if (!mlocationClient.isStarted())
			mlocationClient.start();
		ActionBar actionBar = this.getActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		super.onStart();
	}

	@Override
	protected void onStop() {
		//停止定位
		mBaiduMap.setMyLocationEnabled(false);
		mlocationClient.stop();
		super.onStop();
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
		mPoiSearch.destroy();
		mSuggestionSearch.destroy();
		mMapView.onDestroy();
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
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
	//定位
	private class MyLocationListener implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			MyLocationData data2 = new MyLocationData.Builder()
					.accuracy(location.getRadius())//精度
					.latitude(location.getLatitude())//纬度
					.longitude(location.getLongitude()).build();//经度
			mBaiduMap.setMyLocationData(data2);
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

	public void onGetPoiResult(PoiResult result) {
		if (result == null
				|| result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
			Toast.makeText(PoiSearch.this, "未找到结果", Toast.LENGTH_LONG)
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
			return;
		}
		if (result.error == SearchResult.ERRORNO.AMBIGUOUS_KEYWORD) {
			// 当输入关键字在本市没有找到，但在其他城市找到时，返回包含该关键字信息的城市列表
			String strInfo = "在";
			for (CityInfo cityInfo : result.getSuggestCityList()) {
				strInfo += cityInfo.city;
				strInfo += ",";
			}
			strInfo += "找到结果";
			Toast.makeText(PoiSearch.this, strInfo, Toast.LENGTH_LONG)
					.show();
		}
	}

	public void onGetPoiDetailResult(PoiDetailResult result) {
		if (result.error != SearchResult.ERRORNO.NO_ERROR) {
			Toast.makeText(PoiSearch.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
					.show();
		} else {
			Toast.makeText(PoiSearch.this, result.getName() + ": " + result.getAddress(), Toast.LENGTH_SHORT)
			.show();
		}
		ShowInfoWindow(result);
	}
	//显示infowindow，显示点击的POI的名称。
	private void ShowInfoWindow(PoiDetailResult result) {
		TextView tv = new TextView(this);
		tv.setBackgroundResource(R.drawable.popup);
		tv.setPadding(30, 20, 30, 50);
		tv.setGravity(Gravity.CENTER);
		tv.setText(result.getName());
		LatLng ll = new LatLng(result.getLocation().latitude, result.getLocation().longitude);
		InfoWindow infoWindow = new InfoWindow(tv, ll, -47);
		mBaiduMap.showInfoWindow(infoWindow);
	}

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
