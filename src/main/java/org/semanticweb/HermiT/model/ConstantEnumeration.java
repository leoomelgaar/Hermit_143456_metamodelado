package org.semanticweb.HermiT.model;

import org.semanticweb.HermiT.Prefixes;

public class ConstantEnumeration
extends AtomicDataRange {
    private static final long serialVersionUID = 4663162424764302912L;
    protected final Constant[] m_constants;
    protected static final InterningManager<ConstantEnumeration> s_interningManager = new InterningManager<ConstantEnumeration>(){

        @Override
        protected boolean equal(ConstantEnumeration object1, ConstantEnumeration object2) {
            if (object1.m_constants.length != object2.m_constants.length) {
                return false;
            }
            for (int index = object1.m_constants.length - 1; index >= 0; --index) {
                if (this.contains(object1.m_constants[index], object2.m_constants)) continue;
                return false;
            }
            return true;
        }

        private boolean contains(Constant constant, Constant[] constants2) {
            for (int i = constants2.length - 1; i >= 0; --i) {
                if (!constants2[i].equals(constant)) continue;
                return true;
            }
            return false;
        }

        @Override
        protected int getHashCode(ConstantEnumeration object) {
            int hashCode = 0;
            for (int index = object.m_constants.length - 1; index >= 0; --index) {
                hashCode += object.m_constants[index].hashCode();
            }
            return hashCode;
        }
    };

    protected ConstantEnumeration(Constant[] constants2) {
        this.m_constants = constants2;
    }

    public int getNumberOfConstants() {
        return this.m_constants.length;
    }

    public Constant getConstant(int index) {
        return this.m_constants[index];
    }

    @Override
    public LiteralDataRange getNegation() {
        return AtomicNegationDataRange.create(this);
    }

    @Override
    public boolean isAlwaysTrue() {
        return false;
    }

    @Override
    public boolean isAlwaysFalse() {
        return this.m_constants.length == 0;
    }

    @Override
    public String toString(Prefixes prefixes) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("{ ");
        for (int index = 0; index < this.m_constants.length; ++index) {
            if (index > 0) {
                buffer.append(' ');
            }
            buffer.append(this.m_constants[index].toString(prefixes));
        }
        buffer.append(" }");
        return buffer.toString();
    }

    protected Object readResolve() {
        return s_interningManager.intern(this);
    }

    public static ConstantEnumeration create(Constant[] constants2) {
        return s_interningManager.intern(new ConstantEnumeration(constants2));
    }

}

