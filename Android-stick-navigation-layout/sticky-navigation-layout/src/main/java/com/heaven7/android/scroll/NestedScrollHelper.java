package com.heaven7.android.scroll;

import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.OverScroller;

/**
 * nested scroll helper . it can communicate with {@link NestedScrollingChild}.
 * Created by heaven7 on 2016/11/15.
 */
public class NestedScrollHelper extends ScrollHelper implements INestedScrollHelper {

    private static final String TAG = NestedScrollHelper.class.getSimpleName();

    private final NestedScrollingChild mNestedChild;

    private final int[] mScrollConsumed = new int[2];
    private final int[] mNestedOffsets = new int[2];
    private final int[] mScrollOffset = new int[2];
    private int mScrollPointerId;

    private VelocityTracker mVelocityTracker;
    /**
     * the state of enable nested scroll. default is true.
     */
    private boolean mEnabledNestedScroll = true;

    private int mInitialTouchX;
    private int mLastTouchX;
    private int mInitialTouchY;
    private int mLastTouchY;

    public NestedScrollHelper(View target, OverScroller scroller, NestedScrollCallback callback) {
        this(target, 1, scroller, (NestedScrollingChild) target, callback);
    }

    public NestedScrollHelper(View target, float sensitivity, OverScroller scroller, NestedScrollingChild child, NestedScrollCallback callback) {
        super(target, sensitivity, scroller, callback);
        this.mNestedChild = child;
    }

