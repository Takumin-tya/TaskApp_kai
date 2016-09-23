package jp.techacademy.takumi.fukushima.taskapp;

import java.io.Serializable;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Category extends RealmObject implements Serializable {
    private String category;

    @PrimaryKey
    private int id;

    public int getId(){
        return id;
    }

    public void setId(int id){
        this.id = id;
    }

    public String getCategory(){
        return category;
    }

    public void setCategory(String category){
        this.category = category;
    }

}
