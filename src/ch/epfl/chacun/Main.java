package ch.epfl.chacun;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        try {
            new WebSocketServer("0.0.0.0", 3000);
            // Prevent the program from exiting
            System.in.read();
        } catch (Exception ex) {
            Logger.getLogger(String.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
