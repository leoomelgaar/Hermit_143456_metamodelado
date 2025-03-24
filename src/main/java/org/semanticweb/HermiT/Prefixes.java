package org.semanticweb.HermiT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Prefixes
implements Serializable {
    private static final long serialVersionUID = -158185482289831766L;
    protected static final String PN_CHARS_BASE = "[A-Za-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD]";
    protected static final String PN_CHARS = "[A-Za-z0-9_\\u002D\\u00B7\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0300-\\u036F\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u203F-\\u2040\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD]";
    protected static final Pattern s_localNameChecker = Pattern.compile("([A-Za-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD]|_|[0-9])(([A-Za-z0-9_\\u002D\\u00B7\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0300-\\u036F\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u203F-\\u2040\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD]|[.])*([A-Za-z0-9_\\u002D\\u00B7\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0300-\\u036F\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u203F-\\u2040\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD]))?");
    public static final Map<String, String> s_semanticWebPrefixes = new HashMap<String, String>();
    public static final Prefixes STANDARD_PREFIXES;
    protected final Map<String, String> m_prefixIRIsByPrefixName = new TreeMap<String, String>();
    protected final Map<String, String> m_prefixNamesByPrefixIRI = new TreeMap<String, String>();
    protected Pattern m_prefixIRIMatchingPattern;

    public Prefixes() {
        this.buildPrefixIRIMatchingPattern();
    }

    protected void buildPrefixIRIMatchingPattern() {
        ArrayList<String> list = new ArrayList<String>(this.m_prefixNamesByPrefixIRI.keySet());
        Collections.sort(list, new Comparator<String>(){

            @Override
            public int compare(String lhs, String rhs) {
                return rhs.length() - lhs.length();
            }
        });
        StringBuilder pattern = new StringBuilder("^(");
        boolean didOne = false;
        for (String prefixIRI : list) {
            if (didOne) {
                pattern.append("|(");
            } else {
                pattern.append("(");
                didOne = true;
            }
            pattern.append(Pattern.quote(prefixIRI));
            pattern.append(")");
        }
        pattern.append(")");
        this.m_prefixIRIMatchingPattern = didOne ? Pattern.compile(pattern.toString()) : null;
    }

    public String abbreviateIRI(String iri) {
        Matcher matcher;
        String localName;
        if (this.m_prefixIRIMatchingPattern != null && (matcher = this.m_prefixIRIMatchingPattern.matcher(iri)).find() && Prefixes.isValidLocalName(localName = iri.substring(matcher.end()))) {
            String prefix = this.m_prefixNamesByPrefixIRI.get(matcher.group(1));
            return prefix + localName;
        }
        return "<" + iri + ">";
    }

    public String expandAbbreviatedIRI(String abbreviation) {
        if (abbreviation.length() > 0 && abbreviation.charAt(0) == '<') {
            if (abbreviation.charAt(abbreviation.length() - 1) != '>') {
                throw new IllegalArgumentException("The string '" + abbreviation + "' is not a valid abbreviation: IRIs must be enclosed in '<' and '>'.");
            }
            return abbreviation.substring(1, abbreviation.length() - 1);
        }
        int pos = abbreviation.indexOf(58);
        if (pos != -1) {
            String prefix = abbreviation.substring(0, pos + 1);
            String prefixIRI = this.m_prefixIRIsByPrefixName.get(prefix);
            if (prefixIRI == null) {
                if ("http:".equals(prefix)) {
                    throw new IllegalArgumentException("The IRI '" + abbreviation + "' must be enclosed in '<' and '>' to be used as an abbreviation.");
                }
                throw new IllegalArgumentException("The string '" + prefix + "' is not a registered prefix name.");
            }
            return prefixIRI + abbreviation.substring(pos + 1);
        }
        throw new IllegalArgumentException("The abbreviation '" + abbreviation + "' is not valid (it does not start with a colon).");
    }

    public boolean canBeExpanded(String iri) {
        if (iri.length() > 0 && iri.charAt(0) == '<') {
            return false;
        }
        int pos = iri.indexOf(58);
        if (pos != -1) {
            String prefix = iri.substring(0, pos + 1);
            return this.m_prefixIRIsByPrefixName.get(prefix) != null;
        }
        return false;
    }

    public boolean declarePrefix(String prefixName, String prefixIRI) {
        boolean containsPrefix = this.declarePrefixRaw(prefixName, prefixIRI);
        this.buildPrefixIRIMatchingPattern();
        return containsPrefix;
    }

    protected boolean declarePrefixRaw(String prefixName, String prefixIRI) {
        if (!prefixName.endsWith(":")) {
            throw new IllegalArgumentException("Prefix name '" + prefixName + "' should end with a colon character.");
        }
        String existingPrefixName = this.m_prefixNamesByPrefixIRI.get(prefixIRI);
        if (existingPrefixName != null && !prefixName.equals(existingPrefixName)) {
            throw new IllegalArgumentException("The prefix IRI '" + prefixIRI + "' has already been associated with the prefix name '" + existingPrefixName + "'.");
        }
        this.m_prefixNamesByPrefixIRI.put(prefixIRI, prefixName);
        return this.m_prefixIRIsByPrefixName.put(prefixName, prefixIRI) == null;
    }

    public boolean declareDefaultPrefix(String defaultPrefixIRI) {
        return this.declarePrefix(":", defaultPrefixIRI);
    }

    public Map<String, String> getPrefixIRIsByPrefixName() {
        return Collections.unmodifiableMap(this.m_prefixIRIsByPrefixName);
    }

    public String getPrefixIRI(String prefixName) {
        return this.m_prefixIRIsByPrefixName.get(prefixName);
    }

    public String getPrefixName(String prefixIRI) {
        return this.m_prefixNamesByPrefixIRI.get(prefixIRI);
    }

    public boolean declareInternalPrefixes(Collection<String> individualIRIs, Collection<String> anonIndividualIRIs) {
        boolean containsPrefix = this.declarePrefixRaw("def:", "internal:def#");
        if (this.declarePrefixRaw("defdata:", "internal:defdata#")) {
            containsPrefix = true;
        }
        if (this.declarePrefixRaw("nnq:", "internal:nnq#")) {
            containsPrefix = true;
        }
        if (this.declarePrefixRaw("all:", "internal:all#")) {
            containsPrefix = true;
        }
        if (this.declarePrefixRaw("swrl:", "internal:swrl#")) {
            containsPrefix = true;
        }
        if (this.declarePrefixRaw("prop:", "internal:prop#")) {
            containsPrefix = true;
        }
        int individualIRIsIndex = 1;
        for (String iri : individualIRIs) {
            if (this.declarePrefixRaw("nom" + (individualIRIsIndex == 1 ? "" : String.valueOf(individualIRIsIndex)) + ":", "internal:nom#" + iri)) {
                containsPrefix = true;
            }
            ++individualIRIsIndex;
        }
        int anonymousIndividualIRIsIndex = 1;
        for (String iri : anonIndividualIRIs) {
            if (this.declarePrefixRaw("anon" + (anonymousIndividualIRIsIndex == 1 ? "" : String.valueOf(anonymousIndividualIRIsIndex)) + ":", "internal:anon#" + iri)) {
                containsPrefix = true;
            }
            ++anonymousIndividualIRIsIndex;
        }
        if (this.declarePrefixRaw("nam:", "internal:nam#")) {
            containsPrefix = true;
        }
        this.buildPrefixIRIMatchingPattern();
        return containsPrefix;
    }

    public boolean declareSemanticWebPrefixes() {
        boolean containsPrefix = false;
        for (Map.Entry<String, String> entry : s_semanticWebPrefixes.entrySet()) {
            if (!this.declarePrefixRaw(entry.getKey(), entry.getValue())) continue;
            containsPrefix = true;
        }
        this.buildPrefixIRIMatchingPattern();
        return containsPrefix;
    }

    public boolean addPrefixes(Prefixes prefixes) {
        boolean containsPrefix = false;
        for (Map.Entry<String, String> entry : prefixes.m_prefixIRIsByPrefixName.entrySet()) {
            if (!this.declarePrefixRaw(entry.getKey(), entry.getValue())) continue;
            containsPrefix = true;
        }
        this.buildPrefixIRIMatchingPattern();
        return containsPrefix;
    }

    public String toString() {
        return this.m_prefixIRIsByPrefixName.toString();
    }

    public static boolean isInternalIRI(String iri) {
        return iri.startsWith("internal:");
    }

    public static boolean isValidLocalName(String localName) {
        return s_localNameChecker.matcher(localName).matches();
    }

    static {
        s_semanticWebPrefixes.put("rdf:", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        s_semanticWebPrefixes.put("rdfs:", "http://www.w3.org/2000/01/rdf-schema#");
        s_semanticWebPrefixes.put("owl:", "http://www.w3.org/2002/07/owl#");
        s_semanticWebPrefixes.put("xsd:", "http://www.w3.org/2001/XMLSchema#");
        s_semanticWebPrefixes.put("swrl:", "http://www.w3.org/2003/11/swrl#");
        s_semanticWebPrefixes.put("swrlb:", "http://www.w3.org/2003/11/swrlb#");
        s_semanticWebPrefixes.put("swrlx:", "http://www.w3.org/2003/11/swrlx#");
        s_semanticWebPrefixes.put("ruleml:", "http://www.w3.org/2003/11/ruleml#");
        STANDARD_PREFIXES = new ImmutablePrefixes(s_semanticWebPrefixes);
    }

    public static class ImmutablePrefixes
    extends Prefixes {
        private static final long serialVersionUID = 8517988865445255837L;

        public ImmutablePrefixes(Map<String, String> initialPrefixes) {
            for (Map.Entry<String, String> entry : initialPrefixes.entrySet()) {
                super.declarePrefixRaw(entry.getKey(), entry.getValue());
            }
            this.buildPrefixIRIMatchingPattern();
        }

        @Override
        protected boolean declarePrefixRaw(String prefixName, String prefixIRI) {
            throw new UnsupportedOperationException("The well-known standard Prefix instance cannot be modified.");
        }
    }

}

