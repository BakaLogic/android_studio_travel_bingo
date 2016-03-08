package com.psripinyo.travelbingo;

import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by petersripinyo on 3/2/16.
 */
public class ReceiveAndParsePacketsTask extends AsyncTask<Object, Void, Long> {

    private final String TAG = "ParsePacketsTask";
    private final int BUFFERSIZE = 128;
    private TravelBingoWifiManager tbWiFiManager;
    InputStream incomingStream;
    ServerSocket serverSocket;
    Socket client;

    @Override
    protected Long doInBackground(Object... obj) {
        if(obj.length >= 1 && obj[0] instanceof TravelBingoWifiManager) {
            tbWiFiManager = (TravelBingoWifiManager) obj[0];
        }

        // we don't have a manager to send packets to so we have no purpose in this life.
        if (tbWiFiManager == null)
            return -1L;

        try {
            serverSocket = new ServerSocket(8888);
            serverSocket.setReuseAddress(true);
        }
        catch(IOException e) {
            Log.d(TAG, "Could not open server socket.");
            return -1L;

        }


        do {
            try {
                client = serverSocket.accept();
            }
            catch(IOException e) {
                Log.d(TAG, "Could not open client socket.");
                closeServerSocket();
            }

            try {
                // we got something.  Let's parse it into a packet and pas it on.
                incomingStream = client.getInputStream();
            }
            catch(IOException e) {
                Log.d(TAG, "ERROR: Couldn't open client inputsream.");
            }

            byte[] buffer = new byte[BUFFERSIZE];
            int bytesRead = 0;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try {
                while ((bytesRead = incomingStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            catch(IOException e) {
                Log.d(TAG, "ERROR: Problem reading from the incoming stream");
            }

            try {
                tbWiFiManager.parsePacketInfo(outputStream.toByteArray());
                outputStream.close();
                tbWiFiManager.setLastContactAddress(client.getInetAddress());
            }
            catch(IOException e) {
                // had trouble closing the stream.
            }

        } while(!isCancelled());

        closeServerSocket();
        return 0L;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        closeServerSocket();
    }

    private void closeServerSocket() {
        try {
            if(client != null)
                client.close();
            if(serverSocket != null)
                serverSocket.close();
        } catch (IOException f) {
            // couldn't close the server socket, who cares now.
        }
    }
}
