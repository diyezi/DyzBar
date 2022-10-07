package com.xwrl.custom.volumebar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import java.lang.ref.SoftReference;
import java.util.Locale;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2022/8/29
 * 作用: 绘制一个圆角矩形进度条。
 * 实现步骤：
 *      1. 绘制作背景的圆角矩形，{@link android.widget.SeekBar}
 *      2. 绘制作进度的圆角矩形，通过{@link Canvas#clipRect(Rect)} 实现 圆角矩形的截取，完成进度的显示。
 *      3. 设计进度与总进度，通过{@link MotionEvent}触摸滑动来实时更改{@link DyzRoundProgressBar}的进度。
 *      4. 设置 declare-styleable 与 回调
 */
public class DyzRoundProgressBar extends View {

    private static final String TAG = "RoundProgressBar";
    public static final String DYZ_CUSTOM_ACTION_VOLUME_CAN_FIX = "volume_can_fix_dyz";

    public static final float BAR_ALPHA_MIN = 0.21f;
    private long barAutoCloseTime;

    //画笔
    private Paint mPaint;
    //进度条颜色
    private int fullColor;
    private int progressColor;
    //进度
    private int max;
    private float progress;
    //图形
    private RectF mRectF;
    private float strokeWidth;
    private float mRounds;//圆角幅度
    //触摸
    private float touchX, touchY;
    //动画
    private SoftReference<ViewFadeAnimatorListener> mAnimatorListener;//淡入淡出
    private AnimatorSet mScaleAnimator;
    private long AutoCloseDownTime;
    private boolean isDyzBarChanging, isStartingAnimation;
    //回调
    private OnDyzBarChangeListener mChangeListener;

    public interface OnDyzBarChangeListener {
        void onProgressChanged(DyzRoundProgressBar roundBar, float progress, boolean fromUser);

        void onStartTrackingTouch(DyzRoundProgressBar roundBar);

        void onStopTrackingTouch(DyzRoundProgressBar roundBar);
    }

    public DyzRoundProgressBar(Context context) {
        super(context);
        init(context, null);
        if (mPaint == null) initPaint();
    }

    public DyzRoundProgressBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
        if (mPaint == null) initPaint();
    }

    public DyzRoundProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
        if (mPaint == null) initPaint();
    }

    //注意：安卓子系统获取的控件宽高跟Windows当前的分辨率有关。
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth(), height = getMeasuredHeight();
        Log.d(TAG, "onMeasure: w = " + width + ", h = " + height);

        if (width < height && getRotation() == 0f) {
            setPivotX(width >> 1);
            setPivotY(height);
        }
        if (mRectF == null) {
            mRectF = new RectF(0, 0, width, height);
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Log.d(TAG, "onDraw: w = " + getWidth() + ", h = " + getHeight());

        //绘制圆角矩形，rx，rx共同控制圆角的幅度
        canvas.drawRoundRect(mRectF, mRounds, mRounds, mPaint);
        canvas.save();

        mPaint.setColor(progressColor);
        //翻转180°， translate 和 rotate
        canvas.translate(getWidth(),getHeight());
        canvas.rotate(180);
        //绘制进度的圆角矩形
        canvas.clipRect(0, 0, mRectF.width(), mRectF.height() * progress / max);
        canvas.drawRoundRect(mRectF, mRounds, mRounds, mPaint);
        canvas.restore();

        mPaint.setColor(fullColor);
        //postInvalidateDelayed(100);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
    }

    //!:如果不设置 点击事件 或者 Clickable为True，则只会接收到ACTION_DOWN触摸事件。
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //Log.d(TAG, "dispatchTouchEvent: "+ev.getAction());
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            isDyzBarChanging = true;
            AutoCloseDownTime = System.currentTimeMillis();
            touchX = getVolumePercent(ev.getX());
            touchY = getVolumePercent(ev.getY());
            if (mChangeListener != null) {
                mChangeListener.onStartTrackingTouch(this);
            }
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            AutoCloseDownTime = System.currentTimeMillis();
            //如果在动画执行中则不能更改进度
            if (isStartingAnimation) { return super.dispatchTouchEvent(ev); }

            float x = getVolumePercent(ev.getX()), y = getVolumePercent(ev.getY()),
                    moveX = getVolumePercent(touchX - x),
                    moveY = getVolumePercent(touchY - y),
                    progressN = getVolumePercent(progress + moveY / 2);
            //Log.d(TAG, "dispatchTouchEvent: " + moveX + ", " + moveY);

            setProgress(Math.max(0, Math.min(progressN, max)), true);
            touchX = x;
            touchY = y;
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            touchX = touchY = 0;
            //延迟500ms执行，可有效防止在手指抬起时再次更新音量进度的问题
            new Handler().postDelayed(() -> isDyzBarChanging = false,500);
            if (mChangeListener != null) {
                mChangeListener.onStopTrackingTouch(this);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void init(Context context, AttributeSet attrs){
        if (attrs != null) {
            Log.w(TAG, "读取布局文件参数");
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DyzRoundProgressBar);
            fullColor = ta.getInt(R.styleable.DyzRoundProgressBar_rn_color_bg, 0x20EEEEEE);
            progressColor = ta.getInt(R.styleable.DyzRoundProgressBar_rn_color_progress, 0xCCEEEEEE);
            strokeWidth = ta.getFloat(R.styleable.DyzRoundProgressBar_rn_width_stroke, 10f);
            mRounds = ta.getFloat(R.styleable.DyzRoundProgressBar_rn_round, 26f);
            progress = ta.getFloat(R.styleable.DyzRoundProgressBar_rn_progress, 130f);
            max = ta.getInt(R.styleable.DyzRoundProgressBar_rn_max, 150);
            ta.recycle();
        }else {
            Log.w(TAG, "初始化默认布局文件参数");
            fullColor = 0x20EEEEEE;
            progressColor = 0xCCEEEEEE;
            strokeWidth = 10f;
            mRounds = 26f;
            progress = 130f;
            max = 150;
        }
        setBarAutoCloseTime(1200); //默认
    }

    @SuppressLint("Recycle")
    private void initPaint() {
        mPaint = new Paint();
        mPaint.setColor(fullColor);       //设置画笔的初始颜色
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);  //设置画笔模式为填充
        mPaint.setStrokeWidth(strokeWidth);   //设置画笔宽度
        this.setClickable(true);
        mAnimatorListener = new SoftReference<>(new ViewFadeAnimatorListener());
        //初始化缩放动画
        mScaleAnimator = new AnimatorSet();
        mScaleAnimator.playTogether(
                ObjectAnimator.ofFloat(this, "scaleX", 0f, 1f).setDuration(400),
                ObjectAnimator.ofFloat(this, "scaleY", 0f, 1f).setDuration(400),
                ObjectAnimator.ofFloat(this, "alpha", BAR_ALPHA_MIN, 1f).setDuration(300),
                ObjectAnimator.ofFloat(this,"translationY",0,0 - dpToPx(21)).setDuration(200));
        mScaleAnimator.addListener(getAnimatorListener());
    }

    public void release() {
        clearAnimation();
        if (mScaleAnimator != null) {
            mScaleAnimator.end();
            mScaleAnimator.cancel();
            mScaleAnimator = null;
        }
        if (mChangeListener != null) {
            mChangeListener = null;
        }
        if (mAnimatorListener != null) {
            mAnimatorListener.clear();
            mAnimatorListener = null;
        }
        if (mRectF != null) {
            mRectF = null;
        }
        if (mPaint != null) {
            mPaint.reset();
            mPaint = null;
        }
    }

    /**
     * 更新进度条进度
     *
     * @param progress 进度
     */
    private void setProgress(float progress, boolean isUser) {
        int drawWidth = getWidth(), drawHeight = getHeight();
        if (progress < 0 || max <= 0) {
            Log.e(TAG, "设置进度失败！" + progress + ", 总进度 " + max + ", 绘制宽度 " + drawWidth + ", 绘制高度 " + drawHeight);
            return;
        }else if (drawWidth <= 0 || drawHeight <= 0){
            this.progress = progress;
            return;
        }
        this.progress = progress;
        if (mChangeListener != null) {
            mChangeListener.onProgressChanged(this, progress, isUser);
        }
        //在事件循环的后续循环中导致失效。用此选项可使非UI线程中的视图无效
        //仅当此视图附着到窗口时,此方法可以从UI线程外部调用。</p
        Log.d(TAG, "setProgress: 进度为 " + progress);
        postInvalidate();//绘制刷新
    }

    /**
     * 更新进度条进度
     *
     * @param progress 进度
     */
    public void setProgress(float progress) {
        setProgress(progress, false);
    }

    public float getProgress() {
        return progress;
    }

    public void setMax(int max) {
        this.max = max;
        postInvalidate();
    }

    public int getMax() {
        return max;
    }

    public int getFullColor() {
        return fullColor;
    }

    public void setFullColor(@ColorInt int fullColor) {
        this.fullColor = fullColor;
        postInvalidate();
    }

    public int getProgressColor() {
        return progressColor;
    }

    public void setProgressColor(@ColorInt int progressColor) {
        this.progressColor = progressColor;
        postInvalidate();
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        postInvalidate();
    }

    public float getRounds() {
        return mRounds;
    }

    public void setRounds(float rounds) {
        this.mRounds = rounds;
        postInvalidate();
    }

    public long getBarAutoCloseTime() { return barAutoCloseTime; }

    public void setBarAutoCloseTime(boolean isSubSystem) {
        Log.d(TAG, "setBarAutoCloseTime: 是否是固定音量设备 "+isSubSystem);
        setBarAutoCloseTime(isSubSystem ? 2000 : 1200);
    }
    public void setBarAutoCloseTime(long barAutoCloseTime) {
        this.barAutoCloseTime = barAutoCloseTime;
    }

    public void setChangeListener(OnDyzBarChangeListener changeListener) {
        this.mChangeListener = changeListener;
    }

    public boolean isDyzBarChanging() { return isDyzBarChanging; }

    //将运算后的浮点数float的小数点位控制在两位数！
    public Float getVolumePercent(float x) {
        return Float.parseFloat(String.format(Locale.CHINA, "%.2f", x));
    }

    private class ViewFadeAnimatorListener extends AnimatorListenerAdapter {
        @Override
        public void onAnimationEnd(Animator animation) {
            Log.d(TAG, "onAnimationEnd: " + getAlpha());
            int visible = getAlpha() == BAR_ALPHA_MIN ? GONE : VISIBLE;
            setVisibility(visible);
            isStartingAnimation = false;
        }

        @Override
        public void onAnimationStart(Animator animation, boolean isReverse) {
            Log.d(TAG, "onAnimationStart: isReverse "+isReverse);
            isStartingAnimation = true;
        }
    }

    private ViewFadeAnimatorListener getAnimatorListener() {
        return mAnimatorListener == null ? null : mAnimatorListener.get();
    }

    public void startFadeAnimator() {
        if (getVisibility() == GONE){
            this.setVisibility(VISIBLE);

            mScaleAnimator.start();
            AutoCloseDownTime = System.currentTimeMillis();

        }else {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mScaleAnimator.reverse();

            }else {
                setVisibility(GONE);
                isStartingAnimation = false;
            }
        }
    }

    public long getAutoCloseDownTime() { return AutoCloseDownTime; }

    public float dpToPx(int dp){
        return TypedValue.applyDimension(
                                        TypedValue.COMPLEX_UNIT_DIP,
                                        dp,
                                        getResources().getDisplayMetrics());
    }
}