    @Override
    public void setEnableNestedScroll(boolean enable) {
        if(mEnabledNestedScroll != enable){
            this.mEnabledNestedScroll = enable;
            onNestedScrollStateChanged(enable);
        }
    }
    @Override
    public boolean isEnabledNestedScroll() {
        return mEnabledNestedScroll;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
        boolean canScrollHorizontally = mCallback.canScrollHorizontally(getTarget());
        boolean canScrollVertically = mCallback.canScrollVertically(getTarget());

        final int action = MotionEventCompat.getActionMasked(ev);
        final int actionIndex = MotionEventCompat.getActionIndex(ev);
        final int mTouchSlop = getTouchSlop();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mScrollPointerId = ev.getPointerId(0);
                mInitialTouchX = mLastTouchX = (int) (ev.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (ev.getY() + 0.5f);

                if (getScrollState() == SCROLL_STATE_SETTLING) {
                    getTarget().getParent().requestDisallowInterceptTouchEvent(true);
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
                mNestedChild.startNestedScroll(nestedScrollAxis);
                break;

            case MotionEventCompat.ACTION_POINTER_DOWN:
                mScrollPointerId = ev.getPointerId(actionIndex);
                mInitialTouchX = mLastTouchX = (int) (ev.getX(actionIndex) + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (ev.getY(actionIndex) + 0.5f);
                break;

            case MotionEvent.ACTION_MOVE:
                final int index = ev.findPointerIndex(mScrollPointerId);
                if (index < 0) {
                    Log.e(TAG, "Error processing scroll; pointer index for id " +
                            mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                    return false;
                }

                final int x = (int) (ev.getX(index) + 0.5f);
                final int y = (int) (ev.getY(index) + 0.5f);

                if (getScrollState() != SCROLL_STATE_DRAGGING) {
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
                mNestedChild.stopNestedScroll();
            }
            break;

            case MotionEvent.ACTION_CANCEL: {
                cancelTouch();
            }
            break;
        }
        return getScrollState() == SCROLL_STATE_DRAGGING || ((NestedScrollCallback) mCallback).forceInterceptTouchEvent(this, ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final boolean canScrollHorizontally = mCallback.canScrollHorizontally(getTarget());
        final boolean canScrollVertically = mCallback.canScrollVertically(getTarget());

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        boolean eventAddedToVelocityTracker = false;

        final MotionEvent vtev = MotionEvent.obtain(event);
        final int action = MotionEventCompat.getActionMasked(event);
        final int actionIndex = MotionEventCompat.getActionIndex(event);

        final NestedScrollCallback mCallback = (NestedScrollCallback) this.mCallback;
        final int mTouchSlop = getTouchSlop();

        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsets[0] = mNestedOffsets[1] = 0;
        }
        vtev.offsetLocation(mNestedOffsets[0], mNestedOffsets[1]);

        switch (action) {

            case MotionEvent.ACTION_DOWN: {
                if (mCallback.shouldStopScrollOnActionDown()) {
                    stopScrollerInternal();
                }
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
                mNestedChild.startNestedScroll(nestedScrollAxis);
            }
            break;

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                mScrollPointerId = event.getPointerId(actionIndex);
                mInitialTouchX = mLastTouchX = (int) (event.getX(actionIndex) + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (event.getY(actionIndex) + 0.5f);
            }
            break;

            case MotionEvent.ACTION_MOVE: {
                //we should follow the nested standard of Google.
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

                if (mNestedChild.dispatchNestedPreScroll(dx, dy, mScrollConsumed, mScrollOffset)) {
                    dx -= mScrollConsumed[0];
                    dy -= mScrollConsumed[1];
                    vtev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
                    // Updated the nested offsets
                    mNestedOffsets[0] += mScrollOffset[0];
                    mNestedOffsets[1] += mScrollOffset[1];
                }

                if (getScrollState() != SCROLL_STATE_DRAGGING) {
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

                if (getScrollState() == SCROLL_STATE_DRAGGING) {
                    mLastTouchX = x - mScrollOffset[0];
                    mLastTouchY = y - mScrollOffset[1];
                    if (nestedScrollBy(dx, dy, vtev)) {
                        getTarget().getParent().requestDisallowInterceptTouchEvent(true);
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
                mVelocityTracker.computeCurrentVelocity(1000, getMaxFlingVelocity());
                final float xvel = (int) mVelocityTracker.getXVelocity();
                final float yvel = (int) mVelocityTracker.getYVelocity();
                // final float xvel = ? -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mScrollPointerId) : 0;
                if (!((xvel != 0 || yvel != 0) && fling(xvel, yvel))) {
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

    @Override
    public boolean nestedScrollBy(int dx, int dy, MotionEvent ev) {
        mScrollConsumed[0] = 0;
        mScrollConsumed[1] = 0;

        //here we dispatch scroll later.
        nestedScroll(dx, dy, mScrollConsumed , false);
        final int consumedX = mScrollConsumed[0];
        final int consumedY = mScrollConsumed[1];

        final int unconsumedX = dx - consumedX;
        final int unconsumedY = dy - consumedY;

        if (mNestedChild.dispatchNestedScroll(mScrollConsumed[0], mScrollConsumed[1], unconsumedX, unconsumedY, mScrollOffset)) {
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
      /* if (!awakenScrollBars()) {
           getTarget().invalidate();
       }*/
        return mScrollConsumed[0] != 0 || mScrollConsumed[1] != 0;
    }

    @Override
    public int[] nestedScroll(int dx, int dy, int[] consumed, boolean dispatchScroll) {

        //gesture up dy >0 , gesture down dy < 0
        if (consumed == null) {
            consumed = new int[2];
        }
        // Logger.i(TAG, "scrollInternal", "dx = " + dx + ",dy = " + dy + " ,consumed = " + Arrays.toString(consumed));
        final View target = getTarget();
        final ScrollCallback mCallback = this.mCallback;
        final int maxX = mCallback.getMaximumXScrollDistance(target);
        final int maxY = mCallback.getMaximumYScrollDistance(target);
        final int scrollX = target.getScrollX();  // >0
        final int scrollY = target.getScrollY();

        int by = 0;
        if (mCallback.canScrollVertically(target)) {
            if (dy > 0) {
                //gesture up
                if (scrollY < maxY) {
                    int maxH = maxY - scrollY;
                    by = consumed[1] = Math.min(dy, maxH);
                } else {
                    //ignore
                }
            } else {
                //gesture down
                if (scrollY > 0) {
                    by = consumed[1] = -Math.min(Math.abs(dy), scrollY);
                }
            }
        }

        int bx = 0;
        if (mCallback.canScrollHorizontally(target)) {
            if (dx > 0) {
                //gesture left
                //only cal scroll in maxX
                if (scrollX < maxX) {
                    bx = consumed[0] = Math.min(dx, maxX - scrollX);
                }
            } else {
                //gesture right
                if (scrollX > 0) {
                    by = consumed[0] = -Math.min(Math.abs(dx), scrollX);
                }
            }
        }
        scrollBy(bx, by);
        if(dispatchScroll){
            if (bx != 0 || by != 0) {
                dispatchOnScrolled(bx, by);
            }
        }
        return consumed;
    }

    @Override
    protected boolean onFling(boolean canScrollHorizontal, boolean canScrollVertical, float velocityX, float velocityY) {
        if (!mNestedChild.dispatchNestedPreFling(velocityX, velocityY)) {
            final boolean canScroll = canScrollHorizontal || canScrollVertical;
            mNestedChild.dispatchNestedFling(velocityX, velocityY, canScroll);

            if (canScroll) {
                setScrollState(SCROLL_STATE_SETTLING);
                final float mMaxFlingVelocity = getMaxFlingVelocity();
                final View mTarget = getTarget();

                velocityX = Math.max(-mMaxFlingVelocity, Math.min(velocityX, mMaxFlingVelocity));
                velocityY = Math.max(-mMaxFlingVelocity, Math.min(velocityY, mMaxFlingVelocity));
                //mScroller.fling(0, getScrollY(), velocityX, velocityY, 0, 0, 0, mTopViewHeight);
                getScroller().fling(mTarget.getScrollX(), mTarget.getScrollY(), (int) velocityX, (int) velocityY,
                        0, canScrollHorizontal ? mCallback.getMaximumXScrollDistance(mTarget) : 0,
                        0, canScrollVertical ? mCallback.getMaximumYScrollDistance(mTarget) : 0
                );
                ViewCompat.postInvalidateOnAnimation(mTarget);
                return true;
            }
        }
        return false;
    }

    /**
     * called in {@link #setEnableNestedScroll(boolean)}, when the nested scroll enable state changed.
     * @param enable true to enable
     */
    protected void onNestedScrollStateChanged(boolean enable) {

    }

    protected void cancelTouch() {
      /*  if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }*/
        resetTouch();
        setScrollState(SCROLL_STATE_IDLE);
    }

    protected void resetTouch() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
        mNestedChild.stopNestedScroll();
        //TODO releaseGlows();
    }

    protected void onPointerUp(MotionEvent e) {
        final int actionIndex = MotionEventCompat.getActionIndex(e);
        if (e.getPointerId(actionIndex) == mScrollPointerId) {
            // Pick a new pointer to pick up the slack.
            final int newIndex = actionIndex == 0 ? 1 : 0;
            mScrollPointerId = e.getPointerId(newIndex);
            mInitialTouchX = mLastTouchX = (int) (e.getX(newIndex) + 0.5f);
            mInitialTouchY = mLastTouchY = (int) (e.getY(newIndex) + 0.5f);
        }
    }

    /**
     * the callback of {@link NestedScrollHelper}.
     */
    public static abstract class NestedScrollCallback extends ScrollCallback {
        /**
         * should stop scroll on the down event
         *
         * @return true if should stop scroll on the down event.
         */
        public boolean shouldStopScrollOnActionDown() {
            return false;
        }

        /**
         * force intercept the touch event.
         *
         * @param helper the nested scroll helper.
         * @param ev     the touch event
         * @return true to force intercept touch event.
         */
        public boolean forceInterceptTouchEvent(NestedScrollHelper helper, MotionEvent ev) {

            return false;
        }
    }
}
