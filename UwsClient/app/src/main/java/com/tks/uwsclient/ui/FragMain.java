package com.tks.uwsclient.ui;

import android.graphics.Rect;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.tks.uwsclient.R;
import com.tks.uwsclient.TLog;
import com.tks.uwsclient.ui.FragMainViewModel.ConnectStatus;

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
					getActivity().findViewById(R.id.swhAdvertise).setEnabled(false);
					((SwitchCompat)getActivity().findViewById(R.id.swhAdvertise)).setChecked(false);
					getActivity().findViewById(R.id.glyOnOff).setBackgroundColor(getResources().getColor(R.color.disable_gray, getActivity().getTheme()));
				}
				else {
					rvw.addOnItemTouchListener(mOnItemTouchListener);
					getActivity().findViewById(R.id.swhAdvertise).setEnabled(true);
					getActivity().findViewById(R.id.glyOnOff).setBackgroundColor(getResources().getColor(R.color.white, getActivity().getTheme()));
				}
			}
		});
		((SwitchCompat)view.findViewById(R.id.swhUnLock)).setOnCheckedChangeListener((buttonView, isChecked) -> {
			TLog.d("UnLock isChecked={0}", isChecked);
			mViewModel.UnLock().setValue(isChecked);
		});
		/* アドバタイズON/OFF切替え */
		mViewModel.AdvertisingFlg().observe(getActivity(), new Observer<Boolean>() {
			@Override
			public void onChanged(Boolean advertisingFlg) {
				TLog.d("アドバタイズSwh 切替 advertisingFlg={0}", advertisingFlg);
				if(advertisingFlg != null && advertisingFlg)
					((SwitchCompat)view.findViewById(R.id.swhAdvertise)).setChecked(true);
				else
					((SwitchCompat)view.findViewById(R.id.swhAdvertise)).setChecked(false);
			}
		});
		((SwitchCompat)view.findViewById(R.id.swhAdvertise)).setOnCheckedChangeListener((buttonView, isChecked) -> {
			TLog.d("アドバタイズSwh 切替 isChecked={0}", isChecked);
			mViewModel.AdvertisingFlg().setValue(isChecked);
		});
		/* 情報表示(アドレス) */
		mViewModel.DeviceAddress().observe(getActivity(), new Observer<String>() {
			@Override
			public void onChanged(String address) {
				((TextView)view.findViewById(R.id.txtAddress)).setText(address);
			}
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
		mViewModel.HearBeat().observe(getActivity(), new Observer<Integer>() {
			@Override
			public void onChanged(Integer heartbeat) {
				((TextView)view.findViewById(R.id.txtHeartbeat)).setText(String.valueOf(heartbeat));
			}
		});
		/* 情報表示(状態) */
		mViewModel.ConnectStatus().observe(getActivity(), new Observer<ConnectStatus>() {
			@Override
			public void onChanged(ConnectStatus status) {
				TextView txtStatus = view.findViewById(R.id.txtStatus);
				switch (status) {
					case NONE:				txtStatus.setText("-- none --");			break;
					case SETTING_ID:		txtStatus.setText("ID設定中...");			break;
					case START_ADVERTISE:	txtStatus.setText("アドバタイズ開始");			break;
					case ADVERTISING:		txtStatus.setText("アドバタイズ中... 接続待ち");break;
					case CONNECTED:			txtStatus.setText("接続確立");				break;
					case DISCONNECTED:		txtStatus.setText("接続断");					break;
					case ERROR:				txtStatus.setText("エラーが発生しました。");	break;
				}
			}
		});

		/* SeekerIDのlistView定義 */
		RecyclerView recyclerView = getActivity().findViewById(R.id.rvw_seekerid);
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
		recyclerView.scrollToPosition(5);
		recyclerView.smoothScrollToPosition(0);
		recyclerView.setAdapter(new SeekerIdAdapter());
		/* SeekerIDのlistView(子の中心で収束する設定) */
		new LinearSnapHelper().attachToRecyclerView(recyclerView);
		/* SeekerIDのlistView(端の子も中心で収束する様に調整) */
		recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
				int position = parent.getChildAdapterPosition(view);
				recyclerView.smoothScrollToPosition(position);
				mViewModel.setSeekerID(position);
			}
		});
	}
}