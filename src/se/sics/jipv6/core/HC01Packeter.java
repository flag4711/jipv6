/**
 * Copyright (c) 2009, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of jipv6.
 *
 * $Id: $
 *
 * -----------------------------------------------------------------
 *
 *
 * Author  : Joakim Eriksson
 * Created :  mar 2009
 * Updated : $Date:$
 *           $Revision:$
 */

package se.sics.jipv6.core;

import se.sics.jipv6.util.Utils;

public class HC01Packeter implements IPPacketer {

  private boolean DEBUG = false;//true;
  /*
   * Values of fields within the IPHC encoding first byte
   * (C stands for compressed and I for inline)
   */
  public final static int IPHC_TC_C = 0x80;
  public final static int IPHC_VF_C = 0x40;
  public final static int IPHC_NH_C = 0x20;  
  public final static int IPHC_TTL_1 =  0x08;
  public final static int IPHC_TTL_64 = 0x10;
  public final static int IPHC_TTL_255 = 0x18;
  public final static int IPHC_TTL_I    = 0x00;

  /* Values of fields within the IPHC encoding second byte */
  public final static int IPHC_SAM_I =  0x00;
  public final static int IPHC_SAM_64 =  0x40;
  public final static int IPHC_SAM_16 =  0x80;
  public final static int IPHC_SAM_0 = 0xC0;
  public final static int IPHC_DAM_I = 0x00;
  public final static int IPHC_DAM_64 = 0x04;
  public final static int IPHC_DAM_16 = 0x08;
  public final static int IPHC_DAM_0 = 0x0C;

  public final static int NHC_UDP_ID =  0xF8;
  public final static int NHC_UDP_C =  0xFB;
  public final static int NHC_UDP_I = 0xF8;

  /* Link local context number */
  public final static int IPHC_ADDR_CONTEXT_LL = 0;
  /* 16-bit multicast addresses compression */
  public final static int IPHC_MCAST_RANGE = 0xA0;
  
  /* Min and Max compressible UDP ports */
  public final static int UDP_PORT_MIN = 0xF0B0;
  public final static int UDP_PORT_MAX = 0xF0BF;   /* F0B0 + 15 */
  
  public static final int HC01_DISPATCH = 0x03;

  /* move these to IPv6 Packet !! */
  public final static int PROTO_ICMP  = 1;
  public final static int PROTO_TCP   = 6;
  public final static int PROTO_UDP   = 17;
  public final static int PROTO_ICMP6 = 58;
  
  private static class AddrContext {
    int used;
    int number;
    byte[] prefix = new byte[8];
    
    public boolean matchPrefix(byte[] address) {
      for (int i = 0; i < prefix.length; i++) {
        if (prefix[i] != address[i])
          return false;
      }
      return true;
    }
  }

  private AddrContext[] contexts = new AddrContext[4];

  public HC01Packeter() {
    // set-up some fake contexts just to get started...
    contexts[0] = new AddrContext();
    contexts[1] = new AddrContext();
//    contexts[2] = new AddrContext();
//    contexts[3] = new AddrContext();

    contexts[0].number = 0;
    contexts[0].prefix[0] = (byte) 0xfe;
    contexts[0].prefix[1] = (byte) 0x80;

    contexts[1].number = 1;
    contexts[1].prefix[0] = (byte) 0xaa;
    contexts[1].prefix[1] = (byte) 0xaa;
    
  }

  public byte getDispatch() {
    return HC01_DISPATCH;
  }

  /**
   * \brief check whether we can compress the IID in
   * address to 16 bits.
   * This is used for unicast addresses only, and is true
   * if first 49 bits of IID are 0
   * @return 
   */
  private boolean is16bitCompressable(byte[] address) {
    return ((address[8] | address[9] | address[10] | address[11] |
        address[12] | address[13]) == 0) &&
        (address[14] & 0x80) == 0;
  }
     
