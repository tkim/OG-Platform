/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market.curvebuilding;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.instrument.index.IndexPrice;
import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.math.interpolation.Interpolator1D;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.CurrencyAmount;

/**
 * Data bundle used to construct/calibrate MarketBundle from instruments.
 */
public class MarketFinderDataBundle {

  /**
   * The interest rate instruments used for the curve construction.
   */
  private final InstrumentDerivative[] _instruments;
  //REVIEW: Is the market value required or can we assume it is always 0?
  /**
   * The market value of each instrument.
   */
  private final CurrencyAmount[] _marketValue;
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
   * The links between the price indexes and the order of the price curves to be constructed.
   */
  private final Map<IndexPrice, Integer> _priceIndexReferences;
  /**
   * The points on which each interpolated yield curve is constructed.
   */
  private final double[][] _nodePointsYieldCurve;
  /**
   * The interpolators for each yield curve to be constructed.
   */
  private final Interpolator1D[] _interpolatorsYieldCurve;
  /**
   * The names associated to the different yield curves.
   */
  private final String[] _yieldCurveName;
  /**
   * The points on which each interpolated price curve is constructed.
   */
  private final double[][] _nodePointsPriceCurve;
  /**
   * The interpolators for each price curve to be constructed.
   */
  private final Interpolator1D[] _interpolatorsPriceCurve;
  /**
   * The values already known on the price curve (usually the already fixed price indexes).
   */
  private final double[][] _knownPointsPriceCurve;
  /**
   * The names associated to the different price curves.
   */
  private final String[] _priceCurveName;
  /**
   * The number of yield curves to be constructed.
   */
  private final int _nbYieldCurve;
  /**
   * The number of price curves to be constructed.
   */
  private final int _nbPriceCurve;
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

  /**
   * Data bundle constructor with discounting and forward curves. There is no price index curve.
   * @param instruments The interest rate instruments used for the curve construction.
   * @param marketValue The market value of each instrument.
   * @param discountingReferences The links between the currencies and the order of the yield curves to be constructed.
   * @param forwardReferences The links between the indexes (Ibor and OIS) and the order of the yield curves to be constructed.
   * @param nodePointsYieldCurve The points on which each interpolated yield curve is constructed.
   * @param interpolatorsYieldCurve The interpolators for each yield curve to be constructed.
   * @param yieldCurveName The curve names (in the order of the yield curve to be constructed).
   */
  public MarketFinderDataBundle(InstrumentDerivative[] instruments, CurrencyAmount[] marketValue, Map<Currency, Integer> discountingReferences, Map<IndexDeposit, Integer> forwardReferences,
      double[][] nodePointsYieldCurve, Interpolator1D[] interpolatorsYieldCurve, String[] yieldCurveName) {
    Validate.notNull(instruments, "Instruments");
    Validate.notNull(marketValue, "Market value");
    Validate.isTrue(instruments.length == marketValue.length, "Instruments and marketValue lengths should be equal");
    // TODO: validate the input (length, references, ...)
    _instruments = instruments;
    _marketValue = marketValue;
    _discountingReferences = discountingReferences;
    _forwardReferences = forwardReferences;
    _nodePointsYieldCurve = nodePointsYieldCurve;
    _interpolatorsYieldCurve = interpolatorsYieldCurve;
    _nodePointsPriceCurve = new double[0][0];
    _interpolatorsPriceCurve = new Interpolator1D[0];
    _knownPointsPriceCurve = new double[0][0];
    _nbYieldCurve = nodePointsYieldCurve.length;
    _yieldCurveName = yieldCurveName;
    _nbPriceCurve = 0;
    _nbCurrencies = discountingReferences.size();
    _nbIndexDeposit = forwardReferences.size();
    _nbInstruments = _instruments.length;
    _knownMarket = new MarketBundle();
    _priceIndexReferences = new HashMap<IndexPrice, Integer>();
    _priceCurveName = new String[0];
  }

