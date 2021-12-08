package com.tks.uws.uwsmember.ui.main;

import androidx.lifecycle.ViewModelProvider;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import com.tks.uws.uwsmember.R;

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

		/* No決定 */
		view.findViewById(R.id.btnSetNo).setOnClickListener(view2 -> {

		});
	}
}