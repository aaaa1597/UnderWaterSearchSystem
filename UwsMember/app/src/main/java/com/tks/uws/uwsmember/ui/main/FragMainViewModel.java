package com.tks.uws.uwsmember.ui.main;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FragMainViewModel extends ViewModel {
	private final MutableLiveData<Double>	mLatitude		= new MutableLiveData<>(0.0);
	private final MutableLiveData<Double>	mLongitude		= new MutableLiveData<>(0.0);
	private final MutableLiveData<Integer>	mHearBeat		= new MutableLiveData<>(0);
	private final MutableLiveData<Integer>	mID				= new MutableLiveData<>(-1);
	public MutableLiveData<Double>	Latitude()	{
		return mLatitude;
	}
	public MutableLiveData<Double>	Longitude()	{
		return mLongitude;
	}
	public MutableLiveData<Integer>	HearBeat()	{
		return mHearBeat;
	}
	public void setID(int id)	{
		mID.setValue(id);
	}
	public int getID()	{
		Integer ret = mID.getValue();
		return ret == null ? -1 : ret;
	}
}