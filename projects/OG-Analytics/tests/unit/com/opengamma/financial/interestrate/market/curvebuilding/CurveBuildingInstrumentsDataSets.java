/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market.curvebuilding;

import javax.time.calendar.DateAdjusters;
import javax.time.calendar.DayOfWeek;
import javax.time.calendar.Period;
import javax.time.calendar.ZonedDateTime;

import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.financial.convention.daycount.DayCountFactory;
import com.opengamma.financial.instrument.Convention;
import com.opengamma.financial.instrument.annuity.AnnuityCouponFixedDefinition;
import com.opengamma.financial.instrument.annuity.AnnuityCouponIborDefinition;
import com.opengamma.financial.instrument.cash.CashDefinition;
import com.opengamma.financial.instrument.fra.ForwardRateAgreementDefinition;
import com.opengamma.financial.instrument.future.InterestRateFutureDefinition;
import com.opengamma.financial.instrument.index.CMSIndex;
import com.opengamma.financial.instrument.index.IborIndex;
import com.opengamma.financial.instrument.index.IndexOIS;
import com.opengamma.financial.instrument.index.PriceIndex;
import com.opengamma.financial.instrument.index.SwapGenerator;
import com.opengamma.financial.instrument.index.iborindex.Eonia;
import com.opengamma.financial.instrument.index.iborindex.Euribor3M;
import com.opengamma.financial.instrument.index.iborindex.Euribor6M;
import com.opengamma.financial.instrument.index.priceindex.EurolandHicpXT;
import com.opengamma.financial.instrument.payment.CouponFixedDefinition;
import com.opengamma.financial.instrument.payment.CouponIborDefinition;
import com.opengamma.financial.instrument.swap.SwapFixedIborDefinition;
import com.opengamma.financial.instrument.swap.SwapFixedInflationZeroCouponDefinition;
import com.opengamma.financial.instrument.swap.SwapFixedOISSimplifiedDefinition;
import com.opengamma.financial.interestrate.InterestRateDerivative;
import com.opengamma.financial.interestrate.annuity.definition.GenericAnnuity;
import com.opengamma.financial.interestrate.cash.definition.Cash;
import com.opengamma.financial.interestrate.fra.ForwardRateAgreement;
import com.opengamma.financial.interestrate.inflation.derivatives.CouponInflationZeroCouponInterpolation;
import com.opengamma.financial.interestrate.market.MarketDataSets;
import com.opengamma.financial.interestrate.payments.Coupon;
import com.opengamma.financial.interestrate.swap.definition.Swap;
import com.opengamma.financial.schedule.ScheduleCalculator;
import com.opengamma.util.money.Currency;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.timeseries.zoneddatetime.ArrayZonedDateTimeDoubleTimeSeries;

/**
 * To create different instrument sets for curve construction testing.
 */
public class CurveBuildingInstrumentsDataSets {

  private static final Calendar CALENDAR_EUR = new MondayToFridayCalendar("EUR");
  private static final IborIndex EURIBOR_3M = new Euribor3M(CALENDAR_EUR);
  private static final IborIndex EURIBOR_6M = new Euribor6M(CALENDAR_EUR);
  private static final IndexOIS EONIA = new Eonia(CALENDAR_EUR);
  private static final PriceIndex EUR_HICPXT = new EurolandHicpXT();
  private static final Currency EUR = EURIBOR_3M.getCurrency();
  private static final int SETTLEMENT_DAYS_EUR = EURIBOR_3M.getSettlementDays();

