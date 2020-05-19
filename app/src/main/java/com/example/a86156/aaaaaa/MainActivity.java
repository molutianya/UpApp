package com.example.a86156.aaaaaa;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.model.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import okhttp3.Request;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Context context;
    ProgressDialog dialog;
    Button down_v;
    String apkAdd;
    String newVer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        context = MainActivity.this;
        initView();
        Myprogress();

    }

    /**
     * 初始化所有控件
     */
    private void initView() {
        down_v = findViewById(R.id.download);
        down_v.setOnClickListener(this);
    }

    /**
     * 定义下载进度框
     */
    private void Myprogress() {
        dialog = new ProgressDialog(this);
        dialog.setIcon(R.drawable.ic_launcher_background);
        dialog.setMessage("下载中...");
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);


    }

    /**
     * android6.0之后要动态获取权限
     */
    private void checkPermission() {
        final int REQUEST_EXTERNAL_STORAGE = 1;
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(MainActivity.this,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * 获取当前app的版本信息
     *
     * @param context
     * @return
     */
    private String getVersinName(Context context) {
        String vername = "";
        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            int code = info.versionCode;  //获取版本号
            vername = info.versionName; //获取版本名
            Log.d("版本信息", vername + "   " + code);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return vername;
    }

    /**
     * 获取服务器的apk下载地址
     */
    private void getLoadAppinfo(){
        OkGo.<String>get("服务器地址")
                .tag(this)
                .cacheMode(CacheMode.NO_CACHE)
                .params("Value","4003")
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            JSONObject object = new JSONObject(response.body());
                            JSONArray jr = new JSONArray(object.getString("table"));
                            for (int i = 0; i <jr.length() ; i++) {
                                JSONObject o = new JSONObject(jr.get(i).toString());
                                apkAdd = o.getString("address");
                                newVer = o.getString("ver");
                            }
                            //用当前的app版本与服务器上面的版本进行比较
                            String oldVer = getVersinName(context);
                            if(!oldVer.equals(newVer)){
                                //发现新版本，开始下载
                                downLoadApk(apkAdd);
                            }else {
                                //无需下载
                                Toast.makeText(context,"当前已是最新版本，无需下载",Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.download:
                checkPermission();
                getLoadAppinfo();
                break;
        }
    }


    /**
     * 下载apk
     */
    private void downLoadApk(String path) {
        //下载到的文件夹路径
        final String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/download";
        //下载后的新名称
        final String fileName = "OTG-H30.apk";
        File file = new File(filePath + "/" + fileName);
        if (file.exists()) {
            file.delete();
        }
        OkGo.<File>get(path)
                .execute(new FileCallback(filePath, fileName) {
                    @Override
                    public void onStart(com.lzy.okgo.request.base.Request<File, ? extends com.lzy.okgo.request.base.Request> request) {
                        super.onStart(request);

                    }

                    @Override
                    public void onSuccess(Response<File> response) {

                    }

                    @Override
                    public void onError(Response<File> response) {
                        super.onError(response);
                        dialog.show();
                        dialog.setMessage("下载出错...");
                    }

                    @Override
                    public void downloadProgress(Progress progress) {
                        super.downloadProgress(progress);
                        int res = (int) (progress.fraction * 100);
                        dialog.show();
                        dialog.setMessage("下载中...");
                        dialog.setProgress(res);
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                        //下载完成后跳转安装
                        dialog.dismiss();
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        File file = new File(filePath + "/" + fileName);
                        Uri uri;
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            uri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".MyFileProvider", file);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } else {
                            uri = Uri.fromFile(file);
                        }
                        intent.setDataAndType(uri, "application/vnd.android.package-archive");
                        startActivity(intent);

                    }
                });
    }

}











