package com.heaven7.android.scroll;

import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.VelocityTrackerCompat;
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

    private final NestedScrollingChild mNestedChild;

    private final int[] mScrollConsumed = new int[2];
    private final int[] mNestedOffsets = new int[2];
    private final int[] mScrollOffset = new int[2];
    private int mScrollPointerId;

    protected final int[] mTempXY = new int[2];

    private VelocityTracker mVelocityTracker;
    private int mInitialTouchX;
    private int mLastTouchX;
    private int mInitialTouchY;
    private int mLastTouchY;

    /**
     * create the nested scroll helper. But,the target view must implements interface {@link NestedScrollingChild}.
     * @param target  the target view
     * @param scroller  the scroller
     * @param callback the callback
     */
    public NestedScrollHelper(View target, OverScroller scroller, NestedScrollCallback callback) {
        this(target, 1, scroller, (NestedScrollingChild) target, callback);
    }

    /**
     * create the nested scroll helper.
     * @param target  the target view
     * @param sensitivity Multiplier for how sensitive the helper should be about detecting
     *                    the start of a drag. Larger values are more sensitive. 1.0f is normal.
     * @param scroller  the scroller
     * @param child the NestedScrollingChild.
     * @param callback the callback
     */
    public NestedScrollHelper(View target, float sensitivity, OverScroller scroller, NestedScrollingChild child, NestedScrollCallback callback) {
        super(target, sensitivity, scroller, callback);
        Util.check(child);
        this.mNestedChild = child;
    }

    @Override
    public void setNestedScrollingEnabled(boolean enable) {
        if (mNestedChild.isNestedScrollingEnabled() != enable) {
            mNestedChild.setNestedScrollingEnabled(enable);
            onNestedScrollStateChanged(enable);
        }
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedChild.isNestedScrollingEnabled();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
        boolean canScrollHorizontally = mCallback.canScrollHorizontally(getTarget());
        boolean canScrollVertically = mCallback.canScrollVertically(getTarget());

        final int action = ev.getActionMasked();
        final int actionIndex = ev.getActionIndex();
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

            case MotionEvent.ACTION_POINTER_DOWN:
                mScrollPointerId = ev.getPointerId(actionIndex);
                mInitialTouchX = mLastTouchX = (int) (ev.getX(actionIndex) + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (ev.getY(actionIndex) + 0.5f);
                break;

            case MotionEvent.ACTION_MOVE:
                final int index = ev.findPointerIndex(mScrollPointerId);
                if (index < 0) {
                    Log.e(mTag, "Error processing scroll; pointer index for id " +
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

            case MotionEvent.ACTION_POINTER_UP: {
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
        final int action = event.getActionMasked();
        final int actionIndex = event.getActionIndex();

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

            case MotionEvent.ACTION_POINTER_DOWN: {
                mScrollPointerId = event.getPointerId(actionIndex);
                mInitialTouchX = mLastTouchX = (int) (event.getX(actionIndex) + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (event.getY(actionIndex) + 0.5f);
            }
            break;

            case MotionEvent.ACTION_MOVE: {
                //we should follow the nested standard of Google.
                final int index = event.findPointerIndex(mScrollPointerId);
                if (index < 0) {
                    Log.e(mTag, "Error processing scroll; pointer index for id " +
                            mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                    return false;
                }
                final int x = (int) (event.getX(index) + 0.5f);
                final int y = (int) (event.getY(index) + 0.5f);
                int dx = mLastTouchX - x;
                int dy = mLastTouchY - y;

              /*  if(DEBUG) {
                    Log.i(mTag, "onTouchEvent: " + String.format("before --- dispatchNestedPreScroll ----  (dx = %d ,dy = %d )", dx, dy));
                }*/

                if (mNestedChild.dispatchNestedPreScroll(dx, dy, mScrollConsumed, mScrollOffset)) {
                    /*if(DEBUG) {
                        Log.i(mTag, "onTouchEvent_dispatchNestedPreScroll: parent consumed: " + Arrays.toString(mScrollConsumed));
                    }*/
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

            case MotionEvent.ACTION_POINTER_UP: {
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
                final float xvel = canScrollHorizontally ?
                        -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mScrollPointerId) : 0;
                final float yvel = canScrollVertically ?
                        -VelocityTrackerCompat.getYVelocity(mVelocityTracker, mScrollPointerId) : 0;
            /*    final float xvel = VelocityTrackerCompat.getXVelocity(mVelocityTracker, mScrollPointerId);
                final float yvel =VelocityTrackerCompat.getYVelocity(mVelocityTracker, mScrollPointerId);*/
                // like recycler view
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
        nestedScroll(dx, dy, mScrollConsumed, false);
        final int consumedX = mScrollConsumed[0];
        final int consumedY = mScrollConsumed[1];

        final int unconsumedX = dx - consumedX;
        final int unconsumedY = dy - consumedY;

        if (DEBUG) {
            Log.i(mTag, "nestedScrollBy: before dispatchNestedScroll--> consumedX = " + consumedX + " ,consumedY = " + consumedY +
                    " ,unconsumedX = " + unconsumedX + " ,unconsumedY = " + unconsumedY);
        }
        if (mNestedChild.dispatchNestedScroll(consumedX, consumedY, unconsumedX, unconsumedY, mScrollOffset)) {
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
        return consumedX != 0 || consumedY != 0;
    }

    @Override
    public int[] nestedScroll(int dx, int dy, int[] consumed, boolean dispatchScroll) {

        //gesture up dy >0 , gesture down dy < 0
        if (consumed == null) {
            consumed = new int[2];
        }
        // Logger.i(TAG, "scrollInternal", "dx = " + dx + ",dy = " + dy + " ,consumed = " + Arrays.toString(consumed));

        getScrollXY(mTempXY);
        final View target = getTarget();
        final int maxX = mCallback.getMaximumXScrollDistance(target);
        final int maxY = mCallback.getMaximumYScrollDistance(target);

        final int scrollX = mTempXY[0];
        final int scrollY = mTempXY[1];

        // isNestedScrollingEnabled()
        /** parent scroll done(but child's scroll x and y is zero.) , child can continue scroll ? #getScrollXY(mTempXY); can resolve it.
         * see this error log:
         I/StickyNavigationLayout: called [ nestedScroll() ]: (scrollX = 0 ,scrollY = 525, maxX = 1080 ,maxY = 525)
         I/StickyNavigationLayout: called [ nestedScroll() ]: (scrollX = 0 ,scrollY = 525, maxX = 1080 ,maxY = 525)
         I/NestedScrollFrameLayout: called [ nestedScroll() ]: (scrollX = 0 ,scrollY = 0, maxX = 1080 ,maxY = 262)
         I/NestedScrollFrameLayout: called [ nestedScrollBy() ]: before dispatchNestedScroll--> consumedX = 0 ,consumedY = 1 ,unconsumedX = 15 ,unconsumedY = 0
         */
        if (DEBUG) {
            Log.i(mTag, "nestedScroll: " + String.format("(scrollX = %d ,scrollY = %d, maxX = %d ,maxY = %d)",
                    scrollX, scrollY, maxX, maxY));
        }

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
        if (dispatchScroll) {
            if (bx != 0 || by != 0) {
                dispatchOnScrolled(bx, by);
            }
        }
        return consumed;
    }

    @Override
    protected boolean onFling(boolean canScrollHorizontal, boolean canScrollVertical, float velocityX, float velocityY) {
        if (DEBUG) {
            Log.i(mTag, "onFling:  "+"velocityX = " + velocityX + " ,velocityY = " + velocityY );
        }
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
               // getScrollXY(mTempXY);
                final int maxX;
                final int maxY;
                if(isNestedScrollingEnabled()){
                    final int parentScrollX = mTarget.getParent() != null ? ((View) mTarget.getParent()).getScrollX() : 0;
                    final int parentScrollY = mTarget.getParent() != null ? ((View) mTarget.getParent()).getScrollY() : 0;
                    maxX = parentScrollX == 0 ? mCallback.getMaximumXScrollDistance(mTarget): 0;
                    maxY = parentScrollY == 0 ? mCallback.getMaximumYScrollDistance(mTarget): 0;
                    if(DEBUG){
                        Log.i(mTag, "onFling: parentScrollX = " + parentScrollX + " ,parentScrollY = " + parentScrollY);
                    }
                }else{
                    maxX = mCallback.getMaximumXScrollDistance(mTarget);
                    maxY = mCallback.getMaximumYScrollDistance(mTarget);
                }
                if (DEBUG) {
                    Log.i(mTag, "onFling: after adjust , velocityX = " + velocityX + " ,velocityY = " + velocityY);
                    Log.i(mTag, "onFling: maxX = " + maxX + " ,maxY = " + maxY );
                }
                getScroller().fling(mTarget.getScrollX(), mTarget.getScrollY(), (int) velocityX, (int) velocityY,
                        0, canScrollHorizontal ? maxX : 0,
                        0, canScrollVertical ? maxY: 0
                );
                ViewCompat.postInvalidateOnAnimation(mTarget);
                return true;
            }
        }
        return false;
    }

    /**
     * get the scroll x and y of the current view.
     *
     * @param scrollXY the array of scroll x and y. can be null.
     * @return the array of scroll x and y.
     */
    protected int[] getScrollXY(int[] scrollXY) {
        if (scrollXY == null) {
            scrollXY = new int[2];
        }
        final View target = getTarget();
        final NestedScrollCallback mCallback = (NestedScrollCallback) this.mCallback;
        final int parentScrollX = target.getParent() != null ? ((View) target.getParent()).getScrollX() : 0;
        final int parentScrollY = target.getParent() != null ? ((View) target.getParent()).getScrollY() : 0;

        if (isNestedScrollingEnabled()) {
            scrollXY[0] = mCallback.adjustScrollX(target, parentScrollX, mCallback.getMaximumXScrollDistance(target));
            scrollXY[1] = mCallback.adjustScrollY(target, parentScrollY, mCallback.getMaximumYScrollDistance(target));
        } else {
            scrollXY[0] = target.getScrollX();
            scrollXY[1] = target.getScrollY();
        }
        return scrollXY;
    }

    /**
     * called in {@link #setNestedScrollingEnabled(boolean)} (boolean)}, when the nested scroll enable state changed.
     *
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
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
        mNestedChild.stopNestedScroll();
        //TODO releaseGlows();
    }

    protected void onPointerUp(MotionEvent e) {
        final int actionIndex = e.getActionIndex();
        if (e.getPointerId(actionIndex) == mScrollPointerId) {
            // Pick a new pointer to pick up the slack.
            final int newIndex = actionIndex == 0 ? 1 : 0;
            mScrollPointerId = e.getPointerId(newIndex);
            mInitialTouchX = mLastTouchX = (int) (e.getX(newIndex) + 0.5f);
            mInitialTouchY = mLastTouchY = (int) (e.getY(newIndex) + 0.5f);
        }
    }

    /**
     * the callback of {@link NestedScrollHelper}. this class help we handle the nested scrolling.
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

        /**
         * adjust the current scroll y of the target view. This give a chance to adjust it.
         * this is only called when {@link NestedScrollHelper#isNestedScrollingEnabled()} is true.
         * this if often used in {@link IScrollHelper#scrollBy(int, int)} ]}.
         * But, you should care about it when you want to change.
         *
         * @param target        the target view
         * @param parentScrollY the scroll y of the parent view
         * @param maxY          the max y . which comes from {@link #getMaximumYScrollDistance(View)}.
         * @return the current scroll y.
         */
        public int adjustScrollY(View target, int parentScrollY, int maxY) {
            return target.getScrollY() + parentScrollY;
        }

        /**
         * adjust the current scroll x of the target view. This give a chance to adjust it.
         * this is only called when {@link NestedScrollHelper#isNestedScrollingEnabled()} is true.
         * But, you should care about it when you want to change.
         *
         * @param target        the target view
         * @param parentScrollX the scroll x of the parent view
         * @param maxX          the max x . which comes from {@link #getMaximumXScrollDistance(View)}.
         * @return the current scroll x.
         */
        public int adjustScrollX(View target, int parentScrollX, int maxX) {
            return target.getScrollX() + parentScrollX;
        }
    }
}
