package com.psripinyo.travelbingo;

import android.net.wifi.p2p.WifiP2pDevice;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

/**
 * Created by psripinyo on 2/27/2016.
 */
public class PeerInfo {

    private WifiP2pDevice peerDevice;
    private InetAddress inAdd;
    private Inet4Address in4Add;
    private Inet6Address in6Add;

    public PeerInfo () {

        peerDevice = null;
        inAdd = null;
        in4Add = null;
        in6Add = null;
    }

    public void setPeerDevice(WifiP2pDevice newDevice) {
        peerDevice = newDevice;
    }

    public WifiP2pDevice getPeerDevice() {
        return peerDevice;
    }

    public void setInAdd(InetAddress newInAdd) {
        inAdd = newInAdd;
    }

    public InetAddress getInAdd() {
        return inAdd;
    }


}
