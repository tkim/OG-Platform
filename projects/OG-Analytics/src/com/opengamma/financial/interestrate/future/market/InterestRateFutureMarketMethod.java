/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.future.market;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.interestrate.InterestRateCurveSensitivity;
import com.opengamma.financial.interestrate.future.definition.InterestRateFuture;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.method.PricingMarketMethod;

/**
 * Methods for the pricing of interest rate futures generic to all models.
 */
public abstract class InterestRateFutureMarketMethod implements PricingMarketMethod {

  /**
   * Compute the present value of a future transaction from a quoted price.
   * @param future The future.
   * @param price The quoted price.
   * @return The present value.
   */
  public double presentValueFromPrice(final InterestRateFuture future, final double price) {
    double pv = (price - future.getReferencePrice()) * future.getPaymentAccrualFactor() * future.getNotional();
    return pv;
  }

  /**
   * Compute the present value sensitivity to rates of a interest rate future by discounting.
   * @param future The future.
   * @param market The market curves.
   * @return The present value rate sensitivity.
   */
  public InterestRateCurveSensitivity presentValueCurveSensitivity(final InterestRateFuture future, final MarketBundle market) {
    Validate.notNull(future, "Future");
    InterestRateCurveSensitivity priceSensi = priceCurveSensitivity(future, market);
    InterestRateCurveSensitivity result = priceSensi.multiply(future.getPaymentAccrualFactor() * future.getNotional());
    return result;
  }

  /**
   * Compute the price sensitivity to rates of a interest rate future by discounting.
   * @param future The future.
   * @param market The market curves.
   * @return The price rate sensitivity.
   */
  public abstract InterestRateCurveSensitivity priceCurveSensitivity(final InterestRateFuture future, final MarketBundle market);

}
