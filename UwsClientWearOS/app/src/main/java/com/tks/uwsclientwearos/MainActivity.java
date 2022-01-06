package com.tks.uwsclientwearos;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.tks.uwsclientwearos.databinding.ActivityMainBinding;

public class MainActivity extends Activity {

	private TextView mTextView;
	private ActivityMainBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/* SeekerIDのlistView定義 */
		RecyclerView recyclerView = findViewById(R.id.rvw_seekerid);
		recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false));
		recyclerView.setAdapter(new SeekerIdAdapter());
		/* SeekerIDのlistView(子の中心で収束する設定) */
		LinearSnapHelper linearSnapHelper = new LinearSnapHelper();
		linearSnapHelper.attachToRecyclerView(recyclerView);
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				if(newState == RecyclerView.SCROLL_STATE_IDLE) {
					View lview = linearSnapHelper.findSnapView(recyclerView.getLayoutManager());
					int pos =  recyclerView.getChildAdapterPosition(lview);
//					mViewModel.setSeekerID(pos);
				}
			}
		});
	}
}