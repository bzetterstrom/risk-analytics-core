package org.pillarone.riskanalytics.core.wiring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pillarone.riskanalytics.core.components.Component;
import org.pillarone.riskanalytics.core.packets.Packet;
import org.pillarone.riskanalytics.core.packets.PacketList;

public class Transmitter implements ITransmitter {

    private static Log LOG = LogFactory.getLog(Transmitter.class);

    protected Component sender;
    protected Component receiver;
    protected PacketList target;
    protected PacketList source;
    protected boolean transmitted;
    protected String senderChannelName;

    public Transmitter(Component sender, PacketList source, Component receiver, PacketList target) {
        this.sender = sender;
        this.receiver = receiver;
        this.target = target;
        this.source = source;
        senderChannelName = WiringUtils.getSenderChannelName(sender, source);
    }

    /**
     * Transmits the packets to the receiver, setting their sender and senderChannelName equal to the values
     * of the corresponding properties of the Transmitter.
     */
    public void transmit() {
        if (isTransmitted()) { // TODO (msh): check case of retransmission and delete flag if unneccessary
            throw new IllegalStateException("No retransmission allowed: " + this);
        }
        for (Object packet : source) {
            ((Packet) packet).setSender(sender);
            ((Packet) packet).setSenderChannelName(senderChannelName);
        }
        target.addAll(source);
        setTransmitted(true);
        if (LOG.isDebugEnabled()) {
            StringBuffer buffer = new StringBuffer();
            buffer.append(senderChannelName);
            buffer.append(" [");
            buffer.append(source.size());
            buffer.append("] ");
            buffer.append(sender.getName());
            buffer.append(" -> ");
            buffer.append(receiver.getName());
            buffer.append(" (");
            buffer.append(sender.getClass());
            buffer.append(" -> ");
            buffer.append(receiver.getClass());
            buffer.append(")");
            LOG.debug(buffer.toString());
        }
        notifyReceiver();
    }

    protected void notifyReceiver() {
        receiver.notifyTransmitted(this);
    }

    public boolean isTransmitted() {
        return transmitted;
    }

    public void setTransmitted(boolean transmittedValue) {
        transmitted = transmittedValue;
    }

    public Component getSender() {
        return sender;
    }

    public Component getReceiver() {
        return receiver;
    }

    public PacketList getTarget() {
        return target;
    }

    public PacketList getSource() {
        return source;
    }

    public String toString() {
        return "" + sender.getClass().getSimpleName() +
                " sends to " +
                receiver.getClass().getSimpleName() +
                ", packet type: " +
                source.getType().getSimpleName() +
                " using " +
                this.getClass().getSimpleName() +
                ", senderChannelName " + senderChannelName;
    }
}
