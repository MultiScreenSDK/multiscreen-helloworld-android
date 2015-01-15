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

import com.samsung.multiscreen.Application;
import com.samsung.multiscreen.Channel;
import com.samsung.multiscreen.Client;
import com.samsung.multiscreen.Error;
import com.samsung.multiscreen.Result;
import com.samsung.multiscreen.Service;
import com.samsung.multiscreen.msf20.sdk.ServiceWrapper;
import com.samsung.multiscreen.util.RunUtil;

/**
 * @author plin
 *
 * Main Hello World activity.
 * 
 */
public class MainActivity extends Activity {

    private static final String TAG = new Object() {}.getClass().getEnclosingClass().getName();

    private Menu connectMenu;
    private View connectIconActionView;
    private ImageView connectIconImageView;
    
    private AlertDialog listDialog;

    private static ActionBarConnectIcon connectIcon;

    private App app;
    private HelloWorldWebApplicationHelper msHelloWorld;

    private SearchListener searchListener = new SearchListener() {
        
        @Override
        public void onStop() {
            Log.d(TAG, "Search.onStop()");
            invalidateMenu();
            if (connectMenu != null) {
                MenuItem item = connectMenu.findItem(R.id.action_refresh);
                if (item.getActionView() != null) {
                    item.setActionView(null);
                }
                if (!item.isEnabled()) {
                    item.setEnabled(true);
                }
            }
        }
        
        @Override
        public void onStart() {
            Log.d(TAG, "Search.onStart()");
        }

        @Override
        public void onLost(Service service) {
//            Log.d(TAG, "Search.onLost(): " + service.toString());
            invalidateMenu();
        }

        @Override
        public void onFound(Service service) {
//            Log.d(TAG, "Search.onFound(): " + service.toString());
            invalidateMenu();
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = App.getInstance();
        msHelloWorld = app.getHelloWorldWebApplication();

        msHelloWorld.startDiscovery(searchListener);
        
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
        Log.d(TAG, "onCreateOptionsMenu");
        connectMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem item = connectMenu.findItem(R.id.action_connect);
        connectIconActionView = item.getActionView();
        connectIconActionView.setOnClickListener(connectIconOnClickListener);
        connectIconImageView = (ImageView)connectIconActionView.findViewById(R.id.connect_icon);
        connectIcon = new ActionBarConnectIcon(connectIconImageView, ActionBarConnectIcon.LIGHT_THEME);
        
        item = connectMenu.findItem(R.id.action_refresh);
        if (!msHelloWorld.isRunning()) {
            item.setEnabled(true);
        } else {
            item.setEnabled(false);
            item.setActionView(R.layout.refresh);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "onPrepareOptionsMenu");
        invalidateMenu();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_connect) {
            Drawable d = item.getIcon();
            d.setState(new int[] { android.R.attr.state_checkable,android.R.attr.state_enabled });
            item.setIcon(d);
        } else if (id == R.id.action_refresh) {
            item.setEnabled(false);
            item.setActionView(R.layout.refresh);

            msHelloWorld.startDiscovery(searchListener);
        }
        return super.onOptionsItemSelected(item);
    }

    private void invalidateMenu() {
        updateConnectIcon();
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
        invalidateMenu();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause: " + TAG);
        super.onPause();
        invalidateMenu();
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
        Log.d(TAG, "updateConnectIcon: connected: " + ((msHelloWorld.getApplication() != null) && msHelloWorld.getApplication().isChannelConnected()) + ", count: " + (msHelloWorld.getServiceListAdapter().getCount() > 0));
        
        if (connectIconActionView != null) {
            if (connectIcon != null) {
                Application application = msHelloWorld.getApplication();
                if ((application != null) && application.isChannelConnected()) {
                    connectIcon.setConnected();
                    connectIconActionView.setEnabled(true);
                    connectIconActionView.setClickable(true);
                    return;
                }

                if (msHelloWorld.getServiceListAdapter().getCount() > 0) {
                    connectIcon.setEnabled();
                    connectIconActionView.setEnabled(true);
                    connectIconActionView.setClickable(true);
                    return;
                }

                // If we fall through, then no services were found.
                connectIcon.setDisabled();
            }

            connectIconActionView.setEnabled(false);
            connectIconActionView.setClickable(false);
        }
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
                    Result<Channel> result = null;
                    if ((which == selected) && 
                            (msApplication != null) && 
                            msApplication.isChannelConnected()) {
                        result = new Result<Channel>() {

                            @Override
                            public void onSuccess(Channel result) {
                                RunUtil.runOnUI(new Runnable() {

                                    @Override
                                    public void run() {
                                        invalidateMenu();
                                    }
                                });
                            }

                            @Override
                            public void onError(Error error) {
                                RunUtil.runOnUI(new Runnable() {

                                    @Override
                                    public void run() {
                                        invalidateMenu();
                                    }
                                });
                            }
                        };
//                        return;
                    } else if (which != selected) {
                        Log.d(TAG, "Choosing: " + service.getName());

                        final Runnable runnable = new Runnable() {
                            
                            @Override
                            public void run() {
                                // Show icon connecting animation 
                                showConnecting();
                                
                                msHelloWorld.connectAndLaunch(wrapper, 
                                    launchCallback, 
                                    new ChannelListener() {
                                        
                                        private void resetService() {
                                            msHelloWorld.setService(null);
                                            invalidateMenu();
                                            if (listDialog != null) {
                                                ListView listView = listDialog.getListView();
                                                listView.setItemChecked(listView.getCheckedItemPosition(), false);
                                            }
                                        }
                                        
                                        @Override
                                        public void onConnect(Client client) {
                                        }

                                        @Override
                                        public void onDisconnect(Client client) {
                                            invalidateMenu();
                                            resetService();
                                        }

                                        @Override
                                        public void onClientConnect(Client client) {
                                            Log.d(TAG, "ClientConnect: " + client.toString());
                                            if (client.isHost()) {
                                                invalidateMenu();
                                            }
                                        }

                                        @Override
                                        public void onClientDisconnect(Client client) {
                                            Log.d(TAG, "ClientDisconnect: " + client.toString());
                                            if (client.isHost()) {
                                                resetService();
                                            }
                                        }
                                        
                                        @Override
                                        public void onReady() {
                                            Log.d(TAG, "onReady");
                                            showMessage("Ready, set, go!");
                                        }
                                    }
                                );
                            }
                        };
                        
                        if ((msApplication != null) && 
                                msApplication.isConnected()) {
                            result = new Result<Channel>() {

                                private void launch() {
                                    RunUtil.runInBackground(runnable);
                                }
                                
                                @Override
                                public void onSuccess(Channel channel) {
                                    Log.d(TAG, "onSuccess: " + channel.toString());
                                    launch();
                                }

                                @Override
                                public void onError(Error error) {
                                    Log.d(TAG, "onError: " + error.toString());
                                    launch();
                                }
                            };
                        } else {
                            RunUtil.runInBackground(runnable);
                        }
                    }
                    
                    if (result != null) {
                        msHelloWorld.resetChannel(result);
                    }
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
                if (connectIcon != null) {
                    connectIcon.setConnecting();
                }
                if (connectIconActionView != null) {
                    connectIconActionView.setEnabled(false);
                    connectIconActionView.setClickable(false);
                }
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
                    invalidateMenu();
                }
            });
        }

        @Override
        public void onError(Error error) {
            RunUtil.runOnUI(new Runnable() {
                public void run() {
                    msHelloWorld.setService(null);
                    invalidateMenu();
                    showMessage(app.getConfig().getString(R.string.launch_err));
                }
            });
        }
    };
    
}
