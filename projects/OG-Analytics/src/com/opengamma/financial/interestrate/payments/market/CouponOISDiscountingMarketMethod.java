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

import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.PresentValueCurveSensitivityMarket;
import com.opengamma.financial.interestrate.method.PricingMarketMethod;
import com.opengamma.financial.interestrate.payments.derivative.CouponOIS;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.tuple.DoublesPair;

/**
 * Method to compute present value and present value sensitivity for fixed coupon.
 */
public final class CouponOISDiscountingMarketMethod implements PricingMarketMethod {

  /**
   * The unique instance of the method.
   */
  private static final CouponOISDiscountingMarketMethod INSTANCE = new CouponOISDiscountingMarketMethod();

  /**
   * Return the unique instance of the class.
   * @return The instance.
   */
  public static CouponOISDiscountingMarketMethod getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  private CouponOISDiscountingMarketMethod() {
  }

  /**
   * Compute the present value of a OIS coupon by discounting.
   * @param coupon The coupon.
   * @param market The market curves.
   * @return The present value sensitivity.
   */
  public MultipleCurrencyAmount presentValue(final CouponOIS coupon, final MarketBundle market) {
    Validate.notNull(coupon, "Coupon");
    Validate.notNull(market, "Market");
    final double df = market.getDiscountingFactor(coupon.getCurrency(), coupon.getPaymentTime());
    final double forward = market.getForwardRate(coupon.getIndex(), coupon.getFixingPeriodStartTime(), coupon.getFixingPeriodEndTime(), coupon.getFixingPeriodAccrualFactor());
    final double pv = (coupon.getNotionalAccrued() * (1 + coupon.getFixingPeriodAccrualFactor() * forward) - coupon.getNotional()) * df;
    return MultipleCurrencyAmount.of(coupon.getCurrency(), pv);
  }

  @Override
  public MultipleCurrencyAmount presentValue(final InstrumentDerivative instrument, final MarketBundle market) {
    Validate.isTrue(instrument instanceof CouponOIS, "Coupon OIS");
    return presentValue((CouponOIS) instrument, market);
  }

  /**
   * Compute the present value sensitivity to rates of a OIS coupon by discounting.
   * @param coupon The coupon.
   * @param market The market curves.
   * @return The present value sensitivity.
   */
  public PresentValueCurveSensitivityMarket presentValueCurveSensitivity(final CouponOIS coupon, final MarketBundle market) {
    Validate.notNull(coupon, "Coupon");
    Validate.notNull(market, "Market");
    final double df = market.getDiscountingFactor(coupon.getCurrency(), coupon.getPaymentTime());
    final double dfForwardStart = market.getCurve(coupon.getIndex()).getDiscountFactor(coupon.getFixingPeriodStartTime());
    final double dfForwardEnd = market.getCurve(coupon.getIndex()).getDiscountFactor(coupon.getFixingPeriodEndTime());
    final double accruedFwd = dfForwardStart / dfForwardEnd;
    final double pv = (coupon.getNotionalAccrued() * accruedFwd - coupon.getNotional()) * df;
    // Backward sweep
    final double pvBar = 1.0;
    final double accruedFwdBar = coupon.getNotionalAccrued() * df * pvBar;
    final double dfForwardEndBar = -dfForwardStart / (dfForwardEnd * dfForwardEnd) * accruedFwdBar;
    final double dfForwardStartBar = 1.0 / dfForwardEnd * accruedFwdBar;
    final double dfBar = pv / df * pvBar;

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
