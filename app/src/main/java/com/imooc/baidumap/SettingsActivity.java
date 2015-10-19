package com.imooc.baidumap;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
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


}
