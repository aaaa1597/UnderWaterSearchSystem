package com.tks.uwsclientwearos.ui;

import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.Observer;
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
import android.widget.TextView;

import com.tks.uwsclientwearos.R;
import com.tks.uwsclientwearos.TLog;
import com.tks.uwsclientwearos.ui.FragMainViewModel.ConnectStatus;

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
		mViewModel.UnLock().observe(getActivity(), new Observer<Boolean>() {
			@Override
			public void onChanged(Boolean isUnLock) {
				TLog.d("UnLock isLock={0}", isUnLock);
				RecyclerView rvw = getActivity().findViewById(R.id.rvw_seekerid);
				if(isUnLock) {
					rvw.removeOnItemTouchListener(mOnItemTouchListener);
				}
				else {
					rvw.addOnItemTouchListener(mOnItemTouchListener);
				}
			}
		});
		((SwitchCompat)view.findViewById(R.id.swhUnLock)).setOnCheckedChangeListener((buttonView, isChecked) -> {
			TLog.d("UnLock isChecked={0}", isChecked);
			mViewModel.UnLock().setValue(isChecked);
		});
		/* 情報表示(経度) */
		mViewModel.Longitude().observe(getActivity(), new Observer<Double>() {
			@Override
			public void onChanged(Double lng) {
				((TextView)view.findViewById(R.id.txtLongitude)).setText(String.valueOf(lng));
			}
		});
		/* 情報表示(緯度) */
		mViewModel.Latitude().observe(getActivity(), new Observer<Double>() {
			@Override
			public void onChanged(Double lat) {
				((TextView)view.findViewById(R.id.txtLatitude)).setText(String.valueOf(lat));
			}
		});
		/* 情報表示(脈拍) */
		mViewModel.HearBeat().observe(getActivity(), new Observer<Short>() {
			@Override
			public void onChanged(Short heartbeat) {
				((TextView)view.findViewById(R.id.txtHeartbeat)).setText(String.valueOf(heartbeat));
			}
		});
		/* 情報表示(状態) */
		mViewModel.ConnectStatus().observe(getActivity(), new Observer<ConnectStatus>() {
			@Override
			public void onChanged(ConnectStatus status) {
				TextView txtStatus = view.findViewById(R.id.txtStatus);
				switch (status) {
					case NONE:				txtStatus.setText("-- none --");		break;
					case SETTING_ID:		txtStatus.setText("ID設定中...");		break;
					case START_ADVERTISE:	txtStatus.setText("アドバタイズ開始");		break;
					case ADVERTISING:		txtStatus.setText("アドバタイズ中...");	break;
					case ERROR:				txtStatus.setText("エラーが発生しました。");break;
				}
			}
		});

		/* SeekerIDのlistView定義 */
		RecyclerView recyclerView = getActivity().findViewById(R.id.rvw_seekerid);
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
					mViewModel.setSeekerID(pos);
				}
			}
		});
	}

}