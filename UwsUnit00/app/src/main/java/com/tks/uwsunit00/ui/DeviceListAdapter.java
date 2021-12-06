package com.tks.uwsunit00.ui;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import com.tks.uws.blecentral.DeviceInfo;
import com.tks.uwsunit00.R;

/**
 * -30 dBm	素晴らしい	達成可能な最大信号強度。クライアントは、これを実現するには、APから僅か数フィートである必要があります。現実的には一般的ではなく、望ましいものでもありません	N/A
 * -60 dBm	良好	非常に信頼性の高く、データパケットのタイムリーな伝送を必要とするアプリケーションのための最小信号強度	VoIP/VoWiFi, ストリーミングビデオ
 * -70 dBm	Ok	信頼できるパケット伝送に必要な最小信号強度	Email, web
 * -80 dBm	よくない	基本的なコネクティビティに必要な最小信号強度。パケット伝送は信頼できない可能性があります	N/A
 * -90 dBm	使用不可	ノイズレベルに近いかそれ以下の信号強度。殆ど機能しない	N/A
 * */

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {
	static class ViewHolder extends RecyclerView.ViewHolder {
		TextView	mTxtDeviceName;
		TextView	mTxtDeviceNameAddress;
		ImageView	mImvRssi;
		ImageView	mImvConnectStatus;
		TextView	mTxtHertBeat;
		Button		mBtnConnect;
		ImageButton mBtnBuoy;
		ViewHolder(View view) {
			super(view);
			mTxtDeviceName			= view.findViewById(R.id.device_name);
			mTxtDeviceNameAddress	= view.findViewById(R.id.device_address);
			mImvRssi				= view.findViewById(R.id.imvRssi);
			mImvConnectStatus		= view.findViewById(R.id.imvConnectStatus);
			mTxtHertBeat			= view.findViewById(R.id.txtHertBeat);
			mBtnConnect				= view.findViewById(R.id.btnConnect);
			mBtnBuoy				= view.findViewById(R.id.btnBuoy);
		}
	}

	/* インターフェース : DeviceListAdapterListener */
	public interface DeviceListAdapterListener {
		void onDeviceItemClick(View view, String deviceName, String deviceAddress);
	}

	/* メンバ変数 */
	private ArrayList<DevicveInfoModel>	mDeviceList = new ArrayList<>();
	private DeviceListAdapterListener	mListener;

	public enum ConnectStatus { NONE, CONNECTING, EXPLORING, CHECKAPPLI, TOBEPREPARED, READY}
	private static class DevicveInfoModel {
		public String			mDeviceName;
		public String			mDeviceAddress;
		public int				mDeviceRssi;
		public ConnectStatus	mConnectStatus;
		public int				mHertBeat;
		public DevicveInfoModel(String devicename, String deviceaddress, int devicerssi, ConnectStatus status, int hertbeat) {
			mDeviceName		= devicename;
			mDeviceAddress	= deviceaddress;
			mDeviceRssi		= devicerssi;
			mConnectStatus	= status;
			mHertBeat		= hertbeat;
		}
	}

	/* コンストラクタ */
	public DeviceListAdapter(DeviceListAdapterListener listener) {
		mListener = listener;
	}

	@NonNull
	@Override
	public DeviceListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_devices, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull DeviceListAdapter. ViewHolder holder, int position) {
		DevicveInfoModel model = mDeviceList.get(position);
		final String deviceName		= model.mDeviceName;
		final String deviceAddress	= model.mDeviceAddress;
		final int rssiresid	=	model.mDeviceRssi > -60 ? R.drawable.wifi_level_3 :
								model.mDeviceRssi > -70 ? R.drawable.wifi_level_2 :
								model.mDeviceRssi > -80 ? R.drawable.wifi_level_1 : R.drawable.wifi_level_0;
		final int constsresid = model.mConnectStatus == ConnectStatus.NONE			? R.drawable.status0_none :
								model.mConnectStatus == ConnectStatus.CONNECTING	? R.drawable.status1_connectiong :
								model.mConnectStatus == ConnectStatus.EXPLORING		? R.drawable.status2_exploring :
								model.mConnectStatus == ConnectStatus.CHECKAPPLI	? R.drawable.status3_chkappli :
								model.mConnectStatus == ConnectStatus.TOBEPREPARED	? R.drawable.status4_tobeprepared :
								model.mConnectStatus == ConnectStatus.READY			? R.drawable.status5_ready : R.drawable.status0_none;

		holder.mTxtDeviceName.setText(TextUtils.isEmpty(deviceName) ? "" : deviceName);
		holder.mTxtDeviceNameAddress.setText(TextUtils.isEmpty(deviceAddress) ? "" : deviceAddress);
		holder.mImvConnectStatus.setImageResource(constsresid);
		holder.mImvRssi.setImageResource(rssiresid);
		holder.mTxtHertBeat.setText(model.mHertBeat == 0 ? "-" : ""+model.mHertBeat);
//		holder.itemView.setOnClickListener(view -> {
//			/* 接続実行 */
//			if (mListener != null)
//				mListener.onDeviceItemClick(view, deviceName, deviceAddress);
//		});
		holder.mBtnConnect.setOnClickListener(v -> {
			/* 接続ボタン押下 */
			if (mListener != null)
				mListener.onDeviceItemClick(null, deviceName, deviceAddress);
		});
		holder.mBtnBuoy.setOnClickListener(v -> {
			/* 浮標ボタン押下 */
		});
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
			notifyDataSetChanged();
		}
	}

	public void addDevice(DeviceInfo deviceInfo) {
		addDevice(deviceInfo, true);
	}

	public void addDevice(DeviceInfo deviceInfo, boolean notify) {
		if (deviceInfo == null)
			return;

		int existingPosition = getPosition(deviceInfo.getDeviceAddress());

		if (existingPosition >= 0) {
			/* 追加済 更新する */
			mDeviceList.set(existingPosition, new DevicveInfoModel(deviceInfo.getDeviceName(), deviceInfo.getDeviceAddress(), deviceInfo.getDeviceRssi(), mDeviceList.get(existingPosition).mConnectStatus, mDeviceList.get(existingPosition).mHertBeat));
		}
		else {
			/* 新規追加 */
			mDeviceList.add(new DevicveInfoModel(deviceInfo.getDeviceName(), deviceInfo.getDeviceAddress(), deviceInfo.getDeviceRssi(), ConnectStatus.NONE, 0));
		}

		if (notify) {
			notifyDataSetChanged();
		}

	}

	public void setStatus(String address, ConnectStatus status) {
		int pos = getPosition(address);
		mDeviceList.get(pos).mConnectStatus = status;
		notifyItemChanged(pos);
	}

	public void setHertBeat(String address, int rcvval) {
		int pos = getPosition(address);
		mDeviceList.get(pos).mHertBeat = rcvval;
		notifyItemChanged(pos);
	}

	private int getPosition(String address) {
		int position = -1;
		for (int i = 0; i < mDeviceList.size(); i++) {
			if (mDeviceList.get(i).mDeviceAddress.equals(address)) {
				position = i;
				break;
			}
		}
		return position;
	}

	public void clearDevice() {
		mDeviceList.clear();
		notifyDataSetChanged();
	}
}
