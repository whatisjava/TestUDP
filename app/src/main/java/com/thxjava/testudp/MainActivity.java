package com.thxjava.testudp;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private TextView textView, dianText;

    private DatagramSocket socket;
    private DatagramPacket send_packet, receive_packet;

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 7:
                    byte[] m7 = reverseByteArray((byte[]) msg.obj);
                    System.out.println("msg 7 ----> " + Arrays.toString(m7));

                    dianText.setText("电量：" + "mV");
                    break;
                case 50:
                    byte[] m50 = (byte[]) msg.obj;
                    String msg50 = new String(m50, 0, m50.length);
                    System.out.println("msg 50 ----> " + msg50);
                    textView.setText(msg50);
                    break;
            }
        }
    };

    private byte[] reverseByteArray(byte[] bytes){
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[bytes.length -1 - i];
        }
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.text_view);
        dianText = findViewById(R.id.dian_liang);

        /*
         * 接收广播数据
         */
//        new BroadcastReceiverTask().execute();
        new BroadcastReceiverThread().start();

        /*
         * 接收点对点数据
         */
//        new P2PReceiverTask().execute();

        /*
         * 向服务器端发送数据
         */
        new SenderTask().execute();

    }

    class SenderTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // 1.定义服务器的地址、端口号、发送数据
                InetAddress address = InetAddress.getByName("192.168.31.1");
                int port = 16611;

                byte[] send_data1 = {85, 0, 90, 18, 104, 101, 122, 121, 45, 122, 104, 0, 113, 97, 122, 33, 64, 35, 119, 115, 120, 0};
                System.out.println("sum---" + Integer.parseInt("55", 16));
                byte[] send_data = {85, 0, 90, 18, 104, 101, 122, 121, 45, 122, 104, 0, 113, 97, 122, 33, 64, 35, 119, 115, 120, 0, (byte) sum(send_data1)};

                // 2.创建数据报，包含发送的数据信息
                send_packet = new DatagramPacket(send_data, send_data.length, address, port);

                // 3.创建DatagramSocket对象
                socket = new DatagramSocket(8888);
                socket.setSoTimeout(3000);

                // 4.向服务器端发送数据报
                socket.send(send_packet);

                byte[] reset = {0x55, 0x00, 0x5C, 0x00, (byte) 0xB1};
                send_packet = new DatagramPacket(reset, reset.length, address, port);
                socket.send(send_packet);




                return "success";
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(MainActivity.this, "返回结果" + s, Toast.LENGTH_SHORT).show();
            if (TextUtils.isEmpty(s)) {
                textView.setText("三秒后重新发送");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {

                }
                Toast.makeText(MainActivity.this, "三秒后重新发送", Toast.LENGTH_SHORT).show();
                new SenderTask().execute();
            } else {
                textView.setText("我是客户端，服务器说：" + s);
            }
            // 4.关闭资源
            socket.close();
        }
    }

    class P2PReceiverTask extends AsyncTask<String, Integer, String>{

        private DatagramSocket socket;

        @Override
        protected String doInBackground(String... strings) {
            try {
                socket = new DatagramSocket();

                // 1.创建数据报，用于接收服务器端响应的数据
                byte[] receive_data = new byte[1024];
                receive_packet = new DatagramPacket(receive_data, receive_data.length);

                // 2.接收服务器响应的数据
                System.out.println("等待接收点对点数据");
                socket.receive(receive_packet);
                System.out.println("接收到点对点数据了");
                // 3.读取数据
                String message = new String(receive_data, 0, receive_packet.getLength());
                System.out.println("点对点信息：" + message);
                return message;
            } catch (SocketException e) {
                e.printStackTrace();
                return e.getLocalizedMessage();
            } catch (IOException e) {
                e.printStackTrace();
                return e.getLocalizedMessage();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            textView.setText(s);
            new P2PReceiverTask().execute();
        }
    }

    class BroadcastReceiverThread extends Thread {
        @Override
        public void run() {
            try {
                DatagramChannel channel = DatagramChannel.open();
                socket = channel.socket();
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(16610));

                // 1.创建数据报，用于接收服务器端响应的数据
                byte[] receive_data = new byte[100];
                receive_packet = new DatagramPacket(receive_data, receive_data.length);

                while (true) {
                    // 2.接收服务器响应的数据
                    socket.receive(receive_packet);

                    Message message = new Message();
                    if (receive_data[2] == 7) {
                        message.what = 7;
                    } else if(receive_data[2] == 50){
                        message.what = 50;
                    }
                    // 3.读取数据
                    byte[] msg = new byte[receive_data[3]];
                    System.arraycopy(receive_data, 4, msg, 0, receive_data[3]);
                    message.obj = msg;
                    handler.sendMessage(message);
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class BroadcastReceiverTask extends AsyncTask<String, Integer, String>{

        private DatagramSocket socket;

        @Override
        protected String doInBackground(String... strings) {
            try {
                DatagramChannel channel = DatagramChannel.open();
                socket = channel.socket();
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(16610));

                // 1.创建数据报，用于接收服务器端响应的数据
                byte[] receive_data = new byte[77];
                receive_packet = new DatagramPacket(receive_data, receive_data.length);

                // 2.接收服务器响应的数据
                System.out.println("等待接收广播数据");
                socket.receive(receive_packet);
                System.out.println("接收到广播数据了");
                // 3.读取数据
                String response = new String(receive_data, 0, receive_packet.getLength());
                System.out.println("广播信息：" + response);
                return response;
            } catch (SocketException e) {
                e.printStackTrace();
                return e.getLocalizedMessage();
            } catch (IOException e) {
                e.printStackTrace();
                return e.getLocalizedMessage();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            textView.setText(s);
            new BroadcastReceiverTask().execute();
        }
    }

    private int sum(byte[] bytes) {
        int sum = 0;
        for (int i = 0; i < bytes.length; i++) {
            sum = sum + bytes[i];
        }
        return sum;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null)
            socket.close();
    }
}
