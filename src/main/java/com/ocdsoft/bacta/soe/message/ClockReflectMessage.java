package com.ocdsoft.bacta.soe.message;


/**
 * My Struct
 *
     struct UdpConnection::UdpPacketClockReflect {
         char zeroByte;
         char packetType;
         unsigned __int16 timeStamp;
         unsigned int serverSyncStampLong;
         __int64 yourSent;
         __int64 yourReceived;
         __int64 ourSent;
         __int64 ourReceived;
     };

   Sample Message - PreCU
     0000:   00 08 E2 7E A5 1E B4 37 C3 C3 9A 01 0C 94 2E 43   ...~...7.......C
     0010:   68 AB 8F 10 DA 5A 0F 2A 7E 01 00 8E 11 06 0F 01   h....Z.*~.......
     0020:   6C 30                                             l0

         00  - Zero Byte
         08  - Packet Type = Clock Reflect
         E2 7E A5 1E   - TimeStamp =  Reflect value sent

 B4 37 C3 C3 9A 01 0C 94 2E 43 68 AB 8F 10 DA 5A 0F 2A 7E
 01 00 8E 11 06 0F

   Sample Message - NGE - First Response
     0000:   00 08 72 56 2D 0A 99 4A 00 00 00 00 00 00 00 02    ..rV-..J........
     0010:   00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 01    ................
     0020:   00 00 00 00 00 00 00 02 01 A3 DC                   ...........

         00  - Zero Byte
         08  - Packet Type = Clock Reflect
         72 56   - TimeStamp =  Reflect value sent
         2D 0A 99 4A - Server Sync Stamp - 755669322  ms since server start? @8 days
         00 00 00 00 00 00 00 02 - Connection Sent
         00 00 00 00 00 00 00 01 - Connection Received
         00 00 00 00 00 00 00 01 - Server Sent
         00 00 00 00 00 00 00 02 - Server Received

   Sample Message - NGE - Later Response
     0000:   00 08 4B 42 2D 0B 72 45 00 00 00 00 00 00 00 B2    ..KB-.rE........
     0010:   00 00 00 00 00 00 08 02 00 00 00 00 00 00 08 02    ................
     0020:   00 00 00 00 00 00 00 B2 01 10 D6                   ...........

         00  - Zero Byte
         08  - Packet Type = Clock Reflect
         4B 42   - TimeStamp =  Reflect value sent
         2D 0B 72 45 - Server Sync Stamp - 755724869  ms since server start? 55547 ms between
         00 00 00 00 00 00 00 02 - Connection Sent
         00 00 00 00 00 00 00 01 - Connection Received
         00 00 00 00 00 00 00 01 - Server Sent
         00 00 00 00 00 00 00 02 - Server Received


 */
public final class ClockReflectMessage extends SoeMessage {


	public ClockReflectMessage(short value) {
		super(UdpPacketType.cUdpPacketClockReflect);
		
		buffer.putShort(value);
        buffer.putInt(0x0); // ?
        buffer.putLong(0); // Client Sent ?
        buffer.putLong(0); // Client Received ?
        buffer.putLong(0);
        buffer.putLong(0);
	}
}
