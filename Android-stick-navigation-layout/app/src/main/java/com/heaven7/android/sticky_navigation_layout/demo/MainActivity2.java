package com.heaven7.android.sticky_navigation_layout.demo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by heaven7 on 2016/11/3.
 */
public class MainActivity2 extends AppCompatActivity {

    private final SparseArray<BaseFragment> sCache = new SparseArray<>(5);

     @InjectView(R.id.rg)
     RadioGroup mRg;

     @InjectView(R.id.drawer_layout)
     DrawerLayout mDrawerLayout;

     @InjectView(R.id.fl_content)
     ViewGroup mContent;

    private FragmentPagerAdapter mMainTabData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // setContentView(R.layout.activity_main2);
        setContentView(getLayoutInflater().inflate(R.layout.activity_main2, null));
        ButterKnife.inject(this);

        sCache.put(R.id.rb_chuzhen,  new StickyFragment());
        sCache.put(R.id.rb_circle,  new StickyFragment());
        sCache.put(R.id.rb_first_page,  new StickyFragment());
        sCache.put(R.id.rb_msg,  new StickyFragment());

        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        init();
      //  sCache.get(R.id.rb_chuzhen)
     /*   getSupportFragmentManager().beginTransaction()
                .replace(R.id.fl_content, new StickyFragment(),"StickyFragment")
                .commit();*/
        mRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Fragment fragment = (Fragment) mMainTabData.instantiateItem(mContent, checkedId);
                mMainTabData.setPrimaryItem(mContent, checkedId, fragment);
                mMainTabData.finishUpdate(mContent);
            }
        });
        mRg.check(R.id.rb_chuzhen);
    }

    private void init() {
        mMainTabData = new FragmentPagerAdapter(getSupportFragmentManager()) {
            private SparseArray<BaseFragment> mCache = new SparseArray<>(5);
            @Override
            public Fragment getItem(int position) {
                BaseFragment fragment = mCache.get(position);
                if (null == fragment) {
                    switch (position) {
                        case R.id.rb_chuzhen: //首页
                        case R.id.rb_circle://视频
                        case R.id.rb_first_page://圈子->病例
                        case R.id.rb_msg://消息community
                            default:
                                fragment = (BaseFragment) Fragment.instantiate(MainActivity2.this, StickyFragment.class.getName());
                            break;
                    }
                    mCache.put(position, fragment);
                }
                return fragment;
            }
            @Override
            public int getCount() {
                return 4;
            }
        };
    }
}
