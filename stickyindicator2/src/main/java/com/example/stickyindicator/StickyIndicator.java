package com.example.stickyindicator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * 创建时间:2018-7-21
 */

public class StickyIndicator extends View{
    private final String Log = "StickyIndicator";

    /**
     * 圆点风格,未选的圆点是实心的
     */
    private final static int STYLE_POINT_FILL = 0;

    /**
     * 圆点风格,未选的圆点是空心的
     */
    private final static int STYLE_POINT_STROKE = 1;

    /**
     * 长条风格
     */
    private final static int STYLE_STRIP = 2;

    /**
     * 当前圆点/长条弹出时间
     */
    private final int DURATION_POINT = 300;

    /**
     * 当前圆点/长条弹回时间,
     */
    private final int DURATION_STICKY = 300;

    /**
     * 当前长条恢复为原来长度的时间
     */
    private final int DURATION_REBUILD = 300;

    /**
     * 闪光效果的时间
     */
    private final int DURATION_FLASH = 300;

    private int mColor;
    private int mBackColor;
    private int mCount;
    private float mInterval;
    private float mRadius;
    private boolean allowAnim;
    private int mStyle;
    private float mStripWidth;
    private float mStripHeight;
    private boolean hideFlash;
    private boolean hideBack;
    private boolean isIndicatorClickable;

    private float mFlashRadius;

    private int mCurrentIndex;
    private int mLastIndex;

    private Paint mBackPaint;
    private Paint mPaint;
    private Paint mFlashPaint;

    private PointF mPoint;
    private PointF mStickyPoint;

    private ValueAnimator mPointAnimator = ValueAnimator.ofFloat();
    private ValueAnimator mStickyAnimator = ValueAnimator.ofFloat();
    private ValueAnimator mRebuildAnimator = ValueAnimator.ofFloat();
    private ValueAnimator mFlashAnimator = ValueAnimator.ofFloat();

