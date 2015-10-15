package com.imooc.baidumap;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.baidu.lbsapi.BMapManager;
import com.baidu.lbsapi.panoramaview.PanoramaView;
import com.baidu.lbsapi.panoramaview.PanoramaViewListener;

public class PanoMain extends Activity {

    private static final String LTAG = "BaiduPanoSDKDemo";
    private PanoramaView mPanoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 先初始化BMapManager
        initBMapManager();
        setContentView(R.layout.activity_pano_main);
        initView();
    }

    private void initBMapManager() {
        PanoDemoApplication app = (PanoDemoApplication) this.getApplication();
        if (app.mBMapManager == null) {
            app.mBMapManager = new BMapManager(app);
            app.mBMapManager.init(new PanoDemoApplication.MyGeneralListener());
        }
    }

    private void initView() {
        mPanoView = (PanoramaView) findViewById(R.id.panorama);
        mPanoView.setShowTopoLink(true);
        mPanoView.setPanoramaImageLevel(PanoramaView.ImageDefinition.ImageDefinitionMiddle);
        double lat = 24.334535;
        double lon = 116.134622;
        mPanoView.setPanorama(lon, lat);
        // 测试回调函数,需要注意的是回调函数要在setPanorama()之前调用，否则回调函数可能执行异常
        mPanoView.setPanoramaViewListener(new PanoramaViewListener() {

            @Override
            public void onLoadPanoramaBegin() {
                Log.i(LTAG, "onLoadPanoramaStart...");
            }

            @Override
            public void onLoadPanoramaEnd(String json) {
                Log.i(LTAG, "onLoadPanoramaEnd : " + json);
            }

            @Override
            public void onLoadPanoramaError(String error) {
                Log.i(LTAG, "onLoadPanoramaError : " + error);
            }
        });
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
        mPanoView.destroy();
        super.onDestroy();
    }

}
