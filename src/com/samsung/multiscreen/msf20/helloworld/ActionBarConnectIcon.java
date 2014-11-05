package com.samsung.multiscreen.msf20.helloworld;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * @author plin
 *
 * Manages the actionbar connect icon image view.
 * 
 */
public class ActionBarConnectIcon {

    public static final int LIGHT_THEME = 0;
    public static final int DARK_THEME = 1;
    
    private static int DISABLED_STATE = 0;
    private static int ENABLED_STATE = 1;
    private static int CONNECTING_STATE = 2;
    private static int CONNECTED_STATE = 3;
    
    private static int[][] states = {
        {
            R.drawable.mr_ic_media_route_disabled_holo_light,
            R.drawable.mr_ic_media_route_holo_light,
            R.drawable.mr_ic_media_route_connecting_holo_light,
            R.drawable.mr_ic_media_route_on_holo_light
        },
        {
            R.drawable.mr_ic_media_route_disabled_holo_dark,
            R.drawable.mr_ic_media_route_holo_dark,
            R.drawable.mr_ic_media_route_connecting_holo_dark,
            R.drawable.mr_ic_media_route_on_holo_dark
        }
    };
    
    private int theme = LIGHT_THEME;
    private ImageView imageView;
    private int state = DISABLED_STATE;
    
    public ActionBarConnectIcon(ImageView imageView, int theme) {
        this.imageView = imageView;
        if ((theme == LIGHT_THEME) || 
                (theme == DARK_THEME)) {
            this.theme = theme;
        }
    }
    
    public void setDisabled() {
        setState(DISABLED_STATE);
    }
    
    public void setEnabled() {
        setState(ENABLED_STATE);
    }
    
    public void setConnecting() {
        setState(CONNECTING_STATE);
    }
    
    public void setConnected() {
        setState(CONNECTED_STATE);
    }
    
    private void setState(int state) {
        if (imageView != null) { 
            Drawable d = App.getInstance().getResources().getDrawable(states[theme][state]);
            imageView.setImageDrawable(d);
            this.state = state;
        }
    }
    
    public int getState() {
        return this.state;
    }

}
