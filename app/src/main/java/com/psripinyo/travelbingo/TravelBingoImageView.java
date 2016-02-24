package com.psripinyo.travelbingo;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.widget.ImageView;
import android.content.Context;
import android.graphics.BitmapFactory;

/**
 * Created by psripinyo on 2/23/2016.
 */
public class TravelBingoImageView extends ImageView{

    private boolean isArtSelected;
    private boolean isMarkedOnGameBoard;
    private static Bitmap checkMarkBitmap;

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
        if(checkMarkBitmap != null && isMarkedOnGameBoard)
        {
            //TODO: Center the Check Mark on the bitmap
            Matrix matrix = getImageMatrix();
            canvas.drawBitmap(checkMarkBitmap, matrix, null);
            invalidate();
        }
    }
}
