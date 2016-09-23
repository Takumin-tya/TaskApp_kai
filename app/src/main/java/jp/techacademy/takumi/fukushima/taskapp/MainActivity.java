package jp.techacademy.takumi.fukushima.taskapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public final static String EXTRA_TASK = "jp.techacademy.takumi.fukushima.taskapp.TASK";
    public final static String EXTRA_CATEGORY = "jp.techacademy.takumi.fukushima.taskapp.CATEGORY";

    private Realm mRealm;
    private RealmResults<Task> mTaskRealmResults;
    private RealmResults<Category> mCategoryRealmResults;
    private RealmChangeListener mRealmListener = new RealmChangeListener() {
        @Override
        public void onChange() {
            reloadListView();
        }
    };
    private ListView mListView;
    private ListView categoryView;
    private TaskAdapter mTaskAdapter;
    private CategoryAdapter mCategoryAdapter;

    NavigationView navigationView;
    Menu menu;
    ArrayList<MenuItem> menuItems;
    private String searchString;
    private Category category;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //フローティングアクションボタンの定義
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                startActivity(intent);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        menu = navigationView.getMenu();



        //Task用Realmの設定
        mRealm = Realm.getDefaultInstance();
        mTaskRealmResults = mRealm.where(Task.class).findAll();
        mTaskRealmResults.sort("date");
        mRealm.addChangeListener(mRealmListener);

        //Category用Realmの設定
        mRealm = Realm.getDefaultInstance();
        mCategoryRealmResults = mRealm.where(Category.class).findAll();
        mCategoryRealmResults.sort("id");
        mRealm.addChangeListener(mRealmListener);


        //ListViewの設定
        mTaskAdapter = new TaskAdapter(MainActivity.this);
        mListView = (ListView) findViewById(R.id.listView1);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //入力・編集を行う画面に遷移する
                Task task = (Task) parent.getAdapter().getItem(position);

                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                intent.putExtra(EXTRA_TASK, task);

                startActivity(intent);
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                //タスク削除用処理

                final Task task = (Task) parent.getAdapter().getItem(position);

                //ダイアログの表示
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("削除");
                builder.setMessage(task.getTitle() + "を削除しますか？");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RealmResults<Task> results = mRealm.where(Task.class).equalTo("id", task.getId()).findAll();
                        mRealm.beginTransaction();
                        results.clear();
                        mRealm.commitTransaction();

                        Intent resultIntent = new Intent(getApplicationContext(), TaskAlarmReceiver.class);
                        PendingIntent resultPendingIntent = PendingIntent.getBroadcast(
                                MainActivity.this,
                                task.getId(),
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.cancel(resultPendingIntent);
                        reloadListView();
                    }
                });
                builder.setNegativeButton("CANCEL", null);
                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            }
        });

        //CategoryViewの設定
        mCategoryAdapter = new CategoryAdapter(MainActivity.this);
        categoryView = (ListView) findViewById(R.id.content_list);


        if(mTaskRealmResults.size() == 0){
            //アプリ起動時にタスクの数が0であった場合は表示テスト用のタスクを作成する
            addTaskForTest();
            Log.d("Android", "タスク０");
        }

        if(mCategoryRealmResults.size() == 0){
            //アプリ起動時にカテゴリ数が0であった場合
            addDefaultCategory();
        }

        reloadListView();
        Log.d("Android", "リストの更新");

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        //SearchViewを取得する
        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchString = query;

                Log.d("Android","検索");

                Log.d("Android", "絞込み開始");
                mTaskRealmResults = mRealm.where(Task.class).beginsWith("title", searchString).findAll();
                mTaskRealmResults.sort("date", Sort.DESCENDING);
                mRealm.addChangeListener(mRealmListener);

                reloadListView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d("Android", "絞込み開始");
                if(newText.equals("")){
                    Log.d("Android", "絞込みなし");
                    mTaskRealmResults = mRealm.where(Task.class).findAll();
                    mTaskRealmResults.sort("date", Sort.DESCENDING);
                    mRealm.addChangeListener(mRealmListener);
                }
                reloadListView();
                return false;
            }

        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //登録されているタスクを抽出しリストに格納
    private void reloadListView(){

        ArrayList<Task> taskArrayList = new ArrayList<>();
        ArrayList<Category> categoryArrayList = new ArrayList<>();

        for(int i = 0; i < mTaskRealmResults.size(); i++) {
            Task task = new Task();

            task.setId(mTaskRealmResults.get(i).getId());
            task.setTitle(mTaskRealmResults.get(i).getTitle());
            task.setCategory(mTaskRealmResults.get(i).getCategory());
            task.setContents(mTaskRealmResults.get(i).getContents());
            task.setDate(mTaskRealmResults.get(i).getDate());

            taskArrayList.add(task);
        }

        mTaskAdapter.setTaskArrayList(taskArrayList);
        mListView.setAdapter(mTaskAdapter);
        mTaskAdapter.notifyDataSetChanged();

        menu.clear();
        navigationView.inflateMenu(R.menu.activity_main_drawer);
        for(int i = 0; i < mCategoryRealmResults.size(); i++){

            menu.add(mCategoryRealmResults.get(i).getCategory());
            category = mCategoryRealmResults.get(i);

            View v = new View(this);
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    //ダイアログの表示
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                    builder.setTitle("削除");
                    builder.setMessage("カテゴリーを削除しますか？");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            RealmResults<Category> results = mRealm.where(Category.class).equalTo("id",category.getId() ).findAll();
                            mRealm.beginTransaction();
                            results.clear();
                            mRealm.commitTransaction();
                            reloadListView();
                        }
                    });
                    builder.setNegativeButton("CANCEL", null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return false;
                }
            });

            menu.getItem(i + 1).setActionView(v);
            //category.setCategory(mCategoryRealmResults.get(i).getCategory());
            //category.setId(mCategoryRealmResults.get(i).getId());

            //categoryArrayList.add(category);
        }

        //mCategoryAdapter.setmCategoryArrayList(categoryArrayList);
        //categoryView.setAdapter(mCategoryAdapter);
        //mCategoryAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        mRealm.close();
    }

    private void addTaskForTest(){
        Task task = new Task();
        task.setTitle("作業");
        task.setContents("プログラムを書いてpushする");
        task.setDate(new Date());
        task.setCategory("No Category");
        task.setId(0);
        mRealm.beginTransaction();
        mRealm.copyToRealmOrUpdate(task);
        mRealm.commitTransaction();
        Log.d("Android", "テスト用リスト作成");
    }

    private void addDefaultCategory(){
        Category category = new Category();
        category.setCategory("No Category");
        category.setId(0);
        mRealm.beginTransaction();
        mRealm.copyToRealmOrUpdate(category);
        mRealm.commitTransaction();
        Log.d("Android","defaultカテゴリの登録");
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Log.d("ITEM", String.valueOf(item.getItemId()));
        Log.d("ITEM", String.valueOf(item.getTitle()));
        searchString = String.valueOf(item.getTitle());

        if(searchString.equals("ALL")){
            Log.d("Android", "絞込みなし");
            mTaskRealmResults = mRealm.where(Task.class).findAll();
            mTaskRealmResults.sort("date", Sort.DESCENDING);
            mRealm.addChangeListener(mRealmListener);
        }else {

            Log.d("Android", "検索");

            Log.d("Android", "絞込み開始");
            mTaskRealmResults = mRealm.where(Task.class).beginsWith("category", searchString).findAll();
            mTaskRealmResults.sort("date", Sort.DESCENDING);
            mRealm.addChangeListener(mRealmListener);
        }

        reloadListView();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
