package com.Liani.baidumap;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";   //日志的TAG
    private Button requestButton = null;    //重新定位按钮
    private TextView locationText = null;   //显示位置信息的TextView
    private MapView mapView = null;         //显示地图的控件
    private BaiduMap baiduMap = null;       //地图管理器
    private Marker marker = null;          //覆盖物
    //初始化bitmap信息，不用的时候请及时回收recycle   //覆盖物图标
    private BitmapDescriptor bd = BitmapDescriptorFactory.fromResource(R.drawable.icon_markb);
    //定位服务的客户端。宿主程序在客户端声明此类，并调用，目前只支持在主线程中启动
    private LocationClient locationClient = null;
    //是否是首次定位,实际上没有用到，因为我设置了不定时请求位置信息
    boolean isFirstLoc = true;
    public BDLocationListener myListener = new BDLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            Log.d(TAG, "BDLocationListener -> onReceiveLocation");
            String addr; //定位结果
            if (bdLocation == null || mapView == null) {
                Log.d(TAG, "BDLocation or mapView is null");
                locationText.setText("定位失败...");
                return;
            }
            if (!bdLocation.getLocationDescribe().isEmpty()) {
                addr = bdLocation.getLocationDescribe();
            } else if (bdLocation.hasAddr()) {
                addr = bdLocation.getAddrStr();
            } else {
                Log.d(TAG, "BDLocation has no addr info");
                addr = "定位失败...";
                return;
            }

            StringBuilder sBuilder = new StringBuilder();
            sBuilder.append("\nTime : " + bdLocation.getTime());            //服务器返回的当前定位时间
            sBuilder.append("\nError code : " + bdLocation.getLocType());   //定位结果码
            sBuilder.append("\nLatitude : " + bdLocation.getLatitude());    //获取纬度坐标
            sBuilder.append("\nLongtitude : " + bdLocation.getLongitude()); //获取经度坐标
            sBuilder.append("\nRadius : " + bdLocation.getRadius());        //位置圆心

            //根据定位结果码判断是何种定位以及定位请求是否成功

            if (bdLocation.getLocType() == BDLocation.TypeGpsLocation) {
                //GPS定位结果
                sBuilder.append("\nSpeed : " + bdLocation.getSpeed());//当前运动的速度
                sBuilder.append("\nSatellite number : " + bdLocation.getSatelliteNumber());//定位卫星数量
                sBuilder.append("\nHeight : " + bdLocation.getAltitude());  //位置高度
                sBuilder.append("\nDirection : " + bdLocation.getDirection());  //定位方向
                sBuilder.append("\nAddrStr : " + bdLocation.getAddrStr());  //位置详细信息
                sBuilder.append("\nStreet : " + bdLocation.getStreetNumber() + " " + bdLocation.getStreet());//街道号、路名
                sBuilder.append("\nDescribtion : GPS 定位成功");
            } else if (bdLocation.getLocType() == BDLocation.TypeNetWorkLocation) {
                //网络定位结果

                sBuilder.append("\nAddrStr : " + bdLocation.getAddrStr()); //位置详细信息
                sBuilder.append("\nStreet : " + bdLocation.getStreetNumber() + " " + bdLocation.getStreet());//街道号、路名
                sBuilder.append("\nOperators : " + bdLocation.getOperators());//运营商编号
                sBuilder.append("\nDescribtion : 网络定位成功");
            } else if (bdLocation.getLocType() == BDLocation.TypeOffLineLocation) {
                //离线定位结果
                sBuilder.append("\nAddrStr : " + bdLocation.getAddrStr()); //位置详细信息
                sBuilder.append("\nStreet : " + bdLocation.getStreetNumber() + " " + bdLocation.getStreet());//街道号、路名
                sBuilder.append("\nDescribtion : 离线定位成功");
            } else if (bdLocation.getLocType() == BDLocation.TypeServerError) {
                sBuilder.append("\nDescribtion : 服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
            } else if (bdLocation.getLocType() == BDLocation.TypeNetWorkException) {
                sBuilder.append("\nDescribtion : 网络故障，请检查网络连接是否正常");
            } else if (bdLocation.getLocType() == BDLocation.TypeCriteriaException) {
                sBuilder.append("\nDescribtion : 无法定位结果，一般由于定位SDK内部检测到没有有效的定位依据，" + "" +
                        "比如在飞行模式下就会返回该定位类型， 一般关闭飞行模式或者打开wifi就可以再次定位成功");
            }
            //位置语义化描述
            sBuilder.append("\nLocation decribe : " + bdLocation.getLocationDescribe());
            //poi信息（就是附近的一些建筑信息）,只有设置可获取poi才有值

            List<Poi> poiList = bdLocation.getPoiList();
            if (poiList != null) {
                sBuilder.append
                        ("\nPoilist size : " + poiList.size());
                for (Poi p : poiList) {
                    sBuilder.append("\nPoi : " + p.getId() + " " + p.getName() + " " + p.getRank());
                }
            }
            //打印以上信息
            Log.d(TAG, "定位结果详细信息 : " + sBuilder.toString());

            MyLocationData myLocationData = new MyLocationData.Builder().accuracy(bdLocation.getRadius()) //设置定位数据的精度信息，单位米
                    .direction(100) //设定定位数据的方向信息？？啥意思？？
                    .latitude(bdLocation.getLatitude()) //设定定位数据的纬度
                    .longitude(bdLocation.getLongitude())//设定定位数据的经度
                    .build(); //构建生生定位数据对象

            //设置定位数据, 只有先允许定位图层后设置数据才会生效,setMyLocationEnabled(boolean)
            baiduMap.setMyLocationData(myLocationData);

            if (isFirstLoc) {
                //Log.d(TAG,"首次定位开始");
                // isFirstLoc = false;
                // 地理坐标基本数据结构：经度和纬度
                LatLng latLng = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude()); //为当前定位到的位置设置覆盖物Marker
                resetOverlay(latLng); //描述地图状态将要发生的变化         //生成地图状态将要发生的变化,newLatLngZoom设置地图新中心点
                MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngZoom(latLng, 18.0f);
                //MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngZoom(latLng);
                // 以动画方式更新地图状态，动画耗时 300 ms  (聚焦到当前位置)
                baiduMap.animateMapStatus(mapStatusUpdate);
            }
            locationText.setText(addr);
        }

        @Override
        public void onConnectHotSpotMessage(String s, int i) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        //获取地图控件
        mapView = (MapView) findViewById(R.id.mapview);
        //重新定位按钮
        requestButton = (Button) findViewById(R.id.request_location);
