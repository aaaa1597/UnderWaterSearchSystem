package com.tks.uws.uwsmember.ui.main;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FragMainViewModel extends ViewModel {
	private MutableLiveData<Double> mLatitude  = new MutableLiveData<>();
	private MutableLiveData<Double> mLongitude = new MutableLiveData<>();

	public MutableLiveData<Double> Latitude() {
		return mLatitude;
	}

	public MutableLiveData<Double> Longitude() {
		return mLongitude;
	}
}