package com.sample.testdrawer;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class PeripheralInfo {
	public String	Name;
	public String	UUID;
	public int		resID;
}

public class DrawerListAdapter extends ArrayAdapter<PeripheralInfo> {
	private final LayoutInflater mInflater;

	public DrawerListAdapter(@NonNull Context context) {
		super(context, 0);
		mInflater = LayoutInflater.from(context);
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		if(convertView == null) {
			convertView = mInflater.inflate(R.layout.drawerlist_item, parent, false);
		}

		PeripheralInfo info = getItem(position);
		/* 名前 */
		TextView txtname = convertView.findViewById(R.id.txtName);
		txtname.setText(info.Name);
		/* UUID */
		TextView txtuuid = convertView.findViewById(R.id.txtUUID);
		txtuuid.setText(info.UUID);
		/* icon */
		ImageView imgicon = convertView.findViewById(R.id.imgIcon);
		imgicon.setImageResource(info.resID);

		return convertView;
	}
}
