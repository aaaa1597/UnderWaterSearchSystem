package com.test.blesample.central;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class DeviceConnectActivity extends AppCompatActivity {
	public static final String EXTRAS_DEVICE_NAME	= "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS= "DEVICE_ADDRESS";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_connect);
	}
}