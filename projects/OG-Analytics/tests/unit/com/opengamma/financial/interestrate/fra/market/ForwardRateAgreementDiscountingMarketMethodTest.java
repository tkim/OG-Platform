/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.fra.market;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;

import javax.time.calendar.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.financial.convention.daycount.DayCount;
import com.opengamma.financial.convention.daycount.DayCountFactory;
import com.opengamma.financial.instrument.fra.ForwardRateAgreementDefinition;
import com.opengamma.financial.instrument.index.IborIndex;
import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.interestrate.fra.ForwardRateAgreement;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.MarketDataSets;
import com.opengamma.financial.interestrate.market.PresentValueCurveSensitivityMarket;
import com.opengamma.financial.interestrate.market.PresentValueMarketCalculator;
import com.opengamma.financial.interestrate.method.market.SensitivityFiniteDifferenceMarket;
import com.opengamma.math.differentiation.FiniteDifferenceType;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.tuple.DoublesPair;

/**
 * Tests the ForwardRateAgreement discounting method.
 */
public class ForwardRateAgreementDiscountingMarketMethodTest {
  private static final MarketBundle MARKET = MarketDataSets.createMarket1();
  private static final IndexDeposit[] INDEXES = MarketDataSets.getDepositIndexes();
  private static final IborIndex EURIBOR3M = (IborIndex) INDEXES[0];
  // Dates : The above dates are not standard but selected for insure correct testing.
  private static final ZonedDateTime FIXING_DATE = DateUtils.getUTCDate(2011, 1, 3);
  private static final ZonedDateTime ACCRUAL_START_DATE = DateUtils.getUTCDate(2011, 1, 6);
  private static final ZonedDateTime ACCRUAL_END_DATE = DateUtils.getUTCDate(2011, 4, 4);
  private static final ZonedDateTime PAYMENT_DATE = DateUtils.getUTCDate(2011, 1, 7);
  private static final DayCount DAY_COUNT_PAYMENT = DayCountFactory.INSTANCE.getDayCount("Actual/365");
  private static final double ACCRUAL_FACTOR_PAYMENT = DAY_COUNT_PAYMENT.getDayCountFraction(ACCRUAL_START_DATE, ACCRUAL_END_DATE);
  private static final double FRA_RATE = 0.05;
  private static final double NOTIONAL = 1000000; //1m
  // Coupon with specific payment and accrual dates.
  private static final ForwardRateAgreementDefinition FRA_DEFINITION = new ForwardRateAgreementDefinition(EURIBOR3M.getCurrency(), PAYMENT_DATE, ACCRUAL_START_DATE, ACCRUAL_END_DATE, ACCRUAL_FACTOR_PAYMENT, NOTIONAL,
      FIXING_DATE,EURIBOR3M, FRA_RATE);
  // To derivatives
  private static final ZonedDateTime REFERENCE_DATE = DateUtils.getUTCDate(2010, 10, 9);
  private static final String[] NOT_USED = new String[]{"Not used", "Not used"};
  private static final ForwardRateAgreement FRA = (ForwardRateAgreement) FRA_DEFINITION.toDerivative(REFERENCE_DATE, NOT_USED);

  private static final ForwardRateAgreementDiscountingMarketMethod METHOD_FRA = ForwardRateAgreementDiscountingMarketMethod.getInstance();
  private static final PresentValueMarketCalculator PVC = PresentValueMarketCalculator.getInstance();

  @Test
  public void presentValue() {
    final double forward = MARKET.getForwardRate(EURIBOR3M, FRA.getFixingPeriodStartTime(), FRA.getFixingPeriodEndTime(), FRA.getFixingYearFraction());
    final double dfSettle = MARKET.getDiscountingFactor(FRA.getCurrency(), FRA.getPaymentTime());
    final double expectedPv = FRA.getNotional() * dfSettle * FRA.getPaymentYearFraction() * (forward - FRA_RATE) / (1 + FRA.getPaymentYearFraction() * forward);
    final CurrencyAmount pv = METHOD_FRA.presentValue(FRA, MARKET);
    assertEquals("FRA discounting: present value", expectedPv, pv.getAmount(), 1.0E-2);
  }

  @Test
  public void presentValueMethodVsCalculator() {
    CurrencyAmount pvMethod = METHOD_FRA.presentValue(FRA, MARKET);
    CurrencyAmount pvCalculator = PVC.visit(FRA, MARKET);
    assertEquals("Coupon Fixed: pv by discounting", pvMethod.getCurrency(), pvCalculator.getCurrency());
    assertEquals("Coupon Fixed: pv by discounting", pvMethod.getAmount(), pvCalculator.getAmount(), 1.0E-2);
  }

