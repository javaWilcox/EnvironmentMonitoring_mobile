package com.jsut.qjy;

public class NetConnection {

    private String pcIp;
    private String pcPort;
    private String mcuIp;
    private String mcuPort;
    private String localIp;
    private String localPort;
    private String protocol;
    private String bluetoothDeviceName;
    private String bluetoothUuid;

    public NetConnection(String pcIp, String pcPort, String mcuIp, String mcuPort, String localIp, String localPort, String protocol,String bluetoothDeviceName,String bluetoothUuid) {
        this.pcIp = pcIp;
        this.pcPort = pcPort;
        this.mcuIp = mcuIp;
        this.mcuPort = mcuPort;
        this.localIp = localIp;
        this.localPort = localPort;
        this.protocol = protocol;
        this.bluetoothDeviceName=bluetoothDeviceName;
        this.bluetoothUuid=bluetoothUuid;
    }

    public NetConnection() {
        this("127.0.0.1", "3333", "127.0.0.1", "3333", "127.0.0.1", "3333", "UDP","QJYSWS","00001101-0000-1000-8000-00805B9B34FC");
    }

    public String getPcIp() {
        return pcIp;
    }

    public void setPcIp(String pcIp) {
        this.pcIp = pcIp;
    }

    public String getPcPort() {
        return pcPort;
    }

    public void setPcPort(String pcPort) {
        this.pcPort = pcPort;
    }

    public String getMcuIp() {
        return mcuIp;
    }

    public void setMcuIp(String mcuIp) {
        this.mcuIp = mcuIp;
    }

    public String getMcuPort() {
        return mcuPort;
    }

    public void setMcuPort(String mcuPort) {
        this.mcuPort = mcuPort;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public String getLocalPort() {
        return localPort;
    }

    public void setLocalPort(String localPort) {
        this.localPort = localPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getBluetoothDeviceName() {
        return bluetoothDeviceName;
    }

    public void setBluetoothDeviceName(String bluetoothDeviceName) {
        this.bluetoothDeviceName = bluetoothDeviceName;
    }

    public String getBluetoothUuid() {
        return bluetoothUuid;
    }

    public void setBluetoothUuid(String bluetoothUuid) {
        this.bluetoothUuid = bluetoothUuid;
    }
}
