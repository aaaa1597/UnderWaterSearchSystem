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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.NumberPicker;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;
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
		/* ViewModelインスタンス取得 */
		mViewModel = new ViewModelProvider(requireActivity()).get(FragMainViewModel.class);
		/* ViewModelふるまい定義 */
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
		mViewModel.HearBeat().observe(getActivity(), new Observer<Integer>() {
			@Override
			public void onChanged(Integer heartbeat) {
				((EditText)view.findViewById(R.id.etxHeartbeat)).setText(String.valueOf(heartbeat));
			}
		});

		((EditText)view.findViewById(R.id.etxID)).addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				String tmp = s.toString();
				mViewModel.setID(Integer.parseInt(tmp.equals("")?"-1":tmp));
			}
		});

		/* phase2レイヤは無効化しとく */
		setEnableView(view.findViewById(R.id.ph2), false);

		/* ID決定 */
		view.findViewById(R.id.btnSetId).setOnClickListener(btnview -> {
			((TextView)view.findViewById(R.id.txtStatus)).setText("ID設定中...");
			/* IDの正常性チェック(0-255の間の数値かどうか) */
			String idstr = ((EditText)view.findViewById(R.id.etxID)).getText().toString();
			if(idstr.equals("")) {
				Snackbar.make(view.findViewById(R.id.frag_root), "IDが設定されていません\nIDを設定してください。", Snackbar.LENGTH_LONG).show();
				return;
			}

			int id = Integer.parseInt(idstr);
			if(id < 0 || id > 255) {
				Snackbar.make(view.findViewById(R.id.frag_root), "IDは、0~255の数値を設定してください。", Snackbar.LENGTH_LONG).show();
				return;
			}

			/* アドバタイズ開始 */
			Intent intent = new Intent(getActivity().getApplicationContext(), PeripheralAdvertiseService.class);
			TLog.d("ID ={0}", ((EditText)view.findViewById(R.id.etxID)).getText().toString());
			intent.putExtra(KEY_NO, id);
			getActivity().bindService(intent, new ServiceConnection() {
				@Override
				public void onServiceConnected(ComponentName name, IBinder service) {
					getActivity().runOnUiThread(() -> {
						((TextView)view.findViewById(R.id.txtStatus)).setText("アドバタイズ中... 接続待ち");
						setEnableView(view.findViewById(R.id.ph1), false);
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