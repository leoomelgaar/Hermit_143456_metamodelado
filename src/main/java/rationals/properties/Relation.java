package rationals.properties;

import java.util.Set;
import rationals.Automaton;
import rationals.State;

public interface Relation {
    void setAutomata(Automaton var1, Automaton var2);

    boolean equivalence(State var1, State var2);

    boolean equivalence(Set<State> var1, Set<State> var2);
}

