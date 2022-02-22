package com.tks.uwsserverunit00.ui;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.util.List;

import com.tks.uwsserverunit00.R;
import com.tks.uwsserverunit00.TLog;

public class FragBizLogic extends Fragment {
	private FragMapViewModel		mMapViewModel;
	private FragBleViewModel		mBleViewModel;
	private FragBizLogicViewModel	mBizLogicViewModel;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.frag_bizlogic, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mMapViewModel = new ViewModelProvider(requireActivity()).get(FragMapViewModel.class);
		mBleViewModel = new ViewModelProvider(requireActivity()).get(FragBleViewModel.class);
		mBizLogicViewModel = new ViewModelProvider(requireActivity()).get(FragBizLogicViewModel.class);
		mBizLogicViewModel.onHearBeatChange().observe(getViewLifecycleOwner(), pair -> {
			short seekerid = pair.first;
			short hearbeat = pair.second;
			switch(seekerid) {
				case 0: ((TextView)view.findViewById(R.id.txtHb0)).setText(String.valueOf(hearbeat)); break;
				case 1: ((TextView)view.findViewById(R.id.txtHb1)).setText(String.valueOf(hearbeat)); break;
				case 2: ((TextView)view.findViewById(R.id.txtHb2)).setText(String.valueOf(hearbeat)); break;
				case 3: ((TextView)view.findViewById(R.id.txtHb3)).setText(String.valueOf(hearbeat)); break;
				case 4: ((TextView)view.findViewById(R.id.txtHb4)).setText(String.valueOf(hearbeat)); break;
				case 5: ((TextView)view.findViewById(R.id.txtHb5)).setText(String.valueOf(hearbeat)); break;
				case 6: ((TextView)view.findViewById(R.id.txtHb6)).setText(String.valueOf(hearbeat)); break;
				case 7: ((TextView)view.findViewById(R.id.txtHb7)).setText(String.valueOf(hearbeat)); break;
				case 8: ((TextView)view.findViewById(R.id.txtHb8)).setText(String.valueOf(hearbeat)); break;
				case 9: ((TextView)view.findViewById(R.id.txtHb9)).setText(String.valueOf(hearbeat)); break;
			}
		});
		mBizLogicViewModel.onSeekeridChange().observe(getViewLifecycleOwner(), pair -> {
			short   oldseekerid  = pair.first;
			short   newseekerid  = pair.second;
			if(oldseekerid == newseekerid) return;

			/* 非表示に */
			switch(oldseekerid) {
				case -1: /* 何もする必要なし */ break;
				case 0: view.findViewById(R.id.llHb0).setVisibility(View.GONE); break;
				case 1: view.findViewById(R.id.llHb1).setVisibility(View.GONE); break;
				case 2: view.findViewById(R.id.llHb2).setVisibility(View.GONE); break;
				case 3: view.findViewById(R.id.llHb3).setVisibility(View.GONE); break;
				case 4: view.findViewById(R.id.llHb4).setVisibility(View.GONE); break;
				case 5: view.findViewById(R.id.llHb5).setVisibility(View.GONE); break;
				case 6: view.findViewById(R.id.llHb6).setVisibility(View.GONE); break;
				case 7: view.findViewById(R.id.llHb7).setVisibility(View.GONE); break;
				case 8: view.findViewById(R.id.llHb8).setVisibility(View.GONE); break;
				case 9: view.findViewById(R.id.llHb9).setVisibility(View.GONE); break;
			}

			/* 表示に */
			switch(newseekerid) {
				case -1: /* 何もする必要なし */ break;
				case 0: view.findViewById(R.id.llHb0).setVisibility(View.VISIBLE); break;
				case 1: view.findViewById(R.id.llHb1).setVisibility(View.VISIBLE); break;
				case 2: view.findViewById(R.id.llHb2).setVisibility(View.VISIBLE); break;
				case 3: view.findViewById(R.id.llHb3).setVisibility(View.VISIBLE); break;
				case 4: view.findViewById(R.id.llHb4).setVisibility(View.VISIBLE); break;
				case 5: view.findViewById(R.id.llHb5).setVisibility(View.VISIBLE); break;
				case 6: view.findViewById(R.id.llHb6).setVisibility(View.VISIBLE); break;
				case 7: view.findViewById(R.id.llHb7).setVisibility(View.VISIBLE); break;
				case 8: view.findViewById(R.id.llHb8).setVisibility(View.VISIBLE); break;
				case 9: view.findViewById(R.id.llHb9).setVisibility(View.VISIBLE); break;
			}
		});

		/* メンバ選択ボタン */
		view.findViewById(R.id.btnSelectMember).setOnClickListener(v -> {
			DrawerLayout naviview = getActivity().findViewById(R.id.root_view);
			naviview.openDrawer(GravityCompat.START);
		});
		/* 検索開始/終了ボタン */
		view.findViewById(R.id.btnSerchStartStop).setOnClickListener(v -> {
			Button btnStartStop = (Button)v;
			TLog.d("mBizLogicViewModel.getSerchStatus() = {0}", mBizLogicViewModel.getSerchStatus());
			if(mBizLogicViewModel.getSerchStatus()) {
				mBizLogicViewModel.setSerchStatus(false);
				btnStartStop.setText("検索開始");
			}
			else {
				mBizLogicViewModel.setSerchStatus(true);
				btnStartStop.setText("検索終了");
			}
		});

		/*　検索矩形の塗りつぶし色変更 */
		view.findViewById(R.id.btnCngSerchColor).setOnClickListener(v -> {
			int colidx = mMapViewModel.incrementFillColorCnt();
			((Button)view.findViewById(R.id.btnCngSerchColor)).setTextColor((colidx==9)?0xff000000 : 0xffffffff);
			view.findViewById(R.id.btnCngSerchColor).setBackgroundColor(mMapViewModel.getFillColor());
		});
	}
}