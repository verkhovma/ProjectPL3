package vnl;

import java.util.ArrayList;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class Main {
    private int port;
    private ArrayList<GameRoom> rooms;

    public Main(int port){
        this.port = port;
    }

    public static void main(String[] args) throws Exception{
        // create instance of server and launch it
        int port = 8080;
        new Main(port).run();
    }

    public void run() throws Exception{
        // group of threads processing incoming connections from clients
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // group of threads processing connections 
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        // list of all rooms
        rooms = new ArrayList<>();
        try{
            // setup connection processor
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                // specify class creates connection channels - Nio over TCP
                .channel(NioServerSocketChannel.class)
                // channels configuration
                .childHandler(new ChannelInitializer<SocketChannel>(){
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception{
                        /* register of channel handlers,
                         * which decode received message,
                         * then handle and processing data,
                         * and encode sending message
                        */
                        ch.pipeline().addLast(
                            new LoggingHandler(LogLevel.INFO),
                            new DataDecoder(),
                            new DataEncoder(),
                            new ServerHandler(rooms) // transfer pointer to rooms list
                            );
                    }
                })
                // parameters of connections
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
            // start server on port
            // sunc() is waiting current thread until bind occurs
            ChannelFuture f = b.bind(port).sync();
            // wait until server done it's work and
            // close socket
            f.channel().closeFuture().sync();
        }finally{
            // shutdown groups of threads when server closed
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
