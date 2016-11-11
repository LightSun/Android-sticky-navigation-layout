package com.heaven7.android.StickyLayout;


import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.OverScroller;

import com.heaven7.core.util.Logger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * sticky navigation layout：similar to google+ app.
 * <p>
 * Note: @attr ref android.R.styleable#stickyLayout_content_id the content view must be the direct child of StickyNavigationLayout.
 * or else may cause bug.
 * </p>
 *
 * @author heaven7
 * @attr ref com.heaven7.android.sticky_navigation_layout.demo.R.styleable#stickyLayout_content_id
 */
public class StickyNavigationLayout extends LinearLayout implements NestedScrollingParent , NestedScrollingChild{

    private static final String TAG = "StickyNavLayout";

    private static final boolean DEBUG                 = true;

    private static final long ANIMATED_SCROLL_GAP = 250;

    /**
     * The view is not currently scrolling.
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_IDLE = 0;

    /**
     * The view is currently being dragged by outside input such as user touch input.
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_DRAGGING = 1;

    /**
     * The view is currently animating to a final position while not under
     * outside control.
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_SETTLING = 2;

    /**
     * the view state is shown.
     */
    public static final int VIEW_STATE_SHOW = 1;
    /**
     * the view state is hide
     */
    public static final int VIEW_STATE_HIDE = 2;

    /**
     * the top view
     */
    private View mTop;
    /**
     * the navigation view
     */
    private View mIndicator;
    /**
     * the child view which will be intercept
     */
    private View mContentView;
    private  int mTopViewId;
    private  int mIndicatorId;
    private  int mContentId;

    private int mTopViewHeight;
    private boolean mTopHide = false;

    private GroupStickyDelegate mGroupStickyDelegate;
    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mMaximumVelocity, mMinimumVelocity;

    private int mLastTouchY, mLastTouchX;
    private int mInitialTouchY, mInitialTouchX;
    private boolean mDragging;

    private OnScrollChangeListener mScrollListener;

    private boolean mNeedIntercept;

    private int mScrollState = SCROLL_STATE_IDLE;

    private int mFocusDir;
    private boolean mEnableStickyTouch = true;
    private int mTotalDy;

    /**
     * last scroll time.
     */
    private long mLastScroll;
    /**
     * auto fit the sticky scroll
     */
    private final boolean mAutoFitScroll;
    /**
     * the percent of auto fix.
     */
    private float mAutoFitPercent = 0.5f;
    /**
     * code set the sticky view ,not from xml.
     */
    private boolean mCodeSet;

    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;

    private int mTotalUnconsumed ;
    private int[] mParentScrollConsumed = new int[2];
    private boolean mNestedScrollInProgress;
    private int[] mParentOffsetInWindow = new int[2];
    private int[] mScrollConsumed = new int[2];
    private int[] mNestedOffsets = new int[2];
    private int[] mScrollOffset = new int[2];

