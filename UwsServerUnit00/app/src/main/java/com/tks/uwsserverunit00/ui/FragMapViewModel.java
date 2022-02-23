package com.tks.uwsserverunit00.ui;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.util.Pair;
import com.google.android.gms.maps.model.LatLng;
import java.util.Date;

import com.tks.uwsserverunit00.ui.FragMap.MapDrawInfo;
import static com.tks.uwsserverunit00.Constants.BT_NORTIFY_CLOSE;
import static com.tks.uwsserverunit00.Constants.BT_NORTIFY_SEEKERID;

public class FragMapViewModel extends ViewModel {
	/* Permission */
	private final MutableLiveData<Boolean>	mPermission = new MutableLiveData<>(false);
	public MutableLiveData<Boolean>			Permission() { return mPermission; }
	/* LatLng */
	private final MutableLiveData<MapDrawInfo>	mOnLocationUpdated = new MutableLiveData<>();
	public MutableLiveData<MapDrawInfo>			OnLocationUpdated() { return mOnLocationUpdated; }
	/* Selected Seeker */
	private final MutableLiveData<Pair<String, Boolean>>mSelectedSeeker = new MutableLiveData<>();
	public MutableLiveData<Pair<String, Boolean>>		SelectedSeeker() { return mSelectedSeeker; }
	public void setSelected(String addr, boolean isChecked) {
		mSelectedSeeker.setValue(Pair.create(addr, isChecked));
	}
	/* 状態変化通知 */
	private final MutableLiveData<MapDrawInfo>	mOnChangeStatus = new MutableLiveData<>();
	public MutableLiveData<MapDrawInfo>			onChangeStatus() { return mOnChangeStatus; }

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

	public void onChangeStatus(String aname, String aaddr, int resourceid) {
		short lseekerid;
		if(aname.equals(BT_NORTIFY_SEEKERID)) {
			lseekerid = (short)resourceid;
		}
		else if(aname.equals(BT_NORTIFY_CLOSE)) {
			lseekerid = -1;
		}
		else {
			return;	/* 開始でも終了でもない時は、処理不要 */
		}

		MapDrawInfo mapinfo = new MapDrawInfo() {{
			seekerid= lseekerid;
			name	= aname;
			address	= aaddr;
			date	= new Date();
			pos		= new LatLng(0, 0);
			maker	= null;
			polygon	= null;
			circle	= null;
		}};
		mOnChangeStatus.postValue(mapinfo);
	}

	/* 指揮所位置リセット */
	public void resetCommanderPos() {
		mOnCommanderPosChange.postValue(new Point(9999, 9999));	/* 指揮所リセット */
	}

	/* 地図角度設定 */
	private final MutableLiveData<Integer>	mOnTiltChange = new MutableLiveData<>();
	public MutableLiveData<Integer>			onTiltChange() { return mOnTiltChange; }
	public void setTilt0() {
		mOnTiltChange.postValue(0);
	}

	/* 指揮所位置設定 */
	private final MutableLiveData<Point>	mOnCommanderPosChange = new MutableLiveData<>();
	public MutableLiveData<Point>			onCommanderPosChange() { return mOnCommanderPosChange; }
	public void setCommanderPos(int x, int y) {
		mOnCommanderPosChange.postValue(new Point(x, y));
	}

	/* 隊員位置 点滅実行 */
	private final MutableLiveData<String>	mOnMarkerTicked = new MutableLiveData<>();
	public MutableLiveData<String>			onMarkerTicked() { return mOnMarkerTicked; }
	public void onMarkerTicked(short seekerid, String address) {
		mOnMarkerTicked.postValue(address);
	}
}