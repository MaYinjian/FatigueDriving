package com.sample.tracking;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;

import java.io.File;

public class FaceTrackerActivity extends Activity {

    void InitModelFiles() {

        String assetPath = "ZeuseesFaceTracking";
        String sdcardPath = Environment.getExternalStorageDirectory()
                + File.separator + assetPath;
        Util.copyFilesFromAssets(this, assetPath, sdcardPath);
        final FaceOverlapFragment fragment = (FaceOverlapFragment) getFragmentManager()
                .findFragmentById(R.id.overlapFragment);
        fragment.registTrackCallback(new FaceOverlapFragment.TrackCallBack() {

            @Override
            public void onTrackdetected(final int value, final float pitch, final float roll, final float yaw, final float eye_dist,
                                        final int id, final int eyeBlink, final int mouthAh, final int headYaw, final int headPitch, final int browJump) {

            }
        });

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multitracker);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode != Util.CAMERA_REQUEST_CODE) {
            return;
        }
        // 如果请求被拒绝，那么通常grantResults数组为空
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //申请成功，开始初始化模型
            InitModelFiles();
            return;
        }
        //申请失败，可以继续向用户解释。
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("权限允许");
        builder.setMessage("请先允许SD卡权限、拍照权限");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Util.checkPermission(FaceTrackerActivity.this);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
        return;
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isNeedRequest = Util.checkPermission(FaceTrackerActivity.this);
        if (!isNeedRequest) {
            InitModelFiles();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
