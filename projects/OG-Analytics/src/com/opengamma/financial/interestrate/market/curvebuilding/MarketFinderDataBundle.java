/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market.curvebuilding;

import java.util.Map;

import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.interestrate.InterestRateDerivative;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.math.interpolation.Interpolator1D;
import com.opengamma.util.money.Currency;

/**
 * Data bundle used to construct/calibrate MarketBundle from instruments.
 */
public class MarketFinderDataBundle {

  /**
   * The interest rate instruments used for the curve construction.
   */
  private final InterestRateDerivative[] _instruments;
  /**
   * The market value of each instrument.
   */
  private final double[] _marketValue;
  /**
   * The links between the currencies and the order of the yield curves to be constructed.
   */
  private final Map<Currency, Integer> _discountingReferences;
  /**
   * The links between the indexes (Ibor and OIS) and the order of the yield curves to be constructed.
   * The same order number can appear in several places: in a discounting curve and a Index curve (specially if the discounting is constructed with OIS swaps);
   * in several Index curve (for example for instruments depending on Libor EUR and Euribor using the same curve).
   */
  private final Map<IndexDeposit, Integer> _forwardReferences;
  /**
   * The points on which each interpolated yield curve is constructed.
   */
  private final double[][] _nodePointsYieldCurve;
  /**
   * The interpolators for each yield curve to be constructed.
   */
  private final Interpolator1D[] _interpolatorsYieldCurve;
  /**
   * The number of yield curves to be constructed.
   */
  private final int _nbYieldCurve;
  /**
   * The number of currencies for which the discounting curves are constructed.
   */
  private final int _nbCurrencies;
  /**
   * The number of index for which the forward curves are constructed.
   */
  private final int _nbIndexDeposit;
  /**
   * The number of instruments used to construct the curves.
   */
  private final int _nbInstruments;
  /**
   * The market with the curves already known before the curve construction. The market should not contain the curves for the currencies and index in this construction.
   */
  private final MarketBundle _knownMarket;

  // TODO: Add the already available curves.
  // TODO: Add the PriceIndexCurves.

  /**
   * Data bundle constructor.
   * @param instruments The interest rate instruments used for the curve construction.
   * @param marketValue The market value of each instrument.
   * @param discountingReferences The links between the currencies and the order of the yield curves to be constructed.
   * @param forwardReferences The links between the indexes (Ibor and OIS) and the order of the yield curves to be constructed.
   * @param nodePointsYieldCurve The points on which each interpolated yield curve is constructed.
   * @param interpolatorsYieldCurve The interpolators for each yield curve to be constructed.
   */
  public MarketFinderDataBundle(InterestRateDerivative[] instruments, double[] marketValue, Map<Currency, Integer> discountingReferences, Map<IndexDeposit, Integer> forwardReferences,
      double[][] nodePointsYieldCurve, Interpolator1D[] interpolatorsYieldCurve) {
    // TODO: validate the input (length, references, ...)
    _instruments = instruments;
    _marketValue = marketValue;
    _discountingReferences = discountingReferences;
    _forwardReferences = forwardReferences;
    _nodePointsYieldCurve = nodePointsYieldCurve;
    _interpolatorsYieldCurve = interpolatorsYieldCurve;
    _nbYieldCurve = nodePointsYieldCurve.length;
    _nbCurrencies = discountingReferences.size();
    _nbIndexDeposit = forwardReferences.size();
    _nbInstruments = _instruments.length;
    _knownMarket = new MarketBundle();
  }

  public MarketFinderDataBundle(MarketBundle knownMarket, InterestRateDerivative[] instruments, double[] marketValue, Map<Currency, Integer> discountingReferences,
      Map<IndexDeposit, Integer> forwardReferences, double[][] nodePointsYieldCurve, Interpolator1D[] interpolatorsYieldCurve) {
    // TODO: validate the input (length, references, ...)
    _instruments = instruments;
    _marketValue = marketValue;
    _discountingReferences = discountingReferences;
    _forwardReferences = forwardReferences;
    _nodePointsYieldCurve = nodePointsYieldCurve;
    _interpolatorsYieldCurve = interpolatorsYieldCurve;
    _nbYieldCurve = nodePointsYieldCurve.length;
    _nbCurrencies = discountingReferences.size();
    _nbIndexDeposit = forwardReferences.size();
    _nbInstruments = _instruments.length;
    _knownMarket = knownMarket;
  }

  /**
   * Gets the interest rate instruments used for the curve construction.
   * @return The interest rate instruments.
   */
  public InterestRateDerivative[] getInstruments() {
    return _instruments;
  }

  /**
   * Gets the market value of each instrument.
   * @return The market value.
   */
  public double[] getMarketValue() {
    return _marketValue;
  }

  /**
   * Gets the links between the currencies and the order of the yield curves to be constructed.
   * @return The yield curves links.
   */
  public Map<Currency, Integer> getDiscountingReferences() {
    return _discountingReferences;
  }

  /**
   * Gets the links between the indexes (Ibor and OIS) and the order of the yield curves to be constructed.
   * @return The yield curves links.
   */
  public Map<IndexDeposit, Integer> getForwardReferences() {
    return _forwardReferences;
  }

  /**
   * Gets the points on which each interpolated yield curve is constructed.
   * @return The yield curve points.
   */
  public double[][] getNodePointsYieldCurve() {
    return _nodePointsYieldCurve;
  }

  /**
   * Gets the interpolators for each yield curve to be constructed.
   * @return The interpolators.
   */
  public Interpolator1D[] getInterpolatorsYieldCurve() {
    return _interpolatorsYieldCurve;
  }

  /**
   * Gets the number of yield curves to be constructed.
   * @return The number of yield curves.
   */
  public int getNbYieldCurve() {
    return _nbYieldCurve;
  }

  /**
   * Gets the number of currencies to be added in the market.
   * @return The number currencies.
   */
  public int getNbCurrencies() {
    return _nbCurrencies;
  }

  /**
   * Gets the number of deposit indexes to be added in the market.
   * @return The number indexes.
   */
  public int getNbIndexDeposit() {
    return _nbIndexDeposit;
  }

  /**
   * Gets the number of instruments.
   * @return The number instruments.
   */
  public int getNbInstruments() {
    return _nbInstruments;
  }

  /**
   * Gets the market with the curves already known before the curve construction.
   * @return The known market.
   */
  public MarketBundle getKnownMarket() {
    return _knownMarket;
  }

}
