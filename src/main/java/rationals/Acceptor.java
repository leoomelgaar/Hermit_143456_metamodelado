package rationals;

import java.util.List;
import java.util.Set;

public interface Acceptor {
    Set<State> steps(List<?> var1);
}

