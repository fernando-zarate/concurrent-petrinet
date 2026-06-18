import java.io.FileWriter;
import java.io.IOException;

public class Logger {

    private String TRANSITIONS_LOG_PATH = "logs\\transitions_log.txt";

    public Logger() {
        // Clear the transitions log file at the beginning of the program.
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(TRANSITIONS_LOG_PATH, false);
            fileWriter.write("");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * Logs the firing of a transition to a file.
     * @param transition The index of the fired transition.
     */
    public synchronized void logTransitionFiring(int transition) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(TRANSITIONS_LOG_PATH, true);
            fileWriter.write(String.format("T%d %s\n", transition, Thread.currentThread().getName()));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
