package org.semanticweb.HermiT.cli;

import gnu.getopt.LongOpt;
import java.text.BreakIterator;
import java.util.function.Function;

class Option {
    protected static final Option[] options = new Option[]{
    		new Option(104, "help", "Miscellaneous", "display this help and exit"), 
    		new Option(86, "version", "Miscellaneous", "display version information and exit"), 
    		new Option(118, "verbose", "Miscellaneous", Boolean.FALSE, "AMOUNT", "increase verbosity by AMOUNT levels (default 1)"), 
    		new Option(113, "quiet", "Miscellaneous", Boolean.FALSE, "AMOUNT", "decrease verbosity by AMOUNT levels (default 1)"), 
    		new Option(111, "output", "Miscellaneous", Boolean.TRUE, "FILE", "write output to FILE"), 
    		new Option(1013, "premise", "Miscellaneous", Boolean.TRUE, "PREMISE", "set the premise ontology to PREMISE"), 
    		new Option(1014, "conclusion", "Miscellaneous", Boolean.TRUE, "CONCLUSION", "set the conclusion ontology to CONCLUSION"), 
    		new Option(108, "load", "Actions", "parse and preprocess ontologies (default action)"), 
    		new Option(99, "classify", "Actions", "classify the classes of the ontology, optionally writing taxonomy to a file if -o (--output) is used"), 
    		new Option(79, "classifyOPs", "Actions", "classify the object properties of the ontology, optionally writing taxonomy to a file if -o (--output) is used"), 
    		new Option(68, "classifyDPs", "Actions", "classify the data properties of the ontology, optionally writing taxonomy to a file if -o (--output) is used"), 
    		new Option(80, "prettyPrint", "Actions", "when writing the classified hierarchy to a file, create a proper ontology and nicely indent the axioms according to their leven in the hierarchy"), 
    		new Option(107, "consistency", "Actions", Boolean.FALSE, "CLASS", "check satisfiability of CLASS (default owl:Thing)"), 
    		new Option(100, "direct", "Actions", "restrict next subs/supers call to only direct sub/superclasses"), 
    		new Option(115, "subs", "Actions", Boolean.TRUE, "CLASS", "output classes subsumed by CLASS (or only direct subs if following --direct)"), 
    		new Option(83, "supers", "Actions", Boolean.TRUE, "CLASS", "output classes subsuming CLASS (or only direct supers if following --direct)"), 
    		new Option(101, "equivalents", "Actions", Boolean.TRUE, "CLASS", "output classes equivalent to CLASS"), 
    		new Option(85, "unsatisfiable", "Actions", "output unsatisfiable classes (equivalent to --equivalents=owl:Nothing)"), 
    		new Option(1010, "print-prefixes", "Actions", "output prefix names available for use in identifiers"), 
    		new Option(69, "checkEntailment", "Actions", "check whether the premise (option premise) ontology entails the conclusion ontology (option conclusion)"), 
    		new Option(78, "no-prefixes", "Prefix name and IRI", "do not abbreviate or expand identifiers using prefixes defined in input ontology"), 
    		new Option(112, "prefix", "Prefix name and IRI", Boolean.TRUE, "PN=IRI", "use PN as an abbreviation for IRI in identifiers"), 
    		new Option(1009, "prefix", "Prefix name and IRI", Boolean.TRUE, "IRI", "use IRI as the default identifier prefix"), 
    		new Option(1003, "block-match", "Algorithm settings (expert users only!)", Boolean.TRUE, "TYPE", "identify blocked nodes with TYPE blocking; supported values are 'single', 'pairwise', and 'optimal' (default 'optimal')"), 
    		new Option(1004, "block-strategy", "Algorithm settings (expert users only!)", Boolean.TRUE, "TYPE", "use TYPE as blocking strategy; supported values are 'ancestor', 'anywhere', 'core', and 'optimal' (default 'optimal')"), 
    		new Option(1005, "blockersCache", "Algorithm settings (expert users only!)", "cache blocking nodes for use in later tests; not possible with nominals or core blocking"), 
    		new Option(1012, "ignoreUnsupportedDatatypes", "Algorithm settings (expert users only!)", "ignore unsupported datatypes"), 
    		new Option(1006, "expansion-strategy", "Algorithm settings (expert users only!)", Boolean.TRUE, "TYPE", "use TYPE as existential expansion strategy; supported values are 'el', 'creation', 'reuse', and 'optimal' (default 'optimal')"), 
    		new Option(1015, "noInconsistentException", "Algorithm settings (expert users only!)", "do not throw an exception for an inconsistent ontology"), 
    		new Option(1001, "dump-clauses", "Internals and debugging (unstable)", Boolean.FALSE, "FILE", "output DL-clauses to FILE (default stdout)")
    		};
    protected int optChar;
    protected String longStr;
    protected String group;
    protected Arg arg;
    protected String metavar;
    protected String help;

