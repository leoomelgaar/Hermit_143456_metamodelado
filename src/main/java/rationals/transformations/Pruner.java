package rationals.transformations;

import java.util.HashMap;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;

public class Pruner
implements UnaryTransformation {
    @Override
    public Automaton transform(Automaton a) {
        HashMap<State, State> conversion = new HashMap<State, State>();
        Automaton b = new Automaton();
        for (State e : a.accessibleAndCoAccessibleStates()) {
            conversion.put(e, b.addState(e.isInitial(), e.isTerminal()));
        }
        for (Transition t : a.delta()) {
            State bs = conversion.get(t.start());
            State be = conversion.get(t.end());
            if (bs == null || be == null) continue;
            b.addTransition(new Transition(bs, t.label(), be), null);
        }
        return b;
    }
}

