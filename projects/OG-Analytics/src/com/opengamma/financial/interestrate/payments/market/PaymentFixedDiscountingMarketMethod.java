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
import com.opengamma.financial.interestrate.payments.PaymentFixed;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.tuple.DoublesPair;

/**
 * Method to compute present value and present value sensitivity for fixed coupon.
 */
public final class PaymentFixedDiscountingMarketMethod implements PricingMarketMethod {

  /**
   * The unique instance of the method.
   */
  private static final PaymentFixedDiscountingMarketMethod INSTANCE = new PaymentFixedDiscountingMarketMethod();

  /**
   * Return the unique instance of the class.
   * @return The instance.
   */
  public static PaymentFixedDiscountingMarketMethod getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  private PaymentFixedDiscountingMarketMethod() {
  }

  /**
   * Compute the present value of a Fixed coupon by discounting.
   * @param payment The fixed payment.
   * @param market The market curves.
   * @return The present value sensitivity.
   */
  public MultipleCurrencyAmount presentValue(final PaymentFixed payment, final MarketBundle market) {
    Validate.notNull(payment, "Coupon");
    Validate.notNull(market, "Market");
    final double df = market.getDiscountingFactor(payment.getCurrency(), payment.getPaymentTime());
    final double pv = payment.getAmount() * df;
    return MultipleCurrencyAmount.of(payment.getCurrency(), pv);
  }

  @Override
  public MultipleCurrencyAmount presentValue(final InstrumentDerivative instrument, final MarketBundle market) {
    Validate.isTrue(instrument instanceof CouponFixed, "Coupon Fixed");
    return presentValue(instrument, market);
  }

  /**
   * Compute the present value sensitivity to rates of a Fixed coupon by discounting.
   * @param payment The fixed payment.
   * @param market The market curves.
   * @return The present value sensitivity.
   */
  public PresentValueCurveSensitivityMarket presentValueCurveSensitivity(final PaymentFixed payment, final MarketBundle market) {
    Validate.notNull(payment, "Coupon");
    Validate.notNull(market, "Market");
    final double df = market.getDiscountingFactor(payment.getCurrency(), payment.getPaymentTime());
    final double pvBar = 1.0;
    final double dfBar = payment.getAmount() * pvBar;
    final Map<String, List<DoublesPair>> result = new HashMap<String, List<DoublesPair>>();
    final List<DoublesPair> listDiscounting = new ArrayList<DoublesPair>();
    listDiscounting.add(new DoublesPair(payment.getPaymentTime(), -payment.getPaymentTime() * df * dfBar));
    result.put(market.getCurve(payment.getCurrency()).getCurve().getName(), listDiscounting);
    return new PresentValueCurveSensitivityMarket(result);
  }

}
