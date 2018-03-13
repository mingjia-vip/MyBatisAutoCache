package io.github.mingjia.MyBatisCache.test.dao.po;

import java.io.Serializable;

/**
 * Description: City
 *
 * @auther GuiBin
 * @create 18/3/13
 */
public class City implements Serializable {
    private int id;
    private int countryid;
    private String name;
    private String countryname;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCountryid() {
        return countryid;
    }

    public void setCountryid(int countryid) {
        this.countryid = countryid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountryname() {
        return countryname;
    }

    public void setCountryname(String countryname) {
        this.countryname = countryname;
    }

    @Override
    public String toString() {
        return "City{" +
                "id=" + id +
                ", countryid=" + countryid +
                ", name='" + name + '\'' +
                ", countryname='" + countryname + '\'' +
                '}';
    }
}
