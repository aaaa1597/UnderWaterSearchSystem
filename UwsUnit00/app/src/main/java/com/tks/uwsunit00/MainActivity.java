package com.tks.uwsunit00;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
	private final static int REQUEST_PERMISSIONS = 111;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/* 権限が許可されていない場合はリクエスト. */
		if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
			requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_PERMISSIONS);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		/* 権限リクエストの結果を取得する. */
		if (requestCode == REQUEST_PERMISSIONS) {
			if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
				ErrPopUp.create(MainActivity.this).setErrMsg("失敗しました。\nこのアプリでは、どうしようもないので終了します。").Show(MainActivity.this);
			}
		}
		else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}
}