package vnl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerHandler extends ChannelInboundHandlerAdapter{
    public ArrayList<GameRoom> rooms;
    // current room ID
    public int roomId;
    // true mean this is 1'st player
    // flase mean this is 2'nd player
    public Boolean currentPlayer;
    // list of user's cards; trade list
    public ArrayList<Integer> list;

    public ServerHandler(ArrayList<GameRoom> rooms){
        this.rooms = rooms;
        roomId = 0;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object inMsg){
        // receive decoded data from decoder
        Data msg = (Data) inMsg;
        System.out.println("Receive request #" + msg.header);
        // create new room
        if(msg.header == 3){
            // check if user already at room, then quit
            if((roomId > 0) && rooms.get(roomId-1).free == false){
                if(currentPlayer){
                    rooms.get(roomId-1).player1 = false;
                }else{
                    rooms.get(roomId-1).player2 = false;
                }
                rooms.get(roomId-1).free();
            }
            // choose free room
            roomId = 0;
            for(int i=0; i < rooms.size(); i++){
                if(rooms.get(i).free){
                    rooms.get(i).free = false;
                    rooms.get(i).pl1_ctx = ctx;
                    roomId = i+1;
                    break;
                }
            }
            // create new room if no one free
            if(roomId == 0){
                rooms.add(new GameRoom(ctx));
                roomId = rooms.size();
            }
            // set room waiting
            currentPlayer = true;

            //prepare message: answer of new room
            msg = new Data(4, "", roomId, null, 0, 0, 0);
        // connect to exist room
        }else if(msg.header == 5){
            // check if user already at room, then quit
            if((roomId > 0) && rooms.get(roomId-1).free == false){
                if(currentPlayer){
                    rooms.get(roomId-1).player1 = false;
                }else{
                    rooms.get(roomId-1).player2 = false;
                }
                rooms.get(roomId-1).free();
            }
            // try to enter requested room
            if((msg.id-1 >= 0) && (msg.id-1 < rooms.size()) && (rooms.get(msg.id-1).free == false) && (rooms.get(msg.id-1).player2 == false)){
                roomId = msg.id;
                rooms.get(roomId-1).player2 = true;
                currentPlayer = false;
                rooms.get(roomId-1).connect2nd();
                rooms.get(roomId-1).pl2_ctx = ctx;
                // form random list of cards with numbers from 1 to 100
                list = new ArrayList<>();
                for(int i=0; i<15; i++){
                    list.add(BigDecimal.valueOf(Math.random() * 100 + 1).setScale(0, RoundingMode.HALF_EVEN).intValue());
                }
                msg = new Data(7, "", 0, list, 0, 0, 0);
            }else{
                msg = new Data(6, "", 0, null, 0, 0, 0);
            }
        }
        // send answer to client via encoder
        ctx.writeAndFlush(msg);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx){
        if(currentPlayer){
            rooms.get(roomId-1).player1 = false;
        }else{
            rooms.get(roomId-1).player2 = false;
        }
        rooms.get(roomId-1).free();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        cause.printStackTrace();
        ctx.close();
    }
}
