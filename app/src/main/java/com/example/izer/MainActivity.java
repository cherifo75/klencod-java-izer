package com.example.izer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private WifiManager wifiManager;
    private ListView listView;
    private Button btnScan;
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        btnScan = new Button(this);
        btnScan.setText("البحث عن شبكات الواي فاي");
        layout.addView(btnScan);

        listView = new ListView(this);
        layout.addView(listView);

        setContentView(layout);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        btnScan.setOnClickListener(v -> checkPermissionsAndScan());
    }

    private void checkPermissionsAndScan() {
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "يرجى تفعيل الواي فاي أولاً", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                listPermissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            startScan();
        }
    }

    private void startScan() {
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        boolean success = wifiManager.startScan();
        if (success) {
            Toast.makeText(this, "جاري البحث عن الشبكات...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "فشل بدء المسح", Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                scanSuccess();
            } else {
                scanFailure();
            }
        }
    };

    private void scanSuccess() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        List<ScanResult> results = wifiManager.getScanResults();
        arrayList.clear();
        for (ScanResult scanResult : results) {
            String ssid = scanResult.SSID;
            if (ssid != null && !ssid.isEmpty()) {
                arrayList.add(ssid + " (" + scanResult.level + " dBm)");
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void scanFailure() {
        Toast.makeText(this, "تعذر تحديث نتائج البحث", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(wifiReceiver);
        } catch (IllegalArgumentException e) {
            // استثناء تجنب الخطأ في حال لم يتم التسجيل
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan();
            } else {
                Toast.makeText(this, "يلزم الحصول على إذن الموقع للبحث عن شبكات الواي فاي", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
