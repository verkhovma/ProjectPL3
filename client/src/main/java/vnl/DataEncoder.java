package vnl;

import java.io.UnsupportedEncodingException;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
// encode sent message to frame
public class DataEncoder extends ChannelHandlerAdapter{
    @Override
    public void write(ChannelHandlerContext ctx, Object inmsg, ChannelPromise promise) throws UnsupportedEncodingException{
        Data msg = (Data) inmsg;
        // allocate direct buffer of 256 bytes
        ByteBuf encoded = ctx.alloc().directBuffer(256);
        // write fields to buffer
        encoded.writeInt(msg.header);
        byte[] filedBytes = msg.field.getBytes("UTF-8");
        encoded.writeInt(filedBytes.length);
        encoded.writeBytes(filedBytes);
        encoded.writeInt(msg.id);
        
        int listSize = 0;
        if(msg.list != null){
            listSize = msg.list.size();
            encoded.writeInt(listSize);
            for(int elem: msg.list){
                encoded.writeInt(elem);
            }
        }else{
            encoded.writeInt(listSize);
        }
        encoded.writeInt(msg.card_num);
        encoded.writeInt(msg.cli_row);
        encoded.writeInt(msg.cli_col);
        // fill rest of frame by 0 bytes for unambiguosly reading
        int spaceSize = 256 - (4 * (7 + listSize) + filedBytes.length);
        for(int i = 0; i < spaceSize; i++){
            encoded.writeByte(0);
        }
        // send encoded data
        ctx.write(encoded, promise);
    }
}
