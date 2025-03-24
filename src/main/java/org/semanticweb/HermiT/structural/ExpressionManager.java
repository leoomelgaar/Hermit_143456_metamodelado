package org.semanticweb.HermiT.structural;

import java.util.HashSet;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataComplementOf;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataIntersectionOf;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataOneOf;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDataUnionOf;
import org.semanticweb.owlapi.model.OWLDataVisitorEx;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLFacetRestriction;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

public class ExpressionManager {
    protected final OWLDataFactory m_factory;
    protected final DescriptionNNFVisitor m_descriptionNNFVisitor;
    protected final DataRangeNNFVisitor m_dataRangeNNFVisitor;
    protected final DescriptionComplementNNFVisitor m_descriptionComplementNNFVisitor;
    protected final DataRangeComplementNNFVisitor m_dataRangeComplementNNFVisitor;
    protected final DescriptionSimplificationVisitor m_descriptionSimplificationVisitor;
    protected final DataRangeSimplificationVisitor m_dataRangeSimplificationVisitor;

    public ExpressionManager(OWLDataFactory factory) {
        this.m_factory = factory;
        this.m_descriptionNNFVisitor = new DescriptionNNFVisitor();
        this.m_dataRangeNNFVisitor = new DataRangeNNFVisitor();
        this.m_descriptionComplementNNFVisitor = new DescriptionComplementNNFVisitor();
        this.m_dataRangeComplementNNFVisitor = new DataRangeComplementNNFVisitor();
        this.m_descriptionSimplificationVisitor = new DescriptionSimplificationVisitor();
        this.m_dataRangeSimplificationVisitor = new DataRangeSimplificationVisitor();
    }

    public OWLClassExpression getNNF(OWLClassExpression description) {
        return (OWLClassExpression)description.accept((OWLClassExpressionVisitorEx)this.m_descriptionNNFVisitor);
    }

    public OWLDataRange getNNF(OWLDataRange dataRange) {
        return (OWLDataRange)dataRange.accept((OWLDataVisitorEx)this.m_dataRangeNNFVisitor);
    }

    public OWLClassExpression getComplementNNF(OWLClassExpression description) {
        return (OWLClassExpression)description.accept((OWLClassExpressionVisitorEx)this.m_descriptionComplementNNFVisitor);
    }

    public OWLDataRange getComplementNNF(OWLDataRange dataRange) {
        return (OWLDataRange)dataRange.accept((OWLDataVisitorEx)this.m_dataRangeComplementNNFVisitor);
    }

    public OWLClassExpression getSimplified(OWLClassExpression description) {
        return (OWLClassExpression)description.accept((OWLClassExpressionVisitorEx)this.m_descriptionSimplificationVisitor);
    }

    public OWLDataRange getSimplified(OWLDataRange dataRange) {
        return (OWLDataRange)dataRange.accept((OWLDataVisitorEx)this.m_dataRangeSimplificationVisitor);
    }

