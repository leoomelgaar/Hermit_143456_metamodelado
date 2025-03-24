package rationals.transformations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import rationals.Automaton;
import rationals.DefaultSynchronization;
import rationals.State;
import rationals.Synchronization;
import rationals.Transition;

public class Mix
implements BinaryTransformation {
    private final Synchronization synchronization;

    public Mix() {
        this.synchronization = new DefaultSynchronization();
    }

    public Mix(Synchronization synch) {
        this.synchronization = synch;
    }

    @Override
    public Automaton transform(Automaton a, Automaton b) {
        Automaton ret = new Automaton();
        Set<Object> alph = this.synchronization.synchronizable(a.alphabet(), b.alphabet());
        HashMap<StatesCouple, State> amap = new HashMap<StatesCouple, State>();
        ArrayList<StatesCouple> todo = new ArrayList<StatesCouple>();
        HashSet<StatesCouple> done = new HashSet<StatesCouple>();
        Set<State> as = TransformationsToolBox.epsilonClosure(a.initials(), a);
        Set<State> bs = TransformationsToolBox.epsilonClosure(b.initials(), b);
        State from = ret.addState(true, TransformationsToolBox.containsATerminalState(as) && TransformationsToolBox.containsATerminalState(bs));
        StatesCouple sc = new StatesCouple(as, bs);
        amap.put(sc, from);
        todo.add(sc);
        do {
            State to;
            Object l;
            StatesCouple couple = todo.remove(0);
            from = amap.get(couple);
            if (done.contains(couple)) continue;
            done.add(couple);
            Map<Object, Set<State>> tam = TransformationsToolBox.mapAlphabet(a.delta(couple.sa), a);
            Map<Object, Set<State>> tbm = TransformationsToolBox.mapAlphabet(b.delta(couple.sb), b);
            HashMap<Object, StatesCouple> tcm = new HashMap<Object, StatesCouple>();
            Iterator<Map.Entry<Object, Set<State>>> i = tam.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<Object, Set<State>> me2 = i.next();
                l = me2.getKey();
                as = me2.getValue();
                if (alph.contains(l)) continue;
                Set<State> asc = TransformationsToolBox.epsilonClosure(as, a);
                sc = new StatesCouple(asc, couple.sb);
                tcm.put(l, sc);
                to = amap.get(sc);
                if (to == null) {
                    to = ret.addState(false, TransformationsToolBox.containsATerminalState(sc.sa) && TransformationsToolBox.containsATerminalState(sc.sb));
                    amap.put(sc, to);
                }
                todo.add(sc);
                i.remove();
            }
            i = tbm.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<Object, Set<State>> me = i.next();
                l = me.getKey();
                bs = me.getValue();
                if (alph.contains(l)) continue;
                Set<State> bsc = TransformationsToolBox.epsilonClosure(bs, b);
                sc = new StatesCouple(couple.sa, bsc);
                tcm.put(l, sc);
                to = amap.get(sc);
                if (to == null) {
                    to = ret.addState(false, TransformationsToolBox.containsATerminalState(sc.sa) && TransformationsToolBox.containsATerminalState(sc.sb));
                    amap.put(sc, to);
                }
                todo.add(sc);
                i.remove();
            }
            for (Map.Entry<Object, Set<State>> me : tam.entrySet()) {
                l = me.getKey();
                as = me.getValue();
                for (Map.Entry<Object, Set<State>> mbe : tbm.entrySet()) {
                    Object k = mbe.getKey();
                    bs = mbe.getValue();
                    Object sy = this.synchronization.synchronize(l, k);
                    if (sy == null) continue;
                    Set<State> asc = TransformationsToolBox.epsilonClosure(as, a);
                    Set<State> bsc = TransformationsToolBox.epsilonClosure(bs, b);
                    sc = new StatesCouple(asc, bsc);
                    tcm.put(sy, sc);
                    State to2 = amap.get(sc);
                    if (to2 == null) {
                        to2 = ret.addState(false, TransformationsToolBox.containsATerminalState(sc.sa) && TransformationsToolBox.containsATerminalState(sc.sb));
                        amap.put(sc, to2);
                    }
                    todo.add(sc);
                }
            }
            for (Map.Entry<Object, StatesCouple> me : tcm.entrySet()) {
                l = me.getKey();
                sc = me.getValue();
                State to3 = amap.get(sc);
                if (to3 == null) {
                    to3 = ret.addState(false, TransformationsToolBox.containsATerminalState(sc.sa) && TransformationsToolBox.containsATerminalState(sc.sb));
                    amap.put(sc, to3);
                }
                ret.addTransition(new Transition(from, l, to3), null);
            }
        } while (!todo.isEmpty());
        return ret;
    }
}

