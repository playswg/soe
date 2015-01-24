package com.ocdsoft.bacta.soe.io.udp.game;

import com.ocdsoft.bacta.engine.network.client.ServerStatus;
import com.ocdsoft.bacta.soe.ServerState;
import com.ocdsoft.bacta.soe.ServerType;

/**
 * Created by Kyle on 3/22/14.
 */
public class PingServerState implements ServerState {

    private final ServerType serverType;
    private ServerStatus serverStatus;

    public PingServerState() {
        serverType = ServerType.PING;
        serverStatus = ServerStatus.DOWN;
    }

    @Override
    public ServerType getServerType() {
        return serverType;
    }

    @Override
    public ServerStatus getServerStatus() {
        return serverStatus;
    }

    @Override
    public void setServerStatus(ServerStatus status) {
        this.serverStatus = status;
    }
}

