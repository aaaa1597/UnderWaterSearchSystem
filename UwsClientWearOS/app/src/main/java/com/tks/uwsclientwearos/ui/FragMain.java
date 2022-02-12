package com.tks.uwsclientwearos.ui;

import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.tks.uwsclientwearos.R;
import com.tks.uwsclientwearos.TLog;
import com.tks.uwsclientwearos.Constants.Sender;

public class FragMain extends Fragment {
	private FragMainViewModel mViewModel;
	private final RecyclerView.OnItemTouchListener mOnItemTouchListener = new RecyclerView.OnItemTouchListener() {
		@Override public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}
		@Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
		@Override public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
			return true;
		}
	};

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.frag_main, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		/* ViewModelインスタンス取得 */
		mViewModel = new ViewModelProvider(requireActivity()).get(FragMainViewModel.class);
		/* Lock/Lock解除 設定 */
		mViewModel.UnLock().observe(getActivity(), new Observer<Pair<Sender, Boolean>>() {
			@Override
			public void onChanged(Pair<Sender, Boolean> pair) {
				if(pair == null) return;
				boolean isUnLock = pair.second;
				if(pair.first == Sender.Service)
					((SwitchCompat)getActivity().findViewById(R.id.swhUnLock)).setChecked(isUnLock);

				/* ClickListnerの処理はMainActivityで実行しているのでここでは、リストViewを無効化するだけ */
				RecyclerView rvw = getActivity().findViewById(R.id.rvw_seekerid);
				if(isUnLock)
					rvw.removeOnItemTouchListener(mOnItemTouchListener);
				else
					rvw.addOnItemTouchListener(mOnItemTouchListener);
			}
		});
		/* SeekerId表示更新 */
		mViewModel.UpdDisplaySeerkerId.observe(getActivity(), new Observer<Short>() {
			@Override
			public void onChanged(Short pos) {
				TLog.d("scrollToPosition({0})", pos);
				RecyclerView rvw = getActivity().findViewById(R.id.rvw_seekerid);
				rvw.scrollToPosition(pos);
			}
		});

		/* SeekerIDのlistView定義 */
		RecyclerView recyclerView = getActivity().findViewById(R.id.rvw_seekerid);
//		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
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
					mViewModel.setSeekerId((short)pos);
				}
			}
		});
	}
}