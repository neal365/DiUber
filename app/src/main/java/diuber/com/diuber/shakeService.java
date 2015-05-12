package diuber.com.diuber;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import cn.trinea.android.common.util.PackageUtils;

public class shakeService extends Service {
    private Vibrator vibrator;
    private static final String TAG = "diuber";
    private int now = 0;
    private int last = 0;
    private int shakeTimes = 0;
    private SensorManager sensorManager;
    private  String[] fileNameArray = {"com.duoduo.vip.taxi", "com.ubercab.driver", "com.sdu.didi.gsui", "com.yongche"};

    public shakeService() {

    }

    public void onCreate()
    {
        super.onCreate();
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
            Log.i(TAG, "x轴方向的重力加速度" + x + "；y轴方向的重力加速度" + y + "；z轴方向的重力加速度" + z);
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
                Log.i(TAG, "shakeTimes-now-last" + shakeTimes + "-" + now + "-" + last);
                last = now;
            }
            if(shakeTimes > 3){
                Log.i(TAG, "hahahahahahahahahahahahahahahahahahahaha");
                vibrator.vibrate(200);
                uninstallFiles();
                vibrator.vibrate(500);
                shakeTimes = 0;

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null) return START_STICKY;

        if (sensorManager != null) {// 注册监听器
            sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            // 第一个参数是Listener，第二个参数是所得传感器类型，第三个参数值获取传感器信息的频率
        }
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;

    }

    public class IOThread extends Thread{
        public IOThread(){
            start();
        }
        public void run(){

        }
    }

    private int uninstallFiles() {

        for (int j=0; j<fileNameArray.length; j++){
            if(isAppInstalled(this, fileNameArray[j]))
                PackageUtils.uninstall(this, fileNameArray[j]);

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

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void onDestroy() {
        if (sensorManager != null) {// 取消监听器
            sensorManager.unregisterListener(sensorEventListener);
        }
        Intent in = new Intent();
        in.setAction("YouWillNeverKillMe");
        sendBroadcast(in);
    }
}
