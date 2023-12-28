# Разработка
Осуществляется прием сообщений клиентов и реагирование на них исходя из заголовков, 
согласно логике в файлах ServerHandler и GameRoom. 

Для сборки приложения: mvn package - команда создаст 
./target/server-1.0-SNAPSHOT-jar-with-dependencies.jar

# Запуск
Для запуска лаунчером из-под Windows скопируйте файлы ./target/server-1.0-SNAPSHOT-jar-with-dependencies.jar 
и launcher.exe в целевую папку.

Для использования вашего ip-адреса нужно разобраться с функцией b.bind(port) 
в строке ./src/main/java/vnl/Main.java:65 
и пересобрать приложение, по умолчанию запускается на localhost.