  @Test
  public void presentValueBuySellParity() {
    final ForwardRateAgreementDefinition fraDefinitionSell = new ForwardRateAgreementDefinition(EURIBOR3M.getCurrency(), PAYMENT_DATE, ACCRUAL_START_DATE, ACCRUAL_END_DATE, ACCRUAL_FACTOR_PAYMENT, -NOTIONAL,
        FIXING_DATE, EURIBOR3M, FRA_RATE);
    final ForwardRateAgreement fraSell = (ForwardRateAgreement) fraDefinitionSell.toDerivative(REFERENCE_DATE, NOT_USED);
    final CurrencyAmount pvBuy = METHOD_FRA.presentValue(FRA, MARKET);
    final CurrencyAmount pvSell = METHOD_FRA.presentValue(fraSell, MARKET);
    assertEquals("FRA discounting: present value - buy/sell parity", pvSell.getAmount(), -pvBuy.getAmount(), 1.0E-2);
  }

  //  @Test
  //  public void presentValueMethodVsCalculator() {
  //    final CurrencyAmount pvMethod = FRA_METHOD.presentValue(FRA, MARKET);
  //    final PresentValueCalculator calculator = PresentValueCalculator.getInstance();
  //    final double pvCalculator = calculator.visit(FRA, curves);
  //    assertEquals("FRA discounting: present value calculator vs method", pvCalculator, pvMethod.getAmount(), 1.0E-2);
  //  }

  @Test
  public void presentValueCurveSensitivity() {
    final PresentValueCurveSensitivityMarket pvcs = METHOD_FRA.presentValueCurveSensitivity(FRA, MARKET);
    pvcs.clean();
    final double deltaTolerancePrice = 1.0E+1;
    //Testing note: Sensitivity is for a movement of 1. 1E+2 = 1 cent for a 1 bp move. Tolerance increased to cope with numerical imprecision of finite difference.
    final double deltaShift = 1.0E-6;
    // 1. Forward curve sensitivity
    final double[] nodeTimesForward = new double[] {FRA.getFixingPeriodStartTime(), FRA.getFixingPeriodEndTime()};
    final double[] sensiForwardMethod = SensitivityFiniteDifferenceMarket.curveSensitivity(FRA, MARKET, EURIBOR3M, nodeTimesForward, deltaShift, METHOD_FRA, FiniteDifferenceType.CENTRAL);
    assertEquals("Sensitivity finite difference method: number of node", 2, sensiForwardMethod.length);
    final List<DoublesPair> sensiPvForward = pvcs.getYieldCurveSensitivities().get(MARKET.getCurve(EURIBOR3M).getCurve().getName());
    for (int loopnode = 0; loopnode < sensiForwardMethod.length; loopnode++) {
      final DoublesPair pairPv = sensiPvForward.get(loopnode);
      assertEquals("Sensitivity coupon pv to forward curve: Node " + loopnode, nodeTimesForward[loopnode], pairPv.getFirst(), 1E-8);
      assertEquals("Sensitivity finite difference method: node sensitivity", pairPv.second, sensiForwardMethod[loopnode], deltaTolerancePrice);
    }
    // 2. Discounting curve sensitivity
    final double[] nodeTimesDisc = new double[] {FRA.getPaymentTime()};
    final double[] sensiDiscMethod = SensitivityFiniteDifferenceMarket.curveSensitivity(FRA, MARKET, FRA.getCurrency(), nodeTimesDisc, deltaShift, METHOD_FRA, FiniteDifferenceType.CENTRAL);
    assertEquals("Sensitivity finite difference method: number of node", 1, sensiDiscMethod.length);
    final List<DoublesPair> sensiPvDisc = pvcs.getYieldCurveSensitivities().get(MARKET.getCurve(FRA.getCurrency()).getCurve().getName());
    for (int loopnode = 0; loopnode < sensiDiscMethod.length; loopnode++) {
      final DoublesPair pairPv = sensiPvDisc.get(loopnode);
      assertEquals("Sensitivity coupon pv to forward curve: Node " + loopnode, nodeTimesDisc[loopnode], pairPv.getFirst(), 1E-8);
      assertEquals("Sensitivity finite difference method: node sensitivity", pairPv.second, sensiDiscMethod[loopnode], deltaTolerancePrice);
    }

  }

  //  @Test
  //  public void presentValueSensitivityMethodVsCalculator() {
  //    final YieldCurveBundle curves = TestsDataSets.createCurves1();
  //    final InterestRateCurveSensitivity pvcsMethod = FRA_METHOD.presentValueCurveSensitivity(FRA, curves);
  //    final PresentValueCurveSensitivityCalculator calculator = PresentValueCurveSensitivityCalculator.getInstance();
  //    final Map<String, List<DoublesPair>> pvcsCalculator = calculator.visit(FRA, curves);
  //    assertEquals("FRA discounting: present value calculator vs method", pvcsCalculator, pvcsMethod.getSensitivities());
  //  }


  @Test
  public void parRate() {
    final double forward = METHOD_FRA.parRate(FRA, MARKET);
    final double forwardExpected = MARKET.getForwardRate(EURIBOR3M, FRA.getFixingPeriodStartTime(), FRA.getFixingPeriodEndTime(), FRA.getFixingYearFraction());
    assertEquals("FRA discounting: par rate", forwardExpected, forward, 1.0E-10);
  }

  // TODO: par rate sensitivity

}
