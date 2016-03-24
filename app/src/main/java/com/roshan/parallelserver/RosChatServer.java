package com.roshan.parallelserver;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Roshan Lasrado on 3/22/2016.
 */
public class RosChatServer {
    static Socket clientSocket = null;
    static ServerSocket serverSocket = null;
    static clientThread t[] = new clientThread[10];
    static int i = 0;
    int port_number = 8888;
    Context context;

    public RosChatServer(int portNum, Context context) {
        port_number = portNum;
        this.context = context;
    }

    public void runCommunicator() {
        try {
            serverSocket = new ServerSocket(port_number);
        } catch (IOException e) {
            final Exception e1 = e;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    HomeActivity.setPromptText(e1.toString());
                }
            });
        }
        HomeActivity.setPromptText("Server running on Port:  " + port_number);
        while (true) {
            try {
                clientSocket = serverSocket.accept();
                t[i] = new clientThread(clientSocket, t, i);
                t[i].start();
                i++;
                HomeActivity.parallelMode = true;
            } catch (IOException e) {
                final Exception e1 = e;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        HomeActivity.setPromptText(e1.toString());
                    }
                });
            }
        }
    }
}

class clientThread extends Thread {
    final static LinkedBlockingQueue<String> inputData = new LinkedBlockingQueue<String>();
    final static BlockingQueue<String> outputData = new LinkedBlockingQueue<>();
    DataInputStream is = null;
    PrintStream os = null;
    Socket clientSocket = null;
    static clientThread t[];
    int i;

    public clientThread(Socket clientSocket, clientThread[] t, int i) {
        this.clientSocket = clientSocket;
        clientThread.t = t;
        this.i = i;
        try {
            is = new DataInputStream(clientSocket.getInputStream());
            os = new PrintStream(clientSocket.getOutputStream());
        } catch (final IOException e) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    HomeActivity.setPromptText(e.toString());
                }
            });
        }
    }

    public void run() {
        final String line;
        String name;
        try {
            os.println("Connecting to Parallel Server.");
            name = is.readLine();
            os.println("Hello. You are now connected to the Parallel net.");
            final String name1 = name;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    HomeActivity.setPromptText("New Client: " + name1 + "\nConnected to the Parallel net.");
                }
            });

            try {
                os.println(inputData.take());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            line = is.readLine();
            outputData.offer(line);
        } catch (final IOException e) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    HomeActivity.setPromptText(e.toString());
                }
            });
        }
    }
}
