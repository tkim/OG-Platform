/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.future.market;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.interestrate.InterestRateCurveSensitivity;
import com.opengamma.financial.interestrate.InterestRateDerivative;
import com.opengamma.financial.interestrate.future.definition.InterestRateFuture;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.tuple.DoublesPair;

/**
 * Method to compute the price for an interest rate future with discounting (like a forward). 
 * No convexity adjustment is done. 
 */
public final class InterestRateFutureDiscountingMarketMethod extends InterestRateFutureMarketMethod {

  /*
   * The unique instance of the method.
   */
  private static final InterestRateFutureDiscountingMarketMethod INSTANCE = new InterestRateFutureDiscountingMarketMethod();

  /*
   * Gets the method unique instance.
   */
  public static InterestRateFutureDiscountingMarketMethod getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  private InterestRateFutureDiscountingMarketMethod() {
  }

  /**
   * Computes the price of a future from the curves using an estimation of the future rate without convexity adjustment.
   * @param future The future.
   * @param market The market curves.
   * @return The price.
   */
  public double price(final InterestRateFuture future, final MarketBundle market) {
    Validate.notNull(future, "Future");
    Validate.notNull(market, "Market");
    final double forward = market.getForwardRate(future.getIborIndex(), future.getFixingPeriodStartTime(), future.getFixingPeriodEndTime(), future.getFixingPeriodAccrualFactor());
    final double price = 1.0 - forward;
    return price;
  }

  public CurrencyAmount presentValue(final InterestRateFuture future, final MarketBundle market) {
    final double pv = presentValueFromPrice(future, price(future, market));
    return CurrencyAmount.of(future.getCurrency(), pv);
  }

  @Override
  public CurrencyAmount presentValue(final InterestRateDerivative instrument, final MarketBundle market) {
    Validate.isTrue(instrument instanceof InterestRateFuture, "Interest rate future");
    return presentValue((InterestRateFuture) instrument, market);
  }

  /**
   * Computes the future rate (1-price) from the curves using an estimation of the future rate without convexity adjustment.
   * @param future The future.
   * @param market The market curves.
   * @return The rate.
   */
  public double parRate(final InterestRateFuture future, final MarketBundle market) {
    Validate.notNull(future, "Future");
    Validate.notNull(market, "Market");
    final double forward = market.getForwardRate(future.getIborIndex(), future.getFixingPeriodStartTime(), future.getFixingPeriodEndTime(), future.getFixingPeriodAccrualFactor());
    return forward;
  }

  /**
   * Compute the price sensitivity to rates of a interest rate future by discounting.
   * @param future The future.
   * @param market The market curves.
   * @return The price rate sensitivity.
   */
  @Override
  public InterestRateCurveSensitivity priceCurveSensitivity(final InterestRateFuture future, final MarketBundle market) {
    Validate.notNull(future, "Future");
    Validate.notNull(market, "Market");
    final YieldAndDiscountCurve forwardCurve = market.getCurve(future.getIborIndex());
    double dfForwardStart = forwardCurve.getDiscountFactor(future.getFixingPeriodStartTime());
    double dfForwardEnd = forwardCurve.getDiscountFactor(future.getFixingPeriodEndTime());
    // Backward sweep
    double priceBar = 1.0;
    double forwardBar = -priceBar;
    double dfForwardEndBar = -dfForwardStart / (dfForwardEnd * dfForwardEnd) / future.getFixingPeriodAccrualFactor() * forwardBar;
    double dfForwardStartBar = 1.0 / (future.getFixingPeriodAccrualFactor() * dfForwardEnd) * forwardBar;
    Map<String, List<DoublesPair>> resultMap = new HashMap<String, List<DoublesPair>>();
    List<DoublesPair> listForward = new ArrayList<DoublesPair>();
    listForward.add(new DoublesPair(future.getFixingPeriodStartTime(), -future.getFixingPeriodStartTime() * dfForwardStart * dfForwardStartBar));
    listForward.add(new DoublesPair(future.getFixingPeriodEndTime(), -future.getFixingPeriodEndTime() * dfForwardEnd * dfForwardEndBar));
    resultMap.put(future.getForwardCurveName(), listForward);
    InterestRateCurveSensitivity result = new InterestRateCurveSensitivity(resultMap);
    return result;
  }
}
