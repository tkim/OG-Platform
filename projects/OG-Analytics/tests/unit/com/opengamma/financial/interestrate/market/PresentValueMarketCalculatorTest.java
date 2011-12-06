/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market;

import static org.testng.AssertJUnit.assertEquals;

import javax.time.calendar.Period;
import javax.time.calendar.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.financial.convention.businessday.BusinessDayConvention;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.daycount.DayCount;
import com.opengamma.financial.convention.daycount.DayCountFactory;
import com.opengamma.financial.instrument.annuity.AnnuityCouponFixedDefinition;
import com.opengamma.financial.instrument.annuity.AnnuityCouponIborDefinition;
import com.opengamma.financial.instrument.annuity.AnnuityDefinition;
import com.opengamma.financial.instrument.index.IborIndex;
import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.instrument.index.IndexON;
import com.opengamma.financial.instrument.index.IndexSwap;
import com.opengamma.financial.instrument.payment.CouponDefinition;
import com.opengamma.financial.instrument.payment.CouponFixedDefinition;
import com.opengamma.financial.instrument.payment.CouponIborDefinition;
import com.opengamma.financial.instrument.payment.PaymentDefinition;
import com.opengamma.financial.instrument.swap.SwapDefinition;
import com.opengamma.financial.instrument.swap.SwapFixedIborDefinition;
import com.opengamma.financial.instrument.swap.SwapFixedOISSimplifiedDefinition;
import com.opengamma.financial.interestrate.annuity.definition.AnnuityCouponFixed;
import com.opengamma.financial.interestrate.annuity.definition.GenericAnnuity;
import com.opengamma.financial.interestrate.payments.Coupon;
import com.opengamma.financial.interestrate.payments.Payment;
import com.opengamma.financial.interestrate.payments.market.CouponFixedDiscountingMarketMethod;
import com.opengamma.financial.interestrate.payments.market.CouponIborDiscountingMarketMethod;
import com.opengamma.financial.interestrate.swap.definition.FixedCouponSwap;
import com.opengamma.financial.interestrate.swap.definition.Swap;
import com.opengamma.financial.schedule.ScheduleCalculator;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.timeseries.zoneddatetime.ArrayZonedDateTimeDoubleTimeSeries;

/**
 * Tests related to the PresentValueMarket calculator (present value of linear instruments by discounting).
 */
public class PresentValueMarketCalculatorTest {

  private static final MarketBundle MARKET = MarketDataSets.createMarket1();
  private static final IndexDeposit[] INDEXES = MarketDataSets.getDepositIndexes();
  private static final IborIndex EURIBOR3M = (IborIndex) INDEXES[0];
  private static final IborIndex EURIBOR6M = (IborIndex) INDEXES[1];
  private static final IndexON EONIA = (IndexON) INDEXES[2];
  private static final Calendar CALENDAR_EUR = EURIBOR6M.getCalendar();
  private static final int SETTLEMENT_DAYS = EURIBOR6M.getSettlementDays();
  private static final BusinessDayConvention BUSINESS_DAY = EURIBOR6M.getBusinessDayConvention();
  private static final Boolean EOM = EURIBOR6M.isEndOfMonth();
  private static final Currency EUR = EURIBOR6M.getCurrency();

  private static final ZonedDateTime TRADE_DATE = DateUtils.getUTCDate(2011, 10, 13);
  private static final ZonedDateTime SPOT_DATE = ScheduleCalculator.getAdjustedDate(TRADE_DATE, CALENDAR_EUR, SETTLEMENT_DAYS);
  private static final ZonedDateTime REFERENCE_DATE = DateUtils.getUTCDate(2011, 11, 8);
  private static final Period TENOR_ANNUITY = Period.ofYears(10);
  private static final Period PERIOD_FIXED = Period.ofYears(10);
  private static final DayCount DAY_COUNT_FIXED = DayCountFactory.INSTANCE.getDayCount("30/360");

  private static final double NOTIONAL = 100000000;
  private static final double FIXED_RATE = 0.0225;

  private static final AnnuityCouponFixedDefinition ANNUITY_FIXED_DEFINITION = AnnuityCouponFixedDefinition.from(EUR, SPOT_DATE, TENOR_ANNUITY, PERIOD_FIXED, CALENDAR_EUR, DAY_COUNT_FIXED,
      BUSINESS_DAY, EOM, NOTIONAL, FIXED_RATE, true);
  private static final AnnuityCouponFixed ANNUITY_FIXED = ANNUITY_FIXED_DEFINITION.toDerivative(REFERENCE_DATE, "Not used");

