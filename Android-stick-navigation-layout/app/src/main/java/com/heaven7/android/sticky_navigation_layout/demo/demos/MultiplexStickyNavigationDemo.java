package com.heaven7.android.sticky_navigation_layout.demo.demos;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.heaven7.android.scroll.IScrollHelper;
import com.heaven7.android.sticky_navigation_layout.demo.OnScrollChangeSupplier;
import com.heaven7.android.sticky_navigation_layout.demo.R;
import com.heaven7.android.sticky_navigation_layout.demo.fragment.BaseFragment;
import com.heaven7.android.sticky_navigation_layout.demo.fragment.StickyFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * this is a multiples demo of sticky .
 * Created by heaven7 on 2016/11/3.
 */
public class MultiplexStickyNavigationDemo extends AppCompatActivity implements OnScrollChangeSupplier{


     @BindView(R.id.rg)
     RadioGroup mRg;

     @BindView(R.id.drawer_layout)
     DrawerLayout mDrawerLayout;

     @BindView(R.id.fl_content)
     ViewGroup mContent;

    private final SparseArray<BaseFragment> sCache = new SparseArray<>(6);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // setContentView(R.layout.activity_main2);
        setContentView(getLayoutInflater().inflate(R.layout.activity_main2, null));
        ButterKnife.bind(this);

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

    @Override
    public IScrollHelper.OnScrollChangeListener getOnScrollChangeListener() {
        return mScrollChangeListener;
    }
    private final IScrollHelper.OnScrollChangeListener mScrollChangeListener = new IScrollHelper.OnScrollChangeListener() {
        @Override
        public void onScrollStateChanged(View target, int state) {
            //TODO
        }
        @Override
        public void onScrolled(View target, int dx, int dy) {
            //TODO
        }
    };
}
