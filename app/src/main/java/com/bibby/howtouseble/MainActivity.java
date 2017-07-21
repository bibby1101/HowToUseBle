package com.bibby.howtouseble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    ArrayList<String> data = new ArrayList<>();
    RecyclerView info;
    private MyAdapter myAdapter;

    private BluetoothAdapter mBluetoothAdapter;

    BleInstance bleInstance;

    public final static UUID HEARTRATESERVICE_UUID =
            new UUID(0x0000DFB000001000L, 0x800000805F9B34FBL);
    public final static UUID WRITE_UUID =
            new UUID(0x0000DFB100001000L, 0x800000805F9B34FBL);
    public final static UUID HEARTRATENOTIFY_UUID =
            new UUID(0x0000DFB100001000L, 0x800000805F9B34FBL);

    public Handler CallBackHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case BleInstance.MSG_GOT_DATA:
                    Log.d("MSG_GOT_DATA", "MSG_GOT_DATA");
                    BluetoothGattCharacteristic readCharacteristic = (BluetoothGattCharacteristic)msg.obj;
                    long currentTime = new Date().getTime();
                    if(readCharacteristic.getUuid().compareTo(HEARTRATENOTIFY_UUID)==0){
                        String value = readCharacteristic.getStringValue(0);
                        ShowLog(value);
                    }
                    break;
                case BleInstance.MSG_GOT_NOTSUPPORTBLE:
                    Log.d("MSG_GOT_NOTSUPPORTBLE", "MSG_GOT_NOTSUPPORTBLE");
                    ShowLog("MSG_GOT_NOTSUPPORTBLE");
                    break;
                case BleInstance.MSG_GOT_NODEVICE:
                    Log.d("MSG_GOT_NODEVICE", "MSG_GOT_NODEVICE");
                    ShowLog("MSG_GOT_NODEVICE");
                    if (bleInstance != null)
                        bleInstance.scanLeDevice(true);
                    break;
                case BleInstance.MSG_GOT_HAVEDEVICE:
                    Log.d("MSG_GOT_HAVEDEVICE", "MSG_GOT_HAVEDEVICE");
                    ShowLog("MSG_GOT_HAVEDEVICE");
                    break;
                case BleInstance.MSG_GOT_CONNECTED:
                    Log.d("MSG_GOT_CONNECTED", "MSG_GOT_CONNECTED");
                    ShowLog("MSG_GOT_CONNECTED");
                    break;
                case BleInstance.MSG_GOT_DISCONNECTED:
                    Log.d("MSG_GOT_DISCONNECTED", "MSG_GOT_DISCONNECTED");
                    ShowLog("MSG_GOT_DISCONNECTED");
                    if (bleInstance != null)
                        bleInstance.scanLeDevice(true);
                    break;
                case BleInstance.MSG_GOT_OPENBT:
                    Log.d("MSG_GOT_OPENBT", "MSG_GOT_OPENBT");
                    ShowLog("MSG_GOT_OPENBT");
                    break;
                case BleInstance.MSG_GOT_STARTREAD:
                    Log.d("MSG_GOT_STARTREAD", "MSG_GOT_STARTREAD");
                    ShowLog("MSG_GOT_STARTREAD");
                    break;
                default:
                    break;
            }
            return true;
        }
    });

    final static int LOCATION_REQUEST_CODE = 9999;
    final static int GPS_REQUEST_CODE = 9527;

    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    LocationSettingsRequest.Builder mLSRBuild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myAdapter = new MyAdapter(data);
        info = (RecyclerView) this.findViewById(R.id.info);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        info.setLayoutManager(layoutManager);

        info.setAdapter(myAdapter);

        ShowLog("onCreate()");

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            ShowLog("不支援BLE");
        }
        else{
            ShowLog("支援BLE");

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) { // Android6.0
                if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    if(bleInstance==null)
                        bleInstance = new BleInstance(this, "Bluno", CallBackHandler, HEARTRATESERVICE_UUID, WRITE_UUID, new UUID[]{HEARTRATENOTIFY_UUID});

                } else {
                    // TODO: 2017/6/14 此API目前還搞不懂邏輯
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, ACCESS_FINE_LOCATION)) {
                        Log.e(TAG, "需要顯示視窗給使用者");
                        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                    }
                    else {
                        Log.e(TAG, "使用者選擇不再顯示");
                        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                    }
                }
            }

            mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
            mLocationRequest = LocationRequest.create()
                    .setInterval(10 * 60 * 1000) // every 10 minutes
                    .setExpirationDuration(10 * 1000) // After 10 seconds
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLSRBuild = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
            mLSRBuild.setAlwaysShow(true);

            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, mLSRBuild.build());
            result.setResultCallback(callbackLSR);

        }
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<String> mData;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.info_text.setText(mData.get(position));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView info_text;
            public ViewHolder(View itemView) {
                super(itemView);
                info_text = (TextView) itemView.findViewById(R.id.info_text);
            }
        }
        public MyAdapter(List<String> data){
            this.mData = data;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        ShowLog("onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        ShowLog("onPause()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ShowLog("onDestroy()");

        if(bleInstance!=null){
            bleInstance.disconnect();
            bleInstance.close();
            bleInstance = null;
        }
    }

    public void ShowLog(final String msg){
        Log.d(TAG, "==========\r\n" + msg + "\r\n==========");
        data.add(msg);
        if(myAdapter!=null){
            myAdapter.notifyDataSetChanged();
        }
    }



    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "GoogleApiClient.ConnectionCallbacks onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "GoogleApiClient.ConnectionCallbacks onConnectionSuspended : " + i);
    }

    /*
    * https://developers.google.com/android/reference/com/google/android/gms/common/ConnectionResult
    * **/
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient.OnConnectionFailedListener onConnectionFailed : " + connectionResult.toString());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult");

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if(bleInstance==null)
                    bleInstance = new BleInstance(this, "Bluno", CallBackHandler, HEARTRATESERVICE_UUID, WRITE_UUID, new UUID[]{HEARTRATENOTIFY_UUID});

            }
            else {
                // Permission was denied. Display an error message.
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==GPS_REQUEST_CODE){
            switch (resultCode){
                case Activity.RESULT_OK:
                    Toast.makeText(MainActivity.this, "定位服務已開啟.", Toast.LENGTH_SHORT).show();
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(MainActivity.this, "必須開啟定位服務.", Toast.LENGTH_SHORT).show();
                    PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, mLSRBuild.build());
                    result.setResultCallback(callbackLSR);
                    break;
                default:
                    Log.d(TAG, "onActivityResult : unknow state : " + resultCode);
                    break;
            }
        }
    }

    ResultCallback<LocationSettingsResult> callbackLSR = new ResultCallback<LocationSettingsResult>() {
        @Override
        public void onResult(LocationSettingsResult result) {
            final Status status = result.getStatus();
            final LocationSettingsStates state = result.getLocationSettingsStates();
            switch (status.getStatusCode()) {
                case LocationSettingsStatusCodes.SUCCESS:
                    Log.e(TAG, "LocationSettingsStatusCodes.SUCCESS");
                    break;
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    Log.e(TAG, "LocationSettingsStatusCodes.RESOLUTION_REQUIRED");
                    try {
                        status.startResolutionForResult(MainActivity.this, GPS_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException e) {
                    }
                    break;
                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    Log.e(TAG, "LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE");
                    break;
            }
        }
    };
}
