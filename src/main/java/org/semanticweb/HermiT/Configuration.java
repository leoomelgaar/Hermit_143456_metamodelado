package org.semanticweb.HermiT;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.monitor.TableauMonitor;
import org.semanticweb.owlapi.reasoner.FreshEntityPolicy;
import org.semanticweb.owlapi.reasoner.IndividualNodeSetPolicy;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;

public class Configuration
implements Serializable,
Cloneable,
OWLReasonerConfiguration {
    private static final long serialVersionUID = 7741510316249774519L;
    public final WarningMonitor warningMonitor = null;
    public ReasonerProgressMonitor reasonerProgressMonitor = null;
    public final TableauMonitorType tableauMonitorType = TableauMonitorType.NONE;
    public DirectBlockingType directBlockingType = DirectBlockingType.OPTIMAL;
    public BlockingStrategyType blockingStrategyType = BlockingStrategyType.OPTIMAL;
    public BlockingSignatureCacheType blockingSignatureCacheType = BlockingSignatureCacheType.CACHED;
    public ExistentialStrategyType existentialStrategyType = ExistentialStrategyType.CREATION_ORDER;
    public boolean ignoreUnsupportedDatatypes = false;
    public TableauMonitor monitor = null;
    public Map<String, Object> parameters = new HashMap<String, Object>();
    public long individualTaskTimeout = -1L;
    public IndividualNodeSetPolicy individualNodeSetPolicy = IndividualNodeSetPolicy.BY_NAME;
    public FreshEntityPolicy freshEntityPolicy = FreshEntityPolicy.ALLOW;
    public boolean useDisjunctionLearning = true;
    public boolean bufferChanges = true;
    public boolean throwInconsistentOntologyException = true;
    public PrepareReasonerInferences prepareReasonerInferences = null;
    public final boolean forceQuasiOrderClassification = false;

    protected void setIndividualReuseStrategyReuseAlways(Set<? extends AtomicConcept> concepts) {
        this.parameters.put("IndividualReuseStrategy.reuseAlways", concepts);
    }

    public void loadIndividualReuseStrategyReuseAlways(File file) throws IOException {
        Set<AtomicConcept> concepts = this.loadConceptsFromFile(file);
        this.setIndividualReuseStrategyReuseAlways(concepts);
    }

    protected void setIndividualReuseStrategyReuseNever(Set<? extends AtomicConcept> concepts) {
        this.parameters.put("IndividualReuseStrategy.reuseNever", concepts);
    }

    public void loadIndividualReuseStrategyReuseNever(File file) throws IOException {
        Set<AtomicConcept> concepts = this.loadConceptsFromFile(file);
        this.setIndividualReuseStrategyReuseNever(concepts);
    }

    /*
     * Exception decompiling
     */
    protected Set<AtomicConcept> loadConceptsFromFile(File file) throws IOException {
        // This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
        // org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [0[TRYBLOCK]], but top level block is 4[TRYBLOCK]
        // org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:432)
        // org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
        // org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:2946)
        // org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:716)
        // org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:186)
        // org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:131)
        // org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:96)
        // org.benf.cfr.reader.entities.Method.analyse(Method.java:396)
        // org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:887)
        // org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:789)
        // org.benf.cfr.reader.Driver.doJar(Driver.java:128)
        // org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:63)
        // com.helospark.importjar.handlers.JarWithoutSourceImportHandler.decompileJar(JarWithoutSourceImportHandler.java:96)
        // com.helospark.importjar.handlers.JarWithoutSourceImportHandler.createProject(JarWithoutSourceImportHandler.java:56)
        // com.helospark.importjar.handlers.JarWithoutSourceImportHandler.execute(JarWithoutSourceImportHandler.java:40)
        // com.helospark.importjar.handlers.JarWithoutSourceImportWizard.lambda$0(JarWithoutSourceImportWizard.java:72)
        // org.eclipse.jface.operation.ModalContext$ModalContextThread.run(ModalContext.java:119)
        throw new IllegalStateException("Decompilation failed");
    }

    public Configuration clone() {
        try {
            Configuration result = (Configuration)super.clone();
            result.parameters = new HashMap<String, Object>(this.parameters);
            return result;
        }
        catch (CloneNotSupportedException cantHappen) {
            return null;
        }
    }

    public long getTimeOut() {
        return this.individualTaskTimeout;
    }

    public IndividualNodeSetPolicy getIndividualNodeSetPolicy() {
        return this.individualNodeSetPolicy;
    }

    public ReasonerProgressMonitor getProgressMonitor() {
        return this.reasonerProgressMonitor;
    }

    public FreshEntityPolicy getFreshEntityPolicy() {
        return this.freshEntityPolicy;
    }

    public static class PrepareReasonerInferences {
        public boolean classClassificationRequired = true;
        public boolean objectPropertyClassificationRequired = true;
        public boolean dataPropertyClassificationRequired = true;
        public boolean objectPropertyDomainsRequired = true;
        public boolean objectPropertyRangesRequired = true;
        public boolean realisationRequired = true;
        public boolean objectPropertyRealisationRequired = true;
        public boolean dataPropertyRealisationRequired = true;
        public boolean sameAs = true;
    }

    public interface WarningMonitor {
        void warning(String var1);
    }

    public enum ExistentialStrategyType {
        CREATION_ORDER,
        INDIVIDUAL_REUSE,
        EL

    }

    public enum BlockingSignatureCacheType {
        CACHED,
        NOT_CACHED

    }

    public enum BlockingStrategyType {
        ANYWHERE,
        ANCESTOR,
        COMPLEX_CORE,
        SIMPLE_CORE,
        OPTIMAL

    }

    public enum DirectBlockingType {
        SINGLE,
        PAIR_WISE,
        OPTIMAL

    }

    public enum TableauMonitorType {
        NONE,
        TIMING,
        TIMING_WITH_PAUSE,
        DEBUGGER_NO_HISTORY,
        DEBUGGER_HISTORY_ON

    }

}

