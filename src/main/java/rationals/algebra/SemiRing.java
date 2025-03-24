package rationals.algebra;

public interface SemiRing {
    SemiRing plus(SemiRing var1);

    SemiRing mult(SemiRing var1);

    SemiRing one();

    SemiRing zero();
}

