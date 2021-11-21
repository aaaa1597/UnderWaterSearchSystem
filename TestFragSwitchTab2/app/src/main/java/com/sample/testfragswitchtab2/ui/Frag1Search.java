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

public class Frag1Search extends Fragment {

	private Frag1SearchViewModel mViewModel;

	public static Frag1Search newInstance() {
		return new Frag1Search();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.frag1_search, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mViewModel = new ViewModelProvider(this).get(Frag1SearchViewModel.class);
	}
}