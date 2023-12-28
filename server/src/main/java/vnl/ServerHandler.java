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
    // false mean this is 2'nd player
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
                    rooms.get(roomId-1).pl1_connect = false;
                }else{
                    rooms.get(roomId-1).pl2_connect = false;
                }
                rooms.get(roomId-1).free();
            }
            // choose free room
            roomId = 0;
            for(int i=0; i < rooms.size(); i++){
                if(rooms.get(i).free){
                    rooms.get(i).free = false;
                    rooms.get(i).pl1_connect = true;
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
                    rooms.get(roomId-1).pl1_connect = false;
                }else{
                    rooms.get(roomId-1).pl2_connect = false;
                }
                rooms.get(roomId-1).free();
            }
            // try to enter requested room
            if((msg.id-1 >= 0) && (msg.id-1 < rooms.size()) && (rooms.get(msg.id-1).free == false) && (rooms.get(msg.id-1).pl2_connect == false)){
                roomId = msg.id;
                rooms.get(roomId-1).pl2_connect = true;
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
        
        // get chosen cards
        }else if(msg.header == 8){
            if(msg.list.size() == 5){
                play:{
                    if(currentPlayer && (rooms.get(roomId-1).pl1_ready == false)){
                        rooms.get(roomId-1).pl1_ready = true;
                        rooms.get(roomId-1).pl1_list = msg.list;
                    }else if(rooms.get(roomId-1).pl2_ready == false){
                        rooms.get(roomId-1).pl2_ready = true;
                        rooms.get(roomId-1).pl2_list = msg.list;
                    }else{
                        msg = new Data(14, "already in game", 0, null, 0, 0, 0);
                        break play; // exit from if, when already in game
                    }
                    // start game after receive list
                    rooms.get(roomId-1).start_play();
                    return;
                }
            }else{
                msg = new Data(14, "incorrect amount of cards", 0, null, 0, 0, 0);
            }

        // quit from room
        }else if(msg.header == 9){
            if(currentPlayer){
                rooms.get(roomId-1).pl1_connect = false;
            }else{
                rooms.get(roomId-1).pl2_connect = false;
            }
            rooms.get(roomId-1).free();
            // need to add notification about success quit?
            return;

        // get turn data from player
        }else if(msg.header == 12){
            GameRoom room = rooms.get(roomId-1);
            if((currentPlayer && room.turnID) || ((! currentPlayer) && (! room.turnID))){
                // send data and turn notification to next player
                room.update(msg.card_num, msg.cli_row, msg.cli_col);
                return;
            }else{
                //this is not turn of current player
                msg = new Data(15, "this is not your turn", 0, null, 0, 0, 0);
            }
        }

        // send answer to client via encoder
        ctx.writeAndFlush(msg);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx){
        // free place if one of players disconnected
        if(currentPlayer){
            rooms.get(roomId-1).pl1_connect = false;
        }else{
            rooms.get(roomId-1).pl2_connect = false;
        }
        rooms.get(roomId-1).free();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        cause.printStackTrace();
        ctx.close();
    }
}
