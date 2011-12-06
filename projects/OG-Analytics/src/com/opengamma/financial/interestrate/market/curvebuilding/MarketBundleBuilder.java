/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market.curvebuilding;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.InstrumentDerivativeVisitor;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.math.interpolation.Interpolator1D;
import com.opengamma.math.matrix.DoubleMatrix1D;
import com.opengamma.math.rootfinding.newton.BroydenVectorRootFinder;
import com.opengamma.math.rootfinding.newton.NewtonVectorRootFinder;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.money.MultipleCurrencyAmount;

/**
 * Class with several builder of MarketBundle from instruments.
 */
public class MarketBundleBuilder {

  /**
   * Absolute and relative tolerance for the root finding process.
   */
  private static final double EPS = 1e-8;
  /**
   * The maximum number of steps for the root finding process.
   */
  private static final int STEPS = 100;

  /**
   * Build a unique discounting curve.
   * @param instruments The instruments use to build the curve.
   * @param startRate The start rate of the curve root finding process.
   * @param ccy The curve currency.
   * @param indexes The IndexDeposit for which the discounting curve is used to estimate the forward (usually an OIS index).
   * @param interpolator The interpolator used for the curve.
   * @param nodeTimeCalculator The calculator to compute the curve node from the instruments.
   * @param presentValueCalculator The calculator used to compute the instruments present value.
   * @return The build market bundle.
   */
  public static MarketBundle discounting(final InstrumentDerivative[] instruments, final double[] startRate, Currency ccy, IndexDeposit[] indexes, Interpolator1D interpolator,
      InstrumentDerivativeVisitor<Object, Double> nodeTimeCalculator, InstrumentDerivativeVisitor<MarketBundle, MultipleCurrencyAmount> presentValueCalculator) {
    int nbInstruments = instruments.length;
    CurrencyAmount[] marketValue = new CurrencyAmount[nbInstruments];
    for (int loopins = 0; loopins < nbInstruments; loopins++) {
      marketValue[loopins] = CurrencyAmount.of(ccy, 0);
    }
    double[][] nodePointsYieldCurve = new double[1][nbInstruments];
    for (int loopins = 0; loopins < nbInstruments; loopins++) {
      nodePointsYieldCurve[0][loopins] = nodeTimeCalculator.visit(instruments[loopins]);
    }
    Interpolator1D[] interpolatorsYieldCurve = new Interpolator1D[] {interpolator};
    String name = ccy.toString() + " discounting";
    Map<Currency, Integer> discountingReferences = new HashMap<Currency, Integer>();
    discountingReferences.put(ccy, 0);
    Map<IndexDeposit, Integer> forwardReferences = new HashMap<IndexDeposit, Integer>();
    for (int loopindex = 0; loopindex < indexes.length; loopindex++) {
      forwardReferences.put(indexes[loopindex], 0);
    }
    MarketFinderDataBundle data = new MarketFinderDataBundle(instruments, marketValue, discountingReferences, forwardReferences, nodePointsYieldCurve, interpolatorsYieldCurve, new String[] {name});
    MarketBundleFinderFunction func = new MarketBundleFinderFunction(presentValueCalculator, data);
    final NewtonVectorRootFinder rootFinder = new BroydenVectorRootFinder(EPS, EPS, STEPS);
    final DoubleMatrix1D yieldCurveNodes = rootFinder.getRoot(func, new DoubleMatrix1D(startRate));
    MarketBundle market = MarketBundleBuildingFunction.build(data, yieldCurveNodes);
    return market;
  }

  /**
   * Build a unique discounting curve. The start rate for the root finder process are set at 2.5%.
   * @param instruments The instruments use to build the curve.
   * @param ccy The curve currency.
   * @param indexes The IndexDeposit for which the discounting curve is used to estimate the forward (usually an OIS index).
   * @param interpolator The interpolator used for the curve.
   * @param nodeTimeCalculator The calculator to compute the curve node from the instruments.
   * @param presentValueCalculator The calculator used to compute the instruments present value.
   * @return The build market bundle.
   */
  public static MarketBundle discounting(final InstrumentDerivative[] instruments, Currency ccy, IndexDeposit[] indexes, Interpolator1D interpolator,
      InstrumentDerivativeVisitor<Object, Double> nodeTimeCalculator, InstrumentDerivativeVisitor<MarketBundle, MultipleCurrencyAmount> presentValueCalculator) {
    int nbInstruments = instruments.length;
    double[] startRate = new double[nbInstruments];
    for (int loopins = 0; loopins < nbInstruments; loopins++) {
      startRate[loopins] = 0.025;
    }
    return discounting(instruments, startRate, ccy, indexes, interpolator, nodeTimeCalculator, presentValueCalculator);
  }

}
