package ru.yandex.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity3 extends Activity {
    boolean liked;
    ExportAdapter adapter;
    DocumentFile parent=null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        ;
        setContentView(R.layout.activity_main3);
        Intent intent = getIntent();
        liked = intent.getBooleanExtra("liked", false);
        RecyclerView recyclerView = findViewById(R.id.rv2);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
       adapter = new ExportAdapter(getTracks(liked),this);
        recyclerView.setAdapter(adapter);
        Button button = findViewById(R.id.btn);
        button.setText("Export "+(liked?"Нравятся":"не нравятся")+"");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               openDirectory();
            }
        });


    }

    private List<Track> getTracks(boolean liked) {
        Log.d("MyLogGET", getFilesDir()+"/"+"track.json");
        List<Track> list = new ArrayList<>();
        File file ;
        if (!liked) {
            file = new File(getFilesDir()+"/Unliked/");
        }
        else {
            file = new File(getFilesDir()+"/");
        }
        File jsonFile = new File(getFilesDir()+"/"+"track.json");
        List<Track> tracks = new ArrayList<>();
        if (jsonFile.exists()) {
            String json = "";
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(jsonFile);
                byte[] buffer = new byte[fileInputStream.available()];
                fileInputStream.read(buffer);
                fileInputStream.close();
                json = new String(buffer);
                Log.d("MyLogJSON", json);
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    Track track = new Track(jsonObject.getString("id"));
                    Log.d("MyLogTRACK", track.getId()+"");
                    track.setLiked(jsonObject.getBoolean("liked"));
                    track.setDownloaded(jsonObject.getBoolean("downloaded"));
                    String name = jsonObject.getString("name");
                    track.setName(name);
                    Log.d("MyLogTRACK", name+"");
                    List<Artist> artist = new ArrayList<>();
                    JSONArray jsonArrayArtist = jsonObject.getJSONArray("artist");
                    for (int j = 0; j < jsonArrayArtist.length(); j++) {
                      String id = jsonArrayArtist.getString(j);
                       artist.add(new Artist(id,id));
                    }
                    track.setArtist(artist);
                    tracks.add(track);

                    }Log.d("MyLogTRACK", tracks.size()+"");
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("MyLog", e.getMessage());
            }

            }

       File[] files = file.listFiles();
        if (files != null) {
            for (File f: files) {

                String id = f.getName();
                if (id.endsWith(".mp3")) {
                    id = id.substring(0, id.lastIndexOf("."));



                    Track m = new Track(id);
                    for (Track track : tracks) {
                        if (track.getId().equals(id)) {
                            m = track;
                            list.add(m);
                        }
                    }
                }


            }
        }
        Log.d("MyLog", list.size()+"");

        return list;
    }

    public void openDirectory() {
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        //intent.putExtra("check","919");
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK) {
           // Toast.makeText(this, data.getStringExtra("check"), Toast.LENGTH_SHORT).show();
            Uri uri = data.getData();
            final int takeFlag=data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            parent = DocumentFile.fromTreeUri(this, uri);
            List<Track> list = adapter.getCheckedItems();
            List<File> files = adapter.getCheckedFiles(list,this);
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Export");
            int count = 0;
            for (File file: files) {
                Track track = list.get(count);
                count++;
                Log.d("MyLog", file.getName());
                DocumentFile newFile = parent.createFile("audio/mpeg", ""+track.getName()+"_"+track.getArtist().get(0).name + track.getId() + ".mp3");
                Log.d("MyLog", newFile.toString());
                moveMp3ToExternal(track.getId(),liked, newFile.getUri());


                alertDialog.setMessage("Exporting..."+track.getName());
                alertDialog.show();
                alertDialog.setCancelable(false);
            }
            alertDialog.dismiss();

        }
    }public void moveMp3ToExternal(String id,boolean liked, Uri targetUri) {
        File sourceFile ;
        if(liked){
            sourceFile = new File(getFilesDir() + "/" + id + ".mp3");
        }
        else {
            sourceFile = new File(getFilesDir() + "/Unliked/" + id + ".mp3");
        }
        if (sourceFile == null || !sourceFile.exists()) {
            Log.e("MyLogMP3", "Исходный файл не найден!");
            return;
        }

        // Используем try-with-resources для автоматического закрытия
        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = getContentResolver().openOutputStream(targetUri)) {

            if (out == null) {
                Log.e("MyLogMP3", "Не удалось открыть OutputStream для целевого файла");
                return;
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalWritten = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalWritten += bytesRead;
            }

            out.flush();
            // ВАЖНО: Принудительно синхронизируем данные с диском
            if (out instanceof FileOutputStream) {
                ((FileOutputStream) out).getFD().sync();
            }

            Log.d("MyLogMP3", "Копирование завершено. Записано байт: " + totalWritten);

        } catch (IOException e) {
            Log.e("MyLogMP3", "Ошибка при копировании: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
class ExportAdapter extends RecyclerView.Adapter<ExportAdapter.ViewHolder> {
    private final List<Track> tracks;
    List<Integer> checkedItems = new ArrayList<>();
    public ExportAdapter(List<Track> tracks,Activity activity) {
        this.tracks = tracks;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_export, parent, false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Track track = tracks.get(position);
        Log.d("MyLogNAME", track.getName()+"");
        holder.textView.setText(track.getName()+"");
        holder.textView2.setText(track.getArtist().get(0).name+"");
        CheckBox checkBox = holder.checkBox;
        checkBox.setChecked(checkedItems.contains(position));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkedItems.add(position);
            } else {
                checkedItems.remove((Integer) position);
            }
        });
    }

    @Override
    public int getItemCount() {
        Log.d("MyLogITEM", tracks.size()+"");
        return tracks.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        TextView textView2;
        CheckBox checkBox;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView3);
            textView2 = itemView.findViewById(R.id.textView4);
            checkBox = itemView.findViewById(R.id.checkBox);


        }
    }

public File getFile(Track track,Activity context){
        if (track == null) return null;
        File file ;
       if (track.liked){
           file = new File(context.getFilesDir()+"/"+track.getId()+"mp3");
       }else {
           file = new File(context.getFilesDir()+"/Unliked/"+track.getId()+"mp3");
       }
       return file;
}
public List<Track> getCheckedItems(){
    List<Track> list1 = new ArrayList<>();
    for (int i = 0; i < checkedItems.size(); i++) {
        list1.add(tracks.get(checkedItems.get(i)));
    }
    return list1;
}
public List<File> getCheckedFiles(List<Track> list,Activity context){
    List<File> list2 = new ArrayList<>();
    for (Track track : getCheckedItems()) {
        list2.add(getFile(track,context));
    }
    return list2;

}

}
