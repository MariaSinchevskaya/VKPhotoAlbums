package ru.mashik.photoalbum;

public class ListData {
	String title="";
	String imageURL="";
	String describe="";
	public ListData(String title, String imageURL, String describe) {
		// TODO Auto-generated constructor stub
		this.title=title;
		this.imageURL=imageURL;
		this.describe=describe;
	}
	public ListData(String imageURL){
		this.imageURL=imageURL;
	}

}
