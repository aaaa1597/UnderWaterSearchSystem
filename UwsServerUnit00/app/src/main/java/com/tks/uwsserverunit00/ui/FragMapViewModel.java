package com.tks.uwsserverunit00.ui;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.location.LocationRequest;
import android.util.Pair;
import java.util.ArrayList;
import java.util.List;

import com.tks.uwsserverunit00.TLog;

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
	/* Selected Seeker */
	private final MutableLiveData<Pair<Short, Boolean>>	mSelectedSeeker = new MutableLiveData<>(Pair.create((short)-32768, false));
	private final List<Pair<Short, Boolean>>			mSelectedSeekerList = new ArrayList<>();
	public MutableLiveData<Pair<Short, Boolean>> SelectedSeeker() { return mSelectedSeeker; }
	public void setChecked(short seekerid, boolean isChecked) {
		Pair<Short, Boolean> findit = mSelectedSeekerList.stream().filter(item->item.first==seekerid).findAny().orElse(null);
		if(findit==null) {
			/* 新規追加 */
			mSelectedSeekerList.add(Pair.create(seekerid, isChecked));
			mSelectedSeeker.setValue(Pair.create(seekerid, isChecked));
		}
		else {
			/* 値更新 */
			if(findit.second == isChecked)
				return;	/* 変更がないので何もしない */
			else {
				mSelectedSeekerList.remove(findit);
				mSelectedSeekerList.add(Pair.create(seekerid, isChecked));
				mSelectedSeeker.setValue(Pair.create(seekerid, isChecked));
			}
		}
	}

	public boolean isSelected(short seekerId) {
		Pair<Short, Boolean> findit = mSelectedSeekerList.stream().filter(item->item.first==seekerId).findAny().orElse(null);
		if(findit != null)
			return findit.second;
		else
			return false;
	}
}