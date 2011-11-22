/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.forex.market;

import static org.testng.AssertJUnit.assertEquals;

import javax.time.calendar.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.financial.forex.definition.ForexSwapDefinition;
import com.opengamma.financial.forex.derivative.ForexSwap;
import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.MarketDataSets;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.time.DateUtils;

/**
 * Test related to the method for Forex Swap transaction by discounting on each payment.
 */
public class ForexSwapDiscountingMarketMethodTest {

  private static final Currency CUR_1 = Currency.EUR;
  private static final Currency CUR_2 = Currency.USD;
  private static final ZonedDateTime NEAR_DATE = DateUtils.getUTCDate(2011, 5, 26);
  private static final ZonedDateTime FAR_DATE = DateUtils.getUTCDate(2011, 6, 27); // 1m
  private static final double NOMINAL_1 = 100000000;
  private static final double FX_RATE = 1.4177;
  private static final double FORWARD_POINTS = -0.0007;
  private static final ForexSwapDefinition FX_SWAP_DEFINITION_FIN = new ForexSwapDefinition(CUR_1, CUR_2, NEAR_DATE, FAR_DATE, NOMINAL_1, FX_RATE, FORWARD_POINTS);

  private static final MarketBundle MARKET = MarketDataSets.createMarket1();
  private static final String NOT_USED = "Not used";
  private static final String[] NOT_USED_2 = new String[] {NOT_USED, NOT_USED};
  private static final ZonedDateTime REFERENCE_DATE = DateUtils.getUTCDate(2011, 5, 20);
  private static final ForexSwap FX_SWAP = FX_SWAP_DEFINITION_FIN.toDerivative(REFERENCE_DATE, NOT_USED_2);

  private static final ForexSwapDiscountingMarketMethod METHOD_SWAP = ForexSwapDiscountingMarketMethod.getInstance();
  private static final ForexDiscountingMarketMethod METHOD_FX = ForexDiscountingMarketMethod.getInstance();
  private static final PresentValueForexMarketCalculator PVC_FX = PresentValueForexMarketCalculator.getInstance();

  @Test
  /**
   * Tests the present value computation.
   */
  public void presentValue() {
    final MultipleCurrencyAmount pv = METHOD_SWAP.presentValue(FX_SWAP, MARKET);
    final MultipleCurrencyAmount pvNear = METHOD_FX.presentValue(FX_SWAP.getNearLeg(), MARKET);
    final MultipleCurrencyAmount pvFar = METHOD_FX.presentValue(FX_SWAP.getFarLeg(), MARKET);
    assertEquals(pvNear.getAmount(CUR_1) + pvFar.getAmount(CUR_1), pv.getAmount(CUR_1));
    assertEquals(pvNear.getAmount(CUR_2) + pvFar.getAmount(CUR_2), pv.getAmount(CUR_2));
  }

  @Test
  /**
   * Test the present value through the method and through the calculator.
   */
  public void presentValueMethodVsCalculator() {
    final MultipleCurrencyAmount pvMethod = METHOD_SWAP.presentValue(FX_SWAP, MARKET);
    final MultipleCurrencyAmount pvCalculator = PVC_FX.visit(FX_SWAP, MARKET);
    assertEquals("Forex present value: Method vs Calculator", pvMethod, pvCalculator);
    final InstrumentDerivative fxSwap = FX_SWAP;
    final MultipleCurrencyAmount pvMethod2 = METHOD_SWAP.presentValue(fxSwap, MARKET);
    assertEquals("Forex present value: Method ForexSwap vs Method ForexDerivative", pvMethod, pvMethod2);
  }

  @Test
  /**
   * Tests the currency exposure computation.
   */
  public void currencyExposure() {
    final MultipleCurrencyAmount exposureMethod = METHOD_SWAP.currencyExposure(FX_SWAP, MARKET);
    final MultipleCurrencyAmount pv = METHOD_SWAP.presentValue(FX_SWAP, MARKET);
    assertEquals("Currency exposure", pv, exposureMethod);
    //    final MultipleCurrencyAmount exposureCalculator = CEC_FX.visit(FX_SWAP, MARKET);
    //    assertEquals("Currency exposure: Method vs Calculator", exposureMethod, exposureCalculator);
  }

}
