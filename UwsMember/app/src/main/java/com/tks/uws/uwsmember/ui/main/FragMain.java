package com.tks.uws.uwsmember.ui.main;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

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

		/* NumberPicker */
		NumberPicker npkNo = view.findViewById(R.id.npkNo);
		npkNo.setMinValue(0);
		npkNo.setMaxValue(255);
		npkNo.setFormatter(value -> String.format(Locale.JAPANESE,"%03d", value));

		/* No決定 */
		view.findViewById(R.id.btnSetId).setOnClickListener(view2 -> {
			/* アドバタイズ開始 */
			Intent intent = new Intent(getActivity().getApplicationContext(), PeripheralAdvertiseService.class);
			TLog.d("NumberPicker={0}", npkNo.getValue());
			intent.putExtra(KEY_NO, npkNo.getValue());
			getActivity().bindService(intent, new ServiceConnection() {
				@Override public void onServiceConnected(ComponentName name, IBinder service) {}
				@Override public void onServiceDisconnected(ComponentName name) {}
			}, Context.BIND_AUTO_CREATE);
			/* アドバタイズ停止(サービス終了) */
//			getActivity().stopService(new Intent(getActivity().getApplicationContext(), PeripheralAdvertiseService.class));
		});
	}
}