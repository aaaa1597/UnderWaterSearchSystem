package com.tks.uwsclient.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;

import com.tks.uwsclient.R;

public class SeekerIdAdapter extends RecyclerView.Adapter<SeekerIdAdapter.ViewHolder> {
	static class ViewHolder extends RecyclerView.ViewHolder {
		ImageView mImvSeekerId;
		public ViewHolder(@NonNull View view) {
			super(view);
			mImvSeekerId = view.findViewById(R.id.imvSeekerId);
		}
	}

	private final List<Integer> mSeekersResIdList = Arrays.asList(
			R.drawable.num0, R.drawable.num1, R.drawable.num2, R.drawable.num3,
			R.drawable.num4, R.drawable.num5, R.drawable.num6, R.drawable.num7,
			R.drawable.num8, R.drawable.num9);

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_seekerid, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		int resid = mSeekersResIdList.get(position);
		holder.mImvSeekerId.setImageResource(resid);
	}

	@Override
	public int getItemCount() {
		return mSeekersResIdList.size();
	}
}
