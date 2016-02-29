package com.psripinyo.travelbingo;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * Created by psripinyo on 2/23/2016.
 * Copyright 2016 Peter Sripinyo
 * code from - http://developer.android.com/guide/topics/ui/layout/gridview.html
 * This Adapter is used with a gridview and displays the Game Card for a Travel Bingo game.
 * It is used in conjunction with TravelBingoImageView and cannot be used with regular views.
 */
public class GameCardAdapter extends BaseAdapter {

    private Context mContext;
    private int[] tileImages; // This is actually an array of Resource IDs for tiles.

    //tileImagesIds should be an array of valid resource IDs for the activity.
    public GameCardAdapter(Context c, int[] tileImagesIds) {
        super();
        //We'll make a copy of this array for our own use.  It should be an array of ResIds.
        tileImages = tileImagesIds.clone();

        mContext = c;
    }

    public GameCardAdapter(Context c) {
        super();
        mContext = c;
        //TODO: What do we want to do with this, just use default tiles? Currently empty Adapter
    }

    // This function is used to reset the tile images in the Grid Views child views.
    // note that we just drop our old tileImages array to be GC'd and clone the set given us.
    // then we notifyDataSetChanged() so we can get a redraw with the new tiles.
    // We assume that the free space is in the middle and we don't check for it or mark it.
    public void resetTileImages(int[] newTileImages) {
        tileImages = newTileImages.clone();
        notifyDataSetChanged();
    }

    // return the amount of tiles on our game card as we know it.
    public int getCount() {
        return tileImages.length;
    }

    // unused.
    public Object getItem(int position) {
        return null;
    }

    // unused for now.
    public long getItemId(int position) {
        return 0;
    }


    // Get the view at 'position' in the grid view.  If it doesn't exist, we create it.
    // note that in this case, we assume that the middle is the free space and mark it.
    // We also set the view as having it's art set because it's safe to do so without custom cards.
    public View getView(int position, View convertView, ViewGroup parent) {
        TravelBingoImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            //TODO: Remove hard coding at some point.
            imageView = new TravelBingoImageView(mContext);
            imageView.setAdjustViewBounds(true);
            imageView.setBackgroundColor(Color.WHITE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                imageView.setElevation(10);
            }
            imageView.setLayoutParams(new GridView.LayoutParams(GridLayout.LayoutParams.WRAP_CONTENT,
                    GridLayout.LayoutParams.WRAP_CONTENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (TravelBingoImageView) convertView;
        }

        if (tileImages != null) {
            imageView.setImageResource(tileImages[position]);
            //TODO: When users can create custom cards this may not be true at initialization.
            imageView.setIsArtSelected(true);
            //TODO: This assumes that the middle tile is the free space, maybe check some other way.
            if(position == tileImages.length/2) {
                imageView.setIsMarkedOnGameBoard(true);
            }
        }

        return imageView;
    }
}
