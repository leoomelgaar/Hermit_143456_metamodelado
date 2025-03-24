package org.semanticweb.HermiT.datatypes.rdfplainliteral;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.BasicAutomata;
import dk.brics.automaton.BasicOperations;
import dk.brics.automaton.Datatypes;
import dk.brics.automaton.RegExp;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.HermiT.datatypes.ValueSpaceSubset;

public class RDFPlainLiteralPatternValueSpaceSubset
implements ValueSpaceSubset {
    public static final char SEPARATOR = '\u0001';
    protected static final Automaton s_separator = BasicAutomata.makeChar('\u0001');
    protected static final Automaton s_languagePatternEnd = BasicOperations.optional(BasicAutomata.makeChar('-').concatenate(BasicAutomata.makeAnyString()));
    protected static final Automaton s_languageTag = RDFPlainLiteralPatternValueSpaceSubset.languageTagAutomaton();
    protected static final Automaton s_languageTagOrEmpty = s_languageTag.union(BasicAutomata.makeEmptyString());
    protected static final Automaton s_emptyLangTag = s_separator;
    protected static final Automaton s_nonemptyLangTag = s_separator.concatenate(s_languageTag);
    protected static final Automaton s_anyLangTag = s_separator.concatenate(s_languageTagOrEmpty);
    protected static final Automaton s_xsdString = Datatypes.get("string");
    protected static final Map<String, Automaton> s_anyDatatype = new HashMap<String, Automaton>();
    protected static final Automaton s_anyString;
    protected static final Automaton s_anyChar;
    protected static final Automaton s_anyStringWithNonemptyLangTag;
    protected final Automaton m_automaton;

    protected static Automaton languageTagAutomaton() {
        return new RegExp("(([a-zA-Z]{2,3}((-[a-zA-Z]{3}){0,3})?)|[a-zA-Z]{4}|[a-zA-Z]{5,8})(-[a-zA-Z]{4})?(-([a-zA-Z]{2}|[0-9]{3}))?(-([a-zA-Z0-9]{5,8}|([0-9][a-z0-9]{3})))*(-([a-wy-zA-WY-Z0-9](-[a-zA-Z0-9]{2,8})+))*(-x(-[a-zA-Z0-9]{1,8})+)?").toAutomaton();
    }

    protected static Automaton xmlChar() {
        return new RegExp("[\t\n -\u00a0-\ud7ff\ue000-\ufffd]").toAutomaton();
    }

    protected static Automaton normalizedStringAutomaton() {
        return new RegExp("([ -\u00a0-\ud7ff\ue000-\ufffd])*").toAutomaton();
    }

    protected static Automaton tokenAutomaton() {
        return new RegExp("([!-\ud7ff\ue000-\ufffd]+( [!-\ud7ff\ue000-\ufffd]+)*)?").toAutomaton();
    }

    public RDFPlainLiteralPatternValueSpaceSubset(Automaton automaton) {
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
        if (dataValue instanceof String) {
            String string = (String)dataValue;
            return this.m_automaton.run(string + '\u0001');
        }
        if (dataValue instanceof RDFPlainLiteralDataValue) {
            RDFPlainLiteralDataValue value = (RDFPlainLiteralDataValue)dataValue;
            String string = value.getString();
            String languageTag = value.getLanguageTag().toLowerCase();
            return this.m_automaton.run(string + '\u0001' + languageTag);
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
            int separatorIndex = element.lastIndexOf(1);
            String string = element.substring(0, separatorIndex);
            String languageTag = element.substring(separatorIndex + 1);
            if (languageTag.length() == 0) {
                dataValues.add(string);
                continue;
            }
            dataValues.add(new RDFPlainLiteralDataValue(string, languageTag));
        }
    }

    public String toString() {
        return "rdf:PlainLiteral{" + this.m_automaton + "}";
    }

    public static Automaton toAutomaton(RDFPlainLiteralLengthValueSpaceSubset valueSpaceSubset) {
        List<RDFPlainLiteralLengthInterval> intervals = valueSpaceSubset.m_intervals;
        Automaton result = null;
        for (int intervalIndex = intervals.size() - 1; intervalIndex >= 0; --intervalIndex) {
            RDFPlainLiteralLengthInterval interval = intervals.get(intervalIndex);
            Automaton stringPart = interval.m_maxLength == Integer.MAX_VALUE ? (interval.m_minLength == 0 ? s_anyString : s_anyString.intersection(BasicOperations.repeat(s_anyChar, interval.m_minLength))) : s_anyString.intersection(BasicOperations.repeat(s_anyChar, interval.m_minLength, interval.m_maxLength));
            Automaton intervalAutomaton = interval.m_languageTagMode == RDFPlainLiteralLengthInterval.LanguageTagMode.ABSENT ? stringPart.concatenate(s_emptyLangTag) : stringPart.concatenate(s_nonemptyLangTag);
            result = result == null ? intervalAutomaton : result.intersection(intervalAutomaton);
        }
        return result;
    }

    public static Automaton toAutomaton(int minLength, int maxLength) {
        assert (minLength <= maxLength);
        Automaton stringPart = maxLength == Integer.MAX_VALUE ? (minLength == 0 ? s_anyString : s_anyString.intersection(BasicOperations.repeat(s_anyChar, minLength))) : s_anyString.intersection(BasicOperations.repeat(s_anyChar, minLength, maxLength));
        return stringPart.concatenate(s_anyLangTag);
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
        Automaton stringPart = new RegExp(pattern).toAutomaton();
        return stringPart.concatenate(s_anyLangTag);
    }

    public static Automaton getLanguageRangeAutomaton(String languageRange) {
        if ("*".equals(languageRange)) {
            return s_anyStringWithNonemptyLangTag;
        }
        Automaton languageTagPart = BasicAutomata.makeString(languageRange.toLowerCase()).concatenate(s_languagePatternEnd);
        return s_anyString.concatenate(s_separator).concatenate(languageTagPart);
    }

    public static Automaton getDatatypeAutomaton(String datatypeURI) {
        return s_anyDatatype.get(datatypeURI);
    }

    static {
        s_anyDatatype.put(RDFPlainLiteralDatatypeHandler.XSD_NS + "string", s_xsdString.concatenate(s_emptyLangTag));
        s_anyDatatype.put(RDFPlainLiteralDatatypeHandler.XSD_NS + "normalizedString", RDFPlainLiteralPatternValueSpaceSubset.normalizedStringAutomaton().concatenate(s_emptyLangTag));
        s_anyDatatype.put(RDFPlainLiteralDatatypeHandler.XSD_NS + "token", RDFPlainLiteralPatternValueSpaceSubset.tokenAutomaton().concatenate(s_emptyLangTag));
        s_anyDatatype.put(RDFPlainLiteralDatatypeHandler.XSD_NS + "Name", Datatypes.get("Name2").concatenate(s_emptyLangTag));
        s_anyDatatype.put(RDFPlainLiteralDatatypeHandler.XSD_NS + "NCName", Datatypes.get("NCName").concatenate(s_emptyLangTag));
        s_anyDatatype.put(RDFPlainLiteralDatatypeHandler.XSD_NS + "NMTOKEN", Datatypes.get("Nmtoken2").concatenate(s_emptyLangTag));
        s_anyDatatype.put(RDFPlainLiteralDatatypeHandler.XSD_NS + "language", Datatypes.get("language").concatenate(s_emptyLangTag));
        s_anyDatatype.put(RDFPlainLiteralDatatypeHandler.RDF_NS + "PlainLiteral", s_xsdString.concatenate(s_anyLangTag));
        s_anyChar = RDFPlainLiteralPatternValueSpaceSubset.xmlChar();
        s_anyString = s_anyChar.repeat();
        s_anyStringWithNonemptyLangTag = s_anyString.concatenate(s_nonemptyLangTag);
    }
}

