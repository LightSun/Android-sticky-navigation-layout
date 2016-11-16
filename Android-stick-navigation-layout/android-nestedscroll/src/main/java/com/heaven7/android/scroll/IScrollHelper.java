package com.heaven7.android.scroll;

import android.view.View;

/**
 * the scroll helper. this define a specification of scroll. help to handle the scroll.
 * Created by heaven7 on 2016/11/15.
 */
public interface IScrollHelper {

    /**
     * The view is not currently scrolling.
     */
    int SCROLL_STATE_IDLE = 0;

    /**
     * The view is currently being dragged by outside input such as user touch input.
     */
    int SCROLL_STATE_DRAGGING = 1;

    /**
     * The view is currently animating to a final position while not under
     * outside control.
     */
    int SCROLL_STATE_SETTLING = 2;

    /**
     * set the scroll state.
     * @param state the target scroll state. must be one of {@link #SCROLL_STATE_IDLE} or {@link #SCROLL_STATE_DRAGGING} or {@link #SCROLL_STATE_SETTLING}.
     */
    void setScrollState(int state);

    /**
     * dispatch the scroll distance changed.
     * @param dx the amount of pixels to scroll by horizontally
     * @param dy the amount of pixels to scroll by vertically
     */
    void dispatchOnScrolled(int dx, int dy);

    /**
     * get the scroll state, which is set by call {@link #setScrollState(int)}.
     *  @return {@link IScrollHelper#SCROLL_STATE_IDLE}, {@link IScrollHelper#SCROLL_STATE_DRAGGING} or
     * {@link IScrollHelper#SCROLL_STATE_SETTLING}
     */
    int getScrollState();

    /**
     * Move the scrolled position of your view. This will cause a call to
     * {@link View#onScrollChanged(int, int, int, int)} and the view will be
     * invalidated.
     * @param dx the amount of pixels to scroll by horizontally
     * @param dy the amount of pixels to scroll by vertically
     */
    void scrollBy(int dx, int dy);

    /**
     * Set the scrolled position of your view. This will cause a call to
     * {@link View#onScrollChanged(int, int, int, int)} and the view will be
     * invalidated.
     * @param x the x position to scroll to
     * @param y the y position to scroll to
     */
    void scrollTo(int x, int y);

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     */
    void smoothScrollBy(int dx, int dy);

    /**
     * Like {@link View#scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     */
    void smoothScrollTo(int x, int y);


    /**
     * Stop any current scroll in progress, such as one started by
     * {@link #smoothScrollBy(int, int)}, {@link #fling(float, float)} or a touch-initiated fling.
     */
    void stopScroll();

    /**
     * override method of {@link View#computeScroll()}. you should call this method in it.
     */
    void computeScroll();

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
    boolean fling(float velocityX, float velocityY);


    /**
     * add a scroll change listener.
     * @param l the OnScrollChangeListener
     */
    void addOnScrollChangeListener(OnScrollChangeListener l);

    /**
     * remove a scroll change listener.
     * @param l the OnScrollChangeListener
     */
    void removeOnScrollChangeListener(OnScrollChangeListener l);

    /**
     * judge if has the target OnScrollChangeListener.
     * @param l the OnScrollChangeListener
     * @return true if has the target OnScrollChangeListener
     */
    boolean hasOnScrollChangeListener(OnScrollChangeListener l);

    /**
     * on  scroll change listener.
     */
    interface OnScrollChangeListener {

        /**
         * called when the scroll state change
         *
         * @param target         the target view
         * @param state          the scroll state . see {@link #SCROLL_STATE_IDLE} and etc.
         */
        void onScrollStateChanged(View target, int state);

        /**
         * Callback method to be invoked when the view has been scrolled. This will be
         * called after the scroll has completed.
         * <p>
         * This callback will also be called if visible item range changes after a layout
         * calculation. In that case, dx and dy will be 0.
         *
         * @param target the target view
         * @param dx  The amount of horizontal scroll.
         * @param dy  The amount of vertical scroll.
         */
        void onScrolled(View target, int dx, int dy);
    }

}
