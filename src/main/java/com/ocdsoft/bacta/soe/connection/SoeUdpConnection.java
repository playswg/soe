package com.ocdsoft.bacta.soe.connection;

import com.ocdsoft.bacta.engine.network.client.ConnectionState;
import com.ocdsoft.bacta.engine.network.client.UdpConnection;
import com.ocdsoft.bacta.engine.network.client.UdpMessageProcessor;
import com.ocdsoft.bacta.soe.message.*;
import com.ocdsoft.bacta.soe.util.SoeMessageUtil;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SoeUdpConnection extends UdpConnection {

    private static final Logger logger = LoggerFactory.getLogger(SoeUdpConnection.class);
    private static ResourceBundle messageProperties;

    static {
        messageProperties = ResourceBundle.getBundle("messageprocessing");
    }

    @Getter
    @Setter
    private int id;

    @Getter
    @Setter
    private int udpSize;

    @Getter
    @Setter
    private int sessionKey;

    @Getter
    @Setter
    private int accountId;

    @Getter
    @Setter
    private String accountUsername;

    private final int staleTimeout;

    private ConnectionState state;

    private final UdpMessageProcessor<ByteBuffer> udpMessageProcessor;

    private final AtomicInteger clientSequenceNumber;

    private final FragmentContainer fragmentContainer;

    @Getter
    private long lastActivity;

//    private final ByteBuffer outgoingBuffer;

    public SoeUdpConnection() {
        state = ConnectionState.DISCONNECTED;
        udpMessageProcessor = new SoeUdpMessageProcessor(this, messageProperties);
        staleTimeout = Integer.parseInt(messageProperties.getString("staleDisconnect"));
        clientSequenceNumber = new AtomicInteger();
        fragmentContainer = new FragmentContainer();
        lastActivity = System.currentTimeMillis();

//        int maxQueueSize = Integer.parseInt(messageProperties.getString("MaxQueueSize"));
//        int udpMaxSize = Integer.parseInt(messageProperties.getString("UdpMaxSize"));
//        outgoingBuffer = ByteBuffer.allocate(udpMaxSize * maxQueueSize);
    }

    public void sendMessage(SoeMessage message) {
        if (udpMessageProcessor.addUnreliable(message.slice())) {
            lastActivity = System.currentTimeMillis();
        }
    }

    public void sendMessage(GameNetworkMessage message) {

        ByteBuffer buffer = ByteBuffer.allocate(1500);

        buffer.putShort(message.getPriority());
        buffer.putInt(message.getMessageType());

        message.writeToBuffer(buffer);

        if (!udpMessageProcessor.addReliable(buffer)) {
            if(getState() == ConnectionState.ONLINE) {
                setState(ConnectionState.DISCONNECTED);
            }
        } else {
            lastActivity = System.currentTimeMillis();
        }
    }

    public List<ByteBuffer> getPendingMessages() {

        List<ByteBuffer> pendingMessageList = new ArrayList<>();

        ByteBuffer buffer;
        while ((buffer = udpMessageProcessor.processNext()) != null) {
            pendingMessageList.add(buffer);
            logger.trace("Sending: " + SoeMessageUtil.bytesToHex(buffer));
        }

        if(!pendingMessageList.isEmpty()) {
            lastActivity = System.currentTimeMillis();
        }

        return pendingMessageList;
    }

    public void sendAck(short sequenceNum) {
        lastActivity = System.currentTimeMillis();
        sendMessage(new AckAllMessage(sequenceNum));
    }

    public void processAckAll(short sequenceNum) {
        clientSequenceNumber.set(sequenceNum);
        udpMessageProcessor.acknowledge(sequenceNum);
    }

    /**
     * Idle timeout
     *
     * @return
     */
    public boolean isStale() {
        return (System.currentTimeMillis() - lastActivity > (staleTimeout));
    }

    @Override
    public void setState(ConnectionState state) {
        this.state = state;

        if(state == ConnectionState.DISCONNECTED) {
            Terminate terminate = new Terminate(this.getId(), TerminateReason.NONE);
            sendMessage(terminate);
        }
    }

    @Override
    public ConnectionState getState() {
        return state;
    }

    public ByteBuffer addIncomingFragment(ByteBuffer buffer) {

        ByteBuffer completedMessage = fragmentContainer.addFragment(buffer);

        if (completedMessage != null) {
            return completedMessage;
        }

        return null;
    }

    @SuppressWarnings("serial")
    private class FragmentContainer {

        private final Queue<ByteBuffer> queue = new PriorityBlockingQueue<>();

        private int firstFragment = 0;
        private int lastFragment = 0;

        public ByteBuffer addFragment(ByteBuffer buffer) {
            queue.add(buffer);
            return null;
        }

    }

//    013CA650	UdpManager::UdpManager(UdpManager::Params const *)
//    013CAEB0	UdpManager::~UdpManager(void)
//    013CB150	UdpManager::CreateAndBindSocket(int)
//    013CB370	UdpManager::CloseSocket(void)
//    013CB3C0	UdpManager::GetErrorCondition(void)
//    013CB3E0	UdpManager::ProcessDisconnectPending(void)
//    013CB480	UdpManager::RemoveConnection(UdpConnection *)
//    013CB5E0	UdpManager::AddConnection(UdpConnection *)
//    013CB700	UdpManager::FlushAllMultiBuffer(void)
//    013CB760	UdpManager::GiveTime(int,bool)
//    013CBA90	UdpManager::EstablishConnection(char const *,int,int)
//    013CBC40	UdpManager::KeepUntilDisconnected(UdpConnection *)
//    013CBC90	UdpManager::GetStats(UdpManagerStatistics *)
//    013CBD30	UdpManager::ResetStats(void)
//    013CBD80	UdpManager::DumpPacketHistory(char const *)
//    013CBF80	UdpManager::GetLocalIp(void)
//    013CC040	UdpManager::GetLocalPort(void)
//    013CC100	UdpManager::ActualReceive(void)
//    013CC470	UdpManager::ProcessIcmpErrors(void)
//    013CC490	UdpManager::ActualSend(uchar const *,int,UdpIpAddress,int)
//    013CC6A0	UdpManager::ActualSendHelper(uchar const *,int,UdpIpAddress,int)
//    013CC7B0	UdpManager::SendPortAlive(UdpIpAddress,int)
//    013CC880	UdpManager::ProcessRawPacket(UdpManager::PacketHistoryEntry const *)
//    013CCC30	UdpManager::AddressGetConnection(UdpIpAddress,int)
//    013CCD20	UdpManager::ConnectCodeGetConnection(int)
//    013CCDF0	UdpManager::WrappedBorrow(LogicalPacket const *)
//    013CCF00	UdpManager::WrappedCreated(WrappedLogicalPacket *)
//    013CCF60	UdpManager::WrappedDestroyed(WrappedLogicalPacket *)
//    013CCFF0	UdpManager::CreatePacket(void const *,int,void const *,int)
//    013CD160	UdpManager::PoolCreated(PooledLogicalPacket *)
//    013CD1C0	UdpManager::PoolDestroyed(PooledLogicalPacket *)
//    013CD250	UdpManager::PacketHistoryEntry::PacketHistoryEntry(int)
//    013CD2C0	UdpManager::PacketHistoryEntry::~PacketHistoryEntry(void)

//    013CD300	UdpConnection::UdpConnection(UdpManager *,UdpIpAddress,int,int)
//    013CD390	UdpConnection::UdpConnection(UdpManager *,UdpConnection::PacketHistoryEntry const *)
//    013CD4B0	UdpConnection::Init(UdpManager *,UdpIpAddress,int)
//    013CD770	UdpConnection::~UdpConnection(void)
//    013CD840	UdpConnection::PortUnreachable(void)
//    013CD900	UdpConnection::InternalDisconnect(int,UdpConnection::DisconnectReason)
//    013CDA70	UdpConnection::SendTerminatePacket(int,UdpConnection::DisconnectReason)
//    013CDB30	UdpConnection::SetSilentDisconnect(bool)
//    013CDB50	UdpConnection::Send(UdpChannel,void const *,int)
//    013CDCE0	UdpConnection::Send(UdpChannel,LogicalPacket const *)
//    013CDEA0	UdpConnection::InternalSend(UdpChannel,uchar const *,int,uchar const *,int)
//    013CE2C0	UdpConnection::InternalSend(UdpChannel,LogicalPacket const *)
//    013CE420	UdpConnection::PingStatReset(void)
//    013CE4F0	UdpConnection::GetStats(UdpConnectionStatistics *)
//    013CE660	UdpConnection::ProcessRawPacket(UdpManager::PacketHistoryEntry const *)
//    013CEC60	UdpConnection::CallbackRoutePacket(uchar const *,int)
//    013CED20	UdpConnection::CallbackCorruptPacket(uchar const *,int,UdpCorruptionReason)
//    013CEDD0	UdpConnection::ProcessCookedPacket(uchar const *,int)
//    013CFE30	UdpConnection::FlushChannels(void)
//    013CFE70	UdpConnection::FlagPortUnreachable(void)
//    013CFE90	UdpConnection::GiveTime(void)
//    013CFF00	UdpConnection::InternalGiveTime(void)
//    013D0840	UdpConnection::TotalPendingBytes(void)
//    013D08C0	UdpConnection::RawSend(uchar const *,int)
//    013D09D0	UdpConnection::ExpireSendBin(void)
//    013D0AC0	UdpConnection::ExpireReceiveBin(void)
//    013D0BB0	UdpConnection::PhysicalSend(uchar const *,int,bool)
//    013D1020	UdpConnection::BufferedSend(uchar const *,int,uchar const *,int,bool)
//    013D1290	UdpConnection::InternalAckSend(uchar *,uchar const *,int)
//    013D12F0	UdpConnection::FlushMultiBuffer(void)
//    013D13E0	UdpConnection::EncryptNone(uchar *,uchar const *,int)
//    013D1420	UdpConnection::DecryptNone(uchar *,uchar const *,int)
//    013D1460	UdpConnection::EncryptUserSupplied(uchar *,uchar const *,int)
//    013D14F0	UdpConnection::DecryptUserSupplied(uchar *,uchar const *,int)
//    013D1580	UdpConnection::EncryptUserSupplied2(uchar *,uchar const *,int)
//    013D1610	UdpConnection::DecryptUserSupplied2(uchar *,uchar const *,int)
//    013D16A0	UdpConnection::EncryptXorBuffer(uchar *,uchar const *,int)
//    013D1770	UdpConnection::DecryptXorBuffer(uchar *,uchar const *,int)
//    013D1850	UdpConnection::EncryptXor(uchar *,uchar const *,int)
//    013D1900	UdpConnection::DecryptXor(uchar *,uchar const *,int)
//    013D19B0	UdpConnection::SetupEncryptModel(void)
//    013D1D40	UdpConnection::GetChannelStatus(UdpChannel,UdpConnection::ChannelStatus *)
//    013D1DC0	UdpConnection::DisconnectReasonText(UdpConnection::DisconnectReason)
}