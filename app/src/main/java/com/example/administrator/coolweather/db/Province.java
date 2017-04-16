package com.example.administrator.coolweather.db;

import org.litepal.crud.DataSupport;

/**
 * 这是一个 Provice 数据库表
 */
public class Province extends DataSupport {
    private int id;
    private String provinceName;
    private int provinceCode;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceCode(int provinceCode) {
        this.provinceCode = provinceCode;
    }

    public int getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }
}
