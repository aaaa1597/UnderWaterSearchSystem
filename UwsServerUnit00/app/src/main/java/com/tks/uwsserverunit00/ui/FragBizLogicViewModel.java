package com.tks.uwsserverunit00.ui;

import android.app.Application;
import android.util.Pair;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.HashMap;
import java.util.Map;

import static com.tks.uwsserverunit00.Constants.BT_NORTIFY_CLOSE;
import static com.tks.uwsserverunit00.Constants.BT_NORTIFY_SEEKERID;

public class FragBizLogicViewModel extends ViewModel {
	private boolean										mIsSerching = false;
	private Map<String, Short>							mSeekerids = new HashMap<>();
	private final MutableLiveData<Pair<Short, Short>>	mOnHearBeatChange = new MutableLiveData<>();
	public MutableLiveData<Pair<Short, Short>>			onHearBeatChange() { return mOnHearBeatChange; }
	private final MutableLiveData<Pair<Short, Short>>	mOnSeekeridChange = new MutableLiveData<>();
	public MutableLiveData<Pair<Short, Short>>			onSeekeridChange() { return mOnSeekeridChange; }

	public boolean getSerchStatus() { return mIsSerching; }
	public void setSerchStatus(boolean isSerching) { mIsSerching = isSerching; }

	public void setHeartBeat(short seekerid, String name, String addr, long datetime, short hearbeat) {
		if(seekerid == -1) return;
		mOnHearBeatChange.postValue(new Pair<>(seekerid, hearbeat));
	}

	/* 探索者IDの変更 */
	public void onSeekeridChange(String name, String address, int aseekerid) {
		if( !name.equals(BT_NORTIFY_SEEKERID) && !name.equals(BT_NORTIFY_CLOSE)) return;

		if(name.equals(BT_NORTIFY_SEEKERID)) {
			Short oldseekerid = mSeekerids.get(address);
			/* なければ、探索者IDの新規登録 */
			if(oldseekerid == null) {
				mSeekerids.put(address, (short)aseekerid);
				mOnSeekeridChange.postValue(new Pair<>((short)-1, (short)aseekerid));
			}
			/* 変化してれば、探索者IDの更新 */
			else if((short)oldseekerid != (short)aseekerid) {
				mSeekerids.put(address, (short)aseekerid);
				mOnSeekeridChange.postValue(new Pair<>((short)oldseekerid, (short)aseekerid));
			}
		}
		else {
			Short oldseekerid = mSeekerids.get(address);
			/* なければ、処理不要 */
			if(oldseekerid == null) {
			}
			/* あれば削除 */
			else if((short)oldseekerid != (short)aseekerid) {
				mSeekerids.remove(address);
				mOnSeekeridChange.postValue(new Pair<>((short)oldseekerid, (short)-1));
			}
		}
	}
}