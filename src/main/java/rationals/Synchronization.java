package rationals;

import java.util.Collection;
import java.util.Set;

public interface Synchronization {
    Object synchronize(Object var1, Object var2);

    <T> Set<T> synchronizable(Set<T> var1, Set<T> var2);

    <T> Set<T> synchronizable(Collection<Set<T>> var1);

    boolean synchronizeWith(Object var1, Set<Object> var2);
}

