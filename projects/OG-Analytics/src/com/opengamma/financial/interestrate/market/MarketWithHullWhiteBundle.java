/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market;

import com.opengamma.financial.model.interestrate.definition.HullWhiteOneFactorPiecewiseConstantParameters;
import com.opengamma.util.money.Currency;

/**
 * Market bundle with with Hull-White one-factor parameters.
 */
public class MarketWithHullWhiteBundle extends MarketBundle {

  /**
   * The currency for which the Hull-White parameters are valid.
   */
  private final Currency _currencyHW;
  /**
   * The Hull-White one-factor model parameters.
   */
  private final HullWhiteOneFactorPiecewiseConstantParameters _parameters;

  /**
   * Constructor.
   * @param currency The currency for which the Hull-White parameters are valid.
   * @param parameters The Hull-White one-factor model parameters.
   * @param bundle The yield curve bundle.
   */
  public MarketWithHullWhiteBundle(final Currency currency, final HullWhiteOneFactorPiecewiseConstantParameters parameters, final MarketBundle bundle) {
    super(bundle);
    _currencyHW = currency;
    _parameters = parameters;
  }

  /**
   * Gets the currency for which the Hull-White parameters are valid.
   * @return The currency.
   */
  public Currency getCurrencyHW() {
    return _currencyHW;
  }

  /**
   * Gets the Hull-White one-factor model parameters.
   * @return The parameters.
   */
  public HullWhiteOneFactorPiecewiseConstantParameters getParameters() {
    return _parameters;
  }

  /**
   * Build a new market from an existing one. New maps are created to hold the different curves. 
   * The curves of the existing market are used for the new one (the same curve are used, not copied).
   * @return The new market.
   */
  @Override
  public MarketWithHullWhiteBundle duplicate() {
    return new MarketWithHullWhiteBundle(_currencyHW, _parameters, super.duplicate());
  }

}