//定位结果显示
        locationText = (TextView) findViewById(R.id.title);
        locationText.setText("正在定位...");
        //获取地图控制器
        baiduMap = mapView.getMap();
//允许定位图层,如果不设置这个参数，那么baiduMap.setMyLocationData(myLocationData);定位不起作用
        baiduMap.setMyLocationEnabled(true);
        //设置mark覆盖物拖拽监听器
        baiduMap.setOnMarkerDragListener(new MyMarkerDragListener());
        //设置mark覆盖物点击监听器
        baiduMap.setOnMarkerClickListener(new MyMarkerClickListener());
        //生成定位服务的客户端对象，此处需要注意：LocationClient类必须在主线程中声明
        locationClient = new LocationClient(getApplicationContext());
        //注册定位监听函数，当开始定位.start()或者请求定位.requestLocation()时会触发
        locationClient.registerLocationListener(myListener);
        //设定定位SDK的定位方式
        setLocationOption();
        //开始、请求定位
        // locationClient.start();
        requestLocation();
        //重新定位按钮设置监听器

        requestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //开始定位，或者重新定位
                requestLocation();
            }
        });
    }

//开始定位或者请求定位

    private void requestLocation() {
        //如果请求定位客户端已经开启，就直接请求定位，否则开始定位
        // 很重要的一点，是要在AndroidManifest文件中注册定位服务，否则locationClient.isStarted一直会是false,
        // 而且可能出现一种情况是首次能定位，之后再定位无效
        if (locationClient != null && locationClient.isStarted()) {
            Log.d(TAG, "requestLocation.");
            locationText.setText("正在定位...");
            //请求定位，异步返回，结果在locationListener中获取.
            locationClient.requestLocation();
        } else if (locationClient != null && !locationClient.isStarted()) {
            Log.d(TAG, "locationClient is started : " + locationClient.isStarted());
            locationText.setText("正在定位...");
            //定位没有开启 则开启定位，结果在locationListener中获取.
            locationClient.start();
        } else {
            Log.e(TAG, "request location error!!!");
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        Log.d(TAG, "onDestroy");
        //注销定位监听器
        locationClient.unRegisterLocationListener(myListener); //停止定位
        locationClient.stop(); //不允许图层定位\
        baiduMap.setMyLocationEnabled(false); //清除覆盖物
        clearOverlay(); //回收Bitmap资源
        bd.recycle(); //在activity执行onDestroy时执行MapView.onDestroy()，实现地图生命周期管理
        mapView.onDestroy();
        mapView = null;


    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mapView.onPause();
    }


    private void setLocationOption() { //获取配置参数对象，用于配置定位SDK各配置参数，比如定位模式、定位时间间隔、坐标系类型等
        LocationClientOption option = new LocationClientOption();
        //可选，默认false,设置是否使用gps
        option.setOpenGps(true);
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        option.setIsNeedAddress(true);
        option.setIsNeedLocationDescribe(true);
        option.setIsNeedLocationPoiList(true);
        option.setNeedDeviceDirect(true);
        option.setIgnoreKillProcess(false);
        option.SetIgnoreCacheException(false);
        option.setEnableSimulateGps(false);
        locationClient.setLocOption(option);


    }

    //初始化添加覆盖物mark

    private void initOverlay(LatLng latLng) {
        Log.d(TAG, "Start initOverlay");
//设置覆盖物添加的方式与效果
        MarkerOptions markerOptions = new MarkerOptions() .position(latLng)//mark出现的位置
            .icon(bd) //mark图标
           .draggable(true)//mark可拖拽
         .animateType(MarkerOptions.MarkerAnimateType.drop)//从天而降的方式 //
        .animateType(MarkerOptions.MarkerAnimateType.grow) ;
        marker = (Marker) (baiduMap.addOverlay(markerOptions));//地图上添加mark
        setPopupTipsInfo(marker);
        Log.d(TAG,"End initOverlay");
    }
    //清除覆盖物
    private void clearOverlay(){
        baiduMap.clear();
        marker = null; }

    //重置覆盖物
    private void resetOverlay(LatLng latLng)
    { clearOverlay();
        initOverlay(latLng); }
//覆盖物拖拽监听器
public class MyMarkerDragListener implements BaiduMap.OnMarkerDragListener
{
    @Override
public void onMarkerDrag(Marker marker) {

}
    //拖拽结束，调用方法，弹出View(气泡，意即在地图中显示一个信息窗口)，显示当前mark位置信息
    @Override public void onMarkerDragEnd(Marker marker) {
        setPopupTipsInfo(marker);
    }
    @Override public void onMarkerDragStart(Marker marker) {

    }
}
    //覆盖物点击监听器
    public class MyMarkerClickListener implements BaiduMap.OnMarkerClickListener {
        @Override
        public boolean onMarkerClick(Marker marker) {
            //调用方法,弹出View(气泡，意即在地图中显示一个信息窗口)，显示当前mark位置信息
            setPopupTipsInfo(marker);
            return false;
        }
        }
    //想根据Mark中的经纬度信息，获取当前的位置语义化结果，需要使用地理编码查询和地理反编码请求
    // 在地图中显示一个信息窗口
    private void setPopupTipsInfo(Marker marker){
        //获取当前经纬度信息
        final LatLng latLng = marker.getPosition();
        final String[] addr = new String[1];
        //实例化一个地理编码查询对象
        GeoCoder geoCoder = GeoCoder.newInstance();
        //设置反地理编码位置坐标
        ReverseGeoCodeOption option = new ReverseGeoCodeOption(); option.location(latLng);
        //发起反地理编码请求

        geoCoder.reverseGeoCode(option);
        //为地理编码查询对象设置一个请求结果监听器
        geoCoder.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
            @Override
            public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
                Log.d(TAG, "地理编码信息 ---> \nAddress : " +
                        geoCodeResult.getAddress() + "\ntoString : " +
                        geoCodeResult.toString() + "\ndescribeContents : " +
                        geoCodeResult.describeContents());
            }

            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
                //当获取到反编码信息结果的时候会调用
                addr[0] = reverseGeoCodeResult.getAddress();
                //获取地址的详细内容对象，此类表示地址解析结果的层次化地址信息。
                ReverseGeoCodeResult.AddressComponent addressDetail = reverseGeoCodeResult.getAddressDetail();
                Log.d(TAG, "反地理编码信息 ---> \nAddress : " + addr[0]
                        + "\nBusinessCircle : " + reverseGeoCodeResult.getBusinessCircle()
                        //位置所属商圈名称
                        + "\ncity : " + addressDetail.city  //所在城市名称
                        + "\ndistrict : " + addressDetail.district //区县名称
                        + "\nprovince : " + addressDetail.province  //省份名称
                        + "\nstreet : " + addressDetail.street //街道名
                        + "\nstreetNumber : " + addressDetail.streetNumber);//街道（门牌）号码

                StringBuilder poiInfoBuilder = new StringBuilder();
                //poiInfo信息
                List<PoiInfo> poiInfoList = reverseGeoCodeResult.getPoiList();
                if (poiInfoList != null) {
                    poiInfoBuilder.append("\nPoilist size : " + poiInfoList.size());
                }
                for (PoiInfo p : poiInfoList)

                {
                    poiInfoBuilder.append("\n\taddress: " + p.address);//地址信息
                    poiInfoBuilder.append(" name: " + p.name + " postCode: " + p.postCode);
                    //名称、邮编
                    // 还有其他的一些信息，我这里就不打印了，请参考API
                }
            Log.d(TAG,"poiInfo --> " + poiInfoBuilder.toString());
            //动态创建一个View用于显示位置信息
              Button button = new Button(getApplicationContext());
            //设置view是背景图片
                button.setBackgroundResource(R.drawable.popup);
                button.setText(addr[0]);
                InfoWindow infoWindow = new InfoWindow(BitmapDescriptorFactory.fromView(button), latLng, -47, new InfoWindow.OnInfoWindowClickListener() {
                    @Override
                    public void onInfoWindowClick() {
                        //当InfoWindow被点击后隐藏
                       baiduMap.hideInfoWindow();
                    }

    });
                baiduMap.showInfoWindow(infoWindow);
            }
        });
        return ;

    }
}