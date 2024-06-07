package com.baidu.carlifevehicle;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.baidu.carlifevehicle.fragment.CommonSettingsFragment;

public class CustomSettingsActivity extends AppCompatActivity {

    private Fragment fragment = new CommonSettingsFragment();
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_common_settings);
        getSupportActionBar().setTitle("自定义设置");
        getSupportActionBar().setHomeButtonEnabled(true);
        // 默认显示左上角返回按钮
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout,fragment)
                .commitAllowingStateLoss();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }
}