    public StickyNavigationLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        //setOrientation(LinearLayout.VERTICAL);
        mGroupStickyDelegate = new GroupStickyDelegate();
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);

        mScroller = new OverScroller(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();//触摸阙值
        mMaximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StickyNavigationLayout);
        // mCodeSet will set stick view from onFinishInflate.

        if(!mCodeSet) {
            mTopViewId = a.getResourceId(R.styleable.StickyNavigationLayout_stickyLayout_top_id, 0);
            mIndicatorId = a.getResourceId(R.styleable.StickyNavigationLayout_stickyLayout_indicator_id, 0);
            mContentId = a.getResourceId(R.styleable.StickyNavigationLayout_stickyLayout_content_id, 0);
        }
        mAutoFitScroll = a.getBoolean(R.styleable.StickyNavigationLayout_stickyLayout_auto_fit_scroll, false);
        mAutoFitPercent = a.getFloat(R.styleable.StickyNavigationLayout_stickyLayout_threshold_percent, 0.5f);
        a.recycle();

        //getWindowVisibleDisplayFrame(mExpectTopRect);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTop = findViewById(mTopViewId);
        mIndicator = findViewById(mIndicatorId);
        mContentView = findViewById(mContentId);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mGroupStickyDelegate.afterOnMeasure(this, mTop, mIndicator, mContentView);
        if (mEnableStickyTouch && mContentView != null && mIndicator != null) {
            // 设置view的高度 (将mViewPager。的高度设置为  整个 Height - 导航的高度) - 被拦截的child view
            ViewGroup.LayoutParams params = mContentView.getLayoutParams();
            params.height = getMeasuredHeight() - mIndicator.getMeasuredHeight();
            if (DEBUG) {
                Logger.i(TAG, "onMeasure", "height = " + params.height + ", snv height = " + getMeasuredHeight());
                Logger.i(TAG, "onMeasure", "---> snv  bottom= " + getBottom());
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (DEBUG) {
            Logger.i(TAG, "onLayout");
        }
        if (mEnableStickyTouch && mTop != null) {
            mTopViewHeight = mTop.getMeasuredHeight();
            final ViewGroup.LayoutParams lp = mTop.getLayoutParams();
            if (lp instanceof MarginLayoutParams) {
                mTopViewHeight += ((MarginLayoutParams) lp).topMargin + ((MarginLayoutParams) lp).bottomMargin;
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (DEBUG) {
            Logger.i(TAG, "onSizeChanged");
        }
    }

    /**
     * Return the current scrolling state of the RecyclerView.
     *
     * @return {@link #SCROLL_STATE_IDLE}, {@link #SCROLL_STATE_DRAGGING} or
     * {@link #SCROLL_STATE_SETTLING}
     */
    public int getScrollState() {
        return mScrollState;
    }
    /**
     * set if enable the sticky touch
     * @param enable true to enable, false to disable.
     */
    public void setEnableStickyTouch(boolean enable) {
        if (mEnableStickyTouch != enable) {
            this.mEnableStickyTouch = enable;
            requestLayout();
        }
    }
    /**
     * is the sticky touch enabled.
     * @return true to enable.
     */
    public boolean isStickyTouchEnabled(){
        return mEnableStickyTouch;
    }

    public int getTopViewState(){
        return mTopHide ? VIEW_STATE_HIDE : VIEW_STATE_SHOW;
    }

    /**
     * set the sticky views
     *
     * @param top       the top view
     * @param indicator the indicator view
     * @param content     the content view
     */
    /*public*/ void setStickyViews(View top, View indicator, View content) {
        if (top == null || indicator == null || content == null) {
            throw new NullPointerException();
        }
        mTopViewId = top.getId();
        mIndicatorId = indicator.getId();
        mContentId = content.getId();
        mTop = top;
        mIndicator = indicator;
        mContentView = content;
        mCodeSet = true;
        requestLayout();
    }


    /**
     * use {@link #addStickyDelegate(IStickyDelegate)} instead.
     * set the sticky delegate.
     * @param delegate the delegate
     */
    @Deprecated
    public void setStickyDelegate(IStickyDelegate delegate) {
        mGroupStickyDelegate.addStickyDelegate(delegate);
    }

    /**
     * add a sticky delegate
     * @param delegate sticky delegate.
     */
    public void addStickyDelegate(IStickyDelegate delegate){
        mGroupStickyDelegate.addStickyDelegate(delegate);
    }
    /**
     * remove a sticky delegate
     * @param delegate sticky delegate.
     */
    public void removeStickyDelegate(IStickyDelegate delegate){
        mGroupStickyDelegate.removeStickyDelegate(delegate);
    }

    /**
     * set the OnScrollChangeListener
     *
     * @param l the listener
     */
    public void setOnScrollChangeListener(OnScrollChangeListener l) {
        this.mScrollListener = l;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mEnableStickyTouch) {
            return super.onInterceptTouchEvent(event);
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        int action = event.getAction();
        int y = (int) (event.getY() + 0.5f);
        int x = (int) (event.getY() + 0.5f);

        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsets[0] = mNestedOffsets[1] = 0;
        }
        event.offsetLocation(mNestedOffsets[0], mNestedOffsets[1]);

        switch (action) {

            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished())
                    mScroller.abortAnimation();
               // mVelocityTracker.addMovement(event);
                mInitialTouchX = mLastTouchX = x;
                mInitialTouchY = mLastTouchY = y;
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                return true;

            case MotionEvent.ACTION_MOVE:
                //遵循Google规范(比如recyclerView源代码)。避免处理嵌套滑动出问题。
                int dx = mLastTouchX - x;
                int dy = mLastTouchY - y;

                if(dispatchNestedPreScroll(dx, dy, mScrollConsumed, mScrollOffset)){
                    dx -= mScrollConsumed[0];
                    dy -= mScrollConsumed[1];
                    event.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
                    // Updated the nested offsets
                    mNestedOffsets[0] += mScrollOffset[0];
                    mNestedOffsets[1] += mScrollOffset[1];
                }

                if (!mDragging && Math.abs(dy) > mTouchSlop /*&& Math.abs(dy) > Math.abs(dx)*/ ) {
                    mDragging = true;
                }
                if (mDragging) {
                    mFocusDir = dy < 0 ? View.FOCUS_DOWN : View.FOCUS_UP;
                    mLastTouchX = x - mScrollOffset[0];
                    mLastTouchY = y - mScrollOffset[1];
                    //手向下滑动， dy >0 否则 <0.
                    if(scrollByInternal(0, dy, event)){
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                cancelTouch();
                break;

            case MotionEvent.ACTION_UP:

                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                final float yvel = (int) mVelocityTracker.getYVelocity();
                final float xvel = (int) mVelocityTracker.getXVelocity();
               // final float xvel = -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mScrollPointerId) : 0;
                if (!((xvel != 0 || yvel != 0) && fling((int) xvel, (int) yvel))) {
                    setScrollState(SCROLL_STATE_IDLE);
                }
                resetTouch();
                break;
        }
        return mDragging;
    }

    private void cancelTouch() {
      /*  if (!mScroller.isFinished()) {   //如果手离开了,就终止滑动
            mScroller.abortAnimation();
        }*/
        resetTouch();
        setScrollState(SCROLL_STATE_IDLE);
    }

    private void resetTouch() {
        mDragging = false;
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
        stopNestedScroll();
       // releaseGlows();
    }
    /**
     * Begin a standard fling with an initial velocity along each axis in pixels per second.
     * If the velocity given is below the system-defined minimum this method will return false
     * and no fling will occur.
     *
     * @param velocityX Initial horizontal velocity in pixels per second
     * @param velocityY Initial vertical velocity in pixels per second
     * @return true if the fling was started, false if the velocity was too low to fling or
     * LayoutManager does not support scrolling in the axis fling is issued.
     *
     */
    public boolean fling(int velocityX, int velocityY) {
        //TODO
        return false;
    }
    void setScrollState(int state) {
        if (state == mScrollState) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "setting scroll state to " + state + " from " + mScrollState,
                    new Exception());
        }
        mScrollState = state;
        if (state != SCROLL_STATE_SETTLING) {
           // stopScrollersInternal();
        }
       //TODO dispatchOnScrollStateChanged(state);
    }
    /**
     * Does not perform bounds checking. Used by internal methods that have already validated input.
     * <p>
     * It also reports any unused scroll request to the related EdgeEffect.
     *
     * @param dx The amount of horizontal scroll request
     * @param dy The amount of vertical scroll request
     * @param ev The originating MotionEvent, or null if not from a touch event.
     *
     * @return Whether any scroll was consumed in either direction.
     */
    boolean scrollByInternal(int dx, int dy, MotionEvent ev) {
        //TODO
        mScrollConsumed[0] = 0;
        mScrollConsumed[1] = 0;
        scrollInternal(dx, dy, mScrollConsumed);

        int unconsumedX = dx - mScrollConsumed[0];
        int unconsumedY = dy - mScrollConsumed[1];

        if (dispatchNestedScroll(mScrollConsumed[0], mScrollConsumed[1], unconsumedX, unconsumedY, mScrollOffset)) {
            // Update the last touch co-ords, taking any scroll offset into account
            mLastTouchX -= mScrollOffset[0];
            mLastTouchY -= mScrollOffset[1];
            if (ev != null) {
                ev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
            }
            mNestedOffsets[0] += mScrollOffset[0];
            mNestedOffsets[1] += mScrollOffset[1];
        }
        //TODO dispatch on scroll ?
        if (!awakenScrollBars()) {
            invalidate();
        }
        return mScrollConsumed[0] != 0 || mScrollConsumed[1] != 0;
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     */
    public final void smoothScrollTo(int x, int y) {
        smoothScrollBy(x - getScrollX(), y - getScrollY());
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     */
    public final void smoothScrollBy(int dx, int dy) {
        if (getChildCount() == 0) {
            // Nothing to do.
            return;
        }
        long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
        if (duration > ANIMATED_SCROLL_GAP) {
            // design from scrollView
            mScroller.startScroll(getScrollX(), getScrollY(), dx, dy);
            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            scrollBy(dx, dy);
        }
        mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }

    /**
     * get the focus direction . 0 or  {@link View#FOCUS_DOWN} or {@link View#FOCUS_UP}
     *
     * @return the focus direction
     */
    public int getFocusDirection() {
        return mFocusDir;
    }

    /**
     * called pre ths scroll distance change. may be negative .
     *
     * @param dx      the delta x between this touch and last touch
     * @param dy      the delta y between this touch and last touch
     * @param totalDx the delta x between this touch and first touch
     * @param totalDy the delta y between this touch and first touch
     */
    private void onPreScrollDistanceChange(int dx, int dy, int totalDx, int totalDy) {
        if (mScrollListener != null) {
            mScrollListener.onPreScrollDistanceChange(this, dx, dy, totalDx, totalDy);
        } else {
            if (getContext() instanceof OnScrollChangeListener) {
                ((OnScrollChangeListener) getContext()).onPreScrollDistanceChange(this, dx, dy, totalDx, totalDy);
            }
        }
    }

    /**
     * called after ths scroll distance change. may be negative .
     *
     * @param dx      the delta x between this touch and last touch
     * @param dy      the delta y between this touch and last touch
     * @param totalDx the delta x between this touch and first touch
     * @param totalDy the delta y between this touch and first touch
     */
    private void onAfterScrollDistanceChange(int dx, int dy, int totalDx, int totalDy) {
        if (mScrollListener != null) {
            mScrollListener.onAfterScrollDistanceChange(this, dx, dy, totalDx, totalDy);
        } else {
            if (getContext() instanceof OnScrollChangeListener) {
                ((OnScrollChangeListener) getContext()).onAfterScrollDistanceChange(this, dx, dy, totalDx, totalDy);
            }
        }
    }

    private static String getStateString(int state) {
        switch (state) {
            case SCROLL_STATE_DRAGGING:
                return "SCROLL_STATE_DRAGGING";

            case SCROLL_STATE_SETTLING:
                return "SCROLL_STATE_SETTLING";

            case SCROLL_STATE_IDLE:
            default:
                return "SCROLL_STATE_IDLE";
        }
    }

    public void fling(int velocityY) {
        //使得当前对象只滑动到mTopViewHeight
        mScroller.fling(0, getScrollY(), 0, velocityY, 0, 0, 0, mTopViewHeight);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override //限定滑动的y范围
    public void scrollTo(int x, int y) {
        // Logger.i(TAG, "scrollTo", "x = " + x + ", y = " + y);
        if (y < 0) {
            y = 0;
        }
        //maxY =  mTopViewHeight
        if (y > mTopViewHeight) {
            y = mTopViewHeight;
        }
        if (y == 0 || y == mTopViewHeight) {
            mNeedIntercept = false;
        } else {
            mNeedIntercept = true;
        }
        super.scrollTo(x, y);
        mTopHide = getScrollY() == mTopViewHeight;
    }

    @Override
    public void computeScroll() {
        //super.computeScroll();
        if (mScroller.computeScrollOffset()) {//true滑动尚未完成
            scrollTo(0, mScroller.getCurrY());
            //  invalidate();
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final SaveState saveState = new SaveState(super.onSaveInstanceState());
        saveState.mNeedIntercept = this.mNeedIntercept;
        saveState.mTopHide = this.mTopHide;
        saveState.mLastScroll = this.mLastScroll;
        saveState.mEnableStickyTouch = this.mEnableStickyTouch;
        saveState.mCodeSet = this.mCodeSet;
        saveState.mTopViewId = this.mTopViewId;
        saveState.mIndicatorId = this.mIndicatorId;
        saveState.mContentId = this.mContentId;
        return saveState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        if (state != null) {
            SaveState ss = (SaveState) state;
            this.mNeedIntercept = ss.mNeedIntercept;
            this.mTopHide = ss.mTopHide;
            this.mEnableStickyTouch = ss.mEnableStickyTouch;
            this.mLastScroll = ss.mLastScroll;
            this.mCodeSet = ss.mCodeSet ;
            this.mTopViewId = ss.mTopViewId ;
            this.mIndicatorId = ss.mIndicatorId;
            this.mContentId =  ss.mContentId ;
        }
    }
    protected static class SaveState extends BaseSavedState {

        boolean mNeedIntercept;
        boolean mTopHide;
        boolean mEnableStickyTouch;
        long mLastScroll;

        boolean mCodeSet;
         int mTopViewId;
         int mIndicatorId;
         int mContentId;

        public SaveState(Parcel source) {
            super(source);
            mNeedIntercept = source.readByte() == 1;
            mTopHide = source.readByte() == 1;
            mEnableStickyTouch = source.readByte() == 1;
            mLastScroll = source.readLong();

            mCodeSet = source.readByte() == 1;
            mTopViewId = source.readInt();
            mIndicatorId = source.readInt();
            mContentId = source.readInt();
        }

        public SaveState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte((byte) (mNeedIntercept ? 1 : 0));
            out.writeByte((byte) (mTopHide ? 1 : 0));
            out.writeByte((byte) (mEnableStickyTouch ? 1 : 0));
            out.writeLong(mLastScroll);

            out.writeByte((byte) (mCodeSet ? 1 : 0));
            out.writeInt(mTopViewId);
            out.writeInt(mIndicatorId);
            out.writeInt(mContentId);
        }

        public static final Parcelable.Creator<SaveState> CREATOR
                = new Parcelable.Creator<SaveState>() {
            @Override
            public SaveState createFromParcel(Parcel in) {
                return new SaveState(in);
            }

            @Override
            public SaveState[] newArray(int size) {
                return new SaveState[size];
            }
        };
    }

    /**
     * the internal group Sticky Delegate.
     */
    private class GroupStickyDelegate implements IStickyDelegate{

        private final ArrayList<IStickyDelegate> mDelegates = new ArrayList<>(5);

        public void addStickyDelegate(IStickyDelegate delegate){
            mDelegates.add(delegate);
        }
        public void removeStickyDelegate(IStickyDelegate delegate){
            mDelegates.remove(delegate);
        }
        public void clear(){
            mDelegates.clear();
        }
        @Override
        public boolean shouldIntercept(StickyNavigationLayout snv, int dy, int topViewState) {
            for(IStickyDelegate delegate : mDelegates){
                if(delegate.shouldIntercept(snv, dy, topViewState)){
                    return true;
                }
            }
            return false;
        }
        @Override
        public void afterOnMeasure(StickyNavigationLayout snv, View top, View indicator, View contentView) {
            for(IStickyDelegate delegate : mDelegates){
                delegate.afterOnMeasure(snv,top, indicator, contentView);
            }
        }

        @Override
        public void dispatchTouchEventToChild(StickyNavigationLayout snv, int dx, int dy, MotionEvent event) {
            for(IStickyDelegate delegate : mDelegates){
                delegate.dispatchTouchEventToChild(snv, dx, dy ,event);
            }
        }

        @Override
        public void onTouchEventUp(StickyNavigationLayout snv, MotionEvent event) {
            for(IStickyDelegate delegate : mDelegates){
                delegate.onTouchEventUp(snv ,event);
            }
        }
    }

    /**
     * on scroll  change listener.
     */
    public interface OnScrollChangeListener {
        /**
         * called when ths scroll distance change. may be negative .
         *
         * @param snl     the {@link StickyNavigationLayout}
         * @param dx      the delta x between this touch and last touch
         * @param dy      the delta y between this touch and last touch
         * @param totalDx the delta x between this touch and first touch
         * @param totalDy the delta y between this touch and first touch
         */
        void onPreScrollDistanceChange(StickyNavigationLayout snl, int dx, int dy, int totalDx, int totalDy);

        /**
         * called when ths scroll distance change. may be negative .
         *
         * @param snl     the {@link StickyNavigationLayout}
         * @param dx      the delta x between this touch and last touch
         * @param dy      the delta y between this touch and last touch
         * @param totalDx the delta x between this touch and first touch
         * @param totalDy the delta y between this touch and first touch
         */
        void onAfterScrollDistanceChange(StickyNavigationLayout snl, int dx, int dy, int totalDx, int totalDy);

        /**
         * called when the scroll state change
         *
         * @param snl            the {@link StickyNavigationLayout}
         * @param state          the scroll state . see {@link StickyNavigationLayout#SCROLL_STATE_IDLE} and etc.
         * @param focusDirection {@link View#FOCUS_UP} means finger down or {@link View#FOCUS_DOWN} means finger up.
         */
        void onScrollStateChange(StickyNavigationLayout snl, int state, int focusDirection);
    }

    /**
     * the sticky delegate
     */
    public interface IStickyDelegate {
        /**
         * called when you should intercept child's touch event.
         *
         * @param snv          the {@link StickyNavigationLayout}
         * @param dy           the delta y distance
         * @param topViewState the view state of top view. {@link #VIEW_STATE_SHOW} or {@link #VIEW_STATE_HIDE}
         * @return true to intercept
         */
        boolean shouldIntercept(StickyNavigationLayout snv, int dy, int topViewState);

        /**
         *  called after the {@link StickyNavigationLayout#onMeasure(int, int)}. this is useful used when we want to
         *  toggle two views visibility in {@link StickyNavigationLayout}(or else may cause bug). see it in demo.
         * @param snv the {@link StickyNavigationLayout}
         * @param top the top view
         * @param indicator the indicator view
         * @param contentView the content view
         *
         */
        void afterOnMeasure(StickyNavigationLayout snv, View top, View indicator, View contentView);

        /**
         * dispatch the touch event
         * @param snv the {@link StickyNavigationLayout}
         * @param dx the delta x
         * @param dy the delta y
         * @param event the event.
         */
        void dispatchTouchEventToChild(StickyNavigationLayout snv, int dx, int dy , MotionEvent event);

        void onTouchEventUp(StickyNavigationLayout snv, MotionEvent event);
    }

    /**
     * a simple implements of {@link IStickyDelegate}
     */
    public static class SimpleStickyDelegate implements IStickyDelegate{
        @Override
        public boolean shouldIntercept(StickyNavigationLayout snv, int dy, int topViewState) {
            return false;
        }
        @Override
        public void afterOnMeasure(StickyNavigationLayout snv, View top, View indicator, View contentView) {

        }

        @Override
        public void dispatchTouchEventToChild(StickyNavigationLayout snv, int dx, int dy, MotionEvent event) {

        }

        @Override
        public void onTouchEventUp(StickyNavigationLayout snv, MotionEvent event) {

        }
    }

    public static class RecyclerViewStickyDelegate implements IStickyDelegate {

        private final WeakReference<RecyclerView> mWeakRecyclerView;
        private boolean mParentReceived;

        public RecyclerViewStickyDelegate(RecyclerView mRv) {
            this.mWeakRecyclerView = new WeakReference<>(mRv);
        }

        @Override
        public boolean shouldIntercept(StickyNavigationLayout snv, int dy, int topViewState) {
            final RecyclerView view = mWeakRecyclerView.get();
            if (view == null)
                return false;
            final int position = findFirstVisibleItemPosition(view);
            if(position == -1) return false;
            final View child = view.getChildAt(position);
            boolean isTopHidden = topViewState == StickyNavigationLayout.VIEW_STATE_HIDE;
            if (!isTopHidden || (child != null && child.getTop() == 0 && dy > 0)) {
                //滑动到顶部，并且要继续向下滑动时，拦截触摸
                return true;
            }
            return false;
        }
        @Override
        public void afterOnMeasure(StickyNavigationLayout snv, View top, View indicator, View contentView) {

        }

        @Override
        public void dispatchTouchEventToChild(StickyNavigationLayout snv, int dx, int dy, MotionEvent event) {
            final RecyclerView view = mWeakRecyclerView.get();
            if (view != null) {
               /* final int position = findFirstVisibleItemPosition(view);
                if (position == -1){
                    return;
                }
                final View child = view.getChildAt(position);
                if(child != null && child.getTop() == 0  && dy > 0){
                    if(snv.getTopViewState() == VIEW_STATE_SHOW){
                        ViewGroup vg = (ViewGroup) view.getParent();
                        vg.dispatchTouchEvent(event);
                        mParentReceived = true;
                        return;
                    }
                }*/
                view.scrollBy(0, -dy);
                if(DEBUG){
                    Logger.i(TAG, "dispatchTouchEventToChild", "dy = " + dy
                            +" ,can scroll: " + view.getLayoutManager().canScrollVertically());
                }
            }
        }

        @Override
        public void onTouchEventUp(StickyNavigationLayout snv, MotionEvent event) {
            /*if( mParentReceived ) {
                mParentReceived = false;
                final RecyclerView view = mWeakRecyclerView.get();
                if (view != null) {
                    ViewGroup vg = (ViewGroup) view.getParent();
                    vg.dispatchTouchEvent(event);
                }
            }*/
        }

        public static int findFirstVisibleItemPosition(RecyclerView rv) {
            RecyclerView.LayoutManager lm = rv.getLayoutManager();
            int firstPos = RecyclerView.NO_POSITION;
            if (lm instanceof GridLayoutManager) {
                firstPos = ((GridLayoutManager) lm).findFirstVisibleItemPosition();

            } else if (lm instanceof LinearLayoutManager) {
                firstPos = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();

            } else if (lm instanceof StaggeredGridLayoutManager) {
                int positions[] = ((StaggeredGridLayoutManager) lm).findFirstVisibleItemPositions(null);
                for (int pos : positions) {
                    if (pos < firstPos) {
                        firstPos = pos;
                    }
                }
            }
            return firstPos;
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     *         scroll up. Override this if the child view is a custom view.
     */
   /* public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mTarget, -1) || mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }*/
    //========================  NestedScrollingParent begin ========================
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes){
        return isEnabled() && mEnableStickyTouch && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }
    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes){
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);
        // Dispatch up to the nested parent
        startNestedScroll(nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = 0;
        mNestedScrollInProgress = true;
    }
    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        /**
         * 滑动的最大高度:
         */
        scrollInternal(dx, dy, consumed);

        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
      /*  if (dy > 0 && mTotalUnconsumed > 0) {
            if ( dy > mTotalUnconsumed ) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;
            }
            Logger.i(TAG, "onNestedPreScroll", "mTotalUnconsumed = " + mTotalUnconsumed);
           // moveSpinner(mTotalUnconsumed);
        }*/

        // If a client layout is using a custom start position for the circle
        // view, they mean to hide it again before scrolling the child view
        // If we get back to mTotalUnconsumed == 0 and there is more to go, hide
        // the circle so it isn't exposed if its blocking content is moved
      /*  if (mUsingCustomStart && dy > 0 && mTotalUnconsumed == 0
                && Math.abs(dy - consumed[1]) > 0) {
            mCircleView.setVisibility(View.GONE);
        }*/

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    /**
     * do scroll internal.
     * @param dx  the delta x, may be negative
     * @param dy the delta y , may be negative
     * @param consumed optional , in not null will contains the consumed x and y by this scroll.
     * @return the consumed x and y as array by this scroll.
     */
    private int[] scrollInternal(int dx ,int dy, int[] consumed) {
        final int scrollY = getScrollY(); // >0
        if(consumed == null){
            consumed = new int[2];
        }
        int by = 0;
        if(dy > 0){
            //手势向下，view向上
            if(scrollY > 0){
                consumed[1] =  Math.min(dy, scrollY);
                by = - consumed[1];
            }
        }else{
            //手势向上，view向下
            if(scrollY < mTopViewHeight){
                int maxH = mTopViewHeight - scrollY;
                consumed[1] =  -Math.min(Math.abs(dy), maxH);
                by = - consumed[1];
            }else{
                //ignore
            }
        }
        scrollBy(0, by);
        return consumed;
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed){
       /* net
       inal int myConsumed = moveBy(dyUnconsumed);
        final int myUnconsumed = dyUnconsumed - myConsumed;
        dispatchNestedScroll(0, myConsumed, 0, myUnconsumed, null);*/

        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        Logger.i(TAG, "onNestedScroll", "mTotalUnconsumed = " +   (mTotalUnconsumed + Math.abs(dy))  );
       /* if (dy < 0 && !canChildScrollUp()) {
            mTotalUnconsumed += Math.abs(dy);
          //  moveSpinner(mTotalUnconsumed);
        }*/
    }
    @Override
    public void onStopNestedScroll(View target){
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed > 0) {
            //finishSpinner(mTotalUnconsumed);
            mTotalUnconsumed = 0;
        }
        // Dispatch up our nested parent
        stopNestedScroll();
    }
    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed){
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }
    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY){
        return dispatchNestedPreFling(velocityX, velocityY);
    }
    @Override
    public int getNestedScrollAxes(){
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }
    //========================  NestedScrollingParent end ========================

    //========================  NestedScrollingChild begin ========================
    public void setNestedScrollingEnabled(boolean enabled){
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }
    public boolean isNestedScrollingEnabled(){
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }
    public boolean startNestedScroll(int axes){
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }
    public void stopNestedScroll(){
        mNestedScrollingChildHelper.stopNestedScroll();
    }
    public boolean hasNestedScrollingParent(){
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed,
                                        int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow){
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow){
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed){
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }
    public boolean dispatchNestedPreFling(float velocityX, float velocityY){
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }
    //======================== end NestedScrollingChild =====================
}