    protected class DataRangeSimplificationVisitor
    implements OWLDataVisitorEx<OWLDataRange> {
        protected DataRangeSimplificationVisitor() {
        }

        public OWLDataRange visit(OWLDatatype o) {
            return o;
        }

        public OWLDataRange visit(OWLDataComplementOf o) {
            OWLDataRange dataRangeSimplified = ExpressionManager.this.getSimplified(o.getDataRange());
            if (dataRangeSimplified instanceof OWLDataComplementOf) {
                return ((OWLDataComplementOf)dataRangeSimplified).getDataRange();
            }
            return ExpressionManager.this.m_factory.getOWLDataComplementOf(dataRangeSimplified);
        }

        public OWLDataRange visit(OWLDataOneOf o) {
            return o;
        }

        public OWLDataRange visit(OWLDatatypeRestriction o) {
            return o;
        }

        public OWLDataRange visit(OWLFacetRestriction o) {
            return null;
        }

        public OWLDataRange visit(OWLLiteral o) {
            return null;
        }

        public OWLDataRange visit(OWLDataIntersectionOf range) {
            HashSet<OWLDataRange> newConjuncts = new HashSet<OWLDataRange>();
            for (OWLDataRange dr : range.getOperands()) {
                OWLDataRange drSimplified = ExpressionManager.this.getSimplified(dr);
                if (drSimplified.isTopDatatype()) continue;
                if (drSimplified instanceof OWLDataIntersectionOf) {
                    newConjuncts.addAll(((OWLDataIntersectionOf)drSimplified).getOperands());
                    continue;
                }
                newConjuncts.add(drSimplified);
            }
            return ExpressionManager.this.m_factory.getOWLDataIntersectionOf(newConjuncts);
        }

        public OWLDataRange visit(OWLDataUnionOf range) {
            HashSet<OWLDataRange> newDisjuncts = new HashSet<OWLDataRange>();
            for (OWLDataRange dr : range.getOperands()) {
                OWLDataRange drSimplified = ExpressionManager.this.getSimplified(dr);
                if (drSimplified.isTopDatatype()) {
                    return ExpressionManager.this.m_factory.getTopDatatype();
                }
                if (drSimplified instanceof OWLDataUnionOf) {
                    newDisjuncts.addAll(((OWLDataUnionOf)drSimplified).getOperands());
                    continue;
                }
                newDisjuncts.add(drSimplified);
            }
            return ExpressionManager.this.m_factory.getOWLDataUnionOf(newDisjuncts);
        }
    }

