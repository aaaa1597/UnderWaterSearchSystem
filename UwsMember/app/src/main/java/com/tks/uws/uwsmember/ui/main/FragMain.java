package com.tks.uws.uwsmember.ui.main;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.tks.uws.uwsmember.PeripheralAdvertiseService;
import com.tks.uws.uwsmember.R;
import com.tks.uws.uwsmember.TLog;

import static com.tks.uws.uwsmember.PeripheralAdvertiseService.KEY_NO;

import java.util.Locale;

public class FragMain extends Fragment {
	private FragMainViewModel mViewModel;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.main_fragment, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mViewModel = new ViewModelProvider(requireActivity()).get(FragMainViewModel.class);
		mViewModel.Longitude().observe(getActivity(), new Observer<Double>() {
			@Override
			public void onChanged(Double lng) {
				((EditText)view.findViewById(R.id.etxLongitude)).setText(String.valueOf(lng));
			}
		});
		mViewModel.Latitude().observe(getActivity(), new Observer<Double>() {
			@Override
			public void onChanged(Double lat) {
				((EditText)view.findViewById(R.id.etxLatitude)).setText(String.valueOf(lat));
			}
		});

		/* NumberPicker */
		NumberPicker npkNo = view.findViewById(R.id.npkNo);
		npkNo.setMinValue(0);
		npkNo.setMaxValue(255);
		npkNo.setFormatter(value -> String.format(Locale.JAPANESE,"%03d", value));

		/* phase2レイヤは無効化しとく */
		setEnableView(view.findViewById(R.id.ph2), false);

		/* ID決定 */
		view.findViewById(R.id.btnSetId).setOnClickListener(view2 -> {
			((TextView)view.findViewById(R.id.txtStatus)).setText("ID設定中...");

			/* アドバタイズ開始 */
			Intent intent = new Intent(getActivity().getApplicationContext(), PeripheralAdvertiseService.class);
			TLog.d("NumberPicker={0}", npkNo.getValue());
			intent.putExtra(KEY_NO, npkNo.getValue());
			getActivity().bindService(intent, new ServiceConnection() {
				@Override
				public void onServiceConnected(ComponentName name, IBinder service) {
					getActivity().runOnUiThread(() -> {
						((TextView)view.findViewById(R.id.txtStatus)).setText("アドバタイズ中...");
						setEnableView(view.findViewById(R.id.ph1), false);
						setEnableView(view.findViewById(R.id.ph2), true);
					});
				}
				@Override public void onServiceDisconnected(ComponentName name) {}
			}, Context.BIND_AUTO_CREATE);
			((TextView)view.findViewById(R.id.txtStatus)).setText("アドバタイズ開始");
			/* アドバタイズ停止(サービス終了) */
//			getActivity().stopService(new Intent(getActivity().getApplicationContext(), PeripheralAdvertiseService.class));
		});
	}

	/* 配下を全部(有効/無効)にする */
	private void setEnableView(View view, boolean enabled) {
		view.setEnabled(enabled);

		if (view instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) view;

			for (int idx = 0; idx < group.getChildCount(); idx++)
				setEnableView(group.getChildAt(idx), enabled);
		}
	}
}