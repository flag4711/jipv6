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
 * This file is part of MSPSim.
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
package se.sics.jipv6.mac;

import se.sics.jipv6.core.AbstractPacketHandler;
import se.sics.jipv6.core.IPStack;
import se.sics.jipv6.core.IPv6Packet;
import se.sics.jipv6.core.NetworkInterface;
import se.sics.jipv6.core.Packet;

public class LoWPANHandler extends AbstractPacketHandler implements NetworkInterface {
  
  private IPStack ipStack;
  
  public LoWPANHandler() {
  }

  public void setIPStack(IPStack stack) {
    ipStack = stack;
  }
  
  public String getName() {
    return "6lowpan";
  }
  
  public boolean isReady() {
    return true;
  }
  
  public void packetReceived(Packet packet) {
    /* create IP packet based on the correct dispatch */
    IPv6Packet ipPacket = new IPv6Packet(packet);
    int dispatch = packet.getData(0);
    packet.setAttribute("6lowpan.dispatch", dispatch);
    /* remove the dispatch and continue */
    ipPacket.incPos(1);
    if (dispatch == ipStack.getDefaultPacketer().getDispatch()) {
      ipStack.getDefaultPacketer().parsePacketData(ipPacket);
      /* send in the packet */
      ipPacket.netInterface = this;
      ipStack.receivePacket(ipPacket);
    }
  }

  public void sendPacket(IPv6Packet packet) {
    /* LoWPANHandler is for IP over 802.15.4 */
    // Get packeter and create packet
    byte[] data = ipStack.getPacketer().generatePacketData((IPv6Packet)packet);
    packet.setBytes(data);
    /* set the dispatch */
    byte[] data2 = new byte[1];
    data2[0] = ipStack.getPacketer().getDispatch();
    packet.prependBytes(data2);
    /* give to lower layer for sending on... */
    lowerLayer.sendPacket(packet);
  }

  public void sendPacket(Packet packet) {
    sendPacket((IPv6Packet) packet);
  }

}