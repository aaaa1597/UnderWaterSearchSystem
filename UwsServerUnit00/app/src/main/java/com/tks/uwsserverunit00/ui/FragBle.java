package com.tks.uwsserverunit00.ui;

import androidx.lifecycle.Observer;
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
	private FragBleViewModel	mBleViewModel;
	private FragMapViewModel	mMapViewModel;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.frag_ble, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		TLog.d("xxxxx");

		mMapViewModel = new ViewModelProvider(requireActivity()).get(FragMapViewModel.class);
		mBleViewModel = new ViewModelProvider(requireActivity()).get(FragBleViewModel.class);
		mBleViewModel.setDeviceListAdapter().observe(getViewLifecycleOwner(), deviceListAdapter -> {
			/* BLEデバイスリストの初期化 */
			RecyclerView deviceListRvw = view.findViewById(R.id.rvw_devices);
			deviceListRvw.setAdapter(deviceListAdapter);
		});


		/* BLEデバイスリストの初期化 */
		RecyclerView deviceListRvw = view.findViewById(R.id.rvw_devices);
		/* BLEデバイスリストに区切り線を表示 */
		DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(deviceListRvw.getContext(), new LinearLayoutManager(getActivity().getApplicationContext()).getOrientation());
		deviceListRvw.addItemDecoration(dividerItemDecoration);
		deviceListRvw.setHasFixedSize(true);
		deviceListRvw.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
	}
}