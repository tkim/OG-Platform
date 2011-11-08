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
import com.opengamma.financial.convention.daycount.DayCountFactory;
import com.opengamma.financial.instrument.index.IborIndex;
import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.instrument.index.IndexOIS;
import com.opengamma.financial.instrument.payment.CouponOISDefinition;
import com.opengamma.financial.instrument.payment.CouponOISSimplifiedDefinition;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.MarketDataSets;
import com.opengamma.financial.interestrate.market.PresentValueCurveSensitivityMarket;
import com.opengamma.financial.interestrate.market.PresentValueCurveSensitivityMarketCalculator;
import com.opengamma.financial.interestrate.market.PresentValueMarketCalculator;
import com.opengamma.financial.interestrate.method.market.SensitivityFiniteDifferenceMarket;
import com.opengamma.financial.interestrate.payments.derivative.CouponOIS;
import com.opengamma.financial.schedule.ScheduleCalculator;
import com.opengamma.math.differentiation.FiniteDifferenceType;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.timeseries.DoubleTimeSeries;
import com.opengamma.util.timeseries.zoneddatetime.ArrayZonedDateTimeDoubleTimeSeries;
import com.opengamma.util.tuple.DoublesPair;

/**
 * Tests related to the pricing and sensitivities of Ibor coupon with gearing factor and spread in the discounting method.
 */
public class CouponOISDiscountingMarketMethodTest {

  private static final MarketBundle MARKET = MarketDataSets.createMarket1();
  private static final IndexDeposit[] INDEXES = MarketDataSets.getDepositIndexes();
  private static final IborIndex EURIBOR3M = (IborIndex) INDEXES[0];
  private static final IndexOIS EONIA = (IndexOIS) INDEXES[2];
  private static final Calendar CALENDAR_EUR = EURIBOR3M.getCalendar();
  private static final DayCount DAY_COUNT_COUPON = DayCountFactory.INSTANCE.getDayCount("Actual/365");
  private static final int SETTLEMENT_DAYS = EURIBOR3M.getSettlementDays();
  private static final Currency EUR = EURIBOR3M.getCurrency();

