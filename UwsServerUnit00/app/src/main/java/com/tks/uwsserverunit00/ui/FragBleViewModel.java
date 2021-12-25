package com.tks.uwsserverunit00.ui;

import android.os.RemoteException;
import android.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.tks.uwsserverunit00.DeviceInfo;
import com.tks.uwsserverunit00.IBleServerService;
import com.tks.uwsserverunit00.IBleServerServiceCallback;
import com.tks.uwsserverunit00.TLog;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tks.uwsserverunit00.Constants.UWS_NG_SUCCESS;
import static com.tks.uwsserverunit00.Constants.UWS_NG_AIDL_REMOTE_ERROR;

public class FragBleViewModel extends ViewModel {
	private IBleServerService				mBleServiceIf;
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
	public int onServiceConnected(IBleServerService service) {
		mBleServiceIf = service;

		/* コールバック設定 */
		try { mBleServiceIf.setCallback(mCb); }
		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_REMOTE_ERROR;}

		/* BLE初期化 */
		int ret;
		try { ret = mBleServiceIf.initBle(); }
		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_REMOTE_ERROR;}

		if(ret != UWS_NG_SUCCESS)
			return ret;

		/* scan開始 */
		int retscan = startScan();
		TLog.d("scan開始 ret={0}", retscan);

		return retscan;
	}

	/** *****************
	 * サービス接続Callback
	 * *****************/
	public void onServiceDisconnected() {
		mBleServiceIf = null;
	}

	/** **********
	 * Scan開始
	 * **********/
	public int startScan() {
		TLog.d("Scan開始.");
		int ret;
		try { ret = mBleServiceIf.startScan();}
		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_REMOTE_ERROR;}
		TLog.d("ret={0}", ret);

		if(ret != UWS_NG_SUCCESS)
			return ret;

		mDeviceListAdapter.clearDevice();
		mNotifyDataSetChanged.postValue(true);

		return UWS_NG_SUCCESS;
	}

	/** **********
	 * Scan終了
	 * **********/
	public void stopScan() {
		int ret;
		try { ret = mBleServiceIf.stopScan();}
		catch (RemoteException e) { e.printStackTrace(); return;}
		TLog.d("scan停止 ret={0}", ret);
	}

	/** *****************
	 * AIDLコールバック
	 * *****************/
	private final Map<Pair<String, String>, Boolean> mMsg = new HashMap<>();
	private final IBleServerServiceCallback mCb = new IBleServerServiceCallback.Stub() {
		@Override
		public void notifyDeviceInfolist(List<DeviceInfo> devices) {
			mDeviceListAdapter.addDevice(devices);
			mNotifyDataSetChanged.postValue(true);
		}

		@Override
		public void notifyDeviceInfo(DeviceInfo device) {
			mDeviceListAdapter.addDevice(device);
			mNotifyDataSetChanged.postValue(true);
//			TLog.d("発見!! No:{0}, {1}({2}):Rssi({3})", device.getSeekerId(), device.getDeviceAddress(), device.getDeviceName(), device.getDeviceRssi());
		}
	};
}
