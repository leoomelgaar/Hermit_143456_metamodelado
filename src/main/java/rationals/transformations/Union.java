package rationals.transformations;

import java.util.HashMap;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;

public class Union
implements BinaryTransformation {
    @Override
    public Automaton transform(Automaton a, Automaton b) {
        Automaton ap = (Automaton)a.clone();
        HashMap<State, State> map = new HashMap<State, State>();
        for (State e : b.states()) {
            map.put(e, ap.addState(e.isInitial(), e.isTerminal()));
        }
        for (Transition t : b.delta()) {
            ap.addTransition(new Transition(map.get(t.start()), t.label(), map.get(t.end())), null);
        }
        return ap;
    }
}

