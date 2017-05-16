package s94285.smarthomecontrol;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.StringBuilderPrinter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import com.github.niqdev.mjpeg.DisplayMode;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegView;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.ip.IpParameters;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private ViewFlipper vf;
    private TextView tx_ipcam, switch_ac_tx_nowtemp, switch_ac_tx_targettemp, switch_elevator_tx_floor;
    private MjpegView mjpegView;
    boolean bool_showMenu = true;
    private SeekBar switch_ac_sk_targettemp;
    private Button switch_ac_bt_windstrong, switch_ac_bt_windmedium, switch_ac_bt_windweak, switch_elevator_bt_firstfloor, switch_elevator_bt_secondfloor;
    private ToggleButton switch_ac_tg_toggle;
    private Switch switch_light_1, switch_light_2, switch_light_3, switch_light_4, switch_light_5, switch_light_6, switch_light_7, switch_light_8;

    private ModbusMaster modbusMaster;
    private IpParameters ipSlave;
    private ModbusFactory modbusFactory;
    private String IP;
    private Exception error;
    private short targetTemp = 26,nowTemp = 0,onFloor = 1;
    private Boolean[] xBoolArray = {false,false,false,false,false,false,false,false};

    private final static int VF_SWITCH = 0, VF_THEME = 1, VF_IPCAM = 2, VF_PREFER = 3;
    private final static int TIMEOUT = 5;
    private final static int[] SWITCH_ID = {R.id.switch_light_switch1, R.id.switch_light_switch2, R.id.switch_light_switch3, R.id.switch_light_switch4, R.id.switch_light_switch5, R.id.switch_light_switch6, R.id.switch_light_switch7, R.id.switch_light_switch8};
    private final static int WIND_STRONG = 0,WIND_MEDIUM = 1, WIND_WEAK = 2, AC_POWER = 3, ELEVATOR_FIRST = 4,ELEVATOR_SECOND = 5;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mjpegView = (MjpegView) findViewById(R.id.ipcam_mjpegView);
        vf = (ViewFlipper) findViewById(R.id.vf);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);
        fab.setOnClickListener(view -> {
            switch (vf.getDisplayedChild()) {
                case VF_IPCAM:
                    Snackbar.make(view, getString(R.string.ipcam_fab_load), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    loadIpCam();
                    break;
            }
        });
        fab.setOnLongClickListener(view -> {
            switch (vf.getDisplayedChild()) {
                case VF_IPCAM:
                    Snackbar.make(view, getString(R.string.ipcam_fab_Stop), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    mjpegView.stopPlayback();
                    break;
            }
            return false;
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        findView();
        setListener();
        init();
    }

    private void setListener() {
        View.OnClickListener onClickListener = (view -> {
            switch(view.getId()){
                case R.id.switch_ac_bt_windstrong:
                    xBoolArray[WIND_STRONG] = true;
                    break;
                case R.id.switch_ac_bt_windmedium:
                    xBoolArray[WIND_MEDIUM] = true;
                    break;
                case R.id.switch_ac_bt_windweak:
                    xBoolArray[WIND_WEAK] = true;
                    break;
                case R.id.switch_elevator_bt_firstfloor:
                    xBoolArray[ELEVATOR_FIRST] = true;
                    break;
                case R.id.switch_elevator_bt_secondfloor:
                    xBoolArray[ELEVATOR_SECOND] = true;
                    break;
            }
            startRunnable(writeModbus);
        });
        for (int i = 0; i < 8; i++) {
            Switch switches = (Switch) findViewById(SWITCH_ID[i]);
            switches.setOnClickListener(onClickListener);
        }
        switch_ac_bt_windstrong.setOnClickListener(onClickListener);
        switch_ac_bt_windmedium.setOnClickListener(onClickListener);
        switch_ac_bt_windweak.setOnClickListener(onClickListener);
        switch_elevator_bt_firstfloor.setOnClickListener(onClickListener);
        switch_elevator_bt_secondfloor.setOnClickListener(onClickListener);

        switch_ac_sk_targettemp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Integer integer = i + 16;
                targetTemp = Short.parseShort(integer.toString());
                switch_ac_tx_targettemp.setText(String.valueOf(targetTemp));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                startRunnable(writeModbus);
            }
        });
        switch_ac_tg_toggle.setOnCheckedChangeListener((compoundButton, b) -> {
            xBoolArray[AC_POWER] = b;
            Log.d("acPower",String.valueOf(b));
            startRunnable(writeModbus);
        });
    }

    private void init() {
        TabHost tabHost = (TabHost) findViewById(R.id.tabhost);
        tabHost.setup();

        TabHost.TabSpec spec = tabHost.newTabSpec("tab1");
        spec.setContent(R.id.tab1);
        spec.setIndicator(getString(R.string.switch_tab_tab1_title));
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("tab2");
        spec.setContent(R.id.tab2);
        spec.setIndicator(getString(R.string.switch_tab_tab2_title));
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("tab3");
        spec.setContent(R.id.tab3);
        spec.setIndicator(getString(R.string.switch_tab_tab3_title));
        tabHost.addTab(spec);

        ipSlave = new IpParameters();
        modbusFactory = new ModbusFactory();

        startRunnable(readModbus);


    }

    private void findView() {
        switch_light_1 = (Switch) findViewById(R.id.switch_light_switch1);
        switch_light_2 = (Switch) findViewById(R.id.switch_light_switch2);
        switch_light_3 = (Switch) findViewById(R.id.switch_light_switch3);
        switch_light_4 = (Switch) findViewById(R.id.switch_light_switch4);
        switch_light_5 = (Switch) findViewById(R.id.switch_light_switch5);
        switch_light_6 = (Switch) findViewById(R.id.switch_light_switch6);
        switch_light_7 = (Switch) findViewById(R.id.switch_light_switch7);
        switch_light_8 = (Switch) findViewById(R.id.switch_light_switch8);
        switch_ac_tx_nowtemp = (TextView) findViewById(R.id.switch_ac_tx_nowtemp);
        switch_ac_tx_targettemp = (TextView) findViewById(R.id.switch_ac_tx_targettemp);
        switch_elevator_tx_floor = (TextView) findViewById(R.id.switch_elevator_tx_floor);
        switch_ac_bt_windstrong = (Button) findViewById(R.id.switch_ac_bt_windstrong);
        switch_ac_bt_windmedium = (Button) findViewById(R.id.switch_ac_bt_windmedium);
        switch_ac_bt_windweak = (Button) findViewById(R.id.switch_ac_bt_windweak);
        switch_elevator_bt_firstfloor = (Button) findViewById(R.id.switch_elevator_bt_firstfloor);
        switch_elevator_bt_secondfloor = (Button) findViewById(R.id.switch_elevator_bt_secondfloor);
        switch_ac_tg_toggle = (ToggleButton) findViewById(R.id.switch_ac_tg_toggle);
        switch_ac_sk_targettemp = (SeekBar) findViewById(R.id.switch_ac_sk_targettemp);
    }

    private void refreshModbus() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        Log.d("Host", settings.getString("pref_switch_host", "0"));
        Log.d("port", settings.getString("pref_switch_port", "0"));
        ipSlave.setHost(settings.getString("pref_switch_host", getString(R.string.pref_switch_host_default)));
        ipSlave.setPort(Integer.parseInt(settings.getString("pref_switch_port", getString(R.string.pref_switch_port_default))));
        if (modbusMaster == null)
            modbusMaster = modbusFactory.createTcpMaster(ipSlave, true);     //Check if modbusMaster is null
        if (!modbusMaster.isInitialized()) {      //Initialize if didn't
            try {
                Log.d("timeout", settings.getString("pref_switch_timeout", "0"));
                Log.d("retries", settings.getString("pref_switch_retries", "0"));
                modbusMaster.setTimeout(Integer.parseInt(settings.getString("pref_switch_timeout", getString(R.string.pref_switch_timeout_default))));
                modbusMaster.setRetries(Integer.parseInt(settings.getString("pref_switch_retries", getString(R.string.pref_switch_retries_default))));
                modbusMaster.init();
            } catch (ModbusInitException e) {
                e.printStackTrace();
                error = e;
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, getString(R.string.modbus_not_init), Toast.LENGTH_SHORT).show()
                );
            }
        }
    }

    private Runnable readModbus = () -> {
        refreshModbus();
        if (modbusMaster != null)
            if (modbusMaster.isInitialized()) {
                try {
                    //Lights
                    ModbusRW modbusRW = new ModbusRW(modbusMaster);
                    final Boolean[] boolArray_lights = modbusRW.mbReadByteToBoolean(15);
                    runOnUiThread(() ->
                            changeSwitchStatus(boolArray_lights));
                    //AC
                    nowTemp = modbusRW.mbReadINTToShort(41);
                    targetTemp = modbusRW.mbReadINTToShort(42);
                    onFloor = modbusRW.mbReadINTToShort(43);
                    xBoolArray = modbusRW.mbReadByteToBoolean(40);
                    runOnUiThread(() -> {
                        switch_ac_tx_nowtemp.setText(String.valueOf(nowTemp));
                        switch_ac_tx_targettemp.setText(String.valueOf(targetTemp));
                        switch_ac_sk_targettemp.setProgress(targetTemp-16);
                        switch_ac_tg_toggle.setChecked(xBoolArray[AC_POWER]);
                        switch_elevator_tx_floor.setText(String.valueOf(onFloor));
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, getString(R.string.modbus_not_read), Toast.LENGTH_SHORT).show()
                    );
                }
            }
    };

    private Runnable writeModbus = () -> {
        refreshModbus();
        if (modbusMaster != null)
            if (modbusMaster.isInitialized()) {
                try {
                    //Lights
                    ModbusRW modbusRW = new ModbusRW(modbusMaster);
                    modbusRW.mbWriteShortToINT(15, modbusRW.valueOfBoolArray(readSwitchStatus()).shortValue());
                    //AC
                    modbusRW.mbWriteShortToINT(42, targetTemp);
                    modbusRW.mbWriteShortToINT(40, modbusRW.valueOfBoolArray(xBoolArray).shortValue());
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, getString(R.string.modbus_not_write), Toast.LENGTH_SHORT).show()
                    );
                }
            }
    };

    private void loadIpCam() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        try {
            Mjpeg.newInstance()
                    .open("http://" + settings.getString("pref_ipcam_ed_webcamip", getString(R.string.pref_default_webcamIP)), TIMEOUT)
                    .subscribe(inputStream -> {
                        mjpegView.setSource(inputStream);
                        mjpegView.setDisplayMode(DisplayMode.BEST_FIT);
                        mjpegView.showFps(settings.getBoolean("pref_ipcam_sw_showfps", false));
                    }, throwable -> {
                        Log.e("Error: ", throwable.toString());
                        throwable.printStackTrace();
                    });
        } catch (Throwable onError) {
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
        if (bool_showMenu) {
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_save:
                break;
            case R.id.action_refresh:
                Toast.makeText(MainActivity.this, getString(R.string.modbus_refreshing), Toast.LENGTH_SHORT).show();
                startRunnable(readModbus);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        ViewFlipper vf = (ViewFlipper) findViewById(R.id.vf);
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_switch) {
            // Handle the camera action
            vf.setDisplayedChild(0);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        } else if (id == R.id.nav_theme) {
            vf.setDisplayedChild(1);
        } else if (id == R.id.nav_ipcam) {
            vf.setDisplayedChild(2); //TODO
        } else if (id == R.id.nav_preference) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_about) {
            int versionNumber = 0;
            String versionName = "Cannot Get Version Name";
            try {
                PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                versionNumber = pinfo.versionCode;
                versionName = pinfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.drawer_about)
                    .setCancelable(false)
                    .setMessage("VersionCode: " + versionNumber + "\nVersionName: " + versionName + "\nMade By 中工三電乙 林宏哲")
                    .setPositiveButton(R.string.alertDialog_confirm, (dialogInterface, i) -> {
                    })
                    .setNeutralButton(R.string.about_checkVersion, (dialogInterface, i) ->
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/s94285/SmartHomeControl"))))
                    .show();

        } else if (id == R.id.nav_reference) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(R.string.reference_text)
                    .setCancelable(true)
                    .show();

        } else if (id == R.id.nav_exit) {
            finish();
        }
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(vf.getDisplayedChild() >= 1
                ? View.VISIBLE
                : View.INVISIBLE);
        bool_showMenu = (vf.getDisplayedChild() == 0);
        invalidateOptionsMenu();

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
        //tx_home = (TextView)findViewById(R.id.textView3);
        //tx_home.setText(sharedPreferences.getString("example_text",""));
        tx_ipcam = (TextView) findViewById(R.id.ipcam_tx_IP);
        tx_ipcam.setText(sharedPreferences.getString("pref_ipcam_ed_webcamip", "Default IP"));
        if (modbusMaster != null) modbusMaster.destroy();
        super.onResume();
    }

    private Boolean[] readSwitchStatus() {
        Boolean[] booleans = new Boolean[8];
        for (int i = 0; i < 8; i++) {
            Switch switches = (Switch) findViewById(SWITCH_ID[i]);
            booleans[i] = switches.isChecked();
        }
        return booleans;
    }

    private void changeSwitchStatus(Boolean[] array) {
        for (int i = 0; i < 8; i++) {
            Switch switches = (Switch) findViewById(SWITCH_ID[i]);
            switches.setChecked(array[i]);
        }
    }

    private void startRunnable(Runnable runnable){
        Thread thread = new Thread(runnable);
        thread.start();
    }

}
