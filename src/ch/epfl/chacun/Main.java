package ch.epfl.chacun;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        try {
            new WebSocketServer("0.0.0.0", 3000);
            // Prevent the program from exiting
            new CountDownLatch(1).await();
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
