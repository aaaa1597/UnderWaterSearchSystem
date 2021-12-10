package com.tks.uws.uwsmember.ui.main;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;
import com.tks.uws.uwsmember.R;

import com.tks.uws.uwsmember.ui.main.FragMainViewModel.ConnectStatus;

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
		mViewModel.ConnectStatus().observe(getActivity(), new Observer<ConnectStatus>() {
			@Override
			public void onChanged(ConnectStatus status) {
				TextView txtStatus = view.findViewById(R.id.txtStatus);
				switch (status) {
					case NONE:
						txtStatus.setText("-- none --");
						setEnableView(view.findViewById(R.id.ph1), true);
						setEnableView(view.findViewById(R.id.ph2), false);
						break;
					case SETTING_ID:		txtStatus.setText("ID設定中...");			break;
					case START_ADVERTISE:
						txtStatus.setText("アドバタイズ開始");
						setEnableView(view.findViewById(R.id.ph1), false);
						break;
					case ADVERTISING:		txtStatus.setText("アドバタイズ中... 接続待ち");break;
					case CONNECTED:
						txtStatus.setText("接続確立");
						setEnableView(view.findViewById(R.id.ph2), true);
						break;
				}
			}
		});

		/* ID入力監視 */
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

		/* ID決定ボタン押下 */
		view.findViewById(R.id.btnSetId).setOnClickListener(btnview -> {
			/* ID設定中... */
			mViewModel.ConnectStatus().setValue(ConnectStatus.SETTING_ID);
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

			mViewModel.ConnectStatus().setValue(ConnectStatus.START_ADVERTISE);
		});

		/* 値設定ボタン押下 */
		view.findViewById(R.id.btnSetValue).setOnClickListener(btnview -> {
			mViewModel.PressSetBtn().setValue(true);
		});

		/* 初期化ボタン押下 */
		view.findViewById(R.id.btnInit).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mViewModel.ConnectStatus().setValue(ConnectStatus.NONE);
			}
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