  /**
   * \brief check whether the 9-bit group-id of the
   * compressed multicast address is known. It is true
   * if the 9-bit group is the all nodes or all routers
   * group.
   * \param a is typed u8_t *
   */
//  #define sicslowpan_is_mcast_addr_decompressable(a) \
//     (((*a & 0x01) == 0) &&                           \
//      ((*(a + 1) == 0x01) || (*(a + 1) == 0x02)))

  /**
   * \brief check whether the 112-bit group-id of the
   * multicast address is mappable to a 9-bit group-id
   * It is true if the group is the all nodes or all
   * routers group.
  */
//  #define sicslowpan_is_mcast_addr_compressable(a) \
//    ((((a)->u16[1]) == 0) &&                       \
//     (((a)->u16[2]) == 0) &&                       \
//     (((a)->u16[3]) == 0) &&                       \
//     (((a)->u16[4]) == 0) &&                       \
//     (((a)->u16[5]) == 0) &&                       \
//     (((a)->u16[6]) == 0) &&                       \
//     (((a)->u8[14]) == 0) &&                       \
//     ((((a)->u8[15]) == 1) || (((a)->u8[15]) == 2)))
  
  /* before calling this a call to the routing mechanism needs to be done
   * to get the LL addresses. */
  /* HC01 header compression from 40 bytes to less... */
  public byte[] generatePacketData(IPv6Packet packet) {
    int enc1 = 0, enc2 = 0;
    byte[] data = new byte[40 + 8];
    int pos = 2;
    
    if (packet.flowLabel == 0) {
      /* compress version and flow label! */
      enc1 |= IPHC_VF_C;
    }
    if (packet.trafficClass == 0) {
      enc1 |=  IPHC_TC_C;
    }

    /* write version and flow if needed */ 
    if ((enc1 & IPHC_VF_C) == 0) {
      pos += writeVFlow(packet, data, pos);
    }
    /* write traffic class if needed */
    if ((enc1 & IPHC_TC_C) == 0) {
      data[pos++] = (byte) (packet.trafficClass & 0xff);
    }
    
    /* Note that the payload length is always compressed */

    /* Compress NextHeader if UDP!!! */
    if (packet.nextHeader == UDPPacket.DISPATCH) {
      enc1 |= IPHC_NH_C;
    } else {
      data[pos++] = (byte) (packet.nextHeader & 0xff);
    }
    
    switch (packet.hopLimit) {
    case 1:
      enc1 |= IPHC_TTL_1;
      break;
    case 64:
      enc1 |= IPHC_TTL_64;
      break;
    case 255:
      enc1 |= IPHC_TTL_255;
      break;
    default:
      data[pos++] = (byte) (packet.hopLimit & 0xff);
    }
    
    int context;
    if ((context = lookupContext(packet.sourceAddress)) != -1) {
      if (DEBUG) System.out.println("HC01: Found context (SRC): " + context);
      /* elide the prefix */
      enc2 |= context << 4;
      if (packet.isSourceMACBased()) {
        /* elide the IID */
        enc2 |= IPHC_SAM_0;
      } else if (is16bitCompressable(packet.sourceAddress)){
        enc2 |= IPHC_SAM_16;
        data[pos++] = packet.sourceAddress[14];
        data[pos++] = packet.sourceAddress[15];
      } else {
        enc2 |= IPHC_SAM_64;
        System.arraycopy(packet.sourceAddress, 8, data, pos, 8);
        pos += 8;
      }
    } else {
      if (DEBUG) System.out.println("HC01: no context - use full addr (SRC)");
      enc2 |= IPHC_SAM_I;
      System.arraycopy(packet.sourceAddress, 0, data, pos, 16);
      pos += 16;
    }
    
    /* destination  compression */
    // System.out.print("isMulticastCompressable?: ");
    // IPv6Packet.printAddress(System.out, packet.destAddress);

    if(packet.isMulticastDestination()) {
      /* Address is multicast, try to compress */
      if(isMulticastCompressable(packet.destAddress)) {
        enc2 |= IPHC_DAM_16;
        /* 3 first bits = 101 */
        data[pos] = (byte) IPHC_MCAST_RANGE;
        /* bits 3-6 = scope = bits 8-11 in 128 bits address */
        data[pos++] |= (packet.destAddress[1] & 0x0F) << 1;
        /*
         * bits 7 - 15 = 9-bit group
         * We just copy the last byte because it works
         * with currently supported groups
         */
        data[pos++] = packet.destAddress[15];
      } else {
        /* send the full address */
        enc2 |= IPHC_DAM_I;
        System.arraycopy(packet.destAddress, 0, data, pos, 16);
        pos += 16;
      }
    } else {
      /* Address is unicast, try to compress */
      if((context = lookupContext(packet.destAddress)) != -1) {
        if (DEBUG) System.out.println("HC01: Found context (DST): " + context);
        /* elide the prefix */        
        enc2 |= context;
        if(packet.isDestinationMACBased()) {
          /* elide the IID */
          enc2 |= IPHC_DAM_0;
        } else {
          if(is16bitCompressable(packet.destAddress)) {
            /* compress IID to 16 bits */
            enc2 |= IPHC_DAM_16;
            data[pos++] = packet.destAddress[14];
            data[pos++] = packet.destAddress[15];
          } else {
            /* do not compress IID */
            enc2 |= IPHC_DAM_64;
            System.arraycopy(packet.destAddress, 8, data, pos, 8);
            pos += 8;
          }
        }
      } else {
        /* send the full address */
        if (DEBUG) System.out.println("HC01: full destination address");
        enc2 |= IPHC_DAM_I;
        System.arraycopy(packet.destAddress, 0, data, pos, 16);
        pos += 16;
      }
    }
    
    // uncomp_hdr_len = UIP_IPH_LEN;
    // TODO: add udp header compression!!! 
    
    /* UDP header compression */
    if(packet.nextHeader == UDPPacket.DISPATCH) {
      UDPPacket udp = (UDPPacket) packet.getIPPayload();
      if( udp.sourcePort  >= UDP_PORT_MIN &&
          udp.sourcePort <  UDP_PORT_MAX &&
          udp.destinationPort >= UDP_PORT_MIN &&
          udp.destinationPort <  UDP_PORT_MAX) {
        /* we can compress. Copy compressed ports, full chcksum */
        data[pos++] = (byte) NHC_UDP_C;
        data[pos++] = (byte) (((udp.sourcePort - UDP_PORT_MIN) << 4) +
            (udp.destinationPort - UDP_PORT_MIN));
        int checksum = udp.doVirtualChecksum(packet);
        data[pos++] = (byte) (checksum >> 8);
        data[pos++] = (byte) (checksum & 0xff);
      } else {
        /* we cannot compress. Copy uncompressed ports, full chcksum */
        data[pos++] = (byte) NHC_UDP_I;
        data[pos++] = (byte) (udp.sourcePort >> 8);
        data[pos++] = (byte) (udp.sourcePort & 0xff);
        data[pos++] = (byte) (udp.destinationPort >> 8);
        data[pos++] = (byte) (udp.destinationPort & 0xff);
        int checksum = udp.doVirtualChecksum(packet);
        data[pos++] = (byte) (checksum >> 8);
        data[pos++] = (byte) (checksum & 0xff);
      }
    }

    
    // data[0] = HC01_DISPATCH; - layer below does this!!!
    data[0] = (byte) (enc1 & 0xff);
    data[1] = (byte) (enc2 & 0xff);

    if (DEBUG) System.out.println("HC01 Header compression: size " + pos +
        " enc1: " + Utils.hex8(enc1) + " enc2: " + Utils.hex8(enc2));
    if (DEBUG) {
        System.out.print("HC01: From ");
        IPv6Packet.printAddress(System.out, packet.sourceAddress);
        System.out.print("HC01:   To ");
        IPv6Packet.printAddress(System.out, packet.destAddress);
    }
    byte[] pload;
    if (packet.nextHeader == UDPPacket.DISPATCH) {
      UDPPacket udp = (UDPPacket) packet.getIPPayload();
      /* already have the udp header */
      pload = udp.payload;
    } else {
      IPPayload payload = packet.getIPPayload();
      pload = payload.generatePacketData(packet);
    }
    if (DEBUG) System.out.println("HC01 Payload size: " + pload.length);
    
    byte[] dataPacket = new byte[pos + pload.length];
    System.arraycopy(data, 0, dataPacket, 0, pos);
    System.arraycopy(pload, 0, dataPacket, pos, pload.length);
    return dataPacket;
  }
  
