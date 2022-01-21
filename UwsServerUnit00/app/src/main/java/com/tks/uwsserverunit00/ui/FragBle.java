package com.tks.uwsserverunit00.ui;

import androidx.lifecycle.ViewModelProvider;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.tks.uwsserverunit00.R;
import com.tks.uwsserverunit00.TLog;

public class FragBle extends Fragment {
	private FragBleViewModel mBleViewModel;
	private FragMapViewModel	mMapViewModel;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.frag_ble, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mMapViewModel = new ViewModelProvider(requireActivity()).get(FragMapViewModel.class);
		mBleViewModel = new ViewModelProvider(requireActivity()).get(FragBleViewModel.class);
		mBleViewModel.NotifyDataSetChanged().observe(getViewLifecycleOwner(), upd -> {
			mBleViewModel.getDeviceListAdapter().notifyDataSetChanged();
//			int maxidx = mViewModel.getDeviceListAdapter().getItemCount() - 1;
//			mViewModel.getDeviceListAdapter().notifyItemRangeInserted(maxidx ,1);
		});
		mBleViewModel.NotifyItemChanged().observe(getViewLifecycleOwner(), pos -> {
			if(pos ==-1 )
				TLog.d("Item変更 最初はposが-1になるらしい.)");
			else if(pos < 0|| pos >= mBleViewModel.getDeviceListAdapter().getItemCount()) {
				TLog.w("idx is out of range. pos={0} adapter.size()={1}", pos, mBleViewModel.getDeviceListAdapter().getItemCount());
				return;
			}
			mBleViewModel.getDeviceListAdapter().notifyItemChanged(pos);
		});
		mBleViewModel.setDeviceListAdapter(new DeviceListAdapter(getActivity().getApplicationContext(),
															  (seekerid, isChecked) -> {mBleViewModel.setSelected(seekerid, isChecked);
																						mMapViewModel.setSelected(seekerid, isChecked);},
															  (seekerid, isChecked) -> mBleViewModel.setBuoy(seekerid, isChecked)
															));

		view.findViewById(R.id.btnClear).setOnClickListener(lview -> {
			mBleViewModel.clearDeviceWithoutConnected();
		});
		((CheckBox)view.findViewById(R.id.cbxMember)).setOnCheckedChangeListener((btn, isChecked) -> {
			if(isChecked)
				mBleViewModel.clearDeviceWithoutAppliciated();
			mBleViewModel.OnlySeeker().setValue(isChecked);
		});

		/* BLEデバイスリストの初期化 */
		RecyclerView deviceListRvw = getActivity().findViewById(R.id.rvw_devices);
		/* BLEデバイスリストに区切り線を表示 */
		DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(deviceListRvw.getContext(), new LinearLayoutManager(getActivity().getApplicationContext()).getOrientation());
		deviceListRvw.addItemDecoration(dividerItemDecoration);
		deviceListRvw.setHasFixedSize(true);
		deviceListRvw.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
		deviceListRvw.setAdapter(mBleViewModel.getDeviceListAdapter());
	}
}