  private static final AnnuityCouponIborDefinition ANNUITY_IBOR_DEFINITION = AnnuityCouponIborDefinition.from(SPOT_DATE, TENOR_ANNUITY, NOTIONAL, EURIBOR6M, false);
  private static final ArrayZonedDateTimeDoubleTimeSeries FIXING_TS = new ArrayZonedDateTimeDoubleTimeSeries(new ZonedDateTime[] {TRADE_DATE}, new double[] {0.01});
  private static final String[] NOT_USED_2 = new String[] {"Not used", "Not used"};
  private static final GenericAnnuity<? extends Coupon> ANNUITY_IBOR = ANNUITY_IBOR_DEFINITION.toDerivative(REFERENCE_DATE, FIXING_TS, NOT_USED_2);

  private static final IndexSwap CMS_INDEX = new IndexSwap(PERIOD_FIXED, DAY_COUNT_FIXED, EURIBOR6M, TENOR_ANNUITY);
  private static final SwapFixedIborDefinition SWAP_FIXED_IBOR_DEFINITION = SwapFixedIborDefinition.from(SPOT_DATE, CMS_INDEX, NOTIONAL, FIXED_RATE, true);
  private static final FixedCouponSwap<Coupon> SWAP_FIXED_IBOR = SWAP_FIXED_IBOR_DEFINITION.toDerivative(REFERENCE_DATE, new ArrayZonedDateTimeDoubleTimeSeries[] {FIXING_TS}, NOT_USED_2);

