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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.heaven7.android.scroll.IScrollHelper;
import com.heaven7.android.scroll.NestedScrollFactory;
import com.heaven7.android.scroll.NestedScrollHelper;
import com.heaven7.core.util.Logger;

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
public class StickyNavigationLayout extends LinearLayout implements NestedScrollingParent, NestedScrollingChild {

    private static final String TAG = "StickyNavLayout";

    private static final boolean DEBUG = false;

    private final NestedScrollHelper mNestedHelper;

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
    private int mTopViewId;
    private int mIndicatorId;
    private int mContentId;

    private int mTopViewHeight;
    private boolean mTopHide = false;

    private GroupCallbacks mGroupStickyDelegate;

    /**
     * auto fit the sticky scroll
     */
    private final boolean mAutoFitScroll;
    /**
     * the percent of auto fix.
     */
    private float mAutoFitPercent = 0.5f;

    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;

    private boolean mNestedScrollInProgress;
    private int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];

    private boolean mEnableStickyTouch = true;

    public StickyNavigationLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        //setOrientation(LinearLayout.VERTICAL);
        mGroupStickyDelegate = new GroupCallbacks();
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        mNestedHelper = NestedScrollFactory.create(this, new NestedScrollHelper.NestedScrollCallback() {
            @Override
            public boolean canScrollHorizontally(View target) {
                return false;
            }
            @Override
            public boolean canScrollVertically(View target) {
                return true;
            }
            @Override
            public int getMaximumYScrollDistance(View target) {
                return mTopViewHeight;
            }
        });

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StickyNavigationLayout);
        // mCodeSet will set stick view from onFinishInflate.

        mTopViewId = a.getResourceId(R.styleable.StickyNavigationLayout_stickyLayout_top_id, 0);
        mIndicatorId = a.getResourceId(R.styleable.StickyNavigationLayout_stickyLayout_indicator_id, 0);
        mContentId = a.getResourceId(R.styleable.StickyNavigationLayout_stickyLayout_content_id, 0);

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
     */
    public int getScrollState() {
        return mNestedHelper.getScrollState();
    }

    /**
     * set if enable the sticky touch
     *
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
     *
     * @return true to enable.
     */
    public boolean isStickyTouchEnabled() {
        return mEnableStickyTouch;
    }

    /**
     * add a sticky delegate
     *
     * @param delegate sticky delegate.
     */
    public void addStickyDelegate(IStickyCallback delegate) {
        mGroupStickyDelegate.addStickyDelegate(delegate);
    }

    /**
     * remove a sticky delegate
     *
     * @param delegate sticky delegate.
     */
    public void removeStickyDelegate(IStickyCallback delegate) {
        mGroupStickyDelegate.removeStickyDelegate(delegate);
    }

    /**
     * set the OnScrollChangeListener
     *
     * @param l the listener
     */
    public void addOnScrollChangeListener(IScrollHelper.OnScrollChangeListener l) {
        this.mNestedHelper.addOnScrollChangeListener(l);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!mEnableStickyTouch){
            return super.onTouchEvent(event);
        }
        return mNestedHelper.onTouchEvent(event);
    }


    /**
     * Begin a standard fling with an initial velocity along each axis in pixels per second.
     * If the velocity given is below the system-defined minimum this method will return false
     * and no fling will occur.
     *
     * @param velocityX Initial horizontal velocity in pixels per second
     * @param velocityY Initial vertical velocity in pixels per second
     * @return true if the fling was started, false if the velocity was too low to fling or
     *  does not support scrolling in the axis fling is issued.
     */
    public boolean fling(int velocityX, int velocityY) {
        return mNestedHelper.fling(velocityX , velocityY);
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     */
    public final void smoothScrollTo(int x, int y) {
        mNestedHelper.smoothScrollBy(x - getScrollX(), y - getScrollY());
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     */
    public final void smoothScrollBy(int dx, int dy) {
        mNestedHelper.smoothScrollBy(dx , dy);
    }

    @Override
    public void computeScroll() {
       mNestedHelper.computeScroll();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final SaveState saveState = new SaveState(super.onSaveInstanceState());
        saveState.mTopHide = this.mTopHide;
        saveState.mEnableStickyTouch = this.mEnableStickyTouch;
        return saveState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        if (state != null) {
            SaveState ss = (SaveState) state;
            this.mTopHide = ss.mTopHide;
            this.mEnableStickyTouch = ss.mEnableStickyTouch;
        }
    }

    protected static class SaveState extends BaseSavedState {

        boolean mTopHide;
        boolean mEnableStickyTouch;

        public SaveState(Parcel source) {
            super(source);
            mTopHide = source.readByte() == 1;
            mEnableStickyTouch = source.readByte() == 1;
        }

        public SaveState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte((byte) (mTopHide ? 1 : 0));
            out.writeByte((byte) (mEnableStickyTouch ? 1 : 0));
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
    private class GroupCallbacks implements IStickyCallback {

        private final ArrayList<IStickyCallback> mDelegates = new ArrayList<>(5);

        public void addStickyDelegate(IStickyCallback delegate) {
            if(!mDelegates.contains(delegate)) {
                mDelegates.add(delegate);
            }
        }

        public void removeStickyDelegate(IStickyCallback delegate) {
            mDelegates.remove(delegate);
        }

        public void clear() {
            mDelegates.clear();
        }

        @Override
        public void afterOnMeasure(StickyNavigationLayout snv, View top, View indicator, View content) {
            for (IStickyCallback delegate : mDelegates) {
                delegate.afterOnMeasure(snv, top, indicator, content);
            }
        }

    }


    /**
     * the sticky callback
     */
    public interface IStickyCallback {

        /**
         *  called after the {@link StickyNavigationLayout_backup#onMeasure(int, int)}. this is useful used when we want to
         *  toggle two views visibility in {@link StickyNavigationLayout_backup}(or else may cause bug). see it in demo.
         * @param snv the {@link StickyNavigationLayout_backup}
         * @param top the top view
         * @param indicator the indicator view
         * @param contentView the content view
         *
         */
        void afterOnMeasure(StickyNavigationLayout snv, View top, View indicator, View contentView);
    }

    //========================  NestedScrollingParent begin ========================
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return isEnabled() && mEnableStickyTouch && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);
        // Dispatch up to the nested parent
        startNestedScroll(nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mNestedScrollInProgress = true;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        mNestedHelper.nestedScroll(dx, dy, consumed , true);

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed) {
        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);
    }

    @Override
    public void onStopNestedScroll(View target) {
      //  Logger.i(TAG, "onStopNestedScroll");
        checkAutoFitScroll();
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;

        // Dispatch up our nested parent
        stopNestedScroll();
    }

    private void checkAutoFitScroll() {
        //check auto fit scroll
        if (mAutoFitScroll) {
            //check whole gesture.
            final float scrollY = getScrollY();
            if(scrollY >= mTopViewHeight * mAutoFitPercent){
                smoothScrollTo(0, mTopViewHeight);
            }else{
                smoothScrollTo(0, 0);
            }
        }
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }
    //========================  NestedScrollingParent end ========================

    //========================  NestedScrollingChild begin ========================
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed,
                                        int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }
    //======================== end NestedScrollingChild =====================
}
