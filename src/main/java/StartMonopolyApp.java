import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.logging.LogStartupCleanup;
import processing.core.PApplet;

public class StartMonopolyApp {

    public static void main(String[] args) {
        LogStartupCleanup.deleteTodayAppLogs();
        PApplet.main(MonopolyApp.class.getCanonicalName());
    }
}
