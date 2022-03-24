import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

/**
 * Stores information about a state including whether it is an accepting state and where its out arcs lead to
 *
 * @author Salman Khan
 */
public class State implements Comparable<State> {
    String name;
    boolean accepting;
    HashMap<String, Set<State>> outArcs;

    /**
     * Creates a State object with the received 'name', false 'accepting' status and empty 'outArcs' map
     * @param name name of the state
     */
    public State(String name) {
        this.name = name;
        accepting = false;
        outArcs = new HashMap<>();
    }

    /**
     * Creates a State object with the received 'name', 'accepting' status and empty 'outArcs' map
     * @param name name of the state
     * @param accepting 'accepting' status of the State
     */
    public State(String name, boolean accepting) {
        this.name = name;
        this.accepting = accepting;
        outArcs = new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        return name.equals(((State) o).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(State o) {
        return this.name.compareTo(o.name);
    }
}