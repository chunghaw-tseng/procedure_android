package com.factory.procedure.pictureapp;

import java.io.Serializable;

/**
 * This is the Category class added so the Products can be sorted
 */
public class Category implements Serializable {

    String name;
    Integer catid;

    Category(String name, Integer id){
        this.name = name;
        this.catid = id;
    }

    public void setCategoryName(String name){
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public Integer getId() {
        return catid;
    }

    //    Set the name to send, if success update
    public Category updateCategoryName(String name){
        return new Category(name, this.catid);
    }
}
