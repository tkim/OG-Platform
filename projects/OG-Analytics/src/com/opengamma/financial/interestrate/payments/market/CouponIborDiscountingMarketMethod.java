/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.payments.market;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.interestrate.InterestRateDerivative;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.PresentValueCurveSensitivityMarket;
import com.opengamma.financial.interestrate.method.PricingMarketMethod;
import com.opengamma.financial.interestrate.payments.CouponIbor;
import com.opengamma.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.tuple.DoublesPair;

/**
 * Method to compute present value and present value sensitivity for Ibor coupon with spread by discounting.
 */
public final class CouponIborDiscountingMarketMethod implements PricingMarketMethod {

  /*
   * The unique instance of the method.
   */
  private static final CouponIborDiscountingMarketMethod INSTANCE = new CouponIborDiscountingMarketMethod();

  /*
   * Gets the method unique instance.
   */
  public static CouponIborDiscountingMarketMethod getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  private CouponIborDiscountingMarketMethod() {
  }

  /**
   * Compute the present value of a Ibor coupon with spread by discounting.
   * @param coupon The coupon.
   * @param market The market curves.
   * @return The present value.
   */
  public CurrencyAmount presentValue(final CouponIbor coupon, final MarketBundle market) {
    Validate.notNull(coupon, "Coupon");
    Validate.notNull(market, "Market");
    final double forward = market.getForwardRate(coupon.getIndex(), coupon.getFixingPeriodStartTime(), coupon.getFixingPeriodEndTime(), coupon.getFixingYearFraction());
    final double df = market.getDiscountingFactor(coupon.getCurrency(), coupon.getPaymentTime());
    final double value = (coupon.getNotional() * coupon.getPaymentYearFraction() * forward + coupon.getSpreadAmount()) * df;
    return CurrencyAmount.of(coupon.getCurrency(), value);
  }

  @Override
  public CurrencyAmount presentValue(final InterestRateDerivative instrument, final MarketBundle market) {
    Validate.isTrue(instrument instanceof CouponIbor, "CouponIborDiscountingMethod: The instrument should be of type CouponIbor");
    return presentValue((CouponIbor) instrument, market);
  }

  /**
   * Compute the present value sensitivity to rates of a Ibor coupon with gearing and spread by discounting.
   * @param coupon The coupon.
   * @param market The market curves.
   * @return The present value sensitivity.
   */
  public PresentValueCurveSensitivityMarket presentValueCurveSensitivity(final CouponIbor coupon, final MarketBundle market) {
    Validate.notNull(coupon, "Coupon");
    Validate.notNull(market, "Curves");
    final YieldAndDiscountCurve forwardCurve = market.getCurve(coupon.getIndex());
    final double df = market.getDiscountingFactor(coupon.getCurrency(), coupon.getPaymentTime());
    final double dfForwardStart = forwardCurve.getDiscountFactor(coupon.getFixingPeriodStartTime());
    final double dfForwardEnd = forwardCurve.getDiscountFactor(coupon.getFixingPeriodEndTime());
    final double forward = (dfForwardStart / dfForwardEnd - 1.0) / coupon.getFixingYearFraction();
    // Backward sweep
    final double pvBar = 1.0;
    final double forwardBar = coupon.getNotional() * coupon.getPaymentYearFraction() * df * pvBar;
    final double dfForwardEndBar = -dfForwardStart / (dfForwardEnd * dfForwardEnd) / coupon.getFixingYearFraction() * forwardBar;
    final double dfForwardStartBar = 1.0 / (coupon.getFixingYearFraction() * dfForwardEnd) * forwardBar;
    final double dfBar = (coupon.getNotional() * coupon.getPaymentYearFraction() * forward + coupon.getSpreadAmount()) * pvBar;

    final Map<String, List<DoublesPair>> resultDsc = new HashMap<String, List<DoublesPair>>();
    final List<DoublesPair> listDiscounting = new ArrayList<DoublesPair>();
    listDiscounting.add(new DoublesPair(coupon.getPaymentTime(), -coupon.getPaymentTime() * df * dfBar));
    resultDsc.put(market.getCurve(coupon.getCurrency()).getCurve().getName(), listDiscounting);
    PresentValueCurveSensitivityMarket result = new PresentValueCurveSensitivityMarket(resultDsc);

    final Map<String, List<DoublesPair>> resultFwd = new HashMap<String, List<DoublesPair>>();
    final List<DoublesPair> listForward = new ArrayList<DoublesPair>();
    listForward.add(new DoublesPair(coupon.getFixingPeriodStartTime(), -coupon.getFixingPeriodStartTime() * dfForwardStart * dfForwardStartBar));
    listForward.add(new DoublesPair(coupon.getFixingPeriodEndTime(), -coupon.getFixingPeriodEndTime() * dfForwardEnd * dfForwardEndBar));
    resultFwd.put(market.getCurve(coupon.getIndex()).getCurve().getName(), listForward);
    return PresentValueCurveSensitivityMarket.plus(result, new PresentValueCurveSensitivityMarket(resultFwd));
  }

}
