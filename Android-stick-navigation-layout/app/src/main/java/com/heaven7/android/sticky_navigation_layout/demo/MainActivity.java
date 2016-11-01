package com.heaven7.android.sticky_navigation_layout.demo;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;

import com.heaven7.android.StickyLayout.StickyNavigationLayout;

public class MainActivity extends AppCompatActivity {
	
	private String[] mTitles = new String[] { "简介", "评价", "相关" };
	private SimpleViewPagerIndicator mIndicator;
	private ViewPager mViewPager;
	private FragmentPagerAdapter mAdapter;
	private TabFragment[] mFragments = new TabFragment[mTitles.length];

	private StickyNavigationLayout mStickyNavLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initViews();
		initEvents();
		initDatas();
	}

	private void initEvents() {
		findViewById(R.id.top_view).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Snackbar.make(v, R.string.action_settings, Snackbar.LENGTH_LONG).show();
			}
		});
		mViewPager.addOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				mStickyNavLayout.setStickyDelegate((StickyNavigationLayout.IStickyDelegate) mAdapter.getItem(mViewPager.getCurrentItem()));
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
		mAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
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
		mStickyNavLayout.setStickyDelegate((StickyNavigationLayout.IStickyDelegate) mAdapter.getItem(mViewPager.getCurrentItem()));
	}

	private void initViews() {
		mStickyNavLayout = (StickyNavigationLayout) findViewById(R.id.stickyLayout);
		mIndicator = (SimpleViewPagerIndicator) findViewById(R.id.vp_indicator);
		mViewPager = (ViewPager) findViewById(R.id.vp);
	}

	public static int findFirstVisibleItemPosition(RecyclerView rv) {
		RecyclerView.LayoutManager lm = rv.getLayoutManager();
		int firstPos = RecyclerView.NO_POSITION ;
		if (lm instanceof GridLayoutManager) {
			firstPos = ((GridLayoutManager) lm).findFirstVisibleItemPosition();

		} else if (lm instanceof LinearLayoutManager) {
			firstPos = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();

		} else if (lm instanceof StaggeredGridLayoutManager) {
			int positions[] =  ((StaggeredGridLayoutManager) lm).findFirstVisibleItemPositions(null);
			for(int pos : positions){
				if(pos < firstPos){
					firstPos = pos;
				}
			}
		}
		return firstPos;
	}
}