  private static final ZonedDateTime REFERENCE_DATE = DateUtils.getUTCDate(2011, 11, 9);
  private static final ZonedDateTime SPOT_DATE = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, CALENDAR_EUR, SETTLEMENT_DAYS_EUR);

  private static final double NOTIONAL_DEFAULT = 1000000.0;

  // ===== DEPOSIT
  private static final Period[] DEPOSIT_TENOR = new Period[] {Period.ofDays(1), Period.ofDays(1)};
  private static final int[] DEPOSIT_START = new int[] {0, 1};
  private static final double[] DEPOSIT_RATE = new double[] {0.01, 0.011};
  private static final int DEPOSIT_NB = DEPOSIT_TENOR.length;
  private static final Convention[] CONVENTION_DEPOSIT_EUR = new Convention[DEPOSIT_NB];
  private static final CashDefinition[] DEPOSIT_DEFINITION = new CashDefinition[DEPOSIT_NB];
  static {
    for (int loopdepo = 0; loopdepo < DEPOSIT_NB; loopdepo++) {
      CONVENTION_DEPOSIT_EUR[loopdepo] = new Convention(DEPOSIT_START[loopdepo], EURIBOR_3M.getDayCount(), EURIBOR_3M.getBusinessDayConvention(), CALENDAR_EUR, "EUR Deposit Convention");
      ZonedDateTime startDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, CALENDAR_EUR, DEPOSIT_START[loopdepo]);
      ZonedDateTime maturityDate = ScheduleCalculator.getAdjustedDate(startDate, EURIBOR_3M.getBusinessDayConvention(), CALENDAR_EUR, EURIBOR_3M.isEndOfMonth(), DEPOSIT_TENOR[loopdepo]);
      DEPOSIT_DEFINITION[loopdepo] = new CashDefinition(EUR, maturityDate, NOTIONAL_DEFAULT, DEPOSIT_RATE[loopdepo], CONVENTION_DEPOSIT_EUR[loopdepo]);
    }
  }
  // ===== SWAP EONIA
  private static final Period[] SWAP_EONIA_TENOR = new Period[] {Period.ofMonths(1), Period.ofMonths(3), Period.ofMonths(6), Period.ofMonths(12), Period.ofYears(2), Period.ofYears(3),
      Period.ofYears(4), Period.ofYears(5), Period.ofYears(6), Period.ofYears(7), Period.ofYears(8), Period.ofYears(9), Period.ofYears(10), Period.ofYears(12), Period.ofYears(15), Period.ofYears(20)};
  private static final double[] SWAP_EONIA_RATE = new double[] {0.012, 0.013, 0.014, 0.015, 0.016, 0.017, 0.018, 0.019, 0.020, 0.021, 0.022, 0.023, 0.024, 0.025, 0.026, 0.027};
  private static final int SWAP_EONIA_NB = SWAP_EONIA_TENOR.length;
  private static final SwapFixedOISSimplifiedDefinition[] SWAP_EONIA_DEFINITION = new SwapFixedOISSimplifiedDefinition[SWAP_EONIA_NB];
  static {
    for (int loopois = 0; loopois < SWAP_EONIA_NB; loopois++) {
      ZonedDateTime startDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, CALENDAR_EUR, SETTLEMENT_DAYS_EUR);
      SWAP_EONIA_DEFINITION[loopois] = SwapFixedOISSimplifiedDefinition.from(startDate, SWAP_EONIA_TENOR[loopois], Period.ofYears(1), NOTIONAL_DEFAULT, EONIA, SWAP_EONIA_RATE[loopois], true, 1,
          EURIBOR_3M.getBusinessDayConvention(), EURIBOR_3M.isEndOfMonth());
    }
  }

  // ===== SWAP EURIBOR 3M
  private static final Period[] SWAP_FAKE_EUR3_TENOR = new Period[] {Period.ofDays(1), Period.ofDays(1), Period.ofMonths(1)};
  private static final double[] SWAP_FAKE_EUR3_RATE = new double[] {0.01, 0.011, 0.012};
  private static final int[] SWAP_FAKE_EUR3_START = new int[] {0, 1, 2};
  private static final int SWAP_FAKE_EUR3_NB = SWAP_FAKE_EUR3_TENOR.length;

  private static final Period[] SWAP_EUR3_TENOR = new Period[] {Period.ofMonths(3), Period.ofMonths(6), Period.ofMonths(12), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4),
      Period.ofYears(5), Period.ofYears(6), Period.ofYears(7), Period.ofYears(8), Period.ofYears(9), Period.ofYears(10), Period.ofYears(12), Period.ofYears(15), Period.ofYears(20)};
  private static final double[] SWAP_EUR3_RATE = new double[] {0.015, 0.016, 0.017, 0.018, 0.019, 0.020, 0.021, 0.022, 0.023, 0.024, 0.025, 0.026, 0.027, 0.028, 0.029};
  private static final int SWAP_EUR3_NB = SWAP_EUR3_TENOR.length;

  private static final SwapFixedIborDefinition[] SWAP_EUR3_DEFINITION = new SwapFixedIborDefinition[SWAP_FAKE_EUR3_NB + SWAP_EUR3_NB];
  private static final SwapGenerator SWAP_GENERATOR_EUR3 = new SwapGenerator(Period.ofYears(1), DayCountFactory.INSTANCE.getDayCount("30/360"), EURIBOR_3M);
  static {
    for (int loop3 = 0; loop3 < SWAP_FAKE_EUR3_NB; loop3++) {
      ZonedDateTime startDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, CALENDAR_EUR, SWAP_FAKE_EUR3_START[loop3]);
      ZonedDateTime endDate = ScheduleCalculator.getAdjustedDate(startDate, EURIBOR_3M.getBusinessDayConvention(), CALENDAR_EUR, EURIBOR_3M.isEndOfMonth(), SWAP_FAKE_EUR3_TENOR[loop3]);
      double accrualFactor = EURIBOR_3M.getDayCount().getDayCountFraction(startDate, endDate);
      CouponFixedDefinition cpnFixed = new CouponFixedDefinition(EUR, endDate, startDate, endDate, accrualFactor, -NOTIONAL_DEFAULT, SWAP_FAKE_EUR3_RATE[loop3]);
      CouponIborDefinition cpnIbor = new CouponIborDefinition(EUR, endDate, startDate, endDate, accrualFactor, NOTIONAL_DEFAULT, startDate, startDate, endDate, accrualFactor, EURIBOR_3M);
      AnnuityCouponFixedDefinition legFixed = new AnnuityCouponFixedDefinition(new CouponFixedDefinition[] {cpnFixed});
      AnnuityCouponIborDefinition legIbor = new AnnuityCouponIborDefinition(new CouponIborDefinition[] {cpnIbor});
      SWAP_EUR3_DEFINITION[loop3] = new SwapFixedIborDefinition(legFixed, legIbor);
    }
    for (int loop3 = 0; loop3 < SWAP_EUR3_NB; loop3++) {
      CMSIndex cmsIndex = new CMSIndex(SWAP_GENERATOR_EUR3, SWAP_EUR3_TENOR[loop3]);
      ZonedDateTime startDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, CALENDAR_EUR, SETTLEMENT_DAYS_EUR);
      SWAP_EUR3_DEFINITION[SWAP_FAKE_EUR3_NB + loop3] = SwapFixedIborDefinition.from(startDate, cmsIndex, NOTIONAL_DEFAULT, SWAP_EUR3_RATE[loop3], true);
    }
  }

  // ===== SWAP EURIBOR 6M
  private static final Period[] SWAP_FAKE_EUR6_TENOR = new Period[] {Period.ofDays(1), Period.ofDays(1), Period.ofMonths(1), Period.ofMonths(3)};
  private static final double[] SWAP_FAKE_EUR6_RATE = new double[] {0.01, 0.011, 0.012, 0.013};
  private static final int[] SWAP_FAKE_EUR6_START = new int[] {0, 1, 2, 2};
  private static final int SWAP_FAKE_EUR6_NB = SWAP_FAKE_EUR6_TENOR.length;

  private static final Period[] SWAP_EUR6_TENOR = new Period[] {Period.ofMonths(6), Period.ofMonths(12), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4), Period.ofYears(5), Period.ofYears(6),
      Period.ofYears(7), Period.ofYears(8), Period.ofYears(9), Period.ofYears(10), Period.ofYears(12), Period.ofYears(15), Period.ofYears(20)};
  private static final double[] SWAP_EUR6_RATE = new double[] {0.016, 0.017, 0.018, 0.019, 0.020, 0.021, 0.022, 0.023, 0.024, 0.025, 0.026, 0.027, 0.028, 0.029};
  private static final int SWAP_EUR6_NB = SWAP_EUR6_TENOR.length;

  private static final SwapFixedIborDefinition[] SWAP_EUR6_DEFINITION = new SwapFixedIborDefinition[SWAP_FAKE_EUR6_NB + SWAP_EUR6_NB];
  private static final SwapGenerator SWAP_GENERATOR_EUR6 = new SwapGenerator(Period.ofYears(1), DayCountFactory.INSTANCE.getDayCount("30/360"), EURIBOR_6M);
  static {
    for (int loop6 = 0; loop6 < SWAP_FAKE_EUR6_NB; loop6++) {
      ZonedDateTime startDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, CALENDAR_EUR, SWAP_FAKE_EUR6_START[loop6]);
      ZonedDateTime endDate = ScheduleCalculator.getAdjustedDate(startDate, EURIBOR_6M.getBusinessDayConvention(), CALENDAR_EUR, EURIBOR_6M.isEndOfMonth(), SWAP_FAKE_EUR6_TENOR[loop6]);
      double accrualFactor = EURIBOR_6M.getDayCount().getDayCountFraction(startDate, endDate);
      CouponFixedDefinition cpnFixed = new CouponFixedDefinition(EUR, endDate, startDate, endDate, accrualFactor, -NOTIONAL_DEFAULT, SWAP_FAKE_EUR6_RATE[loop6]);
      CouponIborDefinition cpnIbor = new CouponIborDefinition(EUR, endDate, startDate, endDate, accrualFactor, NOTIONAL_DEFAULT, startDate, startDate, endDate, accrualFactor, EURIBOR_6M);
      AnnuityCouponFixedDefinition legFixed = new AnnuityCouponFixedDefinition(new CouponFixedDefinition[] {cpnFixed});
      AnnuityCouponIborDefinition legIbor = new AnnuityCouponIborDefinition(new CouponIborDefinition[] {cpnIbor});
      SWAP_EUR6_DEFINITION[loop6] = new SwapFixedIborDefinition(legFixed, legIbor);
    }
    for (int loop6 = 0; loop6 < SWAP_EUR6_NB; loop6++) {
      CMSIndex cmsIndex = new CMSIndex(SWAP_GENERATOR_EUR6, SWAP_EUR6_TENOR[loop6]);
      ZonedDateTime startDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, CALENDAR_EUR, SETTLEMENT_DAYS_EUR);
      SWAP_EUR6_DEFINITION[SWAP_FAKE_EUR6_NB + loop6] = SwapFixedIborDefinition.from(startDate, cmsIndex, NOTIONAL_DEFAULT, SWAP_EUR6_RATE[loop6], true);
    }
  }

  // ===== FRA EURIBOR 3M
  private static final int[] FRA_EUR3_TENOR_MONTH = new int[] {1, 2, 3, 6, 9, 12, 15};
  private static final double[] FRA_EUR3_RATE = new double[] {0.0101, 0.0111, 0.0121, 0.0131, 0.0141, 0.0151, 0.0161};
  private static final int FRA_EUR3_NB = FRA_EUR3_TENOR_MONTH.length;
  private static final ZonedDateTime[] FRA_EUR3_START_DATE = new ZonedDateTime[FRA_EUR3_NB];
  private static final ZonedDateTime[] FRA_EUR3_END_DATE = new ZonedDateTime[FRA_EUR3_NB];
  private static final ForwardRateAgreementDefinition[] FRA_EUR3_DEFINITION = new ForwardRateAgreementDefinition[FRA_EUR3_NB];
  static {
    for (int loopfra = 0; loopfra < FRA_EUR3_NB; loopfra++) {
      FRA_EUR3_START_DATE[loopfra] = ScheduleCalculator.getAdjustedDate(SPOT_DATE, EURIBOR_3M.getBusinessDayConvention(), CALENDAR_EUR, EURIBOR_3M.isEndOfMonth(),
          Period.ofMonths(FRA_EUR3_TENOR_MONTH[loopfra]));
      FRA_EUR3_END_DATE[loopfra] = ScheduleCalculator.getAdjustedDate(SPOT_DATE, EURIBOR_3M.getBusinessDayConvention(), CALENDAR_EUR, EURIBOR_3M.isEndOfMonth(),
          Period.ofMonths(3 + FRA_EUR3_TENOR_MONTH[loopfra]));
      FRA_EUR3_DEFINITION[loopfra] = ForwardRateAgreementDefinition.from(FRA_EUR3_START_DATE[loopfra], FRA_EUR3_END_DATE[loopfra], NOTIONAL_DEFAULT, EURIBOR_3M, FRA_EUR3_RATE[loopfra]);
    }
  }

  // ===== FRA EURIBOR 6M
  private static final int[] FRA_EUR6_TENOR_MONTH = new int[] {1, 2, 3, 6, 9, 12, 15};
  private static final double[] FRA_EUR6_RATE = new double[] {0.0121, 0.0131, 0.0141, 0.0151, 0.0161, 0.0171, 0.0181};
  private static final int FRA_EUR6_NB = FRA_EUR6_TENOR_MONTH.length;
  private static final ZonedDateTime[] FRA_EUR6_START_DATE = new ZonedDateTime[FRA_EUR6_NB];
  private static final ZonedDateTime[] FRA_EUR6_END_DATE = new ZonedDateTime[FRA_EUR6_NB];
  private static final ForwardRateAgreementDefinition[] FRA_EUR6_DEFINITION = new ForwardRateAgreementDefinition[FRA_EUR6_NB];
  static {
    for (int loopfra = 0; loopfra < FRA_EUR6_NB; loopfra++) {
      FRA_EUR6_START_DATE[loopfra] = ScheduleCalculator.getAdjustedDate(SPOT_DATE, EURIBOR_6M.getBusinessDayConvention(), CALENDAR_EUR, EURIBOR_6M.isEndOfMonth(),
          Period.ofMonths(FRA_EUR6_TENOR_MONTH[loopfra]));
      FRA_EUR6_END_DATE[loopfra] = ScheduleCalculator.getAdjustedDate(SPOT_DATE, EURIBOR_6M.getBusinessDayConvention(), CALENDAR_EUR, EURIBOR_6M.isEndOfMonth(),
          Period.ofMonths(6 + FRA_EUR3_TENOR_MONTH[loopfra]));
      FRA_EUR6_DEFINITION[loopfra] = ForwardRateAgreementDefinition.from(FRA_EUR6_START_DATE[loopfra], FRA_EUR6_END_DATE[loopfra], NOTIONAL_DEFAULT, EURIBOR_6M, FRA_EUR6_RATE[loopfra]);
    }
  }

  // ===== FUTURES 3M
  private static final ZonedDateTime FUT_EUR3_FIRST_MONTH = DateUtils.getUTCDate(2011, 12, 1);
  private static final double FUT_EUR3_NOTIONAL = 1000000;
  private static final double[] FUT_EUR3_PRICE = new double[] {0.9900, 0.9875, 0.9850, 0.9825};
  private static final int FUT_EUR3_NB = FUT_EUR3_PRICE.length;
  private static final ZonedDateTime[] FUT_EUR3_LAST_TRADING = new ZonedDateTime[FUT_EUR3_NB];
  private static final InterestRateFutureDefinition[] FUT_EUR3_DEFINITION = new InterestRateFutureDefinition[FUT_EUR3_NB];
  static {
    for (int loopfut = 0; loopfut < FUT_EUR3_NB; loopfut++) {
      FUT_EUR3_LAST_TRADING[loopfut] = ScheduleCalculator.getAdjustedDate(FUT_EUR3_FIRST_MONTH.plusMonths(3 * loopfut).with(DateAdjusters.dayOfWeekInMonth(3, DayOfWeek.WEDNESDAY)), CALENDAR_EUR,
          -EURIBOR_3M.getSettlementDays());
      FUT_EUR3_DEFINITION[loopfut] = new InterestRateFutureDefinition(FUT_EUR3_LAST_TRADING[loopfut], EURIBOR_3M, FUT_EUR3_PRICE[loopfut], FUT_EUR3_NOTIONAL, 0.25, "ER");
    }
  }

  // ===== SWAP INFLATION ZERO-COUPON EUR
  private static final int[] INFLZC_EUR_TENOR_YEAR = new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20};
  private static final double[] INFLZC_EUR_RATE = new double[] {0.0201, 0.0202, 0.0203, 0.0204, 0.0205, 0.0206, 0.0207, 0.0208, 0.0209, 0.0210, 0.0211, 0.0212};
  private static final int INFLZC_EUR_NB = INFLZC_EUR_TENOR_YEAR.length;
  private static final SwapFixedInflationZeroCouponDefinition[] INFLZC_EUR_DEFINITION = new SwapFixedInflationZeroCouponDefinition[INFLZC_EUR_NB];
  private static final int EUR_MONTH_LAG = 3;
  private static final ArrayZonedDateTimeDoubleTimeSeries EUR_HICPXT_TS = MarketDataSets.eurolandHICPXTFrom2005();
  static {
    for (int loopzc = 0; loopzc < INFLZC_EUR_NB; loopzc++) {
      INFLZC_EUR_DEFINITION[loopzc] = SwapFixedInflationZeroCouponDefinition.fromInterpolation(EUR_HICPXT, SPOT_DATE, INFLZC_EUR_TENOR_YEAR[loopzc], INFLZC_EUR_RATE[loopzc], NOTIONAL_DEFAULT, true,
          EURIBOR_3M.getBusinessDayConvention(), CALENDAR_EUR, EURIBOR_3M.isEndOfMonth(), EUR_MONTH_LAG, EUR_HICPXT_TS);
    }
  }
  private static final String NOT_USED = "Not used";
  private static final String[] NOT_USED_2 = new String[] {NOT_USED, NOT_USED};

  private static final Cash[] DEPOSIT = new Cash[DEPOSIT_NB];
  private static final InterestRateDerivative[] SWAP_EONIA = new InterestRateDerivative[SWAP_EONIA_NB];
  private static final InterestRateDerivative[] SWAP_EUR3 = new InterestRateDerivative[SWAP_FAKE_EUR3_NB + SWAP_EUR3_NB];
  private static final InterestRateDerivative[] SWAP_EUR6 = new InterestRateDerivative[SWAP_FAKE_EUR6_NB + SWAP_EUR6_NB];
  private static final ForwardRateAgreement[] FRA_EUR3 = new ForwardRateAgreement[FRA_EUR3_NB];
  private static final ForwardRateAgreement[] FRA_EUR6 = new ForwardRateAgreement[FRA_EUR6_NB];
  private static final InterestRateDerivative[] INFLZC_EUR = new InterestRateDerivative[INFLZC_EUR_NB];
  private static final InterestRateDerivative[] FUT_EUR3 = new InterestRateDerivative[FUT_EUR3_NB];
  static {
    for (int loopdepo = 0; loopdepo < DEPOSIT_NB; loopdepo++) {
      DEPOSIT[loopdepo] = DEPOSIT_DEFINITION[loopdepo].toDerivative(REFERENCE_DATE, NOT_USED);
    }
    for (int loopois = 0; loopois < SWAP_EONIA_NB; loopois++) {
      SWAP_EONIA[loopois] = SWAP_EONIA_DEFINITION[loopois].toDerivative(REFERENCE_DATE, NOT_USED_2);
    }
    for (int loop3 = 0; loop3 < SWAP_FAKE_EUR3_NB + SWAP_EUR3_NB; loop3++) {
      SWAP_EUR3[loop3] = SWAP_EUR3_DEFINITION[loop3].toDerivative(REFERENCE_DATE, NOT_USED_2);
    }
    for (int loop6 = 0; loop6 < SWAP_FAKE_EUR6_NB + SWAP_EUR6_NB; loop6++) {
      SWAP_EUR6[loop6] = SWAP_EUR6_DEFINITION[loop6].toDerivative(REFERENCE_DATE, NOT_USED_2);
    }
    for (int loopfra = 0; loopfra < FRA_EUR3_NB; loopfra++) {
      FRA_EUR3[loopfra] = (ForwardRateAgreement) FRA_EUR3_DEFINITION[loopfra].toDerivative(REFERENCE_DATE, NOT_USED_2);
    }
    for (int loopfra = 0; loopfra < FRA_EUR6_NB; loopfra++) {
      FRA_EUR6[loopfra] = (ForwardRateAgreement) FRA_EUR6_DEFINITION[loopfra].toDerivative(REFERENCE_DATE, NOT_USED_2);
    }
    for (int loopzc = 0; loopzc < INFLZC_EUR_NB; loopzc++) {
      INFLZC_EUR[loopzc] = INFLZC_EUR_DEFINITION[loopzc].toDerivative(REFERENCE_DATE, new ArrayZonedDateTimeDoubleTimeSeries[] {EUR_HICPXT_TS}, NOT_USED_2);
    }
    for (int loopfut = 0; loopfut < INFLZC_EUR_NB; loopfut++) {
      FUT_EUR3[loopfut] = FUT_EUR3_DEFINITION[loopfut].toDerivative(REFERENCE_DATE, FUT_EUR3_PRICE[loopfut], NOT_USED_2);
    }
  }

  public static ZonedDateTime referenceDate() {
    return REFERENCE_DATE;
  }

  /**
   * Return a set of instruments (cash deposits and OIS swap) for discounting curve construction.
   * @return The instrument set.
   */
  public static InterestRateDerivative[] instrumentsDiscounting() {
    InterestRateDerivative[] instruments = new InterestRateDerivative[DEPOSIT_NB + SWAP_EONIA_NB];
    for (int loopdepo = 0; loopdepo < DEPOSIT_NB; loopdepo++) {
      instruments[loopdepo] = DEPOSIT[loopdepo];
    }
    for (int loopois = 0; loopois < SWAP_EONIA_NB; loopois++) {
      instruments[DEPOSIT_NB + loopois] = SWAP_EONIA[loopois];
    }
    return instruments;
  }

  /**
   * Return the times associated to the instrument for the curve construction.
   * @return The times.
   */
  public static double[] timeDiscounting() {
    double[] times = new double[DEPOSIT_NB + SWAP_EONIA_NB];
    for (int loopdepo = 0; loopdepo < DEPOSIT_NB; loopdepo++) {
      times[loopdepo] = DEPOSIT[loopdepo].getMaturity();
    }
    for (int loopois = 0; loopois < SWAP_EONIA_NB; loopois++) {
      @SuppressWarnings("unchecked")
      GenericAnnuity<Coupon> leg = ((Swap<Coupon, Coupon>) SWAP_EONIA[loopois]).getFirstLeg();
      times[DEPOSIT_NB + loopois] = leg.getNthPayment(leg.getNumberOfPayments() - 1).getPaymentTime();
    }
    return times;
  }

  /**
   * Returns the market rate associated to the market instrument. Used mainly as starting point for the yield curve construction.
   * @return The market rates.
   */
  public static double[] marketRateDiscounting() {
    double[] rate = new double[DEPOSIT_NB + SWAP_EONIA_NB];
    System.arraycopy(DEPOSIT_RATE, 0, rate, 0, DEPOSIT_NB);
    System.arraycopy(SWAP_EONIA_RATE, 0, rate, DEPOSIT_NB, SWAP_EONIA_NB);
    return rate;
  }

  public static InterestRateDerivative[] instrumentsForward3FullSwap() {
    return SWAP_EUR3;
  }

  public static double[] timeForward3FullSwap() {
    double[] times = new double[SWAP_FAKE_EUR3_NB + SWAP_EUR3_NB];
    for (int loop3 = 0; loop3 < SWAP_FAKE_EUR3_NB + SWAP_EUR3_NB; loop3++) {
      @SuppressWarnings("unchecked")
      GenericAnnuity<Coupon> leg = ((Swap<Coupon, Coupon>) SWAP_EUR3[loop3]).getFirstLeg();
      times[loop3] = leg.getNthPayment(leg.getNumberOfPayments() - 1).getPaymentTime();
    }
    return times;
  }

  public static double[] marketRateForward3FullSwap() {
    double[] rate = new double[SWAP_FAKE_EUR3_NB + SWAP_EUR3_NB];
    System.arraycopy(SWAP_FAKE_EUR3_RATE, 0, rate, 0, SWAP_FAKE_EUR3_NB);
    System.arraycopy(SWAP_EUR3_RATE, 0, rate, SWAP_FAKE_EUR3_NB, SWAP_EUR3_NB);
    return rate;
  }

  public static InterestRateDerivative[] instrumentsForward6FullSwap() {
    return SWAP_EUR6;
  }

  public static double[] timeForward6FullSwap() {
    double[] times = new double[SWAP_FAKE_EUR6_NB + SWAP_EUR6_NB];
    for (int loop6 = 0; loop6 < SWAP_FAKE_EUR6_NB + SWAP_EUR6_NB; loop6++) {
      @SuppressWarnings("unchecked")
      GenericAnnuity<Coupon> leg = ((Swap<Coupon, Coupon>) SWAP_EUR6[loop6]).getFirstLeg();
      times[loop6] = leg.getNthPayment(leg.getNumberOfPayments() - 1).getPaymentTime();
    }
    return times;
  }

  public static double[] marketRateForward6FullSwap() {
    double[] rate = new double[SWAP_FAKE_EUR6_NB + SWAP_EUR6_NB];
    System.arraycopy(SWAP_FAKE_EUR6_RATE, 0, rate, 0, SWAP_FAKE_EUR6_NB);
    System.arraycopy(SWAP_EUR6_RATE, 0, rate, SWAP_FAKE_EUR6_NB, SWAP_EUR6_NB);
    return rate;
  }

  public static InterestRateDerivative[] instrumentsForward3FraSwap() {
    int nbShort = SWAP_FAKE_EUR3_NB;
    int nbFra = FRA_EUR3_NB;
    int indexStartSwap = 3;
    InterestRateDerivative[] result = new InterestRateDerivative[nbShort + nbFra + SWAP_EUR3_NB - indexStartSwap];
    for (int loopins = 0; loopins < nbShort; loopins++) {
      result[loopins] = SWAP_EUR3[loopins];
    }
    for (int loopins = 0; loopins < nbFra; loopins++) {
      result[nbShort + loopins] = FRA_EUR3[loopins];
    }
    for (int loopins = 0; loopins < SWAP_EUR3_NB - indexStartSwap; loopins++) {
      result[nbShort + nbFra + loopins] = SWAP_EUR3[nbShort + indexStartSwap + loopins];
    }
    return result;
  }

  public static double[] timeForward3FraSwap() {
    int nbShort = SWAP_FAKE_EUR3_NB;
    int nbFra = FRA_EUR3_NB;
    int indexStartSwap = 3;
    double[] times = new double[nbShort + nbFra + SWAP_EUR3_NB - indexStartSwap];
    for (int loopins = 0; loopins < nbShort; loopins++) {
      @SuppressWarnings("unchecked")
      GenericAnnuity<Coupon> leg = ((Swap<Coupon, Coupon>) SWAP_EUR3[loopins]).getFirstLeg();
      times[loopins] = leg.getNthPayment(leg.getNumberOfPayments() - 1).getPaymentTime();
    }
    for (int loopins = 0; loopins < nbFra; loopins++) {
      times[nbShort + loopins] = FRA_EUR3[loopins].getFixingPeriodEndTime();
    }
    for (int loopins = 0; loopins < SWAP_EUR3_NB - indexStartSwap; loopins++) {
      @SuppressWarnings("unchecked")
      GenericAnnuity<Coupon> leg = ((Swap<Coupon, Coupon>) SWAP_EUR3[nbShort + indexStartSwap + loopins]).getFirstLeg();
      times[nbShort + nbFra + loopins] = leg.getNthPayment(leg.getNumberOfPayments() - 1).getPaymentTime();
    }
    return times;
  }

  public static double[] marketRateForward3FraSwap() {
    int nbShort = SWAP_FAKE_EUR3_NB;
    int nbFra = FRA_EUR3_NB;
    int indexStartSwap = 3;
    double[] rate = new double[nbShort + nbFra + SWAP_EUR3_NB - indexStartSwap];
    System.arraycopy(SWAP_FAKE_EUR3_RATE, 0, rate, 0, nbShort);
    System.arraycopy(FRA_EUR3_RATE, 0, rate, nbShort, nbFra);
    System.arraycopy(SWAP_EUR3_RATE, indexStartSwap, rate, nbShort + nbFra, SWAP_EUR3_NB - indexStartSwap);
    return rate;
  }

  public static InterestRateDerivative[] instrumentsInflation() {
    return INFLZC_EUR;
  }

  /**
   * Return the last times related to the inflation curve used in pricing (reference end time, component 1).
   * @return The times.
   */
  public static double[] timeInflation() {
    double[] times = new double[INFLZC_EUR_NB];
    for (int loopzc = 0; loopzc < INFLZC_EUR_NB; loopzc++) {
      @SuppressWarnings("unchecked")
      GenericAnnuity<Coupon> leg = ((Swap<Coupon, Coupon>) INFLZC_EUR[loopzc]).getSecondLeg();
      times[loopzc] = ((CouponInflationZeroCouponInterpolation) (leg.getNthPayment(leg.getNumberOfPayments() - 1))).getReferenceEndTime()[1];
    }
    return times;
  }

  public static double[] marketRateInflation() {
    return INFLZC_EUR_RATE;
  }

}
