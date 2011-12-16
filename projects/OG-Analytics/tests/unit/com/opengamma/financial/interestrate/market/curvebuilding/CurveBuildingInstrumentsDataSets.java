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
import com.opengamma.financial.instrument.annuity.AnnuityCouponFixedDefinition;
import com.opengamma.financial.instrument.annuity.AnnuityCouponIborDefinition;
import com.opengamma.financial.instrument.cash.DepositDefinition;
import com.opengamma.financial.instrument.cash.DepositIborDefinition;
import com.opengamma.financial.instrument.fra.ForwardRateAgreementDefinition;
import com.opengamma.financial.instrument.future.InterestRateFutureDefinition;
import com.opengamma.financial.instrument.index.GeneratorDeposit;
import com.opengamma.financial.instrument.index.IborIndex;
import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.instrument.index.IndexON;
import com.opengamma.financial.instrument.index.IndexPrice;
import com.opengamma.financial.instrument.index.IndexSwap;
import com.opengamma.financial.instrument.index.SwapGenerator;
import com.opengamma.financial.instrument.index.generator.EUR1YEURIBOR3M;
import com.opengamma.financial.instrument.index.generator.EUR1YEURIBOR6M;
import com.opengamma.financial.instrument.index.generator.EURDeposit;
import com.opengamma.financial.instrument.index.iborindex.EURIBOR3M;
import com.opengamma.financial.instrument.index.iborindex.EURIBOR6M;
import com.opengamma.financial.instrument.index.iborindex.USDLIBOR3M;
import com.opengamma.financial.instrument.index.indexon.EONIA;
import com.opengamma.financial.instrument.index.priceindex.EurolandHicpXT;
import com.opengamma.financial.instrument.payment.CouponFixedDefinition;
import com.opengamma.financial.instrument.payment.CouponIborDefinition;
import com.opengamma.financial.instrument.swap.SwapFixedIborDefinition;
import com.opengamma.financial.instrument.swap.SwapFixedInflationZeroCouponDefinition;
import com.opengamma.financial.instrument.swap.SwapFixedOISSimplifiedDefinition;
import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.annuity.definition.GenericAnnuity;
import com.opengamma.financial.interestrate.cash.derivative.Cash;
import com.opengamma.financial.interestrate.cash.derivative.DepositIbor;
import com.opengamma.financial.interestrate.fra.ForwardRateAgreement;
import com.opengamma.financial.interestrate.future.definition.InterestRateFuture;
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

  private static final Calendar CALENDAR_EUR = new MondayToFridayCalendar("TARGET");
  private static final Calendar CALENDAR_USD = new MondayToFridayCalendar("USD");
  private static final GeneratorDeposit GENERATOR_EUR = new EURDeposit(CALENDAR_EUR);
  private static final IborIndex EURIBOR_3M = new EURIBOR3M(CALENDAR_EUR);
  private static final IborIndex EURIBOR_6M = new EURIBOR6M(CALENDAR_EUR);
  private static final IborIndex USDLIBOR_3M = new USDLIBOR3M(CALENDAR_USD);
  private static final IndexON EONIA = new EONIA(CALENDAR_EUR);
  private static final IndexDeposit[] INDEXES = new IndexDeposit[] {EURIBOR_3M, EURIBOR_6M, USDLIBOR_3M, EONIA};
  private static final IndexPrice EUR_HICPXT = new EurolandHicpXT();

  private static final SwapGenerator EUR1YEURIBOR6M = new EUR1YEURIBOR6M(CALENDAR_EUR);
  private static final SwapGenerator EUR1YEURIBOR3M = new EUR1YEURIBOR3M(CALENDAR_EUR);

  private static final Currency EUR = EURIBOR_3M.getCurrency();
  private static final int SETTLEMENT_DAYS_EUR = EURIBOR_3M.getSpotLag();

  private static final ZonedDateTime REFERENCE_DATE = DateUtils.getUTCDate(2011, 11, 9);
  private static final ZonedDateTime SPOT_DATE = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, SETTLEMENT_DAYS_EUR, CALENDAR_EUR);

  private static final double NOTIONAL_DEFAULT = 1000000.0;

  // ===== DEPOSIT ON
  private static final int[] DEPOSIT_ON_START = new int[] {0, 1};
  private static final double[] DEPOSIT_ON_RATE = new double[] {0.01, 0.011};
  private static final int DEPOSIT_ON_NB = DEPOSIT_ON_START.length;
  private static final DepositDefinition[] DEPOSIT_ON_DEFINITION = new DepositDefinition[DEPOSIT_ON_NB];
  static {
    for (int loopdepo = 0; loopdepo < DEPOSIT_ON_NB; loopdepo++) {
      DEPOSIT_ON_DEFINITION[loopdepo] = DepositDefinition.fromTrade(REFERENCE_DATE, DEPOSIT_ON_START[loopdepo], NOTIONAL_DEFAULT, DEPOSIT_ON_RATE[loopdepo], GENERATOR_EUR);
    }
  }

  // ===== DEPOSIT PERIOD
  private static final Period[] DEPOSIT_PERIOD_TENOR = new Period[] {Period.ofMonths(1), Period.ofMonths(3)};
  private static final double[] DEPOSIT_PERIOD_RATE = new double[] {0.012, 0.013};
  private static final int DEPOSIT_PERIOD_NB = DEPOSIT_PERIOD_TENOR.length;
  private static final DepositDefinition[] DEPOSIT_PERIOD_DEFINITION = new DepositDefinition[DEPOSIT_PERIOD_NB];
  static {
    for (int loopdepo = 0; loopdepo < DEPOSIT_PERIOD_NB; loopdepo++) {
      DEPOSIT_PERIOD_DEFINITION[loopdepo] = DepositDefinition.fromTrade(REFERENCE_DATE, DEPOSIT_PERIOD_TENOR[loopdepo], NOTIONAL_DEFAULT, DEPOSIT_PERIOD_RATE[loopdepo], GENERATOR_EUR);
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
      ZonedDateTime startDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, SETTLEMENT_DAYS_EUR, CALENDAR_EUR);
      SWAP_EONIA_DEFINITION[loopois] = SwapFixedOISSimplifiedDefinition.from(startDate, SWAP_EONIA_TENOR[loopois], Period.ofYears(1), NOTIONAL_DEFAULT, EONIA, SWAP_EONIA_RATE[loopois], true, 1,
          EURIBOR_3M.getBusinessDayConvention(), EURIBOR_3M.isEndOfMonth());
    }
  }

  // ===== DEPOSIT EURIBOR 3M
  private static final double[] DEPOSIT_IBOR_RATE = new double[] {0.015};
  private static final int DEPOSIT_IBOR_NB = DEPOSIT_IBOR_RATE.length;
  private static final DepositIborDefinition[] DEPOSIT_IBOR_DEFINITION = new DepositIborDefinition[DEPOSIT_IBOR_NB];
  static {
    for (int loopdepo = 0; loopdepo < DEPOSIT_IBOR_NB; loopdepo++) {
      DEPOSIT_IBOR_DEFINITION[loopdepo] = DepositIborDefinition.fromTrade(REFERENCE_DATE, NOTIONAL_DEFAULT, DEPOSIT_IBOR_RATE[loopdepo], EURIBOR_3M);
    }
  }

  // ===== SWAP EURIBOR 3M
  private static final Period[] SWAP_FAKE_EUR3_TENOR = new Period[] {Period.ofMonths(1)};
  private static final double[] SWAP_FAKE_EUR3_RATE = new double[] {0.014};
  private static final int[] SWAP_FAKE_EUR3_START = new int[] {2};
  private static final int SWAP_FAKE_EUR3_NB = SWAP_FAKE_EUR3_TENOR.length;

  private static final Period[] SWAP_EUR3_TENOR = new Period[] {Period.ofMonths(3), Period.ofMonths(6), Period.ofMonths(12), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4),
      Period.ofYears(5), Period.ofYears(6), Period.ofYears(7), Period.ofYears(8), Period.ofYears(9), Period.ofYears(10), Period.ofYears(12), Period.ofYears(15), Period.ofYears(20)};
  private static final double[] SWAP_EUR3_RATE = new double[] {0.015, 0.016, 0.017, 0.018, 0.019, 0.020, 0.021, 0.022, 0.023, 0.024, 0.025, 0.026, 0.027, 0.028, 0.029};
  private static final int SWAP_EUR3_NB = SWAP_EUR3_TENOR.length;

  private static final SwapFixedIborDefinition[] SWAP_EUR3_DEFINITION = new SwapFixedIborDefinition[SWAP_FAKE_EUR3_NB + SWAP_EUR3_NB];
  static {
    for (int loop3 = 0; loop3 < SWAP_FAKE_EUR3_NB; loop3++) {
      ZonedDateTime startDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, SWAP_FAKE_EUR3_START[loop3], CALENDAR_EUR);
      ZonedDateTime endDate = ScheduleCalculator.getAdjustedDate(startDate, SWAP_FAKE_EUR3_TENOR[loop3], EURIBOR_3M.getBusinessDayConvention(), CALENDAR_EUR, EURIBOR_3M.isEndOfMonth());
      double accrualFactor = EURIBOR_3M.getDayCount().getDayCountFraction(startDate, endDate);
      CouponFixedDefinition cpnFixed = new CouponFixedDefinition(EUR, endDate, startDate, endDate, accrualFactor, -NOTIONAL_DEFAULT, SWAP_FAKE_EUR3_RATE[loop3]);
      CouponIborDefinition cpnIbor = new CouponIborDefinition(EUR, endDate, startDate, endDate, accrualFactor, NOTIONAL_DEFAULT, startDate, startDate, endDate, accrualFactor, EURIBOR_3M);
      AnnuityCouponFixedDefinition legFixed = new AnnuityCouponFixedDefinition(new CouponFixedDefinition[] {cpnFixed});
      AnnuityCouponIborDefinition legIbor = new AnnuityCouponIborDefinition(new CouponIborDefinition[] {cpnIbor});
      SWAP_EUR3_DEFINITION[loop3] = new SwapFixedIborDefinition(legFixed, legIbor);
    }
    for (int loop3 = 0; loop3 < SWAP_EUR3_NB; loop3++) {
      IndexSwap cmsIndex = new IndexSwap(EUR1YEURIBOR3M, SWAP_EUR3_TENOR[loop3]);
      ZonedDateTime startDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, SETTLEMENT_DAYS_EUR, CALENDAR_EUR);
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
  static {
    for (int loop6 = 0; loop6 < SWAP_FAKE_EUR6_NB; loop6++) {
      ZonedDateTime startDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, SWAP_FAKE_EUR6_START[loop6], CALENDAR_EUR);
      ZonedDateTime endDate = ScheduleCalculator.getAdjustedDate(startDate, SWAP_FAKE_EUR6_TENOR[loop6], EURIBOR_6M.getBusinessDayConvention(), CALENDAR_EUR, EURIBOR_6M.isEndOfMonth());
      double accrualFactor = EURIBOR_6M.getDayCount().getDayCountFraction(startDate, endDate);
      CouponFixedDefinition cpnFixed = new CouponFixedDefinition(EUR, endDate, startDate, endDate, accrualFactor, -NOTIONAL_DEFAULT, SWAP_FAKE_EUR6_RATE[loop6]);
      CouponIborDefinition cpnIbor = new CouponIborDefinition(EUR, endDate, startDate, endDate, accrualFactor, NOTIONAL_DEFAULT, startDate, startDate, endDate, accrualFactor, EURIBOR_6M);
      AnnuityCouponFixedDefinition legFixed = new AnnuityCouponFixedDefinition(new CouponFixedDefinition[] {cpnFixed});
      AnnuityCouponIborDefinition legIbor = new AnnuityCouponIborDefinition(new CouponIborDefinition[] {cpnIbor});
      SWAP_EUR6_DEFINITION[loop6] = new SwapFixedIborDefinition(legFixed, legIbor);
    }
    for (int loop6 = 0; loop6 < SWAP_EUR6_NB; loop6++) {
      IndexSwap cmsIndex = new IndexSwap(EUR1YEURIBOR6M, SWAP_EUR6_TENOR[loop6]);
      ZonedDateTime startDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, SETTLEMENT_DAYS_EUR, CALENDAR_EUR);
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
      FRA_EUR3_START_DATE[loopfra] = ScheduleCalculator.getAdjustedDate(SPOT_DATE, Period.ofMonths(FRA_EUR3_TENOR_MONTH[loopfra]), EURIBOR_3M.getBusinessDayConvention(), CALENDAR_EUR,
          EURIBOR_3M.isEndOfMonth());
      FRA_EUR3_END_DATE[loopfra] = ScheduleCalculator.getAdjustedDate(SPOT_DATE, Period.ofMonths(3 + FRA_EUR3_TENOR_MONTH[loopfra]), EURIBOR_3M.getBusinessDayConvention(), CALENDAR_EUR,
          EURIBOR_3M.isEndOfMonth());
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
      FRA_EUR6_START_DATE[loopfra] = ScheduleCalculator.getAdjustedDate(SPOT_DATE, Period.ofMonths(FRA_EUR6_TENOR_MONTH[loopfra]), EURIBOR_6M.getBusinessDayConvention(), CALENDAR_EUR,
          EURIBOR_6M.isEndOfMonth());
      FRA_EUR6_END_DATE[loopfra] = ScheduleCalculator.getAdjustedDate(SPOT_DATE, Period.ofMonths(6 + FRA_EUR3_TENOR_MONTH[loopfra]), EURIBOR_6M.getBusinessDayConvention(), CALENDAR_EUR,
          EURIBOR_6M.isEndOfMonth());
      FRA_EUR6_DEFINITION[loopfra] = ForwardRateAgreementDefinition.from(FRA_EUR6_START_DATE[loopfra], FRA_EUR6_END_DATE[loopfra], NOTIONAL_DEFAULT, EURIBOR_6M, FRA_EUR6_RATE[loopfra]);
    }
  }

  // ===== FUTURES 3M
  private static final ZonedDateTime FUT_EUR3_FIRST_MONTH = DateUtils.getUTCDate(2011, 12, 1);
  private static final double FUT_EUR3_NOTIONAL = 1000000;
  private static final double[] FUT_EUR3_PRICE = new double[] {0.9900, 0.9875, 0.9850, 0.9825, 0.9825, 0.9825, 0.9825, 0.9825};
  private static final int FUT_EUR3_NB = FUT_EUR3_PRICE.length;
  private static final ZonedDateTime[] FUT_EUR3_LAST_TRADING = new ZonedDateTime[FUT_EUR3_NB];
  private static final InterestRateFutureDefinition[] FUT_EUR3_DEFINITION = new InterestRateFutureDefinition[FUT_EUR3_NB];
  static {
    for (int loopfut = 0; loopfut < FUT_EUR3_NB; loopfut++) {
      FUT_EUR3_LAST_TRADING[loopfut] = ScheduleCalculator.getAdjustedDate(FUT_EUR3_FIRST_MONTH.plusMonths(3 * loopfut).with(DateAdjusters.dayOfWeekInMonth(3, DayOfWeek.WEDNESDAY)),
          -EURIBOR_3M.getSpotLag(), CALENDAR_EUR);
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

  // ===== Derivatives =====
  private static final String NOT_USED = "Not used";
  private static final String[] NOT_USED_2 = new String[] {NOT_USED, NOT_USED};

  private static final Cash[] DEPOSIT_ON = new Cash[DEPOSIT_ON_NB];
  private static final Cash[] DEPOSIT_PERIOD = new Cash[DEPOSIT_PERIOD_NB];
  private static final DepositIbor[] DEPOSIT_IBOR = new DepositIbor[DEPOSIT_IBOR_NB];
  private static final InstrumentDerivative[] SWAP_EONIA = new InstrumentDerivative[SWAP_EONIA_NB];
  private static final InstrumentDerivative[] SWAP_EUR3 = new InstrumentDerivative[SWAP_FAKE_EUR3_NB + SWAP_EUR3_NB];
  private static final InstrumentDerivative[] SWAP_EUR6 = new InstrumentDerivative[SWAP_FAKE_EUR6_NB + SWAP_EUR6_NB];
  private static final ForwardRateAgreement[] FRA_EUR3 = new ForwardRateAgreement[FRA_EUR3_NB];
  private static final ForwardRateAgreement[] FRA_EUR6 = new ForwardRateAgreement[FRA_EUR6_NB];
  private static final InstrumentDerivative[] INFLZC_EUR = new InstrumentDerivative[INFLZC_EUR_NB];
  private static final InterestRateFuture[] FUT_EUR3 = new InterestRateFuture[FUT_EUR3_NB];
  static {
    for (int loopdepo = 0; loopdepo < DEPOSIT_ON_NB; loopdepo++) {
      DEPOSIT_ON[loopdepo] = DEPOSIT_ON_DEFINITION[loopdepo].toDerivative(REFERENCE_DATE, NOT_USED);
    }
    for (int loopdepo = 0; loopdepo < DEPOSIT_PERIOD_NB; loopdepo++) {
      DEPOSIT_PERIOD[loopdepo] = DEPOSIT_PERIOD_DEFINITION[loopdepo].toDerivative(REFERENCE_DATE, NOT_USED);
    }
    for (int loopdepo = 0; loopdepo < DEPOSIT_IBOR_NB; loopdepo++) {
      DEPOSIT_IBOR[loopdepo] = DEPOSIT_IBOR_DEFINITION[loopdepo].toDerivative(REFERENCE_DATE, NOT_USED);
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
    for (int loopfut = 0; loopfut < FUT_EUR3_NB; loopfut++) {
      FUT_EUR3[loopfut] = FUT_EUR3_DEFINITION[loopfut].toDerivative(REFERENCE_DATE, FUT_EUR3_PRICE[loopfut], NOT_USED_2);
    }
  }

  public static ZonedDateTime referenceDate() {
    return REFERENCE_DATE;
  }

  public static IndexDeposit[] indexes() {
    return INDEXES;
  }

  /**
   * Return a set of instruments (cash deposits and OIS swap) for discounting curve construction.
   * @return The instrument set.
   */
  public static InstrumentDerivative[] instrumentsDiscountingOIS() {
    InstrumentDerivative[] instruments = new InstrumentDerivative[DEPOSIT_ON_NB + SWAP_EONIA_NB];
    for (int loopdepo = 0; loopdepo < DEPOSIT_ON_NB; loopdepo++) {
      instruments[loopdepo] = DEPOSIT_ON[loopdepo];
    }
    for (int loopois = 0; loopois < SWAP_EONIA_NB; loopois++) {
      instruments[DEPOSIT_ON_NB + loopois] = SWAP_EONIA[loopois];
    }
    return instruments;
  }

  /**
   * Returns the market rate associated to the market instrument. Used mainly as starting point for the yield curve construction.
   * @return The market rates.
   */
  public static double[] marketRateDiscountingOIS() {
    double[] rate = new double[DEPOSIT_ON_NB + SWAP_EONIA_NB];
    System.arraycopy(DEPOSIT_ON_RATE, 0, rate, 0, DEPOSIT_ON_NB);
    System.arraycopy(SWAP_EONIA_RATE, 0, rate, DEPOSIT_ON_NB, SWAP_EONIA_NB);
    return rate;
  }

  /**
   * Return a set of instruments (cash deposits and OIS swap) for discounting curve construction.
   * @return The instrument set.
   */
  public static InstrumentDerivative[] instrumentsDiscountingDeposit() {
    InstrumentDerivative[] instruments = new InstrumentDerivative[DEPOSIT_ON_NB + SWAP_EONIA_NB];
    for (int loopdepo = 0; loopdepo < DEPOSIT_ON_NB; loopdepo++) {
      instruments[loopdepo] = DEPOSIT_ON[loopdepo];
    }
    for (int loopdepo = 0; loopdepo < DEPOSIT_PERIOD_NB; loopdepo++) {
      instruments[DEPOSIT_ON_NB + loopdepo] = DEPOSIT_PERIOD[loopdepo];
    }
    for (int loopois = DEPOSIT_PERIOD_NB; loopois < SWAP_EONIA_NB; loopois++) {
      instruments[DEPOSIT_ON_NB + loopois] = SWAP_EONIA[loopois];
    }
    return instruments;
  }

  /**
   * Returns the market rate associated to the market instrument. Used mainly as starting point for the yield curve construction.
   * @return The market rates.
   */
  public static double[] marketRateDiscountingDeposit() {
    double[] rate = new double[DEPOSIT_ON_NB + SWAP_EONIA_NB];
    System.arraycopy(DEPOSIT_ON_RATE, 0, rate, 0, DEPOSIT_ON_NB);
    System.arraycopy(DEPOSIT_PERIOD_RATE, 0, rate, DEPOSIT_ON_NB, DEPOSIT_PERIOD_NB);
    System.arraycopy(SWAP_EONIA_RATE, DEPOSIT_PERIOD_NB, rate, DEPOSIT_ON_NB + DEPOSIT_PERIOD_NB, SWAP_EONIA_NB - DEPOSIT_PERIOD_NB);
    return rate;
  }

  public static InstrumentDerivative[] instrumentsForward3FullSwap() {
    return SWAP_EUR3;
  }

  public static double[] marketRateForward3FullSwap() {
    double[] rate = new double[SWAP_FAKE_EUR3_NB + SWAP_EUR3_NB];
    System.arraycopy(SWAP_FAKE_EUR3_RATE, 0, rate, 0, SWAP_FAKE_EUR3_NB);
    System.arraycopy(SWAP_EUR3_RATE, 0, rate, SWAP_FAKE_EUR3_NB, SWAP_EUR3_NB);
    return rate;
  }

  public static InstrumentDerivative[] instrumentsForward3IborSwap() {
    int indexStartSwap = 1;
    InstrumentDerivative[] result = new InstrumentDerivative[DEPOSIT_IBOR_NB + SWAP_EUR3_NB - indexStartSwap];
    for (int loopins = 0; loopins < DEPOSIT_IBOR_NB; loopins++) {
      result[loopins] = DEPOSIT_IBOR[loopins];
    }
    for (int loopins = 0; loopins < SWAP_EUR3_NB - indexStartSwap; loopins++) {
      result[DEPOSIT_IBOR_NB + loopins] = SWAP_EUR3[indexStartSwap + loopins];
    }
    return result;
  }

  public static double[] marketRateForward3IborSwap() {
    double[] rate = new double[DEPOSIT_IBOR_NB + SWAP_EUR3_NB];
    System.arraycopy(DEPOSIT_IBOR_RATE, 0, rate, 0, DEPOSIT_IBOR_NB);
    System.arraycopy(SWAP_EUR3_RATE, 0, rate, DEPOSIT_IBOR_NB, SWAP_EUR3_NB);
    return rate;
  }

  public static InstrumentDerivative[] instrumentsForward6FullSwap() {
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

  public static InstrumentDerivative[] instrumentsForward3FraSwap() {
    int indexStartSwap = 4;
    InstrumentDerivative[] result = new InstrumentDerivative[DEPOSIT_IBOR_NB + FRA_EUR3_NB + SWAP_EUR3_NB - indexStartSwap];
    for (int loopins = 0; loopins < DEPOSIT_IBOR_NB; loopins++) {
      result[loopins] = DEPOSIT_IBOR[loopins];
    }
    for (int loopins = 0; loopins < FRA_EUR3_NB; loopins++) {
      result[DEPOSIT_IBOR_NB + loopins] = FRA_EUR3[loopins];
    }
    for (int loopins = 0; loopins < SWAP_EUR3_NB - indexStartSwap; loopins++) {
      result[DEPOSIT_IBOR_NB + FRA_EUR3_NB + loopins] = SWAP_EUR3[indexStartSwap + loopins];
    }
    return result;
  }

  public static double[] marketRateForward3FraSwap() {
    int indexStartSwap = 4;
    double[] rate = new double[DEPOSIT_IBOR_NB + FRA_EUR3_NB + SWAP_EUR3_NB - indexStartSwap];
    System.arraycopy(DEPOSIT_IBOR_RATE, 0, rate, 0, DEPOSIT_IBOR_NB);
    System.arraycopy(FRA_EUR3_RATE, 0, rate, DEPOSIT_IBOR_NB, FRA_EUR3_NB);
    System.arraycopy(SWAP_EUR3_RATE, indexStartSwap, rate, DEPOSIT_IBOR_NB + FRA_EUR3_NB, SWAP_EUR3_NB - indexStartSwap);
    return rate;
  }

  public static InstrumentDerivative[] instrumentsForward3FutSwap() {
    int indexStartSwap = 4;
    InstrumentDerivative[] result = new InstrumentDerivative[DEPOSIT_IBOR_NB + FUT_EUR3_NB + SWAP_EUR3_NB - indexStartSwap];
    for (int loopins = 0; loopins < DEPOSIT_IBOR_NB; loopins++) {
      result[loopins] = DEPOSIT_IBOR[loopins];
    }
    for (int loopins = 0; loopins < FUT_EUR3_NB; loopins++) {
      result[DEPOSIT_IBOR_NB + loopins] = FUT_EUR3[loopins];
    }
    for (int loopins = 0; loopins < SWAP_EUR3_NB - indexStartSwap; loopins++) {
      result[DEPOSIT_IBOR_NB + FUT_EUR3_NB + loopins] = SWAP_EUR3[SWAP_FAKE_EUR3_NB + indexStartSwap + loopins];
    }
    return result;
  }

  public static double[] marketRateForward3FutSwap() {
    int indexStartSwap = 4;
    double[] rate = new double[DEPOSIT_IBOR_NB + FUT_EUR3_NB + SWAP_EUR3_NB - indexStartSwap];
    System.arraycopy(DEPOSIT_IBOR_RATE, 0, rate, 0, DEPOSIT_IBOR_NB);
    for (int loopins = 0; loopins < FUT_EUR3_NB; loopins++) {
      rate[DEPOSIT_IBOR_NB + loopins] = 1.0 - FUT_EUR3_PRICE[loopins];
    }
    System.arraycopy(SWAP_EUR3_RATE, indexStartSwap, rate, DEPOSIT_IBOR_NB + FUT_EUR3_NB, SWAP_EUR3_NB - indexStartSwap);
    return rate;
  }

  public static InstrumentDerivative[] instrumentsInflation() {
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
