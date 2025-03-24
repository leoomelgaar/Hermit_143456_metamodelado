package rationals.expr;

import java.util.Objects;

public class Letter
extends RationalExpr {
    private final Object label;

    public Letter(Object o) {
        this.label = o;
    }

    public boolean equals(Object obj) {
        Letter lt = (Letter)obj;
        if (lt == null) {
            return false;
        }
        return Objects.equals(lt.label, this.label);
    }

    public int hashCode() {
        return this.label.hashCode();
    }

    public String toString() {
        return this.label.toString();
    }
}

