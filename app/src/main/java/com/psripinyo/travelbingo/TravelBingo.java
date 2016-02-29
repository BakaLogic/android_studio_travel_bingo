package com.psripinyo.travelbingo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.net.wifi.p2p.WifiP2pDevice;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;
import android.util.Log;
import android.support.v7.widget.Toolbar;
import java.nio.InvalidMarkException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.view.Menu;
import android.view.MenuItem;

/* 2/23/2016
 * GridView code from
 * http://developer.android.com/guide/topics/ui/layout/gridview.html
 *
 * Copyright 2016 Peter Sripinyo
 *
 */

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class TravelBingo extends AppCompatActivity {


    // inner class for holding peer device information
    class PeerNameAndAdd {

        private String myPeerName;
        private String myPeerID;

        PeerNameAndAdd(String peerName, String peerID) {
            myPeerID = peerID;
            myPeerName = peerName;
        }

        PeerNameAndAdd() {
            myPeerID = null;
            myPeerName = null;
        }

        void setMyPeerID(String peerID) {
            myPeerID = peerID;
        }

        public void setMyPeerName(String peerName) {
            myPeerName = peerName;
        }

        public String getMyPeerID() {
            return myPeerID;
        }

        public String getMyPeerName() {
            return myPeerName;
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof PeerNameAndAdd)) {
                return false;
            }
            return(((PeerNameAndAdd) obj).getMyPeerID().equals(myPeerID) &&
                    ((PeerNameAndAdd) obj).getMyPeerName().equals(myPeerName));
        }
    }

    /* psripinyo
     */
    // BLANK - the ImageView has no tile art selected.
    //
    public enum ImageViewArtType {BLANK, SELECTED}  // for future use.
    public enum GameTileState { UNCHECKED, CHECKED} // TODO: replace 0 and 1 with these.
    public static final String TAG = "TravelBingo.java";

    //TODO: The Views hold checkmark info.  They should probably get it from us instead.
    //  Array that holds the information of whether a tile has been marked or not.
    private int[] markedTiles;

    // This holds the resource ID of the current tileset that we are using.
    private int currentTileSet;

    // boolean which stores whether or not the device is WiFi Direct capable
    private boolean isWiFiDirectSupported;

    // This is a wifi Manager for our Travel Bingo Activity.
    private TravelBingoWifiManager tbWifiManager;

    // This holds our selected items from the Peer Selection menu.
    private List<Integer> mSelectedItems;

    // a boolean list of which items have already been checked.
    boolean[] menuItemIsMarked;

    // listOfPeers - a list of peers that we got when we opened the selection menu
    private List<PeerNameAndAdd> menuItemPeers;

    // List of peers in our group
    private List<PeerNameAndAdd> peersInGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_travel_bingo);

        //set up the list of Peers in Group for use.
        //peersInGroup;

        // This is an array containing the Resource IDs of all available tilesets.
        // we need to load the array to get at the resId for the default tile set.
        TypedArray availableTileSets = getResources().obtainTypedArray(R.array.tileset_list);

        // Because this is the start of the app we'll just load the default tile set.
        currentTileSet = availableTileSets.getResourceId(0, -1);

        // create the Int array of resource Ids that we'll use to transfer info to the gcAdapter
        int[] intValImageResIds = getTileSetResIdArrayfromArrayResourceID(currentTileSet);

        // This array holds info on whether or not a tile is check marked.
        if(markedTiles == null)
            markedTiles = new int[intValImageResIds.length];

        // assuming odd number of rows/columns with a center free space.
        markedTiles[intValImageResIds.length / 2] = 1;

        // set up the gridView gameboard.
        GridView gameCard = (GridView) findViewById(R.id.bingoCard);
        gameCard.requestFocus();

        //create the game adapter and pass it the resource Ids of the images from the default set.
        gameCard.setAdapter(new GameCardAdapter(this, intValImageResIds));

        gameCard.setOnItemClickListener(new OnItemClickListener() {

            // Our listener checks to see if the position is not the free space.
            // If it's not, then it will mark the tile if it's not marked or unmark it
            // if it is marked.  It sets the ImageView boolean appropriately, calls
            // an invalidate to force the image to redraw and then checks for victory.
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {

                if (v instanceof TravelBingoImageView) {
                    //don't mark/unmark the freespace
                    //TODO: It's okay to assume that it's a free space in the middle, right?
                    if (position != (markedTiles.length / 2)) {
                        TravelBingoImageView tbImageView = (TravelBingoImageView) v;
                        boolean markBoard = !tbImageView.isMarkedOnGameBoard();
                        tbImageView.setIsMarkedOnGameBoard(markBoard);
                        markedTiles[position] = (!markBoard) ? 0 : 1;
                        v.invalidate();
                        if (checkForVictoryCondition(position)) {
                            doVictoryCelebration();
                        }
                    }
                }
            }
        });

        android.support.v7.widget.Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.setTitle(R.string.app_name);

        availableTileSets.recycle();

        tbWifiManager = new TravelBingoWifiManager(this);

        isWiFiDirectSupported = tbWifiManager.isWifiDirectSupported(this);
    }



    // this name is misleading.  We're looking for ResourceIDs from the TypedArray.
    // TODO: Rename this to something more specific when I'm not lazy.
    private int[] getIntArrayFromTypedArray(TypedArray typArray) {
        int[] intArray = new int[typArray.length()];

        for(int counter = 0; counter < typArray.length(); counter++)
            intArray[counter] = typArray.getResourceId(counter, 0);

        return intArray;
    }

    // Take a tile set resource ID and return an array of resource ids of the tiles in the set.
    private int[] getTileSetResIdArrayfromArrayResourceID(int currentTileSet)
    {
        //we expand this to get the array of resource Ids from resources.
        TypedArray tileImagesIds = getResources().obtainTypedArray(currentTileSet);
        // we convert it to an Int Array to pass it to the gcAdapter.
        int[] returnArray = getIntArrayFromTypedArray(tileImagesIds);
        tileImagesIds.recycle();
        return returnArray;
    }

    // We plan on removing checkmarks when the gameboard is recreated because it's
    // not fair to allow someone to start with new checked tiles.  We want to warn
    // the user before we do it so we pop a dialog with a warning that progress will be reset.
    // TODO: When we have game states such as setting up board for kids, playing, we need to
    // TODO: diable this when playing the game.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AlertDialog.Builder builder;
        switch (item.getItemId()) {
            case R.id.randomize_gamecard:
                builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.randomize_dialog_message);
                builder.setTitle(R.string.randomize_dialog_title);
                builder.setPositiveButton(getResources().getText(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                randomizeGameCard();
                            }
                        });
                builder.setNegativeButton(getResources().getText(R.string.cancel),

                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog rGCDialog = builder.create();
                rGCDialog.show();
                return true;

            case R.id.select_tileset:
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.select_tileset_title);
                builder.setItems(R.array.available_tilesets,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //TODO Decide on if we change tiles if current set is selected
                                //TODO Currently, we do.
                                TypedArray availableTileSets =
                                        getResources().obtainTypedArray(R.array.tileset_list);
                                currentTileSet = availableTileSets.getResourceId(which, -1);
                                randomizeGameCard();
                                availableTileSets.recycle();
                            }
                        });
                AlertDialog stsDialog = builder.create();
                stsDialog.show();
                return true;

            case R.id.wifi_direct_menu_item:
                if(!isWiFiDirectSupported)
                {
                    builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.wifi_direct_nogo);
                    builder.setTitle(R.string.wifi_direct_title);
                    builder.setPositiveButton(getResources().getText(R.string.ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog noWiFiDirectDialog = builder.create();
                    noWiFiDirectDialog.show();
                    return true;
                }
                else {
                    List peerDevices = tbWifiManager.getPeers();
                    if(peerDevices == null) {
                        // TODO For some reason we don't have any peers.  We need to do
                        // TODO an asynchronous search for peers.  Alert user and search for peers.
                    }
                    else {
                        menuItemPeers = new ArrayList<PeerNameAndAdd>();
                        for(int counter = 0; counter < peerDevices.size(); counter++)
                        {
                            TravelBingo.PeerNameAndAdd currentPeerInfo = new PeerNameAndAdd();
                            currentPeerInfo.setMyPeerID(
                                    ((WifiP2pDevice)(peerDevices.get(counter))).deviceAddress);
                            currentPeerInfo.setMyPeerName(
                                    ((WifiP2pDevice)(peerDevices.get(counter))).deviceName);
                            menuItemPeers.add(currentPeerInfo);
                        }
                        showPeerSelectionDialog(menuItemPeers);
                        return true;
                    }
                    return true;
                }

                // fallthrough to default

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void setPeersInGroup() {

        peersInGroup = new ArrayList<PeerNameAndAdd>();

        int numSelectedItems = mSelectedItems.size();

        for(int counter = 0; counter < numSelectedItems; counter++) {
            peersInGroup.add(menuItemPeers.get((int)mSelectedItems.get(counter)));
        }
    }

    // This function is called to create an alert dialog to allow the user to choose peers
    // to include in the WiFi Direct group
    private void showPeerSelectionDialog(List menuItems) {
        mSelectedItems = new ArrayList<Integer>();  // Where we track the selected items, GC old one.
        AlertDialog.Builder builder;
        int numPeers = menuItems.size();
        String[] peerNames = new String[numPeers];
        String peerName;
        menuItemIsMarked = new boolean[numPeers]; // let GC collect the old list.

        for(int counter = 0; counter < numPeers; counter++) {
            //peerName.copy(((WifiP2pDevice) peerDevices[counter]).deviceName);
            peerNames[counter] = ((PeerNameAndAdd) menuItems.get(counter)).getMyPeerName();

            //if the peer device is in peers in group set the check to true.
            if(peersInGroup != null && peersInGroup.contains(menuItems.get(counter))) {
                menuItemIsMarked[counter] = true;
                mSelectedItems.add(counter);
            }
        }
        builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.wifi_direct_peer_choose)
                .setMultiChoiceItems(peerNames, menuItemIsMarked,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    mSelectedItems.add(which);
                                } else if (mSelectedItems.contains(which)) {
                                    // Else, if the item is already in the array, remove it
                                    mSelectedItems.remove(Integer.valueOf(which));
                                }
                            }
                        })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so save the mSelectedItems results somewhere
                        // or return them to the component that opened the dialog
                        ((AlertDialog) dialog).getListView().setItemChecked(0, true);
                        setPeersInGroup();
                    }
                })
                .setNegativeButton(getResources().getText(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog peerListDialog = builder.create();
        peerListDialog.show();
    }

    //Fisher-Yates shuffle array function taken from
    // http://stackoverflow.com/questions/1519736/random-shuffling-of-an-array
    // modified to be less random because of free space.  Yeah, that again.
    private void randomizeTiles(int[] gameTilesResIds) {
        int index, temp;
        Random random = new Random();
        for (int i = gameTilesResIds.length - 1; i > 0; i--)
        {
            if(i == gameTilesResIds.length / 2)
                continue;
            do {
                index = random.nextInt(i + 1);
            } while(index == gameTilesResIds.length / 2);

            temp = gameTilesResIds[index];
            gameTilesResIds[index] = gameTilesResIds[i];
            gameTilesResIds[i] = temp;
        }
    }

    // This sets the check marks back to unchecked for everything except the center tile
    // which we assume is free space.
    private void resetCheckMarks(GridView gameCard) {
        int counter = 0;
        TravelBingoImageView currentView;

        for(counter = 0; counter < markedTiles.length; counter++) {
            // Are we still assuming a free space tile?  Yes, we are.
            if(counter != (markedTiles.length / 2)) {
                currentView = (TravelBingoImageView) gameCard.getChildAt(counter);
                if (currentView != null)
                    currentView.setIsMarkedOnGameBoard(false);
                markedTiles[counter] = 0;
            }
        }
    }

    // We get the default tile set (The only one in current use) and put the ResIds into
    // an int array.  We call the randomize function to randomize the location of each tile
    // and then we feed the new tileSet locations in an int array to the Game Card Adapter.
    // We then reset the check marks on the game card for all tiles except the Free Space.
    // TODO: support for multiple tilesets.
    public void randomizeGameCard() {

        int[] gameTilesResIds = getTileSetResIdArrayfromArrayResourceID(currentTileSet);

        randomizeTiles(gameTilesResIds);

        GridView gameCard = (GridView) findViewById(R.id.bingoCard);
        gameCard.requestFocus();

        GameCardAdapter gcAdapter = (GameCardAdapter) gameCard.getAdapter();
        gcAdapter.resetTileImages(gameTilesResIds);
        // TODO: This wouldn't be necessary if the view knew it's position and asked the activity
        resetCheckMarks(gameCard);
    }

    // I like big butts and I cannot lie.  You other brothers can't deny...
    // inflate the menu on creation.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    //TODO: Make the victory more awesome.  In the future, inform other players that we won.
    private void doVictoryCelebration() {
        Log.d(TAG, "You win. Strut.");
        Toast.makeText(TravelBingo.this, "You win!\nWeep for there are no more worlds to conquer.",
                Toast.LENGTH_SHORT).show();
    }

    // Each time the player sets a check we need to see if they won.  The algorithm is as follows
    // (1) Check the row that we are in.  If we won, call victory function.  Otherwise continue.
    // (2) Check the col that we are in.  If we won, call victory function.  Otherwise continue.
    // (3) Check to see if we are on the descending diagonal.  If so see if we won.
    // (4) Check to see if we are on the ascending diagonal.  If so see if we won.
    private boolean checkForVictoryCondition(final int position) {
        //TODO: Figure out if we're okay assuming this will get us the row and column length.
        // Because it could be that we don't have an odd number of rows/columns -- i.e.
        // no Free Space.  We would have to enforce an odd number of rows/columns.
        int rowColSize = (int) Math.sqrt((double) markedTiles.length);
        boolean hasWon = false;

        //position is the index where we are going to start the check to see if we have won.
        int currentIndex = position;
        int startIndex = 0, loopCounter = 0;

        // setting our current index to the start of the row where our position resides.
        if (currentIndex != 0 && currentIndex % rowColSize != 0)
            currentIndex -= (currentIndex % rowColSize);

        // check the row we are in.  If one of the tiles is not checked, we haven't one.
        for (startIndex = currentIndex; currentIndex < startIndex + rowColSize; currentIndex++) {
            if (markedTiles[currentIndex] == 0) {
                break;
            }
        }

        if (currentIndex >= startIndex + rowColSize) {
            hasWon = true;
        }

        // we didn't complete the row.  Let's see if we completed the column.
        if (!hasWon) {
            //again, let's assume we won
            currentIndex = position;
            //divid by zero is not cool.
            if (position != 0) {
                // Note we want position/rowColSize first to get rid of remainder.
                currentIndex = position - (rowColSize * (position / rowColSize));
            }

            // we're at the 'zero' index of our column
            for (startIndex = currentIndex; currentIndex < markedTiles.length;
                 currentIndex += rowColSize) {
                if (markedTiles[currentIndex] == 0) {
                    break;
                }
            }

            if (currentIndex >= markedTiles.length) {
                hasWon = true;
            }
        }
        //We need to check special cases for the diagonals.  We'll see if our index falls
        //on a diagonal line.
        if (!hasWon) {
            if (position == 0 || position == markedTiles.length - 1 ||
                    (position % rowColSize) == position / rowColSize) {
                for (currentIndex = 0, loopCounter = 0; currentIndex < markedTiles.length;
                     currentIndex += (rowColSize + 1)) {
                    if (markedTiles[currentIndex] == 0)
                        break;
                }

            }

            if (currentIndex >= markedTiles.length) {
                hasWon = true;
            }
        }

        //if we didn't win on the one diagonal, check the other.
        if(!hasWon) {
            // check the other diagonal.
            if(position != 0 &&
                    (((position % rowColSize) + (position/rowColSize)) == rowColSize - 1)) {
                for(currentIndex = rowColSize - 1, loopCounter = 0;
                    currentIndex < markedTiles.length - 1;
                    currentIndex += (rowColSize - 1))
                {
                    if(markedTiles[currentIndex] == 0)
                        break;
                }
                if(currentIndex >= markedTiles.length - 1) {
                    hasWon = true;
                }

            }

        }

        return hasWon;
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        if(tbWifiManager.getMyReceiver()!= null && tbWifiManager.getMyIntentFilter() != null )
            registerReceiver(tbWifiManager.getMyReceiver(), tbWifiManager.getMyIntentFilter());
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        if(tbWifiManager.getMyReceiver()!= null)
            unregisterReceiver(tbWifiManager.getMyReceiver());
    }
}
