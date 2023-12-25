package vnl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import io.netty.channel.ChannelHandlerContext;

public class GameRoom {
    public Boolean free;
    // status of players connect
    public Boolean player1;
    public ChannelHandlerContext pl1_ctx;
    public Boolean player2;
    public ChannelHandlerContext pl2_ctx;
    // true is 1'st player turn,
    // false is 2'nd player turn
    public Boolean turnID;

    // create new room
    public GameRoom(ChannelHandlerContext inCtx){
        free = false;
        // choose which player turn
        turnID = Math.random() >= 0.5 ? true : false;
        player1 = true;
        player2 = false;
        pl1_ctx = inCtx;
    }
    // 2'nd player connected invoke 1'st player card chosing event
    public void connect2nd(){
        // form random list of cards with numbers from 1 to 100
        ArrayList<Integer> list = new ArrayList<>();
        for(int i=0; i<15; i++){
            list.add(BigDecimal.valueOf(Math.random() * 100 + 1).setScale(0, RoundingMode.HALF_EVEN).intValue());
        }
        Data msg = new Data(7, "", 0, list, 0, 0, 0);
        pl1_ctx.writeAndFlush(msg);
    }
    // make room free
    public void free(){
        if(!player1 && !player2){
            free = true;
        }
    }
}
