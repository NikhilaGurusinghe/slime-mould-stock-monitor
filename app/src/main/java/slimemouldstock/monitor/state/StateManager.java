package slimemouldstock.monitor.state;

import java.net.MalformedURLException;

import slimemouldstock.monitor.http.RetrofitClient;

public class StateManager {
    // TODO update this default state
    private final State DEFAULT_STATE = new GrowthState(this);
    private State state;

    protected final RetrofitClient httpClient;

    public StateManager(String connectionURL) throws MalformedURLException {
        this.state = DEFAULT_STATE;
        DEFAULT_STATE.onEnterState();
        this.httpClient = new RetrofitClient(connectionURL);
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
