/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.cash.market;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;

import javax.time.calendar.Period;
import javax.time.calendar.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.daycount.DayCount;
import com.opengamma.financial.instrument.cash.DepositDefinition;
import com.opengamma.financial.instrument.index.GeneratorDeposit;
import com.opengamma.financial.instrument.index.IborIndex;
import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.instrument.index.generator.EURDeposit;
import com.opengamma.financial.interestrate.cash.derivative.Cash;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.MarketDataSets;
import com.opengamma.financial.interestrate.market.MarketQuoteMarketCalculator;
import com.opengamma.financial.interestrate.market.PresentValueCurveSensitivityMarket;
import com.opengamma.financial.interestrate.market.PresentValueCurveSensitivityMarketCalculator;
import com.opengamma.financial.interestrate.market.PresentValueMarketCalculator;
import com.opengamma.financial.interestrate.method.market.SensitivityFiniteDifferenceMarket;
import com.opengamma.financial.schedule.ScheduleCalculator;
import com.opengamma.math.differentiation.FiniteDifferenceType;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.time.TimeCalculator;
import com.opengamma.util.tuple.DoublesPair;

/**
 * Tests related to the pricing and sensitivities of cash loan/deposit in the discounting method.
 */
public class CashDiscountingMarketMethodTest {

  private static final MarketBundle MARKET = MarketDataSets.createMarket1();
  private static final IndexDeposit[] INDEXES = MarketDataSets.getDepositIndexes();
  private static final IborIndex EURIBOR3M = (IborIndex) INDEXES[0];
  private static final Calendar CALENDAR_EUR = EURIBOR3M.getCalendar();
  private static final GeneratorDeposit GENERATOR_EUR = new EURDeposit(CALENDAR_EUR);
  private static final DayCount DAY_COUNT_COUPON = EURIBOR3M.getDayCount();
  private static final int SETTLEMENT_DAYS = EURIBOR3M.getSpotLag();
  private static final Currency EUR = EURIBOR3M.getCurrency();

