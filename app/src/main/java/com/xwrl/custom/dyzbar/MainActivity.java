package com.xwrl.custom.dyzbar;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.xwrl.custom.volumebar.DyzRoundProgressBar;
import com.xwrl.custom.volumebar.DyzVolumeBar;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private DyzVolumeBar mVolumeBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mVolumeBar = findViewById(R.id.activity_test_dyz_bar);

        //设置监听回调
        mVolumeBar.setChangeListener(new VolumeBarChangeListener());
        //设置是否为可更改音量设备(区别安卓子系统等),此项更改只会影响到自动隐藏进度条的时间
        mVolumeBar.isFixedVolumeDevices(true);
        mVolumeBar.setDbv_max(150);//最大值
        mVolumeBar.setDbv_progress(99);//进度
        //mVolumeBar.setRounds();圆角弧度
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVolumeBar != null) { mVolumeBar = null; }
    }

    public class VolumeBarChangeListener implements DyzRoundProgressBar.OnDyzBarChangeListener{

        @Override
        public void onProgressChanged(DyzRoundProgressBar roundBar, float progress, boolean fromUser) {
            Log.d(TAG, "onProgressChanged: "+progress+", 滑动更改是否来自用户 "+fromUser);
            //设置进度不一定在这儿，看更改音量之后回调之地
            mVolumeBar.setDbv_progress(progress);
        }

        @Override
        public void onStartTrackingTouch(DyzRoundProgressBar roundBar) {
            Log.d(TAG, "onStartTrackingTouch: 开始触摸进度条");
        }

        @Override
        public void onStopTrackingTouch(DyzRoundProgressBar roundBar) {
            Log.d(TAG, "onStopTrackingTouch: 结束触摸进度条");
        }
    }
}