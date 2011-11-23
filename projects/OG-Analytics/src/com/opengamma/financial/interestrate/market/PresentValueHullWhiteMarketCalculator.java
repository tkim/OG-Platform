/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market;

import com.opengamma.financial.interestrate.future.definition.InterestRateFuture;
import com.opengamma.financial.interestrate.future.market.InterestRateFutureHullWhiteMarketMethod;
import com.opengamma.util.money.MultipleCurrencyAmount;

/**
 * Calculates the present value of instruments for a given MarketBundle (set of yield and price curves).
 * Calculator for with Hull-White one-factor model for contingent instruments.
 */
public class PresentValueHullWhiteMarketCalculator extends PresentValueMarketCalculator {

  /**
   * The unique instance of the method.
   */
  private static final PresentValueHullWhiteMarketCalculator INSTANCE = new PresentValueHullWhiteMarketCalculator();

  /**
   * Return the unique instance of the class.
   * @return The instance.
   */
  public static PresentValueHullWhiteMarketCalculator getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  protected PresentValueHullWhiteMarketCalculator() {
  }

  /**
   * The methods used by the different instruments.
   */
  private static final InterestRateFutureHullWhiteMarketMethod METHOD_FUT_HW = InterestRateFutureHullWhiteMarketMethod.getInstance();

  @Override
  public MultipleCurrencyAmount visitInterestRateFuture(final InterestRateFuture future, final MarketBundle market) {
    return METHOD_FUT_HW.presentValue(future, market);
  }

}
