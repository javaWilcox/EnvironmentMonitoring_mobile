package com.jsut.qjy;

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
import android.view.View;
import android.widget.Button;
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
import java.net.ServerSocket;
import java.net.Socket;

public class WifiActivity extends AppCompatActivity {
    final private int UDP_RECEIVE_BUFFER_LENGTH = 1024;
    final private int SENSOR_FRAME_LENGTH = 13;
    final private String TDS_LINE_COLOR = "#0000FF";
    final private String TEMP_LINE_COLOR = "#00FF00";
    final private String HUM_LINE_COLOR = "#FF0000";
    final private String LIGHT_LINE_COLOR = "#00FFFF";

    private int[] frameBuffer = new int[SENSOR_FRAME_LENGTH];
    private int frameByteCount = 0;

    private LineChart tdsChart;
    private LineChart temperatureChart;
    private LineChart humidityChart;
    private LineChart lightChart;

    private TextView tvTDS;
    private TextView tvTemperature;
    private TextView tvHumidity;
    private TextView tvLight;

    private NetConnection netConnection;

    private Handler handler;

    private DatagramSocket udpSocket;

    private ServerSocket tcpServerSocket;
    private Socket mcuTcpSocket;
    private InputStream mcuTcpInputStream;

    private Socket pcTcpSocket;
    private OutputStream pcTcpOutputStream;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUI();
        initChart(tdsChart);
        initChart(temperatureChart);
        initChart(humidityChart);
        initChart(lightChart);

        Intent intent = getIntent();
        netConnection = new NetConnection();
        netConnection.setPcIp(intent.getStringExtra("PC_IP"));
        netConnection.setPcPort(intent.getStringExtra("PC_PORT"));
        netConnection.setMcuIp(intent.getStringExtra("MCU_IP"));
        netConnection.setMcuPort(intent.getStringExtra("MCU_PORT"));
        netConnection.setProtocol(intent.getStringExtra("PROTOCOL"));

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
                        Toast.makeText(WifiActivity.this, (String) (msg.obj), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        if (netConnection.getProtocol().equals("UDP")) {
            udpStart(netConnection);
        } else {
            tcpServerStart(netConnection);
            tcpStart(netConnection);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //关单片机TCP输入流
        try {
            mcuTcpInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //关电脑TCP输出流
        try {
            pcTcpOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //关UDP套接字
        try {
            udpSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //关单片机TCP套接字
        try {
            mcuTcpSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //关电脑TCP套接字
        try {
            pcTcpSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //关TCP服务器套接字
        try {
            tcpServerSocket.close();
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
        setContentView(R.layout.activity_wifi);

        setTitle("WIFI节点");

        tvTDS = findViewById(R.id.tvTDS);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvLight = findViewById(R.id.tvLight);
        tdsChart = findViewById(R.id.tdsChart);
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

    private void updateSensorUI(SensorInfo info) {
        tvTDS.setText(info.getTdsOrTurb() + "");
        tvTemperature.setText(info.getTemperature() + "");
        tvHumidity.setText(info.getHumidity() + "");
        tvLight.setText(info.getLight() + "");
    }

    private void udpStart(NetConnection net) {
        //建立与MCU的UDP连接
        try {
            final InetAddress mcuAddress = InetAddress.getByName(net.getMcuIp());
            final int mcuPort = Integer.parseInt(net.getMcuPort());
            int localPort = Integer.parseInt(net.getLocalPort());
            udpSocket = new DatagramSocket(localPort);
            //创建与单片机的UDP接收线程
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[UDP_RECEIVE_BUFFER_LENGTH];
                    while (true) {
                        try {
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, mcuAddress, mcuPort);
                            udpSocket.receive(packet);

                            for (int i = 0; i < buffer.length; i++) {
                                netDataReceived(buffer[i]);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        } catch (Exception e) {
            sendHandlerMessage(1, "与单片机建立UDP连接失败！");
            e.printStackTrace();
        }
    }

    private void tcpServerStart(NetConnection net) {
        //创建TCP服务器
        try {
            int localPort = Integer.parseInt(net.getLocalPort());
            tcpServerSocket = new ServerSocket(localPort);
            sendHandlerMessage(1, "建立TCP服务器成功！");
        } catch (Exception e) {
            sendHandlerMessage(1, "建立TCP服务器失败！");
            e.printStackTrace();
        }
    }

    private void tcpStart(NetConnection net) {
        if (tcpServerSocket == null) {
            return;
        }
        //与单片机建立TCP连接
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mcuTcpSocket = tcpServerSocket.accept();
                    sendHandlerMessage(1, "与单片机建立TCP连接成功！");
                    //获取单片机输入流
                    mcuTcpInputStream = mcuTcpSocket.getInputStream();
                    //TCP接收线程
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int a = -1;
                            while (true) {
                                try {
                                    do {
                                        a = mcuTcpInputStream.read();
                                    } while (a == -1);
                                    netDataReceived((byte) (a & 0xFF));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

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

            int tds = frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 11) % SENSOR_FRAME_LENGTH] * 256 + frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 10) % SENSOR_FRAME_LENGTH];
            int temp = frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 9) % SENSOR_FRAME_LENGTH] * 256 + frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 8) % SENSOR_FRAME_LENGTH];
            int hum = frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 7) % SENSOR_FRAME_LENGTH] * 256 + frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 6) % SENSOR_FRAME_LENGTH];
            int light = frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 5) % SENSOR_FRAME_LENGTH] * 256 + frameBuffer[(frameByteCount + SENSOR_FRAME_LENGTH - 4) % SENSOR_FRAME_LENGTH];

            SensorInfo info = new SensorInfo(tds, temp, hum, light);
            sendHandlerMessage(0, info);
            String protocol = netConnection.getProtocol();
            if (protocol.equals("UDP")) {
                udpSendData(netConnection, info.getFrame(1));
            } else if (protocol.equals("TCP")) {
                tcpSendData(netConnection, info.getFrame(1));
            }
            addEntry(tdsChart, "水质", TDS_LINE_COLOR, tds);
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
        try {
            pcTcpOutputStream.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
