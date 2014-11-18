package com.samsung.multiscreen.msf20.helloworld;

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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.samsung.multiscreen.Error;
import com.samsung.multiscreen.Result;
import com.samsung.multiscreen.channel.Application;
import com.samsung.multiscreen.channel.Channel;
import com.samsung.multiscreen.channel.Channel.OnClientDisconnectListener;
import com.samsung.multiscreen.channel.Client;
import com.samsung.multiscreen.msf20.sdk.ServiceWrapper;
import com.samsung.multiscreen.service.Service;
import com.samsung.multiscreen.util.RunUtil;

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
    private HelloWorldWebApplicationHelper msHelloWorld;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = App.getInstance();
        msHelloWorld = app.getHelloWorldWebApplication();

        msHelloWorld.startDiscovery(new SearchListener() {
            
            @Override
            public void onStop() {
                invalidateOptionsMenu();
            }
            
            @Override
            public void onStart() {
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
                            
                        Application application = msHelloWorld.getApplication();
                        
                        if ((application != null) && application.isChannelConnected()) {
                            application.publish("say", text);
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
        Log.d(TAG, "updateConnectIcon: connected: " + ((msHelloWorld.getApplication() != null) && msHelloWorld.getApplication().isChannelConnected()) + ", count: " + (msHelloWorld.getServiceListAdapter().getCount() > 0));
        
        connectIconActionView.setOnClickListener(connectIconOnClickListener);

        if (connectIcon != null) {
            Application application = msHelloWorld.getApplication();
            if ((application != null) && application.isChannelConnected()) {
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

        ServiceWrapper selectedWrapper = msHelloWorld.getService();
        final ServiceListAdapter serviceListAdapter = msHelloWorld.getServiceListAdapter();
        
        builder
            .setTitle(R.string.services_title)
            .setSingleChoiceItems(serviceListAdapter, 
                    serviceListAdapter.getPosition(selectedWrapper), 
                    new DialogInterface.OnClickListener() {
    
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    listDialog.dismiss();
                    
                    int selected = serviceListAdapter.getPosition(msHelloWorld.getService());
                    final ServiceWrapper wrapper = serviceListAdapter.getItem(which);
                    final Service service = wrapper.getService();
                    final Application msApplication = msHelloWorld.getApplication();

                    // If already connected to the selected service, then 
                    // disconnect. Otherwise, ensure that we disconnect the 
                    // previous connection.
                    if ((which == selected) && 
                            (msApplication != null) && 
                            msApplication.isChannelConnected()) {
                        RunUtil.runInBackground(new Runnable() {
                            
                            @Override
                            public void run() {
                                msHelloWorld.resetChannel(new Result<Channel>() {

                                    @Override
                                    public void onSuccess(Channel result) {
                                        RunUtil.runOnUI(new Runnable() {
                                            
                                            @Override
                                            public void run() {
                                                invalidateOptionsMenu();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(Error error) {
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
                    
                    Log.d(TAG, "Choosing: " + service.getName());

                    RunUtil.runInBackground(new Runnable() {
                        
                        @Override
                        public void run() {
                            msHelloWorld.connectAndLaunch(wrapper, 
                                launchCallback, 
                                new ChannelListener() {
                                    
                                    @Override
                                    public void onConnect(Client client) {
                                    }

                                    @Override
                                    public void onDisconnect(Client client) {
                                    }

                                    @Override
                                    public void onClientConnect(Client client) {
                                        Log.d(TAG, "ClientConnect: " + client.toString());
                                        if (client.isHost()) {
                                            invalidateOptionsMenu();
                                        }
                                    }

                                    @Override
                                    public void onClientDisconnect(Client client) {
                                        Log.d(TAG, "ClientDisconnect: " + client.toString());
                                        if (client.isHost()) {
                                            msHelloWorld.setService(null);
                                            invalidateOptionsMenu();
                                            ListView listView = listDialog.getListView();
                                            listView.setItemChecked(listView.getCheckedItemPosition(), false);
                                        }
                                    }
                                }
                            );
                        }
                    });
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

    private Result<Channel> launchCallback = new Result<Channel>() {

        @Override
        public void onSuccess(Channel channel) {
            Log.d(TAG, "launch Success: " + channel.toString());
            
            RunUtil.runOnUI(new Runnable() {
                public void run() {
                    invalidateOptionsMenu();
                }
            });
        }

        @Override
        public void onError(Error error) {
            RunUtil.runOnUI(new Runnable() {
                public void run() {
                    msHelloWorld.setService(null);
                    invalidateOptionsMenu();
                    showMessage(app.getConfig().getString(R.string.launch_err));
                }
            });
        }
    };
    
}
