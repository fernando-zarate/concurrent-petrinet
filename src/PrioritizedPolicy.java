public class PrioritizedPolicy implements PolicyInterface {

    /*
     * The index of the prioritized transition to be fired if it is enabled.
     */
    private static final int PRIORITIZED_TRANSITION = 4;

    /*
     * A random policy instance for selecting transitions when the prioritized one is not enabled.
     */
    private static final RandomPolicy randomPolicy = new RandomPolicy();

    public PrioritizedPolicy() { }

    /*
     * This policy implements a method that selects the most prioritized transition to fire.
     * @param enabledTransitions An array of booleans indicating which transitions are enabled to fire.
     * @return The index of the selected transition, or -1 if no transition is enabled.
     */
    @Override
    public int selectTransition(boolean[] enabledTransitions) {
        if (enabledTransitions[PRIORITIZED_TRANSITION] == true) {
            return PRIORITIZED_TRANSITION;
        }

        // If the prioritized transition is not enabled, use a random selection.
        return randomPolicy.selectTransition(enabledTransitions);
    }
}
