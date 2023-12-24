package vnl;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ClientController {
    // status of connection
    private BooleanProperty connected = new SimpleBooleanProperty(false);
    // test stroke, transfer received data to binded GUI units
    private StringProperty receivingMessageModel = new SimpleStringProperty("");
    // channel to exchange message
    private Channel channel;
    // thread processing connection
    private EventLoopGroup workerGroup;

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button test_btn;

    @FXML
    private Label test_lbl;

    @FXML
    void initialize() {
        //when receivingMessageModel change, binded object will change too
        test_lbl.textProperty().bind(receivingMessageModel);
    }

    @FXML // click on test_btn
    void send(ActionEvent event) {
        if(!connected.get()){
            return;
        }
        // prepare test data
        ArrayList<Integer> list = new ArrayList<>();
        list.add(101);
        list.add(102);
        list.add(103);
        Data msg = new Data(
            1,
            "Hello:World",
            2,
            list,
            3,
            4,
            5
            );
        
        // task what sends prepared data
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception{
                ChannelFuture f = channel.writeAndFlush(msg);
                f.sync();
                return null;
            }
            @Override
            protected void failed(){
                Throwable e = getException();
                e.printStackTrace();
                connected.set(false);
            }
        };

        // execute of send task in separate thread
        new Thread(task).start();
    }

    public void connect() throws URISyntaxException{
        if(connected.get()){
            return; // already connected
        }
        // test connection properties
        String host = "localhost";
        int port = 8080;
        
        // initialize group of threads for processing connections
        workerGroup = new NioEventLoopGroup();
        // task of connection and get exchange channel
        Task<Channel> task = new Task<Channel>() {
            @Override
            protected Channel call() throws Exception{
                // setup connection processor
                Bootstrap b = new Bootstrap();
                b.group(workerGroup);
                b.channel(NioSocketChannel.class);
                b.option(ChannelOption.SO_KEEPALIVE, true);
                b.handler(new ChannelInitializer<SocketChannel>(){
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception{
                        /* register of channel handlers,
                         * which decode received message,
                         * then handle and processing data,
                         * and encode sending message
                        */
                        ch.pipeline().addLast(
                            new DataDecoder(),
                            new DataEncoder(),
                            new ClientHandler(receivingMessageModel),
                            new LoggingHandler(LogLevel.INFO)
                            );
                    }
                });

                ChannelFuture f = b.connect(host, port);
                Channel ch = f.channel();
                return ch;
            }
            @Override
            protected void succeeded(){
                channel = getValue();
                connected.set(true);
            }
            @Override
            protected void failed(){
                Throwable e = getException();
                e.printStackTrace();
                connected.set(false);
            }
        };

        // execute of connection task in separate thread
        new Thread(task).start();
    }

    public void disconnect(){
        if(!connected.get()){
            return;
        }
        // task of disconnection
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception{
                if(channel.isActive()){
                    channel.close();
                }
                // shutdown group of threads what processed connection
                workerGroup.shutdownGracefully();
                return null;
            }
            @Override
            protected void succeeded(){
                connected.set(false);
            }
            @Override
            protected void failed(){
                Throwable e = getException();
                e.printStackTrace();
                connected.set(false);
            }
        };
        // execute of disconnection task in separate thread
        new Thread(task).start();
    }
}
