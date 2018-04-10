package com.thxjava.testudp;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private TextView textView;

    private DatagramSocket socket;
    private DatagramPacket send_packet, receive_packet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.text_view);

        /*
         * 向服务器端发送数据
         */
        new UDPIMTask().execute();

    }

    class UDPIMTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // 1.定义服务器的地址、端口号、发送数据
                InetAddress address = InetAddress.getByName("192.168.31.1");
                int port = 16611;

                byte[] send_data1 = { 85, 0, 90, 18, 104, 101, 122, 121, 45, 122, 104, 0, 113, 97, 122, 33, 64, 35, 119, 115, 120, 0 };

                System.out.println("sum---" + Integer.parseInt("55", 16));

                byte[] send_data = { 85, 0, 90, 18, 104, 101, 122, 121, 45, 122, 104, 0, 113, 97, 122, 33, 64, 35, 119, 115, 120, 0, (byte) add(send_data1) };

                // 2.创建数据报，包含发送的数据信息
                send_packet = new DatagramPacket(send_data, send_data.length, address, port);

                // 3.创建DatagramSocket对象
                socket = new DatagramSocket(8888);
                socket.setSoTimeout(3000);

                // 4.向服务器端发送数据报
                socket.send(send_packet);

                // 1.创建数据报，用于接收服务器端响应的数据
                byte[] receive_data = new byte[1024];
                receive_packet = new DatagramPacket(receive_data, receive_data.length);

                // 2.接收服务器响应的数据
                System.out.println("等待接收数据");
                socket.receive(receive_packet);
                System.out.println("接收到数据了");

                // 3.读取数据
                String reply = new String(receive_data, 0, receive_packet.getLength());
                System.out.println("我是客户端，服务器说：" + reply);

                byte[] reset = {0x55, 0x00, 0x5C, 0x00, (byte) 0xB1};
                send_packet = new DatagramPacket(reset, reset.length, address, port);

                socket.send(send_packet);

                return reply;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("2222222222222222222");
            }
            return null;
        }

        private int add(byte[] bytes){
            int sum = 0;
            for (int i=0;i<bytes.length;i++) {
                sum = sum + bytes[i];
            }
            return sum;
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(MainActivity.this, "返回结果" + s, Toast.LENGTH_SHORT).show();
            if (TextUtils.isEmpty(s)){
                textView.setText("三秒后重新发送");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {

                }
                Toast.makeText(MainActivity.this, "三秒后重新发送", Toast.LENGTH_SHORT).show();
                new UDPIMTask().execute();
            } else {
                textView.setText("我是客户端，服务器说：" + s);
            }
            // 4.关闭资源
            socket.close();
        }
    }

    public byte[] toHexString(String s) {
        byte[] result = new byte[s.length()];
        for (int i=0;i<s.length();i++) {
            int ch = (int)s.charAt(i);
//            String s4 = Integer.toHexString(ch);
            result[i] = ((byte) ch);

        }
        return result;
    }

//    public byte[] produces(String input){
//
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null)
            socket.close();
    }
}
