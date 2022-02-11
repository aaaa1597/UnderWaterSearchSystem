package com.tks.uwsclientwearos;

import android.os.Parcel;
import android.os.Parcelable;

public class UwsInfo implements Parcelable {
	private final double	mLogitude;
	private final double	mLatitude;
	private final short		mHeartbeat;

	protected UwsInfo(double logitude, double latitude, short heartbeat) {
		mLogitude = logitude;
		mLatitude = latitude;
		mHeartbeat= heartbeat;
	}

	protected UwsInfo(Parcel in) {
		mLogitude = in.readDouble();
		mLatitude = in.readDouble();
		mHeartbeat= (short)in.readInt();
	}

	public static final Creator<UwsInfo> CREATOR = new Creator<UwsInfo>() {
		@Override
		public UwsInfo createFromParcel(Parcel in) {
			return new UwsInfo(in);
		}

		@Override
		public UwsInfo[] newArray(int size) {
			return new UwsInfo[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int i) {
		dest.writeDouble(mLogitude);
		dest.writeDouble(mLatitude);
		dest.writeInt(mHeartbeat);
	}

	public double	getLogitude()	{ return mLogitude;}
	public double	getLatitude()	{ return mLatitude;}
	public short	getHeartbeat()	{ return mHeartbeat;}
}
