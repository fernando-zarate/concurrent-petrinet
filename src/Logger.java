import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class Logger {

    private static final String TRANSITIONS_LOG_PATH = "logs\\transitions_log.txt";
    private static final String DEBUG_TRANSITIONS_LOG_PATH = "logs\\debug_transitions_log.txt";
    
    private long startingTime;

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
        // Clear the debug transitions log file at the beginning of the program.
        fileWriter = null;
        try {
            fileWriter = new FileWriter(DEBUG_TRANSITIONS_LOG_PATH, false);
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
        // Set the starting time of the program to calculate the elapsed time for logging purposes.
        startingTime = System.currentTimeMillis();
    }

    /*
     * Logs the firing of a transition to a file.
     * @param transition The index of the fired transition.
     * @param marking The current marking of the petri net.
     */
    public synchronized void logTransitionFiring(int transition, int[] marking) {
        // Log the firing of the transition to the transitions log file.
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(TRANSITIONS_LOG_PATH, true);
            fileWriter.write(String.format("T%d", transition));
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
        // Log the firing of the transition to the debug transitions log file, with more details like threads name and elapsed time for debugging purposes.
        fileWriter = null;
        try {
            fileWriter = new FileWriter(DEBUG_TRANSITIONS_LOG_PATH, true);
            fileWriter.write(String.format("T%d %s %s %d\n", transition, Arrays.toString(marking), Thread.currentThread().getName(), System.currentTimeMillis() - startingTime));
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
