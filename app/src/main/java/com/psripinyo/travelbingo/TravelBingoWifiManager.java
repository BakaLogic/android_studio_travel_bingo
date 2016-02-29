package com.psripinyo.travelbingo;

import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by psripinyo on 2/26/2016.
 */
public class TravelBingoWifiManager {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WiFiDirectBroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;
    private TravelBingo mActivity;
    private List<WifiP2pDevice> peers; // a list of currently available peers
    //private List<PeerInfo> peerInfoList;  // This will be a a list of devices and IP addresses in an object
    private List peersIPAddresses;
    private WifiP2pManager.PeerListListener peerListListener;
    private static final String TAG = "TravelBingoWifiManager";
    private static final String APPID = "TB";
    private boolean isWiFiDirectSupported;

    private enum PacketType { requestGameBoard, // Host requests a game board from player
                              gameBoardUpdate,  // This packet contains a game board
                              markedTileUpdate, // this packet contains a marked tile update
                              requestCompleteUpdate, // request a full update
                              completeUpdate, // a complete update of game board and marked tiles
                              tileMarkNotification, // Client is updating host about a marked tile
                              iWin,   // packet notifying host and others that a player won
                              forceTileToggle, // host forces a player to toggle a game tile
                              connectionAck // client to server ack for determining IPAddress.
                            };

    public TravelBingoWifiManager(TravelBingo activity) {
        mManager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(activity, activity.getMainLooper(), null);

        // P2p is not enabled on this device.
        if(mChannel == null)
            return;

        isWiFiDirectSupported = checkForWiFiSupport(activity);

        mActivity = activity;

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        //peerInfo = new ArrayList<WifiP2pDevice>();
        peers = new ArrayList<WifiP2pDevice>();

        peerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {

                // Out with the old, in with the new.
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                // If an AdapterView is backed by this data, notify it
                // of the change.  For instance, if you have a ListView of available
                // peers, trigger an update.
                //((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
                if (peers.size() == 0) {
                    Log.d(TAG, "No devices found");
                    return;
                }
            }
        };

        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, activity, peerListListener);
    }

    private boolean checkForWiFiSupport(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        FeatureInfo[] features = pm.getSystemAvailableFeatures();
        for (FeatureInfo info : features) {
            if (info != null && info.name != null &&
                    info.name.equalsIgnoreCase("android.hardware.wifi.direct")) {
                return true;
            }
        }
        return false;
    }

    public boolean isWifiDirectSupported() {
        return isWiFiDirectSupported;
    }

    public WiFiDirectBroadcastReceiver getMyReceiver() {
        return mReceiver;
    }

    public IntentFilter getMyIntentFilter() {
        return mIntentFilter;
    }

    public void lookForPeers() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Code for when the discovery initiation is successful goes here.
                // No services have actually been discovered yet, so this method
                // can often be left blank.  Code for peer discovery goes in the
                // onReceive method, detailed below.
                int i = 1;
                List temp = peers;
            }

            @Override
            public void onFailure(int reasonCode) {
                // Code for when the discovery initiation fails goes here.
                // Alert the user that something went wrong.
                int i = 1;
                List temp = peers;
            }
        });
    }

    public WifiP2pManager.PeerListListener getPeerListListener() {
        return peerListListener;
    }

    public final List getPeers() {

        //DebugCode -- faking a peer list of three.
        if(peers.size() == 0)
        {
            WifiP2pDevice fakeDevice = new WifiP2pDevice();
            fakeDevice.deviceName = "Hannah";
            fakeDevice.deviceAddress = "Hannah's MAC Address";
            fakeDevice.status = WifiP2pDevice.UNAVAILABLE;

            peers.add(fakeDevice);

            fakeDevice = new WifiP2pDevice();
            fakeDevice.deviceName = "Isaac";
            fakeDevice.deviceAddress = "Isaac's MAC Address";
            fakeDevice.status = WifiP2pDevice.CONNECTED;

            peers.add(fakeDevice);

            fakeDevice = new WifiP2pDevice();
            fakeDevice.deviceName = "Lacie";
            fakeDevice.deviceAddress = "Lacie's MAC Address";
            fakeDevice.status = WifiP2pDevice.CONNECTED;

            peers.add(fakeDevice);

        }

        // if the peers list is empty, then we're going to return null to indicate we need
        // to try and initiate a search for peers.
        if(peers.size() == 0)
            return null;
        else
            return peers;
    }
}