    public Option(int inChar, String inLong, String inGroup, String inHelp) {
        this(inChar, inLong, inGroup, null, null, inHelp);
    }

    public Option(int inChar, String inLong, String inGroup, Boolean argRequired, String inMetavar, String inHelp) {
        this.optChar = inChar;
        this.longStr = inLong;
        this.group = inGroup;
        this.arg = Arg.fromBoolean(argRequired);
        this.metavar = inMetavar;
        this.help = inHelp;
    }

    public static LongOpt[] createLongOpts(Option[] opts) {
        LongOpt[] out = new LongOpt[opts.length];
        for (int i = 0; i < opts.length; ++i) {
            out[i] = new LongOpt(opts[i].longStr, opts[i].arg.longOpt, null, opts[i].optChar);
        }
        return out;
    }

    public String getLongOptExampleStr() {
        if (this.longStr == null || this.longStr.equals("")) {
            return "";
        }
        return "--" + this.longStr + this.arg.example.apply(this.metavar);
    }

    public static String formatOptionHelp(Option[] opts) {
        StringBuilder out = new StringBuilder();
        int fieldWidth = 0;
        for (Option o : opts) {
            int curWidth = o.getLongOptExampleStr().length();
            if (curWidth <= fieldWidth) continue;
            fieldWidth = curWidth;
        }
        String curGroup = null;
        for (Option o : opts) {
            if (o.group != curGroup) {
                curGroup = o.group;
                out.append(System.getProperty("line.separator"));
                if (o.group != null) {
                    out.append(curGroup + ":");
                    out.append(System.getProperty("line.separator"));
                }
            }
            if (o.optChar < 256) {
                out.append("  -");
                out.appendCodePoint(o.optChar);
                if (o.longStr != null && o.longStr != "") {
                    out.append(", ");
                } else {
                    out.append("  ");
                }
            } else {
                out.append("      ");
            }
            int fieldLeft = fieldWidth + 1;
            if (o.longStr != null && o.longStr != "") {
                String s = o.getLongOptExampleStr();
                out.append(s);
                fieldLeft -= s.length();
            }
            while (fieldLeft > 0) {
                out.append(' ');
                --fieldLeft;
            }
            out.append(Option.breakLines(o.help, 80, 6 + fieldWidth + 1));
            out.append(System.getProperty("line.separator"));
        }
        return out.toString();
    }

    public static String formatOptionsString(Option[] opts) {
        StringBuilder out = new StringBuilder();
        for (Option o : opts) {
            if (o.optChar >= 256) continue;
            out.appendCodePoint(o.optChar);
            out.append(o.arg.format);
        }
        return out.toString();
    }

    protected static String breakLines(String str, int lineWidth, int indent) {
        StringBuilder out = new StringBuilder();
        BreakIterator i = BreakIterator.getLineInstance();
        i.setText(str);
        int curPos = 0;
        int curLinePos = indent;
        int next = i.first();
        while (next != -1) {
            String curSpan = str.substring(curPos, next);
            if (curLinePos + curSpan.length() > lineWidth) {
                out.append(System.getProperty("line.separator"));
                for (int j = 0; j < indent; ++j) {
                    out.append(" ");
                }
                curLinePos = indent;
            }
            out.append(curSpan);
            curLinePos += curSpan.length();
            curPos = next;
            next = i.next();
        }
        return out.toString();
    }

    enum Arg {
        NONE(0, "", f -> ""),
        OPTIONAL(2, "::", f -> "[=" + f + "]"),
        REQUIRED(1, ":", f -> "=" + f);
        
        int longOpt;
        String format;
        Function<String, String> example;

        Arg(int l, String format, Function<String, String> example) {
            this.longOpt = l;
            this.format = format;
            this.example = example;
        }

        public static Arg fromBoolean(Boolean b) {
            if (b == null) {
                return NONE;
            }
            if (b.booleanValue()) {
                return REQUIRED;
            }
            return OPTIONAL;
        }
    }

}

