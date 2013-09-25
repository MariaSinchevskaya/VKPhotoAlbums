package ru.mashik.photoalbum;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.json.JSONException;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.perm.kate.api.Album;
import com.perm.kate.api.Api;
import com.perm.kate.api.KException;
import com.perm.kate.api.Photo;

public class Main extends ListActivity {
	
	private final int REQUEST_LOGIN=1;
	private final String keyPhoto="Mashik.keyPhoto";
	
	ProgressDialog dialog;
	
	Account account = new Account();
	Api api;
	ArrayList<Album> album;
	String[] AlbumNames;
	String[] AlbumTitle;
	String AlbumDescribe[];
	long[]AlbumID;
	ArrayList <Photo> AlbumPhotos;
	public ArrayList<ListData> Images;

	ArrayList<Photo> photo;
	String[] photoName;
	DisplayMetrics metrics = new DisplayMetrics();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Для передачи параметров экрана для отрисовки адаптера
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		View title = getWindow().findViewById(android.R.id.title);
		View titleBar = (View) title.getParent();
		//для предотвращения ошибок загрузки из сети
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 
		
		titleBar.setBackgroundColor(Color.rgb(102, 153, 204));
		account.restore(this);
		if (account.access_token!=null){
			api=new Api(account.access_token,Constants.API_ID);
			Task task=new Task();
			try {
				task.execute();
			} catch (NullPointerException e) {
				// TODO Auto-generated catch block
				Toast.makeText(this, "Альбомы не найдены", Toast.LENGTH_SHORT).show();
			}
		}
		else {
			Intent intent = new Intent();
			intent.setClass(this, LoginActivity.class);
			startActivityForResult(intent, REQUEST_LOGIN);
		}
	}
	
	
	private class Task extends AsyncTask<Void,Void,Void>{
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
		    dialog = new ProgressDialog(Main.this);
		    dialog.setMessage("Идет загрузка альбомов. Подождите, пожалуйста...");
		    dialog.setIndeterminate(true);
		    dialog.setCancelable(true);
		    dialog.show();
		}

		@Override
		protected Void doInBackground(Void... arg0) throws NullPointerException {
			// TODO Auto-generated method stub

			try {
				album=api.getAlbums(account.user_id, null, null, 1, null);
				if (album==null) {
					return null;
				}
				//Название альбома
				AlbumNames=new String[album.size()];
				//Заглавная картинка
				AlbumTitle=new String[album.size()];
				//Описание альбома
				AlbumDescribe=new String[album.size()];
				//ID альбома
				AlbumID=new long[album.size()];
				//Адаптер для отображения строки
				Images=new ArrayList<ListData>();
				//Заполнение всех массивов элеметнами
				for (int i=0;i<album.size();i++){
					if (album.get(i).title!=null)AlbumNames[i]=album.get(i).title;
					else AlbumNames[i]=" ";
					ArrayList <Photo> AlbumPhotos=api.getPhotos(account.user_id, album.get(i).aid, null, 0);
					if (AlbumPhotos.size()!=0)AlbumTitle[i]=album.get(i).thumb_src;
					else AlbumTitle[i]=" ";
					if (album.get(i).description!=null) AlbumDescribe[i]=album.get(i).description;
					else AlbumDescribe[i]=" ";
					AlbumID[i]=album.get(i).aid;
					Images.add(new ListData(AlbumNames[i],AlbumTitle[i],AlbumDescribe[i]));
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
			} catch (IOException e) {
				// TODO Auto-generated catch block
			} catch (JSONException e) {
				// TODO Auto-generated catch block
			} catch (KException e) {
				// TODO Auto-generated catch block
			}
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if (album==null) {
				Toast.makeText(Main.this, "Альбомы не найдены", Toast.LENGTH_LONG).show();
				return;
			}
			else{
				
			setListAdapter(new AlbumAdapter(Main.this,Images,metrics.heightPixels));
			
			dialog.dismiss();
		}
			OnItemClickListener itemListener = new OnItemClickListener(){



				//@Override
				public void onItemClick(AdapterView<?> parent, View v, int position,
						long id) {
					// TODO Auto-generated method stub
					try {
						photo=api.getPhotos(account.user_id, AlbumID[position], null, null);
						photoName=new String[photo.size()];
						for (int i=0;i<photo.size();i++){
							photoName[i]=photo.get(i).src;
						}
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
					} catch (IOException e) {
						// TODO Auto-generated catch block
					} catch (JSONException e) {
						// TODO Auto-generated catch block
					} catch (KException e) {
						// TODO Auto-generated catch block
					}
					Intent intent=new Intent(Main.this,Photos.class);
					intent.putExtra(keyPhoto, photoName);
					//Intent intent=new Intent(Albums.this,Image.class);
					startActivity(intent);
					
				}};
				getListView().setOnItemClickListener(itemListener);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		
		switch (requestCode){
		case REQUEST_LOGIN:
			if (resultCode==RESULT_OK){
				account.access_token=data.getStringExtra("token");
				account.user_id=data.getLongExtra("user_id", 0);
				account.save(this);
				api=new Api(account.access_token,Constants.API_ID);
				Task task=new Task();
				task.execute();
			}
			break;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return false;
	}

}
