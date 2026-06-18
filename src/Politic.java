public class Politic {

    public Politic() { }

    /*
     * Selects a transition to fire based on the politic.
     * @param enabledTransitions An array of booleans indicating which transitions are enabled to fire.
     * @return The index of the selected transition, or -1 if no transition is enabled.
     */
    public int selectTransition(boolean[] enabledTransitions) {
        for (int i = 0; i < enabledTransitions.length; i++) {
            if (enabledTransitions[i] == true) {
                return i;
            }
        }
        // If there are no enabled transitions, return -1.
        return -1;
    }
}
