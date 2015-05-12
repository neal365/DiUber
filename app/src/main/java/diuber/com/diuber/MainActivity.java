package diuber.com.diuber;

import cn.trinea.android.common.util.PackageUtils;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;
import com.umeng.message.PushAgent;
import com.umeng.message.UmengRegistrar;
import com.umeng.update.UmengUpdateAgent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {
    private Vibrator vibrator;
    private SensorManager sensorManager;
    private int now = 0;
    private int last = 0;
    private int shakeTimes = 0;
    private boolean isRoot = false;

    private Button btn_backup, btn_install, btn_uninstall, btn_start, btn_stop;
    private TextView textView;
    private SeekBar seekBar;

    private  String[] fileNameArray = {"com.duoduo.vip.taxi", "com.ubercab.driver", "com.sdu.didi.gsui", "com.yongche"};

    private Notification baseNF;
    private int Notification_ID_BASE = 110;
    private PendingIntent pd;
    private NotificationManager nm;


    private final static int kSystemRootStateUnknow = -1;
    private final static int kSystemRootStateDisable = 0;
    private final static int kSystemRootStateEnable = 1;
    private static int systemRootState = kSystemRootStateUnknow;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UmengUpdateAgent.setUpdateOnlyWifi(false);
        UmengUpdateAgent.update(this);
        PushAgent mPushAgent = PushAgent.getInstance(this);
        mPushAgent.enable();
        PushAgent.getInstance(this).onAppStart();
        String device_token = UmengRegistrar.getRegistrationId(this);
        Log.d("diuber", "device_token:"+device_token );
        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.textView);


        if (!isRootSystem()) {
            isRoot = false;
            Toast.makeText(this, "还没有ROOT，以一般模式运行!", Toast.LENGTH_SHORT).show();
            textView.setText("root后可无干扰卸载和恢复");
//            new AlertDialog.Builder(this)
//                    .setTitle("手机没有ROOT ⊙︿⊙")
//                    .setMessage("是否花点时间ROOT下，90%的机型能5分钟轻松搞定！")
//                    .setPositiveButton("了解ROOT", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            Uri uri = Uri.parse("http://root.360.cn/");
//                            Intent it = new Intent(Intent.ACTION_VIEW, uri);
//                            startActivity(it);
//                        }
//                    })
//                    .setNegativeButton("先退出", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            finish();
//                        }
//                    })
//                    .show();
        } else {
            isRoot = true;
            Toast.makeText(this, "已经ROOT，以高级模式运行!", Toast.LENGTH_SHORT).show();
        }



        //进来就做一次备份
        if(isRoot) {
            Handler handler = new IOHandler();
            new IOThread(0, handler);
        }

        btn_backup = (Button)findViewById(R.id.btn_backup);
        btn_backup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Handler handler = new IOHandler();
                new IOThread(0, handler);
            }
        });

        btn_install = (Button)findViewById(R.id.btn_install);
        btn_install.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Handler handler = new IOHandler();
                new IOThread(1, handler);
            }
        });

        btn_uninstall = (Button)findViewById(R.id.btn_uninstall);
        btn_uninstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(isRoot){
                    SharedPreferences settings = getSharedPreferences("diuber.com.diuber", 0);
                    boolean rootConfirmed = settings.getBoolean("rootConfirmed", false);

                    if(rootConfirmed){
                        Handler handler = new IOHandler();
                        new IOThread(2, handler);
                    }else {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("一键隐藏测试")
                                .setMessage("【重要提示】\n如果弹出对话框，请务必在弹出的对话框中选择“允许/授权”\n并“不再提示”！")
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Handler handler = new IOHandler();
                                        new IOThread(2, handler);
                                    }
                                })
                                .show();
                        settings.edit().putBoolean("rootConfirmed", true).commit();
                    }
                }else {
                    Handler handler = new IOHandler();
                    new IOThread(2, handler);
                }
            }
        });

        btn_start = (Button)findViewById(R.id.btn_start);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //新建状态栏通知
                baseNF = new Notification();
                //设置通知在状态栏显示的图标
                baseNF.icon = R.drawable.diuber;
                //通知时在状态栏显示的内容
                baseNF.tickerText = "开始咯，紧急时刻点亮屏幕摇一摇";

                //通知的默认参数 DEFAULT_SOUND, DEFAULT_VIBRATE, DEFAULT_LIGHTS.
                //如果要全部采用默认值, 用 DEFAULT_ALL.
                //此处采用默认声音
                baseNF.defaults |= Notification.DEFAULT_SOUND;
                baseNF.defaults |= Notification.DEFAULT_VIBRATE;
                baseNF.defaults |= Notification.DEFAULT_LIGHTS;
                //让声音、振动无限循环，直到用户响应
                //baseNF.flags |= Notification.FLAG_INSISTENT;
                //通知被点击后，自动消失
                baseNF.flags |= Notification.FLAG_AUTO_CANCEL;
                //点击'Clear'时，不清楚该通知(QQ的通知无法清除，就是用的这个)
                baseNF.flags |= Notification.FLAG_NO_CLEAR;
                //第二个参数 ：下拉状态栏时显示的消息标题 expanded message title
                //第三个参数：下拉状态栏时显示的消息内容 expanded message text
                //第四个参数：点击该通知时执行页面跳转
                //发出状态栏通知
                //The first parameter is the unique ID for the Notification
                // and the second is the Notification object.
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                pd = PendingIntent.getActivity(MainActivity.this, 0, intent, 0);
                baseNF.setLatestEventInfo(MainActivity.this, "   后台运行中", "  点击回到设置界面", pd);

                if (isServiceRunning(MainActivity.this, "shakeService")) {
                    Log.d("diuber", "停止服务");
                    Intent serviceIntent = new Intent();
                    serviceIntent.setClass(MainActivity.this, shakeService.class);
                    MainActivity.this.stopService(serviceIntent);
                    btn_start.setText("开始服务");
                } else {
                    Log.d("diuber", "开始服务");
                    Intent serviceIntent = new Intent();
                    serviceIntent.setClass(MainActivity.this, shakeService.class);
                    MainActivity.this.startService(serviceIntent);
                    nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    nm.notify(Notification_ID_BASE, baseNF);
                    finish();
                }

            }
        });

        seekBar=(SeekBar)findViewById(R.id.seekBar);
        SharedPreferences settings = getSharedPreferences("diuber.com.diuber", 0);
        int progress = settings.getInt("seekNum", 18);
        seekBar.setProgress(progress);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            /**
             * 拖动条停止拖动的时候调用
             */
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
            /**
             * 拖动条开始拖动的时候调用
             */
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            /**
             * 拖动条进度改变的时候调用
             */
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                SharedPreferences settings = getSharedPreferences("diuber.com.diuber", 0);
                settings.edit().putInt("seekNum", progress).commit();

            }
        });

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {// 注册监听器
            sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            // 第一个参数是Listener，第二个参数是所得传感器类型，第三个参数值获取传感器信息的频率
        }
    }

    /**
     * 重力感应监听
     */
    private SensorEventListener sensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {

            // 传感器信息改变时执行该方法
            float[] values = event.values;
            float x = values[0]; // x轴方向的重力加速度，向右为正
            float y = values[1]; // y轴方向的重力加速度，向前为正
            float z = values[2]; // z轴方向的重力加速度，向上为正
            // 一般在这三个方向的重力加速度达到40就达到了摇晃手机的状态。
            int medumValue = 11;// 三星 i9250怎么晃都不会超过20，没办法，只设置19了
            SharedPreferences settings = getSharedPreferences("diuber.com.diuber", 0);
            int progress = settings.getInt("seekNum", 30);
            medumValue = 12 + progress/4;
            Log.d("diuber", "medumValue-" + medumValue);
            if (Math.abs(x) > medumValue || Math.abs(y) > medumValue || Math.abs(z) > medumValue) {
                now = (int) (System.currentTimeMillis()/1000);
                if (now - last < 6){
                    shakeTimes += 1;
                }
                else {
                    shakeTimes = 0;
                }
                last = now;
            }
            if (shakeTimes > 3){
                vibrator.vibrate(200);
                shakeTimes = 0;

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    public static boolean isRootSystem() {
        if (systemRootState == kSystemRootStateEnable) {
            return true;
        } else if (systemRootState == kSystemRootStateDisable) {
            return false;
        }
        File f = null;
        final String kSuSearchPaths[] = { "/system/bin/", "/system/xbin/",
                "/system/sbin/", "/sbin/", "/vendor/bin/" };
        try {
            for (int i = 0; i < kSuSearchPaths.length; i++) {
                f = new File(kSuSearchPaths[i] + "su");
                if (f != null && f.exists()) {
                    systemRootState = kSystemRootStateEnable;
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        systemRootState = kSystemRootStateDisable;
        return false;
    }

    public static boolean isServiceRunning(Context mContext,String className) {

        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager)
                mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList
                = activityManager.getRunningServices(100);

        if (!(serviceList.size()>0)) {
            return false;
        }

        for (int i=0; i<serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().contains(className) == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }

    class IOHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    if(msg.arg1 == 0) {
                        Toast.makeText(MainActivity.this,"软件已备份！！！",Toast.LENGTH_LONG).show();
                    }else if(msg.arg1 == 2){
                        Toast.makeText(MainActivity.this, "尼玛，软件还没安装呀!", Toast.LENGTH_LONG).show();
                    }else {
                        Toast.makeText(MainActivity.this,"软件备份失败！！！",Toast.LENGTH_LONG).show();
                    }
                    break;
                case 1:
                    if(msg.arg1 == 0) {
                        Toast.makeText(MainActivity.this,"软件已恢复！！！",Toast.LENGTH_LONG).show();
                    }else {
                        Toast.makeText(MainActivity.this,"软件恢复失败！！！",Toast.LENGTH_LONG).show();
                    }
                    break;
                case 2:
                    if(msg.arg1 == 0) {
                        Toast.makeText(MainActivity.this,"软件隐藏成功！！！",Toast.LENGTH_LONG).show();
                    }else {
                        Toast.makeText(MainActivity.this,"软件隐藏失败！！！",Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        }
    }

    /**
     *  备份文件
     */
    public class IOThread extends Thread{
        private Handler mHandler;
        private int operation;
        public IOThread(int operation, Handler mHandler){
            this.mHandler = mHandler;
            this.operation = operation;
            start();
        }
        public void run(){
            Message msg = new Message();
            switch (operation){
                case 0://backup
                    msg.what = 0;
                    if(0 == backupFiles()){
                        msg.arg1 = 0;
                    }else if(2 == backupFiles()){
                        msg.arg1 = 2;
                    }else {
                        msg.arg1 = 1;
                    }
                    break;
                case 1://install
                    msg.what = 1;
                    if(0 == installFiles()){
                        msg.arg1 = 0;
                    }else{
                        msg.arg1 = 1;
                    }
                    break;
                case 2://uninstall
                    msg.what = 2;
                    if(0 == uninstallFiles()){
                        msg.arg1 = 0;
                    }else{
                        msg.arg1 = 1;
                    }
                    break;
            }
            mHandler.sendMessage(msg);
        }
    }

    private int backupFiles() {
        String targetDir = "/mnt/sdcard/diuber";
        (new File(targetDir)).mkdirs();

        boolean appExist = false;
        for (int i=0; i<fileNameArray.length; i++) {
            if (isAppInstalled(MainActivity.this, fileNameArray[i])) {
                appExist = true;
            }
        }
        if(!appExist){
            return 2;   //没有软件
        }
        int appNum = 0;
        for (int j=0; j<fileNameArray.length; j++){
            try {
                Log.d("diuber", fileNameArray[j]);
                String sourceFilePath = MainActivity.this.getPackageManager().getApplicationInfo(fileNameArray[j], 0).sourceDir;
                Log.d("diuber", sourceFilePath);
                File sourceFile=new File(sourceFilePath);
                File targetFile=new File(new File(targetDir).getAbsolutePath()
                        +File.separator+fileNameArray[j] + ".apk");
                if(!targetFile.exists() || targetFile.length() != sourceFile.length()){
                    copyFile(sourceFile,targetFile);
                }
                appNum += 1;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        if (appNum == 0){
            return 1;   //备份失败
        }
        return 0;
    }

    private int installFiles() {
        String targetDir = "/mnt/sdcard/diuber";
        File[] file = (new File(targetDir)).listFiles();
        for (int i = 0; i < file.length; i++) {
            if (file[i].isFile()) {
                // 源文件
                File sourceFile = file[i];
                String pkgName = sourceFile.getName().replace(".apk", "");
                if(!isAppInstalled(MainActivity.this, pkgName)) {
                    PackageUtils.install(MainActivity.this, targetDir + "/" + sourceFile.getName());
                }
            }
        }
        return 0;
    }

    public boolean isAppInstalled(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        List<String> pName = new ArrayList<String>();
        if (pinfo != null) {
            for (int i = 0; i < pinfo.size(); i++) {
                String pn = pinfo.get(i).packageName;
                pName.add(pn);
            }
        }
        return pName.contains(packageName);
    }

    private int uninstallFiles() {
        for (int j = 0; j < fileNameArray.length; j++) {
            if(isAppInstalled(MainActivity.this, fileNameArray[j])) {
                PackageUtils.uninstall(MainActivity.this, fileNameArray[j]);
            }
        }

        return 0;
    }

    // 复制文件
    public static void copyFile(File sourceFile,File targetFile)
            throws IOException{
        // 新建文件输入流并对它进行缓冲
        FileInputStream input = new FileInputStream(sourceFile);
        BufferedInputStream inBuff=new BufferedInputStream(input);

        // 新建文件输出流并对它进行缓冲
        FileOutputStream output = new FileOutputStream(targetFile);
        BufferedOutputStream outBuff=new BufferedOutputStream(output);

        // 缓冲数组
        byte[] b = new byte[1024 * 5];
        int len;
        while ((len =inBuff.read(b)) != -1) {
            outBuff.write(b, 0, len);
        }
        // 刷新此缓冲的输出流
        outBuff.flush();

        //关闭流
        inBuff.close();
        outBuff.close();
        output.close();
        input.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        if (isServiceRunning(MainActivity.this, "shakeService")) {
            btn_start.setText("停止服务");
        } else {
            btn_start.setText("开始服务");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
        if (sensorManager != null) {// 取消监听器
            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            Uri uri = Uri.parse("http://app.diuber.com");
            Intent it = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(it);
            return true;
        }
        if (id == R.id.action_download) {
            Uri uri = Uri.parse("http://app.diuber.com/app_sj.html");
            Intent it = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(it);
            return true;
        }
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_root) {
            Uri uri = Uri.parse("http://root.360.cn");
            Intent it = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(it);
            return true;
        }
        if (id == R.id.action_exit) {
            Intent serviceIntent = new Intent();
            serviceIntent.setClass(MainActivity.this, shakeService.class);
            MainActivity.this.stopService(serviceIntent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
