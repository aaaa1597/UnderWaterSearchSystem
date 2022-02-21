package com.tks.uwsclient.ui;

import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.example.recyclerviewsimple.R;
import com.tks.uwsclient.Constants;
import com.tks.uwsclient.TLog;

public class FragMain extends Fragment {
    private FragMainViewModel mViewModel;
    private final RecyclerView.OnItemTouchListener mOnItemTouchListener = new RecyclerView.OnItemTouchListener() {
        @Override public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}
        @Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        @Override public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            return true;
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(FragMainViewModel.class);

        mViewModel.UnLock().observe(getActivity(), new Observer<Pair<Constants.Sender, Boolean>>() {
            @Override
            public void onChanged(Pair<Constants.Sender, Boolean> pair) {
                if(pair == null) return;
                boolean isUnLock = pair.second;
                if(pair.first == Constants.Sender.Service)
                    ((SwitchCompat)getActivity().findViewById(R.id.swhUnLock)).setChecked(isUnLock);

                /* ClickListnerの処理はMainActivityで実行しているのでここでは、リストViewを無効化するだけ */
                RecyclerView rvw = getActivity().findViewById(R.id.rvw_seekerid);
                if(isUnLock)
                    rvw.removeOnItemTouchListener(mOnItemTouchListener);
                else
                    rvw.addOnItemTouchListener(mOnItemTouchListener);
            }
        });
        ((SwitchCompat)view.findViewById(R.id.swhUnLock)).setOnCheckedChangeListener((btnView, isChecked) -> {
            TLog.d("UnLock isChecked={0}", isChecked);
            mViewModel.UnLock().setValue(Pair.create(Constants.Sender.App, isChecked));
        });

        RecyclerView recyclerView = getActivity().findViewById(R.id.rvw_seekerid);
        recyclerView.setAdapter(new SeekerIdAdapter());
        /* SeekerIDのlistView(子の中心で収束する設定) */
        LinearSnapHelper linearSnapHelper = new LinearSnapHelper();
        linearSnapHelper.attachToRecyclerView(recyclerView);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View lview = linearSnapHelper.findSnapView(recyclerView.getLayoutManager());
                    int pos =  recyclerView.getChildAdapterPosition(lview);
                    TLog.d("aaaaa pos={0}", pos);
//                    mViewModel.setSeekerId((short)pos);
                }
            }
        });
    }
}