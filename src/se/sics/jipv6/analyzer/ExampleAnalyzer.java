package se.sics.jipv6.analyzer;

import se.sics.jipv6.core.ICMP6Packet;
import se.sics.jipv6.core.IPPayload;
import se.sics.jipv6.core.IPv6ExtensionHeader;
import se.sics.jipv6.core.IPv6Packet;
import se.sics.jipv6.core.Packet;
import se.sics.jipv6.core.RPLPacket;
import se.sics.jipv6.core.UDPPacket;
import se.sics.jipv6.mac.IEEE802154Handler;

public class ExampleAnalyzer implements PacketAnalyzer {

    private int dioPacket;
    private int bcDISPacket;
    private int ucDISPacket;
    private int dataPacket;
    private int sleepPacket;
    private int daoPacket;
    private int nsPacket;
    private int totPacket;
    
    /* 802.15.4 stats */
    private int beacon;
    private int ack;
    private int data;
    private int cmd;

    public void init() {
        new Thread(new Runnable() {
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    System.out.printf("Tot:%d DIO:%d ucDIS:%d mcDIS:%d DAO:%d NS:%d Sleep:%d Data:%d 802154: DATA:%d ACK:%d CMD:%d BEACON:%d\n",
                            totPacket,
                            dioPacket, ucDISPacket, bcDISPacket,
                            daoPacket, nsPacket, sleepPacket, dataPacket,
                            data, ack, cmd, beacon);
                }
            }
        }).start();
    }
    
    /* MAC packet received */
    public void analyzePacket(Packet packet) {
        int type = packet.getAttributeAsInt("802154.type");
        switch (type) {
        case IEEE802154Handler.BEACONFRAME:
            beacon++;
            break;
        case IEEE802154Handler.ACKFRAME:
            ack++;
            break;
        case IEEE802154Handler.DATAFRAME:
            data++;
            break;
        case IEEE802154Handler.CMDFRAME:
            cmd++;
            break;
        }
    }
    
    /* IPv6 packet received */
    public void analyzeIPPacket(IPv6Packet packet) {
        IPPayload payload = packet.getIPPayload();
        totPacket++;
        while (payload instanceof IPv6ExtensionHeader) {
            System.out.println("Analyzer - EXT HDR:");
            payload.printPacket(System.out);
            payload = ((IPv6ExtensionHeader) payload).getNext();
        }
        if (payload instanceof UDPPacket) {
            System.out.println("Analyzer - UDP Packet");
            if (IPv6Packet.isLinkLocal(packet.getDestinationAddress())) {
                System.out.print("*** Link Local Message: Possibly Sleep from:");
                IPv6Packet.printAddress(System.out, packet.getSourceAddress());
                System.out.println();
                sleepPacket++;
            } else {
                dataPacket++;
                System.out.println("*** Message to/from Fiona");
            }
        } else if (payload instanceof RPLPacket) {
            RPLPacket rpl = (RPLPacket) payload;
            switch (rpl.getCode()) {
            case RPLPacket.RPL_DIS:
                if (IPv6Packet.isLinkLocal(packet.getDestinationAddress())) {
                    /* ... */
                    System.out.print("*** Probe or repair from ");
                    IPv6Packet.printAddress(System.out, packet.getSourceAddress());
                    System.out.println();
                    ucDISPacket++;
                } else {
                    System.out.print("*** Warning - broadcast DIS!!! from");
                    IPv6Packet.printAddress(System.out, packet.getSourceAddress());
                    System.out.println();
                    bcDISPacket++;
                }
                break;
            case RPLPacket.RPL_DIO:
                dioPacket++;
                break;
            case RPLPacket.RPL_DAO:
                daoPacket++;
                break;
            }
        } else if (payload instanceof ICMP6Packet) {
            ICMP6Packet icmp6 = (ICMP6Packet) payload;
            if (icmp6.getType() == ICMP6Packet.NEIGHBOR_SOLICITATION) {
                System.out.print("*** Warning - Neighbor solicitation!!! from: ");
                IPv6Packet.printAddress(System.out, packet.getSourceAddress());
                System.out.println();
                nsPacket++;
            } else {
            }
        }
    }
}
