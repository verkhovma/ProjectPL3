package vnl;

import java.util.ArrayList;

public class Data {
    /* Data frame header:
     * 1. запрос аутентификации
     * 2. ответ аутентификации
     * 3. запрос создания комнаты
     * 4. ответ создания комнаты
     * 5. запрос присоединения к комнате
     * 6. ответ неудачного присоединения к комнате
     * 7. запрос выбора карт
     * 8. ответ выбора карт
     * 9. выход из комнаты
     * 10. стартовая позиция
     * 11. статус хода + обновление поля
     * 12. ход
     * 13. статус торговли
     * 14. торговля
     * 15. ошибка + описание
     */
    public int header = 0;

    // client: 1 username:password
    // server: 2 login, 15 error description
    public String field = "";

    // client: 5 id of room
    // server: 4 id of room, 11 turn status (your turn or opponent)
    public int id = 0;

    // client: 8 list of chosen cards
    // server: 7 list of user's cards, 13 list of trade
    public ArrayList<Integer> list = new ArrayList<>();

    // client: 12 card number, position to place: row, col, 14 trade card num
    // server: 11 card number, position of last added card
    public int card_num = 0;
    public int cli_row = 0;
    public int cli_col = 0;

    public Data(int inHeader, String inField, int inId, ArrayList<Integer> inList, int inCard_num, int inCli_row, int inCli_col){
        header = inHeader;
        field = inField;
        id = inId;
        list = inList;
        card_num = inCard_num;
        cli_row = inCli_row;
        cli_col = inCli_col;
    }   
}
