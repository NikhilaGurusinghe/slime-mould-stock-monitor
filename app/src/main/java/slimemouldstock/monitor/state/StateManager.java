package slimemouldstock.monitor.state;

public class StateManager {
    // TODO update this default state
    private final State DEFAULT_STATE = new GrowthState(this);
    private State state;


    public void State() {
        this.state = DEFAULT_STATE;
        DEFAULT_STATE.onEnterState();
    }

    public State getState() {
        return this.state;
    }

    public void setState(State nextState) {
        this.state.onExitState();
        this.state = nextState;
        nextState.onEnterState();
    }
}
