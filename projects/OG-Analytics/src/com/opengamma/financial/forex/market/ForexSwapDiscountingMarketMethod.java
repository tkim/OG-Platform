/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.forex.market;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.forex.derivative.ForexSwap;
import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.method.PricingMarketMethod;
import com.opengamma.util.money.MultipleCurrencyAmount;

/**
 * Pricing method for Forex swap transactions by discounting each payment.
 */
public final class ForexSwapDiscountingMarketMethod implements PricingMarketMethod {

  /**
   * The method unique instance.
   */
  private static final ForexSwapDiscountingMarketMethod INSTANCE = new ForexSwapDiscountingMarketMethod();

  /**
   * Return the unique instance of the class.
   * @return The instance.
   */
  public static ForexSwapDiscountingMarketMethod getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  private ForexSwapDiscountingMarketMethod() {
  }

  /**
   * Forex method by discounting.
   */
  private static final ForexDiscountingMarketMethod METHOD_FX = ForexDiscountingMarketMethod.getInstance();

  /**
   * Compute the present value by discounting the payments in their own currency.
   * @param fx The Forex swap.
   * @param market The market curves.
   * @return The multi-currency present value.
   */
  public MultipleCurrencyAmount presentValue(final ForexSwap fx, final MarketBundle market) {
    final MultipleCurrencyAmount pv = METHOD_FX.presentValue(fx.getNearLeg(), market);
    return pv.plus(METHOD_FX.presentValue(fx.getFarLeg(), market));
  }

  @Override
  public MultipleCurrencyAmount presentValue(final InstrumentDerivative instrument, final MarketBundle market) {
    Validate.isTrue(instrument instanceof ForexSwap, "Instrument should be ForexSwap");
    return presentValue((ForexSwap) instrument, market);
  }

  /**
   * Computes the currency exposure of the instrument.
   * @param instrument The instrument.
   * @param market The market curves.
   * @return The currency exposure.
   */
  public MultipleCurrencyAmount currencyExposure(final InstrumentDerivative instrument, final MarketBundle market) {
    return presentValue(instrument, market);
  }

  //TODO: Add curves sensitivity.

}
