package com.sample.testfragswitchtab2;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;
import com.sample.testfragswitchtab2.ui.main.SectionsFragmentStateAdapter;
import com.sample.testfragswitchtab2.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

	private ActivityMainBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ViewPager2 viewPager2 = findViewById(R.id.view_pager2);
		viewPager2.setAdapter(new SectionsFragmentStateAdapter(this));

		new TabLayoutMediator(findViewById(R.id.tabs), viewPager2,
				(tab, idx) -> tab.setText(idx==0 ? "初期設定" : "検索画面")
		).attach();
	}
}