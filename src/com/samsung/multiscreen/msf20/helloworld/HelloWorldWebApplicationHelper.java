package com.samsung.multiscreen.msf20.helloworld;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.os.CountDownTimer;
import android.util.Log;

import com.samsung.multiscreen.Error;
import com.samsung.multiscreen.Result;
import com.samsung.multiscreen.channel.Application;
import com.samsung.multiscreen.channel.Channel;
import com.samsung.multiscreen.msf20.sdk.ServiceWrapper;
import com.samsung.multiscreen.service.Service;
import com.samsung.multiscreen.service.search.Search;
import com.samsung.multiscreen.service.search.Search.OnServiceFoundListener;
import com.samsung.multiscreen.service.search.Search.OnServiceLostListener;
import com.samsung.multiscreen.util.NetUtil;
import com.samsung.multiscreen.util.RunUtil;

/**
 * @author plin
 *
 * Encapsulates connection, launch and channel initialization of the Hello 
 * World TV web application.
 * 
 */
public class HelloWorldWebApplicationHelper {
    public static final String TAG = HelloWorldWebApplicationHelper.class.getName();

    private static HelloWorldWebApplicationHelper instance;
    
    private App app;
    private static Search search;
    private ServiceWrapper service = null;
    private Application msApplication;
//    private Channel channel = null;
    
    private ServiceListAdapter serviceListAdapter;

    public static synchronized HelloWorldWebApplicationHelper getInstance(App app) {
        if (instance != null) {
            return instance;
        }
        instance = new HelloWorldWebApplicationHelper(app);
        
        return instance;
    }
    
    private HelloWorldWebApplicationHelper(App app) {
        this.app = app;
        search = Service.search(app);
        search.setOnServiceFoundListener(foundListener);
        search.setOnServiceLostListener(lostListener);
        serviceListAdapter = new ServiceListAdapter(app, android.R.layout.select_dialog_singlechoice);
    }
    
    public ServiceWrapper getService() {
        return service;
    }

    public void setService(ServiceWrapper service) {
        this.service = service;
    }

//    public Application getApplication(ServiceWrapper service) {
//        if ((this.service != null) && 
//                (service != null) &&  
//                this.service.equals(service)) { 
//            
//            return msApplication;
//        }
//        return null;
//    }
    public Application getApplication() {
        return msApplication;
    }
    
//    private void setApplication(Application msApplication) {
//        this.msApplication = msApplication;
//    }

//    public Channel getChannel() {
//        return channel;
//    }

//    public void getWebApplicationInstance(Service service, 
//            final MSResult<Application> callback) {
//        // Get a reference to the web application launcher
//        if ((msApplication == null) || !msApplication.isConnected()) {
//            MSWebApplication.create(service, new MSResult<Application>() {
//                
//                @Override
//                public void onComplete(MSError error, Application msApplication) {
//                    setApplication(msApplication);
//                    
//                    if (callback != null) {
//                        callback.onComplete(error, msApplication);
//                    }
//                }
//            });
//        } else {
//            if (callback != null) {
//                callback.onComplete(null, msApplication);
//            }
//        }
//    }
//
//    private MSResult<Boolean> startCallback = new MSResult<Boolean>() {
//        @Override
//        public void onComplete(MSError error, Boolean success) {
//            Log.d(TAG, "ServiceDiscovery start() error: " + error + ", success: " + success);
//        }
//    };
//    private MSResult<Boolean> stopCallback = new MSResult<Boolean>() {
//        @Override
//        public void onComplete(MSError error, Boolean success) {
//            Log.d(TAG, "ServiceDiscovery stop() error: " + error + ", success: " + success);
//        }
//    };
//
    private OnServiceFoundListener foundListener = new OnServiceFoundListener() {
        
        @Override
        public void onFound(final Service service) {
            Log.d(TAG, "ServiceDiscovery onFound() " + service);

            RunUtil.runOnUI(new Runnable() {

                @Override
                public void run() {
                    ServiceWrapper wrapper = new ServiceWrapper(service);
                    if (!serviceListAdapter.contains(wrapper)) {
                        serviceListAdapter.add(wrapper);
                    } else {
                        serviceListAdapter.replace(wrapper);
                    }
                }
            });
            
        }
    };
    
    private OnServiceLostListener lostListener = new OnServiceLostListener() {
        
        @Override
        public void onLost(final Service service) {
            Log.d(TAG, "ServiceDiscovery onRemoved() " + service);

            // Remove this service from the display list
            RunUtil.runOnUI(new Runnable() {

                @Override
                public void run() {
                    ServiceWrapper wrapper = new ServiceWrapper(service);
                    serviceListAdapter.remove(wrapper);
                }
            });
        }
    };
    
    public void startDiscovery(SearchListener searchListener) {
        if (searchListener != null) {
            search.setOnStartListener(searchListener);
            search.setOnStopListener(searchListener);
        }
        search.start();

        startTimer(app.getResources().getInteger(R.integer.max_discovery_wait));
    }
    
    public void stopDiscovery() {
        if ((search != null) && search.isSearching()) {
            search.stop();
        }
    }
    
    public void connectAndLaunch(ServiceWrapper wrapper, 
            Result<Channel> callback, ChannelListener channelListener) {
        Log.d(TAG, "launch() is called");
        this.service = wrapper;
        Service service = wrapper.getService();
        
        Uri uri = app.getConfig().getWebAppUri();
//        String channelId = app.getConfig().getHelloWorldChannel();
        msApplication = service.createApplication(uri);
        msApplication.setStopOnDisconnect(false);
        
        if (channelListener != null) {
            msApplication.setOnConnectListener(channelListener);
            msApplication.setOnDisconnectListener(channelListener);
            msApplication.setOnClientConnectListener(channelListener);
            msApplication.setOnClientDisconnectListener(channelListener);
            msApplication.setOnReadyListener(channelListener);
            msApplication.setOnErrorListener(channelListener);
        }
        // Debug
        msApplication.setDebug(app.getConfig().isDebug());

        InetAddress inetAddr = NetUtil.getDeviceIpAddress(app);

        Map<String, String> attrs = null;
        if (inetAddr != null) {
            attrs = new HashMap<String, String>();
            attrs.put("name", inetAddr.getHostName());
        }
        msApplication.connect(attrs, callback);
    }

    public void resetChannel() {
        if (msApplication != null) {
            resetChannel(new Result<Channel>() {
                
                @Override
                public void onSuccess(Channel channel) {
                    Log.d(TAG, "Channel.disconnect() success: " + channel.toString());
                    msApplication = null;
                    service = null;
                }

                @Override
                public void onError(Error error) {
                    Log.d(TAG, "Channel.disconnect() error: " + error.toString());
                    msApplication = null;
                    service = null;
                }
            });
        }
    }
    
    public void resetChannel(Result<Channel> callback) {
        if (msApplication != null) {
            msApplication.disconnect(callback);
            msApplication = null;
            service = null;
        }
    }
    
    public void cleanup() {
        if (search != null) {
            if (search.isSearching()) {
                search.stop();
            }
            clearSearchListeners();
        }
        resetChannel();
    }
    
    private void clearSearchListeners() {
        if (search != null) {
            search.setOnStartListener(null);
            search.setOnStopListener(null);
        }
    }
    
    private void startTimer(long millis) {
        new CountDownTimer(millis, 250) {

//            private boolean done = false;
            
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "Timer finished. Call completeScan()");
                cancel();
                stopDiscovery();
            }
        }.start();
    }

    public ServiceListAdapter getServiceListAdapter() {
        return serviceListAdapter;
    }
}
