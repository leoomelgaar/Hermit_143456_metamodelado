package rationals;

import java.util.Set;

public interface Rational {
    State addState(boolean var1, boolean var2);

    Set<Object> alphabet();

    Set<State> states();

    Set<State> initials();

    Set<State> terminals();

    Set<State> accessibleStates();

    Set<State> coAccessibleStates();

    Set<State> accessibleAndCoAccessibleStates();

    Set<Transition> delta();

    Set<Transition> delta(State var1, Object var2);

    Set<Transition> delta(State var1);

    Set<Transition> deltaFrom(State var1, State var2);

    Set<Transition> deltaMinusOne(State var1, Object var2);

    boolean addTransition(Transition var1);

    boolean validTransition(Transition var1);

    boolean addTransition(Transition var1, String var2);

    Set<Transition> deltaMinusOne(State var1);
}

