package com.yy.baidumap.bean;

import com.yy.baidumap.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 13160677911 on 2016-11-17.
 */

public class Info implements Serializable {
    private static final long serialVersionUID = -1010711775392052966L;    private double latitude; //纬度
    private double longtitude;  //经度
    private int imgId;  //图片ID
    private String name;  //商家名称
    private String distance; //距离
    private int zan;  //赞的数量

    public static List<Info> infos = new ArrayList<Info>();

    static
    {
        infos.add(new Info(34.242652, 108.971171, R.mipmap.a01, "英伦贵族小旅馆", "距离209米", 1456));
        infos.add(new Info(34.242952, 108.972171, R.mipmap.a02, "沙井国际洗浴会所", "距离897米", 456));
        infos.add(new Info(34.242852, 108.973171, R.mipmap.a03, "五环服装城", "距离249米", 1456));
        infos.add(new Info(34.242152, 108.971971, R.mipmap.a04, "老米家泡馍小炒", "距离679米", 1456));
    }

    public Info(double latitude, double longtitude, int imgId, String name, String distance, int zan) {
        this.latitude = latitude;
        this.longtitude = longtitude;
        this.imgId = imgId;
        this.name = name;
        this.distance = distance;
        this.zan = zan;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongtitude() {
        return longtitude;
    }

    public void setLongtitude(double longtitude) {
        this.longtitude = longtitude;
    }

    public int getImgId() {
        return imgId;
    }

    public void setImgId(int imgId) {
        this.imgId = imgId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public int getZan() {
        return zan;
    }

    public void setZan(int zan) {
        this.zan = zan;
    }

    public static List<Info> getInfos() {
        return infos;
    }

    public static void setInfos(List<Info> infos) {
        Info.infos = infos;
    }
}
