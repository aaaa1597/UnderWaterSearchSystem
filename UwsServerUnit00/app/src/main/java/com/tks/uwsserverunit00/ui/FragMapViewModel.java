package com.tks.uwsserverunit00.ui;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.graphics.Color;
import android.location.Location;
import android.util.Pair;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.tks.uwsserverunit00.ui.FragMap.MapDrawInfo;

import java.util.Date;

public class FragMapViewModel extends ViewModel {
	/* Permission */
	private final MutableLiveData<Boolean>	mPermission = new MutableLiveData<>(false);
	public MutableLiveData<Boolean>			Permission() { return mPermission; }
	/* LatLng */
	private final MutableLiveData<MapDrawInfo>	mOnLocationUpdated = new MutableLiveData<>();
	public MutableLiveData<MapDrawInfo>			OnLocationUpdated() { return mOnLocationUpdated; }
	/* Selected Seeker */
	private final MutableLiveData<Pair<Short, Boolean>>	mSelectedSeeker = new MutableLiveData<>(Pair.create((short)-32768, false));
	public MutableLiveData<Pair<Short, Boolean>>		SelectedSeeker() { return mSelectedSeeker; }
	public void setSelected(short seekerid, boolean isChecked) {
		mSelectedSeeker.setValue(Pair.create(seekerid, isChecked));
	}

	/* Change Fill Color */
	private int mFillColorCnt = 0;
	/* 塗りつぶし色カウンタ */
	public int incrementFillColorCnt() {
		mFillColorCnt++;
		mFillColorCnt%=10;
		return mFillColorCnt;
	}

	/* 塗りつぶし色生成 */
	static private int createColor(int cnt) {
		switch(cnt) {
			case 0: return Color.rgb(  0,   0,   0);
			case 1: return Color.rgb(127,  79,  33);
			case 2: return Color.rgb(234,  44,  59);
			case 3: return Color.rgb(255, 106,  54);
			case 4: return Color.rgb(234, 190,  59);
			case 5: return Color.rgb(  0, 145,  58);
			case 6: return Color.rgb( 46, 167, 224);
			case 7: return Color.rgb(149,  85, 170);
			case 8: return Color.rgb(128, 128, 128);
			case 9: return Color.rgb(240, 240, 240);
			default:
				return Color.rgb(  0,   0,   0);
		}
	}

	/* 塗りつぶし色取得 */
	public int getFillColor() {
		return createColor(mFillColorCnt);
	}

	public void onLocationUpdated(short aseekerid, String aname, String addr, long datetime, Location loc) {
		MapDrawInfo mapinfo = new MapDrawInfo() {{
			seekerid= aseekerid;
			name	= aname;
			address	= addr;
			date	= new Date(datetime);
			pos		= new LatLng(loc.getLatitude(), loc.getLongitude());
			maker	= null;
			polygon	= null;
			circle	= null;
		}};
		mOnLocationUpdated.postValue(mapinfo);
	}
}