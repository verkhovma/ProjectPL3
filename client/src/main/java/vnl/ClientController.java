package vnl;

import java.net.URISyntaxException;
import java.util.ArrayList;

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
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ClientController {
    
    @FXML
    public Button btn_connect;

    @FXML
    public Button btn_createRoom;

    @FXML
    public Button btn_disconnect;

    @FXML
    public Button btn_enterRoom;

    @FXML
    public Button btn_quitRoom;

    @FXML
    public Button btn_startRoom;

    @FXML
    public HBox hbox_cardSelector;

    @FXML
    public HBox hbox_titleRoom;

    @FXML
    public ImageView imgv_cardPreview;

    @FXML
    public ImageView imgv_connect;

    @FXML
    public Label lbl_cardPreview;

    @FXML
    public Label lbl_connect;

    @FXML
    public Label lbl_descriptionRoom;

    @FXML
    public Label lbl_idRoom;

    @FXML
    public Label lbl_listAvailable;

    @FXML
    public Label lbl_listSelected;

    @FXML
    public Label lbl_lobby;

    @FXML
    public Label lbl_titleRoom;

    @FXML
    public TextField tf_address;

    @FXML
    public TextField tf_idRoom;

    @FXML
    public TextField tf_port;

    @FXML
    public VBox vbox_cardPreview;

    @FXML
    public VBox vbox_connect;

    @FXML
    public VBox vbox_listAvailable;

    @FXML
    public VBox vbox_listSelected;

    @FXML
    public VBox vbox_lobby;

    @FXML
    public VBox vbox_room;

    // status of connection
    public BooleanProperty connected;
    // test stroke, transfer received data to binded GUI units
    public StringProperty receivingMessageModel = new SimpleStringProperty("");
    // contain current Application controller, which allow
    // ClientHandler change GUI
    public ClientController thisController;
    // channel to exchange message
    public Channel channel;
    // thread processing connection
    public EventLoopGroup workerGroup;

    //resources
    public Image loadingImage;

    @FXML
    void initialize() {
        //when receivingMessageModel change, binded object will change too
        //test_lbl.textProperty().bind(receivingMessageModel);
        vbox_connect.setVisible(true);
        vbox_lobby.setVisible(false);
        vbox_room.setVisible(false);
        connected = new SimpleBooleanProperty(false);
        thisController = this;
        loadingImage = new Image(getClass().getClassLoader().getResource("loading.gif").toExternalForm());
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

    @FXML // click on btn_connect
    public void connect() throws URISyntaxException{
        if(connected.get()){
            return; // already connected
        }
        // get connection properties
        String host = tf_address.getText(); //"localhost";
        int port = Integer.parseInt(tf_port.getText()); //8080;

        // set GUI status
        lbl_connect.setText("Connection in progress");
        imgv_connect.setImage(loadingImage);
        
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
                            new ClientHandler(thisController, receivingMessageModel),
                            new LoggingHandler(LogLevel.INFO)
                            );
                    }
                });

                ChannelFuture f = b.connect(host, port).sync();
                Channel ch = f.channel();
                return ch;
            }
            @Override
            protected void succeeded(){
                channel = getValue();
                connected.set(true);
                // update GUI status
                Platform.runLater(()->{
                    lbl_connect.setText("Connection succeessful");
                    imgv_connect.setImage(null);
                    // go to lobby screen
                    vbox_connect.setVisible(false);
                    vbox_lobby.setVisible(true);
                    lbl_lobby.setText("Welcome");
                });
            }
            @Override
            protected void failed(){
                Throwable e = getException();
                e.printStackTrace();
                connected.set(false);
                // update GUI status
                Platform.runLater(()->{
                    lbl_connect.setText("Connection failed");
                    imgv_connect.setImage(null);
                });
            }
        };

        // execute of connection task in separate thread
        new Thread(task).start();
    }

    @FXML
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
                // go to connection screen
                Platform.runLater(()->{
                    lbl_connect.setText("Disconnection");
                    vbox_lobby.setVisible(false);
                    vbox_connect.setVisible(true);
                });
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

    // Task of send Data via argument
    public class MyTask extends Task<Void>{
        public Data msg;

        public MyTask(Data inMsg){
            msg = inMsg;
        }
        public Void call() throws Exception{
            return null;
        }
    }

    @FXML // click on btn_createRoom
    void createRoom(ActionEvent event) {
        if(!connected.get()){
            return;
        }
        // prepare request
        Data msg = new Data(
            3,
            "",
            0,
            null,
            0,
            0,
            0
            );
        
        // task what sends prepared data
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception{
                ChannelFuture f = channel.writeAndFlush(msg);
                f.sync();
                Platform.runLater(()->{
                    lbl_lobby.setText("Creating room");
                });
                return null;
            }
            @Override
            protected void failed(){
                Throwable e = getException();
                e.printStackTrace();
                disconnect();
            }
        };

        // execute of send task in separate thread
        new Thread(task).start();
    }

    @FXML // click on btn_enterRoom
    void enterRoom(ActionEvent event) {
        if(!connected.get()){
            return;
        }
        // prepare request
        Data msg = new Data(0, "", 0, null, 0, 0, 0);
        try{
            msg = new Data(
                5,
                "",
                Integer.parseInt(tf_idRoom.getText()),
                null,
                0,
                0,
                0
                );
        }catch(NumberFormatException e){
            e.printStackTrace();
            lbl_lobby.setText("Invalid room ID");
            return;
        }
        
        // task what sends prepared data
        Task<Void> task = new MyTask(msg) {
            @Override
            public Void call() throws Exception{
                ChannelFuture f = channel.writeAndFlush(msg);
                f.sync();
                Platform.runLater(()->{
                    lbl_idRoom.setText(tf_idRoom.getText());
                    lbl_lobby.setText("Waiting room");
                });
                return null;
            }
            @Override
            protected void failed(){
                Throwable e = getException();
                e.printStackTrace();
                disconnect();
            }
        };

        // execute of send task in separate thread
        new Thread(task).start();
    }

    @FXML
    void quitRoom(ActionEvent event) {

    }

    @FXML
    void startRoom(ActionEvent event) {

    }
}
