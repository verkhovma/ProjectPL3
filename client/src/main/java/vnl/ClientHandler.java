package vnl;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

public class ClientHandler extends ChannelHandlerAdapter{
    private final StringProperty receivingMessageModel;
    private ClientController app;
    // card selectors
    Button btn_select;
    Button btn_deselect;
    

    public ClientHandler(ClientController inApp, StringProperty inReceivingMessageModel){
        app = inApp;
        receivingMessageModel = inReceivingMessageModel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object inMsg){
        // receive decoded data from decoder
        Data msg = (Data) inMsg;

        // room created
        if(msg.header == 4){
            Platform.runLater(()->{
                app.vbox_lobby.setVisible(false);
                app.vbox_room.setVisible(true);
                app.lbl_idRoom.setText(Integer.toString(msg.id));
                app.lbl_descriptionRoom.setText("Waiting for opponent");
                app.imgv_cardPreview.setImage(app.loadingImage);
            });

        // can not enter room
        }else if(msg.header == 6){
            Platform.runLater(()->{
                app.lbl_lobby.setText("Room not found");
            });

        // enter to room and start choose cards
        }else if(msg.header == 7){
            Platform.runLater(()->{
                app.vbox_lobby.setVisible(false);
                app.vbox_room.setVisible(true);
                app.lbl_descriptionRoom.setText("Choose 5 cards");
                app.imgv_cardPreview.setImage((null));
                // setup card listing
                for(int i=0; i<msg.list.size(); i++){
                    // initial add cards
                    btn_select = new Button("+");
                    HBox card = new HBox(
                        new Label(Integer.toString(msg.list.get(i)) + ": card title"),
                        btn_select
                    );
                    app.vbox_listAvailable.getChildren().add(card);
                    btn_select.setUserData(app.vbox_listAvailable.getChildren().size()-1); // save index
                    // select card via click on button +
                    btn_select.setOnAction(e->select(card));
                }
            });

        // start play
        }else if(msg.header == 10){
            Platform.runLater(()->{
                // setup field
                app.vbox_room.setVisible(false);
                app.vbox_field.setVisible(true);
                app.lbl_fieldRoomID.setText(app.lbl_idRoom.getText());
                for(int i=0; i<9; i++){
                    Button btn_elem = (Button) app.gp_field.getChildren().get(i);
                    // means what square is free
                    btn_elem.setUserData(new CardStatus(0));
                    btn_elem.setText("free");
                }
                // create lists of cards
                for(Integer elem: app.chosen_cards){
                    app.vbox_opponentCards.getChildren().add(new Button("opponent card"));
                    Button card = new Button(Integer.toString(elem) + " card");
                    //card.setUserData(new CardStatus(elem));
                    app.vbox_myCards.getChildren().add(card);
                }
                // initialization turn
                if(msg.id == 0){
                    app.lbl_turn.setText("Opponent turn");
                    app.lbl_action.setText("Wait");
                    for(Node elem: app.vbox_myCards.getChildren()){
                        Button btn_elem = (Button) elem;
                        btn_elem.setOnAction(null);
                    }
                }else{
                    app.lbl_turn.setText("Your turn");
                    app.lbl_action.setText("Choose card");
                    for(int i=0; i<5; i++){
                        Button btn_elem = (Button) app.vbox_myCards.getChildren().get(i);
                        btn_elem.setUserData(new CardStatus(i));
                        btn_elem.setOnAction(e->choose_card(btn_elem));
                    }
                }
            });
        }
    }

    public class CardStatus{
        public int value;
        public Boolean selectStatus;

        public CardStatus(int inValue){
            value = inValue;
            selectStatus = false;
        }
    }

    public void choose_square(Button square, int inIndex, Button card){
        // save parameters
        int cardNum = app.chosen_cards.get(((CardStatus) card.getUserData()).value);
        ((CardStatus) square.getUserData()).value = cardNum;
        ((CardStatus) square.getUserData()).selectStatus = true;
        // send chosen card to server and give turn to next player
        for(int i=0; i<9; i++){
            Button btn_elem = (Button) app.gp_field.getChildren().get(i);
            btn_elem.setOnAction(null);
        }
        Data msg = new Data(12, "", 0, null, 
            cardNum,
            inIndex / 3,
            inIndex % 3);
        
        // task what sends prepared data
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception{
                ChannelFuture f = app.channel.writeAndFlush(msg);
                f.sync();
                Platform.runLater(()->{
                    app.lbl_turn.setText("Opponent turn");
                    app.lbl_action.setText("Wait");
                    for(Node elem: app.vbox_myCards.getChildren()){
                        Button btn_elem = (Button) elem;
                        btn_elem.setOnAction(null);
                    }
                    square.setText(Integer.toString(cardNum));
                    app.vbox_myCards.getChildren().remove(((CardStatus) card.getUserData()).value);
                });
                return null;
            }
            @Override
            protected void failed(){
                Throwable e = getException();
                e.printStackTrace();
                app.disconnect();
            }
        };

