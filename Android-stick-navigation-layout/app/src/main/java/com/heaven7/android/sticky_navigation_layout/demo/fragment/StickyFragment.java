package com.heaven7.android.sticky_navigation_layout.demo.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.heaven7.adapter.QuickRecycleViewAdapter;
import com.heaven7.android.StickyLayout.StickyNavigationLayout;
import com.heaven7.android.sticky_navigation_layout.demo.OnScrollChangeSupplier;
import com.heaven7.android.sticky_navigation_layout.demo.R;
import com.heaven7.android.sticky_navigation_layout.demo.StickyDelegateSupplier;
import com.heaven7.android.sticky_navigation_layout.demo.view.SimpleViewPagerIndicator;
import com.heaven7.core.util.Logger;
import com.heaven7.core.util.ViewHelper;

import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;

/**
 * Created by heaven7 on 2016/11/3.
 */
public class StickyFragment extends BaseFragment {

    private static final String TAG = "StickyFragment";

    private static final int MODE_FEED       = 1;
    private static final int MODE_SUBSCRIBE  = 2;
    private int mMode = MODE_FEED;

     @InjectView(R.id.stickyLayout)
     StickyNavigationLayout mStickyNavLayout;

     @InjectView(R.id.vp_indicator)
     SimpleViewPagerIndicator mIndicator;
     @InjectView(R.id.vp)
     ViewPager mViewPager;
     @InjectView(R.id.top_view)
     View mTopView;

    @InjectView(R.id.fl_subscribe)
    ViewGroup mVg_subscribe;
    @InjectView(R.id.ll_indicator)
    LinearLayout mLl_indicator;

    @InjectView(R.id.rv_subscribe)
    RecyclerView mRv_subscribe;
    @InjectView(R.id.rv_tabs)
    RecyclerView mRv_tabs;

    private String[] mTitles = new String[] { "简介", "评价", "相关" };
    private TabFragment[] mFragments = new TabFragment[mTitles.length];
    private FragmentPagerAdapter mAdapter;
    private QuickRecycleViewAdapter<TabFragment.Data> mSubscribeAdapter;


    @Override
    protected int getLayoutId() {
        return R.layout.frag_sticky_layout;
    }

    @Override
    protected void init(Context context, Bundle savedInstanceState) {
        initEvents();
        initDatas();
    }

    private void initEvents() {
        mTopView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode();
               // Snackbar.make(v, R.string.action_settings, Snackbar.LENGTH_LONG).show();
            }
        });
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            /**
             * position : 中的position
             * positionOffset: 当前页面偏移的百分比
             * positionOffsetPixels: 当前页面偏移的像素位置
             */
            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) { // 会多次调用
                mIndicator.scroll(position, positionOffset);
            }
        });
        if( getContext() instanceof OnScrollChangeSupplier){
            mStickyNavLayout.addOnScrollChangeListener(((OnScrollChangeSupplier) getContext()).getOnScrollChangeListener());
        }
    }

    private void switchMode() {
        if(mMode == MODE_FEED){
            mMode = MODE_SUBSCRIBE;
            mLl_indicator.setVisibility(View.GONE);
            mViewPager.setVisibility(View.GONE);
            mVg_subscribe.setVisibility(View.VISIBLE);
          //  mStickyNavLayout.setEnableStickyTouch(false);
            Logger.i(TAG, "switchMode" , "to mode: MODE_SUBSCRIBE");
        }else{
            mMode = MODE_FEED;
            mVg_subscribe.setVisibility(View.GONE);
            mLl_indicator.setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.VISIBLE);
           // mStickyNavLayout.setEnableStickyTouch(true);
            Logger.i(TAG, "switchMode" , "to mode: MODE_FEED");
        }
    }

    private void initDatas() {
        List<TabFragment.Data> mDatas = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            mDatas.add(new TabFragment.Data(" title -> " + i));
        }
        mRv_subscribe.setLayoutManager(new LinearLayoutManager(getContext()));
        mRv_subscribe.setAdapter(mSubscribeAdapter = new QuickRecycleViewAdapter<TabFragment.Data>(R.layout.item, mDatas) {
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
        for (int i = 0; i < mTitles.length; i++) {
            mFragments[i] = TabFragment.newInstance(mTitles[i]);
        }
        mAdapter = new FragmentPagerAdapter(getChildFragmentManager()) {
            @Override
            public int getCount() {
                return mTitles.length;
            }
            @Override
            public Fragment getItem(int position) {
                return mFragments[position];
            }
        };
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(0);
        mStickyNavLayout.addStickyDelegate(mStickyDelegate);

        mRv_tabs.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        mRv_tabs.setAdapter(new QuickRecycleViewAdapter<TabFragment.Data>(R.layout.item_hor_tab, mDatas) {
            @Override
            protected void onBindData(Context context, int position, TabFragment.Data item, int itemLayoutId, ViewHelper helper) {
                helper.setText(R.id.tv_tab, item.title)
                        .setRootOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Snackbar.make(v, R.string.action_settings, Snackbar.LENGTH_LONG).show();
                            }
                        });
            }
        });
    }

    private StickyNavigationLayout.IStickyCallback getChildStickyDelegate() {
        final Fragment item = mAdapter.getItem(mViewPager.getCurrentItem());
        if(item instanceof StickyDelegateSupplier) {
           return ((StickyDelegateSupplier) item).getStickyDelegate();
        }
        Logger.i(TAG, "getChildStickyDelegate", "can't find ChildStickyDelegate.");
        return null;
    }

    private final StickyNavigationLayout.IStickyCallback mStickyDelegate = new StickyNavigationLayout.IStickyCallback() {
        @Override
        public void afterOnMeasure(StickyNavigationLayout snv, View top, View indicator, View contentView) {
            final ViewGroup.LayoutParams lp = mVg_subscribe.getLayoutParams();
            lp.height = snv.getMeasuredHeight() - snv.getScrollY();
            mVg_subscribe.setLayoutParams(lp);
          //  Logger.i(TAG, "afterOnMeasure" , "mVg_subscribe: height = " + lp.height +" ,snv.scrollY = " + snv.getScrollY());
        }
    };
}
