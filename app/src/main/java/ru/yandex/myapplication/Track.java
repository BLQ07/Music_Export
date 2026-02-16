package ru.yandex.myapplication;

import android.provider.MediaStore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Track {
    String id;
    String name;
   List<Artist> artist=new ArrayList<>();
    Album album;
    boolean downloaded=false;

    public Long getDate() {

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        try {if (date == null) {
            return null;
        }
            Date date1 = sdf.parse(date);

            if (date1 == null) {
                return null;
            }
            return date1.getTime();
        } catch (Exception e) {

        }

        return null;
    }
    public void setDate(String date) {
        date = date;
    }

    String date="";

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    boolean liked=false;

    public String getUrlLoad() {
        return urlLoad;
    }

    public void setUrlLoad(String urlLoad) {
        this.urlLoad = urlLoad;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Artist> getArtist() {
        return artist;
    }

    public void setArtist(List<Artist> artist) {
        this.artist = artist;
    }

    String urlLoad;
    public Track(String id){
        this.id = id;
    }
}
