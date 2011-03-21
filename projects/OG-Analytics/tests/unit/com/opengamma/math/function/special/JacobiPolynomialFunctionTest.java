/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.math.function.special;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.opengamma.math.function.DoubleFunction1D;

/**
 * 
 */
public class JacobiPolynomialFunctionTest {
  private static final double ALPHA = 0.12;
  private static final double BETA = 0.34;
  private static final DoubleFunction1D P0 = new DoubleFunction1D() {

    @Override
    public Double evaluate(final Double x) {
      return 1.;
    }

  };
  private static final DoubleFunction1D P1 = new DoubleFunction1D() {

    @Override
    public Double evaluate(final Double x) {
      return 0.5 * (2 * (ALPHA + 1) + (ALPHA + BETA + 2) * (x - 1));
    }

  };
  private static final DoubleFunction1D P2 = new DoubleFunction1D() {

    @Override
    public Double evaluate(final Double x) {
      return 0.125 * (4 * (ALPHA + 1) * (ALPHA + 2) + 4 * (ALPHA + BETA + 3) * (ALPHA + 2) * (x - 1) + (ALPHA + BETA + 3) * (ALPHA + BETA + 4) * (x - 1) * (x - 1));
    }

  };
  private static final DoubleFunction1D[] P = new DoubleFunction1D[] {P0, P1, P2};
  private static final JacobiPolynomialFunction JACOBI = new JacobiPolynomialFunction();
  private static final double EPS = 1e-9;

  @Test(expected = UnsupportedOperationException.class)
  public void testNoAlphaBeta() {
    JACOBI.getPolynomials(3);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeN() {
    JACOBI.getPolynomials(-3, ALPHA, BETA);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetPolynomials() {
    JACOBI.getPolynomialsAndFirstDerivative(3);
  }

  @Test
  public void test() {
    DoubleFunction1D[] p = JACOBI.getPolynomials(0, ALPHA, BETA);
    assertEquals(p.length, 1);
    final double x = 1.23;
    assertEquals(p[0].evaluate(x), 1, EPS);
    for (int i = 0; i <= 2; i++) {
      p = JACOBI.getPolynomials(i, ALPHA, BETA);
      for (int j = 0; j <= i; j++) {
        assertEquals(P[j].evaluate(x), p[j].evaluate(x), EPS);
      }
    }
  }
}
