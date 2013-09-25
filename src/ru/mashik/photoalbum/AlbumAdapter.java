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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AlbumAdapter extends BaseAdapter {
	Context context;
	LayoutInflater inflater;
	ArrayList<ListData> objects;
	int metrics;

	AlbumAdapter(Context context,ArrayList<ListData> list, int metrics) {
		
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
		View view = convertView;
		if (view==null){
			view =inflater.inflate(R.layout.list, parent,false);
		}
		ListData listdata=(ListData)getItem(position);
		
		//Заполняем TextView
        ((TextView) view.findViewById(R.id.textView1)).setText(listdata.title);
        ((TextView) view.findViewById(R.id.textView2)).setText(listdata.describe);
        try {
        	ImageView iv=(ImageView) view.findViewById(R.id.imageView1);
        	iv.getLayoutParams().height=metrics/6;
        	iv.getLayoutParams().width=iv.getLayoutParams().height;
			iv.setImageDrawable(Drawable.createFromStream(
					(InputStream) new URL(listdata.imageURL).getContent(), "src"));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
        view.getLayoutParams().height=metrics/6;
		return view;
	}

}
