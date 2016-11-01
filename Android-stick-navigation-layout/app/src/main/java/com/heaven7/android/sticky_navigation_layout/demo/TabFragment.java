package com.heaven7.android.sticky_navigation_layout.demo;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.heaven7.adapter.BaseSelector;
import com.heaven7.adapter.QuickRecycleViewAdapter;
import com.heaven7.android.StickyLayout.StickyNavigationLayout;
import com.heaven7.core.util.ViewHelper;

import java.util.ArrayList;
import java.util.List;

public class TabFragment extends Fragment implements StickyNavigationLayout.IStickyDelegate {

    public static final String TITLE = "title";

    private String mTitle = "Defaut Value";
    private RecyclerView mListView;
    private List<Data> mDatas = new ArrayList<Data>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTitle = getArguments().getString(TITLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab, container, false);
        mListView = (RecyclerView) view.findViewById(R.id.rv);
        mListView.setLayoutManager(new LinearLayoutManager(container.getContext()));

        final SwipeRefreshLayout swipeView = (SwipeRefreshLayout) view.findViewById(R.id.swipe);
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeView.setRefreshing(false);
                    }
                }, 2000);
            }
        });
        for (int i = 0; i < 50; i++) {
            mDatas.add(new Data(mTitle + " -> " + i));
        }
        mListView.setAdapter(new QuickRecycleViewAdapter<Data>(R.layout.item, mDatas) {
            @Override
            protected void onBindData(Context context, int position, Data item, int itemLayoutId, ViewHelper helper) {
                helper.setText(R.id.id_info, item.title)
                        .setRootOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Snackbar.make(v, R.string.action_settings, Snackbar.LENGTH_LONG).show();
                            }
                        });
            }
        });
        return view;

    }

    public static TabFragment newInstance(String title) {
        TabFragment tabFragment = new TabFragment();
        Bundle bundle = new Bundle();
        bundle.putString(TITLE, title);
        tabFragment.setArguments(bundle);
        return tabFragment;
    }

    @Override
    public boolean shouldIntercept(StickyNavigationLayout snv, int dy, int topViewState) {
        final int position = MainActivity.findFirstVisibleItemPosition(mListView);
        final View child = mListView.getChildAt(position);
        boolean isTopHidden = topViewState == StickyNavigationLayout.VIEW_STATE_HIDE;
        if (!isTopHidden || (child != null && child.getTop() == 0 && dy > 0)) {
            //listview 滑动到顶部，并且要继续向下滑动时，拦截触摸
            return true;
        }
        return false;
    }

    @Override
    public void scrollBy(int dy) {
        mListView.scrollBy(0, -dy);
    }

    private static class Data extends BaseSelector {
        String title;

        public Data(String title) {
            this.title = title;
        }
    }

}
