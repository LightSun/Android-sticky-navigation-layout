package com.heaven7.android.scroll;

import android.support.v4.view.NestedScrollingChild;
import android.view.View;
import android.widget.OverScroller;

/**
 * the scroll factory of {@link ScrollHelper} and  {@link NestedScrollHelper}.
 * Created by heaven7 on 2016/11/15.
 */
public class NestedScrollFactory {

    public static ScrollHelper create(View target, float sensitivity, OverScroller scroller, ScrollHelper.ScrollCallback callback){
        return new ScrollHelper(target , sensitivity, scroller, callback);
    }
    public static ScrollHelper create(View target, OverScroller scroller, ScrollHelper.ScrollCallback callback){
        return new ScrollHelper(target , 1, scroller, callback);
    }
    public static ScrollHelper create(View target, ScrollHelper.ScrollCallback callback){
        return new ScrollHelper(target , 1, new OverScroller(target.getContext()), callback);
    }

    public static NestedScrollHelper create(View target, float sensitivity, OverScroller scroller, NestedScrollingChild child,
                                      NestedScrollHelper.NestedScrollCallback callback) {
        return new NestedScrollHelper(target , sensitivity, scroller, child , callback);
    }
    public static NestedScrollHelper create(View target, OverScroller scroller, NestedScrollHelper.NestedScrollCallback callback) {
        return new NestedScrollHelper(target , 1, scroller, (NestedScrollingChild) target, callback);
    }
    public static NestedScrollHelper create(View target, NestedScrollHelper.NestedScrollCallback callback) {
        return new NestedScrollHelper(target , 1, new OverScroller(target.getContext()), (NestedScrollingChild) target, callback);
    }
}
