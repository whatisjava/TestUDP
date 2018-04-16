package com.thxjava.testudp;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;

public class MultiplierService extends Service {

    private static final String TAG = MultiplierService.class.getName();

    private WifiManager wifiManager;

    private DatagramSocket broadcastUdpSocket, p2pUdpSocket;
    private DatagramPacket send_packet, broadcast_receive_packet, p2p_receive_packet;
    private String deviceIp;

    private boolean flag = false;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            HeartBeat heartBeat = (HeartBeat) msg.obj;
            System.out.println("heartBeat ----> " + heartBeat);
            if (heartBeat.getCode() == 50) {
                if (!flag) {
                    new SendCmdTask(heartBeat, 51).execute();
                } else {
                    new SendCmdTask(heartBeat, 81).execute();
                }
            } else if (heartBeat.getCode() == 70) {
                new SendCmdTask(heartBeat, 81).execute();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        new BroadcastReceiverThread().start();

        new P2pReceiverThread().start();

    }

    // 接收广播信息
    class BroadcastReceiverThread extends Thread {
        @Override
        public void run() {
            try {
                DatagramChannel channel = DatagramChannel.open();
                broadcastUdpSocket = channel.socket();
                broadcastUdpSocket.setReuseAddress(true);
                broadcastUdpSocket.bind(new InetSocketAddress(16610));

                // 1.创建数据报，用于接收服务器端响应的数据
                byte[] receive_data = new byte[1024];
                broadcast_receive_packet = new DatagramPacket(receive_data, receive_data.length);

                while (true) {
                    // 2.接收服务器响应的数据
                    Log.d(TAG, "开始接收广播信息");
                    broadcastUdpSocket.receive(broadcast_receive_packet);
                    Log.d(TAG, "接收到广播信息了");

                    deviceIp = broadcast_receive_packet.getAddress().getHostAddress();
                    int devicePort = broadcast_receive_packet.getPort();
                    int length = broadcast_receive_packet.getLength();
                    System.out.println("广播脑电设备IP--->" + deviceIp + "; 端口--->" + devicePort + "; 数据长度--->" + length);

                    if (receive_data[2] != 7) {
                        HeartBeat heartBeat = new HeartBeat();
                        heartBeat.setCode(receive_data[2]);

                        heartBeat.setDeviceIp(deviceIp);

                        byte[] msg = new byte[receive_data[3]];
                        System.arraycopy(receive_data, 4, msg, 0, receive_data[3]);
                        heartBeat.setData(msg);

                        Message message = new Message();
                        message.obj = heartBeat;
                        handler.sendMessage(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 接收广播信息
    class P2pReceiverThread extends Thread {
        @Override
        public void run() {
            try {
                DatagramChannel channel = DatagramChannel.open();
                p2pUdpSocket = channel.socket();
                p2pUdpSocket.setReuseAddress(true);
                p2pUdpSocket.bind(new InetSocketAddress(8888));

                // 1.创建数据报，用于接收服务器端响应的数据
                byte[] receive_data = new byte[100];
                p2p_receive_packet = new DatagramPacket(receive_data, receive_data.length);

                while (true) {
                    // 2.接收服务器响应的数据
                    Log.d(TAG, "开始接收定点信息");
                    p2pUdpSocket.receive(p2p_receive_packet);
                    Log.d(TAG, "接收到定点信息了");


                    HeartBeat heartBeat = new HeartBeat();
                    heartBeat.setCode(receive_data[2]);
                    deviceIp = p2p_receive_packet.getAddress().getHostAddress();
                    System.out.println("定点脑电设备IP--->" + deviceIp);
                    heartBeat.setDeviceIp(deviceIp);

                    byte[] msg = new byte[receive_data[3]];
                    System.arraycopy(receive_data, 4, msg, 0, receive_data[3]);
                    heartBeat.setData(msg);

                    Message message = new Message();
                    message.obj = heartBeat;
                    handler.sendMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class SendCmdTask extends AsyncTask<Integer, Integer, Boolean> {

        private HeartBeat heartBeat;
        private int code;

        public SendCmdTask(HeartBeat heartBeat, int code) {
            this.heartBeat = heartBeat;
            this.code = code;
            System.out.println("=======================" + "开始发送" + code + "=======================");
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            try {
                byte[] send_data = null;

                switch (code) {
                    case 51:
                        String hostIP = intToIp(wifiManager.getDhcpInfo().ipAddress);
                        System.out.println("本机IP--->" + hostIP);
                        int localPort = 8888;

                        send_data = msg51(hostIP, localPort);
                        System.out.println("send 51" + Arrays.toString(send_data));
                        break;
                    case 81:
                        String deviceSN = new String(heartBeat.getData(), 0, heartBeat.getData().length);
                        if (!TextUtils.isEmpty(deviceSN)) {
                            deviceSN = deviceSN.substring(deviceSN.lastIndexOf(":") + 1);
                        }
                        System.out.println("设备序列号--->" + deviceSN);

                        send_data = msg81(deviceSN);
                        System.out.println("send 81" + Arrays.toString(send_data));
                        break;
                }

                // 2.创建数据报，包含发送的数据信息
                send_packet = new DatagramPacket(send_data, send_data.length, InetAddress.getByName(heartBeat.getDeviceIp()), 16611);

                // 3.创建DatagramSocket对象
//                if (params[0] == 51) {
//                    p2pUdpSocket = new DatagramSocket();
//                } else {
//                    p2pUdpSocket = new DatagramSocket(localPort);
//                }

                // 4.向服务器端发送数据报
                p2pUdpSocket.send(send_packet);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                System.out.println("=======================" + code + "发送成功" + "=======================");
                if (code == 51) {
                    flag = true;
                    new SendCmdTask(heartBeat, 81).execute();
                }
            } else {
                System.out.println("=======================" + code + "发送失败" + "=======================");
            }
        }
    }

    private String intToIp(int paramInt) {
        return (paramInt & 0xFF) + "." + (0xFF & paramInt >> 8) + "." + (0xFF & paramInt >> 16) + "." + (0xFF & paramInt >> 24);
    }

    private byte[] msg51(String hostIp, int localPort) {
        byte[] cmd = new byte[11];
        cmd[0] = 0x55;
        cmd[1] = 0x00;
        cmd[2] = 0x33;
        cmd[3] = 0x06;
        int pos1 = hostIp.indexOf(".");
        int pos2 = hostIp.indexOf(".", pos1 + 1);
        int pos3 = hostIp.indexOf(".", pos2 + 1);
        cmd[4] = (byte) 0xC0;
        cmd[5] = (byte) 0xA8;
        cmd[6] = (byte) Integer.parseInt(hostIp.substring(pos2 + 1, pos3));
        cmd[7] = (byte) Integer.parseInt(hostIp.substring(pos3 + 1));
        cmd[8] = 0x22;
        cmd[9] = (byte) 0xb8;
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += cmd[i];
        }
        cmd[10] = (byte) sum;
        return cmd;
    }

    private byte[] msg81(String deviceId) throws UnsupportedEncodingException {
        byte[] cmd = new byte[100];
        cmd[0] = 0x55;
        cmd[1] = 0x00;
        cmd[2] = 0x51;

        byte[] deviceIds = deviceId.getBytes("US-ASCII");
        cmd[3] = (byte) deviceIds.length;

        for (int i = 0; i < deviceIds.length; i++) {
            cmd[4 + i] = deviceIds[i];
        }
        int count = 4 + deviceIds.length;
        byte sum = 0;
        for (int i = 0; i < count; i++) {
            sum += cmd[i];
        }
        cmd[count] = sum;
        count++;
        byte[] data = new byte[count];
        for (int i = 0; i < count; i++) {
            data[i] = cmd[i];
        }
        System.out.println("last data is " + data[count -1] + ",total length is " + data.length);
        return data;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Toast.makeText(this, "service被销毁了", Toast.LENGTH_SHORT).show();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