    private ValueAnimator.AnimatorUpdateListener mPointAnimatorLinstener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mPoint.x = (float) animation.getAnimatedValue();
            invalidate();
        }
    };

    private ValueAnimator.AnimatorListener mPointEndListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            //弹出动画结束后,开始播放弹回动画
            runStickyAnim();
        }
    };

    private ValueAnimator.AnimatorUpdateListener mStickyAnimatorLinstener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mStickyPoint.x = (float) animation.getAnimatedValue();
        }
    };

    private ValueAnimator.AnimatorListener mStickyEndListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            if(mStyle == STYLE_STRIP){
                //长条到达目的地后,开始播放恢复原状长度的动画
                float startX = mPoint.x;
                mRebuildAnimator.setFloatValues(startX,startX - mStripWidth / 2);
                mRebuildAnimator.start();
            }
        }
    };

    private ValueAnimator.AnimatorUpdateListener mRebuildAnimatorLinstener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mPoint.x = (float) animation.getAnimatedValue();
            //+1是为了避免Canvas.drawLine()绘制一条长度为0的线段
            float currentStripWidth = (mStripWidth / 2 + getCurrentPointX() - mPoint.x ) * 2 + 1;
            mStickyPoint.x = mPoint.x + currentStripWidth;
            invalidate();
        }
    };

    private ValueAnimator.AnimatorUpdateListener mFlashAnimatorLinstener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            //通过改变圆环的半径和厚度来实现闪光的效果
            float value = (float) animation.getAnimatedValue();
            if(mStyle == STYLE_STRIP){
                if(value < mStripHeight / 2 * 1.7f){
                    mFlashRadius = value;
                }else if(value == mStripHeight){
                    mFlashRadius = 0;
                }
                mFlashPaint.setStrokeWidth(mStripHeight / 2 * 1.5f - value * 0.75f);
            }else{
                if(value < mRadius * 1.7f){
                    mFlashRadius = value;
                }else if(value == mRadius * 2f){
                    mFlashRadius = 0;
                }
                mFlashPaint.setStrokeWidth(mRadius * 1.5f - value * 0.75f);
            }
            invalidate();
        }
    };


    private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        private float mLastPositionOffset = 0;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if(!mStickyAnimator.isRunning() && allowAnim){
                boolean rightScroll = (positionOffset - mLastPositionOffset) >= 0;

                if(rightScroll){
                    mPoint.x = getPostionX(position) + positionOffset * (mInterval + mStripWidth);
                }else{
                    mPoint.x = getPostionX(position) - positionOffset * (mInterval + mStripWidth);
                }
                invalidate();
            }
        }

        @Override
        public void onPageSelected(int position) {
            if(mCurrentIndex != position){
                setPosition(position);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    private Context mContext;

    private IndicatorListener mIndicatorListener;

    public StickyIndicator(Context context) {
        this(context,null);
    }

    public StickyIndicator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StickyIndicator);
        mColor = typedArray.getColor(R.styleable.StickyIndicator_color, Color.RED);
        mBackColor = typedArray.getColor(R.styleable.StickyIndicator_backColor,Color.LTGRAY);
        mCount = typedArray.getInteger(R.styleable.StickyIndicator_count,1);
        mRadius = typedArray.getDimension(R.styleable.StickyIndicator_radius,ScreenUtil.dipTopx(mContext,10));
        mInterval = typedArray.getDimension(R.styleable.StickyIndicator_interval,mRadius * 2);
        allowAnim = typedArray.getBoolean(R.styleable.StickyIndicator_allowAnim,true);
        mStyle = typedArray.getInt(R.styleable.StickyIndicator_style, STYLE_POINT_FILL);
        mStripWidth = typedArray.getDimension(R.styleable.StickyIndicator_stripWidth,ScreenUtil.dipTopx(mContext,30));
        mStripHeight = typedArray.getDimension(R.styleable.StickyIndicator_stripHeight,10);
        hideFlash = typedArray.getBoolean(R.styleable.StickyIndicator_hideFlash,false);
        hideBack = typedArray.getBoolean(R.styleable.StickyIndicator_hideBack,false);
        mCurrentIndex = typedArray.getInt(R.styleable.StickyIndicator_currentIndex,0);
        isIndicatorClickable = typedArray.getBoolean(R.styleable.StickyIndicator_indicatorClickable,false);
        typedArray.recycle();

        mBackPaint = new Paint();
        mBackPaint.setAlpha(250);
        mBackPaint.setAntiAlias(true);
        mBackPaint.setStrokeCap(Paint.Cap.ROUND);
        setBackColor(mBackColor);

        mPaint = new Paint();
        mPaint.setAlpha(250);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        setColor(mColor);

        mFlashPaint = new Paint();
        mFlashPaint.setColor(250);
        mFlashPaint.setAntiAlias(true);
        mFlashPaint.setColor(Color.WHITE);
        mFlashPaint.setStyle(Paint.Style.STROKE);

        mPoint = new PointF();
        mStickyPoint = new PointF();

        mPointAnimator.addUpdateListener(mPointAnimatorLinstener);
        mPointAnimator.addListener(mPointEndListener);

        mFlashAnimator.addUpdateListener(mFlashAnimatorLinstener);

        mStickyAnimator.addUpdateListener(mStickyAnimatorLinstener);
        mStickyAnimator.addListener(mStickyEndListener);

        mRebuildAnimator.addUpdateListener(mRebuildAnimatorLinstener);

        allowAnim(allowAnim);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if(mStyle == STYLE_STRIP){
            onMeasureStrip(widthMeasureSpec,heightMeasureSpec);
        }else{
            onMeasurePoint(widthMeasureSpec,heightMeasureSpec);
        }
    }

    private void onMeasurePoint(int widthMeasureSpec, int heightMeasureSpec){
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if(widthMode == MeasureSpec.AT_MOST){
            width = (int) (mCount * (mRadius * 2 + mInterval));
        }else{
            if(mRadius * 2 * mCount > width){
                mRadius = width / mCount / 2;
            }
            mInterval = width / mCount - mRadius * 2;
        }

        if(heightMode == MeasureSpec.AT_MOST){
            height = (int) (mRadius * 2 * 1.5f);
        }else{
            if(height < mRadius * 2 * 1.5f){
                height = (int) (mRadius * 2 * 1.5f);
            }
        }

        mPoint.x = getCurrentPointX();
        mPoint.y = height / 2;

        mStickyPoint.x = mPoint.x;
        mStickyPoint.y = height / 2;

        if(mStyle == STYLE_POINT_STROKE){
            mBackPaint.setStyle(Paint.Style.STROKE);
            mBackPaint.setStrokeWidth(mRadius * 0.3f);
        }

        setMeasuredDimension(width,height);
    }

    private void onMeasureStrip(int widthMeasureSpec, int heightMeasureSpec){
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if(widthMode == MeasureSpec.AT_MOST){
            width = (int) ((mStripWidth + mInterval) * mCount);
        }else{
            if(mStripWidth > width / mCount){
                mStripWidth = width / mCount;
            }
            mInterval = width / mCount - mStripWidth;
        }

        if(heightMode == MeasureSpec.AT_MOST){
            height = (int) (mStripHeight * 1.5f);
        }else{
            if(height < mStripHeight * 1.5f){
                height = (int) (mStripHeight * 1.5f);
            }
        }

        mPoint.x = getCurrentPointX();
        mPoint.y = height / 2;

        mStickyPoint.x = mPoint.x + mStripWidth;
        mStickyPoint.y = height / 2;

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStripHeight);
        mBackPaint.setStrokeWidth(mStripHeight);

        setMeasuredDimension(width,height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(mStyle == STYLE_STRIP){
            drawOnStripStyle(canvas);
        }else{
            drawOnPointStyle(canvas);
        }
    }

    private void drawOnPointStyle(Canvas canvas){
        if(!hideBack) {
            onDrawPointBack(canvas);
        }

        if(!hideFlash) {
            onDrawPointFlash(canvas);
        }

        onDrawPoint(canvas);
    }

    private void onDrawPointBack(Canvas canvas){
        int x;
        final int y = getHeight() / 2;
        for(int i = 0;i < mCount;i++){
            x = (int) (mInterval / 2 + mRadius + i * (mInterval + mRadius * 2));
            canvas.drawCircle(x,y,mRadius,mBackPaint);
        }
    }

    private void onDrawPoint(Canvas canvas){
        canvas.drawCircle(mPoint.x,mPoint.y,mRadius,mPaint);
        canvas.drawCircle(mStickyPoint.x,mStickyPoint.y,mRadius,mPaint);

        Path path = new Path();
        path.reset();
        path.moveTo(mStickyPoint.x,mStickyPoint.y - mRadius);
        path.quadTo(mStickyPoint.x + (mPoint.x - mStickyPoint.x) / 2,
                mStickyPoint.y,
                mPoint.x,
                mPoint.y - mRadius);
        path.lineTo(mPoint.x,mStickyPoint.y + mRadius);
        path.quadTo(mStickyPoint.x + (mPoint.x - mStickyPoint.x) / 2,
                mStickyPoint.y,
                mStickyPoint.x,
                mStickyPoint.y + mRadius);
        path.lineTo(mStickyPoint.x,mStickyPoint.y - mRadius);
        canvas.drawPath(path,mPaint);
    }

    private void onDrawPointFlash(Canvas canvas){
        canvas.drawCircle(getLastPointX(),getHeight() / 2,mFlashRadius,mFlashPaint);
    }

    private void drawOnStripStyle(Canvas canvas){
        if(!hideBack) {
            onDrawStripBack(canvas);
        }

        if(!hideFlash) {
            onDrawStripFlash(canvas);
        }

        onDrawStrip(canvas);
    }

    private void onDrawStripBack(Canvas canvas){
        int x;
        final int y = getHeight() / 2;
        for(int i = 0;i < mCount;i++){
            x = (int) (mInterval / 2 + i * (mStripWidth + mInterval));
            canvas.drawLine(x,y,x + mStripWidth,y,mBackPaint);
        }
    }

    private void onDrawStrip(Canvas canvas){
        canvas.drawLine(mPoint.x,mPoint.y,mStickyPoint.x,mStickyPoint.y,mPaint);
    }

    private void onDrawStripFlash(Canvas canvas){
        canvas.drawCircle(getLastPointX() + mStripWidth,getHeight() / 2,mFlashRadius,mFlashPaint);
    }

    private int downOnPosition;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!isIndicatorClickable){
            return super.onTouchEvent(event);
        }

        float eventX = event.getX();
        float eventY = event.getY();

        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                downOnPosition = pointerOnPosition(eventX,eventY);
                if(downOnPosition == -1){
                    return false;
                }
                break;

            case MotionEvent.ACTION_UP:
                if(pointerOnPosition(eventX,eventY) == downOnPosition &&
                        mIndicatorListener != null){
                    mIndicatorListener.onIndicatorClicked(downOnPosition);
                }
                return true;
        }

        return super.onTouchEvent(event);
    }

    private int pointerOnPosition(float x, float y){
        final int dp_1 = ScreenUtil.dipTopx(mContext,1);

        if(mStyle == STYLE_STRIP){
            for(int i = 0;i < mCount;i++){
                final float startX = getPostionX(i);
                final float centerY = getHeight() / 2;
                //dp_1用于增加2dp点击范围
                if(x >= startX - dp_1 && x <= startX + mStripWidth + dp_1 &&
                        y >= centerY - mStripHeight / 2 - dp_1 && y <= centerY + mStripHeight / 2 + dp_1){
                    return i;
                }
            }
        }else{
            for(int i = 0;i < mCount;i++){
                final float centerX = getPostionX(i);
                final float centerY = getHeight() / 2;
                //dp_1用于增加2dp点击范围
                if(x >= centerX - mRadius - dp_1 && x <= centerX + mRadius + dp_1 &&
                        y >= centerY - mRadius - dp_1 && y <= centerY + mRadius + dp_1){
                    return i;
                }
            }
        }

        return -1;
    }

    private void runAnim(){
        if(mStyle == STYLE_STRIP){
            mPointAnimator.setFloatValues(mPoint.x,getCurrentPointX() + mStripWidth / 2);
        }else{
            mPointAnimator.setFloatValues(mPoint.x,getCurrentPointX());
        }

        //提前结束上一次的弹回动画和恢复动画,避免与本次的动画冲突
        if(mStickyAnimator.isRunning()){
            mStickyAnimator.end();
        }

        if(mRebuildAnimator.isRunning()){
            mRebuildAnimator.end();
        }

        mPointAnimator.start();
    }

    private void runStickyAnim(){
        //提前结束上一次的恢复动画,避免与本次的动画冲突
        if(mRebuildAnimator.isRunning()){
            mRebuildAnimator.end();
        }

        mStickyAnimator.setFloatValues(mStickyPoint.x,mPoint.x + 1);
        mStickyAnimator.start();

        float endValue;
        if(mStyle == STYLE_STRIP)
            endValue = mStripHeight;
        else
            endValue = mRadius * 2;

        mFlashAnimator.setFloatValues(0,endValue);
        mFlashAnimator.start();
    }

    /**
     * 若为圆点风格,则获取当前圆点圆心的X坐标
     * 若为长条风格,在获取当前长条的起始X坐标
     * @return
     */
    private float getCurrentPointX(){
        return getPostionX(mCurrentIndex);
    }

    /**
     * 若为圆点风格,则获取指定位置圆点圆心的X坐标
     * 若为长条风格,在获取指定位置长条的起始X坐标
     * @param index
     * @return
     */
    private float getPostionX(int index){
        if(index > mCount - 1 || index < 0)
            return -1;

        float destX;
        if(mStyle == STYLE_STRIP){
            destX = mInterval / 2 + index * (mStripWidth + mInterval);
        }else{
            destX = mInterval / 2 + mRadius + index * (mRadius * 2 + mInterval);
        }
        return destX;
    }

    private float getLastPointX(){
        return getPostionX(mLastIndex);
    }

    /**
     * 设置当前圆点/长条的位置
     * @param newPosition
     */
    public void setPosition(int newPosition){
        if(newPosition >= mCount){
            newPosition = mCount - 1;
        }

        if(newPosition < 0){
            newPosition = 0;
        }

        mLastIndex = mCurrentIndex;
        mCurrentIndex = newPosition;

        if(mIndicatorListener != null){
            mIndicatorListener.onIndicatorSelected(newPosition);
        }

        runAnim();
    }

    /**
     * 获取当前圆点/长条的位置
     * @return
     */
    public int getCurrentPosition(){
        return mCurrentIndex;
    }

    /**
     * 是否隐藏未选的圆点/长条
     * @param hide
     */
    public void hideBack(boolean hide){
        hideBack = hide;
    }

    /**
     * 是否隐藏闪光效果
     * @param hide
     */
    public void hideFlash(boolean hide){
        hideFlash = hide;
    }

    /**
     * 是否播放弹性动画
     * @param allow
     */
    public void allowAnim(boolean allow){
        allowAnim = allow;
        setAnimDuration(allow);
    }

    private void setAnimDuration(boolean allowAnim){
        if(allowAnim){
            mPointAnimator.setDuration(DURATION_POINT);
            mStickyAnimator.setDuration(DURATION_STICKY);
            mRebuildAnimator.setDuration(DURATION_REBUILD);
            mFlashAnimator.setDuration(DURATION_FLASH);
        }else{
            mPointAnimator.setDuration(0);
            mStickyAnimator.setDuration(0);
            mRebuildAnimator.setDuration(0);
            mFlashAnimator.setDuration(0);
        }
    }

    /**
     * 设置当前圆点/长条的颜色
     * @param color
     */
    public void setColor(int color){
        mColor = color;
        mPaint.setColor(color);
    }

    /**
     * 设置未选的圆点/长条的颜色
     * @param color
     */
    public void setBackColor(int color){
        mBackColor = color;
        mBackPaint.setColor(color);
    }

    /**
     * 设置指示器点击监听器
     * @param listener
     */
    public void setOnIndicatorClickedListener(IndicatorListener listener){
        setIndicatorClickable(true);
        mIndicatorListener = listener;
    }

    /**
     * 指示器是否可以点击
     * @param clickable
     */
    public void setIndicatorClickable(boolean clickable){
        setClickable(clickable);
        isIndicatorClickable = clickable;
    }

    /**
     * 添加圆点/长条
     * @param num
     */
    public void addIndicator(int num){
        mCount += num;
        requestLayout();
    }

    /**
     * 移除圆点/长条
     * @param num
     */
    public void removeIndicator(int num){
        mCount -= num;
        if(mCount < 0){
            throw new IllegalArgumentException("indicator count < 0");
        }

        if(mCurrentIndex >= mCount){
            mCurrentIndex = mCount - 1;
        }

        requestLayout();
    }

    /**
     * 获取页面监听器,可以监听ViewPager状态,并与页面翻动同步,需要设置{@link #allowAnim}为true
     * 否则不会有任何动画效果
     * @see #allowAnim(boolean)
     * @return
     */
    public ViewPager.OnPageChangeListener getOnPageChangeListener(){
        return mOnPageChangeListener;
    }

    /**
     * 指示器点击监听器,用于监听圆点/长条的点击事件
     */
    public interface IndicatorListener {
        /**
         * 圆点/长条点击处理
         * @param position
         */
        void onIndicatorClicked(int position);

        /**
         * 选中时的处理
         * @param position
         */
        void onIndicatorSelected(int position);
    }
}
