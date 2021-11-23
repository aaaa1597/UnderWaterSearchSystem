package com.test.blesample.central;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class DeviceConnectActivity extends AppCompatActivity {
	public static final String EXTRAS_DEVICE_NAME	= "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS= "DEVICE_ADDRESS";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_connect);

		Intent intent = getIntent();
		((TextView)findViewById(R.id.txtConnectedDeviceName)).setText(intent.getStringExtra(EXTRAS_DEVICE_NAME));


	}
}