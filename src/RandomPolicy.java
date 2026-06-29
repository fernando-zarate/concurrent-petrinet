import java.util.ArrayList;
import java.util.Random;

public class RandomPolicy implements PolicyInterface {

    private static final Random random = new Random();

    public RandomPolicy() { }

    /*
     * This policy implements a method that selects a random transition to fire among the enabled ones.
     * @param enabledTransitions An array of booleans indicating which transitions are enabled to fire.
     * @return The index of the selected transition, or -1 if no transition is enabled.
     */
    @Override
    public int selectTransition(boolean[] enabledTransitions) {

        // Create a list of the indexes of the enabled transitions.
        ArrayList<Integer> enabledTransitionIndexes = new ArrayList<>();
        for (int i = 0; i < enabledTransitions.length; i++) {
            if (enabledTransitions[i] == true) {
                enabledTransitionIndexes.add(i);
            }
        }

        // If there are no enabled transitions, return -1 (Should not happen).
        if (enabledTransitionIndexes.isEmpty()) {
            return -1;
        }

        // Select a random index from the list of enabled transitions and return it.
        int randomIndex = random.nextInt(enabledTransitionIndexes.size());
        return enabledTransitionIndexes.get(randomIndex);
    }
}
