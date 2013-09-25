package ru.mashik.photoalbum;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;


public class PhotoAdapter extends BaseAdapter {
	Context context;
	LayoutInflater inflater;
	int metrics;
	ImageView view;
	ArrayList<ListData> objects;

	PhotoAdapter(Context context,ArrayList<ListData> list, int metrics) {
		
		// TODO Auto-generated constructor stub
		this.metrics=metrics;
		this.context=context;
		objects=list;
		inflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return objects.size();
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return objects.get(arg0);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		ListData listdata=(ListData)getItem(position);

		if (convertView==null){
			view = new ImageView(context);
			view.setLayoutParams(new GridView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			view.setScaleType(ImageView.ScaleType.CENTER_CROP);
			view.getLayoutParams().height = (int)(metrics/3);
			view.getLayoutParams().width = (int)(metrics/3);
			
		}
		else {
			view = (ImageView)convertView;
		}
		try {
			view.setImageDrawable(Drawable.createFromStream(
					(InputStream) new URL(listdata.imageURL).getContent(), "src"));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block;
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
		return view;
	}
}
