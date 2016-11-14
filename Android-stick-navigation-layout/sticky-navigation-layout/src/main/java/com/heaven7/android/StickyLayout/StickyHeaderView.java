package com.heaven7.android.StickyLayout;

import android.annotation.TargetApi;
import android.content.Context;
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
import android.widget.FrameLayout;
import android.widget.OverScroller;

import com.heaven7.core.util.Logger;

import java.util.Arrays;

/**
 * Created by heaven7 on 2016/11/14.
 */
public class StickyHeaderView extends FrameLayout implements NestedScrollingChild, NestedScrollingParent{

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

    private static final String TAG = StickyHeaderView.class.getSimpleName();
    private static final boolean DEBUG = true;


    private  NestedScrollingParentHelper mNestedScrollingParentHelper;
    private  NestedScrollingChildHelper mNestedScrollingChildHelper;

    private int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private final int[] mNestedOffsets = new int[2];
    private final int[] mScrollOffset = new int[2];
    private int mScrollPointerId ;

    private boolean mNestedScrollInProgress;
    private VelocityTracker mVelocityTracker;
    private OverScroller mScroller;

    private int mInitialTouchX;
    private int mLastTouchX;
    private int mInitialTouchY;
    private int mLastTouchY;
    private int mTouchSlop;

    private int mScrollState ;
    private float mMaximumVelocity;

    public StickyHeaderView(Context context) {
        this(context, null);
    }
    public StickyHeaderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public StickyHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }
    @TargetApi(21)
    public StickyHeaderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs){
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);

        mScroller = new OverScroller(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();//触摸阙值
        mMaximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
      //  mMinimumVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
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
        //TODO dispatchOnScrollStateChanged(state);
    }

    @Override
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
    }

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
                // final float xvel = ? -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mScrollPointerId) : 0;
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

    boolean fling(int xvel, int yvel) {
        mScroller.fling(0, getScrollY(), 0, yvel, 0, 0, 0, getHeight());
        ViewCompat.postInvalidateOnAnimation(this);
        return false;
    }

    private void cancelTouch() {
      /*  if (!mScroller.isFinished()) {   //如果手离开了,就终止滑动
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
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {//true滑动尚未完成
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            //  invalidate();
            ViewCompat.postInvalidateOnAnimation(this);
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


    // =========================== nested parent =======================================
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return isEnabled()  && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);
        // Dispatch up to the nested parent
        startNestedScroll(nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL);
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
        Logger.i(TAG, "scrollInternal", "dx = " + dx + ",dy = " + dy + " ,consumed = " + Arrays.toString(consumed));
        final int scrollY = getScrollY(); // >0
        if (consumed == null) {
            consumed = new int[2];
        }
        final int height  = getHeight();

        int by = 0;
        if (dy > 0) {
            //gesture up
            if (scrollY < height) {
                int maxH = height - scrollY;
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

        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;

        // Dispatch up our nested parent
        stopNestedScroll();
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
