package org.semanticweb.HermiT.cli;

import java.io.PrintWriter;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.reasoner.Node;

class EquivalentsAction
implements Action {
    final String conceptName;

    public EquivalentsAction(String name) {
        this.conceptName = name;
    }

    @Override
    public void run(Reasoner hermit, StatusOutput status, PrintWriter output, boolean ignoreOntologyPrefixes) {
        OWLClass owlClass;
        String conceptUri;
        status.log(2, "Finding equivalents of '" + this.conceptName + "'");
        Prefixes prefixes = hermit.getPrefixes();
        String string = conceptUri = prefixes.canBeExpanded(this.conceptName) ? prefixes.expandAbbreviatedIRI(this.conceptName) : this.conceptName;
        if (conceptUri.startsWith("<") && conceptUri.endsWith(">")) {
            conceptUri = conceptUri.substring(1, conceptUri.length() - 1);
        }
        if (!hermit.isDefined(owlClass = OWLManager.createOWLOntologyManager().getOWLDataFactory().getOWLClass(IRI.create(conceptUri)))) {
            status.log(0, "Warning: class '" + this.conceptName + "' was not declared in the ontology.");
        }
        Node<OWLClass> classes = hermit.getEquivalentClasses(owlClass);
        if (ignoreOntologyPrefixes) {
            output.println("Classes equivalent to '" + this.conceptName + "':");
        } else {
            output.println("Classes equivalent to '" + prefixes.abbreviateIRI(this.conceptName) + "':");
        }
        for (OWLClass classInSet : classes) {
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
        output.flush();
    }
}

