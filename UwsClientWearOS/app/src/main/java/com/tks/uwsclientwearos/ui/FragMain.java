package com.tks.uwsclientwearos.ui;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.tks.uwsclientwearos.R;
import com.tks.uwsclientwearos.TLog;

public class FragMain extends Fragment {
	private FragMainViewModel mViewModel;

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

		/* SeekerIDのlistView定義 */
		RecyclerView recyclerView = getActivity().findViewById(R.id.rvw_seekerid);
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
		recyclerView.setAdapter(new SeekerIdAdapter());
		LinearSnapHelper linearSnapHelper = new LinearSnapHelper();
		linearSnapHelper.attachToRecyclerView(recyclerView);
		/* SeekerIDのlistView(子の中心で収束する設定) */
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				if(newState == RecyclerView.SCROLL_STATE_IDLE) {
					View lview = linearSnapHelper.findSnapView(recyclerView.getLayoutManager());
					int pos =  recyclerView.getChildAdapterPosition(lview);
					TLog.d("aaaaa pos={0}", pos);
//					mViewModel.setSeekerID(pos);
				}
			}
		});
	}
}