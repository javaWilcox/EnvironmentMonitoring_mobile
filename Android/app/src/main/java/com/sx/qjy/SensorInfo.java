package com.jsut.qjy;

public class SensorInfo {
    private int tdsOrTurb;
    private int temperature;
    private int humidity;
    private int light;


    public SensorInfo(int tsdOrTurb, int temperature, int humidity, int light) {
        this.tdsOrTurb = tsdOrTurb;
        this.temperature = temperature;
        this.humidity = humidity;
        this.light = light;
    }

    public SensorInfo() {
        this(0, 0, 0, 0);
    }

    public int getTdsOrTurb() {
        return tdsOrTurb;
    }

    public void setTdsOrTurb(int tdsOrTurb) {
        this.tdsOrTurb = tdsOrTurb;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public int getLight() {
        return light;
    }

    public void setLight(int light) {
        this.light = light;
    }

    public String getTdsOrTurbString() {
        return tdsOrTurb + "";
    }

    public String getTemperatureString() {
        return temperature + "";
    }

    public String getHumidityString() {
        return humidity + "";
    }

    public String getLightString() {
        return light + "";
    }

    public byte[] getFrame(int id) {
        byte[] frame = new byte[14];

        frame[0] = (byte) 0xA5;
        frame[1] = (byte) 0xA5;
        frame[2] = (byte) (id & 0xFF);
        frame[3] = (byte) ((tdsOrTurb >> 8) & 0xFF);
        frame[4] = (byte) (tdsOrTurb & 0xFF);
        frame[5] = (byte) ((temperature >> 8) & 0xFF);
        frame[6] = (byte) (temperature & 0xFF);
        frame[7] = (byte) ((humidity >> 8) & 0xFF);
        frame[8] = (byte) (humidity & 0xFF);
        frame[9] = (byte) ((light >> 8) & 0xFF);
        frame[10] = (byte) (light & 0xFF);
        //校验
        int check = 0;
        for (int i = 2; i < 11; i++) {
            int a = frame[i];
            a = a & 0xFF;
            check ^= a;
        }
        check &= 0xFF;
        frame[11] = (byte) check;
        frame[12] = (byte) 0x5A;
        frame[13] = (byte) 0x5A;

        return frame;
    }
}
