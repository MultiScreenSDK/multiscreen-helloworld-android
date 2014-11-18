package com.samsung.multiscreen.msf20.helloworld;

import com.samsung.multiscreen.Error;
import com.samsung.multiscreen.channel.Channel.OnClientConnectListener;
import com.samsung.multiscreen.channel.Channel.OnClientDisconnectListener;
import com.samsung.multiscreen.channel.Channel.OnConnectListener;
import com.samsung.multiscreen.channel.Channel.OnDisconnectListener;
import com.samsung.multiscreen.channel.Channel.OnErrorListener;
import com.samsung.multiscreen.channel.Channel.OnMessageListener;
import com.samsung.multiscreen.channel.Channel.OnReadyListener;
import com.samsung.multiscreen.channel.Client;
import com.samsung.multiscreen.channel.Message;

public abstract class ChannelListener implements OnConnectListener,
        OnDisconnectListener, OnClientConnectListener,
        OnClientDisconnectListener, OnErrorListener, OnReadyListener {

    @Override
    public void onError(Error error) {
    }

    @Override
    public void onClientDisconnect(Client client) {
    }

    @Override
    public void onClientConnect(Client client) {
    }

    @Override
    public void onDisconnect(Client client) {
    }

    @Override
    public void onConnect(Client client) {
    }

    @Override
    public void onReady() {
    }
}
