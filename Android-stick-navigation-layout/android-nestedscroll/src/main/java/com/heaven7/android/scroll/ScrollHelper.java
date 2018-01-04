package com.heaven7.android.scroll;

import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.OverScroller;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <p>
 * this class is a simple implement of {@link IScrollHelper}. it can do most work of scroller.
 * such as {@link IScrollHelper#smoothScrollTo(int, int)}, {@link IScrollHelper#smoothScrollBy(int, int)} and etc.
 * </p>
 * Created by heaven7 on 2016/11/14.
 */
public class ScrollHelper implements IScrollHelper {

    /*protected*/ static final boolean DEBUG = false;

    private static final long ANIMATED_SCROLL_GAP = 250;

    private CopyOnWriteArrayList<OnScrollChangeListener> mScrollListeners;
    private final OverScroller mScroller;
    protected final ScrollCallback mCallback;
    protected final String mTag;
    private final View mTarget;

    private final int mTouchSlop;
    private final float mMinFlingVelocity;
    private final float mMaxFlingVelocity;

    private long mLastScroll;
    private int mScrollState = SCROLL_STATE_IDLE;

    /**
     * create a ScrollHelper.
     *
     * @param target   the target view
     * @param scroller the over Scroller
     * @param callback the callback
     */
    public ScrollHelper(View target, OverScroller scroller, ScrollCallback callback) {
        this(target, 1, scroller, callback);
    }

    /**
     * create a ScrollHelper.
     *
     * @param target      the target view
     * @param sensitivity Multiplier for how sensitive the helper should be about detecting
     *                    the start of a drag. Larger values are more sensitive. 1.0f is normal.
     * @param scroller    the over Scroller
     * @param callback    the callback
     */
    public ScrollHelper(View target, float sensitivity, OverScroller scroller, ScrollCallback callback) {
        Util.check(target, "target view can't be null.");
        Util.check(scroller, null);
        Util.check(callback, "ScrollCallback can't be null");
        final ViewConfiguration vc = ViewConfiguration.get(target.getContext());
        this.mTag = target.getClass().getSimpleName();
        this.mTarget = target;
        this.mCallback = callback;
        this.mScroller = scroller;
        this.mTouchSlop = (int) (vc.getScaledTouchSlop() * (1 / sensitivity));
        this.mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        this.mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
    }

    public OverScroller getScroller() {
        return mScroller;
    }

    public int getTouchSlop() {
        return mTouchSlop;
    }

    public float getMinFlingVelocity() {
        return mMinFlingVelocity;
    }

    public float getMaxFlingVelocity() {
        return mMaxFlingVelocity;
    }

    public View getTarget() {
        return mTarget;
    }

    @Override
    public void dispatchOnScrolled(int dx, int dy) {
        // Pass the current scrollX/scrollY values; no actual change in these properties occurred
        // but some general-purpose code may choose to respond to changes this way.
       /* final int scrollX = mTarget.getScrollX();
        final int scrollY = mTarget.getScrollY();
        mTarget.onScrollChanged(scrollX, scrollY, scrollX, scrollY);*/

        // Invoke listeners last. Subclassed view methods always handle the event first.
        // All internal state is consistent by the time listeners are invoked.
        if (mScrollListeners != null && mScrollListeners.size() > 0) {
            for (OnScrollChangeListener l : mScrollListeners) {
                if (l != null) {
                    l.onScrolled(mTarget, dx, dy);
                }
            }
        }
        // Pass the real deltas to onScrolled, the RecyclerView-specific method.
        onScrolled(dx, dy);
    }

    /**
     * Called when the scroll position of this view changes. Subclasses should use
     * this method to respond to scrolling within the adapter's data set instead of an explicit
     * listener. this is called in {@link #dispatchOnScrolled(int, int)}.
     * <p/>
     * <p>This method will always be invoked before listeners. If a subclass needs to perform
     * any additional upkeep or bookkeeping after scrolling but before listeners run,
     * this is a good place to do so.</p>
     *
     * @param dx horizontal distance scrolled in pixels
     * @param dy vertical distance scrolled in pixels
     */
    protected void onScrolled(int dx, int dy) {
        mCallback.onScrolled(dx, dy);
    }

    @Override
    public int getScrollState() {
        return mScrollState;
    }

    @Override
    public void setScrollState(int state) {
        if (state == mScrollState) {
            return;
        }
        if (DEBUG) {
            Log.d(mTag, "setting scroll state to " + state + " from " + mScrollState,
                    new Exception());
        }
        mScrollState = state;
        if (state != SCROLL_STATE_SETTLING) {
            stopScrollerInternal();
        }
        dispatchOnScrollStateChanged(state);
    }

    protected void stopScrollerInternal() {
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
    }

    /**
     * dispatch the scroll state change, this is called in {@link #setScrollState(int)}.
     *
     * @param state the target scroll state.
     */
    protected void dispatchOnScrollStateChanged(int state) {
        if (mScrollListeners != null && mScrollListeners.size() > 0) {
            for (OnScrollChangeListener l : mScrollListeners) {
                if (l != null) {
                    l.onScrollStateChanged(mTarget, state);
                }
            }
        }
    }

    @Override
    public void scrollBy(int dx, int dy) {
        scrollTo(mTarget.getScrollX() + dx, mTarget.getScrollY() + dy);
    }

    /**
     * {@inheritDoc}.  Note: this is  similar to {@link View#scrollTo(int, int)}, but limit the range of scroll,
     * which is indicate by {@link ScrollCallback#getMaximumXScrollDistance(View)} with {@link ScrollCallback#getMaximumYScrollDistance(View)}.
     *
     * @param x the x position to scroll to
     * @param y the y position to scroll to
     */
    @Override
    public void scrollTo(int x, int y) {
        mTarget.scrollTo(Math.min(x, mCallback.getMaximumXScrollDistance(mTarget)),
                Math.min(y, mCallback.getMaximumYScrollDistance(mTarget)));
    }

    @Override
    public void smoothScrollBy(int dx, int dy) {
        if (mTarget instanceof ViewGroup && ((ViewGroup) mTarget).getChildCount() == 0) {
            // Nothing to do.
            return;
        }
        long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
        if (duration > ANIMATED_SCROLL_GAP) {
            // design from scrollView
            final int scrollX = mTarget.getScrollX();
            final int scrollY = mTarget.getScrollY();
            final int maxX = mCallback.getMaximumXScrollDistance(mTarget);
            final int maxY = mCallback.getMaximumYScrollDistance(mTarget);
            if ((scrollX + dx) > maxX) {
                dx -= scrollX + dx - maxX;
            }
            if ((scrollY + dy) > maxY) {
                dy -= scrollY + dy - maxY;
            }
            setScrollState(SCROLL_STATE_SETTLING);
            mScroller.startScroll(scrollX, scrollY, dx, dy);
            ViewCompat.postInvalidateOnAnimation(mTarget);
        } else {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            mTarget.scrollBy(dx, dy);
        }
        mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }

    @Override
    public final void smoothScrollTo(int x, int y) {
        smoothScrollBy(x - mTarget.getScrollX(), y - mTarget.getScrollY());
    }

    @Override
    public void stopScroll() {
        setScrollState(SCROLL_STATE_IDLE);
        stopScrollerInternal();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {//true if not finish
            if(DEBUG){
                Log.i(mTag, "computeScroll: scroll not finished: currX = " + mScroller.getCurrX()
                        + " ,currY = " + mScroller.getCurrY());
            }
            mTarget.scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            ViewCompat.postInvalidateOnAnimation(mTarget);
        }
    }

    @Override
    public boolean fling(float velocityX, float velocityY) {
        final boolean canScrollHorizontal = mCallback.canScrollHorizontally(mTarget);
        final boolean canScrollVertical = mCallback.canScrollVertically(mTarget);

        if (!canScrollHorizontal || Math.abs(velocityX) < mMinFlingVelocity) {
            velocityX = 0;
        }
        if (!canScrollVertical || Math.abs(velocityY) < mMinFlingVelocity) {
            velocityY = 0;
        }
        if (velocityX == 0 && velocityY == 0) {
            // If we don't have any velocity, return false
            return false;
        }
        return onFling(canScrollHorizontal, canScrollVertical, velocityX, velocityY);
    }

    @Override
    public void addOnScrollChangeListener(OnScrollChangeListener l) {
        if (mScrollListeners == null) {
            mScrollListeners = new CopyOnWriteArrayList<>();
        }
        mScrollListeners.add(l);
    }

    @Override
    public void removeOnScrollChangeListener(OnScrollChangeListener l) {
        if (mScrollListeners != null) {
            mScrollListeners.remove(l);
        }
    }

    @Override
    public boolean hasOnScrollChangeListener(OnScrollChangeListener l) {
        return mScrollListeners != null && mScrollListeners.contains(l);
    }

    /**
     * do fling , this method is called in {@link #fling(float, float)}
     *
     * @param canScrollHorizontal if can scroll in Horizontal
     * @param canScrollVertical   if can scroll in Vertical
     * @param velocityX           the velocity of X
     * @param velocityY           the velocity of y
     * @return true if the fling was started.
     */
    protected boolean onFling(boolean canScrollHorizontal, boolean canScrollVertical, float velocityX, float velocityY) {
        if (canScrollHorizontal || canScrollVertical) {
            setScrollState(SCROLL_STATE_SETTLING);
            velocityX = Math.max(-mMaxFlingVelocity, Math.min(velocityX, mMaxFlingVelocity));
            velocityY = Math.max(-mMaxFlingVelocity, Math.min(velocityY, mMaxFlingVelocity));

            mScroller.fling(mTarget.getScrollX(), mTarget.getScrollY(), (int) velocityX, (int) velocityY,
                    0, canScrollHorizontal ? mCallback.getMaximumXScrollDistance(mTarget) : 0,
                    0, canScrollVertical ? mCallback.getMaximumYScrollDistance(mTarget) : 0
            );
            //TODO why recyclerView use mScroller.fling(0, 0, (int)velocityX, (int)velocityY,Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            ViewCompat.postInvalidateOnAnimation(mTarget);
            return true;
        }
        return false;
    }

    /**
     * get the scroll state as string log.
     * @param state the scroll state.
     * @return the state as string
     */
    public static String getScrollStateString(int state) {
        switch (state) {
            case SCROLL_STATE_DRAGGING:
                return "SCROLL_STATE_DRAGGING";

            case SCROLL_STATE_SETTLING:
                return "SCROLL_STATE_SETTLING";

            case SCROLL_STATE_IDLE:
                return "SCROLL_STATE_IDLE";

            default:
                return "unknown state";
        }
    }


    /**
     * the scroll callback of {@link ScrollHelper}.
     */
    public static abstract class ScrollCallback {

        /**
         * if can scroll in Horizontal
         *
         * @param target the target view.
         * @return true if can scroll in Horizontal
         */
        public abstract boolean canScrollHorizontally(View target);

        /**
         * if can scroll in Vertical
         *
         * @param target the target view.
         * @return true if can scroll in Vertical
         */
        public abstract boolean canScrollVertically(View target);

        /**
         * get the maximum x scroll distance of the target view.
         *
         * @param target the target view.
         * @return the maximum x scroll distance
         */
        public int getMaximumXScrollDistance(View target) {
            return target.getWidth();
        }

        /**
         * get the maximum y scroll distance of the target view.
         *
         * @param target the target view.
         * @return the maximum y scroll distance
         */
        public int getMaximumYScrollDistance(View target) {
            return target.getHeight();
        }

        /**
         * called in {@link ScrollHelper#dispatchOnScrolled(int, int)}.
         *
         * @param dx the delta x
         * @param dy the delta y
         */
        public void onScrolled(int dx, int dy) {

        }

    }
}
