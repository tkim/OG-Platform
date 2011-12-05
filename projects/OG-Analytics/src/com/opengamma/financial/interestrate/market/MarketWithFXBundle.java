/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market;

import com.opengamma.financial.forex.method.FXMatrix;
import com.opengamma.util.money.Currency;

/**
 * Market bundle with FX rate matrix.
 */
public class MarketWithFXBundle extends MarketBundle {

  /**
   * The forex exchange rates at the valuation date.
   */
  private final FXMatrix _fxRates;

  /**
   * Constructor.
   * @param fxRates The FXMatrix with the FX exchange rates.
   * @param bundle The yield curve bundle.
   */
  public MarketWithFXBundle(final FXMatrix fxRates, final MarketBundle bundle) {
    super(bundle);
    _fxRates = fxRates;
  }

  /**
   * Return the exchange rate between two currencies.
   * @param ccy1 The first currency.
   * @param ccy2 The second currency.
   * @return The exchange rate: 1.0 * ccy1 = x * ccy2.
   */
  public double getFxRate(final Currency ccy1, final Currency ccy2) {
    return _fxRates.getFxRate(ccy1, ccy2);
  }

  /**
   * Gets the FX matrix embedded in the market.
   * @return The FX matrix.
   */
  public FXMatrix getFXMatrix() {
    return _fxRates;
  }

  /**
   * Return the forward Forex rate at a given time for a given currency pair.
   * @param ccy1 The first currency.
   * @param ccy2 The second currency.
   * @param time The forward time.
   * @return The forward exchange rate: 1.0 * ccy1 = x * ccy2.
   */
  public double forwardForexRate(final Currency ccy1, final Currency ccy2, final double time) {
    final double dfDomestic = getDiscountingFactor(ccy2, time);
    final double dfForeign = getDiscountingFactor(ccy1, time);
    return _fxRates.getFxRate(ccy1, ccy2) * dfForeign / dfDomestic;
  }

  /**
   * Build a new market from an existing one. New maps are created to hold the different curves. 
   * The curves of the existing market are used for the new one (the same curve are used, not copied).
   * @return The new market.
   */
  @Override
  public MarketWithFXBundle duplicate() {
    return new MarketWithFXBundle(_fxRates, super.duplicate());
  }

}
