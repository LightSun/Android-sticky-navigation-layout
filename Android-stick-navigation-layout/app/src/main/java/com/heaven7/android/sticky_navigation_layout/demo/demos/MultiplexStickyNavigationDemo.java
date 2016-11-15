package com.heaven7.android.sticky_navigation_layout.demo.demos;

import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import com.heaven7.android.sticky_navigation_layout.demo.R;
import com.heaven7.android.sticky_navigation_layout.demo.fragment.BaseFragment;
import com.heaven7.android.sticky_navigation_layout.demo.fragment.StickyFragment;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * this is a multiples demo of sticky .
 * Created by heaven7 on 2016/11/3.
 */
public class MultiplexStickyNavigationDemo extends AppCompatActivity {

    private final SparseArray<BaseFragment> sCache = new SparseArray<>(6);

     @InjectView(R.id.rg)
     RadioGroup mRg;

     @InjectView(R.id.drawer_layout)
     DrawerLayout mDrawerLayout;

     @InjectView(R.id.fl_content)
     ViewGroup mContent;

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

        mRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                    getSupportFragmentManager().beginTransaction()
                .replace(R.id.fl_content, sCache.get(checkedId),"StickyFragment")
                .commit();
            }
        });
        mRg.check(R.id.rb_chuzhen);
    }

}