    protected class DescriptionSimplificationVisitor
    implements OWLClassExpressionVisitorEx<OWLClassExpression> {
        protected DescriptionSimplificationVisitor() {
        }

        public OWLClassExpression visit(OWLClass d) {
            return d;
        }

        public OWLClassExpression visit(OWLObjectIntersectionOf d) {
            HashSet<OWLClassExpression> newConjuncts = new HashSet<OWLClassExpression>();
            for (OWLClassExpression description : d.getOperands()) {
                OWLClassExpression descriptionSimplified = ExpressionManager.this.getSimplified(description);
                if (descriptionSimplified.isOWLThing()) continue;
                if (descriptionSimplified.isOWLNothing()) {
                    return ExpressionManager.this.m_factory.getOWLNothing();
                }
                if (descriptionSimplified instanceof OWLObjectIntersectionOf) {
                    newConjuncts.addAll(((OWLObjectIntersectionOf)descriptionSimplified).getOperands());
                    continue;
                }
                newConjuncts.add(descriptionSimplified);
            }
            return ExpressionManager.this.m_factory.getOWLObjectIntersectionOf(newConjuncts);
        }

        public OWLClassExpression visit(OWLObjectUnionOf d) {
            HashSet<OWLClassExpression> newDisjuncts = new HashSet<OWLClassExpression>();
            for (OWLClassExpression description : d.getOperands()) {
                OWLClassExpression descriptionSimplified = ExpressionManager.this.getSimplified(description);
                if (descriptionSimplified.isOWLThing()) {
                    return ExpressionManager.this.m_factory.getOWLThing();
                }
                if (descriptionSimplified.isOWLNothing()) continue;
                if (descriptionSimplified instanceof OWLObjectUnionOf) {
                    newDisjuncts.addAll(((OWLObjectUnionOf)descriptionSimplified).getOperands());
                    continue;
                }
                newDisjuncts.add(descriptionSimplified);
            }
            return ExpressionManager.this.m_factory.getOWLObjectUnionOf(newDisjuncts);
        }

        public OWLClassExpression visit(OWLObjectComplementOf d) {
            OWLClassExpression operandSimplified = ExpressionManager.this.getSimplified(d.getOperand());
            if (operandSimplified.isOWLThing()) {
                return ExpressionManager.this.m_factory.getOWLNothing();
            }
            if (operandSimplified.isOWLNothing()) {
                return ExpressionManager.this.m_factory.getOWLThing();
            }
            if (operandSimplified instanceof OWLObjectComplementOf) {
                return ((OWLObjectComplementOf)operandSimplified).getOperand();
            }
            return ExpressionManager.this.m_factory.getOWLObjectComplementOf(operandSimplified);
        }

        public OWLClassExpression visit(OWLObjectOneOf d) {
            return d;
        }

        public OWLClassExpression visit(OWLObjectSomeValuesFrom d) {
            OWLClassExpression filler = ExpressionManager.this.getSimplified(d.getFiller());
            if (filler.isOWLNothing()) {
                return ExpressionManager.this.m_factory.getOWLNothing();
            }
            return ExpressionManager.this.m_factory.getOWLObjectSomeValuesFrom(d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLObjectAllValuesFrom d) {
            OWLClassExpression filler = ExpressionManager.this.getSimplified(d.getFiller());
            if (filler.isOWLThing()) {
                return ExpressionManager.this.m_factory.getOWLThing();
            }
            return ExpressionManager.this.m_factory.getOWLObjectAllValuesFrom(d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLObjectHasValue d) {
            OWLObjectOneOf nominal = ExpressionManager.this.m_factory.getOWLObjectOneOf(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLObjectSomeValuesFrom(d.getProperty(), nominal);
        }

        public OWLClassExpression visit(OWLObjectHasSelf d) {
            return ExpressionManager.this.m_factory.getOWLObjectHasSelf(d.getProperty());
        }

        public OWLClassExpression visit(OWLObjectMinCardinality d) {
            OWLClassExpression filler = ExpressionManager.this.getSimplified(d.getFiller());
            if (d.getCardinality() <= 0) {
                return ExpressionManager.this.m_factory.getOWLThing();
            }
            if (filler.isOWLNothing()) {
                return ExpressionManager.this.m_factory.getOWLNothing();
            }
            if (d.getCardinality() == 1) {
                return ExpressionManager.this.m_factory.getOWLObjectSomeValuesFrom(d.getProperty(), filler);
            }
            return ExpressionManager.this.m_factory.getOWLObjectMinCardinality(d.getCardinality(), d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLObjectMaxCardinality d) {
            OWLClassExpression filler = ExpressionManager.this.getSimplified(d.getFiller());
            if (filler.isOWLNothing()) {
                return ExpressionManager.this.m_factory.getOWLThing();
            }
            if (d.getCardinality() <= 0) {
                return ExpressionManager.this.m_factory.getOWLObjectAllValuesFrom(d.getProperty(), ExpressionManager.this.m_factory.getOWLObjectComplementOf(filler));
            }
            return ExpressionManager.this.m_factory.getOWLObjectMaxCardinality(d.getCardinality(), d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLObjectExactCardinality d) {
            OWLClassExpression filler = ExpressionManager.this.getSimplified(d.getFiller());
            if (d.getCardinality() < 0) {
                return ExpressionManager.this.m_factory.getOWLNothing();
            }
            if (d.getCardinality() == 0) {
                return ExpressionManager.this.m_factory.getOWLObjectAllValuesFrom(d.getProperty(), ExpressionManager.this.m_factory.getOWLObjectComplementOf(filler));
            }
            if (filler.isOWLNothing()) {
                return ExpressionManager.this.m_factory.getOWLNothing();
            }
            OWLObjectMinCardinality minCardinality = ExpressionManager.this.m_factory.getOWLObjectMinCardinality(d.getCardinality(), d.getProperty(), filler);
            OWLObjectMaxCardinality maxCardinality = ExpressionManager.this.m_factory.getOWLObjectMaxCardinality(d.getCardinality(), d.getProperty(), filler);
            return ExpressionManager.this.m_factory.getOWLObjectIntersectionOf(minCardinality, maxCardinality);
        }

        public OWLClassExpression visit(OWLDataSomeValuesFrom d) {
            OWLDataRange filler = ExpressionManager.this.getSimplified(d.getFiller());
            if (this.isBottomDataRange(filler)) {
                return ExpressionManager.this.m_factory.getOWLNothing();
            }
            return ExpressionManager.this.m_factory.getOWLDataSomeValuesFrom(d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLDataAllValuesFrom d) {
            OWLDataRange filler = ExpressionManager.this.getSimplified(d.getFiller());
            if (filler.isTopDatatype()) {
                return ExpressionManager.this.m_factory.getOWLThing();
            }
            return ExpressionManager.this.m_factory.getOWLDataAllValuesFrom(d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLDataHasValue d) {
            OWLDataOneOf nominal = ExpressionManager.this.m_factory.getOWLDataOneOf(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLDataSomeValuesFrom(d.getProperty(), nominal);
        }

        public OWLClassExpression visit(OWLDataMinCardinality d) {
            OWLDataRange filler = ExpressionManager.this.getSimplified(d.getFiller());
            if (d.getCardinality() <= 0) {
                return ExpressionManager.this.m_factory.getOWLThing();
            }
            if (this.isBottomDataRange(filler)) {
                return ExpressionManager.this.m_factory.getOWLNothing();
            }
            if (d.getCardinality() == 1) {
                return ExpressionManager.this.m_factory.getOWLDataSomeValuesFrom(d.getProperty(), filler);
            }
            return ExpressionManager.this.m_factory.getOWLDataMinCardinality(d.getCardinality(), d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLDataMaxCardinality d) {
            OWLDataRange filler = ExpressionManager.this.getSimplified(d.getFiller());
            if (this.isBottomDataRange(filler)) {
                return ExpressionManager.this.m_factory.getOWLThing();
            }
            if (d.getCardinality() <= 0) {
                return ExpressionManager.this.m_factory.getOWLDataAllValuesFrom(d.getProperty(), ExpressionManager.this.m_factory.getOWLDataComplementOf(filler));
            }
            return ExpressionManager.this.m_factory.getOWLDataMaxCardinality(d.getCardinality(), d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLDataExactCardinality d) {
            OWLDataRange filler = ExpressionManager.this.getSimplified(d.getFiller());
            if (d.getCardinality() < 0) {
                return ExpressionManager.this.m_factory.getOWLNothing();
            }
            if (d.getCardinality() == 0) {
                return ExpressionManager.this.m_factory.getOWLDataAllValuesFrom(d.getProperty(), ExpressionManager.this.m_factory.getOWLDataComplementOf(filler));
            }
            if (this.isBottomDataRange(filler)) {
                return ExpressionManager.this.m_factory.getOWLNothing();
            }
            OWLDataMinCardinality minCardinality = ExpressionManager.this.m_factory.getOWLDataMinCardinality(d.getCardinality(), d.getProperty(), filler);
            OWLDataMaxCardinality maxCardinality = ExpressionManager.this.m_factory.getOWLDataMaxCardinality(d.getCardinality(), d.getProperty(), filler);
            return ExpressionManager.this.m_factory.getOWLObjectIntersectionOf(minCardinality, maxCardinality);
        }

        protected boolean isBottomDataRange(OWLDataRange dataRange) {
            return dataRange instanceof OWLDataComplementOf && ((OWLDataComplementOf)dataRange).getDataRange().isTopDatatype();
        }
    }

    protected class DataRangeComplementNNFVisitor
    implements OWLDataVisitorEx<OWLDataRange> {
        protected DataRangeComplementNNFVisitor() {
        }

        public OWLDataRange visit(OWLDatatype o) {
            return ExpressionManager.this.m_factory.getOWLDataComplementOf(o);
        }

        public OWLDataRange visit(OWLDataComplementOf o) {
            return ExpressionManager.this.getNNF(o.getDataRange());
        }

        public OWLDataRange visit(OWLDataOneOf o) {
            return ExpressionManager.this.m_factory.getOWLDataComplementOf(o);
        }

        public OWLDataRange visit(OWLDatatypeRestriction o) {
            return ExpressionManager.this.m_factory.getOWLDataComplementOf(o);
        }

        public OWLDataRange visit(OWLFacetRestriction o) {
            return null;
        }

        public OWLDataRange visit(OWLLiteral o) {
            return null;
        }

        public OWLDataRange visit(OWLDataIntersectionOf range) {
            HashSet<OWLDataRange> newDisjuncts = new HashSet<OWLDataRange>();
            for (OWLDataRange dr : range.getOperands()) {
                newDisjuncts.add(ExpressionManager.this.getComplementNNF(dr));
            }
            return ExpressionManager.this.m_factory.getOWLDataUnionOf(newDisjuncts);
        }

        public OWLDataRange visit(OWLDataUnionOf range) {
            HashSet<OWLDataRange> newConjuncts = new HashSet<OWLDataRange>();
            for (OWLDataRange dr : range.getOperands()) {
                newConjuncts.add(ExpressionManager.this.getComplementNNF(dr));
            }
            return ExpressionManager.this.m_factory.getOWLDataIntersectionOf(newConjuncts);
        }
    }

    protected class DescriptionComplementNNFVisitor
    implements OWLClassExpressionVisitorEx<OWLClassExpression> {
        protected DescriptionComplementNNFVisitor() {
        }

        public OWLClassExpression visit(OWLClass d) {
            if (d.isOWLThing()) {
                return ExpressionManager.this.m_factory.getOWLNothing();
            }
            if (d.isOWLNothing()) {
                return ExpressionManager.this.m_factory.getOWLThing();
            }
            return ExpressionManager.this.m_factory.getOWLObjectComplementOf(d);
        }

        public OWLClassExpression visit(OWLObjectIntersectionOf d) {
            HashSet<OWLClassExpression> newDisjuncts = new HashSet<OWLClassExpression>();
            for (OWLClassExpression description : d.getOperands()) {
                newDisjuncts.add(ExpressionManager.this.getComplementNNF(description));
            }
            return ExpressionManager.this.m_factory.getOWLObjectUnionOf(newDisjuncts);
        }

        public OWLClassExpression visit(OWLObjectUnionOf d) {
            HashSet<OWLClassExpression> newConjuncts = new HashSet<OWLClassExpression>();
            for (OWLClassExpression description : d.getOperands()) {
                newConjuncts.add(ExpressionManager.this.getComplementNNF(description));
            }
            return ExpressionManager.this.m_factory.getOWLObjectIntersectionOf(newConjuncts);
        }

        public OWLClassExpression visit(OWLObjectComplementOf d) {
            return ExpressionManager.this.getNNF(d.getOperand());
        }

        public OWLClassExpression visit(OWLObjectOneOf d) {
            return ExpressionManager.this.m_factory.getOWLObjectComplementOf(d);
        }

        public OWLClassExpression visit(OWLObjectSomeValuesFrom d) {
            OWLClassExpression filler = ExpressionManager.this.getComplementNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLObjectAllValuesFrom(d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLObjectAllValuesFrom d) {
            OWLClassExpression filler = ExpressionManager.this.getComplementNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLObjectSomeValuesFrom(d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLObjectHasValue d) {
            return ExpressionManager.this.m_factory.getOWLObjectComplementOf(ExpressionManager.this.getNNF(d));
        }

        public OWLClassExpression visit(OWLObjectHasSelf d) {
            return ExpressionManager.this.m_factory.getOWLObjectComplementOf(ExpressionManager.this.getNNF(d));
        }

        public OWLClassExpression visit(OWLObjectMinCardinality d) {
            if (d.getCardinality() == 0) {
                return ExpressionManager.this.m_factory.getOWLNothing();
            }
            OWLClassExpression filler = ExpressionManager.this.getNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLObjectMaxCardinality(d.getCardinality() - 1, d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLObjectMaxCardinality d) {
            OWLClassExpression filler = ExpressionManager.this.getNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLObjectMinCardinality(d.getCardinality() + 1, d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLObjectExactCardinality d) {
            OWLClassExpression filler = ExpressionManager.this.getNNF(d.getFiller());
            if (d.getCardinality() == 0) {
                return ExpressionManager.this.m_factory.getOWLObjectMinCardinality(1, d.getProperty(), filler);
            }
            HashSet<OWLClassExpression> disjuncts = new HashSet<OWLClassExpression>();
            disjuncts.add(ExpressionManager.this.m_factory.getOWLObjectMaxCardinality(d.getCardinality() - 1, d.getProperty(), filler));
            disjuncts.add(ExpressionManager.this.m_factory.getOWLObjectMinCardinality(d.getCardinality() + 1, d.getProperty(), filler));
            return ExpressionManager.this.m_factory.getOWLObjectUnionOf(disjuncts);
        }

        public OWLClassExpression visit(OWLDataSomeValuesFrom d) {
            OWLDataRange filler = ExpressionManager.this.getComplementNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLDataAllValuesFrom(d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLDataAllValuesFrom d) {
            OWLDataRange filler = ExpressionManager.this.getComplementNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLDataSomeValuesFrom(d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLDataHasValue d) {
            return ExpressionManager.this.m_factory.getOWLObjectComplementOf(d);
        }

        public OWLClassExpression visit(OWLDataMinCardinality d) {
            if (d.getCardinality() == 0) {
                return ExpressionManager.this.m_factory.getOWLNothing();
            }
            OWLDataRange filler = ExpressionManager.this.getNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLDataMaxCardinality(d.getCardinality() - 1, d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLDataMaxCardinality d) {
            OWLDataRange filler = ExpressionManager.this.getNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLDataMinCardinality(d.getCardinality() + 1, d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLDataExactCardinality d) {
            OWLDataRange filler = ExpressionManager.this.getNNF(d.getFiller());
            if (d.getCardinality() == 0) {
                return ExpressionManager.this.m_factory.getOWLDataMinCardinality(1, d.getProperty(), filler);
            }
            HashSet<OWLClassExpression> disjuncts = new HashSet<OWLClassExpression>();
            disjuncts.add(ExpressionManager.this.m_factory.getOWLDataMaxCardinality(d.getCardinality() - 1, d.getProperty(), filler));
            disjuncts.add(ExpressionManager.this.m_factory.getOWLDataMinCardinality(d.getCardinality() + 1, d.getProperty(), filler));
            return ExpressionManager.this.m_factory.getOWLObjectUnionOf(disjuncts);
        }
    }

    protected class DataRangeNNFVisitor
    implements OWLDataVisitorEx<OWLDataRange> {
        protected DataRangeNNFVisitor() {
        }

        public OWLDataRange visit(OWLDatatype o) {
            return o;
        }

        public OWLDataRange visit(OWLDataComplementOf o) {
            return ExpressionManager.this.getComplementNNF(o.getDataRange());
        }

        public OWLDataRange visit(OWLDataOneOf o) {
            return o;
        }

        public OWLDataRange visit(OWLDataRange o) {
            return o;
        }

        public OWLDataRange visit(OWLDatatypeRestriction o) {
            return o;
        }

        public OWLDataRange visit(OWLFacetRestriction node) {
            return null;
        }

        public OWLDataRange visit(OWLLiteral o) {
            return null;
        }

        public OWLDataRange visit(OWLDataIntersectionOf range) {
            HashSet<OWLDataRange> newConjuncts = new HashSet<OWLDataRange>();
            for (OWLDataRange dr : range.getOperands()) {
                newConjuncts.add(ExpressionManager.this.getNNF(dr));
            }
            return ExpressionManager.this.m_factory.getOWLDataIntersectionOf(newConjuncts);
        }

        public OWLDataRange visit(OWLDataUnionOf range) {
            HashSet<OWLDataRange> newDisjuncts = new HashSet<OWLDataRange>();
            for (OWLDataRange dr : range.getOperands()) {
                newDisjuncts.add(ExpressionManager.this.getNNF(dr));
            }
            return ExpressionManager.this.m_factory.getOWLDataUnionOf(newDisjuncts);
        }
    }

    protected class DescriptionNNFVisitor
    implements OWLClassExpressionVisitorEx<OWLClassExpression> {
        protected DescriptionNNFVisitor() {
        }

        public OWLClassExpression visit(OWLClass d) {
            return d;
        }

        public OWLClassExpression visit(OWLObjectIntersectionOf d) {
            HashSet<OWLClassExpression> newConjuncts = new HashSet<OWLClassExpression>();
            for (OWLClassExpression description : d.getOperands()) {
                OWLClassExpression descriptionNNF = ExpressionManager.this.getNNF(description);
                newConjuncts.add(descriptionNNF);
            }
            return ExpressionManager.this.m_factory.getOWLObjectIntersectionOf(newConjuncts);
        }

        public OWLClassExpression visit(OWLObjectUnionOf d) {
            HashSet<OWLClassExpression> newDisjuncts = new HashSet<OWLClassExpression>();
            for (OWLClassExpression description : d.getOperands()) {
                OWLClassExpression descriptionNNF = ExpressionManager.this.getNNF(description);
                newDisjuncts.add(descriptionNNF);
            }
            return ExpressionManager.this.m_factory.getOWLObjectUnionOf(newDisjuncts);
        }

        public OWLClassExpression visit(OWLObjectComplementOf d) {
            return ExpressionManager.this.getComplementNNF(d.getOperand());
        }

        public OWLClassExpression visit(OWLObjectOneOf d) {
            return d;
        }

        public OWLClassExpression visit(OWLObjectSomeValuesFrom d) {
            OWLClassExpression filler = ExpressionManager.this.getNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLObjectSomeValuesFrom(d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLObjectAllValuesFrom d) {
            OWLClassExpression filler = ExpressionManager.this.getNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLObjectAllValuesFrom(d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLObjectHasValue d) {
            return ExpressionManager.this.m_factory.getOWLObjectHasValue(d.getProperty(), d.getFiller());
        }

        public OWLClassExpression visit(OWLObjectHasSelf d) {
            return ExpressionManager.this.m_factory.getOWLObjectHasSelf(d.getProperty());
        }

        public OWLClassExpression visit(OWLObjectMinCardinality d) {
            OWLClassExpression filler = ExpressionManager.this.getNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLObjectMinCardinality(d.getCardinality(), d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLObjectMaxCardinality d) {
            OWLClassExpression filler = ExpressionManager.this.getNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLObjectMaxCardinality(d.getCardinality(), d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLObjectExactCardinality d) {
            OWLClassExpression filler = ExpressionManager.this.getNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLObjectExactCardinality(d.getCardinality(), d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLDataSomeValuesFrom d) {
            OWLDataRange filler = ExpressionManager.this.getNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLDataSomeValuesFrom(d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLDataAllValuesFrom d) {
            OWLDataRange filler = ExpressionManager.this.getNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLDataAllValuesFrom(d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLDataHasValue d) {
            return d;
        }

        public OWLClassExpression visit(OWLDataMinCardinality d) {
            OWLDataRange filler = ExpressionManager.this.getNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLDataMinCardinality(d.getCardinality(), d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLDataMaxCardinality d) {
            OWLDataRange filler = ExpressionManager.this.getNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLDataMaxCardinality(d.getCardinality(), d.getProperty(), filler);
        }

        public OWLClassExpression visit(OWLDataExactCardinality d) {
            OWLDataRange filler = ExpressionManager.this.getNNF(d.getFiller());
            return ExpressionManager.this.m_factory.getOWLDataExactCardinality(d.getCardinality(), d.getProperty(), filler);
        }
    }

}

