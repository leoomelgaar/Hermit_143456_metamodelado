package org.semanticweb.HermiT.datatypes.anyuri;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.BasicAutomata;
import dk.brics.automaton.BasicOperations;
import dk.brics.automaton.Datatypes;
import dk.brics.automaton.RegExp;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import org.semanticweb.HermiT.datatypes.ValueSpaceSubset;

public class AnyURIValueSpaceSubset
implements ValueSpaceSubset {
    protected static final Automaton s_anyChar = BasicAutomata.makeAnyChar();
    protected static final Automaton s_anyString = BasicAutomata.makeAnyString();
    protected static final Automaton s_anyURI = Datatypes.get("URI");
    protected static final Automaton s_empty = BasicAutomata.makeEmpty();
    protected final Automaton m_automaton;

    public AnyURIValueSpaceSubset(Automaton automaton) {
        this.m_automaton = automaton;
    }

    @Override
    public boolean hasCardinalityAtLeast(int number) {
        Set elements = this.m_automaton.getFiniteStrings(number);
        if (elements == null) {
            return true;
        }
        return elements.size() >= number;
    }

    @Override
    public boolean containsDataValue(Object dataValue) {
        if (dataValue instanceof URI) {
            return this.m_automaton.run(dataValue.toString());
        }
        return false;
    }

    @Override
    public void enumerateDataValues(Collection<Object> dataValues) {
        Set<String> elements = this.m_automaton.getFiniteStrings();
        if (elements == null) {
            throw new IllegalStateException("The value space range is infinite.");
        }
        for (String element : elements) {
            dataValues.add(URI.create(element));
        }
    }

    public String toString() {
        String buffer = "xsd:anyURI{" +
                this.m_automaton.toString() +
                '}';
        return buffer;
    }

    public static Automaton toAutomaton(int minLength, int maxLength) {
        assert (minLength <= maxLength);
        if (maxLength == Integer.MAX_VALUE) {
            if (minLength == 0) {
                return s_anyString;
            }
            return s_anyString.intersection(BasicOperations.repeat(s_anyChar, minLength));
        }
        return s_anyString.intersection(BasicOperations.repeat(s_anyChar, minLength, maxLength));
    }

    public static boolean isValidPattern(String pattern) {
        try {
            new RegExp(pattern);
            return true;
        }
        catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static Automaton getPatternAutomaton(String pattern) {
        return new RegExp(pattern).toAutomaton();
    }
}

