package vnl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import io.netty.channel.ChannelHandlerContext;

public class GameRoom {
    public Boolean free;
    // status of players
    public Boolean pl1_connect;
    public ChannelHandlerContext pl1_ctx;
    public Boolean pl1_ready;
    public ArrayList<Integer> pl1_list;
    public Boolean pl2_connect;
    public ChannelHandlerContext pl2_ctx;
    public Boolean pl2_ready;
    public ArrayList<Integer> pl2_list;
    // true is 1'st player turn,
    // false is 2'nd player turn
    public Boolean turnID;

    // create new room
    public GameRoom(ChannelHandlerContext inCtx){
        free = false;
        // choose which player turn
        turnID = (Math.random() >= 0.5) ? true : false;
        pl1_connect = true;
        pl1_ready = false;
        pl1_ctx = inCtx;
        pl2_connect = false;
        pl2_ready = false;
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
    // start game after receiving both player's card list
    public void start_play(){
        if(pl1_ready && pl2_ready){
            int pl1_turn = 0;
            int pl2_turn = 0;
            if(turnID){
                pl1_turn = 1;
                pl2_turn = 0;
            }else{
                pl1_turn = 0;
                pl2_turn = 1;
            }
            // get start status to players
            Data pl1_msg = new Data(10, "", pl1_turn, null, 0, 0, 0);
            Data pl2_msg = new Data(10, "", pl2_turn, null, 0, 0, 0);
            pl1_ctx.writeAndFlush(pl1_msg);
            pl2_ctx.writeAndFlush(pl2_msg);
        }
    }
    // update field, and move turn to next player
    public void update(int inNumCard, int inRow, int inCol){
        // next player
        turnID = ! turnID;
        // calculate field
            // not ready...
            
        // send data and notification to next player
        Data msg = new Data(11, "", 0, null, inNumCard, inRow, inCol);
        if(turnID){
            pl1_ctx.writeAndFlush(msg);
        }else{
            pl2_ctx.writeAndFlush(msg);
        }
    }
    // make room free
    public void free(){
        if((! pl1_connect) && (! pl2_connect)){
            free = true;
        }
    }
}
