# Разработка
Для компиляции лаунчера используйте параметр g++ -mwindows, 
чтобы не появлялось консольное окно при запуске.

Для сборки приложения: mvn package - команда создаст 
./target/client-1.0-SNAPSHOT-jar-with-dependencies.jar

# Запуск
Для старта клиента из-под Windows скопируйте в целевую папку файлы:

./target/client-1.0-SNAPSHOT-jar-with-dependencies.jar
./launcher.exe

#### и всю папку:

./javafx-sdk-21

запускать из launcher.exe
для подключения к localhost можно в меню connect клиента оставить
поле адреса пустым.
