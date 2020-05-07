package com.heaven7.android.sticky_navigation_layout.demo.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.heaven7.adapter.BaseSelector;
import com.heaven7.adapter.QuickRecycleViewAdapter;
import com.heaven7.android.sticky_navigation_layout.demo.R;
import com.heaven7.core.util.ViewHelper;

import java.util.ArrayList;
import java.util.List;

public class TabFragment extends Fragment {

    public static final String TITLE = "title";

    private String mTitle = "Defaut Value";
    private RecyclerView mRecyclerView;
    private List<Data> mDatas = new ArrayList<Data>();

    private SwipeRefreshLayout swipeView ;
    private QuickRecycleViewAdapter<Data> mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTitle = getArguments().getString(TITLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.rv);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(container.getContext()));
       // mDelegate = new StickyNavigationLayout.RecyclerViewStickyDelegate(mRecyclerView);

        swipeView = (SwipeRefreshLayout) view.findViewById(R.id.swipe);
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeView.setRefreshing(false);
                    }
                }, 2000);
            }
        });
        for (int i = 0; i < 50; i++) {
            mDatas.add(new Data(mTitle + " -> " + i));
        }
        mRecyclerView.setAdapter(mAdapter = new QuickRecycleViewAdapter<Data>(R.layout.item, mDatas) {
            @Override
            protected void onBindData(Context context, int position, Data item, int itemLayoutId, ViewHelper helper) {
                helper.setText(R.id.id_info, item.title)
                        .setRootOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Snackbar.make(v, R.string.action_settings, Snackbar.LENGTH_LONG).show();
                            }
                        });
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public static TabFragment newInstance(String title) {
        TabFragment tabFragment = new TabFragment();
        Bundle bundle = new Bundle();
        bundle.putString(TITLE, title);
        tabFragment.setArguments(bundle);
        return tabFragment;
    }

    public static class Data extends BaseSelector {
        public String title;

        public Data(String title) {
            this.title = title;
        }
    }

}
