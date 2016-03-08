package com.psripinyo.travelbingo;

import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

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
    private ReentrantLock peerListLock, outgoingQueueLock, clientSocketLock;
    WifiP2pManager.ActionListener peerDiscoveryListener;
    CallDiscoverPeersTask discoverPeersTask;
    ReceiveAndParsePacketsTask incomingPacketsTask;
    SendPacketsTask outgoingPacketsTask;
    private HashMap ipAddressHashMap;
    private List<byte[]> outgoingSocketQueue, incomingSocketQueue;
    private WifiP2pManager.ConnectionInfoListener connectInfoListener;
    private String hostAddress;
    private String hostName;
    private InetAddress lastContactAddress;
    private String myWiFiMacAddress;


    private enum PacketType { requestGameBoard, // Host requests a game board from player
                              gameBoardUpdate,  // This packet contains a game board
                              requestCompleteUpdate, // request a full update
                              completeUpdate, // a complete update of game board and marked tiles
                              tileMarkNotification, // Client is updating host about a marked tile
                              iWin,   // packet notifying host and others that a player won
                              forceTileToggle, // host forces a player to toggle a game tile
                              connectionAck // client to server ack for determining IPAddress.
                            };

    private final byte TILEPRESSUPDATEPACKET = 0x01; // this packet contains a tile press update
    private final byte GCBYTILESETANDSEEDPACKET = 0x02; // packet contains gamecard tilset and seed
    private final byte INTRODUCTIONPACKET = 0x03;

    // packet sizes by byte
    private final int TILEPRESSUPDATEPACKETSIZE = 9;    // not including sender information.
    private final int GCBYTILESETANDSEEDPACKETSIZE = 9;
    private final int INRODUCTIONPACKETSIZE = 2; // not including MacAddress information.
                                                 // second byte is 0 for host, 1 for client

    public TravelBingoWifiManager(TravelBingo activity) {
        mManager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(activity, activity.getMainLooper(), null);

        // P2p is not enabled on this device.
        if(mChannel == null)
            return;

        hostAddress = null;

        isWiFiDirectSupported = checkForWiFiSupport(activity);

        mActivity = activity;

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        //HashMap that holds WifiDevice, IpAddress information.
        ipAddressHashMap = new HashMap(20);

        //Lock that protects the peer list
        peerListLock = new ReentrantLock();
        outgoingQueueLock = new ReentrantLock();
        clientSocketLock = new ReentrantLock();

        // queues to hold packets that are outgoing and incoming
        outgoingSocketQueue = new ArrayList<byte[]>();
        incomingSocketQueue = new ArrayList<byte[]>();

        //peerInfo = new ArrayList<WifiP2pDevice>();
        peers = new ArrayList<WifiP2pDevice>();

        peerDiscoveryListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Code for when the discovery initiation is successful goes here.
                // No services have actually been discovered yet, so this method
                // can often be left blank.  Code for peer discovery goes in the
                // onReceive method, detailed below.

            }

            @Override
            public void onFailure(int reasonCode) {
                // Code for when the discovery initiation fails goes here.
                // Alert the user that something went wrong.
                int i = 1;
                Log.d(TAG, "Failure with code " + Integer.toString(reasonCode));
            }
        };

        peerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {
                peerListLock.lock();
                try {
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
                finally {
                    peerListLock.unlock();
                }
            }
        };

        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, activity, peerListListener,
                                                    getConnectInfoListener());

        if(isWiFiDirectSupported) {
            myWiFiMacAddress = getWFDMacAddress();
            lookForPeers();
            mManager.requestConnectionInfo(mChannel, connectInfoListener);
        }
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

    public final WiFiDirectBroadcastReceiver getMyReceiver() {
        return mReceiver;
    }

    public final WifiP2pManager getMManager() {
        return mManager;
    }

    public IntentFilter getMyIntentFilter() {
        return mIntentFilter;
    }

    public final WifiP2pManager.Channel getmChannel () {
        return mChannel;
    }

    public final WifiP2pManager.ActionListener getPeerDiscoveryListener () {
        return peerDiscoveryListener;
    }

    public void setLastContactAddress(InetAddress lastContact) {
        lastContactAddress = lastContact;
    }

    public void killTasks() {
        stopLookingForPeers();
        disconnectFromGroup();
    }

    public void disconnectFromGroup() {
        if(incomingPacketsTask != null && !incomingPacketsTask.isCancelled()) {
            incomingPacketsTask.cancel(true);
        }
        if (mManager != null && mChannel != null) {
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Log.d(TAG, "removeGroup onSuccess -");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "removeGroup onFailure -" + reason);
                }
            });
            /*
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && mManager != null && mChannel != null
                            && group.isGroupOwner()) {
                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "removeGroup onSuccess -");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(TAG, "removeGroup onFailure -" + reason);
                            }
                        });
                    }
                }
            });*/
        }
    }

    public void stopLookingForPeers() {
        // lowest build available with stop peer discovery.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            mManager.stopPeerDiscovery(mChannel, peerDiscoveryListener);
        if(discoverPeersTask != null) {
            discoverPeersTask.cancel(true);
            discoverPeersTask = null;
        }
    }

    public void lookForPeers() {
//        if(discoverPeersTask == null || discoverPeersTask.isCancelled() == true) {
            // force this call.  Note that this is an async call.
            discoverPeersTask = new CallDiscoverPeersTask();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            discoverPeersTask.executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR, mManager, mChannel, peerDiscoveryListener);
        else
            discoverPeersTask.execute(mManager, mChannel, peerDiscoveryListener);
