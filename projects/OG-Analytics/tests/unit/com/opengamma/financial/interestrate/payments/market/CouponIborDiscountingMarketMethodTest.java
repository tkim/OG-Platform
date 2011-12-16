/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.payments.market;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;

import javax.time.calendar.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.daycount.DayCount;
import com.opengamma.financial.convention.daycount.DayCountFactory;
import com.opengamma.financial.instrument.index.IborIndex;
import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.instrument.payment.CouponIborSpreadDefinition;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.MarketDataSets;
import com.opengamma.financial.interestrate.market.PresentValueCurveSensitivityMarket;
import com.opengamma.financial.interestrate.market.PresentValueCurveSensitivityMarketCalculator;
import com.opengamma.financial.interestrate.market.PresentValueMarketCalculator;
import com.opengamma.financial.interestrate.method.market.SensitivityFiniteDifferenceMarket;
import com.opengamma.financial.interestrate.payments.CouponIbor;
import com.opengamma.financial.schedule.ScheduleCalculator;
import com.opengamma.math.differentiation.FiniteDifferenceType;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.tuple.DoublesPair;

/**
 * Tests related to the pricing and sensitivities of Ibor coupon with gearing factor and spread in the discounting method.
 */
public class CouponIborDiscountingMarketMethodTest {
  private static final MarketBundle MARKET = MarketDataSets.createMarket1();
  private static final IndexDeposit[] INDEXES = MarketDataSets.getDepositIndexes();
  private static final IborIndex EURIBOR3M = (IborIndex) INDEXES[0];
  private static final Calendar CALENDAR_EUR = EURIBOR3M.getCalendar();
  private static final Currency EUR = EURIBOR3M.getCurrency();
  private static final DayCount DAY_COUNT_COUPON = DayCountFactory.INSTANCE.getDayCount("Actual/365");
  private static final ZonedDateTime ACCRUAL_START_DATE = DateUtils.getUTCDate(2011, 5, 23);
  private static final ZonedDateTime ACCRUAL_END_DATE = DateUtils.getUTCDate(2011, 8, 22);
  private static final double ACCRUAL_FACTOR = DAY_COUNT_COUPON.getDayCountFraction(ACCRUAL_START_DATE, ACCRUAL_END_DATE);
  private static final double NOTIONAL = 100000000; //100 m
  private static final double SPREAD = 0.0050;
  private static final ZonedDateTime FIXING_DATE = ScheduleCalculator.getAdjustedDate(ACCRUAL_START_DATE, -EURIBOR3M.getSpotLag(), CALENDAR_EUR);
  private static final CouponIborSpreadDefinition COUPON_DEFINITION = new CouponIborSpreadDefinition(EURIBOR3M.getCurrency(), ACCRUAL_END_DATE, ACCRUAL_START_DATE, ACCRUAL_END_DATE, ACCRUAL_FACTOR,
      NOTIONAL, FIXING_DATE, EURIBOR3M, SPREAD);
  private static final ZonedDateTime REFERENCE_DATE = DateUtils.getUTCDate(2010, 12, 27);
  private static final CouponIbor COUPON = (CouponIbor) COUPON_DEFINITION.toDerivative(REFERENCE_DATE, new String[] {"Not used", "Not used"});

  private static final CouponIborDiscountingMarketMethod METHOD = CouponIborDiscountingMarketMethod.getInstance();
  private static final PresentValueMarketCalculator PVC = PresentValueMarketCalculator.getInstance();
  private static final PresentValueCurveSensitivityMarketCalculator PVCSC = PresentValueCurveSensitivityMarketCalculator.getInstance();

  @Test
  /**
   * Tests the present value.
   */
  public void presentValue() {
    MultipleCurrencyAmount pv = METHOD.presentValue(COUPON, MARKET);
    double df = MARKET.getDiscountFactor(COUPON.getCurrency(), COUPON.getPaymentTime());
    double forward = MARKET.getForwardRate(EURIBOR3M, COUPON.getFixingPeriodStartTime(), COUPON.getFixingPeriodEndTime(), COUPON.getFixingYearFraction());
    double pvExpected = (forward + SPREAD) * COUPON.getPaymentYearFraction() * COUPON.getNotional() * df;
    assertEquals("Coupon Ibor Gearing: Present value by discounting", pvExpected, pv.getAmount(EUR), 1.0E-2);
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
   * Test the present value curves sensitivity.
   */
  public void presentValueCurveSensitivity() {
    final PresentValueCurveSensitivityMarket pvcs = METHOD.presentValueCurveSensitivity(COUPON, MARKET);
    pvcs.clean();
    final double deltaTolerancePrice = 1.0E+1;
    //Testing note: Sensitivity is for a movement of 1. 1E+2 = 1 cent for a 1 bp move. Tolerance increased to cope with numerical imprecision of finite difference.
    final double deltaShift = 1.0E-6;
    // 1. Forward curve sensitivity
    final double[] nodeTimesForward = new double[] {COUPON.getFixingPeriodStartTime(), COUPON.getFixingPeriodEndTime()};
    final double[] sensiForwardMethod = SensitivityFiniteDifferenceMarket.curveSensitivity(COUPON, MARKET, EURIBOR3M, nodeTimesForward, deltaShift, METHOD, FiniteDifferenceType.CENTRAL);
    assertEquals("Sensitivity finite difference method: number of node", 2, sensiForwardMethod.length);
    final List<DoublesPair> sensiPvForward = pvcs.getYieldCurveSensitivities().get(MARKET.getCurve(EURIBOR3M).getCurve().getName());
    for (int loopnode = 0; loopnode < sensiForwardMethod.length; loopnode++) {
      final DoublesPair pairPv = sensiPvForward.get(loopnode);
      assertEquals("Sensitivity coupon pv to forward curve: Node " + loopnode, nodeTimesForward[loopnode], pairPv.getFirst(), 1E-8);
      assertEquals("Sensitivity finite difference method: node sensitivity", pairPv.second, sensiForwardMethod[loopnode], deltaTolerancePrice);
    }
    // 2. Discounting curve sensitivity
    final double[] nodeTimesDisc = new double[] {COUPON.getPaymentTime()};
    final double[] sensiDiscMethod = SensitivityFiniteDifferenceMarket.curveSensitivity(COUPON, MARKET, COUPON.getCurrency(), nodeTimesDisc, deltaShift, METHOD, FiniteDifferenceType.CENTRAL);
    assertEquals("Sensitivity finite difference method: number of node", 1, sensiDiscMethod.length);
    final List<DoublesPair> sensiPvDisc = pvcs.getYieldCurveSensitivities().get(MARKET.getCurve(COUPON.getCurrency()).getCurve().getName());
    for (int loopnode = 0; loopnode < sensiDiscMethod.length; loopnode++) {
      final DoublesPair pairPv = sensiPvDisc.get(loopnode);
      assertEquals("Sensitivity coupon pv to forward curve: Node " + loopnode, nodeTimesDisc[loopnode], pairPv.getFirst(), 1E-8);
      assertEquals("Sensitivity finite difference method: node sensitivity", pairPv.second, sensiDiscMethod[loopnode], deltaTolerancePrice);
    }
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
