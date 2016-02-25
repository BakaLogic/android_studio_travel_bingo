package com.psripinyo.travelbingo;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.widget.ImageView;
import android.content.Context;
import android.graphics.BitmapFactory;

/**
 * Created by psripinyo on 2/23/2016.
 * Copyright 2016 Peter Sripinyo
 */
public class TravelBingoImageView extends ImageView{

    private boolean isArtSelected;  // for future use, determine if the art is game card art or not.
    //TODO: The Views hold checkmark info.  They should probably get it from the activity instead.
    private boolean isMarkedOnGameBoard; // whether or not to draw a check over the Image.
    private static Bitmap checkMarkBitmap; // one copy of the check mark bitmap for use by all.

    TravelBingoImageView(Context context) {
        super(context);

        isArtSelected = false;
        isMarkedOnGameBoard = false;
        if(checkMarkBitmap == null)
            checkMarkBitmap = BitmapFactory.decodeResource(getResources(),
                            R.drawable.transparent_green_check_temp);

    }

    public boolean isArtSelected() {
        return isArtSelected;
    }

    public void setIsArtSelected(boolean isArtSelected) {
        this.isArtSelected = isArtSelected;
    }

    public boolean isMarkedOnGameBoard() {
        return isMarkedOnGameBoard;
    }

    public void setIsMarkedOnGameBoard(boolean isMarkedOnGameBoard) {
        this.isMarkedOnGameBoard = isMarkedOnGameBoard;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // we want to draw the checkmark on top of the Image if the user has marked it off.
        if(checkMarkBitmap != null && isMarkedOnGameBoard)
        {
            //TODO: Center the Check Mark on the bitmap
            Matrix matrix = getImageMatrix();
            canvas.drawBitmap(checkMarkBitmap, matrix, null);
            invalidate();
        }
    }
}
