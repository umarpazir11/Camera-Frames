package com.example.cameraframes;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;


/**
 * The main activity for the app. See README for more information about the app.
 * @author Navid Rojiani
 * @date Apr 8, 2014
 */
public class MainActivity extends Activity implements OnTouchListener, SurfaceTextureListener {
    
    /** ScrollView that can be enabled or disabled so that scrolling doesn't interfere with the
     * dragging of image frames.*/
    private LockableScrollView scrollView;
    
    /** Relative Layout for all camera frames */
    private RelativeLayout relativeLayout;
    
    /** Screen Dimensions in px (Landscape Orientation locked) */
    private int SCREEN_WIDTH;    
    private int SCREEN_HEIGHT;
    
    /** Dimensions of each frame - 12 per screen (4 columns, 3 rows) */
    private int FRAME_WIDTH;    
    private int FRAME_HEIGHT;
    
    /** Camera */
    private Camera camera;
    
    /** The camera preview frame */
    private TextureView previewFrame;    
    
    /** Stores the frozen frames */
    private ArrayList<ImageView> freezeFrames;    
    
    /** Preview Frame Layout Parameters */
    private LayoutParams previewParams;
    
    /** Column that the preview frame is in (0-3) */
    private int previewColumn;
    
    /** Row that the preview frame is in (0-inf) */
    private int previewRow;
    
    /** ImageView for the saved image */
    private ImageView iv;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create new ScrollView
        scrollView = new LockableScrollView(this);
        
        // Put a RelativeLayout in the ScrollView
        relativeLayout = new RelativeLayout(this);
        scrollView.addView(relativeLayout);
        
        setDimensions();
        
        freezeFrames = new ArrayList<ImageView>();
                                
        // Initialize the previewFrame
        previewFrame = new TextureView(this);
        previewFrame.setSurfaceTextureListener(this);
        previewFrame.setOnTouchListener(this);
        previewFrame.setId(99);
        previewParams = new LayoutParams(FRAME_WIDTH, FRAME_HEIGHT);
        previewParams.leftMargin = 0;
        previewParams.topMargin = 0;
        previewFrame.setLayoutParams(previewParams);
        relativeLayout.addView(previewFrame, previewParams);
        previewColumn = previewRow = 0;
        
