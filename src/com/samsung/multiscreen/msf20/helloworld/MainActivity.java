package com.samsung.multiscreen.msf20.helloworld;

import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.samsung.multiscreen.MSError;
import com.samsung.multiscreen.MSResult;
import com.samsung.multiscreen.application.MSWebApplication;
import com.samsung.multiscreen.channel.MSChannel;
import com.samsung.multiscreen.channel.MSChannelClient;
import com.samsung.multiscreen.channel.events.MSClientConnectEvent;
import com.samsung.multiscreen.channel.events.MSClientDisconnectEvent;
import com.samsung.multiscreen.msf20.sdk.MSServiceWrapper;
import com.samsung.multiscreen.service.MSService;
import com.samsung.multiscreen.service.events.MSAddedEvent;
import com.samsung.multiscreen.service.events.MSRemovedEvent;
import com.samsung.multiscreen.util.RunUtil;
import com.samsung.multiscreen.util.event.MSEventListener;

/**
 * @author plin
 *
 * Main Hello World activity.
 * 
 */
public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getName();

    private Menu connectMenu;
    private View connectIconActionView;
    private ImageView connectIconImageView;
    
    private AlertDialog listDialog;

    private static ActionBarConnectIcon connectIcon;

    private App app;
    private MSHelloWorldWebApplication msHelloWorld;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = App.getInstance();
        msHelloWorld = app.getHelloWorldWebApplication();

        msHelloWorld.startDiscovery(new MSResult<Boolean>() {

            @Override
            public void onComplete(MSError error, Boolean result) {
                invalidateOptionsMenu();
            };
        }, new MSEventListener<MSAddedEvent>() {

            @Override
            public void on(MSAddedEvent event) {
                invalidateOptionsMenu();
            }
        }, new MSEventListener<MSRemovedEvent>() {

            @Override
            public void on(MSRemovedEvent event) {
                invalidateOptionsMenu();
            }
        });
        
        final EditText editText = (EditText) findViewById(R.id.sendText);
        editText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    String text = v.getText().toString();
                    if (!TextUtils.isEmpty(text)) {
                            
                        MSChannel channel = msHelloWorld.getChannel();
                        
                        if ((channel != null) && channel.isChannelConnected()) {
                            channel.sendMessage("say", text);
                        }
                    }
                    handled = true;
                }
                return handled;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        connectMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        
        MenuItem item = connectMenu.findItem(R.id.action_connect);
        connectIconActionView = item.getActionView();
        connectIconImageView = (ImageView)connectIconActionView.findViewById(R.id.connect_icon);
        connectIcon = new ActionBarConnectIcon(connectIconImageView, ActionBarConnectIcon.LIGHT_THEME);
        
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "onPrepareOptionsMenu");
        updateConnectIcon();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_connect) {
            Drawable d = item.getIcon();
            d.setState(new int[] { android.R.attr.state_checkable,android.R.attr.state_enabled });
            item.setIcon(d);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart: " + TAG);
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume: " + TAG);
        super.onResume();
        invalidateOptionsMenu();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause: " + TAG);
        super.onPause();
        invalidateOptionsMenu();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: " + TAG);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: " + TAG);
        super.onDestroy();
        msHelloWorld.cleanup();
    }

    private OnClickListener connectIconOnClickListener = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            
            showServices();
        }
    };
    
    private void updateConnectIcon() {
//        MenuItem item = connectMenu.findItem(R.id.action_connect);
//        View view = item.getActionView();
//        ImageView iv = (ImageView)view.findViewById(R.id.connect_icon);
        connectIconActionView.setOnClickListener(connectIconOnClickListener);

        if (connectIcon != null) {
            MSChannel channel = msHelloWorld.getChannel();
            if ((channel != null) && channel.isChannelConnected()) {
                connectIcon.setConnected();
                connectIconActionView.setEnabled(true);
                return;
            }

            if (msHelloWorld.getServiceListAdapter().getCount() > 0) {
                connectIcon.setEnabled();
                connectIconActionView.setEnabled(true);
                return;
            }
            
            // If we fall through, then no services were found.
            connectIcon.setDisabled();
        }
        
        connectIconActionView.setEnabled(false);
        connectIconActionView.setOnClickListener(null);
    }
    
    /**
     * Show the list of services found during discovery. 
     */
    private void showServices() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        MSServiceWrapper selectedWrapper = msHelloWorld.getService();
        final ServiceListAdapter serviceListAdapter = msHelloWorld.getServiceListAdapter();
        final int selected = serviceListAdapter.getPosition(selectedWrapper);
        
        builder
            .setTitle(R.string.services_title)
            .setSingleChoiceItems(serviceListAdapter, selected, new DialogInterface.OnClickListener() {
    
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    listDialog.dismiss();
                    
                    final MSServiceWrapper wrapper = serviceListAdapter.getItem(which);
                    final MSService service = wrapper.getService();
                    final MSWebApplication webApplication = msHelloWorld.getWebApplication(wrapper);
                    final MSChannel channel = msHelloWorld.getChannel();

                    // If already connected to the selected service, then 
                    // disconnect. Otherwise, ensure that we disconnect the 
                    // previous connection.
                    if ((which == selected) && 
                            (channel != null) && 
                            channel.isChannelConnected()) {
                        RunUtil.runInBackground(new Runnable() {
                            
                            @Override
                            public void run() {
                                msHelloWorld.resetChannel(new MSResult<Boolean>() {

                                    @Override
                                    public void onComplete(MSError error,
                                            Boolean result) {
                                        msHelloWorld.setService(null);

                                        if (channel != null) {
                                            channel.getEvents().clearListeners();
                                        }

                                        RunUtil.runOnUI(new Runnable() {
                                            
                                            @Override
                                            public void run() {
                                                invalidateOptionsMenu();
                                            }
                                        });
                                    }
                                });
                            }
                        });
                        return;
                    } else if (which != selected) {
                        msHelloWorld.resetChannel();
                    }
                    
                    // Show icon connecting animation 
                    showConnecting();
                    
                    Log.d(TAG, "Choosing: " + service.getDeviceName());

                    // Get a web application instance for launching the Hello World 
                    // app. If previously launched and an instance exists, then 
                    // just use the current application channel and re-launch 
                    // the app.
                    Runnable runnable;
                    if (webApplication == null) {
                        msHelloWorld.setService(wrapper);
                        runnable = new Runnable() {
                            
                            @Override
                            public void run() {
                                msHelloWorld.getWebApplicationInstance(service, getWebAppCallback);
                            }
                        };
                    } else {
                        runnable = new Runnable() {
                            
                            @Override
                            public void run() {
                                msHelloWorld.launch(webApplication, launchCallback);
                            }
                        };
                    }

                    RunUtil.runInBackground(runnable);
                }
            })
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
    
                @Override
                public void onCancel(DialogInterface dialog) {
                    Log.d(TAG, "Canceled");
                }
            });
        listDialog = builder.create();
        listDialog.show();
    }

    /**
     * Show the connecting icon while the device is connecting to the selected 
     * service.
     */
    private void showConnecting() {
        RunUtil.runOnUI(new Runnable() {
            
            @Override
            public void run() {
//                MenuItem item = connectMenu.findItem(R.id.action_connect);
//                View v = item.getActionView();
//                ImageView iv = (ImageView)v.findViewById(R.id.connect_icon);
                connectIcon.setConnecting();
                connectIconActionView.setEnabled(false);
            }
        });
    }
    
    private void showMessage(String message) {
        Toast.makeText(this, 
                message, 
                Toast.LENGTH_LONG).show();
    }

    MSResult<MSWebApplication> getWebAppCallback = new MSResult<MSWebApplication>() {
        public void onComplete(MSError error, final MSWebApplication webApplication) {
            Log.d(TAG, "MSWebApplication.getWebApplication() error: " + error + ", webApplication: " + webApplication);
            if (webApplication != null) {
                // Launch the Hello World app, if we got a web application 
                // instance and the application channel started successfully.
                RunUtil.runInBackground(new Runnable() {
                    
                    @Override
                    public void run() {
                        msHelloWorld.launch(webApplication, launchCallback);
                    }
                });
            } else {
                // Update the connect icon on error
                RunUtil.runOnUI(new Runnable() {
                    public void run() {
                        invalidateOptionsMenu();
                        showMessage(app.getConfig().getString(R.string.web_launcher_err));       
                    }
                });
            }
        }
    };

    private MSResult<Map<String, Object>> launchCallback = new MSResult<Map<String, Object>>() {
        @Override
        public void onComplete(MSError error, Map<String, Object> result) {
            Log.d(TAG, "webApplication.launch() error: " + error + ", result: " + result);
            Boolean success = Boolean.FALSE;
            if (result != null) {
                try {
                    success = (Boolean)result.get(MSChannel.PROPERTY_RESULT);
                } catch (Exception e) {
                }
            }
            if (success) {
                // Initialize the Hello World 
                RunUtil.runInBackground(new Runnable() {
                    
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                        msHelloWorld.initializeChannel(msHelloWorld.getService().getService(), 
                                initializeChannelCallback);
                    }
                });
            } else {
                RunUtil.runOnUI(new Runnable() {
                    public void run() {
                        invalidateOptionsMenu();
                        showMessage(app.getConfig().getString(R.string.launch_err));
                    }
                });
            }
        }
    };
    
    private MSResult<Boolean> initializeChannelCallback = new MSResult<Boolean>() {
        public void onComplete(MSError error, final Boolean success) {
            Log.d(TAG, "MSChannel.connect() error: " + error + ", success: " + success);
            RunUtil.runOnUI(new Runnable() {
                
                @Override
                public void run() {
                    invalidateOptionsMenu();
                    if (success) {
                        MSChannel.Events events = msHelloWorld.getChannel().getEvents();
                        events.onClientConnect().add(new MSEventListener<MSClientConnectEvent>() {
                            
                            @Override
                            public void on(MSClientConnectEvent event) {
                                MSChannelClient client = event.getClient();
                                if (client.isHost()) {
                                    invalidateOptionsMenu();
                                }
                            }
                        });
                        events.onClientDisconnect().add(new MSEventListener<MSClientDisconnectEvent>() {
                            
                            @Override
                            public void on(MSClientDisconnectEvent event) {
                                MSChannelClient client = event.getClient();
                                if (client.isHost()) {
                                    invalidateOptionsMenu();
                                    msHelloWorld.setService(null);
                                }
                            }
                        });
                    } else {
                        msHelloWorld.setService(null);
                    }
                }
            });
        }
    };
}
