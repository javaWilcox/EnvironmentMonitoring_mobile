package com.jsut.qjy;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity {
    final private int SENSOR_FRAME_LENGTH = 13;
    final private String TURB_LINE_COLOR = "#0000FF";
    final private String TEMP_LINE_COLOR = "#00FF00";
    final private String HUM_LINE_COLOR = "#FF0000";
    final private String LIGHT_LINE_COLOR = "#00FFFF";

    private int[] frameBuffer = new int[SENSOR_FRAME_LENGTH];
    private int frameByteCount = 0;

    private TextView tvTurbidity;
    private TextView tvTemperature;
    private TextView tvHumidity;
    private TextView tvLight;

    private LineChart turbidityChart;
    private LineChart temperatureChart;
    private LineChart humidityChart;
    private LineChart lightChart;

    private Handler handler;

    private NetConnection netConnection;

    private DatagramSocket udpSocket;

    private Socket pcTcpSocket;
    private OutputStream pcTcpOutputStream;

    private BluetoothSocket mcuBluetoothSocket;
    private InputStream mcuBluetoothInputStream;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        initUI();
        initChart(turbidityChart);
        initChart(temperatureChart);
        initChart(humidityChart);
        initChart(lightChart);

        Intent intent = getIntent();
        netConnection = new NetConnection();
        netConnection.setPcIp(intent.getStringExtra("PC_IP"));
        netConnection.setPcPort(intent.getStringExtra("PC_PORT"));
        netConnection.setProtocol(intent.getStringExtra("PROTOCOL"));
        netConnection.setBluetoothDeviceName(intent.getStringExtra("BT_NAME"));

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0:
                        updateSensorUI((SensorInfo) (msg.obj));
                        Log.d("Handler", "传感器信息已更新");
                        break;
                    case 1:
                        Toast.makeText(BluetoothActivity.this, (String) (msg.obj), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        if (netConnection.getProtocol().equals("UDP")) {
            udpStart(netConnection);
        } else {
            tcpStart(netConnection);
        }
        bluetoothStart(netConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //关电脑TCP输出流
        try {
            pcTcpOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //关单片机蓝牙输入流
        try {
            mcuBluetoothInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //关UDP套接字
        try {
            udpSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //关电脑TCP套接字
        try {
            pcTcpSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //关单片机蓝牙套接字
        try {
            mcuBluetoothSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(1, 1, 1, "返回");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initUI() {
        setContentView(R.layout.activity_bluetooth);

        setTitle("蓝牙节点");

        tvTurbidity = findViewById(R.id.tvTurbidity);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvLight = findViewById(R.id.tvLight);
        turbidityChart = findViewById(R.id.turbChart);
        temperatureChart = findViewById(R.id.tempChart);
        humidityChart = findViewById(R.id.humChart);
        lightChart = findViewById(R.id.lightChart);
    }

    private void initChart(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setPinchZoom(true);
        chart.setDrawBorders(true);
        chart.setBorderColor(0xA2A2A2);
        LineData data = new LineData();
        chart.setData(data);
        Legend l = chart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setEnabled(true);

        XAxis xl = chart.getXAxis();
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        xl.setDrawLabels(false);
        xl.setGranularity(1f);
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(true);
        xl.enableGridDashedLine(10f, 10f, 0f);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setGranularity(1f);
        //leftAxis.setTextColor(Color.parseColor("#A2A2A2"));
        leftAxis.setAxisMaximum(100f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.resetAxisMinimum();
        leftAxis.resetAxisMaximum();

        leftAxis.setDrawGridLines(true);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void addEntry(LineChart chart, String name, String color, float number) {

        LineData data = chart.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createLineDataSet(name, color);
                data.addDataSet(set);
            }
            data.addEntry(new Entry(set.getEntryCount(), number), 0);
            data.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.setVisibleXRangeMaximum(30);
            chart.moveViewToX(data.getEntryCount());
        }
    }

    private LineDataSet createLineDataSet(String name, String color) {

        LineDataSet set = new LineDataSet(null, name);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.parseColor(color));
        set.setCircleColor(Color.WHITE);
//        set.setLineWidth(2f);
//        set.setCircleRadius(3f);
        set.setDrawCircles(false);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    private void bluetoothStart(NetConnection net) {
        //建立蓝牙连接
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        String dstDeviceName = net.getBluetoothDeviceName();
        BluetoothDevice dstDevice = null;
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().trim().equals(dstDeviceName.trim())) {
                dstDevice = device;
                break;
            }
        }
        if (dstDevice == null) {
            sendHandlerMessage(1, "未与" + dstDeviceName + "配对！");
            return;
        }
        final BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(dstDevice.getAddress());
        //final BluetoothDevice bluetoothDevice=bluetoothAdapter.getRemoteDevice("00:21:13:03:38:7A");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //连接蓝牙
                    mcuBluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(netConnection.getBluetoothUuid()));
                    bluetoothAdapter.cancelDiscovery();
                    mcuBluetoothSocket.connect();
                    sendHandlerMessage(1, "蓝牙连接建立成功！");
                    mcuBluetoothInputStream = mcuBluetoothSocket.getInputStream();
                    //蓝牙接收线程
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int a = -1;
                            while (true) {
                                try {
                                    do {
                                        a = mcuBluetoothInputStream.read();
                                    } while (a == -1);
                                    netDataReceived((byte) (a & 0xFF));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    sendHandlerMessage(1, "蓝牙连接建立失败！");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void udpStart(NetConnection net) {
        //建立与电脑的UDP连接
        try {
            int localPort = Integer.parseInt(net.getLocalPort());
            udpSocket = new DatagramSocket(localPort);
        } catch (Exception e) {
            sendHandlerMessage(1, "与电脑建立UDP连接失败！");
            e.printStackTrace();
        }
    }

    private void tcpStart(NetConnection net) {
        //与电脑建立TCP连接
        final String pcIp = net.getPcIp();
        final int pcPort = Integer.parseInt(net.getPcPort());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pcTcpSocket = new Socket(pcIp, pcPort);
                    sendHandlerMessage(1, "与电脑建立TCP连接成功！");
                    //获取电脑输出流
                    pcTcpOutputStream = pcTcpSocket.getOutputStream();
                } catch (Exception e) {
                    sendHandlerMessage(1, "与电脑建立TCP连接失败！");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void netDataReceived(byte b) {
        frameBuffer[frameByteCount] = b & 0xFF;
        frameByteCount = (frameByteCount + 1) % SENSOR_FRAME_LENGTH;
        //检测帧头帧尾
        if (frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 13) % SENSOR_FRAME_LENGTH] == 0xA5 &&
                frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 12) % SENSOR_FRAME_LENGTH] == 0xA5 &&
                frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 2) % SENSOR_FRAME_LENGTH] == 0x5A &&
                frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 1) % SENSOR_FRAME_LENGTH] == 0x5A) {
            int check = 0;
            for (int i = 4; i <= 11; i++) {
                check ^= frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - i) % SENSOR_FRAME_LENGTH];
            }
            int correct = frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 3) % SENSOR_FRAME_LENGTH];

            if (check != correct) {
                return;
            }

            int turb = frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 11) % SENSOR_FRAME_LENGTH] * 256 + frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 10) % SENSOR_FRAME_LENGTH];
            int temp = frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 9) % SENSOR_FRAME_LENGTH] * 256 + frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 8) % SENSOR_FRAME_LENGTH];
            int hum = frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 7) % SENSOR_FRAME_LENGTH] * 256 + frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 6) % SENSOR_FRAME_LENGTH];
            int light = frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 5) % SENSOR_FRAME_LENGTH] * 256 + frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 4) % SENSOR_FRAME_LENGTH];

            Log.i("SensorInfo", turb + "-" + temp + "-" + hum + "-" + light);

            SensorInfo info = new SensorInfo(turb, temp, hum, light);
            sendHandlerMessage(0, info);
            String protocol = netConnection.getProtocol();
            if (protocol.equals("UDP")) {
                udpSendData(netConnection, info.getFrame(2));
            } else if (protocol.equals("TCP")) {
                tcpSendData(netConnection, info.getFrame(2));
            }
            addEntry(turbidityChart, "浊度", TURB_LINE_COLOR, turb / 100.0f);
            addEntry(temperatureChart, "温度", TEMP_LINE_COLOR, temp);
            addEntry(humidityChart, "湿度", HUM_LINE_COLOR, hum);
            addEntry(lightChart, "光照", LIGHT_LINE_COLOR, light);
        }
    }

    private void sendHandlerMessage(int what, Object msg) {
        Message message = new Message();
        message.what = what;
        message.obj = msg;
        handler.sendMessage(message);
    }

    private void updateSensorUI(SensorInfo info) {
        tvTurbidity.setText(info.getTdsOrTurb() / 100.0 + "");
        tvTemperature.setText(info.getTemperature() + "");
        tvHumidity.setText(info.getHumidity() + "");
        tvLight.setText(info.getLight() + "");
    }

    private void udpSendData(NetConnection net, byte[] bytes) {
        if (udpSocket == null) {
            return;
        }
        try {
            InetAddress pcAddress = InetAddress.getByName(net.getPcIp());
            int pcPort = Integer.parseInt(net.getPcPort());
            final DatagramPacket packet = new DatagramPacket(bytes, bytes.length, pcAddress, pcPort);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        udpSocket.send(packet);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void tcpSendData(NetConnection net, byte[] bytes) {
        if (pcTcpOutputStream == null) {
            return;
        }
        final byte[] buffer = bytes;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pcTcpOutputStream.write(buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
