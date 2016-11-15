package com.heaven7.android.sticky_navigation_layout.demo.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.heaven7.core.util.Toaster;

import butterknife.ButterKnife;

public abstract class BaseFragment extends Fragment {

    private Toaster mToaster;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getLayoutId(), container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.inject(this, view);
        mToaster = new Toaster(view.getContext());
        init(view.getContext(), savedInstanceState);
    }

    public Toaster getToaster() {
        return mToaster;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }


    protected abstract int getLayoutId();

    protected abstract void init(Context context, Bundle savedInstanceState);
}
