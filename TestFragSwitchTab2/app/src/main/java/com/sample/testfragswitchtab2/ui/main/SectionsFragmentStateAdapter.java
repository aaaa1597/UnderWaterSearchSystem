package com.sample.testfragswitchtab2.ui.main;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.sample.testfragswitchtab2.R;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsFragmentStateAdapter extends FragmentStateAdapter {
	public SectionsFragmentStateAdapter(FragmentActivity activity) {
		super(activity);
	}

	@NonNull
	@Override
	public Fragment createFragment(int idx) {
		return PlaceholderFragment.newInstance(idx);
	}

	@Override
	public int getItemCount() {
		return 2;	/* 初期設定と検索画面の2つ */
	}
}