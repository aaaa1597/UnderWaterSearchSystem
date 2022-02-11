package com.tks.uwsclient;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class StatusInfo implements Parcelable {
	private final int		mStatus;
	private final short		mSeekerId;

	protected StatusInfo(int status, short seekerid) {
		mStatus		= status;
		mSeekerId	= seekerid;
	}

	protected StatusInfo(Parcel in) {
		mStatus		= in.readInt();
		mSeekerId	= (short)in.readInt();
	}

	public static final Creator<StatusInfo> CREATOR = new Creator<StatusInfo>() {
		@Override
		public StatusInfo createFromParcel(Parcel in) {
			return new StatusInfo(in);
		}

		@Override
		public StatusInfo[] newArray(int size) {
			return new StatusInfo[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int i) {
		dest.writeInt(mStatus);
		dest.writeInt(mSeekerId);
	}

	public int		getStatus()		{ return mStatus;}
	public short	getSeekerId()	{ return mSeekerId;}
}
