package com.heaven7.android.scroll;

import android.support.v4.view.NestedScrollingChild;
import android.view.MotionEvent;

/**
 * an expand interface of {@link IScrollHelper}
 * Created by heaven7 on 2016/11/15.
 */
public interface INestedScrollHelper extends IScrollHelper{


    /**
     * set to enable or disable the nested scroll.
     * @param enable true to enable
     */
    void setEnableNestedScroll(boolean enable);

    /**
     * is enabled the nested scroll.
     * @return true to enable.
     */
    boolean isEnabledNestedScroll();

    /**
     * similar to {@link android.view.ViewGroup#onInterceptTouchEvent(MotionEvent)} , but is a nested scroll implement of it.
     * @param ev the touch event
     * @return  true if should intercept.
     */
    boolean onInterceptTouchEvent(MotionEvent ev);

    /**
     * similar to {@link android.view.ViewGroup#onTouchEvent(MotionEvent)} , but is a nested scroll implement of it.
     * @param event the touch event
     * @return  true if handle the touch event.
     */
    boolean onTouchEvent(MotionEvent event);

    /**
     * Does not perform bounds checking. Used by internal methods that have already validated input.
     * <p/>
     * It is unlike the {@link #scrollBy(int, int)}, and it can communicate with {@link NestedScrollingChild}.
     * It also reports any unused scroll request to the related EdgeEffect (current not implements).
     *
     * @param dx The amount of horizontal scroll request
     * @param dy The amount of vertical scroll request
     * @param ev The originating MotionEvent, or null if not from a touch event.
     * @return Whether any scroll was consumed in either direction.
     */
    boolean nestedScrollBy(int dx, int dy, MotionEvent ev);

    /**
     * do scroll internal. this method is often called in {@link #nestedScrollBy(int, int, MotionEvent)}.
     *
     * @param dx       the delta x, may be negative
     * @param dy       the delta y , may be negative
     * @param consumed optional , in not null will contains the consumed x and y by this scroll.
     * @param dispatchScroll if scrolled , whether dispatch the scroll distance changed or not.
     * @return the consumed x and y as array by this scroll.
     */
    int[] nestedScroll(int dx, int dy, int[] consumed , boolean dispatchScroll);
}
