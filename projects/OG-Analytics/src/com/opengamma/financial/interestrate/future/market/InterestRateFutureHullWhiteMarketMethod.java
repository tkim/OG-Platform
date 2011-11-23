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

import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.InterestRateCurveSensitivity;
import com.opengamma.financial.interestrate.future.definition.InterestRateFuture;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.MarketWithHullWhiteBundle;
import com.opengamma.financial.model.interestrate.HullWhiteOneFactorPiecewiseConstantInterestRateModel;
import com.opengamma.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.tuple.DoublesPair;

/**
 * Method to compute the price for an interest rate future with discounting (like a forward). 
 * No convexity adjustment is done. 
 */
public final class InterestRateFutureHullWhiteMarketMethod extends InterestRateFutureMarketMethod {

  /**
   * The unique instance of the method.
   */
  private static final InterestRateFutureHullWhiteMarketMethod INSTANCE = new InterestRateFutureHullWhiteMarketMethod();

  /**
   * Return the unique instance of the class.
   * @return The instance.
   */
  public static InterestRateFutureHullWhiteMarketMethod getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  private InterestRateFutureHullWhiteMarketMethod() {
  }

  /**
   * The Hull-White model.
   */
  private static final HullWhiteOneFactorPiecewiseConstantInterestRateModel MODEL = new HullWhiteOneFactorPiecewiseConstantInterestRateModel();

  /**
   * Computes the price of a future from the curves using an estimation of the future rate without convexity adjustment.
   * @param future The future.
   * @param market The market curves.
   * @return The price.
   */
  public double price(final InterestRateFuture future, final MarketWithHullWhiteBundle market) {
    Validate.notNull(future, "Future");
    Validate.notNull(market, "Market");
    final YieldAndDiscountCurve forwardCurve = market.getCurve(future.getIborIndex());
    double dfForwardStart = forwardCurve.getDiscountFactor(future.getFixingPeriodStartTime());
    double dfForwardEnd = forwardCurve.getDiscountFactor(future.getFixingPeriodEndTime());
    double forward = (dfForwardStart / dfForwardEnd - 1) / future.getFixingPeriodAccrualFactor();
    double futureConvexityFactor = MODEL.futureConvexityFactor(future.getLastTradingTime(), future.getFixingPeriodStartTime(), future.getFixingPeriodEndTime(), market.getParameters());
    double price = 1.0 - futureConvexityFactor * forward + (1 - futureConvexityFactor) / future.getFixingPeriodAccrualFactor();
    return price;
  }

  /**
   * Compute the present value of a future from the curves using an estimation of the future rate without convexity adjustment.
   * @param future The future.
   * @param market The market.
   * @return The present value.
   */
  public MultipleCurrencyAmount presentValue(final InterestRateFuture future, final MarketWithHullWhiteBundle market) {
    final double pv = presentValueFromPrice(future, price(future, market));
    return MultipleCurrencyAmount.of(future.getCurrency(), pv);
  }

  @Override
  public MultipleCurrencyAmount presentValue(final InstrumentDerivative instrument, final MarketBundle market) {
    Validate.isTrue(instrument instanceof InterestRateFuture, "Interest rate future");
    Validate.isTrue(market instanceof MarketWithHullWhiteBundle, "Market shoudl provide Hull-White parameters");
    return presentValue((InterestRateFuture) instrument, (MarketWithHullWhiteBundle) market);
  }

  /**
   * Compute the price sensitivity to rates of a interest rate future by discounting.
   * @param future The future.
   * @param market The market curves.
   * @return The price rate sensitivity.
   */
  public InterestRateCurveSensitivity priceCurveSensitivity(final InterestRateFuture future, final MarketWithHullWhiteBundle market) {
    Validate.notNull(future, "Future");
    Validate.notNull(market, "Market");
    final YieldAndDiscountCurve forwardCurve = market.getCurve(future.getIborIndex());
    double dfForwardStart = forwardCurve.getDiscountFactor(future.getFixingPeriodStartTime());
    double dfForwardEnd = forwardCurve.getDiscountFactor(future.getFixingPeriodEndTime());
    double futureConvexityFactor = MODEL.futureConvexityFactor(future.getLastTradingTime(), future.getFixingPeriodStartTime(), future.getFixingPeriodEndTime(), market.getParameters());
    // Backward sweep
    double priceBar = 1.0;
    double forwardBar = -futureConvexityFactor * priceBar;
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

  @Override
  public InterestRateCurveSensitivity priceCurveSensitivity(final InterestRateFuture future, final MarketBundle market) {
    Validate.isTrue(market instanceof MarketWithHullWhiteBundle, "Market shoudl provide Hull-White parameters");
    return priceCurveSensitivity(future, (MarketWithHullWhiteBundle) market);
  }

}
