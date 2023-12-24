package vnl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
// decode received message before handling it
public class DataDecoder extends ByteToMessageDecoder{
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws UnsupportedEncodingException{
        // frame size directly set to 256 bytes, so wait until receive all
        if(in.readableBytes() < 256)
            return;
        // read fields from buffer
        int header = in.readInt();
        int fieldLength = in.readInt();
        String field = in.readBytes(fieldLength).toString(CharsetUtil.UTF_8);
        int id = in.readInt();
        int listSize = in.readInt();
        ArrayList<Integer> list = new ArrayList<>();
        for(int i = 0; i < listSize; i++){
            list.add(in.readInt());
        }
        int card_num = in.readInt();
        int cli_row = in.readInt();
        int cli_col = in.readInt();
        // discard rest of frame to prepare next unambiguosly reading
        int spaceSize = 256 - (4 * (7 + listSize) + fieldLength);
        in.readBytes(spaceSize);
        /*
        for(int i=0; i < spaceSize; i++){
            in.readByte();
        }
        */
        // transfer received object to handler
        out.add(new Data(
                header,
                field,
                id,
                list,
                card_num,
                cli_row,
                cli_col
            ));
    }
}
