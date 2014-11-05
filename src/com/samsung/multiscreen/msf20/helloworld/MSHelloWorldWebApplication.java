package com.samsung.multiscreen.msf20.helloworld;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.os.CountDownTimer;
import android.util.Log;

import com.samsung.multiscreen.MSError;
import com.samsung.multiscreen.MSResult;
import com.samsung.multiscreen.application.MSApplication;
import com.samsung.multiscreen.application.MSWebApplication;
import com.samsung.multiscreen.channel.MSChannel;
import com.samsung.multiscreen.msf20.sdk.MSServiceWrapper;
import com.samsung.multiscreen.service.MSService;
import com.samsung.multiscreen.service.MSServiceDiscovery;
import com.samsung.multiscreen.service.events.MSAddedEvent;
import com.samsung.multiscreen.service.events.MSRemovedEvent;
import com.samsung.multiscreen.util.NetUtil;
import com.samsung.multiscreen.util.RunUtil;
import com.samsung.multiscreen.util.event.MSEventListener;

/**
 * @author plin
 *
 * Encapsulates connection, launch and channel initialization of the Hello 
 * World TV web application.
 * 
 */
public class MSHelloWorldWebApplication {
    public static final String TAG = MSHelloWorldWebApplication.class.getName();

    private static MSHelloWorldWebApplication instance;
    
    private App app;
    private static MSServiceDiscovery discoveryService = MSServiceDiscovery.getInstance();
    private MSServiceWrapper service = null;
    private MSApplication msApplication;
    private MSChannel channel = null;
    
    private ServiceListAdapter serviceListAdapter;

    public static synchronized MSHelloWorldWebApplication getInstance(App app) {
        if (instance != null) {
            return instance;
        }
        instance = new MSHelloWorldWebApplication(app);
        
        return instance;
    }
    
    private MSHelloWorldWebApplication(App app) {
        this.app = app;
        serviceListAdapter = new ServiceListAdapter(app, android.R.layout.select_dialog_singlechoice);
    }
    
    public MSServiceWrapper getService() {
        return service;
    }

    public void setService(MSServiceWrapper service) {
        this.service = service;
    }

    public MSApplication getApplication(MSServiceWrapper service) {
        if ((this.service != null) && 
                (service != null) &&  
                this.service.equals(service)) { 
            
            return msApplication;
        }
        return null;
    }

    private void setApplication(MSApplication msApplication) {
        this.msApplication = msApplication;
    }

    public MSChannel getChannel() {
        return channel;
    }

    public void getWebApplicationInstance(MSService service, 
            final MSResult<MSApplication> callback) {
        // Get a reference to the web application launcher
        if ((msApplication == null) || !msApplication.isConnected()) {
            MSWebApplication.create(service, new MSResult<MSApplication>() {
                
                @Override
                public void onComplete(MSError error, MSApplication msApplication) {
                    setApplication(msApplication);
                    
                    if (callback != null) {
                        callback.onComplete(error, msApplication);
                    }
                }
            });
        } else {
            if (callback != null) {
                callback.onComplete(null, msApplication);
            }
        }
    }

    private MSResult<Boolean> startCallback = new MSResult<Boolean>() {
        @Override
        public void onComplete(MSError error, Boolean success) {
            Log.d(TAG, "ServiceDiscovery start() error: " + error + ", success: " + success);
        }
    };
    private MSResult<Boolean> stopCallback = new MSResult<Boolean>() {
        @Override
        public void onComplete(MSError error, Boolean success) {
            Log.d(TAG, "ServiceDiscovery stop() error: " + error + ", success: " + success);
        }
    };

    private MSEventListener<MSAddedEvent> msAddedEventListener = new MSEventListener<MSAddedEvent>() {
        @Override
        public void on(MSAddedEvent event) {
            final MSService service = event.getService();
            Log.d(TAG, "ServiceDiscovery onAdded() " + service);

            // Add service to a visual list where your user can select.
            // For display, we recommend that you show: service.getDeviceName()
            RunUtil.runOnUI(new Runnable() {

                @Override
                public void run() {
                    MSServiceWrapper wrapper = new MSServiceWrapper(service);
                    if (!serviceListAdapter.contains(wrapper)) {
                        serviceListAdapter.add(wrapper);
                    } else {
                        serviceListAdapter.replace(wrapper);
                    }
                }
            });
        }
    };

    private MSEventListener<MSRemovedEvent> msRemovedEventListener = new MSEventListener<MSRemovedEvent>() {

        @Override
        public void on(MSRemovedEvent event) {
            final MSService service = event.getService();
            Log.d(TAG, "ServiceDiscovery onRemoved() " + service);

            // Remove this service from the display list
            RunUtil.runOnUI(new Runnable() {

                @Override
                public void run() {
                    MSServiceWrapper wrapper = new MSServiceWrapper(service);
                    serviceListAdapter.remove(wrapper);
                }
            });
        }
    };
    
