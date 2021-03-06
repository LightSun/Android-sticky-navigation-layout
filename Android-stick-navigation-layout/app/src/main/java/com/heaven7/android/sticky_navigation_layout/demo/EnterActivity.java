package com.heaven7.android.sticky_navigation_layout.demo;

import com.heaven7.android.sticky_navigation_layout.demo.demos.MultiplexStickyNavigationDemo;
import com.heaven7.android.sticky_navigation_layout.demo.demos.SimpleStickyNavigationDemo;
import com.heaven7.android.sticky_navigation_layout.demo.demos.TestNestedScrollFrameLayout;

import java.util.List;

/**
 * Created by heaven7 on 2016/11/14.
 */
public class EnterActivity extends AbsMainActivity {

    @Override
    protected void addDemos(List<ActivityInfo> list) {
        list.add(new ActivityInfo(TestNestedScrollFrameLayout.class, "TestNestedScrollFrameLayout")) ;
        list.add(new ActivityInfo(SimpleStickyNavigationDemo.class, "SimpleStickyNavigationDemo")) ;
        list.add(new ActivityInfo(MultiplexStickyNavigationDemo.class, "MultiplexStickyNavigationDemo")) ;
    }

}
