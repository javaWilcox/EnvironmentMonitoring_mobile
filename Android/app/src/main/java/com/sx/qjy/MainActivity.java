package com.jsut.qjy;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private EditText edtPcIp;
    private EditText edtPcPort;
    private EditText edtMcuIp;
    private EditText edtMcuPort;
    private Button btnWifi;
    private Button btnBluetooth;
    private Button btnExit;
    private RadioButton rbTcp;
    private RadioButton rbUdp;
    private Spinner spBluetoothDevice;

    private String pcIp;
    private String pcPort;
    private String mcuIP;
    private String mcuPort;
    private String protocol;
    private String bluetoothName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUI();

        addBluetoothDevices();

        btnWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, WifiActivity.class);

                pcIp = edtPcIp.getText().toString().trim();
                pcPort = edtPcPort.getText().toString().trim();
                mcuIP = edtMcuIp.getText().toString().trim();
                mcuPort = edtMcuPort.getText().toString().trim();

                if (rbTcp.isChecked()) {
                    protocol = "TCP";
                } else if (rbUdp.isChecked()) {
                    protocol = "UDP";
                }

                Bundle bundle = new Bundle();
                bundle.putString("PC_IP", pcIp);
                bundle.putString("PC_PORT", pcPort);
                bundle.putString("MCU_IP", mcuIP);
                bundle.putString("MCU_PORT", mcuPort);
                bundle.putString("PROTOCOL", protocol);

                intent.putExtras(bundle);

                startActivity(intent);
            }
        });

        btnBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, BluetoothActivity.class);

                pcIp = edtPcIp.getText().toString().trim();
                pcPort = edtPcPort.getText().toString().trim();
                mcuIP = edtMcuIp.getText().toString().trim();
                mcuPort = edtMcuPort.getText().toString().trim();
                bluetoothName=spBluetoothDevice.getSelectedItem().toString();

                if (rbTcp.isChecked()) {
                    protocol = "TCP";
                } else if (rbUdp.isChecked()) {
                    protocol = "UDP";
                }

                Bundle bundle = new Bundle();
                bundle.putString("PC_IP", pcIp);
                bundle.putString("PC_PORT", pcPort);
                bundle.putString("PROTOCOL", protocol);
                bundle.putString("BT_NAME", bluetoothName);

                intent.putExtras(bundle);

                startActivity(intent);
            }
        });

        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.exit(0);
            }
        });
    }

    private void initUI() {
        setContentView(R.layout.activity_main);

        edtPcIp = findViewById(R.id.edtPcIp);
        edtPcPort = findViewById(R.id.edtPcPort);
        edtMcuIp = findViewById(R.id.edtMcuIp);
        edtMcuPort = findViewById(R.id.edtMcuPort);
        btnWifi = findViewById(R.id.btnWifi);
        btnBluetooth = findViewById(R.id.btnBluetooth);
        btnExit=findViewById(R.id.btnExit);
        rbTcp = findViewById(R.id.rbTcp);
        rbUdp = findViewById(R.id.rbUdp);
        spBluetoothDevice = findViewById(R.id.spBtDevices);
    }

    private void addBluetoothDevices() {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        ArrayList<String> devices=new ArrayList<>();
        for(BluetoothDevice device : pairedDevices){
            devices.add(device.getName().trim());
        }
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,devices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBluetoothDevice.setAdapter(adapter);
    }
}
