package slimemouldstock.monitor.state;

import java.net.MalformedURLException;

public class StateManager {
    // TODO update this default state
    private final State DEFAULT_STATE = new TrayOutState(this); //new GrowthState(this);
    private State state;

    // TODO this should be elsewhere
    protected int growthCycleNumber = 0;

    public StateManager(String connectionURL) throws MalformedURLException {
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
