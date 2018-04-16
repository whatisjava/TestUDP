package com.thxjava.testudp;

import java.util.Arrays;

public class HeartBeat {

    private int code;

    private byte[] data;

    private String deviceIp;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getDeviceIp() {
        return deviceIp;
    }

    public void setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
    }

    @Override
    public String toString() {
        return "HeartBeat{" +
                "code=" + code +
                ", data=" + Arrays.toString(data) +
                ", deviceIp='" + deviceIp + '\'' +
                '}';
    }
}
