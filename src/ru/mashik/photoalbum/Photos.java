package ru.mashik.photoalbum;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.View;
import android.widget.GridView;

public class Photos extends Activity {
	String PhotoName[];
	ArrayList<ListData> photos;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_photos);
		//для отрисовки столбцов GridView
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		GridView gridview = (GridView) findViewById(R.id.gridView1);
		//Отрисовка TitleBar
		View title = getWindow().findViewById(android.R.id.title);
		View titleBar = (View) title.getParent();
		titleBar.setBackgroundColor(Color.rgb(102, 153, 204));
		
		//Массив ссылок на фотографии
		PhotoName=getIntent().getStringArrayExtra("Mashik.keyPhoto");
		photos=new ArrayList<ListData>();
		for (int i=0;i<PhotoName.length;i++){
			photos.add(new ListData(PhotoName[i]));
		}
		PhotoAdapter adapter=new PhotoAdapter(this,photos,metrics.widthPixels);
		gridview.setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.photos, menu);
		return false;
	}
}
