package com.psripinyo.travelbingo;

import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by petersripinyo on 3/2/16.
 */
public class SendPacketsTask extends AsyncTask<Object, Void, Long> {

    private final String TAG = "SendPacketsTask";

    @Override
    protected Long doInBackground(Object... obj) {
            String host;
            int port;
            TravelBingoWifiManager tbWifiManager;
        if(obj.length >= 1 && obj[0] instanceof String) {
            host = (String) obj[0];
        }
        else {
            Log.d(TAG,"ERROR: Host address could not be parsed.");
            return -1L;
        }
        if(obj.length >= 2 && obj[1] instanceof Integer) {
            port = ((Integer)obj[1]).intValue();
        }
        else {
            Log.d(TAG, "ERROR: Port number could not be parsed.");
            return -1L;
        }
        if(obj.length >= 3 && obj[2] instanceof TravelBingoWifiManager) {
            tbWifiManager = (TravelBingoWifiManager)obj[2];
        }
        else {
            Log.d(TAG,"ERROR: Could not get tbWifiManager.");
            return -1L;
        }

        int len;
        byte[] buf = new byte[1024];

        try {
            byte[] packetInfo = tbWifiManager.getPacketFromOutgoingQueue();
            while(packetInfo != null) {
                Socket socket = new Socket();
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), 500);
                ByteArrayInputStream packetOut = new ByteArrayInputStream(packetInfo);
                OutputStream outputStream = socket.getOutputStream();
                while((len = packetOut.read(buf)) != -1) {
                    outputStream.write(buf, 0, len);
                }

                outputStream.close();
                packetOut.close();

                if(socket!= null && socket.isConnected()) {
                    socket.close();
                }
                packetInfo = tbWifiManager.getPacketFromOutgoingQueue();
            };
        }
        catch(IOException e) {
            Log.d(TAG, e.getMessage());
        }

        return 1L;
    }
}