//        }
    }

    public WifiP2pManager.PeerListListener getPeerListListener() {
        return peerListListener;
    }

    public final List getPeers() {

        peerListLock.lock();
        try {
            //lookForPeers();
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

            return peers;
        }
        finally {
            peerListLock.unlock();
        }

    }

    public int connectToPeer(String connectToThisAddress) {
        int counter = 0;
        WifiP2pDevice connectToThisDevice= null;

        if(peers.size() == 0) {
          return -1;
        }

        for(counter = 0; counter < peers.size(); counter++) {
            if(connectToThisAddress.equals((peers.get(counter)).deviceAddress )) {
                connectToThisDevice = peers.get(counter);
                break;
            }
        }

        if(connectToThisDevice == null) {
            return -2;
        }

        WifiP2pConfig configInfo = new WifiP2pConfig();
        configInfo.deviceAddress = connectToThisDevice.deviceAddress;
        configInfo.groupOwnerIntent = 15;
        //configInfo.wps.setup = WpsInfo.PBC;

        mManager.connect(mChannel, configInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // TODO Success stuff
                Log.d(TAG, "Successful connection.");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Failed with failure code " + Integer.toString(reason));
            }
        });

        return 0;
    }

    public final TravelBingoWifiManager getMe() {
        return this;
    }

    private WifiP2pManager.ConnectionInfoListener getConnectInfoListener() {

        if(connectInfoListener == null) {
            connectInfoListener = new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo info) {

                    // both group owners and listeners need an incoming packet parser.
                    if(incomingPacketsTask == null || incomingPacketsTask.isCancelled()) {
                        incomingPacketsTask = new ReceiveAndParsePacketsTask();
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                            incomingPacketsTask.executeOnExecutor(
                                    AsyncTask.THREAD_POOL_EXECUTOR, getMe(), 0, 0);
                        else
                            incomingPacketsTask.execute(getMe(), 0, 0);
                    }

                    // After the group negotiation, we can determine the group owner.
                    if (info.groupFormed && info.isGroupOwner) {
                        // we want to open a server socket for accepting incoming packets.
                        // also want to save the ip address to our hashtag table.
                        //sendIntroductionInfo(0);
                        int hurm = 0;
                    } else if (info.groupFormed) {
                        // InetAddress from WifiP2pInfo struct.
                        hostAddress = info.groupOwnerAddress.getHostAddress();
                        //hostName = info.groupOwnerAddress.getHostName();
                        hostName = "host";
                        getMe().tellActivityHostInfo(hostName, hostAddress);
                        // Send our Mac Address to the host.  Host will match it to our InetAddy
                        // and thus be able to tell which peer is using which iNetAddress.
                        sendIntroductionInfo(1);
                    }
                }
            };
        }

        return connectInfoListener;
    }

    public void tellActivityHostInfo(String name, String address) {
        mActivity.setHost(name, address);
    }

    // from an answer at
    // http://stackoverflow.com/questions/10968951/wifi-direct-and-normal-wifi-different-mac
    public String getWFDMacAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ntwInterface : interfaces) {

                if (ntwInterface.getName().contains("p2p")) {
                    byte[] byteMac = ntwInterface.getHardwareAddress();
                    if (byteMac == null) {
                        return null;
                    }
                    StringBuilder strBuilder = new StringBuilder();
                    for (int i = 0; i < byteMac.length; i++) {
                        strBuilder.append(String.format("%02X:", byteMac[i]));
                    }

                    if (strBuilder.length() > 0) {
                        strBuilder.deleteCharAt(strBuilder.length() - 1);
                    }

                    return strBuilder.toString();
                }

            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        return null;
    }

    public void recordIntroPacketInfo(int which, String macAddress) {
        if(which != 0) {
            // TODO  Get the Peer Device and fetch the last received IP address and hash
            // TODO  them.  Set up host information if other person is host.
            int i;
        }
    }

    // This function will attempt to connect to a peer.  Note that this is an async call so
    // we shouldn't count on the fact that we have a connection.

    // This function is called by the TravelBingo activity to notify the host that a tile was
    // pressed by someone in the group.  It may be used to update a view of the other player's
    // gamecard when we have multiple views for the host.
    public void notifyHostOfTilePress(int position, int marked) {
        //TODO set up creation of a packet to send to the host which contains the
        //TODO position and marked information.  Use a separate thread to send it.
        byte[] notifyTilePressPacket = new byte[TILEPRESSUPDATEPACKETSIZE];

        notifyTilePressPacket[0] = TILEPRESSUPDATEPACKET;

        ByteBuffer tilePressPacket = ByteBuffer.wrap(notifyTilePressPacket);

        //TODO: Get IP of person we are sending packet to, host in this case.

        tilePressPacket.putInt(1, position);
        tilePressPacket.putInt(5, marked);

        // TODO send it to the packet send function.  But it doesn't exist now.
        notifyTilePressPacket = tilePressPacket.array();

        addPacketToOutgoingQueue(notifyTilePressPacket);

        //debug... send this back to our TravelBingo activity to notify of presses.
        //parsePacketInfo(notifyTilePressPacket);
    }

    // this sends information to the other connected player for the purposes of sending packets
    public void sendIntroductionInfo(int which) {
        if(myWiFiMacAddress == null)
            myWiFiMacAddress = getWFDMacAddress();
        byte[] myMacIDByteArray = myWiFiMacAddress.getBytes( Charset.forName("UTF-8"));
        byte[] introPacket = new byte [INRODUCTIONPACKETSIZE + myMacIDByteArray.length];

        introPacket[0] = INTRODUCTIONPACKET;
        introPacket[1] = (byte) which;

        ByteBuffer introSendBuffer = ByteBuffer.wrap(introPacket);
        introSendBuffer.position(INRODUCTIONPACKETSIZE);
        introSendBuffer.put(myMacIDByteArray);

        introPacket = introSendBuffer.array();

        addPacketToOutgoingQueue(introPacket);

    };

    // This is used by the host to send a game card to a player.  Note that it uses resource ids
    // for tilesets and thus means that the host and players MUST use the same version of the app
    // for this to work.
    public void sendGameCard(String macAddOfWho, int tileSet, int seed) {
        //TODO all kinds of checks to make sure the person exists.
        //TODO An eventual check to see if versions match.

        byte[] macAddToByteArray = macAddOfWho.getBytes( Charset.forName("UTF-8" ));
        byte[] sendGCPacket = new byte [GCBYTILESETANDSEEDPACKETSIZE + macAddToByteArray.length];
        sendGCPacket[0] = GCBYTILESETANDSEEDPACKET;

        ByteBuffer gcSendBBuffer = ByteBuffer.wrap(sendGCPacket);
        gcSendBBuffer.putInt(1, tileSet);
        gcSendBBuffer.putInt(5, seed);
        gcSendBBuffer.position(GCBYTILESETANDSEEDPACKETSIZE);
        gcSendBBuffer.put(macAddToByteArray);


        sendGCPacket = gcSendBBuffer.array();

        //TODO Check the packet queue and remove previous sends of gamecards to the same person.

        outgoingQueueLock.lock();
        try {
            int counter = 0;
            for(counter = 0; counter < outgoingSocketQueue.size(); counter++) {
                if(outgoingSocketQueue.get(counter)[0] == GCBYTILESETANDSEEDPACKET) {
                    String receiverName = new String(outgoingSocketQueue.get(counter),
                            GCBYTILESETANDSEEDPACKETSIZE,
                            outgoingSocketQueue.get(counter).length - GCBYTILESETANDSEEDPACKETSIZE);

                    Log.d(TAG," receiverName is "+ receiverName.substring(9));

                    if(macAddOfWho.equals(receiverName))
                    {
                        outgoingSocketQueue.remove(counter);
                        counter--;
                    }
                }
            }

        }
        finally
        {
            outgoingQueueLock.unlock();
        }
        addPacketToOutgoingQueue(sendGCPacket);

        //debug send this back to our TravelBingo activity to update gamecard.
        //parsePacketInfo(sendGCPacket);
    }

    // Parse the packet we received from our WiFi Direct connection and take the appropriate
    // action.
    public void parsePacketInfo(byte[] packetInfo) {
            ByteBuffer packetInfoBuffer = ByteBuffer.wrap(packetInfo);
        switch(packetInfo[0]) {
            case TILEPRESSUPDATEPACKET:
                int position = packetInfoBuffer.getInt(1);
                int marked = packetInfoBuffer.getInt(5);
                byte[] fromWho = new byte[packetInfo.length - TILEPRESSUPDATEPACKET];
                packetInfoBuffer.position(TILEPRESSUPDATEPACKETSIZE);
                packetInfoBuffer.get(fromWho);
                String sender = new String(fromWho);
                mActivity.notifyTilePress(position, marked, sender);
                break;
            case GCBYTILESETANDSEEDPACKET:
                int tileSet = packetInfoBuffer.getInt(1);
                int seed = packetInfoBuffer.getInt(5);
                mActivity.updateGameCard(tileSet, seed);
                break;
            case INTRODUCTIONPACKET:
                int which = packetInfoBuffer.getInt(1);
                byte[] macAddInfo = new byte [packetInfo.length - INRODUCTIONPACKETSIZE];
                packetInfoBuffer.position(INRODUCTIONPACKETSIZE);
                packetInfoBuffer.get(macAddInfo);
                String macAddInfoString = new String(macAddInfo);
                recordIntroPacketInfo(which, macAddInfoString);
                break;

            default:
                Log.d(TAG, "Unrecognized packet type(" + packetInfo[0] +
                        "), discarding packet information.");
                break;

        }
    }

    private void addPacketToOutgoingQueue(byte[] packet) {
        outgoingQueueLock.lock();
        try {
            outgoingSocketQueue.add(packet);
        }
        finally {
            outgoingQueueLock.unlock();
        }

        if(outgoingPacketsTask == null || outgoingPacketsTask.isCancelled() ||
                outgoingPacketsTask.getStatus() == AsyncTask.Status.FINISHED) {
            outgoingPacketsTask = new SendPacketsTask();
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                outgoingPacketsTask.executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, hostAddress, new Integer(8888), getMe());
            else
                outgoingPacketsTask.execute(getMe(), 0, 0);
        }
    }

    public byte[] getPacketFromOutgoingQueue() {
        outgoingQueueLock.lock();
        try {
            if(outgoingSocketQueue.size() == 0)
                return null;
            return outgoingSocketQueue.remove(0);
        }
        finally {
            outgoingQueueLock.unlock();
        }
    }


}
