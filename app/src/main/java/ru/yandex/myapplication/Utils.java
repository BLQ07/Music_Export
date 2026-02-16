package ru.yandex.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Utils {
    static final String SALT = "XGRB01828"; // Секретный ключ для MD5

    public static void moveToLiked(String id,Context c) {
        File file = new File(c.getFilesDir()+"/"+id+".mp3");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File file1 = new File(c.getFilesDir()+"/Unliked/"+id+".mp3");
        if(file1.exists()) {file1.renameTo(file);}
        else {
            Toast.makeText(c, "не загружено", Toast.LENGTH_SHORT).show();
        }
        File file2 = new File(c.getFilesDir()+"/Unliked/"+id+".mp3");
        if (file2.exists()) {
            file2.delete();
        }
    }
    public static void moveToUnliked(String id,Context context) {
        File file = new File(context.getFilesDir()+"/Unliked/"+id+".mp3");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File file1 = new File(context.getFilesDir()+"/"+id+".mp3");
        if(file1.exists()){ file1.renameTo(file);


        }
        else {
            Toast.makeText(context, "не загружено", Toast.LENGTH_SHORT).show();
        }
        File file2 = new File(context.getFilesDir()+"/"+id+".mp3");
        if (file2.exists()) {
            file2.delete();
        }

    }
    public static boolean isLiked(String id,Context context) {
        File file = new File(context.getFilesDir()+"/"+id+".mp3");
        return file.exists();
    }
    public static File searchFile(String id, Context context) {
        File file = new File(context.getFilesDir()+"/"+id+".mp3");


        if (file.exists()) {

            return file;
        }
        File file1 = new File(context.getFilesDir()+"/Unliked/"+id+".mp3");
        // Log.e("FILE", file1.getParentFile().listFiles().toString());
        if (file1.exists()) {

            return file1;
        }

        return null;
    }
    public static   void save(JSONArray jsonArray, Activity context) {
        try {
            File file =   new File(context.getFilesDir()+"/"+"track.json");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(jsonArray.toString().getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public static void saveTrack(Track track,Activity context) {
        File file = new File(context.getFilesDir()+"/"+"track.json");
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String json = "";
        try {
            InputStream inputStream = new FileInputStream(file);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer);
            JSONArray jsonArray ;
            if (json.isEmpty()) jsonArray = new JSONArray();
            else jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString("id").equals(track.getId())) {
                    //update
                    jsonObject.put("liked", track.isLiked());
                    jsonObject.put("downloaded", track.isDownloaded());
                    jsonObject.put("name", track.getName());
                    List<Artist> artist = track.getArtist();
                    if (artist != null) {
                        JSONArray jsonArrayArtist = new JSONArray();
                        for (int j = 0; j < artist.size(); j++) {
                            jsonArrayArtist.put(artist.get(j).name);
                        }
                        jsonObject.put("artist", jsonArrayArtist);
                    }

                    jsonArray.put(i, jsonObject);
                    save(jsonArray,context);

                    return;
                }
            }
            JSONObject jsonObject = getJsonObject(track);
            jsonArray.put(jsonObject);
            Utils. save(jsonArray,context);
        }catch (Exception e){

        }

    }

    @NonNull
    private static JSONObject getJsonObject(Track track) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", track.getId());
        jsonObject.put("liked", track.isLiked());
        jsonObject.put("downloaded", track.isDownloaded());
        jsonObject.put("name", track.getName());
        List<Artist> artist = track.getArtist();
        if (artist != null) {
            JSONArray jsonArrayArtist = new JSONArray();
            for (int j = 0; j < artist.size(); j++) {
                jsonArrayArtist.put(artist.get(j).name);
            }
            jsonObject.put("artist", jsonArrayArtist);
        }
        return jsonObject;
    }

    public static File downloadTemp(Track track, Activity context, String path1) throws Exception {
        OkHttpClient client = new OkHttpClient();
        File file = new File(context.getFilesDir()+"/"+path1+"/", track.getId() + ".mp3");
        if(file.getParentFile().mkdirs()){}
        if (file.exists()) {
            return file;
        }
        else {file.createNewFile();}

        // 1. Получаем XML с параметрами сервера
        Request xmlReq = new Request.Builder().url(track.getUrlLoad()).build();
        String host, path, ts, s;
        try (Response xmlRes = client.newCall(xmlReq).execute()) {
            String xml = xmlRes.body().string();
            host = getXmlTag(xml, "host");
            path = getXmlTag(xml, "path");
            ts = getXmlTag(xml, "ts");
            s = getXmlTag(xml, "s");
        }

        // 2. Генерируем подпись MD5 (алгоритм Яндекса)
        String sign = md5(SALT + path.substring(1) + s);

        // 3. Формируем финальный URL
        String finalUrl = "https://" + host + "/get-mp3/" + sign + "/" + ts + path;
        Log.i("WEB", "Final URL: " + finalUrl);

        // 4. Скачиваем сам файл
        Request fileReq = new Request.Builder().url(finalUrl).build();
        try (Response fileRes = client.newCall(fileReq).execute()) {
            if (!fileRes.isSuccessful()) throw new IOException("Download Error " + fileRes.code());

            try (InputStream is = fileRes.body().byteStream();
                 FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
            }
        }
        Log.i("WEB", "File saved, size: " + file.length());
        track.setDownloaded(true);

        saveTrack(track,context);
        return file;
    }



    public static String getXmlTag(String xml, String tag) {
        String startTag = "<" + tag + ">";
        String endTag = "</" + tag + ">";
        int start = xml.indexOf(startTag) + startTag.length();
        int end = xml.indexOf(endTag);
        return xml.substring(start, end);
    }

    public static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }
    public static final void playAudio(File file, Activity context) {
        try { MediaPlayer player = new MediaPlayer();
            FileInputStream fis = new FileInputStream(file);
            player.setDataSource(fis.getFD(), 0, file.length());
            fis.close();
            player.prepare();
            player.start();
            AlertDialog alert = new AlertDialog.Builder(context).create();
            alert.setTitle("Воспроизведение");
            alert.setMessage("Воспроизведение " + file.getName());
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            Button back = new Button(context);
            back.setText("Назад");
            layout.addView(back);
            back.setOnClickListener(v -> {
                if (player.isPlaying()) {
                    player.pause();
                    //getCurrentPosition() - позиция в мс
                    //getDuration() - длительность в мс
                    //seekTo() - переместить позицию в мс
                    //seekTo(long msec) - переместить позицию в мс
                    if (player.getCurrentPosition() > 10000&&player.getCurrentPosition()+10000 < player.getDuration()) {
                        player.seekTo(player.getCurrentPosition() - 10000);
                        player.start();
                    }
                    else {

                    }
                    player.seekTo(0);

                }

            });
            Button button = new Button(context);
            button.setText("Пауза");
            layout.addView(button);
            alert.setView(layout);
            button.setOnClickListener(v -> {

                if (player.isPlaying()) {
                    player.pause();
                    button.setText("play");
                }
                else {
                    player.start();
                    button.setText("pause");
                }
            });
            Button next = new Button(context);
            next.setText("Вперед");
            layout.addView(next);
            next.setOnClickListener(v -> {
                if (player.isPlaying()) {

                    if (player.getCurrentPosition() + 10000 < player.getDuration()){player.pause();
                        player.seekTo(player.getCurrentPosition() + 10000);
                        player.start();}
                }
            });
            alert.show();
            alert.setOnDismissListener(dialog -> player.release());

        } catch (IOException e) {
            Log.e("PLAYER", "Ошибка плеера", e);
        }
    }


}
