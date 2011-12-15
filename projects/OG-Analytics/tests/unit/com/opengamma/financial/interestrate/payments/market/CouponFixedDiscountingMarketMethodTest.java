/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.payments.market;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;

import javax.time.calendar.Period;
import javax.time.calendar.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.daycount.DayCount;
import com.opengamma.financial.instrument.index.IborIndex;
import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.instrument.payment.CouponFixedDefinition;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.MarketDataSets;
import com.opengamma.financial.interestrate.market.PresentValueCurveSensitivityMarket;
import com.opengamma.financial.interestrate.market.PresentValueCurveSensitivityMarketCalculator;
import com.opengamma.financial.interestrate.market.PresentValueMarketCalculator;
import com.opengamma.financial.interestrate.method.market.SensitivityFiniteDifferenceMarket;
import com.opengamma.financial.interestrate.payments.CouponFixed;
import com.opengamma.financial.schedule.ScheduleCalculator;
import com.opengamma.math.differentiation.FiniteDifferenceType;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.tuple.DoublesPair;

/**
 * Tests related to the pricing and sensitivities of fixed coupon in the discounting method.
 */
public class CouponFixedDiscountingMarketMethodTest {

  private static final MarketBundle MARKET = MarketDataSets.createMarket1();
  private static final IndexDeposit[] INDEXES = MarketDataSets.getDepositIndexes();
  private static final IborIndex EURIBOR3M = (IborIndex) INDEXES[0];
  private static final Calendar CALENDAR_EUR = EURIBOR3M.getCalendar();
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

  private static final CouponFixedDefinition COUPON_DEFINITION = new CouponFixedDefinition(EUR, PAYMENT_DATE, SPOT_DATE, PAYMENT_DATE, ACCRUAL_FACTOR_PAYMENT, NOTIONAL, RATE);
  private static final CouponFixed COUPON = COUPON_DEFINITION.toDerivative(REFERENCE_DATE, "Not used");

  private static final CouponFixedDiscountingMarketMethod METHOD = CouponFixedDiscountingMarketMethod.getInstance();
  private static final PresentValueMarketCalculator PVC = PresentValueMarketCalculator.getInstance();
  private static final PresentValueCurveSensitivityMarketCalculator PVCSC = PresentValueCurveSensitivityMarketCalculator.getInstance();

  @Test
  /**
   * Tests the present value.
   */
  public void presentValue() {
    MultipleCurrencyAmount pv = METHOD.presentValue(COUPON, MARKET);
    double df = MARKET.getCurve(EUR).getDiscountFactor(COUPON.getPaymentTime());
    double pvExpected = COUPON.getAmount() * df;
    assertEquals("Coupon Fixed: pv by discounting", pvExpected, pv.getAmount(EUR), 1.0E-2);
  }

  @Test
  /**
   * Compares the present value from the method and the one from the calculator.
   */
  public void presentValueMethodVsCalculator() {
    MultipleCurrencyAmount pvMethod = METHOD.presentValue(COUPON, MARKET);
    MultipleCurrencyAmount pvCalculator = PVC.visit(COUPON, MARKET);
    assertEquals("Coupon Fixed: pv by discounting", pvMethod.size(), pvCalculator.size());
    assertEquals("Coupon Fixed: pv by discounting", pvMethod.getAmount(EUR), pvCalculator.getAmount(EUR), 1.0E-2);
  }

  @Test
  /**
   * Tests the present value curve sensitivity.
   */
  public void presentValueCurveSensitivity() {
    PresentValueCurveSensitivityMarket pvcs = METHOD.presentValueCurveSensitivity(COUPON, MARKET);
    final double deltaTolerancePrice = 1.0E+0;
    // Testing note: Sensitivity is for a movement of 1. 1E+2 = 1 cent for a 1 bp move.
    final double deltaShift = 1.0E-6;
    // Discounting curve sensitivity
    final double[] nodeTimesDisc = new double[] {COUPON.getPaymentTime()};
    final double[] sensiDiscMethod = SensitivityFiniteDifferenceMarket.curveSensitivity(COUPON, MARKET, COUPON.getCurrency(), nodeTimesDisc, deltaShift, METHOD, FiniteDifferenceType.CENTRAL);
    assertEquals("Sensitivity finite difference method: number of node", 1, sensiDiscMethod.length);
    final List<DoublesPair> sensiPvDisc = pvcs.getYieldCurveSensitivities().get(MARKET.getCurve(COUPON.getCurrency()).getCurve().getName());
    assertEquals("Sensitivity fixed coupon pv to dsc curve", nodeTimesDisc[0], sensiPvDisc.get(0).getFirst(), 1E-8);
    assertEquals("Sensitivity finite difference method: node sensitivity", sensiPvDisc.get(0).second, sensiDiscMethod[0], deltaTolerancePrice);
  }

  @Test
  /**
   * Compare the present value curve sensitivity from the method and from the standard calculator.
   */
  public void presentValueCurveSensitivityMethodVsCalculator() {
    PresentValueCurveSensitivityMarket pvcsMethod = METHOD.presentValueCurveSensitivity(COUPON, MARKET);
    PresentValueCurveSensitivityMarket pvcsCalculator = PVCSC.visit(COUPON, MARKET);
    assertEquals("Sensitivity cash pv to curve", pvcsMethod, pvcsCalculator);
  }

}
