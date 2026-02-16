package ru.yandex.myapplication;

import static ru.yandex.myapplication.Utils.isLiked;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity implements OnBack {
RecyclerView recyclerView;
Boolean shared=true;
String trackIdMain="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Button select = findViewById(R.id.button10);
        select.setText("АВТОР");
        select.setOnClickListener(v -> {
            Log.d("TAG", "onCreate: "+shared+" "+trackIdMain);
            if (shared) {
                select.setText("ПОХОЖИЕ");
                shared=false;
                getSimilar(trackIdMain);
            }
            else {
                select.setText("АВТОР");
                shared=true;
                getOther(trackIdMain);
            }
            recyclerView.getAdapter().notifyDataSetChanged();
        });
        Button but = findViewById(R.id.button6);
        but.setOnClickListener(v -> {
Intent intent1 = new Intent(this, MainActivity2.class);
startActivity(intent1);
        });
        // Проверяем, что это нужный нам экшен и тип данных
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                   if (sharedText.contains("track/")) {
                       String trackIdS = sharedText.split("track/")[1];
                       Pattern pattern = Pattern.compile("^\\d+");
                       Matcher matcher = pattern.matcher(trackIdS);
                       if (matcher.find()) {
                           String trackId = matcher.group();
                                this.trackIdMain = trackId;
                                Log.d("TAG", "onCreate: "+sharedText);
                               Track track = new Track(trackId);
                               playMP3(track,findViewById(R.id.but));
                               if (shared ) {
                                   getSimilar(trackId);
                               }
                               else {
                                   getOther(trackId);
                               }


                       }

                   }

                }}}
    }private void getSimilar(String trackId) {
        recyclerView = findViewById(R.id.rv);
        SimilarAdapter similarAdapter = new SimilarAdapter(trackId,this,this);
        recyclerView.setAdapter(similarAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        new Thread(() -> {
            try {


                runOnUiThread(() -> {
                    recyclerView.setAdapter(new SimilarAdapter(trackId,MainActivity.this,this));
                    recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                });
            } catch (Exception e) {
                Log.d("TAG", "getSimilar: " + e.getMessage());
            }
        });
    }


    private void getOther(String trackId) {
        recyclerView = findViewById(R.id.rv);

        OtherAdapter otherAdapter = new OtherAdapter("",this,this);
        recyclerView.setAdapter(otherAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        new Thread(() -> {
            try {
                WEB web = new WEB();
           String other =     web.getArtist(trackId);
           runOnUiThread(() -> {
               recyclerView.setAdapter(new OtherAdapter(other,MainActivity.this,this));
               recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));



           });
            } catch (Exception e) {
              Log.d("TAG", "getOther: " + e.getMessage());
            }
        }).start();
    }






    private void updateUI(Button btn, String text) {
        runOnUiThread(() -> btn.setText(text));
    }
    @SuppressLint("SetTextI18n")
    public  void playMP3(Track track, Button playButton) {

        playButton.setText("Инициализация...");


                    new Thread(() -> {
                        try {
                            WEB web = new WEB();
                            File file =Utils.searchFile(track.getId(),this);
                            if(file!=null){
                                playButton.setText("Play: " + track.getName());
                                playButton.setEnabled(true);
                                File finalFile1 = file;
                                playButton.setOnClickListener(v -> Utils.playAudio(finalFile1,this));
                                track.setDownloaded(true);
                              Utils.  saveTrack(track,this);
                                return;
                            }
                            updateUI(playButton, "Получение инфо...");
                             web.getTrack(track);

                            updateUI(playButton, "Скачивание MP3...");
                             file =Utils.downloadTemp(track, this, "/Unliked/");

                            File finalFile = file;
                            runOnUiThread(() -> {
                                playButton.setText("Play: " + track.getName());
                                playButton.setEnabled(true);
                                playButton.setOnClickListener(v ->Utils. playAudio(finalFile,this));
                                Toast.makeText(this, "Готово к проигрыванию", Toast.LENGTH_SHORT).show();
                                track.setDownloaded(true);
                               Utils.saveTrack(track,this);
                            });

                        } catch (Exception e) {
                            Log.e("FATAL", "Детали ошибки: ", e);
                            String errorMessage = e.getMessage();

                            // Если ошибка пустая, выведем название класса исключения
                            if (errorMessage == null) errorMessage = e.toString();

                            final String finalMsg = errorMessage;
                            runOnUiThread(() -> {
                                playButton.setText("Ошибка: " + finalMsg);
                                // Выведем тост с полным текстом
                                Toast.makeText(MainActivity.this, "Ошибка: " + finalMsg, Toast.LENGTH_LONG).show();
                            });
                        }
                    }).start();
                }



    @Override
    public void onBack(Track track,Button button) {
playMP3(track,button);
    }
}