  private static final ZonedDateTime REFERENCE_DATE_1 = DateUtils.getUTCDate(2010, 12, 27);
  private static final ZonedDateTime SPOT_DATE = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE_1, CALENDAR_EUR, SETTLEMENT_DAYS);
  private static final Period TENOR = Period.ofMonths(3);
  private static final ZonedDateTime PAYMENT_DATE = ScheduleCalculator.getAdjustedDate(SPOT_DATE, EURIBOR3M.getBusinessDayConvention(), CALENDAR_EUR, EURIBOR3M.isEndOfMonth(), TENOR);
  private static final double ACCRUAL_FACTOR_PAYMENT = DAY_COUNT_COUPON.getDayCountFraction(SPOT_DATE, PAYMENT_DATE);
  private static final double NOTIONAL = 100000000; // 100m

  private static final CouponOISSimplifiedDefinition COUPON_DEFINITION = new CouponOISSimplifiedDefinition(EUR, PAYMENT_DATE, SPOT_DATE, PAYMENT_DATE, ACCRUAL_FACTOR_PAYMENT, NOTIONAL, EONIA,
      SPOT_DATE, PAYMENT_DATE, ACCRUAL_FACTOR_PAYMENT);
  private static final CouponOIS COUPON = COUPON_DEFINITION.toDerivative(REFERENCE_DATE_1, new String[] {"Not used", "Not used"});

  private static final ZonedDateTime REFERENCE_DATE_2 = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE_1, CALENDAR_EUR, 5);
  private static final CouponOISDefinition EONIA_COUPON_2_DEFINITION = new CouponOISDefinition(EUR, PAYMENT_DATE, SPOT_DATE, PAYMENT_DATE, ACCRUAL_FACTOR_PAYMENT, NOTIONAL, EONIA, SPOT_DATE,
      PAYMENT_DATE);

  private static final DoubleTimeSeries<ZonedDateTime> FIXING_TS = new ArrayZonedDateTimeDoubleTimeSeries(new ZonedDateTime[] {ScheduleCalculator.getAdjustedDate(REFERENCE_DATE_1, CALENDAR_EUR, 1),
      ScheduleCalculator.getAdjustedDate(REFERENCE_DATE_1, CALENDAR_EUR, 2), ScheduleCalculator.getAdjustedDate(REFERENCE_DATE_1, CALENDAR_EUR, 3),
      ScheduleCalculator.getAdjustedDate(REFERENCE_DATE_1, CALENDAR_EUR, 4), ScheduleCalculator.getAdjustedDate(REFERENCE_DATE_1, CALENDAR_EUR, 5)}, new double[] {0.01, 0.011, 0.012, 0.013, 0.014});
  private static final CouponOIS EONIA_COUPON_2 = (CouponOIS) EONIA_COUPON_2_DEFINITION.toDerivative(REFERENCE_DATE_2, FIXING_TS, new String[] {"Not used", "Not used"});

  private static final CouponOISDiscountingMarketMethod METHOD = CouponOISDiscountingMarketMethod.getInstance();
  private static final PresentValueMarketCalculator PVC = PresentValueMarketCalculator.getInstance();
  private static final PresentValueCurveSensitivityMarketCalculator PVCSC = PresentValueCurveSensitivityMarketCalculator.getInstance();

  @Test
  /**
   * Tests the present value for a coupon where the fixing period has not started yet.
   */
  public void presentValueNotStarted() {
    CurrencyAmount pv = METHOD.presentValue(COUPON, MARKET);
    double df = MARKET.getDiscountingFactor(COUPON.getCurrency(), COUPON.getPaymentTime());
    final double dfForwardStart = MARKET.getCurve(EONIA).getDiscountFactor(COUPON.getFixingPeriodStartTime());
    final double dfForwardEnd = MARKET.getCurve(EONIA).getDiscountFactor(COUPON.getFixingPeriodEndTime());
    final double accruedFwd = dfForwardStart / dfForwardEnd;
    final double pvExpected = (COUPON.getNotionalAccrued() * accruedFwd - COUPON.getNotional()) * df;
    assertEquals("Coupon OIS: Present value by discounting", pvExpected, pv.getAmount(), 1.0E-2);
  }

  @Test
  /**
   * Compare the present value from the method and the one from the calculator for a coupon where the fixing period has not started yet.
   */
  public void presentValueNotStartedMethodVsCalculator() {
    CurrencyAmount pvMethod = METHOD.presentValue(COUPON, MARKET);
    CurrencyAmount pvCalculator = PVC.visit(COUPON, MARKET);
    assertEquals("Coupon Fixed: pv by discounting", pvMethod.getCurrency(), pvCalculator.getCurrency());
    assertEquals("Coupon Fixed: pv by discounting", pvMethod.getAmount(), pvCalculator.getAmount(), 1.0E-2);
  }

  @Test
  /**
   * Tests the present value for a coupon where the fixing period has started already.
   */
  public void presentValueStarted() {
    CurrencyAmount pv = METHOD.presentValue(EONIA_COUPON_2, MARKET);
    double df = MARKET.getDiscountingFactor(EONIA_COUPON_2.getCurrency(), EONIA_COUPON_2.getPaymentTime());
    final double dfForwardStart = MARKET.getCurve(EONIA).getDiscountFactor(EONIA_COUPON_2.getFixingPeriodStartTime());
    final double dfForwardEnd = MARKET.getCurve(EONIA).getDiscountFactor(EONIA_COUPON_2.getFixingPeriodEndTime());
    final double accruedFwd = dfForwardStart / dfForwardEnd;
    final double pvExpected = (EONIA_COUPON_2.getNotionalAccrued() * accruedFwd - EONIA_COUPON_2.getNotional()) * df;
    assertEquals("Coupon OIS: Present value by discounting", pvExpected, pv.getAmount(), 1.0E-2);
  }

  @Test
  /**
   * Compare the present value from the method and the one from the calculator for a coupon where the fixing period has started already.
   */
  public void presentValueStartedMethodVsCalculator() {
    CurrencyAmount pvMethod = METHOD.presentValue(EONIA_COUPON_2, MARKET);
    CurrencyAmount pvCalculator = PVC.visit(EONIA_COUPON_2, MARKET);
    assertEquals("Coupon Fixed: pv by discounting", pvMethod.getCurrency(), pvCalculator.getCurrency());
    assertEquals("Coupon Fixed: pv by discounting", pvMethod.getAmount(), pvCalculator.getAmount(), 1.0E-2);
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
    final double[] sensiForwardMethod = SensitivityFiniteDifferenceMarket.curveSensitivity(COUPON, MARKET, EONIA, nodeTimesForward, deltaShift, METHOD, FiniteDifferenceType.CENTRAL);
    assertEquals("Sensitivity finite difference method: number of node", 2, sensiForwardMethod.length);
    final List<DoublesPair> sensiPvForward = pvcs.getYieldCurveSensitivities().get(MARKET.getCurve(EONIA).getCurve().getName());
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
