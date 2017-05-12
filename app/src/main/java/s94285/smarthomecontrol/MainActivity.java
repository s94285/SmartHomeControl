package s94285.smarthomecontrol;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewFlipper;
import com.github.niqdev.mjpeg.DisplayMode;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    ViewFlipper vf;
    TextView tx_home,tx_ipcam;
    MjpegView mjpegView;
    private final static int VF_SWITCH=0,VF_THEME=1,VF_IPCAM=2,VF_PREFER=3;
    private final static int TIMEOUT = 5;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mjpegView = (MjpegView)findViewById(R.id.ipcam_mjpegView);
        ViewFlipper vf = (ViewFlipper)findViewById(R.id.vf);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view->{
            switch(vf.getDisplayedChild()){
                case VF_IPCAM :
                    Snackbar.make(view, "Loading", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    loadIpCam();
                    break;
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        init();
    }

    private void init(){

    }

    private void loadIpCam() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        try{
            Mjpeg.newInstance()
                    .open("http://"+settings.getString("pref_ipcam_ed_webcamip","127.0.0.1:8080")+"/video", TIMEOUT)
                    .subscribe(inputStream -> {
                        mjpegView.setSource(inputStream);
                        mjpegView.setDisplayMode(DisplayMode.BEST_FIT);
                        mjpegView.showFps(settings.getBoolean("pref_ipcam_sw_showfps",false));
                    },throwable->{
                        Log.e("Error: ",throwable.toString());
                        throwable.printStackTrace();
                    });}
        catch (Throwable onError){
            onError.printStackTrace();
        }
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        ViewFlipper vf = (ViewFlipper)findViewById(R.id.vf);
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_switch) {
            // Handle the camera action
            vf.setDisplayedChild(0);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
/*            tx_home = (TextView)findViewById(R.id.textView3);
            tx_home.setText(sharedPreferences.getString("example_text",""));*/
        } else if (id == R.id.nav_theme) {
            vf.setDisplayedChild(1);
        } else if (id == R.id.nav_ipcam) {
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            vf.setDisplayedChild(2); //TODO
        } else if (id == R.id.nav_preference) {
            Intent intent = new Intent(MainActivity.this,SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_about) {

        } else if (id == R.id.nav_reference) {

        } else if (id == R.id.nav_exit) {
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onPause() {
        mjpegView.stopPlayback();
        super.onPause();
    }

    @Override
    protected void onResume() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        tx_home = (TextView)findViewById(R.id.textView3);
        tx_home.setText(sharedPreferences.getString("example_text",""));
        tx_ipcam = (TextView)findViewById(R.id.ipcam_tx_IP);
        tx_ipcam.setText(sharedPreferences.getString("pref_ipcam_ed_webcamip","Default IP"));
        super.onResume();
    }
}
