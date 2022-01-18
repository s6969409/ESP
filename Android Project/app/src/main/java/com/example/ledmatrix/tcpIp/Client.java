package com.example.ledmatrix.tcpIp;

import android.os.Handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import javax.net.SocketFactory;

public class Client {
    private Socket socket;
    private ConnectionCreateThread connectionCreateThread;
    private ReadWriteThread readWriteThread;

    private OnUpdateListener onUpdateListener;
    private Handler UIHandler;

    public static final String CONNECT_SUCCESSFUL = "---ConnectSuccessful---";
    public static final String CONNECT_FAILED = "---ConnectFalid---";
    public static final String READ_TITTLE = "Server: ";
    public static final String WRITE_TITTLE = "Write: ";
    public static final String CLIENT_COMMAND = "ClientCommand";

    public Client(String serverIp,int serverPort,OnUpdateListener onUpdateListener,Handler handler) {
        this.onUpdateListener = onUpdateListener;
        this.UIHandler = handler;

        connectionCreateThread = new ConnectionCreateThread(serverIp, serverPort);
    }

    public void connectToServer(){
        connectionCreateThread.start();
    }

    public void close(String message){
        try {
            if(socket!=null) {
                socket.close();
                socket=null;
                //filter message
                if(!message.equals(CLIENT_COMMAND)) {
                    updateConnectionMsgInfo(message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            updateConnectionMsgInfo(message + "--->Faild");
        }
    }

    public void writeToServer(String data){
        if (isClosed()){
            connectToServer();
        }
        new WriteThread(data).start();
    }

    public boolean isClosed(){
        return socket==null;
    }

    private class WriteThread extends Thread{
        private String data;

        private WriteThread(String data) {
            this.data = data;
        }

        @Override
        public void run() {
            readWriteThread.write(data);
        }
    }

    private void updateConnectionMsgInfo(final String message){
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                onUpdateListener.OnUpdateConnectionMsgInfo(message);
            }
        });
    }

    private void updateServerSendData(final String message){
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                onUpdateListener.OnServerSendData(message);
            }
        });
    }

    public interface OnUpdateListener {
        void OnUpdateConnectionMsgInfo(String message);
        void OnServerSendData(String message);
    }

    private class ConnectionCreateThread extends Thread {
        private String ip;
        private int port;

        private ConnectionCreateThread(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void run() {
            socket = null;
            try {
                socket = SocketFactory.getDefault().createSocket();
                SocketAddress remoteaddr = new InetSocketAddress(ip, port);
                socket.connect(remoteaddr, 3000);

                readWriteThread = new ReadWriteThread(socket);
                readWriteThread.start();
                updateConnectionMsgInfo(CONNECT_SUCCESSFUL);
            } catch (IOException e) {
                e.printStackTrace();
                close(CONNECT_FAILED);
            }
        }
    }

    private class ReadWriteThread extends Thread{
        private Socket clientSocket;
        private BufferedReader input;
        private OutputStream outputStream;

        private ReadWriteThread(Socket clientSocket){
            this.clientSocket = clientSocket;

            try {
                input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
                outputStream = clientSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                updateConnectionMsgInfo("---BufferedReaderError---");
            }
        }

        private void write(String message){
            try {
                outputStream.write((message+"\n").getBytes());//socket.getLocalAddress().getHostAddress()+": "+
                updateConnectionMsgInfo(WRITE_TITTLE + message);
            } catch (IOException e) {
                e.printStackTrace();
                updateConnectionMsgInfo("---WriteFaild---");
            }
        }

        @Override
        public void run() {
            String read;
            while (!clientSocket.isClosed()){
                try {
                    read = input.readLine();
                    if(read != null){
                        //readData
                        updateServerSendData(READ_TITTLE + read);
                    }else {
                        close("---"+socket.getInetAddress().getHostAddress()+":"+socket.getPort()+" closed---");
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    close("??");
                    return;
                }
            }
        }
    }
}
