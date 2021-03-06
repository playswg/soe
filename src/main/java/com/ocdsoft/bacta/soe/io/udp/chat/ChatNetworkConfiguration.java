package com.ocdsoft.bacta.soe.io.udp.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ocdsoft.bacta.engine.conf.BactaConfiguration;
import com.ocdsoft.bacta.soe.io.udp.BaseNetworkConfiguration;
import com.ocdsoft.bacta.soe.io.udp.NetworkConfiguration;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by kyle on 4/12/2016.
 */

@Singleton
public final class ChatNetworkConfiguration extends BaseNetworkConfiguration implements NetworkConfiguration {

    @Inject
    public ChatNetworkConfiguration(final BactaConfiguration configuration) throws UnknownHostException {
        super(configuration);

        bindIp = InetAddress.getByName(configuration.getString("Bacta/ChatServer", "BindIp"));
        port = configuration.getInt("Bacta/ChatServer", "Port");
        trustedClients = configuration.getStringCollection("Bacta/ChatServer", "TrustedClient");
    }
}
