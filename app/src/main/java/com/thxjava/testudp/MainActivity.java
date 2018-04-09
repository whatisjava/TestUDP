package com.thxjava.testudp;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

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
                // 1.定义服务器的地址、端口号、数据
                InetAddress address = InetAddress.getByName("192.168.31.1");
                int port = 16611;
//                byte[] send_data = "55 00 5A 12 68 65 7A 79 2D 7A 68 71 61 7A 00 21 40 23 77 73 78 00 C2".getBytes();
                byte[] send_data = "55 00 00 00 55".getBytes();

                // 2.创建数据报，包含发送的数据信息
                send_packet = new DatagramPacket(send_data, send_data.length, address, port);

                // 3.创建DatagramSocket对象
                socket = new DatagramSocket();

                // 4.向服务器端发送数据报
                socket.send(send_packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {

            /*
             * 接收服务器端响应的数据
             */
            new UDPIMTask1().execute();

        }
    }

    class UDPIMTask1 extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... voids) {
            // 1.创建数据报，用于接收服务器端响应的数据
            byte[] receive_data = new byte[1024];
            receive_packet = new DatagramPacket(receive_data, receive_data.length);

            try {
                // 2.接收服务器响应的数据
                socket.receive(receive_packet);

                // 3.读取数据
                String reply = new String(receive_data, 0, receive_packet.getLength());
                System.out.println("我是客户端，服务器说：" + reply);

                return reply;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            textView.setText("我是客户端，服务器说：" + s);
            // 4.关闭资源
            socket.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
