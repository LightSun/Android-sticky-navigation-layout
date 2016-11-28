package com.heaven7.android.sticky_navigation_layout.demo.extra;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

import com.heaven7.adapter.RecyclerViewUtils;
import com.heaven7.core.util.Logger;

public class OnScrollListenerImpl extends RecyclerView.OnScrollListener {

    private static final String TAG = "OnScrollListenerImpl";

    public void onScrollStateChanged(RecyclerView rv, int newState){
         //dragging-> setting->idle  and dragging->idle
         switch (newState){
             case RecyclerView.SCROLL_STATE_DRAGGING:
                 Logger.i(TAG, "SCROLL_STATE_DRAGGING");
                 break;

             case RecyclerView.SCROLL_STATE_SETTLING:
                 Logger.i(TAG, "SCROLL_STATE_SETTLING");
                // final int firstPos = findFirstVisibleItemPosition(rv);
                 break;

             case RecyclerView.SCROLL_STATE_IDLE :
                 Logger.i(TAG, "SCROLL_STATE_IDLE");
                 final int lastPos = RecyclerViewUtils.findLastVisibleItemPosition(rv);
                 if(lastPos == RecyclerView.NO_POSITION){
                     Logger.i(TAG, "onScrollStateChanged", "can't find last position of RecyclerView.");
                     return;
                 }
                 break;
         }
    }

    public void onScrolled(RecyclerView recyclerView, int dx, int dy){

    }

    public static int findFirstVisibleItemPosition(RecyclerView rv) {
        RecyclerView.LayoutManager lm = rv.getLayoutManager();
        int firstPos = RecyclerView.NO_POSITION;
        if (lm instanceof GridLayoutManager) {
            firstPos = ((GridLayoutManager) lm).findFirstVisibleItemPosition();

        } else if (lm instanceof LinearLayoutManager) {
            firstPos = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();

        } else if (lm instanceof StaggeredGridLayoutManager) {
            int positions[] = ((StaggeredGridLayoutManager) lm).findFirstVisibleItemPositions(null);
            for (int pos : positions) {
                if (pos < firstPos) {
                    firstPos = pos;
                }
            }
        }
        return firstPos;
    }

}
