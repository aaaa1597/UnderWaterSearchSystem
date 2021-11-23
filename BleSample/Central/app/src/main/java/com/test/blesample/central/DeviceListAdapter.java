package com.test.blesample.central;

import android.bluetooth.le.ScanResult;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {
	/* サブクラス : DeviceListAdapter.ViewHolder */
	static class ViewHolder extends RecyclerView.ViewHolder {
		TextView mTxtDeviceName;
		TextView mTxtDeviceAddress;

		ViewHolder(View view) {
			super(view);
			mTxtDeviceName		= (TextView)view.findViewById(R.id.txtDeviceName);
			mTxtDeviceAddress	= (TextView)view.findViewById(R.id.txtDeviceAddress);
		}
	}

	/* インターフェース : DeviceListAdapterListener */
	public interface DeviceListAdapterListener {
		void onDeviceItemClick(String deviceName, String deviceAddress);
	}

	/* メンバ変数 */
	private ArrayList<ScanResult>		mDeviceList = new ArrayList<ScanResult>();
	private DeviceListAdapterListener	mListener;

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
	public void onBindViewHolder(@NonNull DeviceListAdapter.ViewHolder holder, int position) {
		ScanResult scanResult = mDeviceList.get(position);
		final String deviceName		= scanResult.getDevice().getName();
		final String deviceAddress	= scanResult.getDevice().getAddress();

		holder.mTxtDeviceName   .setText(TextUtils.isEmpty(deviceName)    ? "" : deviceName);
		holder.mTxtDeviceAddress.setText(TextUtils.isEmpty(deviceAddress) ? "" : deviceAddress);
		holder.itemView.setOnClickListener(view -> {
			if (!TextUtils.isEmpty(deviceName) && !TextUtils.isEmpty(deviceAddress)) {
				if (mListener != null) {
					mListener.onDeviceItemClick(deviceName, deviceAddress);
				}
			}
		});
	}

	@Override
	public int getItemCount() {
		return mDeviceList.size();
	}

	public void addDevice(List<ScanResult> scanResults) {
		if(scanResults == null)
			return;

		for (ScanResult scanResult : scanResults)
			addDevice(scanResult, false);

		notifyDataSetChanged();
	}

	public void addDevice(ScanResult scanResult) {
		addDevice(scanResult, true);
	}

	public void addDevice(ScanResult scanResult, boolean isNotify) {
		if (scanResult == null)
			return;

		int idx = IntStream.range(0, mDeviceList.size())
						.filter(lidx->mDeviceList.get(lidx).getDevice().getAddress().equals(scanResult.getDevice().getAddress()))
						.findFirst().orElse(-1);
		if(idx == -1)
			mDeviceList.add(scanResult);
		else
			mDeviceList.set(idx, scanResult);

		if (isNotify)
			notifyDataSetChanged();
	}

	public void clearDevice() {
		mDeviceList.clear();
		notifyDataSetChanged();
	}
}
