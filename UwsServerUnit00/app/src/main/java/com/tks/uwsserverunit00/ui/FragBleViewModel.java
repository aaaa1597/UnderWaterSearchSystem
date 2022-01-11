package com.tks.uwsserverunit00.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FragBleViewModel extends ViewModel {
	/* ---------------- */
	private final MutableLiveData<Boolean>	mNotifyDataSetChanged	= new MutableLiveData<>(false);
	public MutableLiveData<Boolean>			NotifyDataSetChanged()	{ return mNotifyDataSetChanged; }
	/* ---------------- */
	private final MutableLiveData<Integer>	mNotifyItemChanged		= new MutableLiveData<>(-1);
	public MutableLiveData<Integer>			NotifyItemChanged()		{ return mNotifyItemChanged; }
	/* ---------------- */
	private final MutableLiveData<String>	mShowSnacbar			= new MutableLiveData<>("");
	public LiveData<String>					ShowSnacbar()			{ return mShowSnacbar; }
	public void								showSnacbar(String showmMsg) { mShowSnacbar.postValue(showmMsg);}
	/* ---------------- */
	private DeviceListAdapter				mDeviceListAdapter;
	public void								setDeviceListAdapter(DeviceListAdapter adapter)	{ mDeviceListAdapter = adapter; }
	public DeviceListAdapter				getDeviceListAdapter()	{ return mDeviceListAdapter; }
	/* ---------------- */
	/** *****************
	 * サービス接続Callback
	 * *****************/
//	public int onServiceConnected(IUwsServer service) {
//		mBleServiceIf = service;
//
//		/* コールバック設定 */
//		try { mBleServiceIf.setCallback(mCb); }
//		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_REMOTE_ERROR;}
//
//		/* BLE初期化 */
//		int ret;
//		try { ret = mBleServiceIf.initBle(); }
//		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_REMOTE_ERROR;}
//
//		if(ret != UWS_NG_SUCCESS)
//			return ret;
//
//		/* scan開始 */
//		int retscan = startScan();
//		TLog.d("scan開始 ret={0}", retscan);
//
//		return retscan;
//	}

	/** *****************
	 * サービス接続Callback
	 * *****************/
//	public void onServiceDisconnected() {
//		mBleServiceIf = null;
//	}

	public void clearDeviceWithoutAppliciated() {
		mDeviceListAdapter.clearDeviceWithoutAppliciated();
		mNotifyDataSetChanged.postValue(true);
	}

	public void setChecked(short seekerid, boolean isChecked) {
		int idx = mDeviceListAdapter.setChecked(seekerid, isChecked);
		mNotifyItemChanged.postValue(idx);
	}

	public void setBuoy(short seekerid, boolean isChecked) {
		mDeviceListAdapter.setBuoy(seekerid, isChecked);
		mNotifyDataSetChanged.postValue(true);
	}
}
