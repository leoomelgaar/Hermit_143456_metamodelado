package org.semanticweb.HermiT.cli;

import gnu.getopt.Getopt;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.HermiT.monitor.Timer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;

public class CommandLine {
    public static void main(String[] argv) {
        try {
            int opt;
            URI base;
            int verbosity = 1;
            boolean ignoreOntologyPrefixes = false;
            PrintWriter output = new PrintWriter(System.out);
            String defaultPrefix = null;
            HashMap<String, String> prefixMappings = new HashMap<String, String>();
            String resultsFileLocation = null;
            boolean classifyClasses = false;
            boolean classifyOPs = false;
            boolean classifyDPs = false;
            boolean prettyPrint = false;
            LinkedList<Action> actions = new LinkedList<Action>();
            IRI conclusionIRI = null;
            Configuration config = new Configuration();
            boolean doAll = true;
            try {
                base = new URI("file", System.getProperty("user.dir") + "/", null);
            }
            catch (URISyntaxException e) {
                throw new RuntimeException("unable to create default IRI base");
            }
            LinkedList<IRI> ontologies = new LinkedList<IRI>();
            boolean didSomething = false;
            Getopt g = new Getopt("java-jar Hermit.jar", argv, Option.formatOptionsString(Option.options), Option.createLongOpts(Option.options));
            g.setOpterr(false);
            block55 : while ((opt = g.getopt()) != -1) {
            	String arg;
                switch (opt) {
                    case 104: {
                        System.exit(0);
                        didSomething = true;
                        continue block55;
                    }
                    case 86: {
                        System.exit(0);
                        didSomething = true;
                        continue block55;
                    }
                    case 118: {
                        arg = g.getOptarg();
                        if (arg == null) {
                            ++verbosity;
                            continue block55;
                        }
                        try {
                            verbosity += Integer.parseInt(arg, 10);
                            continue block55;
                        }
                        catch (NumberFormatException e) {
                            throw new UsageException("argument to --verbose must be a number");
                        }
                    }
                    case 113: {
                        arg = g.getOptarg();
                        if (arg == null) {
                            --verbosity;
                            continue block55;
                        }
                        try {
                            verbosity -= Integer.parseInt(arg, 10);
                            continue block55;
                        }
                        catch (NumberFormatException e) {
                            throw new UsageException("argument to --quiet must be a number");
                        }
                    }
                    case 111: {
                        arg = g.getOptarg();
                        if (arg == null) {
                            throw new UsageException("--output requires an argument");
                        }
                        if (arg.equals("-")) {
                            output = new PrintWriter(System.out);
                            continue block55;
                        }
                        try {
                            File file = new File(arg);
                            if (!file.exists()) {
                                file.createNewFile();
                            }
                            file = file.getAbsoluteFile();
                            output = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file)), true);
                            resultsFileLocation = file.getAbsolutePath();
                            continue block55;
                        }
                        catch (FileNotFoundException e) {
                            throw new IllegalArgumentException("unable to open " + arg + " for writing");
                        }
                        catch (SecurityException e) {
                            throw new IllegalArgumentException("unable to write to " + arg);
                        }
                        catch (IOException e) {
                            throw new IllegalArgumentException("unable to write to " + arg + ": " + e.getMessage());
                        }
                    }
                    case 1013: {
                        arg = g.getOptarg();
                        if (arg == null) {
                            throw new UsageException("--premise requires a IRI as argument");
                        }
                        ontologies.add(IRI.create(arg));
                        continue block55;
                    }
                    case 1014: {
                        arg = g.getOptarg();
                        if (arg == null) {
                            throw new UsageException("--conclusion requires a IRI as argument");
                        }
                        conclusionIRI = IRI.create(arg);
                        continue block55;
                    }
                    case 108: {
                        continue block55;
                    }
                    case 99: {
                        classifyClasses = true;
                        continue block55;
                    }
                    case 79: {
                        classifyOPs = true;
                        continue block55;
                    }
                    case 68: {
                        classifyDPs = true;
                        continue block55;
                    }
                    case 80: {
                        prettyPrint = true;
                        continue block55;
                    }
                    case 107: {
                        arg = g.getOptarg();
                        if (arg == null) {
                            arg = "http://www.w3.org/2002/07/owl#Thing";
                        }
                        actions.add(new SatisfiabilityAction(arg));
                        continue block55;
                    }
                    case 100: {
                        doAll = false;
                        continue block55;
                    }
                    case 115: {
                        arg = g.getOptarg();
                        actions.add(new SubsAction(arg, doAll));
                        doAll = true;
                        continue block55;
                    }
                    case 83: {
                        arg = g.getOptarg();
                        actions.add(new SupersAction(arg, doAll));
                        doAll = true;
                        continue block55;
                    }
                    case 101: {
                        arg = g.getOptarg();
                        actions.add(new EquivalentsAction(arg));
                        continue block55;
                    }
                    case 85: {
                        actions.add(new EquivalentsAction("http://www.w3.org/2002/07/owl#Nothing"));
                        continue block55;
                    }
                    case 69: {
                        if (conclusionIRI == null) continue block55;
                        actions.add(new EntailsAction(config, conclusionIRI));
                        continue block55;
                    }
                    case 1010: {
                        actions.add(new DumpPrefixesAction());
                        continue block55;
                    }
                    case 78: {
                        ignoreOntologyPrefixes = true;
                        continue block55;
                    }
                    case 112: {
                        arg = g.getOptarg();
                        int eqIndex = arg.indexOf(61);
                        if (eqIndex == -1) {
                            throw new IllegalArgumentException("the prefix declaration '" + arg + "' is not of the form PN=IRI.");
                        }
                        prefixMappings.put(arg.substring(0, eqIndex), arg.substring(eqIndex + 1));
                        continue block55;
                    }
                    case 1009: {
                        defaultPrefix = arg = g.getOptarg();
                        continue block55;
                    }
                    case 1007: {
                        arg = g.getOptarg();
                        try {
                            base = new URI(arg);
                            continue block55;
                        }
                        catch (URISyntaxException e) {
                            throw new IllegalArgumentException("'" + arg + "' is not a valid base URI.");
                        }
                    }
                    case 1003: {
                        arg = g.getOptarg();
                        if (arg.equalsIgnoreCase("pairwise")) {
                            config.directBlockingType = Configuration.DirectBlockingType.PAIR_WISE;
                            continue block55;
                        }
                        if (arg.equalsIgnoreCase("single")) {
                            config.directBlockingType = Configuration.DirectBlockingType.SINGLE;
                            continue block55;
                        }
                        if (arg.equalsIgnoreCase("optimal")) {
                            config.directBlockingType = Configuration.DirectBlockingType.OPTIMAL;
                            continue block55;
                        }
                        throw new UsageException("unknown direct blocking type '" + arg + "'; supported values are 'pairwise', 'single', and 'optimal'");
                    }
                    case 1004: {
                        arg = g.getOptarg();
                        if (arg.equalsIgnoreCase("anywhere")) {
                            config.blockingStrategyType = Configuration.BlockingStrategyType.ANYWHERE;
                            continue block55;
                        }
                        if (arg.equalsIgnoreCase("ancestor")) {
                            config.blockingStrategyType = Configuration.BlockingStrategyType.ANCESTOR;
                            continue block55;
                        }
                        if (arg.equalsIgnoreCase("core")) {
                            config.blockingStrategyType = Configuration.BlockingStrategyType.SIMPLE_CORE;
                            continue block55;
                        }
                        if (arg.equalsIgnoreCase("optimal")) {
                            config.blockingStrategyType = Configuration.BlockingStrategyType.OPTIMAL;
                            continue block55;
                        }
                        throw new UsageException("unknown blocking strategy type '" + arg + "'; supported values are 'ancestor' and 'anywhere'");
                    }
                    case 1005: {
                        config.blockingSignatureCacheType = Configuration.BlockingSignatureCacheType.CACHED;
                        continue block55;
                    }
                    case 1006: {
                        arg = g.getOptarg();
                        if (arg.equalsIgnoreCase("creation")) {
                            config.existentialStrategyType = Configuration.ExistentialStrategyType.CREATION_ORDER;
                            continue block55;
                        }
                        if (arg.equalsIgnoreCase("el")) {
                            config.existentialStrategyType = Configuration.ExistentialStrategyType.EL;
                            continue block55;
                        }
                        if (arg.equalsIgnoreCase("reuse")) {
                            config.existentialStrategyType = Configuration.ExistentialStrategyType.INDIVIDUAL_REUSE;
                            continue block55;
                        }
                        throw new UsageException("unknown existential strategy type '" + arg + "'; supported values are 'creation', 'el', and 'reuse'");
                    }
                    case 1012: {
                        config.ignoreUnsupportedDatatypes = true;
                        continue block55;
                    }
                    case 1015: {
                        config.throwInconsistentOntologyException = false;
                        continue block55;
                    }
                    case 1001: {
                        actions.add(new DumpClausesAction(g.getOptarg()));
                        continue block55;
                    }
                }
                if (g.getOptopt() != 0) {
                    throw new UsageException("invalid option -- " + (char)g.getOptopt());
                }
                throw new UsageException("invalid option");
            }
            for (int i = g.getOptind(); i < argv.length; ++i) {
                try {
                    ontologies.add(IRI.create(base.resolve(argv[i])));
                    continue;
                }
                catch (IllegalArgumentException e) {
                    throw new UsageException(argv[i] + " is not a valid ontology name");
                }
            }
            StatusOutput status = new StatusOutput(verbosity);
            if (verbosity > 3) {
                config.monitor = new Timer(System.err);
            }
            if (classifyClasses || classifyOPs || classifyDPs) {
                actions.add(new ClassifyAction(classifyClasses, classifyOPs, classifyDPs, prettyPrint, resultsFileLocation));
            }
            for (IRI ont : ontologies) {
                didSomething = true;
                status.log(2, "Processing " + ont.toString());
                status.log(2, actions.size() + " actions");
                try {
                    File file;
                    String scheme;
                    URI uri;
                    long startTime = System.currentTimeMillis();
                    OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
                    if (ont.isAbsolute() && (scheme = (uri = URI.create(ont.getNamespace())).getScheme()) != null && scheme.equalsIgnoreCase("file") && (file = new File(URI.create(ont.getNamespace()))).isDirectory()) {
                        AutoIRIMapper mapper = new AutoIRIMapper(file, false);
                        ontologyManager.getIRIMappers().add(mapper);
                    }
                    OWLOntology ontology = ontologyManager.loadOntology(ont);
                    long parseTime = System.currentTimeMillis() - startTime;
                    status.log(2, "Ontology parsed in " + parseTime + " msec.");
                    startTime = System.currentTimeMillis();
                    Reasoner hermit = new Reasoner(config, ontology);
                    Prefixes prefixes = hermit.getPrefixes();
                    if (defaultPrefix != null) {
                        try {
                            prefixes.declareDefaultPrefix(defaultPrefix);
                        }
                        catch (IllegalArgumentException e) {
                            status.log(2, "Default prefix " + defaultPrefix + " could not be registered because there is already a registered default prefix. ");
                        }
                    }
                    for (String prefixName : prefixMappings.keySet()) {
                        try {
                            prefixes.declarePrefix(prefixName, prefixMappings.get(prefixName));
                        }
                        catch (IllegalArgumentException e) {
                            status.log(2, "Prefixname " + prefixName + " could not be set to " + prefixMappings.get(prefixName) + " because there is already a registered prefix name for the IRI. ");
                        }
                    }
                    long loadTime = System.currentTimeMillis() - startTime;
                    status.log(2, "Reasoner created in " + loadTime + " msec.");
                    for (Action action : actions) {
                        status.log(2, "Doing action...");
                        startTime = System.currentTimeMillis();
                        action.run(hermit, status, output, ignoreOntologyPrefixes);
                        long actionTime = System.currentTimeMillis() - startTime;
                        status.log(2, "...action completed in " + actionTime + " msec.");
                    }
                }
                catch (OWLException e) {
                    System.err.println("It all went pear-shaped: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
            if (!didSomething) {
                throw new UsageException("No ontologies given.");
            }
        }
        catch (UsageException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage: hermit [OPTION]... IRI...");
            System.err.println("Try 'hermit --help' for more information.");
        }
    }
}

