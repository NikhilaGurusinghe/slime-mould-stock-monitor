package slimemouldstock.monitor.state;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import slimemouldstock.monitor.http.HttpClient;

public class StateManager {
    // TODO update this default state
    private final State DEFAULT_STATE = new GrowthState(this);
    private State state;

    protected final HttpClient httpClient;

    public StateManager(String connectionURL) throws MalformedURLException {
        this.state = DEFAULT_STATE;
        DEFAULT_STATE.onEnterState();
        this.httpClient = new HttpClient(connectionURL);
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
