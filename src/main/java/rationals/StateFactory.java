package rationals;

import java.util.Set;

public interface StateFactory {
    State create(boolean var1, boolean var2);

    Set<State> stateSet();

    Object clone() throws CloneNotSupportedException;
}