  /**
   * Data bundle constructor with discounting and forward curves. There is no price index curve.
   * @param knownMarket The market with the curves (and other data, like FX) already known.
   * @param instruments The interest rate instruments used for the curve construction.
   * @param marketValue The market value of each instrument.
   * @param discountingReferences The links between the currencies and the order of the yield curves to be constructed.
   * @param forwardReferences The links between the indexes (Ibor and OIS) and the order of the yield curves to be constructed.
   * @param nodePointsYieldCurve The points on which each interpolated yield curve is constructed.
   * @param interpolatorsYieldCurve The interpolators for each yield curve to be constructed.
   * @param yieldCurveName The curve names (in the order of the yield curve to be constructed).
   */
  public MarketFinderDataBundle(MarketBundle knownMarket, InstrumentDerivative[] instruments, CurrencyAmount[] marketValue, Map<Currency, Integer> discountingReferences,
      Map<IndexDeposit, Integer> forwardReferences, double[][] nodePointsYieldCurve, Interpolator1D[] interpolatorsYieldCurve, String[] yieldCurveName) {
    Validate.notNull(instruments, "Instruments");
    Validate.notNull(marketValue, "Market value");
    Validate.isTrue(instruments.length == marketValue.length, "Instruments and marketValue lengths should be equal");
    // TODO: validate the input (length, references, ...)
    _instruments = instruments;
    _marketValue = marketValue;
    _discountingReferences = discountingReferences;
    _forwardReferences = forwardReferences;
    _nodePointsYieldCurve = nodePointsYieldCurve;
    _interpolatorsYieldCurve = interpolatorsYieldCurve;
    _nodePointsPriceCurve = new double[0][0];
    _interpolatorsPriceCurve = new Interpolator1D[0];
    _knownPointsPriceCurve = new double[0][0];
    _nbYieldCurve = nodePointsYieldCurve.length;
    _yieldCurveName = yieldCurveName;
    _nbPriceCurve = 0;
    _nbCurrencies = discountingReferences.size();
    _nbIndexDeposit = forwardReferences.size();
    _nbInstruments = _instruments.length;
    _knownMarket = knownMarket;
    _priceIndexReferences = new HashMap<IndexPrice, Integer>();
    _priceCurveName = new String[0];
  }

  /**
   * Data bundle constructor with discounting and forward curves. There is no price index curve.
   * @param knownMarket The market with the curves (and other data, like FX) already known.
   * @param instruments The interest rate instruments used for the curve construction.
   * @param marketValue The market value of each instrument.
   * @param discountingReferences The links between the currencies and the order of the yield curves to be constructed.
   * @param forwardReferences The links between the indexes (Ibor and OIS) and the order of the yield curves to be constructed.
   * @param priceIndexReferences The links between the price indexes and the order of the price curves to be constructed.
   * @param nodePointsYieldCurve The points on which each interpolated yield curve is constructed.
   * @param interpolatorsYieldCurve The interpolators for each yield curve to be constructed.
   * @param nodePointsPriceCurve The points on which each interpolated price curve is constructed.
   * @param interpolatorsPriceCurve The interpolators for each price curve to be constructed.
   * @param knownPointsPriceCurve The values already known on the price curve (usually the already fixed price indexes).
   */
  public MarketFinderDataBundle(final MarketBundle knownMarket, final InstrumentDerivative[] instruments, final CurrencyAmount[] marketValue, final Map<Currency, Integer> discountingReferences,
      final Map<IndexDeposit, Integer> forwardReferences, final Map<IndexPrice, Integer> priceIndexReferences, final double[][] nodePointsYieldCurve, final Interpolator1D[] interpolatorsYieldCurve,
      final double[][] nodePointsPriceCurve, final Interpolator1D[] interpolatorsPriceCurve, final double[][] knownPointsPriceCurve) {
    Validate.notNull(instruments, "Instruments");
    Validate.notNull(marketValue, "Market value");
    Validate.isTrue(instruments.length == marketValue.length, "Instruments and marketValue lengths should be equal");
    // TODO: validate the input (length, references, ...)
    _instruments = instruments;
    _marketValue = marketValue;
    _discountingReferences = discountingReferences;
    _forwardReferences = forwardReferences;
    _nodePointsYieldCurve = nodePointsYieldCurve;
    _interpolatorsYieldCurve = interpolatorsYieldCurve;
    _nodePointsPriceCurve = nodePointsPriceCurve;
    _interpolatorsPriceCurve = interpolatorsPriceCurve;
    _knownPointsPriceCurve = knownPointsPriceCurve;
    _nbYieldCurve = nodePointsYieldCurve.length;
    _yieldCurveName = new String[_nbYieldCurve];
    for (int loopcurve = 0; loopcurve < _nbYieldCurve; loopcurve++) {
      _yieldCurveName[loopcurve] = "YieldCurve" + loopcurve;
    }
    _nbPriceCurve = nodePointsPriceCurve.length;
    _priceCurveName = new String[_nbPriceCurve];
    for (int loopcurve = 0; loopcurve < _nbPriceCurve; loopcurve++) {
      _priceCurveName[loopcurve] = "PriceCurve" + loopcurve;
    }
    _nbCurrencies = discountingReferences.size();
    _nbIndexDeposit = forwardReferences.size();
    _nbInstruments = _instruments.length;
    _knownMarket = knownMarket;
    _priceIndexReferences = priceIndexReferences;
  }