        // execute of send task in separate thread
        new Thread(task).start();
    }

    public void choose_card(Button card){
        if(! ((CardStatus) card.getUserData()).selectStatus){
            app.lbl_action.setText("Choose square of board");
            // clear previuos selected cards 
            for(Node elem: app.vbox_myCards.getChildren()){
                Button btn_elem = (Button) elem;
                btn_elem.setBorder(null);
                ((CardStatus) btn_elem.getUserData()).selectStatus = false;
            }
            // highlight selected card
            card.setBorder(new Border(new BorderStroke(
                Color.BLUE, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(5)
            )));
            ((CardStatus) card.getUserData()).selectStatus = true;
            // available place card on field
            for(int i=0; i<9; i++){
                Button btn_elem = (Button) app.gp_field.getChildren().get(i);
                //if card not already selected, when available it for select
                int index = i;
                if(! ((CardStatus) btn_elem.getUserData()).selectStatus){
                    btn_elem.setOnAction(e->choose_square(btn_elem, index, card));
                }
            }
        }else{
            app.lbl_action.setText("Choose card");
            card.setBorder(null);
            ((CardStatus) card.getUserData()).selectStatus = false;
            // if no chosen card, deny place on field
            for(int i=0; i<9; i++){
                Button btn_elem = (Button) app.gp_field.getChildren().get(i);
                btn_elem.setOnAction(null);
            }
        }
    }

    // selecting card for game
    public void select(HBox inCard){
        Platform.runLater(()->{
            // add new selected card
            btn_deselect = new Button("-");
            HBox card = new HBox(
                inCard.getChildren().get(0),
                btn_deselect
            );
            app.vbox_listSelected.getChildren().add(card);
            btn_deselect.setUserData(app.vbox_listSelected.getChildren().size()-1); // save index
            // delete from selection list
            int indexToRemove = (Integer) inCard.getChildren().get(0).getUserData();
            app.vbox_listAvailable.getChildren().remove(indexToRemove);
            // update btn_selectors indexes and actions
            for(int i=indexToRemove; i < app.vbox_listAvailable.getChildren().size(); i++){
                // read data
                HBox iterated_card = (HBox) app.vbox_listAvailable.getChildren().get(indexToRemove);
                Button iterated_button = (Button) iterated_card.getChildren().get(1);
                // set new index
                iterated_button.setUserData(i);
                // update links via rotation
                app.vbox_listAvailable.getChildren().remove(indexToRemove);
                app.vbox_listAvailable.getChildren().add(iterated_card);
            }
            // deselect card via click -
            btn_deselect.setOnAction(e->deselect(card));
        });
    }

    // deselecting card for game
    public void deselect(HBox inCard){
        Platform.runLater(()->{
            btn_select = new Button("+");
            HBox card = new HBox(
                inCard.getChildren().get(0),
                btn_select
            );
            app.vbox_listAvailable.getChildren().add(card);
            btn_select.setUserData(app.vbox_listAvailable.getChildren().size()-1); // save index
            int indexToRemove = (Integer) inCard.getChildren().get(0).getUserData();
            app.vbox_listSelected.getChildren().remove(indexToRemove);
            // update btn_deselectors indexes and actions
            for(int i=indexToRemove; i < app.vbox_listSelected.getChildren().size(); i++){
                // read data
                HBox iterated_card = (HBox) app.vbox_listSelected.getChildren().get(indexToRemove);
                Button iterated_button = (Button) iterated_card.getChildren().get(1);
                // set new index
                iterated_button.setUserData(i);
                // update links via rotation
                app.vbox_listSelected.getChildren().remove(indexToRemove);
                app.vbox_listSelected.getChildren().add(iterated_card);
            }
            // select card via click on button +
            btn_select.setOnAction(e->select(card));
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        cause.printStackTrace();
        ctx.close();
    }
}