class WEB {
    private final OkHttpClient client = new OkHttpClient();
    private final String TOKEN = "y0__xCphuC2Bhje-AYgufvHthbeBInM8kGg5y_TuxAToeFe-G_-qg";

    public String getArtist(String trackId) throws Exception {
        Request req1 = new Request.Builder()

            .url("https://api.music.yandex.net:443/tracks/" + trackId )
            .addHeader("Authorization", "OAuth " + TOKEN)
            .build();
        try (Response res1 = client.newCall(req1).execute()) {
            if (!res1.isSuccessful()) throw new IOException("API Error " + res1.code());
            JSONObject root = new JSONObject(res1.body().string());

            JSONObject trackObj = root.getJSONArray("result").getJSONObject(0);
            JSONArray artists = trackObj.getJSONArray("artists");
            JSONObject artistObj = artists.getJSONObject(0);
            String artistId = artistObj.getString("id");
            Log.d("ARTIST", artistId);
         return artistId;

        }}
    public void getTrack(Track track) throws Exception {
        Request req = new Request.Builder()
            .url("https://api.music.yandex.net:443/tracks/" +track.getId() )
            .addHeader("Authorization", "OAuth " + TOKEN)
            .build();

        try (Response res = client.newCall(req).execute()) {
            if (!res.isSuccessful()) throw new IOException("API Error " + res.code());
            JSONObject root = new JSONObject(res.body().string());
            JSONObject trackObj = root.getJSONArray("result").getJSONObject(0);


            track.setName(trackObj.getString("title"));
            JSONArray artists = trackObj.getJSONArray("artists");
            for (int i = 0; i < artists.length(); i++) {
                JSONObject artist = artists.getJSONObject(i);
                track.getArtist().add(new Artist(artist.getString("id"), artist.getString("name")));
            }

            // Ссылка на XML с данными для скачивания
            Request dlReq = new Request.Builder()
                .url("https://api.music.yandex.net:443/tracks/" +track.getId() + "/download-info")
                .addHeader("Authorization", "OAuth " + TOKEN)
                .build();

            try (Response dlRes = client.newCall(dlReq).execute()) {
                JSONObject dlInfo = new JSONObject(dlRes.body().string()).getJSONArray("result").getJSONObject(0);
                track.setUrlLoad(dlInfo.getString("downloadInfoUrl"));
            }
        }
    }


}
class OtherAdapter extends RecyclerView.Adapter<OtherAdapter.ViewHolder> {
    private final List<Track> tracks;
    OnBack onBack;
    public OtherAdapter(String ArtistID,Activity context,OnBack onBack) {
        this.tracks = new ArrayList<>();
        loadData(ArtistID,context);
        this.onBack = onBack;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void run() {
                try {
                    for (Track track : tracks) {
                        if (Utils.searchFile(track.getId(), context)==null) {
                            track.downloaded = false;
                        }
                        else {track.downloaded = true;
                            track.liked = isLiked(track.getId(), context);


                        }
                     Utils.   saveTrack(track,context);
                    }

                  context.runOnUiThread(()->notifyDataSetChanged());
                } catch (Exception e) {
                    Log.e("ERROR", e.toString());
                }

            }


        }, 0, 5000);



        Log.i("Adapter", "OtherAdapter: " + tracks.size());
    }



    @SuppressLint("NotifyDataSetChanged")
    private void loadData(String artistID, Activity context) {
        new Thread(() -> {
            try {
                final String TOKEN = "y0__xCphuC2Bhje-AYgufvHthbeBInM8kGg5y_TuxAToeFe-G_-qg";
                Log.d("ARTIST", artistID);
                OkHttpClient client = new OkHttpClient();
                Request req = new Request.Builder()

                    .url("https://api.music.yandex.net:443/artists/" + artistID + "/tracks")
                    .addHeader("Authorization", "OAuth " + TOKEN)
                    .build();
                try (Response res = client.newCall(req).execute()) {
                    // if (!res.isSuccessful()) throw new IOException("API Error " + res.code());

                    JSONObject root1 = new JSONObject(res.body().string());
                    JSONObject result1 = root1.getJSONObject("result");

                    // Получаем массив tracks напрямую из result
                    JSONArray tracks1 = result1.getJSONArray("tracks");
                    Log.d("TRACKS", tracks1.length() + "");
                    tracks.clear();
                    for (int i = 0; i < tracks1.length(); i++) {
                        JSONObject trackObj1 = tracks1.getJSONObject(i);
                        Track track = new Track(trackObj1.getString("id"));
                        track.setName(trackObj1.getString("title"));
                        Log.d("TRACK", track.getName());

                        JSONArray artists1 = trackObj1.getJSONArray("artists");
                        for (int j = 0; j < artists1.length(); j++) {
                            JSONObject artObj = artists1.getJSONObject(j);
                            // Используем optString или String.valueOf, так как id в JSON может быть числом
                            track.getArtist().add(new Artist(String.valueOf(artObj.get("id")), artObj.getString("name")));
                        }
                        tracks.add(track);
                      Utils.  saveTrack(track,context);

                    }
                    context.runOnUiThread(this::notifyDataSetChanged);

                } catch (Exception e) {
                   Log.e("ERROR", e.toString());
                }
            } catch (Exception e) {
                Log.e("ERROR", e.toString());
            }
        }).start();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_track, parent, false);
        return new ViewHolder(view);

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Track track = tracks.get(position);
        holder.textView.setText(track.getName());
        holder.textView2.setText(track.getArtist().get(0).name);

        if (track.downloaded) {
            holder.button.setText("Скачан");
            holder.button.setTextColor(Color.GREEN);
        }
        else{ holder.button.setText("Скачать");holder.button.setTextColor(Color.WHITE);}

        if (track.liked) {
            holder.button2.setText("Нравится");
        }
        else holder.button2.setText("Не нравится");

        holder.button.setOnClickListener(v -> onBack.onBack(track,holder.button));
        holder.button2.setOnClickListener(v -> {
        if (track.isLiked()){
      Utils.  moveToUnliked(track.getId(),holder.button2.getContext());
        holder.button2.setText("wait");
        }
        else {
         Utils.   moveToLiked(track.getId(),holder.button2.getContext());
            holder.button2.setText("wait");
        }

        });
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
TextView textView;
TextView textView2;
Button button;
Button button2;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
            textView2 = itemView.findViewById(R.id.textView2);
            button = itemView.findViewById(R.id.button);
            button2 = itemView.findViewById(R.id.button2);



        }
    }




    }

