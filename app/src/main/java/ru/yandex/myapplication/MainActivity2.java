package ru.yandex.myapplication;

import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;
import static ru.yandex.myapplication.Utils.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity2 extends AppCompatActivity implements OnBack {
Boolean liked=true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Utils.getToken(this)==null) {
            Toast.makeText(this, "Token не найден", Toast.LENGTH_SHORT).show();
           checkAuth();
        }
        setContentView(R.layout.activity_main2);
        Button export =findViewById(R.id.button5);
        export.setText("Export");
        export.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity3.class);
            intent.putExtra("liked", liked);

            startActivity(intent);
        });
        export.setOnLongClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("TOKEN");
            TextView textView =new TextView(this);
            textView.setTextIsSelectable(true);
            textView.setText(Utils.getToken(this));
            builder.setView(textView);
            builder.show();
            return true;
        });

        RecyclerView rv = findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        LikedAdapter adapter = new LikedAdapter(getTracks(liked),this,MainActivity2.this,        liked);
        rv.setAdapter(adapter);
        Button button3 = findViewById(R.id.button3);
        button3.setText("Нравится");
        button3.setOnClickListener(v -> {
        if (liked){
            liked = false;
            button3.setText("Не нравится");
        }
        else {
            liked = true;
            button3.setText("Нравится");
        }
        LikedAdapter adapter1 = new LikedAdapter(getTracks(liked),this,MainActivity2.this,liked);
        rv.setAdapter(adapter1);
        adapter1.notifyDataSetChanged();
        });

    }public Map<String, String> getTracks(Boolean liked) {
        Map<String, String> n = new HashMap<>();
        File[] files;

        if (liked){
            files = getFilesDir().listFiles();
            Log.d("TAG", "getTracksLiked: " + getFilesDir().listFiles().length);
        }
        else {
            File f = new File(getFilesDir() + "/Unliked/");
            files = f.listFiles();
            Log.d("TAG", "getTracks: " + f.listFiles().length);
        }
        for (File file : files) {
           String id = file.getName().split(".mp3")[0];
           Log.d("TAG", "getTracks: " + id + " " + file.getName());
            Date date = new Date(file.lastModified());

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

            String dateString = simpleDateFormat.format(date);
           if(!id.equals("Unliked") && !id.equals("liked")&&!id.equals("profileInstalled")&&!id.equals("track.json")){
           n.put(id, dateString);}
        }
        return n;
    }
    private final ActivityResultLauncher<Intent> loginLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                   recreate();

                } else {

                    finish();
                }
            }
    );

    private void checkAuth() {

            Intent intent = new Intent(this, MainActivity4.class);
            loginLauncher.launch(intent);


    }

    @Override
    public void onBack(Track track, Button button) {
        playMP3(track,button);
    } private void updateUI(Button btn, String text) {
        runOnUiThread(() -> btn.setText(text));
    }

    public  void playMP3(Track track,Button playButton) {

        playButton.setText("Инициализация...");


        new Thread(() -> {
            try {
                WEB web = new WEB();
                File file =searchFile(track.getId(),this);
                if(file!=null){
                    playButton.setText("Play: " + track.getName());
                    playButton.setEnabled(true);
                    File finalFile1 = file;
                    playButton.setOnClickListener(v -> playAudio(finalFile1,this));
                    track.setDownloaded(true);
                    saveTrack(track,this);
                    return;
                }
                updateUI(playButton, "Получение инфо...");
                web.getTrack(track,Utils.getToken(this));

                updateUI(playButton, "Скачивание MP3...");
                file = Utils.downloadTemp(track, this, "/Unliked/");

                File finalFile = file;
                runOnUiThread(() -> {
                    playButton.setText("Play: " + track.getName());
                    playButton.setEnabled(true);
                    playButton.setOnClickListener(v -> playAudio(finalFile,this));
                    Toast.makeText(this, "Готово к проигрыванию", Toast.LENGTH_SHORT).show();
                    track.setDownloaded(true);
                    saveTrack(track,this);
                });

            } catch (Exception e) {
                Log.e("FATAL", "Детали ошибки: ", e);
                String errorMessage = e.getMessage();

                if (errorMessage == null) errorMessage = e.toString();

                final String finalMsg = errorMessage;
                runOnUiThread(() -> {
                    playButton.setText("Ошибка: " + finalMsg);

                    Toast.makeText(this, "Ошибка: " + finalMsg, Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

}
class LikedAdapter extends RecyclerView.Adapter<LikedAdapter.ViewHolder> {
    OnBack onBack;

    public List<Track> getN() {
        return n;
    }

    List<Track> n = new ArrayList<>();
    Activity activity;
    public LikedAdapter(Map<String,String> tracks,Activity activity,OnBack onBack,boolean liked) {
        this.onBack = onBack;

        this.activity = activity;
        Timer timer = new Timer();loadData(tracks,liked);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
                    for (Track track : n) {
                        if (searchFile(track.getId(), activity)==null) {
                            track.downloaded = false;
                        }
                        else {track.downloaded = true;
                            if (isLiked(track.getId(),activity)) track.liked = true;
                            else track.liked = false;


                        }
                        if (track.liked!=liked) {
                          n.remove(track);

                        }
                        saveTrack(track,activity);
                    }
                    n.sort(Comparator.comparingLong(Track::getDate).reversed());

                    activity.runOnUiThread(()->notifyDataSetChanged());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        },0,5000);
    }
    private void loadData(Map<String,String> tracks,boolean liked) {
         final String TOKEN = Utils.getToken(activity);

       new Thread(() -> {
           int count = 0;
           for (String trackId : tracks.keySet()) {
               String date = tracks.get(trackId);
               Track track = new Track(trackId);
               track.date = date;
              Log.e("TRACKLoad",trackId);
               OkHttpClient client = new OkHttpClient();
               Request req = new Request.Builder()
                   .url("https://api.music.yandex.net:443/tracks/" +track.getId() )
                   .addHeader("Authorization", "OAuth " + TOKEN)
                   .build();

               try (Response res = client.newCall(req).execute()) {

                   JSONObject root = new JSONObject(res.body().string());
                   JSONObject trackObj = root.getJSONArray("result").getJSONObject(0);


                   track.setName(trackObj.getString("title"));

                   JSONArray artists = trackObj.getJSONArray("artists");
                   for (int i = 0; i < artists.length(); i++) {
                       JSONObject artist = artists.getJSONObject(i);
                       track.getArtist().add(new Artist(artist.getString("id"), artist.getString("name")));
                   }
                   track.liked = liked;
                   n.add(track);
                   saveTrack(track,activity);

               }catch (Exception e){
                   e.printStackTrace();
                   Log.e("ERROR",e.getMessage());
               }

       }}).start();

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_track, parent, false);
        return new LikedAdapter.ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Track track = n.get(position);
        holder.name.setText(track.getName() + "\n " + track.date);
        holder.artist.setText(track.getArtist().get(0).name);
        Button button = holder.button;
        button.setText("Play");
        button.setOnClickListener(v -> {
            onBack.onBack(track, button);
        });  if (track.liked) {
            holder.button2.setText("Нравится");
        } else holder.button2.setText("Не нравится");


        Button button2 = holder.button2;
        button2.setOnClickListener(v -> {
            if (track.isLiked()) {
                moveToUnliked(track.getId(), holder.button2.getContext());
                holder.button2.setText("wait");
            } else {
                moveToLiked(track.getId(), holder.button2.getContext());
                holder.button2.setText("wait");
            }

        });
    }
    @Override
    public int getItemCount() {
        if (n == null) return 0;
        return n.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView artist;
        Button button;
        Button button2;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textView);
            artist = itemView.findViewById(R.id.textView2);
            button = itemView.findViewById(R.id.button);
            button2 = itemView.findViewById(R.id.button2);

        }
    }
    }

