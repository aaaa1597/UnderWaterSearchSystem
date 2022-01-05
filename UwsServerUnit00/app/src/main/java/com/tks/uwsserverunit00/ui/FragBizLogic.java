package com.tks.uwsserverunit00.ui;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.tks.uwsserverunit00.R;
import com.tks.uwsserverunit00.TLog;

public class FragBizLogic extends Fragment {
	private FragMapViewModel		mMapLogicViewModel;
	private FragBizLogicViewModel	mBizLogicViewModel;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.frag_bizlogic, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mMapLogicViewModel = new ViewModelProvider(requireActivity()).get(FragMapViewModel.class);
		mBizLogicViewModel = new ViewModelProvider(requireActivity()).get(FragBizLogicViewModel.class);

		view.findViewById(R.id.btnSelectMember).setOnClickListener(v -> {
			DrawerLayout naviview = getActivity().findViewById(R.id.root_view);
			naviview.openDrawer(GravityCompat.START);
		});

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
			mMapLogicViewModel.incrementFillColorCnt();
			view.findViewById(R.id.btnCngSerchColor).setBackgroundColor(mMapLogicViewModel.getFillColor());
		});
	}
}