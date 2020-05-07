package com.heaven7.android.sticky_navigation_layout.demo.demos;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import butterknife.ButterKnife;

/**
 * Created by heaven7 on 2016/11/15.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        ButterKnife.bind(this);
        init(savedInstanceState);
    }

    protected abstract int getLayoutId();

    protected abstract void init(Bundle savedInstanceState);
}
