package com.tks.uwsserverunit00.ui;

import android.os.RemoteException;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.tks.uwsserverunit00.DeviceInfo;
import com.tks.uwsserverunit00.IUwsInfoCallback;
import com.tks.uwsserverunit00.IUwsScanCallback;
import com.tks.uwsserverunit00.IUwsServer;
import com.tks.uwsserverunit00.IUwsSystemCallback;
import com.tks.uwsserverunit00.TLog;
import com.tks.uwsserverunit00.UwsInfo;

import static com.tks.uwsserverunit00.Constants.UWS_NG_AIDL_REMOTE_ERROR;
import static com.tks.uwsserverunit00.Constants.UWS_NG_SUCCESS;
import static com.tks.uwsserverunit00.Constants.d2Str;

public class FragBleViewModel extends ViewModel {
	private IUwsServer							mUwsServiceIf;
	/* ---------------- */
	private final MutableLiveData<Boolean>		mNotifyDataSetChanged	= new MutableLiveData<>(false);
	public MutableLiveData<Boolean>				NotifyDataSetChanged()	{ return mNotifyDataSetChanged; }
	/* ---------------- */
	private final MutableLiveData<Integer>		mNotifyItemChanged		= new MutableLiveData<>(-1);
	public MutableLiveData<Integer>				NotifyItemChanged()		{ return mNotifyItemChanged; }
	/* ---------------- */
	private final MutableLiveData<String>		mShowSnacbar			= new MutableLiveData<>("");
	public LiveData<String>						ShowSnacbar()			{ return mShowSnacbar; }
	public void									showSnacbar(String showmMsg) { mShowSnacbar.postValue(showmMsg);}
	/* ---------------- */
	private DeviceListAdapter					mDeviceListAdapter;
	public void									setDeviceListAdapter(DeviceListAdapter adapter)	{ mDeviceListAdapter = adapter; }
	public DeviceListAdapter					getDeviceListAdapter()	{ return mDeviceListAdapter; }
	/* ---------------- */
	private final MutableLiveData<UwsInfo>		mUpdUwsInfo				= new MutableLiveData<>(null);
	public MutableLiveData<UwsInfo>				UpdUwsInfo()			{ return mUpdUwsInfo; }
	/* ---------------- */
	private final MutableLiveData<Boolean>		mOnlySeeker				= new MutableLiveData<>(true);
	public MutableLiveData<Boolean>				OnlySeeker()			{ return mOnlySeeker; }
	/* ---------------- */

	/** *********************************************
	 * サービス接続完了 -> BLE初期化,scan開始
	 * **********************************************/
	public int onServiceConnected(IUwsServer service) {
		mUwsServiceIf = service;

		/* BLE初期化 */
		int ret;
		try { ret = mUwsServiceIf.initBle(new IUwsSystemCallback.Stub() {
						@Override
						public boolean getStartStopStatus(int seekerid) {
							return mDeviceListAdapter.getChecked((short)seekerid);
						}
					});
		}
		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_REMOTE_ERROR;}

		if(ret != UWS_NG_SUCCESS)
			return ret;

		/* scan開始 */
		int retscan = startScan(new IUwsScanCallback.Stub() {
			@Override
			public void notifyDeviceInfo(DeviceInfo device) {
				int pos = mDeviceListAdapter.updDeviceInfo(device, mOnlySeeker.getValue());
				if(pos >= 0)
					mNotifyDataSetChanged.postValue(true);
				if(device.getSeekerId()!=-1)
					mUpdUwsInfo.postValue(new UwsInfo(device));
			}
		});
		TLog.d("scan開始 ret={0}", retscan);

		return retscan;
	}

	/** *********
	 * サービス切断
	 * **********/
	public void onServiceDisconnected() {
		mUwsServiceIf = null;
	}

	/** **********
	 * Scan開始
	 * **********/
	public int startScan(IUwsScanCallback cb) {
		TLog.d("Scan開始.");
		int ret;
		try { ret = mUwsServiceIf.startScan(cb);}
		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_REMOTE_ERROR;}
		TLog.d("ret={0}", ret);

		if(ret != UWS_NG_SUCCESS)
			return ret;

		mDeviceListAdapter.clearDeviceInfo();
		mNotifyDataSetChanged.postValue(true);

		return UWS_NG_SUCCESS;
	}

	/** **********
	 * Scan終了
	 * **********/
	public void stopScan() {
		try { mUwsServiceIf.stopScan();}
		catch (RemoteException e) { e.printStackTrace(); return;}
		TLog.d("scan停止");
	}

	public void clearAll() {
		mDeviceListAdapter.clearDeviceInfo();
		mNotifyDataSetChanged.postValue(true);
	}

	public void clearDeviceWithoutAppliciated() {
		mDeviceListAdapter.clearDeviceWithoutAppliciated();
		mNotifyDataSetChanged.postValue(true);
	}

	public void setSelected(short seekerid, boolean isChecked) {
		/* TODO */TLog.d("seekerid={0} isChecked={1}", seekerid, isChecked);
		boolean nowStatus = mDeviceListAdapter.getChecked(seekerid);
		if(nowStatus == isChecked) return;
		/* TODO */TLog.d("seekerid={0} nowStatus:({1})->isChecked:({2})", seekerid, nowStatus, isChecked);

		/* メンバ選択Switchに設定 */
		int pos = mDeviceListAdapter.setSelected(seekerid, isChecked);
		mNotifyItemChanged.postValue(pos);

		/* サービスに通知(開始/終了) */
		int ret = UWS_NG_SUCCESS;
		try {
			if(isChecked)
				ret = mUwsServiceIf.startPeriodicNotify(seekerid, new IUwsInfoCallback.Stub() {
						@Override
						public void notifyUwsData(UwsInfo uwsInfo) {
							int pos = mDeviceListAdapter.updDeviceInfo(uwsInfo);
							mNotifyItemChanged.postValue(pos);
							mUpdUwsInfo.postValue(uwsInfo);
							TLog.d("UwsInfo受信({0} {1} {2} {3})", d2Str(uwsInfo.getDate()), d2Str(uwsInfo.getLongitude()), d2Str(uwsInfo.getLatitude()), uwsInfo.getHeartbeat());
						}

					@Override
					public void notifyStatus(int status) {
						/* TODO 要実装 */
					}
				});
			else
				mUwsServiceIf.stopPeriodicNotify(seekerid);
		}
		catch (RemoteException e) { e.printStackTrace(); ret = UWS_NG_AIDL_REMOTE_ERROR; }
		TLog.d("ret={0}", ret);
	}

	public void setBuoy(short seekerid, boolean isChecked) {
		mDeviceListAdapter.setBuoy(seekerid, isChecked);
		mNotifyDataSetChanged.postValue(true);
	}
}
