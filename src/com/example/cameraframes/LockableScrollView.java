package com.example.cameraframes;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * Extends ScrollView in order to freely enable or disable the scroll feature.
 * @author Navid Rojiani
 * @date Apr 7, 2014
 */
public class LockableScrollView extends ScrollView {

    private boolean scrollable;

    public LockableScrollView(Context context) {
        super(context);
    }      
    
    /** Toggle scrolling on or off */
    public void enableScrolling() { scrollable = true; }    
    public void disableScrolling() { scrollable = false; }    
    /** Check if scrollable */
    public boolean isScrollable() { return scrollable; }
 
    /** Overrides touch event so that super class called only if scrolling is enabled */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (scrollable) { // if scrolling allowed, let parent class handle event
                    return super.onTouchEvent(event);
                }
                return false;
            default: return super.onTouchEvent(event);
        }
    }
    
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Do nothing if not scrollable
        if (!scrollable) { return false; }
        return super.onInterceptTouchEvent(event);
    }
    
}
