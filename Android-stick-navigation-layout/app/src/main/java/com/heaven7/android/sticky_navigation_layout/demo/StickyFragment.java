package com.heaven7.android.sticky_navigation_layout.demo;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.heaven7.android.StickyLayout.StickyNavigationLayout;
import com.heaven7.core.util.Logger;

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

    private String[] mTitles = new String[] { "简介", "评价", "相关" };
    private TabFragment[] mFragments = new TabFragment[mTitles.length];
    private FragmentPagerAdapter mAdapter;

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
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
            }

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

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void switchMode() {
        if(mMode == MODE_FEED){
            mMode = MODE_SUBSCRIBE;
            mLl_indicator.setVisibility(View.GONE);
            mViewPager.setVisibility(View.GONE);
            mVg_subscribe.setVisibility(View.VISIBLE);
            Logger.i(TAG, "switchMode" , "to mode: MODE_SUBSCRIBE");
        }else{
            mMode = MODE_FEED;
            mVg_subscribe.setVisibility(View.GONE);
            mLl_indicator.setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.VISIBLE);
            Logger.i(TAG, "switchMode" , "to mode: MODE_FEED");
        }
    }

    private void initDatas() {
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
    }

    private StickyNavigationLayout.IStickyDelegate getChildStickyDelegate() {
        final Fragment item = mAdapter.getItem(mViewPager.getCurrentItem());
        if(item instanceof StickyDelegateSupplier) {
           return ((StickyDelegateSupplier) item).getStickyDelegate();
        }
        Logger.i(TAG, "getChildStickyDelegate", "can't find ChildStickyDelegate.");
        return null;
    }

    private final StickyNavigationLayout.IStickyDelegate mStickyDelegate = new StickyNavigationLayout.IStickyDelegate() {
        @Override
        public boolean shouldIntercept(StickyNavigationLayout snv, int dy, int topViewState) {
            final StickyNavigationLayout.IStickyDelegate stickyDelegate = getChildStickyDelegate();
            return stickyDelegate !=null && stickyDelegate.shouldIntercept(snv, dy, topViewState);
        }
        @Override
        public void scrollBy(StickyNavigationLayout snv, int dy) {
            final StickyNavigationLayout.IStickyDelegate stickyDelegate = getChildStickyDelegate();
             if(stickyDelegate != null){
                 stickyDelegate.scrollBy(snv , dy);
             }
        }
        @Override
        public void afterOnMeasure(StickyNavigationLayout snv, View top,View indicator, View contentView) {
            final ViewGroup.LayoutParams lp = mVg_subscribe.getLayoutParams();
            lp.height = snv.getMeasuredHeight() - snv.getScrollY();
            mVg_subscribe.setLayoutParams(lp);
            Logger.i(TAG, "afterOnMeasure" , "mVg_subscribe: height = " + lp.height +" ,snv.scrollY = " + snv.getScrollY());
        }
    };
}