  private static final SwapFixedOISSimplifiedDefinition SWAP_FIXED_OIS_DEFINITION = SwapFixedOISSimplifiedDefinition.from(
      ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, CALENDAR_EUR, SETTLEMENT_DAYS), Period.ofYears(2), Period.ofYears(1), NOTIONAL, EONIA, FIXED_RATE, true, SETTLEMENT_DAYS, BUSINESS_DAY, EOM);
  private static final Swap<? extends Payment, ? extends Payment> SWAP_FIXED_OIS = SWAP_FIXED_OIS_DEFINITION.toDerivative(REFERENCE_DATE, NOT_USED_2);

  private static final CouponFixedDiscountingMarketMethod METHOD_FIXED = CouponFixedDiscountingMarketMethod.getInstance();
  private static final CouponIborDiscountingMarketMethod METHOD_IBOR = CouponIborDiscountingMarketMethod.getInstance();
  private static final PresentValueMarketCalculator PVC = PresentValueMarketCalculator.getInstance();

  @Test
  /**
   * Tests the present value of an annuity composed of fixed coupons.
   */
  public void presentValueAnnuityFixed() {
    MultipleCurrencyAmount pvCalculator = PVC.visit(ANNUITY_FIXED, MARKET);
    MultipleCurrencyAmount pvExpected = MultipleCurrencyAmount.of(EUR, 0.0);
    for (int loopcpn = 0; loopcpn < ANNUITY_FIXED.getNumberOfPayments(); loopcpn++) {
      pvExpected = pvExpected.plus(METHOD_FIXED.presentValue(ANNUITY_FIXED.getNthPayment(loopcpn), MARKET));
    }
    assertEquals("Annuity Fixed: pv by discounting", pvExpected.size(), pvCalculator.size());
    assertEquals("Annuity Fixed: pv by discounting", pvExpected.getAmount(EUR), pvCalculator.getAmount(EUR), 1.0E-2);
  }

  @Test
  /**
   * Tests the present value of an annuity created from a Ibor annuity (with a coupon already fixed).
   */
  public void presentValueAnnuityIbor() {
    MultipleCurrencyAmount pvCalculator = PVC.visit(ANNUITY_IBOR, MARKET);
    MultipleCurrencyAmount pvExpected = METHOD_FIXED.presentValue(ANNUITY_IBOR.getNthPayment(0), MARKET);
    for (int loopcpn = 1; loopcpn < ANNUITY_IBOR.getNumberOfPayments(); loopcpn++) {
      pvExpected = pvExpected.plus(METHOD_IBOR.presentValue(ANNUITY_IBOR.getNthPayment(loopcpn), MARKET));
    }
    assertEquals("Annuity Ibor: pv by discounting", pvExpected.size(), pvCalculator.size());
    assertEquals("Annuity Ibor: pv by discounting", pvExpected.getAmount(EUR), pvCalculator.getAmount(EUR), 1.0E-2);
  }

  @Test
  /**
   * Tests the present value of an swap created from a Fixed-Ibor swap (with a coupon already fixed on the Ibor leg).
   */
  public void presentValueSwapFixedIbor() {
    MultipleCurrencyAmount pvCalculator = PVC.visit(SWAP_FIXED_IBOR, MARKET);
    MultipleCurrencyAmount pvFixed = PVC.visit(SWAP_FIXED_IBOR.getFixedLeg(), MARKET);
    MultipleCurrencyAmount pvIbor = PVC.visit(SWAP_FIXED_IBOR.getSecondLeg(), MARKET);
    assertEquals("Annuity Ibor: pv by discounting", pvFixed.size(), pvCalculator.size());
    assertEquals("Annuity Ibor: pv by discounting", pvFixed.plus(pvIbor).getAmount(EUR), pvCalculator.getAmount(EUR), 1.0E-2);
  }

  @Test
  /**
   * Tests the present value of an swap created from a Fixed-Ibor swap. The swap has one coupon on Euribor 3M and one on Euribor 6M.
   */
  public void presentValueSwapFixedIborHeterogeneousIbor() {
    CouponIborDefinition cpn3Definition = CouponIborDefinition.from(NOTIONAL, REFERENCE_DATE, EURIBOR3M);
    CouponIborDefinition cpn6Definition = CouponIborDefinition.from(NOTIONAL, ScheduleCalculator.getAdjustedDate(cpn3Definition.getPaymentDate(), CALENDAR_EUR, SETTLEMENT_DAYS), EURIBOR6M);
    CouponFixedDefinition cpnFDefinition = CouponFixedDefinition.from(EUR, cpn6Definition.getPaymentDate(), cpn3Definition.getAccrualStartDate(), cpn6Definition.getAccrualEndDate(),
        DAY_COUNT_FIXED.getDayCountFraction(cpn3Definition.getAccrualStartDate(), cpn6Definition.getAccrualEndDate()), -NOTIONAL, FIXED_RATE);
    AnnuityDefinition<PaymentDefinition> legIborHeterogeneousDefinition = new AnnuityDefinition<PaymentDefinition>(new CouponDefinition[] {cpn3Definition, cpn6Definition});
    AnnuityDefinition<PaymentDefinition> legFixedDefinition = new AnnuityDefinition<PaymentDefinition>(new CouponDefinition[] {cpnFDefinition});
    SwapDefinition swapHeterogeneousDefinition = new SwapDefinition(legFixedDefinition, legIborHeterogeneousDefinition);
    Coupon cpn3 = cpn3Definition.toDerivative(REFERENCE_DATE, NOT_USED_2);
    Coupon cpn6 = cpn6Definition.toDerivative(REFERENCE_DATE, NOT_USED_2);
    Coupon cpnF = cpnFDefinition.toDerivative(REFERENCE_DATE, NOT_USED_2);
    Swap<? extends Payment, ? extends Payment> swapHeterogeneous = swapHeterogeneousDefinition.toDerivative(REFERENCE_DATE, NOT_USED_2);
    MultipleCurrencyAmount pvSwap = PVC.visit(swapHeterogeneous, MARKET);
    MultipleCurrencyAmount pvSum = PVC.visit(cpn3, MARKET);
    pvSum = pvSum.plus(PVC.visit(cpn6, MARKET));
    pvSum = pvSum.plus(PVC.visit(cpnF, MARKET));
    assertEquals("Swap Heterogeneous: pv by discounting", pvSum.size(), pvSwap.size());
    assertEquals("Swap Heterogeneous: pv by discounting", pvSum.getAmount(EUR), pvSwap.getAmount(EUR), 1.0E-2);
  }

  @Test
  /**
   * Tests the present value of a swap created from a Fixed-OIS swap starting in the future.
   */
  public void presentValueSwapOIS() {
    MultipleCurrencyAmount pvCalculator = PVC.visit(SWAP_FIXED_OIS, MARKET);
    MultipleCurrencyAmount pvFixed = PVC.visit(SWAP_FIXED_OIS.getFirstLeg(), MARKET);
    MultipleCurrencyAmount pvIbor = PVC.visit(SWAP_FIXED_OIS.getSecondLeg(), MARKET);
    assertEquals("Annuity Ibor: pv by discounting", pvFixed.size(), pvCalculator.size());
    assertEquals("Annuity Ibor: pv by discounting", pvFixed.plus(pvIbor).getAmount(EUR), pvCalculator.getAmount(EUR), 1.0E-2);
  }

}
