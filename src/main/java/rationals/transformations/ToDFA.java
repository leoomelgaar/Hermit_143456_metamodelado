package rationals.transformations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;

public class ToDFA
implements UnaryTransformation {
    @Override
    public Automaton transform(Automaton a) {
        Automaton ret = new Automaton();
        HashMap<Set<State>, State> bmap = new HashMap<Set<State>, State>();
        ArrayList<Set<State>> todo = new ArrayList<Set<State>>();
        HashSet<Set> done = new HashSet<Set>();
        Set<State> as = TransformationsToolBox.epsilonClosure(a.initials(), a);
        State from = ret.addState(true, TransformationsToolBox.containsATerminalState(as));
        bmap.put(as, from);
        todo.add(as);
        do {
            Set sts = todo.remove(0);
            from = bmap.get(sts);
            if (done.contains(sts)) continue;
            done.add(sts);
            Map<Object, Set<State>> tam = TransformationsToolBox.mapAlphabet(a.delta(sts), a);
            for (Map.Entry<Object, Set<State>> me : tam.entrySet()) {
                Object l = me.getKey();
                as = me.getValue();
                Set<State> asc = TransformationsToolBox.epsilonClosure(as, a);
                State to = bmap.get(asc);
                if (to == null) {
                    to = ret.addState(false, TransformationsToolBox.containsATerminalState(asc));
                    bmap.put(asc, to);
                }
                todo.add(asc);
                boolean valid = ret.addTransition(new Transition(from, l, to), null);
                assert (valid);
            }
        } while (!todo.isEmpty());
        return ret;
    }
}

