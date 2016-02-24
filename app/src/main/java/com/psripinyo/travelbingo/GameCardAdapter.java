package com.psripinyo.travelbingo;

import android.content.res.TypedArray;
import android.widget.BaseAdapter;
import android.content.Context;
import android.widget.ImageView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

/**
 * Created by psripinyo on 2/23/2016.
 * code from - http://developer.android.com/guide/topics/ui/layout/gridview.html
 */
public class GameCardAdapter extends BaseAdapter {

    private Context mContext;
    private TypedArray tileImages;

    public GameCardAdapter(Context c, int tileSetResourceID) {
        super();
        //TODO: Check for leaky memory here.
        tileImages = c.getResources().obtainTypedArray(tileSetResourceID);
        mContext = c;
    }

    public GameCardAdapter(Context c) {
        super();
        mContext = c;
        //TODO: What do we want to do with this, just use default tiles?
//        tileImages = mContext.getResources().obtainTypedArray(R.array.default_tileset);
    }

    public int getCount() {
        return tileImages.length();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        TravelBingoImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new TravelBingoImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (TravelBingoImageView) convertView;
        }

        if (tileImages != null) {
            imageView.setImageResource(tileImages.getResourceId(position, -1));
            imageView.setIsArtSelected(true);
            //TODO: This assumes that the middle tile is the free space, maybe check some other way.
            if(position == (tileImages.length()/2)) {
                //TODO: do we want to mark this or use a boolean to indicate that we draw the check?
                imageView.setIsMarkedOnGameBoard(true);
            }
        }

        return imageView;
    }
}