  public int writeVFlow(IPv6Packet packet, byte[] data, int pos) {
    data[pos++] = (byte) (0x60 | (packet.flowLabel >> 16) & 0x0f);
    data[pos++] = (byte)((packet.flowLabel >> 8) & 0xff);
    data[pos++] = (byte) (packet.flowLabel & 0xff);
    return 3;
  }

  
  public boolean parsePacketData(IPv6Packet packet) {
    /* first two is ... */
    UDPPacket udp = null;
    int pos = 2;
    int enc1 = packet.getData(0);
    int enc2 = packet.getData(1);
    if ((enc1 & 0x40) == 0) {
      if ((enc1 & 0x80) == 0) {
        packet.version = (packet.getData(pos) & 0xf0) >> 4;
      packet.trafficClass = ((packet.getData(pos) & 0x0f) << 4) + ((packet.getData(pos + 1) & 0xff) >> 4);
      packet.flowLabel = ((packet.getData(pos + 1) & 0x0f) << 16)
              + ((packet.getData(pos + 2) & 0xff) << 8)
              + (packet.getData(pos + 3) & 0xff);
      pos += 4;
      } else {
        packet.version = 6;
        packet.trafficClass = 0;
        packet.flowLabel = ((packet.getData(pos) & 0x0f) << 16) 
                + ((packet.getData(pos + 1) & 0xff) << 8)
                + (packet.getData(pos + 2) & 0xff);
        pos += 3;
      }
    } else {
      packet.version = 6;
      packet.flowLabel = 0;
      if ((enc1 & 0x80) == 0) {
        packet.trafficClass = (packet.getData(pos) & 0xff);
        pos++;
      } else {
        packet.trafficClass = 0;
      }
    }
    
    /* next header not compressed -> get it */
    if ((enc1 & 0x20) == 0) {
      packet.nextHeader = packet.getData(pos++);
    }
    
    /* encoding of TTL */
    switch (enc1 & 0x18) {
    case IPHC_TTL_1:
      packet.hopLimit = 1;
      break;
    case IPHC_TTL_64:
      packet.hopLimit = 64;
      break;
    case IPHC_TTL_255:
      packet.hopLimit = 0xff;
      break;
    case IPHC_TTL_I:
      packet.hopLimit = packet.getData(pos++);
      break;
    }

    /* 0, 1, 2, 3 as source address ??? */
    int srcAddress = (enc2 & 0x30) >> 4;
    AddrContext context = lookupContext(srcAddress);
    if (DEBUG) {
        System.out.println("HC01: uncompress (SRC) enc2 & c0 = " + (enc2 & 0xc0) +
            " ctx =" + srcAddress);
    }
    switch (enc2 & 0xc0) {
    case IPHC_SAM_0:
      if(context == null) {
        System.out.println("sicslowpan uncompress_hdr: error context not found\n");
        return false;
      }
      /* set hi address as prefix from context */
      System.arraycopy(context.prefix, 0, packet.sourceAddress, 0, 8);
      /* infer IID from L2 address */
      byte[] linkAddress = packet.getLinkSource();//getLinkSourceAddress(packet);
      System.arraycopy(linkAddress, 0, packet.sourceAddress, 8, 8);
      /* TODO: clean autoconf stuff up */
      packet.sourceAddress[8] ^= 0x02;
      break;
    case IPHC_SAM_16:
      if((packet.getData(pos) & 0x80) == 0) {
        /* unicast address */
        if(context == null) {
          System.out.println("sicslowpan uncompress_hdr: error context not found\n");
          return false;
        }
        /* set hi address as prefix from context */
        System.arraycopy(context.prefix, 0, packet.sourceAddress, 0, 8);
        /* copy 6 NULL bytes then 2 last bytes of IID */
        Utils.fill(packet.sourceAddress, 8, 14, (byte)0);
        packet.sourceAddress[14] = packet.getData(pos);
        packet.sourceAddress[15] = packet.getData(pos + 1);
        pos += 2;
      } else {
        /* [ignore] multicast address check the 9-bit group-id is known */
        Utils.fill(packet.sourceAddress, 0, 16, (byte)0);
        packet.sourceAddress[0] = (byte)0xff;
        packet.sourceAddress[1] = (byte)(((packet.getData(pos) & 0xff) >> 1) & 0x0f);
        packet.sourceAddress[15] = packet.getData(pos + 1);
        pos += 2;
      }
      break;
    case IPHC_SAM_64:
      if(context == null) {
        System.out.println("sicslowpan uncompress_hdr: error context not found\n");
        return false;
      }
      /* copy prefix from context */
      System.arraycopy(context.prefix, 0, packet.sourceAddress, 0, 8);
      /* copy IID from packet */
      packet.copy(pos, packet.sourceAddress, 8, 8);
      pos += 8;
      break;
    case IPHC_SAM_I:
      if (DEBUG) System.out.println("HC01: full address used (SRC)");
      /* copy whole address from packet */
      packet.copy(pos, packet.sourceAddress, 0, 16);
      pos += 16;
      break;
    }

    /* Destination address */
    context = lookupContext(enc2 & 0x03);
    if (DEBUG) {
      System.out.println("HC01: uncompress (DST) enc2 & 0x0c = " + (enc2 & 0x0c) +
          " ctx =" + (enc2 & 0x03));
    }
  
    switch(enc2 & 0x0C) {
    case IPHC_DAM_0:
      if(context == null) {
        System.out.println("sicslowpan uncompress_hdr: error context not found\n");
        return false;
      }
      /* copy prefix from context */
      System.arraycopy(context.prefix, 0, packet.destAddress, 0, 8);
      /* infer IID from L2 address */
      byte[] destAddress = packet.getLinkDestination();
      System.arraycopy(destAddress, 0, packet.destAddress, 8, 8);
      /* cleanup autoconf stuff later ... */
      packet.destAddress[8] ^= 0x02;
      break;
    case IPHC_DAM_16:
      if((packet.getData(pos) & 0x80) == 0) {
        /* unicast address */
        if(context == null) {
          System.out.println("sicslowpan uncompress_hdr: error context not found\n");
          return false;
        }
        System.arraycopy(context.prefix, 0, packet.destAddress, 0, 8);
        /* copy 6 NULL bytes then 2 last bytes of IID */
        packet.destAddress[14] = packet.getData(pos);
        packet.destAddress[15] = packet.getData(pos + 1);
        pos += 2;
      } else {
        /* [ignore] multicast address check the 9-bit group-id is known */
        //System.out.println("*** Multicast address!!! HC01: " + packet.getData(pos) + "," + packet.getData(pos + 1));
        Utils.fill(packet.destAddress, 0, 16, (byte)0);
        packet.destAddress[0] = (byte) 0xff; 
        packet.destAddress[1] = (byte)(((packet.getData(pos) & 0xff) >> 1) & 0x0F);
        packet.destAddress[15] = (byte) (packet.getData(pos + 1) & 0xff);
        pos += 2;
      }
      break;
    case IPHC_DAM_64:
      if(context == null) {
        System.out.println("sicslowpan uncompress_hdr: error context not found\n");
        return false;
      }
      /* copy prefix from context */
      System.arraycopy(context.prefix, 0, packet.destAddress, 0, 8);
      /* copy IID from packet */
      packet.copy(pos, packet.destAddress, 8, 8);
      pos += 8;
      break;
    case IPHC_DAM_I:
      /* copy whole address from packet */
      packet.copy(pos, packet.destAddress, 0, 16);
      pos += 16;
      break;
    }

    if ((enc1 & 0x20) != 0) {
      /* The next header is compressed, NHC is following */
      if ((packet.getData(pos) & 0xfc) == NHC_UDP_ID) {
        if (DEBUG) System.out.println("HC01: Next header UDP!");
        packet.nextHeader = PROTO_UDP;
        int srcPort = 0;
        int destPort = 0;
        int checkSum = 0;
        switch(packet.getData(pos) & 0xff) {
        case NHC_UDP_C:
          /* 1 byte for NHC, 1 byte for ports, 2 bytes chksum */
          srcPort = UDP_PORT_MIN + ((packet.getData(pos + 1) & 0xff) >> 4);
          destPort = UDP_PORT_MIN + (packet.getData(pos + 1) & 0x0F);
          checkSum = ((packet.getData(pos + 2) & 0xff) << 8) + (packet.getData(pos + 3) & 0xff);
          pos += 4;
          break;
        case NHC_UDP_I:
          /* 1 byte for NHC, 4 byte for ports, 2 bytes chksum */
          srcPort = ((packet.getData(pos + 1) & 0xff)<< 8) + (packet.getData(pos + 2) & 0xff);
          destPort = ((packet.getData(pos + 3) & 0xff)<< 8) + (packet.getData(pos + 4) & 0xff);
          checkSum = ((packet.getData(pos + 5) & 0xff)<< 8) + (packet.getData(pos + 6) & 0xff);
          pos += 7;
          break;
        default:
          System.out.println("sicslowpan uncompress_hdr: error unsupported UDP compression\n");
        return false;
        }
        udp = new UDPPacket();
        udp.sourcePort = srcPort;
        udp.destinationPort = destPort;
        udp.checkSum = checkSum;
      }
    }

    boolean frag = false;
    /* fragment handling ... */
    if (!frag) {
      /* this does not handle the UDP header compression yet... */
      /* move cursor pos steps forward */
      packet.incPos(pos);
    } else {
    }

    if (DEBUG) {
      System.out.println("Encoding 0: " + Utils.hex8(enc1) +
          " Encoding 1: " + Utils.hex8(enc2));
      System.out.println("TTL: " + packet.hopLimit);
      System.out.print("Src Addr: ");
      IPv6Packet.printAddress(System.out, packet.sourceAddress);
      System.out.print("Dst Addr: ");
      IPv6Packet.printAddress(System.out, packet.destAddress);
      System.out.println();
    }
    // packet.setPayload(data, 40, ???);
    packet.payloadLen = packet.getPayloadLength();

    if (udp != null) {
      /* if we have a udp payload we already have the udp headers in place */
      /* the rest is only the payload */
      udp.payload = packet.getPayload();
      udp.length = udp.payload.length + 8;
      /* add 8 to the payload length of the UDP packet */
      packet.payloadLen += 8;
      udp.doVirtualChecksum(packet);
      packet.setIPPayload(udp);
    }
    return true;
  }
  
  private boolean isMulticastCompressable(byte[] address) {
    for (int i = 2; i < 15; i++) {
      if (address[i] != 0) return false;
    }
    return (address[15] == 1 || address[15] == 2);
  }
  
  
  private AddrContext lookupContext(int index) {
    if (index < contexts.length)
      return contexts[index];
    return null;
  }
  
  private int lookupContext(byte[] address) {
    for (int i = 0; i < contexts.length; i++) {
      if (contexts[i] != null && contexts[i].matchPrefix(address)) {
        return i;
      }
    }
    return -1;
  }
}