        // Disallow scrolling until the screen has 5 or more freeze frames
        scrollView.disableScrolling();
        this.setContentView(scrollView);
        
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        CreateMenu(menu);
        return true;
    }    
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return MenuChoice(item);        
    }
    
    private void CreateMenu(Menu menu) {
        MenuItem save = menu.add(0, 0, 0, "Save");
        {
            save.setIcon(R.drawable.ic_action_save_dark);
            save.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        MenuItem clear = menu.add(0, 1, 1, "Clear");
        {
            clear.setIcon(R.drawable.ic_action_discard_dark);
            clear.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        MenuItem seePic = menu.add(0, 2, 2, "See Picture");
        {
            seePic.setIcon(R.drawable.ic_action_picture);
            seePic.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
    }
    
    private boolean MenuChoice(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                // convert list of imageviews to a list of bitmaps
                ArrayList<Bitmap> bitmaps = extractBitmaps(freezeFrames);
                
                Bitmap bitmapToSave = combineImageIntoOne(bitmaps);                
                //String filename = createFilename();
                String filename = "saved_image";
                saveBitmap(bitmapToSave, filename);                
                
                return true;
            case 1:
                clear();
                return true;
            case 2:
                showSavedPicture();
                return true;
        }
        return false;
    }
    
    /**
     * Extracts the bitmap from each ImageView and puts it in a new list.
     * @param imageViews An ArrayList of ImageView
     * @return An ArrayList of bitmaps
     */
    private ArrayList<Bitmap> extractBitmaps(ArrayList<ImageView> imageViews) {
        ArrayList<Bitmap> bitmapList = new ArrayList<Bitmap>();
        for (int i = 0; i < imageViews.size(); i++) {
            Bitmap bMap = ((BitmapDrawable) imageViews.get(i).getDrawable()).getBitmap();
            bitmapList.add(bMap);
        }
        return bitmapList;
    }
    
    
    /**
     * Saves a bitmap to internal storage (as a PNG) with the specified filename.
     * @param bitmap The bitmap to save
     * @param filename The name to save it as (not including file extension)
     */
    public void saveBitmap(Bitmap bitmap, String filename) {
        FileOutputStream fos; // TODO try wrapping in BufferedOutputStream if slow
        try {
            fos = openFileOutput(filename, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        }
        catch(FileNotFoundException e) { e.printStackTrace(); }
        catch(IOException e) { e.printStackTrace(); }        
    }
    
    
    public void showSavedPicture() {
        clear();
        iv = new ImageView(this);
        previewFrame.setVisibility(View.INVISIBLE);
        Bitmap savedBitmap = null;
        FileInputStream fis;
        try {
            fis = openFileInput("saved_image");
            savedBitmap = BitmapFactory.decodeStream(fis);
            fis.close();
        }
        catch (FileNotFoundException e) { e.printStackTrace(); } 
        catch (IOException e) { e.printStackTrace(); }
        
        LayoutParams params = new LayoutParams(SCREEN_WIDTH, SCREEN_HEIGHT);
        params.leftMargin = 0;
        params.topMargin = 0;
        iv.setLayoutParams(params);        
        relativeLayout.addView(iv);
        iv.setImageBitmap(savedBitmap);
        scrollView.enableScrolling();
    }
    
    /** Creates a filename 
    public String createFilename() {        
        //StringBuffer sb = new StringBuffer(SAVE_FOLDER);
        StringBuffer sb = new StringBuffer();
        sb.append("saved_image_");
        sb.append(String.valueOf(++saveNumber));
        return sb.toString();
    }
    */
    
    /** Get the device dimensions, and set constants for the screen and frame size. */
    public void setDimensions() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        SCREEN_HEIGHT = metrics.heightPixels;
        SCREEN_WIDTH = metrics.widthPixels;        
        FRAME_WIDTH = (SCREEN_WIDTH / 4);
        FRAME_HEIGHT = (SCREEN_HEIGHT / 3);
    }
    
            
    /**
     * Handles touch events for the freeze frames (ImageViews) and the camera
     * preview frame (TextureView).
     */
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        int id = view.getId();
        boolean result;
        switch(id) {
            case 99: // CAMERA PREVIEW FRAME (TextureView)
                // take picture get bitmap
                Bitmap bitmap = previewFrame.getBitmap();
                addImageView(previewColumn, previewRow, bitmap);
                movePreviewFrame();
                result = false;
                break;
            default: // IMAGE FRAMES
                // do nothing if > 4 freeze frames (scrolling enabled, dragging disabled)
                if (scrollView.isScrollable()) { return false; }
                
                // otherwise drag imageview
                ImageView imageView = freezeFrames.get(id);
                result = dragFrame(imageView, event);
                break;
        }
        return result;
    }    
    
    
    public boolean dragFrame(View v, MotionEvent event) {
        LayoutParams params = (LayoutParams) v.getLayoutParams();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: break;
            case MotionEvent.ACTION_MOVE:
                int x = (int) event.getRawX();
                int y = (int) event.getRawY();                
                if (x > SCREEN_WIDTH)  { x = SCREEN_WIDTH; }
                if (y > SCREEN_HEIGHT) { y = SCREEN_HEIGHT; }                 
                params.leftMargin = x - (FRAME_WIDTH / 2);
                params.topMargin = y - (FRAME_HEIGHT / 2);                
                v.setLayoutParams(params);
                break;
            default: break;
        }
        return true;        
    }
    
    
    /** Move the preview frame to its next location */
    public void movePreviewFrame() {
        if (previewColumn <= 2) {
            previewParams.leftMargin = ((++previewColumn) * FRAME_WIDTH);
        }
        else { // shift to next row
            previewColumn = 0;
            previewParams.leftMargin = 0;
            previewParams.topMargin = ((++previewRow) * FRAME_HEIGHT);
        }
        previewFrame.setLayoutParams(previewParams);
    }
    
    
    /**
     * Creates a new ImageView to display a freeze frame (captured picture),
     * placing it in the position of the previewFrame at the time it was
     * touched.
     * @param column The column number where the ImageView should be placed
     * @param row The row number where the ImageView should be placed
     * @param bm The bitmap to set as the ImageView's content
     */
    public View addImageView(int column, int row, Bitmap bm) {        
        ImageView imageView = new ImageView(this);        
        imageView.setOnTouchListener(this);        
        
        LayoutParams imageParams = new LayoutParams(FRAME_WIDTH, FRAME_HEIGHT);
        imageParams.leftMargin = column * FRAME_WIDTH;
        imageParams.topMargin = row * FRAME_HEIGHT;
        imageView.setLayoutParams(imageParams);
        imageView.setImageBitmap(bm);
        imageView.setId(freezeFrames.size());
        freezeFrames.add(imageView);
        // enable scrolling if > 4 freeze frames
        if (freezeFrames.size() > 4) { scrollView.enableScrolling(); }
        
        relativeLayout.addView(imageView);
        return imageView;
    }
    
    /** Removes all freeze frames and moves the preview frame back to its original position */
    public void clear() {
        for (int i = 0; i < freezeFrames.size(); i++) {
            relativeLayout.removeView(freezeFrames.get(i));
        }
        freezeFrames = new ArrayList<ImageView>();
        scrollView.disableScrolling();
        
        // Put preview frame back at (0, 0)
        previewColumn = 0;
        previewRow = 0;
        previewParams = new LayoutParams(FRAME_WIDTH, FRAME_HEIGHT);
        previewParams.leftMargin = 0;
        previewParams.topMargin = 0;
        previewFrame.setLayoutParams(previewParams);
        previewFrame.setVisibility(View.VISIBLE);
        scrollView.disableScrolling();
        if (iv != null) { relativeLayout.removeView(iv); }
    }
    
    /**
     * Provided code for creating a bitmap from a list of bitmaps. Saving the
     * images calls this to stack the frozen frames together into a new bitmap.
     * @param bitmaps An array of bitmaps
     * @return A bitmap containing all the input bitmaps combined & stacked
     */
    private Bitmap combineImageIntoOne(ArrayList<Bitmap> bitmaps) {
        int w = 0;
        int h = 0;
        for (int i = 0; i < bitmaps.size(); i++) {
            if (i < bitmaps.size() - 1) {
                w = bitmaps.get(i).getWidth() > bitmaps.get(i + 1).getWidth() 
                        ? bitmaps.get(i).getWidth() 
                        : bitmaps.get(i + 1).getWidth();
            }
            h += bitmaps.get(i).getHeight();
        }

        Bitmap temp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(temp);
        int top = 0;
        for (int i = 0; i < bitmaps.size(); i++) {
            Log.d("HTML", "Combine: "+i+"/"+bitmaps.size()+1);
            
            top = (i == 0 ? 0 : top+bitmaps.get(i).getHeight());
            canvas.drawBitmap(bitmaps.get(i), 0f, top, null);
        }
        return temp;
    }
         
    
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        camera = Camera.open();
        try {
            camera.setPreviewTexture(surface);
            camera.startPreview();
        }
        catch (IOException e) {}
    }
    
    
    
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            return true;
        }
        return true;
    }
    
    
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { }       
    // Android dev guide: Ignored, camera does all the work for us
    
    
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
    // Invoked every time there's a new Camera preview frame
    
   
    
    
    
}
