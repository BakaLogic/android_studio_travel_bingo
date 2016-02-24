package com.psripinyo.travelbingo;

import android.annotation.SuppressLint;
import android.content.res.TypedArray;
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
import android.view.Menu;
import android.view.MenuItem;

/* 2/23/2016
 * GridView code from
 * http://developer.android.com/guide/topics/ui/layout/gridview.html
 */

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class TravelBingo extends AppCompatActivity {

    /* psripinyo
     */
    // BLANK - the ImageView has no tile art selected.
    //
    public enum ImageViewArtType {BLANK, SELECTED}
    public enum GameTileState { UNCHECKED, CHECKED}
    public static final String TAG = "com.psripinyo.travelbingo.TravelBingo.java";

    //psripinyo
    private int[] markedTiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_travel_bingo);

        //we don't recycle this because GameCardAdapter is using it.
        TypedArray tileImages = getResources().obtainTypedArray(R.array.default_tileset);

        if(markedTiles == null)
            markedTiles = new int[tileImages.length()];

        // assuming odd number of rows/columns with a center free space.
        markedTiles[tileImages.length() / 2] = 1;

        // set up the gridView gameboard.
        GridView gameCard = (GridView) findViewById(R.id.bingoCard);
        gameCard.requestFocus();
        gameCard.setAdapter(new GameCardAdapter(this, R.array.default_tileset));

        gameCard.setOnItemClickListener(new OnItemClickListener() {
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

                //TODO: when we have a game stat of building custom cards, check game state.
                TravelBingoImageView gameTile = null;

            }
        });

        android.support.v7.widget.Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.setTitle(R.string.app_name);

        tileImages.recycle();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.randomize_gamecard:
                Toast.makeText(TravelBingo.this, "Sorry, this does nothing.",
                        Toast.LENGTH_SHORT).show();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    private void doVictoryCelebration() {
        Log.d(TAG, "You win. Strut.");
        Toast.makeText(TravelBingo.this, "You win!\nWeep for there are no more worlds to conquer.",
                Toast.LENGTH_SHORT).show();
    }

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
}
