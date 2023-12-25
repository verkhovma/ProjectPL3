package vnl;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

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
        }
    }

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
