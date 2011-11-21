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
import com.opengamma.financial.interestrate.payments.CouponFixed;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.tuple.DoublesPair;

/**
 * Method to compute present value and present value sensitivity for fixed coupon.
 */
public final class CouponFixedDiscountingMarketMethod implements PricingMarketMethod {

  /*
   * The unique instance of the method.
   */
  private static final CouponFixedDiscountingMarketMethod INSTANCE = new CouponFixedDiscountingMarketMethod();

  /*
   * Gets the method unique instance.
   */
  public static CouponFixedDiscountingMarketMethod getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  private CouponFixedDiscountingMarketMethod() {
  }

  /**
   * Compute the present value of a Fixed coupon by discounting.
   * @param coupon The coupon.
   * @param market The market curves.
   * @return The present value sensitivity.
   */
  public CurrencyAmount presentValue(final CouponFixed coupon, final MarketBundle market) {
    Validate.notNull(coupon, "Coupon");
    Validate.notNull(market, "Market");
    final double df = market.getDiscountingFactor(coupon.getCurrency(), coupon.getPaymentTime());
    final double pv = coupon.getAmount() * df;
    return CurrencyAmount.of(coupon.getCurrency(), pv);
  }

  @Override
  public CurrencyAmount presentValue(final InstrumentDerivative instrument, final MarketBundle market) {
    Validate.isTrue(instrument instanceof CouponFixed, "Coupon Fixed");
    return presentValue((CouponFixed) instrument, market);
  }

  /**
   * Compute the present value sensitivity to rates of a Fixed coupon by discounting.
   * @param coupon The coupon.
   * @param market The market curves.
   * @return The present value sensitivity.
   */
  public PresentValueCurveSensitivityMarket presentValueCurveSensitivity(final CouponFixed coupon, final MarketBundle market) {
    Validate.notNull(coupon, "Coupon");
    Validate.notNull(market, "Market");
    final double df = market.getDiscountingFactor(coupon.getCurrency(), coupon.getPaymentTime());
    final double pvBar = 1.0;
    final double dfBar = coupon.getAmount() * pvBar;
    final Map<String, List<DoublesPair>> result = new HashMap<String, List<DoublesPair>>();
    final List<DoublesPair> listDiscounting = new ArrayList<DoublesPair>();
    listDiscounting.add(new DoublesPair(coupon.getPaymentTime(), -coupon.getPaymentTime() * df * dfBar));
    result.put(market.getCurve(coupon.getCurrency()).getCurve().getName(), listDiscounting);
    return new PresentValueCurveSensitivityMarket(result);
  }

}
