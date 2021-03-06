package com.ocdsoft.bacta.soe.controller;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.ocdsoft.bacta.engine.utils.UnsignedUtil;
import com.ocdsoft.bacta.soe.connection.SoeUdpConnection;
import com.ocdsoft.bacta.soe.io.udp.SoeTransceiver;
import com.ocdsoft.bacta.soe.message.TerminateReason;
import com.ocdsoft.bacta.soe.message.UdpPacketType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SoeController(handles = {UdpPacketType.cUdpPacketOrdered, UdpPacketType.cUdpPacketOrdered2})
public class OrderedController extends BaseSoeController {

    private static final Logger logger = LoggerFactory.getLogger(OrderedController.class);
    private final Counter rejectedOrderedMessages;

    @Inject
    public OrderedController(final MetricRegistry metrics) {
        rejectedOrderedMessages =  metrics.counter(MetricRegistry.name(SoeTransceiver.class, "message", "rejected-ordered"));
    }

    @Override
    public void handleIncoming(byte zeroByte, UdpPacketType type, SoeUdpConnection connection, ByteBuffer buffer) {

        short orderedStamp = buffer.getShort();
        int diff = orderedStamp - connection.getOrderedStampLast();

        if (diff <= 0) {      // equal here makes it strip dupes too
            diff += 0x10000;
        }
        if (diff < 30000) {
            connection.setOrderedStampLast(orderedStamp);

            buffer.order(ByteOrder.LITTLE_ENDIAN);
            int opcode = buffer.getInt();

            gameNetworkMessageDispatcher.dispatch(zeroByte, opcode, connection, buffer.slice().order(ByteOrder.LITTLE_ENDIAN));

        } else {
            rejectedOrderedMessages.inc();
        }
    }

}
