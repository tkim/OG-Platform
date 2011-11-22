/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.forex.market;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.forex.derivative.Forex;
import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.method.PricingMarketMethod;
import com.opengamma.financial.interestrate.payments.market.PaymentFixedDiscountingMarketMethod;
import com.opengamma.util.money.MultipleCurrencyAmount;

/**
 * Pricing method for Forex transactions (spot or forward) by discounting each payment.
 */
public final class ForexDiscountingMarketMethod implements PricingMarketMethod {

  /**
   * The method unique instance.
   */
  private static final ForexDiscountingMarketMethod INSTANCE = new ForexDiscountingMarketMethod();

  /**
   * Return the unique instance of the class.
   * @return The instance.
   */
  public static ForexDiscountingMarketMethod getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  private ForexDiscountingMarketMethod() {
  }

  /**
   * The method for fixed payments.
   */
  public static final PaymentFixedDiscountingMarketMethod METHOD_PAYMENT = PaymentFixedDiscountingMarketMethod.getInstance();

  /**
   * Compute the present value by discounting in payment in its own currency.
   * @param fx The Forex derivative.
   * @param market The market curves.
   * @return The multi-currency present value.
   */
  public MultipleCurrencyAmount presentValue(final Forex fx, final MarketBundle market) {
    final MultipleCurrencyAmount pv = METHOD_PAYMENT.presentValue(fx.getPaymentCurrency1(), market);
    return pv.plus(METHOD_PAYMENT.presentValue(fx.getPaymentCurrency2(), market));
  }

  @Override
  public MultipleCurrencyAmount presentValue(final InstrumentDerivative derivative, final MarketBundle market) {
    Validate.isTrue(derivative instanceof Forex, "Derivative should be Forex");
    return presentValue((Forex) derivative, market);
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
