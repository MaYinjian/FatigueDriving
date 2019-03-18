package com.sample.tracking;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class Util {

    public final static int CAMERA_REQUEST_CODE = 0x111;

    public static void copyFilesFromAssets(Context context, String oldPath, String newPath) {
        try {
            String[] fileNames = context.getAssets().list(oldPath);
            if (fileNames.length > 0) {
                // directory
                File file = new File(newPath);
                if (!file.mkdir()) {
                    Log.d("mkdir", "can't make folder");

                }

                for (String fileName : fileNames) {
                    copyFilesFromAssets(context, oldPath + "/" + fileName,
                            newPath + "/" + fileName);
                }
            } else {
                // file
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    public static boolean checkPermission(Activity activity) {
        String permissions[] = {
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
        };
        ArrayList<String> toApplyList = new ArrayList<>();
        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(activity,
                    perm)) {
                toApplyList.add(perm);
                //进入到这里代表没有权限.
            }
        }
        if (!toApplyList.isEmpty()) {
            String tmpList[] = new String[toApplyList.size()];
            ActivityCompat.requestPermissions(activity, toApplyList.toArray(tmpList), CAMERA_REQUEST_CODE);
            return true;
        }
        return false;
    }


    private static MediaPlayer mMediaPlayer;
    private static boolean isPlaying;

    public static void playWarning(Context context) {
        if (isPlaying) {
            return;
        }
        isPlaying = true;
        if (mMediaPlayer == null) {
            synchronized (Util.class) {
                if (mMediaPlayer == null) {
                    mMediaPlayer = new MediaPlayer();
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                }
            }
        }
        try {
            AssetManager mAssetManager = context.getAssets();
            AssetFileDescriptor mAssetFileDescriptor = mAssetManager.openFd("warning.wav");
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(mAssetFileDescriptor.getFileDescriptor(),
                    mAssetFileDescriptor.getStartOffset(), mAssetFileDescriptor.getLength());
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer.start();
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    isPlaying = false;
                    return false;
                }
            });
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    isPlaying = false;
                }
            });
            mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            isPlaying = false;
        }

    }


}
