package org.pudding.transport.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import org.apache.log4j.Logger;
import org.pudding.transport.api.Config;
import org.pudding.transport.exception.IllegalOptionException;
import org.pudding.transport.common.Option;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

/**
 * 基于Netty的Connector实现.
 *
 * @author Yohann.
 */
public class NettyConnector extends ConfigOptions implements INettyConnector {

    private static final Logger logger = Logger.getLogger(NettyConnector.class);

    private IConnectNettyConfig nettyConfig;
    private SocketAddress remoteAddress;

    /**
     * Default NettyConfig.
     */
    public NettyConnector() {
        super(false);
        nettyConfig = new DefaultConnectNettyConfig();
    }

    /**
     * Custom NettyConfig.
     *
     * @param nettyConfig
     */
    public NettyConnector(IConnectNettyConfig nettyConfig) {
        super(false);
        this.nettyConfig = nettyConfig;
    }

    @Override
    public SocketAddress remoteAddress() {
        return remoteAddress;
    }

    @Override
    public void connect(String host, int port) {
        connect(new InetSocketAddress(host, port));
    }

    @Override
    public void connect(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        try {
            doConnect();
        } catch (InterruptedException e) {
            logger.warn("bind exception", e);
        } catch (IllegalOptionException e) {
            logger.warn("option exception", e);
        } finally {
            shutdownGracefully();
        }
    }

    @SuppressWarnings("unchecked")
    private void doConnect() throws InterruptedException, IllegalOptionException {
        validate(remoteAddress, bootstrap);
        bootstrap.group(group() )
                .channel(channel())
                .handler(handler());

        if (!setOption()) {
            throw new IllegalOptionException("setOption Exception");
        }

        ChannelFuture future = bootstrap.connect(remoteAddress).sync();
        logger.info("Connect to " + remoteAddress);

        future.channel().closeFuture().sync();
    }

    private boolean setOption() {
        Map<Option<?>, Object> childOptions = nettyConfig.options();
        for (Map.Entry<Option<?>, Object> entry : childOptions.entrySet()) {
            Option<?> option = entry.getKey();
            Object value = entry.getValue();
            if (option == Option.SO_SNDBUF) {
                child.setSoSndbuf((Integer) value);
            } else if (option == Option.SO_RCVBUF) {
                child.setSoRcvbuf((Integer) value);
            } else if (option == Option.TCP_NODELAY) {
                child.setTcpNodelay((Boolean) value);
            } else if (option == Option.SO_KEEPALIVE) {
                child.setSoKeepalive((Boolean) value);
            } else if (option == Option.SO_KEEPALIVE) {
                child.setSoReuseaddr((Boolean) value);
            } else if (option == Option.SO_LINGER) {
                child.setSoLinger((Integer) value);
            } else {
                setOption0(child, option, value);
            }
        }
        return true;
    }

    private boolean setOption0(Netty netty, Option<?> option, Object value) {
        if (option == Option.CONNECT_TIMEOUT_MILLIS) {
            netty.setConnectTimeoutMillis((Integer) value);
        } else if (option == Option.WRITE_SPIN_COUNT) {
            netty.setWriteSpinCount((Integer) value);
        } else if (option == Option.ALLOCATOR) {
            netty.setAllocator((ByteBufAllocator) value);
        } else if (option == Option.RCVBUF_ALLOCATOR) {
            netty.setRcvbufAllocator((RecvByteBufAllocator) value);
        } else if (option == Option.AUTO_READ) {
            netty.setAutoRead((Boolean) value);
        } else if (option == Option.WRITE_BUFFER_WATER_MARK) {
            netty.setWriteBufferWaterMark((WriteBufferWaterMark) value);
        } else if (option == Option.MESSAGE_SIZE_ESTIMATOR) {
            netty.setMessageSizeEstimator((MessageSizeEstimator) value);
        } else if (option == Option.SINGLE_EVENTEXECUTOR_PER_GROUP) {
            netty.setSingleEventexecutorPerGroup((Boolean) value);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public Config config() {
        return nettyConfig;
    }

    @Override
    public void shutdownGracefully() {
        group().shutdownGracefully();
    }

    @Override
    public EventLoopGroup group() {
        return nettyConfig.group();
    }

    @Override
    public Class channel() {
        return nettyConfig.channel();
    }

    @Override
    public ChannelInitializer handler() {
        return nettyConfig.handler();
    }

    private void validate(SocketAddress remoteAddress, Bootstrap bootstrap) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress == null");
        }
        validate(bootstrap);
    }

    private void validate(Bootstrap bootstrap) {
        if (bootstrap == null) {
            throw new NullPointerException("bootstrap == null");
        }
    }
}