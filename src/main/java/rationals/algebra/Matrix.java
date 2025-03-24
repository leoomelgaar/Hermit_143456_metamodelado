package rationals.algebra;

import java.util.Arrays;

public final class Matrix
implements SemiRing {
    private final SemiRing[][] matrix;
    private final int line;
    private final int col;

    public Matrix(int ns) {
        this.line = this.col = ns;
        this.matrix = new SemiRing[ns][ns];
    }

    public Matrix(int l, int c) {
        this.line = l;
        this.col = c;
        this.matrix = new SemiRing[l][c];
    }

    public Matrix power(int n, Matrix res) {
        int l = this.line;
        if (this.line != this.col) {
            throw new IllegalStateException("Cannot compute power of a non square matrix");
        }
        SemiRing[][] tmp = new SemiRing[l][l];
        for (int i = 0; i < l; ++i) {
            Arrays.fill(tmp[i], this.matrix[0][0].zero());
        }
        for (int k = 0; k < n; ++k) {
            int i;
            for (i = 0; i < l; ++i) {
                for (int j = 0; j < l; ++j) {
                    for (int m = 0; m < l; ++m) {
                        tmp[i][j] = k == 0 ? tmp[i][j].plus(this.matrix[i][m].mult(this.matrix[m][j])) : tmp[i][j].plus(res.matrix[i][m].mult(this.matrix[m][j]));
                    }
                }
            }
            for (i = 0; i < l; ++i) {
                System.arraycopy(tmp[i], 0, res.matrix[i], 0, l);
            }
        }
        return res;
    }

    public int getLine() {
        return this.line;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < this.line; ++i) {
            sb.append("[ ");
            for (int j = 0; j < this.col; ++j) {
                String s = this.matrix[i][j].toString();
                sb.append(s).append(' ');
            }
            sb.append("]\n");
        }
        return sb.toString();
    }

    @Override
    public SemiRing plus(SemiRing s2) {
        if (s2 == null) {
            throw new IllegalArgumentException("Null argument");
        }
        Matrix o = (Matrix)s2;
        if (this.col != o.col || this.line != o.line) {
            throw new IllegalArgumentException("Incompatible matrices dimensions : cannot add non square matrices");
        }
        int l = this.line;
        int c = this.col;
        Matrix res = Matrix.zero(l, c, this.matrix[0][0]);
        for (int i = 0; i < l; ++i) {
            for (int j = 0; j < c; ++j) {
                res.matrix[i][j] = this.matrix[i][j].plus(o.matrix[i][j]);
            }
        }
        return res;
    }

    @Override
    public SemiRing mult(SemiRing s2) {
        if (s2 == null) {
            throw new IllegalArgumentException("Null argument");
        }
        Matrix o = (Matrix)s2;
        if (this.col != o.line) {
            throw new IllegalArgumentException("Incompatible matrices dimensions");
        }
        int l = this.line;
        int c = o.col;
        int m = this.col;
        Matrix res = Matrix.zero(l, c, this.matrix[0][0]);
        for (int i = 0; i < l; ++i) {
            for (int j = 0; j < c; ++j) {
                for (int k = 0; k < m; ++k) {
                    res.matrix[i][j] = k == 0 ? this.matrix[i][k].mult(o.matrix[k][j]) : res.matrix[i][j].plus(this.matrix[i][k].mult(o.matrix[k][j]));
                }
            }
        }
        return res;
    }

    @Override
    public SemiRing one() {
        if (this.line != this.col) {
            throw new IllegalStateException("Cannot get unit matrix on non-square matrices");
        }
        return Matrix.one(this.line, this.matrix[0][0]);
    }

    @Override
    public SemiRing zero() {
        return Matrix.zero(this.line, this.col, this.matrix[0][0]);
    }

    public int getCol() {
        return this.col;
    }

    public static Matrix zero(int line, int col, SemiRing sr) {
        Matrix m = new Matrix(line, col);
        for (int i = 0; i < line; ++i) {
            for (int j = 0; j < col; ++j) {
                m.matrix[i][j] = sr.zero();
            }
        }
        return m;
    }

    public static Matrix one(int dim, SemiRing sr) {
        Matrix m = new Matrix(dim);
        for (int i = 0; i < dim; ++i) {
            for (int j = 0; j < dim; ++j) {
                m.matrix[i][j] = i == j ? sr.one() : sr.zero();
            }
        }
        return m;
    }
}

