package com.psripinyo.travelbingo;

import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.SystemClock;

/**
 * Created by petersripinyo on 3/1/16.
 */
public class CallDiscoverPeersTask extends AsyncTask<Object, Void, Long> {

    private final int DELAYUNTILNEXTPEERDISCOVERYCALL = 10000;

    @Override
    protected Long doInBackground(Object... obj) {
        WifiP2pManager wifiManager = null;
        WifiP2pManager.Channel wifiChannel = null;
        WifiP2pManager.ActionListener wifiActionListener = null;

        if(obj.length >= 1 && obj[0] instanceof WifiP2pManager) {
            wifiManager = (WifiP2pManager) obj[0];
        }
        if(obj.length >= 2 && obj[1] instanceof WifiP2pManager.Channel) {
            wifiChannel = (WifiP2pManager.Channel) obj[1];
        }
        if(obj.length >= 3 && obj[2] instanceof WifiP2pManager.ActionListener) {
            wifiActionListener = (WifiP2pManager.ActionListener) obj[2];
        }

        while (wifiActionListener != null && wifiChannel != null && wifiManager != null &&
                this.isCancelled() != true) {
            wifiManager.discoverPeers(wifiChannel, wifiActionListener);
            SystemClock.sleep(DELAYUNTILNEXTPEERDISCOVERYCALL);
        }
        return 0L;
    }

}
