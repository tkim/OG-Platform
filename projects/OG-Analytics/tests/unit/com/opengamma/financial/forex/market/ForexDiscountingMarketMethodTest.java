/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.forex.market;

import static org.testng.AssertJUnit.assertEquals;

import javax.time.calendar.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.financial.forex.definition.ForexDefinition;
import com.opengamma.financial.forex.derivative.Forex;
import com.opengamma.financial.instrument.payment.PaymentFixedDefinition;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.MarketDataSets;
import com.opengamma.financial.interestrate.payments.PaymentFixed;
import com.opengamma.financial.interestrate.payments.market.PaymentFixedDiscountingMarketMethod;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.time.DateUtils;

/**
 * Test related to the method for Forex transaction by discounting on each payment.
 */
public class ForexDiscountingMarketMethodTest {

  private static final Currency CUR_1 = Currency.EUR;
  private static final Currency CUR_2 = Currency.USD;
  private static final ZonedDateTime PAYMENT_DATE = DateUtils.getUTCDate(2011, 5, 24);
  private static final double NOMINAL_1 = 100000000;
  private static final double FX_RATE = 1.4177;
  private static final ForexDefinition FX_DEFINITION = new ForexDefinition(CUR_1, CUR_2, PAYMENT_DATE, NOMINAL_1, FX_RATE);
  private static final MarketBundle MARKET = MarketDataSets.createMarket1();
  private static final String NOT_USED = "Not used";
  private static final String[] NOT_USED_2 = new String[] {NOT_USED, NOT_USED};
  private static final ZonedDateTime REFERENCE_DATE = DateUtils.getUTCDate(2011, 5, 20);
  private static final Forex FX = FX_DEFINITION.toDerivative(REFERENCE_DATE, NOT_USED_2);
  private static final PaymentFixedDefinition PAY_DEFINITION_1 = new PaymentFixedDefinition(CUR_1, PAYMENT_DATE, NOMINAL_1);
  private static final PaymentFixed PAY_1 = PAY_DEFINITION_1.toDerivative(REFERENCE_DATE, NOT_USED);
  private static final PaymentFixedDefinition PAY_DEFINITION_2 = new PaymentFixedDefinition(CUR_2, PAYMENT_DATE, -NOMINAL_1 * FX_RATE);
  private static final PaymentFixed PAY_2 = PAY_DEFINITION_2.toDerivative(REFERENCE_DATE, NOT_USED);

  private static final ForexDiscountingMarketMethod METHOD_FX = ForexDiscountingMarketMethod.getInstance();
  private static final PaymentFixedDiscountingMarketMethod METHOD_PAYMENT = PaymentFixedDiscountingMarketMethod.getInstance();
  private static final PresentValueForexMarketCalculator PVC_FX = PresentValueForexMarketCalculator.getInstance();

  @Test
  /**
   * Tests the present value computation.
   */
  public void presentValue() {
    final MultipleCurrencyAmount pv = METHOD_FX.presentValue(FX, MARKET);
    final MultipleCurrencyAmount ca1 = METHOD_PAYMENT.presentValue(PAY_1, MARKET);
    final MultipleCurrencyAmount ca2 = METHOD_PAYMENT.presentValue(PAY_2, MARKET);
    assertEquals("FX - Present value", ca1.getAmount(CUR_1), pv.getAmount(CUR_1));
    assertEquals("FX - Present value", ca2.getAmount(CUR_2), pv.getAmount(CUR_2));
  }

  @Test
  /**
   * Test the present value through the method and through the calculator.
   */
  public void presentValueMethodVsCalculator() {
    final MultipleCurrencyAmount pvMethod = METHOD_FX.presentValue(FX, MARKET);
    final MultipleCurrencyAmount pvCalculator = PVC_FX.visit(FX, MARKET);
    assertEquals("Forex present value: Method vs Calculator", pvMethod, pvCalculator);
  }

  @Test
  /**
   * Test the present value of EUR/USD is the same as an USD/EUR.
   */
  public void presentValueReverse() {
    final ForexDefinition fxReverseDefinition = new ForexDefinition(CUR_2, CUR_1, PAYMENT_DATE, -NOMINAL_1 * FX_RATE, 1.0 / FX_RATE);
    final Forex fxReverse = fxReverseDefinition.toDerivative(REFERENCE_DATE, NOT_USED_2);
    final MultipleCurrencyAmount pv = METHOD_FX.presentValue(FX, MARKET);
    final MultipleCurrencyAmount pvReverse = METHOD_FX.presentValue(fxReverse, MARKET);
    assertEquals("Forex present value: Reverse description", pv.getAmount(CUR_1), pvReverse.getAmount(CUR_1), 1.0E-2);
    assertEquals("Forex present value: Reverse description", pv.getAmount(CUR_2), pvReverse.getAmount(CUR_2), 1.0E-2);
  }

  @Test
  /**
   * Tests the currency exposure computation.
   */
  public void currencyExposure() {
    final MultipleCurrencyAmount exposureMethod = METHOD_FX.currencyExposure(FX, MARKET);
    final MultipleCurrencyAmount pv = METHOD_FX.presentValue(FX, MARKET);
    assertEquals("Currency exposure", pv, exposureMethod);
    //    final MultipleCurrencyAmount exposureCalculator = CEC_FX.visit(FX, MARKET);
    //    assertEquals("Currency exposure: Method vs Calculator", exposureMethod, exposureCalculator);
  }

}
