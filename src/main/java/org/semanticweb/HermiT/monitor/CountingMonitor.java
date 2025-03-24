package org.semanticweb.HermiT.monitor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.tableau.BranchingPoint;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.ReasoningTaskDescription;

public class CountingMonitor
extends TableauMonitorAdapter {
    private static final long serialVersionUID = -8144444618897251350L;
    protected long m_problemStartTime;
    protected long m_validationStartTime;
    protected long m_datatypeCheckingStartTime;
    protected int m_testNo = 0;
    protected long m_time;
    protected int m_numberOfBacktrackings;
    protected int m_numberOfNodes;
    protected int m_numberOfBlockedNodes;
    protected ReasoningTaskDescription m_reasoningTaskDescription;
    protected boolean m_testResult;
    protected int m_initialModelSize;
    protected int m_initiallyBlocked;
    protected int m_initiallyInvalid;
    protected int m_noValidations;
    protected long m_validationTime;
    protected int m_numberDatatypesChecked;
    protected int m_datatypeCheckingTime;
    protected final Map<String, List<TestRecord>> m_testRecords = new HashMap<String, List<TestRecord>>();
    protected long m_overallTime = 0L;
    protected int m_overallNumberOfBacktrackings = 0;
    protected int m_overallNumberOfNodes = 0;
    protected int m_overallNumberOfBlockedNodes = 0;
    protected int m_overallNumberOfTests = 0;
    protected int m_overallNumberOfClashes = 0;
    protected int m_possibleInstancesTested = 0;
    protected int m_possibleInstancesInstances = 0;
    protected int m_overallInitialModelSize = 0;
    protected int m_overallInitiallyBlocked = 0;
    protected int m_overallInitiallyInvalid = 0;
    protected int m_overallNoValidations = 0;
    protected long m_overallValidationTime = 0L;
    protected int m_overallDatatypeCheckingTime = 0;
    protected int m_overallNumberDatatypesChecked;

    @Override
    public void isSatisfiableStarted(ReasoningTaskDescription reasoningTaskDescription) {
        super.isSatisfiableStarted(reasoningTaskDescription);
        ++this.m_testNo;
        this.m_reasoningTaskDescription = reasoningTaskDescription;
        ++this.m_overallNumberOfTests;
        this.m_problemStartTime = System.currentTimeMillis();
        this.m_numberOfBacktrackings = 0;
        this.m_numberOfNodes = 0;
        this.m_numberOfBlockedNodes = 0;
        this.m_initialModelSize = 0;
        this.m_initiallyBlocked = 0;
        this.m_initiallyInvalid = 0;
        this.m_noValidations = 0;
        this.m_validationTime = 0L;
        this.m_datatypeCheckingTime = 0;
        this.m_numberDatatypesChecked = 0;
    }

    @Override
    public void isSatisfiableFinished(ReasoningTaskDescription reasoningTaskDescription, boolean result) {
        super.isSatisfiableFinished(reasoningTaskDescription, result);
        if (reasoningTaskDescription.flipSatisfiabilityResult()) {
            result = !result;
        }
        this.m_testResult = result;
        this.m_time = System.currentTimeMillis() - this.m_problemStartTime;
        String messagePattern = this.m_reasoningTaskDescription.getMessagePattern();
        List<TestRecord> records = this.m_testRecords.get(messagePattern);
        if (records == null) {
            records = new ArrayList<TestRecord>();
            this.m_testRecords.put(messagePattern, records);
        }
        records.add(new TestRecord(this.m_time, this.m_reasoningTaskDescription.getTaskDescription(Prefixes.STANDARD_PREFIXES), this.m_testResult));
        this.m_overallTime += this.m_time;
        this.m_overallNumberOfBacktrackings += this.m_numberOfBacktrackings;
        this.m_numberOfNodes = this.m_tableau.getNumberOfNodesInTableau() - this.m_tableau.getNumberOfMergedOrPrunedNodes();
        for (Node node = this.m_tableau.getFirstTableauNode(); node != null; node = node.getNextTableauNode()) {
            if (!node.isActive() || !node.isBlocked() || !node.hasUnprocessedExistentials()) continue;
            ++this.m_numberOfBlockedNodes;
        }
        this.m_overallNumberOfNodes += this.m_numberOfNodes;
        this.m_overallNumberOfBlockedNodes += this.m_numberOfBlockedNodes;
        this.m_overallInitialModelSize += this.m_initialModelSize;
        this.m_overallInitiallyBlocked += this.m_initiallyBlocked;
        this.m_overallInitiallyInvalid += this.m_initiallyInvalid;
        this.m_overallNoValidations += this.m_noValidations;
        this.m_overallValidationTime += this.m_validationTime;
        this.m_overallDatatypeCheckingTime += this.m_datatypeCheckingTime;
        this.m_overallNumberDatatypesChecked += this.m_numberDatatypesChecked;
    }

    @Override
    public void backtrackToFinished(BranchingPoint newCurrentBrancingPoint) {
        ++this.m_numberOfBacktrackings;
    }

    @Override
    public void possibleInstanceIsInstance() {
        ++this.m_possibleInstancesTested;
        ++this.m_possibleInstancesInstances;
    }

    @Override
    public void possibleInstanceIsNotInstance() {
        ++this.m_possibleInstancesTested;
    }

    @Override
    public void blockingValidationStarted() {
        ++this.m_noValidations;
        if (this.m_noValidations == 1) {
            for (Node node = this.m_tableau.getFirstTableauNode(); node != null; node = node.getNextTableauNode()) {
                if (!node.isActive()) continue;
                ++this.m_initialModelSize;
                if (!node.isBlocked() || !node.hasUnprocessedExistentials()) continue;
                ++this.m_initiallyBlocked;
            }
        }
        this.m_validationStartTime = System.currentTimeMillis();
    }

    @Override
    public void blockingValidationFinished(int noInvalidlyBlocked) {
        this.m_validationTime += System.currentTimeMillis() - this.m_validationStartTime;
        if (this.m_noValidations == 1) {
            this.m_initiallyInvalid = noInvalidlyBlocked;
        }
    }

    @Override
    public void datatypeCheckingStarted() {
        ++this.m_numberDatatypesChecked;
        this.m_datatypeCheckingStartTime = System.currentTimeMillis();
    }

    @Override
    public void datatypeCheckingFinished(boolean result) {
        this.m_datatypeCheckingTime = (int)((long)this.m_datatypeCheckingTime + (System.currentTimeMillis() - this.m_datatypeCheckingStartTime));
    }

    public Set<String> getUsedMessagePatterns() {
        return this.m_testRecords.keySet();
    }

    public long getTime() {
        return this.m_time;
    }

    public int getNumberOfBacktrackings() {
        return this.m_numberOfBacktrackings;
    }

    public int getNumberOfNodes() {
        return this.m_numberOfNodes;
    }

    public int getNumberOfBlockedNodes() {
        return this.m_numberOfBlockedNodes;
    }

    public String getTestDescription() {
        return this.m_reasoningTaskDescription.getTaskDescription(Prefixes.STANDARD_PREFIXES);
    }

    public boolean getTestResult() {
        return this.m_testResult;
    }

    public int getInitialModelSize() {
        return this.m_initialModelSize;
    }

    public int getInitiallyBlocked() {
        return this.m_initiallyBlocked;
    }

    public int getInitiallyInvalid() {
        return this.m_initiallyInvalid;
    }

    public int getNoValidations() {
        return this.m_noValidations;
    }

    public long getValidationTime() {
        return this.m_validationTime;
    }

    public int getNumberDatatypesChecked() {
        return this.m_numberDatatypesChecked;
    }

    public long getDatatypeCheckingTime() {
        return this.m_datatypeCheckingTime;
    }

    public long getOverallTime() {
        return this.m_overallTime;
    }

    public int getOverallNumberOfBacktrackings() {
        return this.m_overallNumberOfBacktrackings;
    }

    public int getOverallNumberOfNodes() {
        return this.m_overallNumberOfNodes;
    }

    public int getOverallNumberOfBlockedNodes() {
        return this.m_overallNumberOfBlockedNodes;
    }

    public int getOverallNumberOfTests() {
        return this.m_overallNumberOfTests;
    }

    public int getOverallNumberOfClashes() {
        return this.m_overallNumberOfClashes;
    }

    public int getNumberOfPossibleInstancesTested() {
        return this.m_possibleInstancesTested;
    }

    public int getNumberOfPossibleInstancesInstances() {
        return this.m_possibleInstancesInstances;
    }

    public int getOverallInitialModelSize() {
        return this.m_overallInitialModelSize;
    }

    public int getOverallInitiallyBlocked() {
        return this.m_overallInitiallyBlocked;
    }

    public int getOverallInitiallyInvalid() {
        return this.m_overallInitiallyInvalid;
    }

    public int getOverallNoValidations() {
        return this.m_overallNoValidations;
    }

    public long getOverallValidationTime() {
        return this.m_overallValidationTime;
    }

    public int getOverallNumberDatatypesChecked() {
        return this.m_overallNumberDatatypesChecked;
    }

    public long getOverallDatatypeCheckingTime() {
        return this.m_overallDatatypeCheckingTime;
    }

    public long getAverageTime() {
        if (this.m_testNo == 0) {
            return this.m_testNo;
        }
        return this.m_overallTime / (long)this.m_testNo;
    }

    public double getAverageNumberOfBacktrackings() {
        if (this.m_testNo == 0) {
            return this.m_testNo;
        }
        return this.getRounded(this.m_overallNumberOfBacktrackings, this.m_testNo);
    }

    protected double getRounded(long nominator, long denominator) {
        return this.getRounded(nominator, denominator, 2);
    }

    protected double getRounded(long nominator, long denominator, int noDecimalPlaces) {
        double number = (double)nominator / (double)denominator;
        int tmp = (int)(number * Math.pow(10.0, noDecimalPlaces));
        return (double)tmp / Math.pow(10.0, noDecimalPlaces);
    }

    public double getAverageNumberOfNodes() {
        if (this.m_testNo == 0) {
            return this.m_testNo;
        }
        return this.getRounded(this.m_overallNumberOfNodes, this.m_testNo);
    }

    public double getAverageNumberOfBlockedNodes() {
        if (this.m_testNo == 0) {
            return this.m_testNo;
        }
        return this.getRounded(this.m_overallNumberOfBlockedNodes, this.m_testNo);
    }

    public double getAverageNumberOfClashes() {
        if (this.m_testNo == 0) {
            return this.m_testNo;
        }
        return this.getRounded(this.m_overallNumberOfClashes, this.m_testNo);
    }

    public double getPossiblesToInstances() {
        if (this.m_possibleInstancesTested == 0) {
            return 0.0;
        }
        return this.getRounded(this.m_possibleInstancesInstances, this.m_possibleInstancesTested);
    }

    public double getAverageInitialModelSize() {
        if (this.m_testNo == 0) {
            return this.m_testNo;
        }
        return this.getRounded(this.m_overallInitialModelSize, this.m_testNo);
    }

    public double getAverageInitiallyBlocked() {
        if (this.m_testNo == 0) {
            return this.m_testNo;
        }
        return this.getRounded(this.m_overallInitiallyBlocked, this.m_testNo);
    }

    public double getAverageInitiallyInvalid() {
        if (this.m_testNo == 0) {
            return this.m_testNo;
        }
        return this.getRounded(this.m_overallInitiallyInvalid, this.m_testNo);
    }

    public double getAverageNoValidations() {
        if (this.m_testNo == 0) {
            return this.m_testNo;
        }
        return this.getRounded(this.m_overallNoValidations, this.m_testNo);
    }

    public long getAverageValidationTime() {
        if (this.m_testNo == 0) {
            return this.m_testNo;
        }
        return this.m_overallValidationTime / (long)this.m_testNo;
    }

    public long getAverageNumberDatatypesChecked() {
        if (this.m_testNo == 0) {
            return this.m_testNo;
        }
        return this.m_overallNumberDatatypesChecked / this.m_testNo;
    }

    public long getAverageDatatypeCheckingTime() {
        if (this.m_testNo == 0) {
            return this.m_testNo;
        }
        return this.m_overallDatatypeCheckingTime / this.m_testNo;
    }

    public static String millisToHoursMinutesSecondsString(long millis) {
        long hours;
        long mins;
        long time = millis / 1000L;
        long ms = time % 1000L;
        String timeStr = String.format(String.format("%%0%dd", 3), ms) + "ms";
        String format = String.format("%%0%dd", 2);
        long secs = time % 60L;
        if (secs > 0L) {
            timeStr = String.format(format, secs) + "s" + timeStr;
        }
        if ((mins = time % 3600L / 60L) > 0L) {
            timeStr = String.format(format, mins) + "m" + timeStr;
        }
        if ((hours = time / 3600L) > 0L) {
            timeStr = String.format(format, hours) + "h" + timeStr;
        }
        return timeStr;
    }

    public static class TestRecord
    implements Comparable<TestRecord>,
    Serializable {
        private static final long serialVersionUID = -3815493500625020183L;
        protected final long m_testTime;
        protected final String m_testDescription;
        protected final boolean m_testResult;

        public TestRecord(long testTime, String testDescription, boolean result) {
            this.m_testTime = testTime;
            this.m_testDescription = testDescription;
            this.m_testResult = result;
        }

        @Override
        public int compareTo(TestRecord that) {
            if (this == that) {
                return 0;
            }
            int result = Long.valueOf(that.m_testTime).compareTo(this.m_testTime);
            if (result != 0) {
                return result;
            }
            return this.m_testDescription.compareToIgnoreCase(that.m_testDescription);
        }

        public long getTestTime() {
            return this.m_testTime;
        }

        public String getTestDescription() {
            return this.m_testDescription;
        }

        public boolean getTestResult() {
            return this.m_testResult;
        }

        public String toString() {
            return this.m_testTime + " ms" + (this.m_testTime > 1000L ? " (" + CountingMonitor.millisToHoursMinutesSecondsString(this.m_testTime) + ")" : "") + " for " + this.m_testDescription + " (result: " + this.m_testResult + ")";
        }
    }

}

