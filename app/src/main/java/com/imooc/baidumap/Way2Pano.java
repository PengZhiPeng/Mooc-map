package com.imooc.baidumap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class Way2Pano extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_way2_pano);
        Button geo2Pano = (Button) findViewById(R.id.btn_GEO2Pano);
        Button interior2Pano = (Button) findViewById(R.id.btn_Interior2Pano);
        geo2Pano.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent geoOpenPano = new Intent(Way2Pano.this,PanoMain.class);
                startActivity(geoOpenPano);
            }
        });
        interior2Pano.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent interOpenPano = new Intent(Way2Pano.this,PanoMain.class);
                startActivity(interOpenPano);
            }
        });
    }
}
