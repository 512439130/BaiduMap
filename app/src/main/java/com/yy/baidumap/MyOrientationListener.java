package com.yy.baidumap;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by 13160677911 on 2016-11-17.
 */

public class MyOrientationListener implements SensorEventListener {
    private SensorManager mSensorManager;//传感器的管理者
    private Context mContext;
    private Sensor mSensor;

    private float lastX;

    public MyOrientationListener(Context mContext){
        this.mContext = mContext;
    }
    public void start(){  //开始监听
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        if(mSensorManager != null){
            //获得方向传感器
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        }
        if(mSensor != null){
            mSensorManager.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void stop(){   //结束监听
        //停止监听
        mSensorManager.unregisterListener(this);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {  //经度

    }

    @Override
    public void onSensorChanged(SensorEvent event) {  //方向发生变化
        //event携带传感器类型（三个数据X,,Y,Z）
        //事件返回的类型是否为方向传感器
        if(event.sensor.getType() == Sensor.TYPE_ORIENTATION){
            float x = event.values[SensorManager.DATA_X];
            if(Math.abs( x - lastX) > 1.0){  //1.0:1度
                if(mOnOrientationListener != null){ //如果主界面注册不等于null，则进行一个回调
                    mOnOrientationListener.onOrientationChanged(x);
                }
            }
            lastX  = x;
        }

    }

    private OnOrientationListener mOnOrientationListener;

    public void setOnOrientationListener(OnOrientationListener mOnOrientationListener) {
        this.mOnOrientationListener = mOnOrientationListener;
    }

    public interface OnOrientationListener{
        void onOrientationChanged(float x);
    }


}
