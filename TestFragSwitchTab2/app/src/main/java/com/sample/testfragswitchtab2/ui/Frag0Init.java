package com.sample.testfragswitchtab2.ui;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sample.testfragswitchtab2.R;

public class Frag0Init extends Fragment {

	private Frag0InitViewModel mViewModel;

	public static Frag0Init newInstance() {
		return new Frag0Init();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.frag0_init, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mViewModel = new ViewModelProvider(this).get(Frag0InitViewModel.class);
	}
}