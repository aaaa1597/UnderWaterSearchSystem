package com.tks.uws.blecentral;

import android.bluetooth.le.ScanResult;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static com.tks.uws.blecentral.DeviceListAdapter.ConnectStatus.NONE;

/**
 * Created by itanbarpeled on 28/01/2018.
 */

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {
	/* インターフェース : DeviceListAdapterListener */
	public interface DeviceListAdapterListener {
		void onDeviceItemClick(View view, String deviceName, String deviceAddress);
	}

	public enum ConnectStatus { NONE, CONNECTING, EXPLORING, CHECKAPPLI, TOBEPREPARED, READY}
	private class DevicveInfoModel {
		public ScanResult		mScanResult;
		public ConnectStatus	mStatus;
		public int				mHertBeat;
		public DevicveInfoModel(ScanResult scanResult, ConnectStatus status) {
			mScanResult	= scanResult;
			mStatus		= status;
			mHertBeat	= 0;
		}
	}

	static class ViewHolder extends RecyclerView.ViewHolder {
		TextView	mTxtDeviceName;
		TextView	mTxtDeviceNameAddress;
		ImageView	mImvRssi;
		ImageView	mImvConnectStatus;
		TextView	mTxtHertBeat;
		ViewHolder(View view) {
			super(view);
			mTxtDeviceName			= view.findViewById(R.id.device_name);
			mTxtDeviceNameAddress	= view.findViewById(R.id.device_address);
			mImvRssi				= view.findViewById(R.id.imvRssi);
			mImvConnectStatus		= view.findViewById(R.id.imvConnectStatus);
			mTxtHertBeat			= view.findViewById(R.id.txtHertBeat);
		}
	}

	/* メンバ変数 */
	private ArrayList<DevicveInfoModel>	mDeviceList = new ArrayList<>();
	private DeviceListAdapterListener	mListener;

	public DeviceListAdapter(DeviceListAdapterListener listener) {
		mListener = listener;
	}

	public DeviceListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.lay_listitem_devices, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		DevicveInfoModel model = mDeviceList.get(position);
		final String deviceName		= model.mScanResult.getDevice().getName();
		final String deviceAddress	= model.mScanResult.getDevice().getAddress();
		final int rssiresid	=	model.mScanResult.getRssi() > -40 ? R.drawable.wifi_level_3 :
								model.mScanResult.getRssi() > -50 ? R.drawable.wifi_level_2 :
								model.mScanResult.getRssi() > -60 ? R.drawable.wifi_level_1 : R.drawable.wifi_level_0;
		final int constsresid = model.mStatus == ConnectStatus.NONE			? R.drawable.status0_none :
								model.mStatus == ConnectStatus.CONNECTING	? R.drawable.status1_connectiong :
								model.mStatus == ConnectStatus.EXPLORING	? R.drawable.status2_exploring :
								model.mStatus == ConnectStatus.CHECKAPPLI	? R.drawable.status3_chkappli :
								model.mStatus == ConnectStatus.TOBEPREPARED	? R.drawable.status4_tobeprepared :
								model.mStatus == ConnectStatus.READY		? R.drawable.status5_ready : R.drawable.status0_none;

        holder.mTxtDeviceName.setText(TextUtils.isEmpty(deviceName) ? "" : deviceName);
        holder.mTxtDeviceNameAddress.setText(TextUtils.isEmpty(deviceAddress) ? "" : deviceAddress);
		holder.mImvConnectStatus.setImageResource(constsresid);
		holder.mImvRssi.setImageResource(rssiresid);
		holder.mTxtHertBeat.setText(model.mHertBeat == 0 ? "-" : ""+model.mHertBeat);
		holder.itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
                if (mListener != null)
                    mListener.onDeviceItemClick(view, deviceName, deviceAddress);
			}
		});
	}

	@Override
	public int getItemCount() {
		return mDeviceList.size();
	}

	public void addDevice(ScanResult scanResult) {
		addDevice(scanResult, true);
	}

	public void addDevice(ScanResult scanResult, boolean notify) {
		if (scanResult == null)
			return;

		int existingPosition = getPosition(scanResult.getDevice().getAddress());

		if (existingPosition >= 0) {
			/* 追加済 更新する */
			mDeviceList.set(existingPosition, new DevicveInfoModel(scanResult, mDeviceList.get(existingPosition).mStatus));
		}
		else {
            /* 新規追加 */
			mDeviceList.add(new DevicveInfoModel(scanResult, NONE));
		}

		if (notify) {
			notifyDataSetChanged();
		}

	}

	public void addDevice(List<ScanResult> scanResults) {
		if (scanResults != null) {
			for (ScanResult scanResult : scanResults) {
				addDevice(scanResult, false);
			}
			notifyDataSetChanged();
		}
	}

	public void setStatus(String address, ConnectStatus status) {
		int pos = getPosition(address);
		mDeviceList.get(pos).mStatus = status;
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
			if (mDeviceList.get(i).mScanResult.getDevice().getAddress().equals(address)) {
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
