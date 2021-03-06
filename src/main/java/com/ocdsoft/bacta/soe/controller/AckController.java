package com.ocdsoft.bacta.soe.controller;

import com.ocdsoft.bacta.soe.connection.SoeUdpConnection;
import com.ocdsoft.bacta.soe.message.UdpPacketType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

@SoeController(handles = {UdpPacketType.cUdpPacketAck1, UdpPacketType.cUdpPacketAck2, UdpPacketType.cUdpPacketAck3, UdpPacketType.cUdpPacketAck4})
public class AckController extends BaseSoeController {

    private static final Logger logger = LoggerFactory.getLogger(AckController.class);

    @Override
    public void handleIncoming(byte zeroByte, UdpPacketType type, SoeUdpConnection connection, ByteBuffer buffer) throws Exception {
        short sequenceNum = buffer.getShort();
        connection.sendAck(sequenceNum);
    }
}
