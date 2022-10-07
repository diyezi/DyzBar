package com.xwrl.custom.volumebar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2022/9/27
 * 作用:
 */
public class DyzVolumeBar extends ConstraintLayout{
    private static final String TAG = "DyzVolumeBar";

    private ImageView iv_volume;
    private DyzRoundProgressBar bar_volume;
    private float progressOld;
    private int maxSize;
    private Timer mTimer;

    public DyzVolumeBar(@NonNull Context context) {
        super(context);
        initView(context,null);
    }

    public DyzVolumeBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context,attrs);
    }

    public DyzVolumeBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
    }

    public void release(){
        if (mTimer != null) {
            mTimer.purge();
            mTimer.cancel();
            mTimer = null;
        }
        if (iv_volume != null) { iv_volume = null; }
        if (bar_volume != null) { bar_volume = null; }
    }

    /**
     * @param context 上下文对象
     * @param attrs
     * 用代码来替代简单的布局文件，及控件初始化
     * */
    @SuppressLint("ResourceType")
    private void initView(Context context, AttributeSet attrs){
        //1.初始化
        iv_volume = new ImageView(context);
        bar_volume = new DyzRoundProgressBar(context);

        iv_volume.setImageResource(R.drawable.iv_sounds); //默认图标
        iv_volume.setScaleType(ImageView.ScaleType.FIT_XY);//铺满控件

        //2.代码添加 ConstraintLayout 布局规则
        maxSize = (int)dpToPx(50); //最大控件宽度

        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.bottomToBottom = LayoutParams.PARENT_ID; //底部与父控件相同
        params.startToStart = LayoutParams.PARENT_ID; //左边与父控件相同
        params.endToEnd = LayoutParams.PARENT_ID; //右边与父控件相同，居中
        params.width = params.height = maxSize; //设置控件的宽高
        iv_volume.setLayoutParams(params);
        iv_volume.setId(998); //设置控件id
        int padding = (int) (maxSize / 5 * 1.4f);
        iv_volume.setPadding(padding,padding,padding,padding); //设置Padding
        addView(iv_volume); //添加该View

        LayoutParams paramsBar = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        paramsBar.bottomToBottom = LayoutParams.PARENT_ID; //底部与父控件相同
        paramsBar.circleConstraint = 998; //设置角度约束中的另一控件id，达到底部处于其中心高度的约束关系
        paramsBar.width = maxSize; //设置控件的宽
        paramsBar.height = (int) (maxSize * 2.6f); //设置控件的高
        paramsBar.circleRadius = paramsBar.height >> 1; //由于是两个控件的中心距离并在同一直线上，所以取最长控件的宽或高的一半
        bar_volume.setLayoutParams(paramsBar);
        bar_volume.setVisibility(GONE);//默认隐藏
        bar_volume.setAlpha(0); //默认Alpha值为0
        bar_volume.setId(999); //设置控件id
        addView(bar_volume); //添加该View

        //3.点击事件 和 布局配置读取
        iv_volume.setOnClickListener(view -> openVolumeView());
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DyzVolumeBar);
            int fullColor = ta.getInt(R.styleable.DyzVolumeBar_dbv_color_bg, 0x20EEEEEE);
            int progressColor = ta.getInt(R.styleable.DyzVolumeBar_dbv_color_progress, 0xCCEEEEEE);
            float strokeWidth = ta.getFloat(R.styleable.DyzVolumeBar_dbv_width_stroke, 10f);
            float mRounds = ta.getFloat(R.styleable.DyzVolumeBar_dbv_round, 26f);
            float progress = ta.getFloat(R.styleable.DyzVolumeBar_dbv_progress, 130f);
            int max = ta.getInt(R.styleable.DyzVolumeBar_dbv_max, 150);
            ta.recycle();

            bar_volume.setMax(max);
            bar_volume.setProgress(progress);
            bar_volume.setRounds(mRounds);
            bar_volume.setStrokeWidth(strokeWidth);
            bar_volume.setProgressColor(progressColor);
            bar_volume.setFullColor(fullColor);
        }
    }

    /**
     * @param progress 当前音量
     * 可绑DataBinding 根据其规则来命名此Setter方法名
     * */
    public void setDbv_progress(float progress){
        if (bar_volume != null){
            if (!bar_volume.isDyzBarChanging()) { bar_volume.setProgress(progress); }

            int maxHalf = bar_volume.getMax() >> 1;
            //Log.e(TAG, "setProgress: "+progress+", 原Progress："+progressOld+", "+maxHalf);
            //progressOld == 0 : 该控件还未初始化
            if (progressOld == 0 || (progress >= maxHalf && progressOld < maxHalf) ||
                    (progressOld >= maxHalf && progress < maxHalf)){
                changeVolumeIcon(progress,false);
            }
            progressOld = progress;
        }
    }

    /**
     * @param max 最大音量
     * 可绑DataBinding 根据其规则来命名此Setter方法名
     * */
    public void setDbv_max(int max){
        //Log.e(TAG, "setMax: "+max);
        if (bar_volume != null){ bar_volume.setMax(max); }
    }

    /**
     * 点击事件
     * 用属性动画来显示音量进度条
     * 并且使用{@link Timer}定时器 在一段时间后 来 自动隐藏 音量进度条
     * */
    private void openVolumeView(){
        LayoutParams layoutParams = (LayoutParams) getLayoutParams();
        int size = maxSize >> 1, height = (int) (maxSize * 3.1f) +
                (bar_volume.getVisibility() == GONE ? size : size - bar_volume.getHeight());
        layoutParams.height = height;
        Log.d(TAG, "openVolumeView: "+bar_volume.getVisibility()+", "+height);

        changeVolumeIcon(bar_volume.getProgress(),true);
        bar_volume.startFadeAnimator();

        if (bar_volume.getAlpha() == DyzRoundProgressBar.BAR_ALPHA_MIN) {
            if (mTimer == null) { mTimer = new Timer(); }
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (bar_volume != null && !bar_volume.isDyzBarChanging() &&
                            System.currentTimeMillis() - bar_volume.getAutoCloseDownTime() >
                                    bar_volume.getBarAutoCloseTime()){
                        //当前在子线程，需要切换回主线程去调用执行缩小动画
                        bar_volume.post(()-> openVolumeView());
                    }
                }
            },100,100);
        }else {
            if (mTimer != null){
                mTimer.purge();
                mTimer.cancel();
                mTimer = null;
            }
        }
    }

    /**
     * @param progress 音量进度条进度
     * @param isClick 音量进度条是否在显示中
     * 根据进度来显示合适的音量状态图标
     * */
    private void changeVolumeIcon(float progress, boolean isClick){
        Log.d(TAG, "changeVolumeIcon: "+progress+", "+bar_volume.getAlpha());

        int r1 = R.drawable.iv_sounds, r2 = R.drawable.iv_sounds_fill,
                r3 = R.drawable.iv_sounds_small, r4 = R.drawable.iv_sounds_small_fill;

        if (progress >= bar_volume.getMax() >> 1){
            iv_volume.setImageResource(bar_volume.getAlpha() <=
                    DyzRoundProgressBar.BAR_ALPHA_MIN ? (isClick ? r2 : r1) : (isClick ? r1 : r2));

        }else {
            iv_volume.setImageResource(bar_volume.getAlpha() <=
                    DyzRoundProgressBar.BAR_ALPHA_MIN ? (isClick ? r4 : r3) : (isClick ? r3 : r4));
        }
    }

    /**
     * @param isFixed 是否是固定音量设备
     * 如果是，那么就只能调节媒体声道音量
     * 否则还可以调节系统音量
     * 这个结果只会影响到本音量进度条的自动关闭时间
     * 比如是，那么自动关闭时间为 2000ms，否为 1200ms */
    public void isFixedVolumeDevices(boolean isFixed) {
        if (bar_volume != null){
            bar_volume.setBarAutoCloseTime(isFixed);
        }else {
            throw new IllegalArgumentException(" VolumeProgressBar View not ViewBinding! ");
        }
    }

    public void setAutoCloseTime(long time){
        if (bar_volume != null){
            bar_volume.setBarAutoCloseTime(time < 500 ? 500 : time);
        }else {
            throw new IllegalArgumentException(" VolumeProgressBar View not ViewBinding! ");
        }
    }

    /**
     * @param changeListener 进度条更改的回调
     * 跟{@link android.widget.SeekBar.OnSeekBarChangeListener} 差不多
     * */
    public void setChangeListener(DyzRoundProgressBar.OnDyzBarChangeListener changeListener) {
        if (bar_volume != null) {
            bar_volume.setChangeListener(changeListener);
        }else {
            throw new IllegalArgumentException(" VolumeProgressBar View not ViewBinding! ");
        }
    }

    public void setRounds(float rounds){
        if (bar_volume != null) {
            bar_volume.setRounds(rounds);
        }else {
            throw new IllegalArgumentException(" VolumeProgressBar View not ViewBinding! ");
        }
    }

    /**
     * @param dp
     * dp 转 px
     * */
    public float dpToPx(int dp){
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources()
                .getDisplayMetrics());
    }
}
