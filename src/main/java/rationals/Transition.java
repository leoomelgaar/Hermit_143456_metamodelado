package rationals;

import java.awt.Point;

public class Transition {
    private int hash = Integer.MIN_VALUE;
    private final State start;
    private final Object label;
    private final State end;

    public Transition(State start, Object label, State end) {
        this.start = start;
        this.label = label;
        this.end = end;
    }

    public State start() {
        return this.start;
    }

    public Object label() {
        return this.label;
    }

    public State end() {
        return this.end;
    }

    public String toString() {
        if (this.label == null) {
            return "(" + this.start + " , 1 , " + this.end + ")";
        }
        return "(" + this.start + " , " + this.label + " , " + this.end + ")";
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (o instanceof Transition) {
            Transition t = (Transition)o;
            if (this.label != t.label) {
                if (this.label == null || t.label == null) {
                    return false;
                }
                if (!t.label.equals(this.label)) {
                    return false;
                }
            }
            return this.start == t.start() && this.end == t.end();
        }
        return false;
    }

    public int hashCode() {
        if (this.hash != Integer.MIN_VALUE) {
            return this.hash;
        }
        int x = this.start == null ? 0 : this.start.hashCode();
        int y = this.end == null ? 0 : this.end.hashCode();
        int z = this.label == null ? 0 : this.label.hashCode();
        int t = new Point(x, y).hashCode();
        this.hash = new Point(t, z).hashCode();
        return this.hash;
    }
}

