package com.heaven7.android.sticky_navigation_layout.demo;

import java.util.List;

/**
 * Created by heaven7 on 2016/11/14.
 */
public class EnterActivity extends AbsMainActivity {

    @Override
    protected void addDemos(List<ActivityInfo> list) {
        list.add(new ActivityInfo(MainActivity2.class, "StickyNavigationLayout")) ;
    }

}
