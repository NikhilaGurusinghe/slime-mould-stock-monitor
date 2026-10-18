package slimemouldstock.monitor.State;

public class StateManager {
    private State state;

    public State getState() {
        return this.state;
    }

    public void setState(State nextState) {
        this.state.onExitState();
        this.state = nextState;
        nextState.onEnterState();
    }
}
