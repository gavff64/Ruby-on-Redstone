package io.github.hello09x.fakeplayer.v26_2.network;

import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.manager.FakeplayerManager;
import io.github.hello09x.fakeplayer.core.network.FakeChannel;
import io.netty.channel.ChannelFutureListener;
import lombok.Lombok;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.logging.Logger;

public class FakeConnection extends Connection {

    private final static Logger log = Main.getInstance().getLogger();
    private final FakeplayerManager manager = Main.getInjector().getInstance(FakeplayerManager.class);

    public FakeConnection(@NotNull InetAddress address) {
        super(PacketFlow.SERVERBOUND);
        this.channel = new FakeChannel(null, address);
        this.address = this.channel.remoteAddress();
        Connection.configureSerialization(this.channel.pipeline(), PacketFlow.SERVERBOUND, false, null);
    }

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener channelfuturelistener) {
    }

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener channelfuturelistener, boolean flag) {
    }



    @Override
    public void send(Packet<?> packet) {

    }

    /**
     * 完成 channel 的关闭流程.
     * <p>FakeChannelPipeline 的 close 是 no-op, closeFuture 永远不会完成,
     * 导致 26.2 中 Connection#handleDisconnection 永远不会触发, 假人被踢出后不会真正下线.
     * CloseFuture 的 setSuccess/trySuccess 被 netty 禁用 (直接抛异常),
     * 只能通过包私有的 setClosed() 完成, 这里用反射调用</p>
     */
    public void forceClose() {
        if (this.channel.closeFuture().isDone()) {
            return;
        }
        try {
            var closeFuture = this.channel.closeFuture();
            var method = closeFuture.getClass().getDeclaredMethod("setClosed");
            method.setAccessible(true);
            method.invoke(closeFuture);
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
    }

}