package org.semanticweb.HermiT.cli;

import java.io.PrintWriter;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;

class SupersAction
implements Action {
    final String conceptName;
    final boolean all;

    public SupersAction(String name, boolean getAll) {
        this.conceptName = name;
        this.all = getAll;
    }

    @Override
    public void run(Reasoner hermit, StatusOutput status, PrintWriter output, boolean ignoreOntologyPrefixes) {
        OWLClass owlClass;
        NodeSet<OWLClass> classes;
        String conceptUri;
        status.log(2, "Finding supers of '" + this.conceptName + "'");
        Prefixes prefixes = hermit.getPrefixes();
        String string = conceptUri = prefixes.canBeExpanded(this.conceptName) ? prefixes.expandAbbreviatedIRI(this.conceptName) : this.conceptName;
        if (conceptUri.startsWith("<") && conceptUri.endsWith(">")) {
            conceptUri = conceptUri.substring(1, conceptUri.length() - 1);
        }
        if (!hermit.isDefined(owlClass = OWLManager.createOWLOntologyManager().getOWLDataFactory().getOWLClass(IRI.create(conceptUri)))) {
            status.log(0, "Warning: class '" + conceptUri + "' was not declared in the ontology.");
        }
        if (this.all) {
            classes = hermit.getSuperClasses(owlClass, false);
            output.println("All super-classes of '" + this.conceptName + "':");
        } else {
            classes = hermit.getSuperClasses(owlClass, false);
            output.println("Direct super-classes of '" + this.conceptName + "':");
        }
        for (Node<OWLClass> set : classes) {
            for (OWLClass classInSet : set) {
                if (ignoreOntologyPrefixes) {
                    String iri = classInSet.getIRI().toString();
                    if (prefixes.canBeExpanded(iri)) {
                        output.println("\t" + prefixes.expandAbbreviatedIRI(iri));
                        continue;
                    }
                    output.println("\t" + iri);
                    continue;
                }
                output.println("\t" + prefixes.abbreviateIRI(classInSet.getIRI().toString()));
            }
        }
        output.flush();
    }
}

