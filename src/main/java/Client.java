import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws Exception {

        NetworkClient networkClient = new NetworkClient();
        networkClient.connect();

    }
}