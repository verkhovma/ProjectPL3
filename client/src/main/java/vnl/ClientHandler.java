package vnl;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;

public class ClientHandler extends ChannelHandlerAdapter{
    private final StringProperty receivingMessageModel;

    public ClientHandler(StringProperty inReceivingMessageModel){
        receivingMessageModel = inReceivingMessageModel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object inMsg){
        // receive decoded data from decoder
        Data msg = (Data) inMsg;
        // setup received data to property, which will lead to change
        // all binded GUI units
        Platform.runLater(()->{
            receivingMessageModel.set(msg.field);
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        cause.printStackTrace();
        ctx.close();
    }
}
