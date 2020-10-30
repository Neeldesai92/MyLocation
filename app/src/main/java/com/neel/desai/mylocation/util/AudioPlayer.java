package com.neel.desai.mylocation.util;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

public class AudioPlayer {

    private MediaPlayer mMediaPlayer;

    public void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
    // mothod for raw folder (R.raw.fileName)
    public void play(Context context, int rid){
        stop();

        mMediaPlayer = MediaPlayer.create(context, rid);
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stop();
            }
        });

        mMediaPlayer.start();
    }

    // mothod for other folder
    public void play(Context context, String name) {
        stop();

        //mMediaPlayer = MediaPlayer.create(c, rid);
        mMediaPlayer = MediaPlayer.create(context, Uri.parse("android.resource://"+ context.getPackageName()+"/your_file/"+name+".mp3"));
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stop();
            }
        });

        mMediaPlayer.start();
    }

}