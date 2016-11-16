package com.heaven7.android.scroll;

import android.support.v4.view.NestedScrollingChild;
import android.view.View;
import android.widget.OverScroller;

/**
 * the scroll factory of {@link ScrollHelper} and  {@link NestedScrollHelper}.
 * Created by heaven7 on 2016/11/15.
 */
public class NestedScrollFactory {

    /**
     * create a ScrollHelper.
     * @param target      the target view
     * @param sensitivity Multiplier for how sensitive the helper should be about detecting
     *                    the start of a drag. Larger values are more sensitive. 1.0f is normal.
     * @param scroller    the over Scroller
     * @param callback    the callback
     */
    public static ScrollHelper create(View target, float sensitivity, OverScroller scroller, ScrollHelper.ScrollCallback callback){
        return new ScrollHelper(target , sensitivity, scroller, callback);
    }
    /**
     * create a ScrollHelper.
     * @param target      the target view
     * @param scroller    the over Scroller
     * @param callback    the callback
     */
    public static ScrollHelper create(View target, OverScroller scroller, ScrollHelper.ScrollCallback callback){
        return new ScrollHelper(target , 1, scroller, callback);
    }
    /**
     * create a ScrollHelper and use the default {@link OverScroller]} with default the interpolator.
     * @param target      the target view
     * @param callback    the callback
     */
    public static ScrollHelper create(View target, ScrollHelper.ScrollCallback callback){
        return new ScrollHelper(target , 1, new OverScroller(target.getContext()), callback);
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
    public static NestedScrollHelper create(View target, float sensitivity, OverScroller scroller, NestedScrollingChild child,
                                      NestedScrollHelper.NestedScrollCallback callback) {
        return new NestedScrollHelper(target , sensitivity, scroller, child , callback);
    }
    /**
     * create the nested scroll helper, but the target view must implements {@link NestedScrollingChild}.
     * @param target  the target view
     * @param scroller  the scroller
     * @param callback the callback
     */
    public static NestedScrollHelper create(View target, OverScroller scroller, NestedScrollHelper.NestedScrollCallback callback) {
        return new NestedScrollHelper(target , 1, scroller, (NestedScrollingChild) target, callback);
    }
    /**
     * create the nested scroll helper, use default {@link OverScroller} , but the target view must implements {@link NestedScrollingChild}.
     * @param target  the target view
     * @param callback the callback
     */
    public static NestedScrollHelper create(View target, NestedScrollHelper.NestedScrollCallback callback) {
        return new NestedScrollHelper(target , 1, new OverScroller(target.getContext()), (NestedScrollingChild) target, callback);
    }
}
