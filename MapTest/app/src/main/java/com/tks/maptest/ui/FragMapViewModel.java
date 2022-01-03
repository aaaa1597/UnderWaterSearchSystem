package com.tks.maptest.ui;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.location.LocationRequest;

public class FragMapViewModel extends ViewModel {
	/* Permission */
	private final MutableLiveData<Boolean> mPermission = new MutableLiveData<>(false);
	public MutableLiveData<Boolean> Permission() { return mPermission; }
	/* LocationRequest */
	private final LocationRequest mLocationRequest = LocationRequest.create().setInterval(1000)
																			.setFastestInterval(1000/2)
																			.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	public LocationRequest getLocationRequest() {
		return mLocationRequest;
	}
}