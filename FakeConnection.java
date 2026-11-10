package shieldbot;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import shieldbot.mixins.ConnectionAccessor;

/** A network connection that goes nowhere. Adapted from Carpet's FakeClientConnection (MIT). */
public class FakeConnection extends Connection
{
    public FakeConnection(PacketFlow flow)
    {
        super(flow);
        ((ConnectionAccessor) this).shieldbot$setChannel(new EmbeddedChannel());
    }

    @Override
    public void setReadOnly() {}

    @Override
    public void send(Packet<?> packet, ChannelFutureListener listener, boolean flush) {}

    @Override
    public void handleDisconnection() {}

    @Override
    public void setListenerForServerboundHandshake(PacketListener packetListener) {}

    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocolInfo, T packetListener) {}
}
