package com.heaven7.android.sticky_navigation_layout.demo.demos;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.heaven7.adapter.QuickRecycleViewAdapter;
import com.heaven7.android.StickyLayout.StickyNavigationLayout;
import com.heaven7.android.sticky_navigation_layout.demo.R;
import com.heaven7.android.sticky_navigation_layout.demo.fragment.TabFragment;
import com.heaven7.android.sticky_navigation_layout.demo.view.SimpleViewPagerIndicator;
import com.heaven7.core.util.ViewHelper;

import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;

/**
 * this is a simple demo of sticky navigation layout..
 * Created by heaven7 on 2016/11/15.
 */
public class SimpleStickyNavigationDemo extends BaseActivity {

    @InjectView(R.id.stickyLayout)
    StickyNavigationLayout mStickyNavLayout;

    @InjectView(R.id.vp_indicator)
    SimpleViewPagerIndicator mIndicator;
    @InjectView(R.id.rv)
    RecyclerView mRv_subscribe;

    private final String[] mTitles = new String[] { "简介", "评价", "相关" };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        initEvents();
        initDatas();
    }

    private void initEvents() {
        //TODO fix StickyHeaderView
    }
    private void initDatas() {
        List<TabFragment.Data> mDatas = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            mDatas.add(new TabFragment.Data(" title -> " + i));
        }
        mRv_subscribe.setLayoutManager(new LinearLayoutManager(this));
        mRv_subscribe.setAdapter(new QuickRecycleViewAdapter<TabFragment.Data>(R.layout.item, mDatas) {
            @Override
            protected void onBindData(Context context, int position, TabFragment.Data item, int itemLayoutId, ViewHelper helper) {
                helper.setText(R.id.id_info, item.title)
                        .setRootOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Snackbar.make(v, R.string.action_settings, Snackbar.LENGTH_LONG).show();
                            }
                        });
            }
        });
        mIndicator.setTitles(mTitles);
    }
}