  private static final ZonedDateTime REFERENCE_DATE = DateUtils.getUTCDate(2010, 11, 8);
  private static final ZonedDateTime SPOT_DATE = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, SETTLEMENT_DAYS, CALENDAR_EUR);
  private static final Period TENOR = Period.ofYears(1);
  private static final ZonedDateTime PAYMENT_DATE = ScheduleCalculator.getAdjustedDate(SPOT_DATE, TENOR, EURIBOR3M.getBusinessDayConvention(), CALENDAR_EUR, EURIBOR3M.isEndOfMonth());
  private static final double ACCRUAL_FACTOR_PAYMENT = DAY_COUNT_COUPON.getDayCountFraction(SPOT_DATE, PAYMENT_DATE);
  private static final double NOTIONAL = 100000000; // 100m
  private static final double RATE = 0.0225;
  private static final double SPOT_TIME = TimeCalculator.getTimeBetween(REFERENCE_DATE, SPOT_DATE);
  private static final double PAYMENT_TIME = TimeCalculator.getTimeBetween(REFERENCE_DATE, PAYMENT_DATE);

  private static final DepositDefinition DEPOSIT_DEFINITION = DepositDefinition.fromTrade(REFERENCE_DATE, TENOR, NOTIONAL, RATE, GENERATOR_EUR);
  private static final Cash DEPOSIT = new Cash(EUR, SPOT_TIME, PAYMENT_TIME, NOTIONAL, RATE, ACCRUAL_FACTOR_PAYMENT, "Not used");

  private static final CashDiscountingMarketMethod METHOD_DEPOSIT = CashDiscountingMarketMethod.getInstance();
  private static final PresentValueMarketCalculator PVC = PresentValueMarketCalculator.getInstance();
  private static final PresentValueCurveSensitivityMarketCalculator PVCSC = PresentValueCurveSensitivityMarketCalculator.getInstance();
  private static final MarketQuoteMarketCalculator MQC = MarketQuoteMarketCalculator.getInstance();

  private static final double TOLERANCE_PRICE = 1.0E-2;
  private static final double TOLERANCE_RATE = 1.0E-8;

  @Test
  /**
   * Tests the present value.
   */
  public void presentValueTrade() {
    MultipleCurrencyAmount pv = METHOD_DEPOSIT.presentValue(DEPOSIT, MARKET);
    double dfStart = MARKET.getDiscountFactor(EUR, DEPOSIT.getStartTime());
    double dfEnd = MARKET.getDiscountFactor(EUR, DEPOSIT.getEndTime());
    double pvExpected = -NOTIONAL * dfStart + NOTIONAL * (1.0 + ACCRUAL_FACTOR_PAYMENT * RATE) * dfEnd;
    assertEquals("Cash: pv by discounting", pvExpected, pv.getAmount(EUR), TOLERANCE_PRICE);
  }

  @Test
  /**
   * Tests present value.
   */
  public void presentValueBetweenTradeAndSettle() {
    ZonedDateTime referenceDate = DateUtils.getUTCDate(2010, 11, 9);
    Cash deposit = DEPOSIT_DEFINITION.toDerivative(referenceDate, "Not used");
    MultipleCurrencyAmount pvComputed = METHOD_DEPOSIT.presentValue(deposit, MARKET);
    double dfStart = MARKET.getDiscountFactor(EUR, deposit.getStartTime());
    double dfEnd = MARKET.getDiscountFactor(EUR, deposit.getEndTime());
    double pvExpected = (NOTIONAL + deposit.getInterestAmount()) * dfEnd - NOTIONAL * dfStart;
    assertEquals("DepositDefinition: present value", pvExpected, pvComputed.getAmount(EUR), TOLERANCE_PRICE);
  }

  @Test
  /**
   * Tests present value.
   */
  public void presentValueSettle() {
    ZonedDateTime referenceDate = SPOT_DATE;
    Cash deposit = DEPOSIT_DEFINITION.toDerivative(referenceDate, "Not used");
    MultipleCurrencyAmount pvComputed = METHOD_DEPOSIT.presentValue(deposit, MARKET);
    double dfEnd = MARKET.getDiscountFactor(EUR, deposit.getEndTime());
    double dfStart = MARKET.getDiscountFactor(EUR, deposit.getStartTime());
    double pvExpected = (NOTIONAL + deposit.getInterestAmount()) * dfEnd - NOTIONAL * dfStart;
    assertEquals("DepositDefinition: present value", pvExpected, pvComputed.getAmount(EUR), TOLERANCE_PRICE);
  }

  @Test
  /**
   * Tests present value.
   */
  public void presentValueBetweenSettleMaturity() {
    ZonedDateTime referenceDate = DateUtils.getUTCDate(2010, 11, 30);
    Cash deposit = DEPOSIT_DEFINITION.toDerivative(referenceDate, "Not used");
    MultipleCurrencyAmount pvComputed = METHOD_DEPOSIT.presentValue(deposit, MARKET);
    double dfEnd = MARKET.getDiscountFactor(EUR, deposit.getEndTime());
    double pvExpected = (NOTIONAL + deposit.getInterestAmount()) * dfEnd;
    assertEquals("DepositDefinition: present value", pvExpected, pvComputed.getAmount(EUR), TOLERANCE_PRICE);
  }

  @Test
  /**
   * Tests present value.
   */
  public void presentValueMaturity() {
    ZonedDateTime referenceDate = PAYMENT_DATE;
    Cash deposit = DEPOSIT_DEFINITION.toDerivative(referenceDate, "Not used");
    MultipleCurrencyAmount pvComputed = METHOD_DEPOSIT.presentValue(deposit, MARKET);
    double pvExpected = NOTIONAL + deposit.getInterestAmount();
    assertEquals("DepositDefinition: present value", pvExpected, pvComputed.getAmount(EUR), TOLERANCE_PRICE);
  }

  @Test
  /**
   * Compare the present value from the method and from the standard calculator.
   */
  public void presentValueMethodVsCalculator() {
    MultipleCurrencyAmount pvMethod = METHOD_DEPOSIT.presentValue(DEPOSIT, MARKET);
    MultipleCurrencyAmount pvCalculator = PVC.visit(DEPOSIT, MARKET);
    assertEquals("Coupon Fixed: pv by discounting", pvMethod.size(), pvCalculator.size());
    assertEquals("Coupon Fixed: pv by discounting", pvMethod.getAmount(EUR), pvCalculator.getAmount(EUR), 1.0E-2);
  }

  @Test
  /**
   * Tests the present value curve sensitivity by discounting.
   */
  public void presentValueCurveSensitivity() {
    PresentValueCurveSensitivityMarket pvcs = METHOD_DEPOSIT.presentValueCurveSensitivity(DEPOSIT, MARKET);
    pvcs = pvcs.clean();
    final double deltaTolerancePrice = 1.0E+0;
    // Testing note: Sensitivity is for a movement of 1. 1E+2 = 1 cent for a 1 bp move.
    final double deltaShift = 1.0E-6;
    // Discounting curve sensitivity
    final double[] nodeTimesDisc = new double[] {DEPOSIT.getStartTime(), DEPOSIT.getEndTime()};
    final double[] sensiDiscMethod = SensitivityFiniteDifferenceMarket
        .curveSensitivity(DEPOSIT, MARKET, DEPOSIT.getCurrency(), nodeTimesDisc, deltaShift, METHOD_DEPOSIT, FiniteDifferenceType.CENTRAL);
    assertEquals("Sensitivity finite difference method: number of node", 2, sensiDiscMethod.length);
    final List<DoublesPair> sensiPvDisc = pvcs.getYieldCurveSensitivities().get(MARKET.getCurve(DEPOSIT.getCurrency()).getCurve().getName());
    for (int looptime = 0; looptime < nodeTimesDisc.length; looptime++) {
      assertEquals("Sensitivity cash pv to dsc curve: time", nodeTimesDisc[looptime], sensiPvDisc.get(looptime).getFirst(), 1E-8);
      assertEquals("Sensitivity cash pv to dsc curve: value", sensiDiscMethod[looptime], sensiPvDisc.get(looptime).second, deltaTolerancePrice);
    }
  }

  @Test
  /**
   * Compare the present value curve sensitivity from the method and from the standard calculator.
   */
  public void presentValueCurveSensitivityMethodVsCalculator() {
    PresentValueCurveSensitivityMarket pvcsMethod = METHOD_DEPOSIT.presentValueCurveSensitivity(DEPOSIT, MARKET);
    PresentValueCurveSensitivityMarket pvcsCalculator = PVCSC.visit(DEPOSIT, MARKET);
    assertEquals("Sensitivity cash pv to curve", pvcsMethod, pvcsCalculator);
  }

  @Test
  /**
   * Tests the par rate.
   */
  public void parRate() {
    double rateMethod = METHOD_DEPOSIT.parRate(DEPOSIT, MARKET);
    double dfStart = MARKET.getDiscountFactor(EUR, DEPOSIT.getStartTime());
    double dfEnd = MARKET.getDiscountFactor(EUR, DEPOSIT.getEndTime());
    double rateExpected = (dfStart / dfEnd - 1) / DEPOSIT.getAccrualFactor();
    assertEquals("Cash: rate by discounting", rateExpected, rateMethod, TOLERANCE_RATE);
  }

  @Test
  /**
   * Tests the par rate.
   */
  public void marketQuote() {
    double rateMethod = METHOD_DEPOSIT.parRate(DEPOSIT, MARKET);
    double marketQuote = MQC.visit(DEPOSIT, MARKET);
    assertEquals("Cash: rate by discounting", rateMethod, marketQuote, TOLERANCE_RATE);
  }

}