class SimilarAdapter extends RecyclerView.Adapter<SimilarAdapter.ViewHolder>{
    private final List<Track> tracks;
    OnBack onBack;
    public SimilarAdapter(String ArtistID,Activity context,OnBack onBack) {
        this.tracks = new ArrayList<>();
        loadData(ArtistID,context);
        this.onBack = onBack;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void run() {
                try {
                    for (Track track : tracks) {
                        if (Utils.searchFile(track.getId(), context)==null) {
                            track.downloaded = false;
                        }
                        else {track.downloaded = true;
                            track.liked = isLiked(track.getId(), context);


                        }
                        Utils.   saveTrack(track,context);
                    }

                    context.runOnUiThread(()->notifyDataSetChanged());
                } catch (Exception e) {
                    Log.e("ERROR", e.toString());
                }

            }


        }, 0, 5000);



        Log.i("Adapter", "OtherAdapter: " + tracks.size());
    }



    @SuppressLint("NotifyDataSetChanged")
    private void loadData(String trackID, Activity context) {
        new Thread(() -> {
            try {
                final String TOKEN = "y0__xCphuC2Bhje-AYgufvHthbeBInM8kGg5y_TuxAToeFe-G_-qg";
                Log.d("ARTIST", trackID);
                OkHttpClient client = new OkHttpClient();
                Request req = new Request.Builder()

                    .url("https://api.music.yandex.net:443/tracks/" + trackID + "/similar")
                    .addHeader("Authorization", "OAuth " + TOKEN)
                    .build();
                try (Response res = client.newCall(req).execute()) {
                    // if (!res.isSuccessful()) throw new IOException("API Error " + res.code());

                    JSONObject root1 = new JSONObject(res.body().string());
                    Log.d("JSON", root1.toString());
                    JSONObject result1 = root1.getJSONObject("result");

                    // Получаем массив tracks напрямую из result
                    JSONArray tracks1 = result1.getJSONArray("similarTracks");
                    Log.d("TRACKS", tracks1.length() + "");
                    tracks.clear();
                    for (int i = 0; i < tracks1.length(); i++) {
                        JSONObject trackObj1 = tracks1.getJSONObject(i);
                        Track track = new Track(trackObj1.getString("id"));
                        track.setName(trackObj1.getString("title"));
                        Log.d("TRACK", track.getName());

                        JSONArray artists1 = trackObj1.getJSONArray("artists");
                        for (int j = 0; j < artists1.length(); j++) {
                            JSONObject artObj = artists1.getJSONObject(j);
                            // Используем optString или String.valueOf, так как id в JSON может быть числом
                            track.getArtist().add(new Artist(String.valueOf(artObj.get("id")), artObj.getString("name")));
                        }
                        tracks.add(track);
                        Utils.  saveTrack(track,context);

                    }
                    context.runOnUiThread(this::notifyDataSetChanged);

                } catch (Exception e) {
                    Log.e("ERROR", e.toString());
                }
            } catch (Exception e) {
                Log.e("ERROR", e.toString());
            }
        }).start();
    }

    @NonNull
    @Override
    public SimilarAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_track, parent, false);
        return new SimilarAdapter.ViewHolder(view);

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull SimilarAdapter.ViewHolder holder, int position) {
        Track track = tracks.get(position);
        holder.textView.setText(track.getName());
        holder.textView2.setText(track.getArtist().get(0).name);

        if (track.downloaded) {
            holder.button.setText("Скачан");
            holder.button.setTextColor(Color.GREEN);
        }
        else{ holder.button.setText("Скачать");holder.button.setTextColor(Color.WHITE);}

        if (track.liked) {
            holder.button2.setText("Нравится");
        }
        else holder.button2.setText("Не нравится");

        holder.button.setOnClickListener(v -> onBack.onBack(track,holder.button));
        holder.button2.setOnClickListener(v -> {
            if (track.isLiked()){
                Utils.  moveToUnliked(track.getId(),holder.button2.getContext());
                holder.button2.setText("wait");
            }
            else {
                Utils.   moveToLiked(track.getId(),holder.button2.getContext());
                holder.button2.setText("wait");
            }

        });
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        TextView textView2;
        Button button;
        Button button2;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
            textView2 = itemView.findViewById(R.id.textView2);
            button = itemView.findViewById(R.id.button);
            button2 = itemView.findViewById(R.id.button2);



        }
    }

}

interface OnBack{
    void onBack(Track track,Button button);
}
