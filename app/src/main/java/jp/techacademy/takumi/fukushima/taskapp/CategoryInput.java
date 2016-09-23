package jp.techacademy.takumi.fukushima.taskapp;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

import io.realm.Realm;
import io.realm.RealmResults;

public class CategoryInput extends AppCompatActivity {

    EditText new_category_editText;
    private Category mCategory;
    String category;
    int id;

    private View.OnClickListener mOnDoneClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            addCategory();
            finish();
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_category);

        //ActionBarを設定する
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        findViewById(R.id.category_done_button).setOnClickListener(mOnDoneClickListener);
        new_category_editText = (EditText) findViewById(R.id.new_category_editText);
    }

    private void addCategory(){

        Realm realm = Realm.getDefaultInstance();
        //新規作成の場合
        mCategory = new Category();

        RealmResults<Category> categoryRealmResults = realm.where(Category.class).findAll();

        int identifier;
        if(categoryRealmResults.max("id") != null){
            identifier = categoryRealmResults.max("id").intValue() + 1;
        }else{
            identifier = 0;
        }
        mCategory.setId(identifier);
        category = new_category_editText.getText().toString();
        mCategory.setCategory(category);

        realm.beginTransaction();
        realm.copyToRealmOrUpdate(mCategory);
        realm.commitTransaction();

        realm.close();

    }
}
