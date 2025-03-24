package rationals;

import java.util.List;
import java.util.Set;

public interface StateMachine {
    Set<Object> alphabet();

    StateFactory getStateFactory();

    Set<Transition> delta(State var1, Object var2);

    Set<Transition> delta(State var1);

    Set<Transition> delta(Set<State> var1);

    Set<State> steps(Set<State> var1, List<?> var2);

    Set<State> step(Set<State> var1, Object var2);

    Set<State> initials();

    Set<Transition> deltaMinusOne(State var1);
}

