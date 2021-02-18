package com.factory.procedure.pictureapp;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Product class to store the product information to create metadata
 */
public class Product implements Serializable{

    String  name, author;
    Integer id;
    Long  created, modified;
//    Need the [] of the procedures
    ArrayList<Procedure> procedure = new ArrayList<>();

//  Constructor
    Product(Integer id, String name, Long created, Long modified, String creator){
        this.created = created;
        this.id = id;
        this.modified = modified;
        this.name = name;
        this.author = creator;
    }

    public void setProcedureList(ArrayList<Procedure> procedureList) {
        this.procedure = procedureList;
    }

    public ArrayList<Procedure> getProcedureList() {
        return procedure;
    }

    public Integer getProductid() {
        return id;
    }

    public void setModifiedTime(Long modifiedTime) {
        this.modified = modifiedTime;
    }

    public String getProductName() {
        return name;
    }

    public void setProductName(String name){
        this.name = name;
    }

//    Set the name to send, if success update
    public Product updateProductName(String name){
        return new Product(this.id, name, this.created, this.modified, this.author);
    }

}





