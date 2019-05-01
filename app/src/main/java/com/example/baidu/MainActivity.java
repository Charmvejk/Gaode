package com.example.baidu;

import android.app.ProgressDialog;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.SupportMapFragment;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.core.SuggestionCity;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;

import java.util.List;

public class MainActivity extends FragmentActivity implements LocationSource,
        AMapLocationListener, AMap.OnMapClickListener, GeocodeSearch.OnGeocodeSearchListener, View.OnClickListener, TextWatcher, PoiSearch.OnPoiSearchListener, AMap.InfoWindowAdapter {
    private AMap mMap;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mLocationClient;
    private String simpleAddress;//自己的地址
    /*************************************** 搜索******************************************************/
    private AutoCompleteTextView searchText;// 输入搜索关键字
    private String keyWord = "";// 要输入的poi搜索关键字
    private ProgressDialog progressDialog;
    private PoiSearch.Query query;// Poi查询条件类
    private EditText editText;
    //todo 点击自己区域也显示地区
    private StringBuffer strBuffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 不显示程序的标题栏
        setContentView(R.layout.activity_main);
        AppCompatButton searButton = findViewById(R.id.searchButton);
        searchText = findViewById(R.id.keyWord);
        editText = findViewById(R.id.city);
        searchText.addTextChangedListener(this);// 添加文本输入框监听事件
        searButton.setOnClickListener(this);
        initView();

    }


    /**
     * 初始化
     */
    private void initView() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map)).getMap();
            //todo 地图点击事件
            mMap.setOnMapClickListener(this);
            //todo 显示符合的地点
            mMap.setInfoWindowAdapter(this);// 添加显示infowindow监听事件

            setUpMap();
        }
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        mLocationClient.stopLocation();
//    }

    /**
     * 设置一些amap的属性
     */
    private void setUpMap() {
        //定义一个UiSettings对象
        UiSettings mUiSettings = mMap.getUiSettings();

        mUiSettings.setZoomControlsEnabled(true);//缩放按钮
        mUiSettings.setCompassEnabled(true);//指南针
        mUiSettings.setScaleControlsEnabled(true);//控制比例尺控件是否显示
        mUiSettings.setLogoPosition(AMapOptions.LOGO_POSITION_BOTTOM_RIGHT);//设置logo位置
        mMap.setLocationSource(this);// 设置定位监听
        mUiSettings.setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        mMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
    }


    /**
     * 定位成功后回调函数
     */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (mListener != null && aMapLocation != null) {
            if (aMapLocation != null
                    && aMapLocation.getErrorCode() == 0) {
                LatLng latLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                mListener.onLocationChanged(aMapLocation);// 显示系统小蓝点
                //首次定位,选择移动到地图中心点并修改级别到15级
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                //todo 获取定位信息
                strBuffer = new StringBuffer();
                strBuffer.append(
                        aMapLocation.getProvince() + ""
                                + aMapLocation.getCity() + ""
                                + aMapLocation.getDistrict() + ""
                                + aMapLocation.getStreet() + ""
                                + aMapLocation.getStreetNum());
                Toast.makeText(getApplicationContext(), strBuffer.toString(), Toast.LENGTH_LONG).show();

            } else {
                String errText = "定位失败," + aMapLocation.getErrorCode() + ": " + aMapLocation.getErrorInfo();
                Log.e("AmapErr", errText);
            }
        }
    }


    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener aMapLocation) {
        mListener = aMapLocation;
        if (mLocationClient == null) {
            mLocationClient = new AMapLocationClient(this);
            AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mLocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //是指定位间隔
            mLocationOption.setInterval(2000);
            //设置定位参数
            mLocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mLocationClient.startLocation();
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        deactivate();
    }


    @Override
    public void onMapClick(LatLng latLng) {
        LatLonPoint latLonPoint = new LatLonPoint(latLng.latitude, latLng.longitude);
        //地理搜索类
        GeocodeSearch geocodeSearch = new GeocodeSearch(getApplicationContext());
        geocodeSearch.setOnGeocodeSearchListener(this);
        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 25, GeocodeSearch.AMAP);
        geocodeSearch.getFromLocationAsyn(query);
    }

    /**
     * 经纬度转换成地址
     */
    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
        simpleAddress = regeocodeResult.getRegeocodeAddress().getFormatAddress();
        Toast.makeText(getApplicationContext(), simpleAddress, Toast.LENGTH_LONG).show();
        mLocationClient.stopLocation();
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deactivate();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.searchButton:
                searchButton();
                break;
        }
    }

    private void searchButton() {
        keyWord = searchText.getText().toString().trim();
        if ("".equals(keyWord)) {
            Toast.makeText(getApplicationContext(), "请输入搜索关键字", Toast.LENGTH_LONG).show();
            return;
        }
        doSearchQuery();
    }

    private void doSearchQuery() {
        String mCity = editText.getText().toString().trim();
        if (mCity.equals("")) {
            Toast.makeText(getApplicationContext(), "请输入要查询的城市", Toast.LENGTH_LONG).show();
            return;
        }
        showProgressDialog();// 显示进度框
        query = new PoiSearch.Query(keyWord, "", mCity);// 第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索区域（空字符串代表全国）
        // POI搜索
        PoiSearch poiSearch = new PoiSearch(this, query);
        poiSearch.setOnPoiSearchListener(this);
        poiSearch.searchPOIAsyn();
        //todo 消耗资源
        mLocationClient.stopLocation();

    }

    /**
     * 显示进度框
     */
    private void showProgressDialog() {
        progressDialog = null;
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("正在搜索:\n" + keyWord);
        progressDialog.show();
    }

    /**
     * 隐藏进度框
     */
    private void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onPoiSearched(PoiResult result, int rCode) {
        dismissProgressDialog();// 隐藏对话框
        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getQuery() != null) {// 搜索poi的结果
                if (result.getQuery().equals(query)) {// 是否是同一条
                    // poi返回的结果
                    PoiResult poiResult = result;
                    // 取得搜索到的poiitems有多少页
                    List<PoiItem> poiItems = poiResult.getPois();// 取得第一页的poiitem数据，页数从数字0开始
                    List<SuggestionCity> suggestionCities = poiResult
                            .getSearchSuggestionCitys();// 当搜索不到poiitem数据时，会返回含有搜索关键字的城市信息

                    if (poiItems != null && poiItems.size() > 0) {
                        mMap.clear();// 清理之前的图标
                        PoiOverlay poiOverlay = new PoiOverlay(mMap, poiItems);
                        poiOverlay.removeFromMap();
                        poiOverlay.addToMap();
                        poiOverlay.zoomToSpan();
                    } else if (suggestionCities != null
                            && suggestionCities.size() > 0) {
                        Toast.makeText(getApplicationContext(), "未获取到有关信息", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "未获取到有关信息", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), "未获取到有关信息", Toast.LENGTH_LONG).show();

            }
        } else {
            Toast.makeText(getApplicationContext(), "错误！", Toast.LENGTH_LONG).show();

        }

    }


    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }

    @Override
    public View getInfoWindow(final Marker marker) {
        if (marker.getTitle() != null && marker.getSnippet() != null) {
            Toast.makeText(getApplicationContext(), marker.getTitle() + marker.getSnippet(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), strBuffer.toString(), Toast.LENGTH_LONG).show();
        }
        return null;
    }


    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}


