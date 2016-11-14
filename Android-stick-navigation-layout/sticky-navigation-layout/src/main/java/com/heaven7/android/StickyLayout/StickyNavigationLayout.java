package com.heaven7.android.StickyLayout;


import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
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

    private static final long ANIMATED_SCROLL_GAP = 250;

    /**
     * The view is not currently scrolling.
     *
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_IDLE = 0;

    /**
     * The view is currently being dragged by outside input such as user touch input.
     *
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_DRAGGING = 1;

    /**
     * The view is currently animating to a final position while not under
     * outside control.
     *
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_SETTLING = 2;

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

    private GroupStickyDelegate mGroupStickyDelegate;
    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mMaximumVelocity, mMinimumVelocity;

    private int mLastTouchY, mLastTouchX;
    private int mInitialTouchY, mInitialTouchX;

    private OnScrollChangeListener mScrollListener;

    private boolean mNeedIntercept;

    private int mScrollState = SCROLL_STATE_IDLE;

    private boolean mEnableStickyTouch = true;

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

    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;

    private boolean mNestedScrollInProgress;
    private int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private final int[] mNestedOffsets = new int[2];
    private final int[] mScrollOffset = new int[2];
    private int mScrollPointerId;

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
     *
     * @return {@link #SCROLL_STATE_IDLE}, {@link #SCROLL_STATE_DRAGGING} or
     * {@link #SCROLL_STATE_SETTLING}
     */
    public int getScrollState() {
        return mScrollState;
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
     * use {@link #addStickyDelegate(IStickyDelegate)} instead.
     * set the sticky delegate.
     *
     * @param delegate the delegate
     */
    @Deprecated
    public void setStickyDelegate(IStickyDelegate delegate) {
        mGroupStickyDelegate.addStickyDelegate(delegate);
    }

    /**
     * add a sticky delegate
     *
     * @param delegate sticky delegate.
     */
    public void addStickyDelegate(IStickyDelegate delegate) {
        mGroupStickyDelegate.addStickyDelegate(delegate);
    }

    /**
     * remove a sticky delegate
     *
     * @param delegate sticky delegate.
     */
    public void removeStickyDelegate(IStickyDelegate delegate) {
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

  /*  @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
        // TODO, 1: up时，检查自动停靠
        boolean canScrollHorizontally = canScrollHorizontally();
        boolean canScrollVertically = canScrollVertically();

        final int action = MotionEventCompat.getActionMasked(ev);
        final int actionIndex = MotionEventCompat.getActionIndex(ev);
        final int y = (int) (ev.getY() + 0.5f);
        final int x = (int) (ev.getX() + 0.5f);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mScrollPointerId = ev.getPointerId(0);
                mInitialTouchX = mLastTouchX = (int) (ev.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (ev.getY() + 0.5f);

                if (mScrollState == SCROLL_STATE_SETTLING) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    setScrollState(SCROLL_STATE_DRAGGING);
                }

                // Clear the nested offsets
                mNestedOffsets[0] = mNestedOffsets[1] = 0;

                int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
                if (canScrollHorizontally) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_HORIZONTAL;
                }
                if (canScrollVertically) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_VERTICAL;
                }
                startNestedScroll(nestedScrollAxis);
                break;

            case MotionEventCompat.ACTION_POINTER_DOWN:
                mScrollPointerId = ev.getPointerId(actionIndex);
                mInitialTouchX = mLastTouchX = x;
                mInitialTouchY = mLastTouchY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                final int index = ev.findPointerIndex(mScrollPointerId);
                if (index < 0) {
                    Log.e(TAG, "Error processing scroll; pointer index for id " +
                            mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                    return false;
                }

                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    final int dx = x - mInitialTouchX;
                    final int dy = y - mInitialTouchY;

                    boolean startScroll = false;
                    if (canScrollHorizontally && Math.abs(dx) > mTouchSlop) {
                        mLastTouchX = mInitialTouchX + mTouchSlop * (dx < 0 ? -1 : 1);
                        startScroll = true;
                    }
                    if (canScrollVertically && Math.abs(dy) > mTouchSlop) {
                        mLastTouchY = mInitialTouchY + mTouchSlop * (dy < 0 ? -1 : 1);
                        startScroll = true;
                    }
                    if (startScroll) {
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP: {
                onPointerUp(ev);
            }
            break;

            case MotionEvent.ACTION_UP: {
                mVelocityTracker.clear();
                stopNestedScroll();
            }
            break;

            case MotionEvent.ACTION_CANCEL: {
                cancelTouch();
            }
        }

        return mScrollState == SCROLL_STATE_DRAGGING;
    }*/

   /* @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Logger.i(TAG, "onInterceptTouchEvent", ev.toString());
        if(ev.getAction() == MotionEvent.ACTION_UP){
            checkAutoFitScroll();
        }else if(ev.getAction() == MotionEvent.ACTION_DOWN){
            mTotalDy = 0;
        }
        return super.onInterceptTouchEvent(ev);
    }*/

    private void onPointerUp(MotionEvent e) {
        final int actionIndex = MotionEventCompat.getActionIndex(e);
        if (e.getPointerId(actionIndex) == mScrollPointerId) {
            // Pick a new pointer to pick up the slack.
            final int newIndex = actionIndex == 0 ? 1 : 0;
            mScrollPointerId = e.getPointerId(newIndex);
            mInitialTouchX = mLastTouchX = (int) (e.getX(newIndex) + 0.5f);
            mInitialTouchY = mLastTouchY = (int) (e.getY(newIndex) + 0.5f);
        }
    }

    private boolean canScrollHorizontally() {
        return false;
    }

    private boolean canScrollVertically() {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final boolean canScrollHorizontally = canScrollHorizontally();
        final boolean canScrollVertically = canScrollVertically();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        boolean eventAddedToVelocityTracker = false;

        final MotionEvent vtev = MotionEvent.obtain(event);
        final int action = MotionEventCompat.getActionMasked(event);
        final int actionIndex = MotionEventCompat.getActionIndex(event);

        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsets[0] = mNestedOffsets[1] = 0;
        }
        vtev.offsetLocation(mNestedOffsets[0], mNestedOffsets[1]);

        switch (action) {

            case MotionEvent.ACTION_DOWN: {
                if (!mScroller.isFinished())
                    mScroller.abortAnimation();
                mScrollPointerId = event.getPointerId(0);
                mInitialTouchX = mLastTouchX = (int) (event.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (event.getY() + 0.5f);

                int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
                if (canScrollHorizontally) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_HORIZONTAL;
                }
                if (canScrollVertically) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_VERTICAL;
                }
                startNestedScroll(nestedScrollAxis);
            }
            break;

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                mScrollPointerId = event.getPointerId(actionIndex);
                mInitialTouchX = mLastTouchX = (int) (event.getX(actionIndex) + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (event.getY(actionIndex) + 0.5f);
            }
            break;

            case MotionEvent.ACTION_MOVE: {
                //遵循Google规范(比如recyclerView源代码)。避免处理嵌套滑动出问题。
                final int index = event.findPointerIndex(mScrollPointerId);
                if (index < 0) {
                    Log.e(TAG, "Error processing scroll; pointer index for id " +
                            mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                    return false;
                }
                final int x = (int) (event.getX(index) + 0.5f);
                final int y = (int) (event.getY(index) + 0.5f);
                int dx = mLastTouchX - x;
                int dy = mLastTouchY - y;

                if (dispatchNestedPreScroll(dx, dy, mScrollConsumed, mScrollOffset)) {
                    dx -= mScrollConsumed[0];
                    dy -= mScrollConsumed[1];
                    vtev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
                    // Updated the nested offsets
                    mNestedOffsets[0] += mScrollOffset[0];
                    mNestedOffsets[1] += mScrollOffset[1];
                }

                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    boolean startScroll = false;
                    if (canScrollHorizontally && Math.abs(dx) > mTouchSlop) {
                        if (dx > 0) {
                            dx -= mTouchSlop;
                        } else {
                            dx += mTouchSlop;
                        }
                        startScroll = true;
                    }
                    if (canScrollVertically && Math.abs(dy) > mTouchSlop) {
                        if (dy > 0) {
                            dy -= mTouchSlop;
                        } else {
                            dy += mTouchSlop;
                        }
                        startScroll = true;
                    }
                    if (startScroll) {
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }

                if (mScrollState == SCROLL_STATE_DRAGGING) {
                    mLastTouchX = x - mScrollOffset[0];
                    mLastTouchY = y - mScrollOffset[1];
                    //手向下滑动， dy >0 否则 <0.
                    if (scrollByInternal(0, dy, vtev)) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
            }
            break;

            case MotionEventCompat.ACTION_POINTER_UP: {
                onPointerUp(event);
            }
            break;

            case MotionEvent.ACTION_CANCEL:
                cancelTouch();
                break;

            case MotionEvent.ACTION_UP:
                mVelocityTracker.addMovement(vtev);
                eventAddedToVelocityTracker = true;
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                final float xvel = (int) mVelocityTracker.getXVelocity();
                final float yvel = (int) mVelocityTracker.getYVelocity();
                // final float xvel = -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mScrollPointerId) : 0;
                if (!((xvel != 0 || yvel != 0) && fling((int) xvel, (int) yvel))) {
                    setScrollState(SCROLL_STATE_IDLE);
                }
                resetTouch();
                break;
        }
        if (!eventAddedToVelocityTracker) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();

        return true;
    }

    private void cancelTouch() {
      /*  if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }*/
        resetTouch();
        setScrollState(SCROLL_STATE_IDLE);
    }

    private void resetTouch() {
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
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
        }
        dispatchOnScrollStateChanged(state);
    }

    private void dispatchOnScrollStateChanged(int state) {
        if (mScrollListener != null) {
            mScrollListener.onScrollStateChanged(this, state);
        }
    }


    /**
     * Does not perform bounds checking. Used by internal methods that have already validated input.
     * <p/>
     * It also reports any unused scroll request to the related EdgeEffect.
     *
     * @param dx The amount of horizontal scroll request
     * @param dy The amount of vertical scroll request
     * @param ev The originating MotionEvent, or null if not from a touch event.
     * @return Whether any scroll was consumed in either direction.
     */
    boolean scrollByInternal(int dx, int dy, MotionEvent ev) {
        mScrollConsumed[0] = 0;
        mScrollConsumed[1] = 0;
        scrollInternal(dx, dy, mScrollConsumed);
        int consumedX = mScrollConsumed[0];
        int consumedY = mScrollConsumed[1];

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
        if (consumedX != 0 || consumedY != 0) {
            dispatchOnScrolled(consumedX, consumedY);
        }
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

    @Override
    public void computeScroll() {
        //super.computeScroll();
        if (mScroller.computeScrollOffset()) {//true滑动尚未完成
            scrollTo(0, mScroller.getCurrY());
            //  invalidate();
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    void dispatchOnScrolled(int hresult, int vresult) {
        // Pass the current scrollX/scrollY values; no actual change in these properties occurred
        // but some general-purpose code may choose to respond to changes this way.
        final int scrollX = getScrollX();
        final int scrollY = getScrollY();
        onScrollChanged(scrollX, scrollY, scrollX, scrollY);

        // Pass the real deltas to onScrolled, the RecyclerView-specific method.
        onScrolled(hresult, vresult);

        // Invoke listeners last. Subclassed view methods always handle the event first.
        // All internal state is consistent by the time listeners are invoked.
        if (mScrollListener != null) {
            mScrollListener.onScrolled(this, hresult, vresult);
        }
    }
    protected void onScrolled(int dx, int dy) {
        // do nothing
        //dy > 0 ? gesture up :gesture down
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final SaveState saveState = new SaveState(super.onSaveInstanceState());
        saveState.mNeedIntercept = this.mNeedIntercept;
        saveState.mTopHide = this.mTopHide;
        saveState.mLastScroll = this.mLastScroll;
        saveState.mEnableStickyTouch = this.mEnableStickyTouch;
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
        }
    }

    protected static class SaveState extends BaseSavedState {

        boolean mNeedIntercept;
        boolean mTopHide;
        boolean mEnableStickyTouch;
        long mLastScroll;

        public SaveState(Parcel source) {
            super(source);
            mNeedIntercept = source.readByte() == 1;
            mTopHide = source.readByte() == 1;
            mEnableStickyTouch = source.readByte() == 1;
            mLastScroll = source.readLong();
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
    private class GroupStickyDelegate implements IStickyDelegate {

        private final ArrayList<IStickyDelegate> mDelegates = new ArrayList<>(5);

        public void addStickyDelegate(IStickyDelegate delegate) {
            mDelegates.add(delegate);
        }

        public void removeStickyDelegate(IStickyDelegate delegate) {
            mDelegates.remove(delegate);
        }

        public void clear() {
            mDelegates.clear();
        }

        @Override
        public void afterOnMeasure(StickyNavigationLayout snv, View top, View indicator, View content) {
            for (IStickyDelegate delegate : mDelegates) {
                delegate.afterOnMeasure(snv, top, indicator, content);
            }
        }

    }

    /**
     * on scroll  change listener.
     */
    public interface OnScrollChangeListener {

        /**
         * called when the scroll state change
         *
         * @param snl            the {@link StickyNavigationLayout}
         * @param state          the scroll state . see {@link StickyNavigationLayout#SCROLL_STATE_IDLE} and etc.
         */
        void onScrollStateChanged(StickyNavigationLayout snl, int state);

        /**
         * Callback method to be invoked when the RecyclerView has been scrolled. This will be
         * called after the scroll has completed.
         * <p>
         * This callback will also be called if visible item range changes after a layout
         * calculation. In that case, dx and dy will be 0.
         *
         * @param snl the {@link StickyNavigationLayout} which scrolled.
         * @param dx  The amount of horizontal scroll.
         * @param dy  The amount of vertical scroll.
         */
        void onScrolled(StickyNavigationLayout snl, int dx, int dy);
    }

    /**
     * the sticky delegate
     */
    public interface IStickyDelegate {

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

    /**
     * a simple implements of {@link IStickyDelegate}
     */
    public static class SimpleStickyDelegate implements IStickyDelegate {

        @Override
        public void afterOnMeasure(StickyNavigationLayout snv, View top, View indicator, View contentView) {

        }
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
        scrollInternal(dx, dy, consumed);

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    /**
     * do scroll internal.
     *
     * @param dx       the delta x, may be negative
     * @param dy       the delta y , may be negative
     * @param consumed optional , in not null will contains the consumed x and y by this scroll.
     * @return the consumed x and y as array by this scroll.
     */
    private int[] scrollInternal(int dx, int dy, int[] consumed) {
        //向上滑 dy >0 , 下滑 dy < 0
       // Logger.i(TAG, "scrollInternal", "dx = " + dx + ",dy = " + dy + " ,consumed = " + Arrays.toString(consumed));
        final int scrollY = getScrollY(); // >0
        if (consumed == null) {
            consumed = new int[2];
        }
        int by = 0;
        if (dy > 0) {
            //gesture up
            if (scrollY < mTopViewHeight) {
                int maxH = mTopViewHeight - scrollY;
                by = consumed[1] = Math.min(dy, maxH);
            } else {
                //ignore
            }
        } else {
            //gesture down
            if (scrollY > 0) {
                consumed[1] = -Math.min(Math.abs(dy), scrollY);
                by = consumed[1];
            }
        }
        scrollBy(0, by);
        return consumed;
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed) {
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
        // Logger.i(TAG, "onNestedScroll", "mTotalUnconsumed = " +   (mTotalUnconsumed + Math.abs(dy))  );
       /* if (dy < 0 && !canChildScrollUp()) {
            mTotalUnconsumed += Math.abs(dy);
          //  moveSpinner(mTotalUnconsumed);
        }*/
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
           /* if (mTotalDy > 0) {
                //finger up
                if (Math.abs(mTotalDy) >= mTopViewHeight * mAutoFitPercent) {
                    smoothScrollTo(0, mTopViewHeight);
                } else {
                    smoothScrollTo(0, 0);
                }
            } else {
                //finger down
                //if larger the 1/2 * maxHeight go to maxHeight
                if (Math.abs(mTotalDy) >= mTopViewHeight * mAutoFitPercent) {
                    smoothScrollTo(0, mTopViewHeight);
                } else {
                    smoothScrollTo(0, 0);
                }
            }*/
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