  /**
   * Data bundle constructor with discounting and forward curves. There is no price index curve.
   * @param knownMarket The market with the curves (and other data, like FX) already known.
   * @param instruments The interest rate instruments used for the curve construction.
   * @param marketValue The market value of each instrument.
   * @param priceIndexReferences The links between the price indexes and the order of the price curves to be constructed.
   * @param nodePointsPriceCurve The points on which each interpolated price curve is constructed.
   * @param interpolatorsPriceCurve The interpolators for each price curve to be constructed.
   * @param knownPointsPriceCurve The values already known on the price curve (usually the already fixed price indexes).
   * @param priceCurveName The names associated to the different price curves.
   */
  public MarketFinderDataBundle(final MarketBundle knownMarket, final InstrumentDerivative[] instruments, final CurrencyAmount[] marketValue, final Map<IndexPrice, Integer> priceIndexReferences,
      final double[][] nodePointsPriceCurve, final Interpolator1D[] interpolatorsPriceCurve, final double[][] knownPointsPriceCurve, final String[] priceCurveName) {
    Validate.notNull(instruments, "Instruments");
    Validate.notNull(marketValue, "Market value");
    Validate.isTrue(instruments.length == marketValue.length, "Instruments and marketValue lengths should be equal");
    // TODO: validate the input (length, references, ...)
    _instruments = instruments;
    _marketValue = marketValue;
    _discountingReferences = new HashMap<Currency, Integer>();
    _forwardReferences = new HashMap<IndexDeposit, Integer>();
    _nodePointsYieldCurve = new double[0][0];
    _interpolatorsYieldCurve = new Interpolator1D[0];
    _nodePointsPriceCurve = nodePointsPriceCurve;
    _interpolatorsPriceCurve = interpolatorsPriceCurve;
    _knownPointsPriceCurve = knownPointsPriceCurve;
    _nbYieldCurve = 0;
    _yieldCurveName = new String[0];
    _nbPriceCurve = nodePointsPriceCurve.length;
    _priceCurveName = priceCurveName;
    _nbCurrencies = 0;
    _nbIndexDeposit = 0;
    _nbInstruments = _instruments.length;
    _knownMarket = knownMarket;
    _priceIndexReferences = priceIndexReferences;
  }

  /**
   * Gets the interest rate instruments used for the curve construction.
   * @return The interest rate instruments.
   */
  public InstrumentDerivative[] getInstruments() {
    return _instruments;
  }

  /**
   * Gets the market value of each instrument.
   * @return The market value.
   */
  public CurrencyAmount[] getMarketValue() {
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
   * Gets the links between the price indexes and the order of the price curves to be constructed.
   * @return The price curves links.
   */
  public Map<IndexPrice, Integer> getPriceIndexReferences() {
    return _priceIndexReferences;
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
   * Gets the names of the yield curves.
   * @return The names.
   */
  public String[] getYieldCurveName() {
    return _yieldCurveName;
  }

  /**
   * Gets the points on which each interpolated price curve is constructed.
   * @return The yield curve points.
   */
  public double[][] getNodePointsPriceCurve() {
    return _nodePointsPriceCurve;
  }

  /**
   * Gets the interpolators for each price curve to be constructed.
   * @return The interpolators.
   */
  public Interpolator1D[] getInterpolatorsPriceCurve() {
    return _interpolatorsPriceCurve;
  }

  /**
   * Gets the names of the price curves.
   * @return The names.
   */
  public String[] getPriceCurveName() {
    return _priceCurveName;
  }

  /**
   * Gets the values already known on the price curve (usually the already fixed price indexes).
   * @return The known values.
   */
  public double[][] getKnownPointsPriceCurve() {
    return _knownPointsPriceCurve;
  }

  /**
   * Gets the number of yield curves to be constructed.
   * @return The number of yield curves.
   */
  public int getNbYieldCurve() {
    return _nbYieldCurve;
  }

  /**
   * Gets the number of price curves to be constructed.
   * @return The number of price curves.
   */
  public int getNbPriceCurve() {
    return _nbPriceCurve;
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
