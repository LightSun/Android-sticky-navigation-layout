package com.heaven7.android.sticky_navigation_layout.demo;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.heaven7.android.StickyLayout.StickyNavigationLayout;

import butterknife.InjectView;

/**
 * Created by heaven7 on 2016/11/3.
 */
public class StickyFragment extends BaseFragment {

     @InjectView(R.id.stickyLayout)
     StickyNavigationLayout mStickyNavLayout;

     @InjectView(R.id.vp_indicator)
     SimpleViewPagerIndicator mIndicator;

     @InjectView(R.id.vp)
     ViewPager mViewPager;

     @InjectView(R.id.top_view)
     View mTopView;

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
                Snackbar.make(v, R.string.action_settings, Snackbar.LENGTH_LONG).show();
            }
        });
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                setStickyDelegate();
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
        setStickyDelegate();
    }

    private void setStickyDelegate() {
        final Fragment item = mAdapter.getItem(mViewPager.getCurrentItem());
        if(item instanceof StickyDelegateSupplier) {
            mStickyNavLayout.setStickyDelegate(((StickyDelegateSupplier) item).getStickyDelegate());
        }
    }
}
