package com.yy.baidumap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.InfoWindow.OnInfoWindowClickListener;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNaviSettingManager;
import com.baidu.navisdk.adapter.BaiduNaviManager.NaviInitListener;

import com.baidu.navisdk.adapter.BNOuterTTSPlayerCallback;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.baidu.navisdk.adapter.BNRoutePlanNode.CoordinateType;
import com.yy.baidumap.bean.Info;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class MainActivity extends Activity {

    private MapView mMapView;
    private BaiduMap mBaiduMap;

    private Context context;

    //定位相关
    private LocationClient mLocationClient;
    private MyLocationListener mLocationlistener;//定位监听器
    private boolean isFirstIn = true;
    private double mLatitude;    //动态维度
    private double mLongitude;    //动态经度
    /*导航插入代码*/
    private LatLng mLastLocationData;  //存放定位地址
    private LatLng mDestLocationData;  //终点

    public static final String ROUTE_PLAN_NODE = "routePlanNode";
    private static final String APP_FOLDER_NAME = "BNSDKSimpleDemo-YY";
    private String mSDCardPath = null;

    String authinfo = null;//验证成功或失败


    public static List<Activity> activityList = new LinkedList<Activity>();


    //自定义图标
    private BitmapDescriptor mIconLocation;
    private MyOrientationListener myOrientationListener;
    private float mCurrentX; //记录图标的位置

    //模式
    private LocationMode mLocationMode;


    //覆盖物相关
    private BitmapDescriptor mMarker;
    private RelativeLayout mMarlerLy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //导航添加
        activityList.add(this);

        //为了使百度地图在手机中显示更好的效果，去除掉APP(action—bar)
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //在使用SDK个组件之前初始化context信息，传入ApplicationContext
        //注意该方法要在setContextView方法前调用
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        this.context = this;
        //初始化地图
        initView();

        //初始化定位
        initLocation();

        //添加覆盖物
        initMarker();

        //给地图添加点击
        mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {  //当点击marker时
                Bundle extraInfo = marker.getExtraInfo();
                Info info = (Info) extraInfo.getSerializable("info");
                ImageView iv = (ImageView) mMarlerLy.findViewById(R.id.id_info_img);
                TextView distance = (TextView) mMarlerLy.findViewById(R.id.id_info_distance);
                TextView name = (TextView) mMarlerLy.findViewById(R.id.id_info_name);
                TextView zan = (TextView) mMarlerLy.findViewById(R.id.id_info_zan);

                iv.setImageResource(info.getImgId());
                distance.setText(info.getDistance());
                name.setText(info.getName());
                zan.setText(info.getZan() + "");

                InfoWindow infoWindow;
                TextView tv = new TextView(context);
                tv.setBackgroundResource(R.mipmap.location_tips);
                tv.setPadding(30, 20, 30, 50);
                tv.setText(info.getName());
                tv.setTextColor(Color.parseColor("#ffffff"));

                BitmapDescriptor tips = BitmapDescriptorFactory.fromView(tv);//??

                //拿到经纬度
                final LatLng latLng = marker.getPosition();
                //从经纬度转化为屏幕上的点
                Point p = mBaiduMap.getProjection().toScreenLocation(latLng);
                p.y -= 47;
                LatLng ll = mBaiduMap.getProjection().fromScreenLocation(p);


                infoWindow = new InfoWindow(tips, ll, 10, new OnInfoWindowClickListener() {  //百度地图更新（参照源代码的构造方法）
                    @Override
                    public void onInfoWindowClick() {
                        mBaiduMap.hideInfoWindow();
                    }
                });
                mBaiduMap.showInfoWindow(infoWindow);

                //设置布局可见
                mMarlerLy.setVisibility(View.VISIBLE);
                return true;
            }
        });

        //添加地图的点击事件
        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {  //当地图点击时
                mMarlerLy.setVisibility(View.GONE);  //关闭覆盖布局
                mBaiduMap.hideInfoWindow();  //关闭InfoWindow
            }

            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                return false;
            }
        });
        //地图长点击事件
        mBaiduMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                Toast.makeText(context, "设置目的地成功", Toast.LENGTH_SHORT).show();
                mDestLocationData = latLng;
                addDestInfoOverlay(latLng);
            }
        });


        //导航添加
        //初始化导航相关代码
        if (initDirs()) {
            initNavi();
        }
    }

    private boolean initDirs() {
        mSDCardPath = getSdcardDir();
        if (mSDCardPath == null) {
            return false;
        }
        File f = new File(mSDCardPath, APP_FOLDER_NAME);
        if (!f.exists()) {
            try {
                f.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private void initNavi() {

        BNOuterTTSPlayerCallback ttsCallback = null;

        BaiduNaviManager.getInstance().init(this, mSDCardPath, APP_FOLDER_NAME, new NaviInitListener() {
            @Override
            public void onAuthResult(int status, String msg) {
                if (0 == status) {
                    authinfo = "key校验成功!";
                } else {
                    authinfo = "key校验失败, " + msg;
                }
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, authinfo, Toast.LENGTH_LONG).show();
                    }
                });
            }

            public void initSuccess() {
                Toast.makeText(MainActivity.this, "百度导航引擎初始化成功", Toast.LENGTH_SHORT).show();
                initSetting();
            }

            public void initStart() {
                Toast.makeText(MainActivity.this, "百度导航引擎初始化开始", Toast.LENGTH_SHORT).show();
            }

            public void initFailed() {
                Toast.makeText(MainActivity.this, "百度导航引擎初始化失败", Toast.LENGTH_SHORT).show();
            }


        }, null, ttsHandler, ttsPlayStateListener);

    }

    private String getSdcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

    private void initSetting() {
        // 设置是否双屏显示
        BNaviSettingManager.setShowTotalRoadConditionBar(BNaviSettingManager.PreViewRoadCondition.ROAD_CONDITION_BAR_SHOW_ON);
        // 设置导航播报模式
        BNaviSettingManager.setVoiceMode(BNaviSettingManager.VoiceMode.Veteran);
        // 是否开启路况
        BNaviSettingManager.setRealRoadCondition(BNaviSettingManager.RealRoadCondition.NAVI_ITS_ON);
    }

    /**
     * 内部TTS播报状态回传handler
     */
    private Handler ttsHandler = new Handler() {
        public void handleMessage(Message msg) {
            int type = msg.what;
            switch (type) {
                case BaiduNaviManager.TTSPlayMsgType.PLAY_START_MSG: {
                    showToastMsg("Handler : TTS play start");
                    break;
                }
                case BaiduNaviManager.TTSPlayMsgType.PLAY_END_MSG: {
                    showToastMsg("Handler : TTS play end");
                    break;
                }
                default:
                    break;
            }
        }
    };
    /**
     * 内部TTS播报状态回调接口
     */
    private BaiduNaviManager.TTSPlayStateListener ttsPlayStateListener = new BaiduNaviManager.TTSPlayStateListener() {

        @Override
        public void playEnd() {
            showToastMsg("TTSPlayStateListener : TTS play end");
        }

        @Override
        public void playStart() {
            showToastMsg("TTSPlayStateListener : TTS play start");
        }
    };

    public void showToastMsg(final String msg) {
        MainActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 导航
     *
     * @param mock
     */
    private void routeplanToNavi(Boolean mock) {
        CoordinateType coType = CoordinateType.BD09LL;

        BNRoutePlanNode sNode = null;
        BNRoutePlanNode eNode = null;



        //mLastLocationDate
        //mDestLocationDate
        sNode = new BNRoutePlanNode(mLastLocationData.longitude, mDestLocationData.latitude, "我的地点", null, coType);
        eNode = new BNRoutePlanNode(mLastLocationData.longitude, mDestLocationData.latitude, "目标地点", null, coType);

        if (sNode != null && eNode != null) {
            List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
            list.add(sNode);
            list.add(eNode);

            //根据教程修改launchNavigator（）第4个参数值true为false
            BaiduNaviManager.getInstance().launchNavigator(this, list, 1, mock, new DemoRoutePlanListener(sNode));
        }
    }

    public class DemoRoutePlanListener implements BaiduNaviManager.RoutePlanListener {

        private BNRoutePlanNode mBNRoutePlanNode = null;

        public DemoRoutePlanListener(BNRoutePlanNode node) {
            mBNRoutePlanNode = node;
        }

        @Override
        public void onJumpToNavigator() {
            /*
             * 设置途径点以及resetEndNode会回调该接口
			 */

            for (Activity ac : activityList) {

                if (ac.getClass().getName().endsWith("BNDemoGuideActivity")) {

                    return;
                }
            }
            Intent intent = new Intent(MainActivity.this, BNDemoGuideActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(ROUTE_PLAN_NODE, (BNRoutePlanNode) mBNRoutePlanNode);
            intent.putExtras(bundle);
            startActivity(intent);

        }

        @Override
        public void onRoutePlanFailed() {
            // TODO Auto-generated method stub
            /*
            导航时算路失败的原因有以下几种：
            （1）地理位置获取失败
            （2）传入的经纬度有误（例如经纬度弄反，经纬度标注的点在国外）
            （3）定位服务未开启
            （4）传入的节点距离太近
            （5）节点输入有误（例如设置了某个节点为空）
            （6）上次算路取消了，需要等一会才能进行下一次算路
            * */
            Toast.makeText(MainActivity.this, "算路失败", Toast.LENGTH_SHORT).show();
        }
    }


    private void addDestInfoOverlay(LatLng destInfo) {
        //设置终点图标
        mBaiduMap.clear();
        OverlayOptions options = new MarkerOptions().position(destInfo)//
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon_en))//
                .zIndex(5);
        mBaiduMap.addOverlay(options);

    }

    private void initMarker() {
        mMarker = BitmapDescriptorFactory.fromResource(R.mipmap.maker);
        mMarlerLy = (RelativeLayout) findViewById(R.id.id_marker_ly);
    }

    private void initView() {
        mMapView = (MapView) findViewById(R.id.id_bmapView);
        mBaiduMap = mMapView.getMap();
        //设置地图的放大比例
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(18.0f);
        mBaiduMap.setMapStatus(msu);
    }

    private void initLocation() {
        mLocationMode = LocationMode.NORMAL;  //地图默认的模式是普通模式

        mLocationClient = new LocationClient(this);
        mLocationlistener = new MyLocationListener();
        mLocationClient.registerLocationListener(mLocationlistener);//注册

        //给LocationClient设置一些必要的配置
        LocationClientOption option = new LocationClientOption();
        option.setCoorType("bd09ll");//设置坐标类型（精确定位），此属性默认为gcj02
        option.setIsNeedAddress(true);//当前位置
        option.setOpenGps(true);//打开GPS
        option.setScanSpan(1000);//请求时间为1000毫秒
        mLocationClient.setLocOption(option);//设置(重要)
        //初始化图标(1.通过一个文件,2.通过一个路径，3.通过一个View)
        mIconLocation = BitmapDescriptorFactory.fromResource(R.mipmap.arrow);

        myOrientationListener = new MyOrientationListener(context);

        //方向传感器的回调
        myOrientationListener.setOnOrientationListener(new MyOrientationListener.OnOrientationListener() {

            @Override
            public void onOrientationChanged(float x) {
                //更新地图上图标的位置
                mCurrentX = x;
            }
        });
    }


    //上下文菜单


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.id_map_common:  //普通地图
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                break;
            case R.id.id_map_site:  //卫星地图
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                break;
            case R.id.id_map_traffic:  //实时交通
                if (mBaiduMap.isTrafficEnabled()) {  //判断实时交通是否显示
                    mBaiduMap.setTrafficEnabled(false);//关闭实时交通
                    item.setTitle("实时交通(off)");
                } else {
                    mBaiduMap.setTrafficEnabled(true);//打开实时交通
                    item.setTitle("实时交通(on)");
                }
                break;
            case R.id.id_map_location://返回当前位置
                centerToMyLocation(mLatitude, mLongitude);
                break;
            case R.id.id_map_mode_common://普通模式
                mLocationMode = LocationMode.NORMAL;
                break;
            case R.id.id_map_mode_following://跟随模式
                mLocationMode = LocationMode.FOLLOWING;
                break;
            case R.id.id_map_mode_compass://罗盘模式
                mLocationMode = LocationMode.COMPASS;
                break;
            case R.id.id_add_overlay:
                addOverlays(Info.infos);
                break;
            case R.id.id_btn_mocknav:   //模拟导航
                mDestLocationDataState(false);
                break;
            case R.id.id_btn_realnav:   //开始导航
                mDestLocationDataState(true);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void mDestLocationDataState(Boolean state) {
        if (mDestLocationData == null) {
            Toast.makeText(MainActivity.this, "长按地图设置目标地点", Toast.LENGTH_SHORT).show();
        }else{
            routeplanToNavi(state);
        }

    }

    /**
     * 添加覆盖物
     *
     * @param infos
     */
    private void addOverlays(List<Info> infos) {
        mBaiduMap.clear();  //清除地图图层
        LatLng latLng = null;
        Marker marker = null;
        OverlayOptions options;
        for (Info info : infos) {
            //得到经纬度
            latLng = new LatLng(info.getLatitude(), info.getLongtitude());
            //得到图标
            options = new MarkerOptions().position(latLng).icon(mMarker).zIndex(5);
            //实例化marker
            marker = (Marker) mBaiduMap.addOverlay(options);

            Bundle bundle = new Bundle();
            bundle.putSerializable("info", info);
            marker.setExtraInfo(bundle);
        }
        //每次添加完图层的时候，把移动到第一个图层位置(移动位置)
        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
        mBaiduMap.setMapStatus(msu);
    }

    /**
     * 定位到我的位置
     *
     * @param mLatitude  维度
     * @param mLongitude 经度
     */
    private void centerToMyLocation(double mLatitude, double mLongitude) {
        //设置变量经度和纬度
        LatLng latlng = new LatLng(mLatitude, mLongitude);
        //两个参数值（经度和维度）
        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latlng);
        mBaiduMap.animateMapStatus(msu);  //移动地图位置
    }

    @Override
    protected void onStart() {
        super.onStart();
        //开启定位
        mBaiduMap.setMyLocationEnabled(true);   //开启定位的允许
        if (!mLocationClient.isStarted()) {
            mLocationClient.start();  //启动定位
        }
        //开启方向传感器
        myOrientationListener.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //停止定位
        mBaiduMap.setMyLocationEnabled(false);
        mLocationClient.stop();
        //停止方向传感器
        myOrientationListener.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestrory时执行mMapView.onDestrory(),实现地图生命周期的管理
        //保护性能
        mMapView.onDestroy();


        //导航

        MainActivity.activityList.remove(this);
    }

    private class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {//定位成功以后的回调
            //当接收到Location的时候，需要给location进行转换
            MyLocationData data = new MyLocationData.Builder()//
                    .direction(mCurrentX)//方向的设置
                    .accuracy(location.getRadius())//
                    .latitude(location.getLatitude())//
                    .longitude(location.getLongitude())//
                    .build();
            mBaiduMap.setMyLocationData(data);//转化坐标数据给mBaiduMap
            //设置自定义图标
            MyLocationConfiguration config =
                    new MyLocationConfiguration(mLocationMode, true, mIconLocation);
            mBaiduMap.setMyLocationConfigeration(config);

            //当第一次定位成功以后,将地图中心点设置为用户当前位置
            //更新经纬度
            mLatitude = location.getLatitude();
            mLongitude = location.getLongitude();

            //导航功能添加记录坐标
            LatLng ll = new LatLng(mLatitude, mLongitude);
            mLastLocationData = ll;
            if (isFirstIn) {
                //设置变量纬度和经度
                centerToMyLocation(location.getLatitude(), location.getLongitude());
                isFirstIn = false;
                Toast.makeText(context, location.getAddrStr(), Toast.LENGTH_SHORT).show();
                System.out.println("错误码：" + location.getLocType());
            }
        }
    }
}
