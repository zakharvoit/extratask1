package ru.ifmo.zakharvoit.extratask1.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import ru.ifmo.zakharvoit.extratask1.R;
import ru.ifmo.zakharvoit.extratask1.images.Image;
import ru.ifmo.zakharvoit.extratask1.images.ImagesDownloadService;
import ru.ifmo.zakharvoit.extratask1.images.ImagesResultReceiver;
import ru.ifmo.zakharvoit.extratask1.provider.picture.PictureContentValues;
import ru.ifmo.zakharvoit.extratask1.provider.picture.PictureSelection;

public class MainActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private ImagesAdapter adapter;
    private int loadersCount = 0;
    private ProgressBar downloadProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        downloadProgressBar = (ProgressBar) findViewById(R.id.download_progress_bar);

        adapter = new ImagesAdapter(this, new PictureSelection()
                .query(getContentResolver()));

        GridView imagesGrid = (GridView) findViewById(R.id.images_grid);
        imagesGrid.setAdapter(adapter);

        ImagesResultReceiver receiver = new ImagesResultReceiver();
        receiver.setReceiver(createReceiver(this));

        getLoaderManager().initLoader(loadersCount++, null, this);

        Intent intent = new Intent(this, ImagesDownloadService.class);
        intent.putExtra(ImagesDownloadService.RESULT_RECEIVER_EXTRA_KEY, receiver);
        startService(intent);
    }

    private ImagesResultReceiver.Receiver createReceiver(final Context context) {
        return new ImagesResultReceiver.Receiver() {
            public static final String TAG = "ResultReceiver";

            private Image[] images;
            int currentSize;

            @Override
            public void onListDownload(int size) {
                Log.d(TAG, "List downloaded");
                images = new Image[size];
                currentSize = 0;
                downloadProgressBar.setProgress(100 * currentSize / size);
                downloadProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onImageDownload(Image image) {
                Log.d(TAG, "New image downloaded");
                if (image != null) { // FIXME: Strange hack
                    images[currentSize++] = image;
                    downloadProgressBar.setProgress(100 * currentSize / images.length);
                }
            }

            @Override
            public void onFinishDownload() {
                Log.d(TAG, "Finished downloading");
                new PictureSelection().delete(getContentResolver());
                for (int i = 0; i < currentSize; i++) {
                    Image image = images[i];
                    PictureContentValues contentValues = new PictureContentValues();
                    contentValues.putTitle(image.getTitle());
                    contentValues.putContents(image.getContents());
                    contentValues.putLargeLink(image.getLargeLink());
                    contentValues.insert(getContentResolver());
                }
                images = null;
                downloadProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onError() {
                Toast.makeText(context, "Error during loading images", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "An error happened");
                images = null;
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d("LoaderCallbacks", "Create loader");
        return new CursorLoader(this, new PictureSelection().uri(), null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d("LoaderCallbacks", "Load finished " + data.getCount());
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d("LoaderCallbacks", "Load reset");
        adapter.swapCursor(null);
    }
}
