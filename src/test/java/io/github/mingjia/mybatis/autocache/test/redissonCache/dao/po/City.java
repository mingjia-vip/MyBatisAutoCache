package io.github.mingjia.mybatis.autocache.test.redissonCache.dao.po;

import java.io.Serializable;

/**
 * Description: City
 *
 * @auther GuiBin
 * @create 18/3/13
 */
public class City implements Serializable {
    private Integer id;
    private Integer countryid;
    private String name;
    private String countryname;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCountryid() {
        return countryid;
    }

    public void setCountryid(Integer countryid) {
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
