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
    private static final boolean DEBUG_DISPATCH_EVENT  = false;

    private static final long ANIMATED_SCROLL_GAP = 250;

    /**
     * indicate the scroll state is just end
     */
    public static final int SCROLL_STATE_IDLE = 1;
    /**
     * indicate the scroll state is just begin.
     */
    public static final int SCROLL_STATE_START = 2;
    /**
     * indicate the scroll state is setting/scrolling.
     */
    public static final int SCROLL_STATE_SETTING = 3;

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

    private int mLastY;
    private boolean mDragging;

    private OnScrollChangeListener mScrollListener;

    private boolean mNeedIntercept;
    private int mDownY;
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
    private int[] mParentScrollConsumed ;
    private boolean mNestedScrollInProgress;
    private int[] mParentOffsetInWindow;

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

        if (mEnableStickyTouch && mContentView != null && mIndicator != null) {
            // 设置view的高度 (将mViewPager。的高度设置为  整个 Height - 导航的高度) - 被拦截的child view
            ViewGroup.LayoutParams params = mContentView.getLayoutParams();
            params.height = getMeasuredHeight() - mIndicator.getMeasuredHeight();
            mGroupStickyDelegate.afterOnMeasure(this, mTop, mIndicator, mContentView);
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
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (DEBUG) {
            Logger.i(TAG, "onSizeChanged");
        }
        if (mEnableStickyTouch && mTop != null) {
            mTopViewHeight = mTop.getMeasuredHeight();
            final ViewGroup.LayoutParams lp = mTop.getLayoutParams();
            if (lp instanceof MarginLayoutParams) {
                mTopViewHeight += ((MarginLayoutParams) lp).topMargin + ((MarginLayoutParams) lp).bottomMargin;
            }
        }
    }

    public void setEnableStickyTouch(boolean enable) {
        if (mEnableStickyTouch != enable) {
            this.mEnableStickyTouch = enable;
            requestLayout();
        }
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
        if (!mEnableStickyTouch) {
            return super.onInterceptTouchEvent(ev);
        }
        /**
         * 1, 上拉的时候，停靠后分发给child滑动. max = mTopViewHeight
         * 2, 下拉时，先拉上面的，拉完后分发给child.
         */
        int action = ev.getAction();
        int y = (int) (ev.getY() + 0.5f);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownY = mLastY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                int dy = y - mLastY;

                if (Math.abs(dy) > mTouchSlop) {
                    if (mNeedIntercept) {
                        return true;
                    }
                    if (dy > 0) {
                        return getScrollY() == mTopViewHeight;
                    }
                    if (mGroupStickyDelegate.shouldIntercept(this, dy,
                            mTopHide ? VIEW_STATE_HIDE : VIEW_STATE_SHOW)) {
                        mDragging = true;
                        return true;
                    }
                }
                break;
        }
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

        switch (action) {

            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished())
                    mScroller.abortAnimation();
                mVelocityTracker.addMovement(event);
                mDownY = mLastY = y;
                return true;

            case MotionEvent.ACTION_MOVE:
                int dy = y - mLastY;

                if (!mDragging && Math.abs(dy) > mTouchSlop) {
                    mDragging = true;
                }
                if (mDragging) {
                    //手向下滑动， dy >0 否则 <0.
                    final int scrollY = getScrollY();
                    final int totalDy = mTotalDy = y - mDownY;
                    //Logger.i(TAG, "onTouchEvent", "ScrollY = " + scrollY + " ,dy = " + dy + " , mTopViewHeight = " + mTopViewHeight);
                    mLastY = y;

                    mFocusDir = dy < 0 ? View.FOCUS_DOWN : View.FOCUS_UP;
                    setScrollState(SCROLL_STATE_START);

                    onPreScrollDistanceChange(0, dy, 0, totalDy);
                    if (dy < 0) {
                        //手势向上滑动 ,view down
                        /**
                         *  called [ onTouchEvent() ]: ScrollY = 666 ,dy = -7.4692383 , mTopViewHeight = 788
                         *  called [ onTouchEvent() ]: ScrollY = 673 ,dy = -3.748291 , mTopViewHeight = 788
                         */
                        if (scrollY == mTopViewHeight) {
                            //分发给child
                                mGroupStickyDelegate.scrollBy(this, dy);
                                //mGroupStickyDelegate.dispatchTouchEventToChild(this, event);
                        } else if (scrollY - dy > mTopViewHeight) {
                            //top height is the max scroll height
                            scrollTo(getScrollX(), mTopViewHeight);
                        } else {
                            scrollBy(0, -dy);
                        }
                    } else {
                        //手势向下
                        if (scrollY == 0) {
                            //分发事件给child
                            mGroupStickyDelegate.scrollBy(this, dy);
                           // mGroupStickyDelegate.dispatchTouchEventToChild(this, event);
                        } else {
                            if (scrollY - dy < 0) {
                                dy = scrollY;
                            }
                            scrollBy(0, -dy);
                        }
                    }
                    onAfterScrollDistanceChange(0, dy, 0, totalDy);
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                mDragging = false;
                if (!mScroller.isFinished()) {//如果手离开了,就终止滑动
                    mScroller.abortAnimation();
                }
                setScrollState(SCROLL_STATE_IDLE);
                mFocusDir = 0;
                break;

            case MotionEvent.ACTION_UP:

                if (Math.abs(y - mLastY) > mTouchSlop) {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity); //1000表示像素/秒
                    int velocityY = (int) mVelocityTracker.getYVelocity();
                    if (Math.abs(velocityY) > mMinimumVelocity) {
                        if (DEBUG) {
                            Logger.i(TAG, "onTouchEvent", "begin fling: velocityY = " + velocityY);
                        }
                        //fling(-velocityY);
                        smoothScrollTo(0, mTotalDy < 0 ? mTopViewHeight : 0);
                    }
                } else {
                    //check auto fit scroll
                    if (mAutoFitScroll) {
                        //check whole gesture.
                        if (mTotalDy < 0) {
                            //finger up
                            //if larger the 1/2 * maxHeight go to maxHeight
                            if (Math.abs(mTotalDy) >= mTopViewHeight * mAutoFitPercent) {
                                smoothScrollTo(0, mTopViewHeight);
                            } else {
                                smoothScrollTo(0, 0);
                            }
                        } else {
                            //finger down
                            if (Math.abs(mTotalDy) >= mTopViewHeight * mAutoFitPercent) {
                                smoothScrollTo(0, 0);
                            } else {
                                smoothScrollTo(0, mTopViewHeight);
                            }
                        }
                    }
                }

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                mDragging = false;
                setScrollState(SCROLL_STATE_IDLE);
                mFocusDir = 0;
                break;
        }
        return super.onTouchEvent(event);
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

    /**
     * called when the scroll state change
     *
     * @param expectScrollState the expect state.
     */
    private void setScrollState(int expectScrollState) {
        expectScrollState = adjustState(expectScrollState);
        if (mScrollState == expectScrollState) {
            //ignore
            return;
        }
        if (DEBUG) {
            Logger.i(TAG, "setScrollState", "new state = " + getStateString(expectScrollState));
        }
        mScrollState = expectScrollState;

        if (mScrollListener != null) {
            mScrollListener.onScrollStateChange(this, expectScrollState, mFocusDir);
        } else {
            if (getContext() instanceof OnScrollChangeListener) {
                ((OnScrollChangeListener) getContext()).onScrollStateChange(this, expectScrollState, mFocusDir);
            }
        }
    }

    private static String getStateString(int state) {
        switch (state) {
            case SCROLL_STATE_START:
                return "SCROLL_STATE_START";

            case SCROLL_STATE_SETTING:
                return "SCROLL_STATE_SETTING";

            case SCROLL_STATE_IDLE:
            default:
                return "SCROLL_STATE_IDLE";
        }
    }

    private int adjustState(int expectScrollState) {
        switch (expectScrollState) {
            case SCROLL_STATE_START: {
                if (mScrollState == SCROLL_STATE_START) {
                    return SCROLL_STATE_SETTING;
                } else if (mScrollState == SCROLL_STATE_IDLE) {
                    return expectScrollState;
                } else {
                    return SCROLL_STATE_SETTING;
                }
            }

            case SCROLL_STATE_SETTING:
            case SCROLL_STATE_IDLE:
            default:
                return expectScrollState;
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
        public void scrollBy(StickyNavigationLayout snv, int dy) {
            for(IStickyDelegate delegate : mDelegates){
                delegate.scrollBy(snv, dy);
            }
        }
        @Override
        public void afterOnMeasure(StickyNavigationLayout snv, View top, View indicator, View contentView) {
            for(IStickyDelegate delegate : mDelegates){
                delegate.afterOnMeasure(snv,top, indicator, contentView);
            }
        }

        @Override
        public void dispatchTouchEventToChild(StickyNavigationLayout snv,MotionEvent event) {
            for(IStickyDelegate delegate : mDelegates){
                delegate.dispatchTouchEventToChild(snv,event);
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
         * @param state          the scroll state . see {@link StickyNavigationLayout#SCROLL_STATE_START} and etc.
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
         * scroll child view by the target delta y distance.
         *
         * @param snv the {@link StickyNavigationLayout}
         * @param dy  the delta y
         */
        void scrollBy(StickyNavigationLayout snv, int dy);

        /**
         *  called after the {@link StickyNavigationLayout#onMeasure(int, int)}. this is useful used when we want to
         *  toggle two views visibility(or else may cause bug). see it in demo.
         * @param snv the {@link StickyNavigationLayout}
         * @param top the top view
         * @param indicator the indicator view
         * @param contentView the content view
         *
         */
        void afterOnMeasure(StickyNavigationLayout snv, View top, View indicator, View contentView);


        void dispatchTouchEventToChild(StickyNavigationLayout snv,MotionEvent event);
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
        public void scrollBy(StickyNavigationLayout snv, int dy) {

        }
        @Override
        public void afterOnMeasure(StickyNavigationLayout snv, View top, View indicator, View contentView) {

        }
        @Override
        public void dispatchTouchEventToChild(StickyNavigationLayout snv,MotionEvent event) {

        }
    }

    public static class RecyclerViewStickyDelegate implements IStickyDelegate {

        private final WeakReference<RecyclerView> mWeakRecyclerView;

        public RecyclerViewStickyDelegate(RecyclerView mRv) {
            this.mWeakRecyclerView = new WeakReference<>(mRv);
        }

        @Override
        public boolean shouldIntercept(StickyNavigationLayout snv, int dy, int topViewState) {
            final RecyclerView view = mWeakRecyclerView.get();
            if (view == null)
                return false;
            final int position = findFirstVisibleItemPosition(view);
            final View child = view.getChildAt(position);
            boolean isTopHidden = topViewState == StickyNavigationLayout.VIEW_STATE_HIDE;
            if (!isTopHidden || (child != null && child.getTop() == 0 && dy > 0)) {
                //滑动到顶部，并且要继续向下滑动时，拦截触摸
                return true;
            }
            return false;
        }
        @Override
        public void scrollBy(StickyNavigationLayout snv, int dy) {
            if(DEBUG_DISPATCH_EVENT){
                return;
            }
            final RecyclerView view = mWeakRecyclerView.get();
            if (view != null) {
                view.scrollBy(0, -dy);
                if (DEBUG) {
                    Logger.i(TAG, "scrollBy", "RecyclerView height = " + view.getMeasuredHeight() + " ,bottom = " + view.getBottom());
                    Logger.i(TAG, "scrollBy", "StickyNavigationLayout height = " + snv.getMeasuredHeight());
                }
            }
        }
        @Override
        public void afterOnMeasure(StickyNavigationLayout snv, View top, View indicator, View contentView) {

        }

        @Override
        public void dispatchTouchEventToChild(StickyNavigationLayout snv,MotionEvent event) {
            final RecyclerView view = mWeakRecyclerView.get();
            if (view != null) {
                view.dispatchTouchEvent(event);
            }
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
        return isEnabled() && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
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
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;
            }
            Logger.i(TAG, "onNestedPreScroll", "mTotalUnconsumed = " + mTotalUnconsumed);
           // moveSpinner(mTotalUnconsumed);
        }

        // If a client layout is using a custom start position for the circle
        // view, they mean to hide it again before scrolling the child view
        // If we get back to mTotalUnconsumed == 0 and there is more to go, hide
        // the circle so it isn't exposed if its blocking content is moved
      /*  if (mUsingCustomStart && dy > 0 && mTotalUnconsumed == 0
                && Math.abs(dy - consumed[1]) > 0) {
            mCircleView.setVisibility(View.GONE);
        }*/

        // Now let our nested parent consume the leftovers
       /* final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
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
