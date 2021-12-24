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

import com.tks.uwsserverunit00.R;
import com.tks.uwsserverunit00.TLog;

public class FragBle extends Fragment {
	private FragBleViewModel	mViewModel;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.frag_ble, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mViewModel = new ViewModelProvider(requireActivity()).get(FragBleViewModel.class);
		mViewModel.NotifyDataSetChanged().observe(getViewLifecycleOwner(), upd -> {
			mViewModel.getDeviceListAdapter().notifyDataSetChanged();
//			int maxidx = mViewModel.getDeviceListAdapter().getItemCount() - 1;
//			mViewModel.getDeviceListAdapter().notifyItemRangeInserted(maxidx ,1);
		});
		mViewModel.NotifyItemChanged().observe(getViewLifecycleOwner(), pos -> {
			if(pos ==-1 )
				TLog.d("Item変更 最初はposが-1になるらしい.)");
			else if(pos < 0|| pos >= mViewModel.getDeviceListAdapter().getItemCount()) {
				TLog.w("idx is out of range. pos={0} adapter.size()={1}", pos, mViewModel.getDeviceListAdapter().getItemCount());
				return;
			}
			mViewModel.getDeviceListAdapter().notifyItemChanged(pos);
		});
		mViewModel.setDeviceListAdapter(new DeviceListAdapter(getActivity().getApplicationContext(), (view1, sUuid, address, isChecked) -> {
			int pos = mViewModel.getDeviceListAdapter().setChecked(sUuid, address, isChecked);
			mViewModel.NotifyItemChanged().postValue(pos);

			if(isChecked)
				mViewModel.startPeriodicRead(sUuid, address);
			else
				mViewModel.stopPeriodicRead(sUuid, address);
		}));

		/* BLEデバイスリストの初期化 */
		RecyclerView deviceListRvw = getActivity().findViewById(R.id.rvw_devices);
		/* BLEデバイスリストに区切り線を表示 */
		DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(deviceListRvw.getContext(), new LinearLayoutManager(getActivity().getApplicationContext()).getOrientation());
		deviceListRvw.addItemDecoration(dividerItemDecoration);
		deviceListRvw.setHasFixedSize(true);
		deviceListRvw.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
		deviceListRvw.setAdapter(mViewModel.getDeviceListAdapter());
	}
}