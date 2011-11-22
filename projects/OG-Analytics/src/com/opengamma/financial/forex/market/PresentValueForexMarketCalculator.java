/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.forex.market;

import com.opengamma.financial.forex.derivative.Forex;
import com.opengamma.financial.forex.derivative.ForexSwap;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.PresentValueMarketCalculator;
import com.opengamma.util.money.MultipleCurrencyAmount;

/**
 * Calculates the present value of instruments for a given MarketBundle (set of yield and price curves).
 * Calculator for Forex instruments requiring only discounting (and no exchange rate).
 */
public class PresentValueForexMarketCalculator extends PresentValueMarketCalculator {

  /**
   * The unique instance of the method.
   */
  private static final PresentValueForexMarketCalculator INSTANCE = new PresentValueForexMarketCalculator();

  /**
   * Return the unique instance of the class.
   * @return The instance.
   */
  public static PresentValueForexMarketCalculator getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  protected PresentValueForexMarketCalculator() {
  }

  /**
   * The methods used by the different instruments.
   */
  private static final ForexDiscountingMarketMethod METHOD_FOREX = ForexDiscountingMarketMethod.getInstance();
  private static final ForexSwapDiscountingMarketMethod METHOD_FXSWAP = ForexSwapDiscountingMarketMethod.getInstance();

  @Override
  public MultipleCurrencyAmount visitForex(final Forex derivative, final MarketBundle market) {
    return METHOD_FOREX.presentValue(derivative, market);
  }

  @Override
  public MultipleCurrencyAmount visitForexSwap(final ForexSwap derivative, final MarketBundle market) {
    return METHOD_FXSWAP.presentValue(derivative, market);
  }

}