    public void startDiscovery(final MSResult<Boolean> discoveryCallback, 
            final MSEventListener<MSAddedEvent> addCallback, 
            final MSEventListener<MSRemovedEvent> removeCallback) {
        discoveryService = MSServiceDiscovery.getInstance();
        MSServiceDiscovery.Events events = discoveryService.getEvents();
        events.onAdded().add(msAddedEventListener);
        if (addCallback != null) {
            events.onAdded().add(addCallback);;
        }
        events.onRemoved().add(msRemovedEventListener);
        if (removeCallback != null) {
            events.onRemoved().add(removeCallback);
        }
        discoveryService.start(app, startCallback);
        startTimer(app.getResources().getInteger(R.integer.max_discovery_wait), discoveryCallback);
    }
    
    public void stopDiscovery(MSResult<Boolean> stopCallback) {
        if ((discoveryService != null) && discoveryService.isRunning()) {
            discoveryService.stop(stopCallback);
        }
    }
    
    public boolean launch(MSResult<Map<String, Object>> callback) {
        Log.d(TAG, "launch() is called");
        if (msApplication != null) {
            Uri uri = app.getConfig().getWebAppUri();
            msApplication.launch(Uri.decode(uri.toString()), callback);
//            String appId = app.getConfig().getString(R.string.app_id);
//            ((MSApplication)msApplication).launch(appId, callback);
            return true;
        }
        return false;
    }

    public boolean initializeChannel(MSService service, 
            MSResult<Boolean> callback) {

        if (service != null) {
            // Connect to a channel.
            // Note: We recommend that you use a reverse domain style id for your channel to prevent collisions.
            String channelId = app.getConfig().getHelloWorldChannel();
            // Reuse the channel if it is already assigned
            if ((channel == null) || 
                    !service.getId().equals(channel.getService().getId()) ||  
                    !channelId.equals(channel.getChannelId())) {
                channel = new MSChannel(service, channelId);
                channel.enableLogging(true);
            } else {
                MSChannel.Events events = channel.getEvents();
                events.onConnectionLost().clear();
                events.onClientDisconnect().clear();
            }

            InetAddress inetAddr = NetUtil.getDeviceIpAddress(app);

            if (inetAddr != null) {
                Map<String, String> attrs = new HashMap<String, String>();
                attrs.put("name", inetAddr.getHostName());
                channel.connect(attrs, callback);
            } else {
                channel.connect(callback);
            }
            return true;
        }
        return false;
    }
    
    public void resetChannel() {
        if (channel != null) {
            resetChannel(new MSResult<Boolean>() {
                public void onComplete(MSError error, Boolean success) {
                    Log.d(TAG, "MSChannel.disconnect() error: " + error + ", success: " + success);
                    channel.getEvents().onConnectionLost().clear();
                    channel = null;
                }
            });
        }
    }
    
    public void resetChannel(MSResult<Boolean> callback) {
        if ((channel != null) && channel.isConnected()) {
            channel.disconnect(callback);
        } else if (callback != null) {
            callback.onComplete(MSError.create("Not connected"), false);
        }
    }
    
    public void cleanup() {
        if (discoveryService != null) {
            if (discoveryService.isRunning()) {
                discoveryService.stop(new MSResult<Boolean>() {
                    
                    @Override
                    public void onComplete(MSError error, Boolean result) {
                        discoveryService.getEvents().clearListeners();
                    }
                });
            } else{
                discoveryService.getEvents().clearListeners();
            }
        }
        resetChannel(new MSResult<Boolean>() {

            @Override
            public void onComplete(MSError error, Boolean success) {
                Log.d(TAG, "MSChannel.disconnect() error: " + error + ", success: " + success);
                if (channel != null) {
                    channel.getEvents().clearListeners();
                    channel = null;
                }

                if (msApplication != null) {
                    msApplication.shutdown(new MSResult<Boolean>() {

                        @Override
                        public void onComplete(MSError error, Boolean success) {
                            service = null;
                            msApplication = null;
                        }
                    });
                }
            }
        });
    }
    
    private void startTimer(long millis, final MSResult<Boolean> callback) {
        new CountDownTimer(millis, 250) {

            private boolean done = false;
            
            @Override
            public void onTick(long millisUntilFinished) {
                if (!done && (serviceListAdapter.getCount() > 0)) {
                    done = true;
                    callback.onComplete(null, true);
                }
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "Timer finished. Call completeScan()");
                cancel();
                if (!done) {
                    done = true;
                    if (serviceListAdapter.getCount() > 0) {
                        callback.onComplete(null, true);
                    } else {
                        callback.onComplete(null, false);
                    }
                }
                stopDiscovery(stopCallback);
            }
        }.start();
    }

    public ServiceListAdapter getServiceListAdapter() {
        return serviceListAdapter;
    }
}
