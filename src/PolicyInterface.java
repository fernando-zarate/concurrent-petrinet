public interface PolicyInterface {

    /*
     * Selects a transition to fire based on the implemented policy.
     * @param enabledTransitions An array of booleans indicating which transitions are enabled to fire.
     * @return The index of the selected transition, or -1 if no transition is enabled.
     */
    public abstract int selectTransition(boolean[] enabledTransitions);
}
