package com.tks.uwsserverunit00.ui;

import android.graphics.Color;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.tks.uwsserverunit00.DeviceInfo;
import com.tks.uwsserverunit00.R;
import static com.tks.uwsserverunit00.Constants.UWS_NG_DEVICE_NOTFOUND;

/**
 * -30 dBm	素晴らしい	達成可能な最大信号強度。クライアントは、これを実現するには、APから僅か数フィートである必要があります。現実的には一般的ではなく、望ましいものでもありません	N/A
 * -60 dBm	良好	非常に信頼性の高く、データパケットのタイムリーな伝送を必要とするアプリケーションのための最小信号強度	VoIP/VoWiFi, ストリーミングビデオ
 * -70 dBm	Ok	信頼できるパケット伝送に必要な最小信号強度	Email, web
 * -80 dBm	よくない	基本的なコネクティビティに必要な最小信号強度。パケット伝送は信頼できない可能性があります	N/A
 * -90 dBm	使用不可	ノイズレベルに近いかそれ以下の信号強度。殆ど機能しない	N/A
 * */

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {
	static class ViewHolder extends RecyclerView.ViewHolder {
//		TextView	mTxtShortUuid;		サービスUUIDは内部保持の変数なので見せない
//		TextView	mTxtPrevShortUuid;	サービスUUIDは内部保持の変数なので見せない
		TextView	mTxtSeekerId;
		TextView	mTxtDeviceName;
		TextView	mTxtDeviceNameAddress;
		ImageView	mImvRssi;
		ImageView	mImvConnectStatus;
		TextView	mTxtConnectStatus;
		TextView	mTxtHertBeat;
		CheckBox	mCkbConnect;
		ImageButton	mBtnBuoy;
		TextView	mTxtLongitude;
		TextView	mTxtLatitude;
		ViewHolder(View view) {
			super(view);
//			mTxtShortUuid			= view.findViewById(R.id.txtShortUuid);		サービスUUIDは内部保持の変数なので見せない
//			mTxtPrevShortUuid		= view.findViewById(R.id.txtPrevShortUuid);	サービスUUIDは内部保持の変数なので見せない
			mTxtSeekerId			= view.findViewById(R.id.txtSeekerId);
			mTxtDeviceName			= view.findViewById(R.id.txtDeeviceName);
			mTxtDeviceNameAddress	= view.findViewById(R.id.txtDeviceAddress);
			mImvRssi				= view.findViewById(R.id.imvRssi);
			mImvConnectStatus		= view.findViewById(R.id.imvConnectStatus);
			mTxtConnectStatus		= view.findViewById(R.id.txtConnectStatus);
			mTxtHertBeat			= view.findViewById(R.id.txtHertBeat);
			mCkbConnect				= view.findViewById(R.id.ckbConnect);
			mBtnBuoy				= view.findViewById(R.id.btnBuoy);
			mTxtLongitude			= view.findViewById(R.id.txtLongitude);
			mTxtLatitude			= view.findViewById(R.id.txtLatitude);
		}
	}

	/* インターフェース : OnConnectBtnClickListener */
	public interface OnConnectBtnClickListener {
		void OnConnectBtnClick(View view, boolean isChecked, String deviceName, String deviceAddress);
	}
	private OnConnectBtnClickListener mClickListener;
	/* コンストラクタ */
	public DeviceListAdapter(OnConnectBtnClickListener clickListener) {
		mClickListener = clickListener;
	}

	/* メンバ変数 */
	private ArrayList<DevicveInfoModel> mDeviceList = new ArrayList<>();
	public enum ConnectStatus { NONE, CONNECTING, EXPLORING, CHECKAPPLI, TOBEPREPARED, WAITFORREAD, READSUCCEED, DISCONNECTED, FAILURE, OUTOFSERVICE}
	private static class DevicveInfoModel {
		public String			mShortUuid;
		public String			mPrevShortUuid;
		public int				mSeekerId;
		public String			mDeviceName;
		public String			mDeviceAddress;
		public int				mDeviceRssi;
		public ConnectStatus	mConnectStatus;
		public int				mHertBeat;
		public boolean			mIsApplicable;
		public boolean			mIsReading;
		public double			mLongitude;
		public double			mLatitude;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_devices, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		DevicveInfoModel model = mDeviceList.get(position);
		final String deviceName		= model.mDeviceName;
		final String deviceAddress	= model.mDeviceAddress;
		final int rssiresid	=	model.mDeviceRssi > -60 ? R.drawable.wifi_level_3 :
								model.mDeviceRssi > -70 ? R.drawable.wifi_level_2 :
								model.mDeviceRssi > -80 ? R.drawable.wifi_level_1 : R.drawable.wifi_level_0;
		final int constsresid =	!model.mIsApplicable								? R.drawable.statusx_na :
								model.mConnectStatus == ConnectStatus.TOBEPREPARED	? R.drawable.status5_ready :
																					  R.drawable.status0_none;
		final Pair<String, Integer> statusinfo =
								model.mConnectStatus == ConnectStatus.NONE			? Pair.create("", Color.BLACK) :
								model.mConnectStatus == ConnectStatus.CONNECTING	? Pair.create("接続中", Color.rgb(0xf0, 0xe6, 0x8c)) :
								model.mConnectStatus == ConnectStatus.EXPLORING		? Pair.create("探索中", Color.YELLOW) :
								model.mConnectStatus == ConnectStatus.CHECKAPPLI	? Pair.create("対象確認中", Color.rgb(0xff, 0xd7, 0x00)) :
								model.mConnectStatus == ConnectStatus.TOBEPREPARED	? Pair.create("通信中", Color.BLUE) :
								model.mConnectStatus == ConnectStatus.WAITFORREAD	? Pair.create("読込中", Color.BLUE) :
								model.mConnectStatus == ConnectStatus.READSUCCEED	? Pair.create("読込成功.", Color.BLUE) :
								model.mConnectStatus == ConnectStatus.DISCONNECTED	? Pair.create("切断!!", Color.RED) :
								model.mConnectStatus == ConnectStatus.FAILURE		? Pair.create("失敗!!", Color.RED) :
								model.mConnectStatus == ConnectStatus.OUTOFSERVICE	? Pair.create("対象外!!", Color.rgb(0xff,0x8c,00)) : Pair.create("", Color.BLACK);
		holder.mTxtDeviceName.setText(TextUtils.isEmpty(deviceName) ? "" : deviceName);
		holder.mTxtDeviceNameAddress.setText(TextUtils.isEmpty(deviceAddress) ? "" : deviceAddress);
		holder.mTxtSeekerId.setText((model.mSeekerId ==-1) ? " - " : String.valueOf(model.mSeekerId));
		holder.mImvConnectStatus.setImageResource(constsresid);
		holder.mTxtConnectStatus.setText(statusinfo.first);
		holder.mTxtConnectStatus.setTextColor(statusinfo.second);
		holder.mImvRssi.setImageResource(rssiresid);
		holder.mTxtHertBeat.setText(model.mHertBeat == 0 ? "-" : ""+model.mHertBeat);
		holder.mCkbConnect.setChecked(model.mIsReading);
		holder.mCkbConnect.setOnCheckedChangeListener((view, isChecked) -> {
			model.mIsReading = isChecked;
			mClickListener.OnConnectBtnClick(view, isChecked, deviceName, deviceAddress);
		});
		holder.mBtnBuoy.setOnClickListener(v -> {
			/* 浮標ボタン押下 */
		});
		holder.mTxtLongitude.setText(String.valueOf(model.mLongitude));
		holder.mTxtLatitude .setText(String.valueOf(model.mLatitude));
	}

	@Override
	public int getItemCount() {
		return mDeviceList.size();
	}

	public void addDevice(List<DeviceInfo> deviceInfos) {
		if (deviceInfos != null) {
			for (DeviceInfo deviceInfo : deviceInfos) {
				addDevice(deviceInfo, false);
			}
//			notifyDataSetChanged();
		}
	}

	public void addDevice(DeviceInfo deviceInfo) {
		addDevice(deviceInfo, true);
	}

	public void addDevice(DeviceInfo deviceInfo, boolean notify) {
		if (deviceInfo == null)
			return;

		DevicveInfoModel model = mDeviceList.stream().filter(item -> item.mShortUuid.equals(deviceInfo.getShortUuid())).findFirst().orElse(null);
		if(model == null) {
			/* 新規追加 */
			mDeviceList.add(
					new DevicveInfoModel() {{
						mShortUuid		= deviceInfo.getShortUuid();
						mPrevShortUuid	= deviceInfo.getShortUuid();
						mSeekerId		= deviceInfo.getSeekerId();
						mDeviceName		= deviceInfo.getDeviceName();
						mDeviceAddress	= deviceInfo.getDeviceAddress();
						mDeviceRssi		= deviceInfo.getDeviceRssi();
						mConnectStatus	= ConnectStatus.NONE;
						mHertBeat		= 0;
						mIsApplicable	= deviceInfo.isApplicable();
						mIsReading		= false;
						mLongitude		= 0.0;
						mLatitude		= 0.0;
					}});
		}
		else {
			model.mPrevShortUuid= model.mShortUuid;
			model.mShortUuid	= deviceInfo.getShortUuid();
//			model.mPrevShortUuid= model.mShortUuid;
			model.mSeekerId		= deviceInfo.getSeekerId();
			model.mDeviceName	= deviceInfo.getDeviceName();
			model.mDeviceAddress= deviceInfo.getDeviceAddress();
			model.mDeviceRssi	= deviceInfo.getDeviceRssi();
//			model.mConnectStatus= model.mConnectStatus;	更新しない
//			model.mHertBeat		= model.mHertBeat;		更新しない
			model.mIsApplicable	= deviceInfo.isApplicable();
			model.mIsReading	= deviceInfo.isReading();
			model.mLongitude	= deviceInfo.getLongitude();
			model.mLatitude		= deviceInfo.getLatitude();
			/* 並び替え。 */
			mDeviceList.sort((o1, o2) -> {
				/* SeekerIdの昇順 */
				if(o1.mIsApplicable && o2.mIsApplicable)
					return Integer.compare(o1.mSeekerId, o2.mSeekerId);
				/* 対象のデバイス優先 */
				else if(o1.mIsApplicable && !o2.mIsApplicable)
					return -1;
				else if(!o1.mIsApplicable && o2.mIsApplicable)
					return 1;

				/* 次にアドレス名で並び替え */
				int compare = o1.mDeviceAddress.compareTo(o2.mDeviceAddress);
				if(compare == 0) return 0;
				return compare < 0 ? -1 : 1;
			});
		}

//		if (notify) {
//			notifyDataSetChanged();
//		}
	}

	public int setStatus(String address, ConnectStatus status) {
		AtomicInteger index = new AtomicInteger(-1);
		DevicveInfoModel device = mDeviceList.stream().peek(x->index.incrementAndGet()).filter(item -> item.mDeviceAddress.equals(address)).findFirst().orElse(null);
		if(device == null)
			return UWS_NG_DEVICE_NOTFOUND;
		device.mConnectStatus = status;
//		notifyItemChanged(pos);
		return index.get();
	}

	public int setStatusAndReadData(String address, ConnectStatus status, double longitude, double latitude, int heartbeat) {
		AtomicInteger index = new AtomicInteger(-1);
		DevicveInfoModel device = mDeviceList.stream().peek(x->index.incrementAndGet()).filter(item -> item.mDeviceAddress.equals(address)).findFirst().orElse(null);
		if(device == null)
			return UWS_NG_DEVICE_NOTFOUND;

		device.mConnectStatus= status;
		device.mLongitude	= longitude;
		device.mLatitude	= latitude;
		device.mHertBeat	= heartbeat;
//		notifyItemChanged(pos);
		return index.get();
	}

	public void clearDevice() {
		mDeviceList.clear();
//		notifyDataSetChanged();
	}
}
