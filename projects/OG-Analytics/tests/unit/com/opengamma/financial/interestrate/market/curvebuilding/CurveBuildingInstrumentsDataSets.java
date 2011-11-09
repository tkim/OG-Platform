/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market.curvebuilding;

import javax.time.calendar.Period;
import javax.time.calendar.ZonedDateTime;

import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.financial.instrument.Convention;
import com.opengamma.financial.instrument.cash.CashDefinition;
import com.opengamma.financial.instrument.index.IborIndex;
import com.opengamma.financial.instrument.index.IndexOIS;
import com.opengamma.financial.instrument.index.iborindex.Eonia;
import com.opengamma.financial.instrument.index.iborindex.Euribor3M;
import com.opengamma.financial.instrument.index.iborindex.Euribor6M;
import com.opengamma.financial.instrument.swap.SwapFixedOISSimplifiedDefinition;
import com.opengamma.financial.schedule.ScheduleCalculator;
import com.opengamma.util.money.Currency;
import com.opengamma.util.time.DateUtils;

/**
 * To create different instrument sets for curve construction testing.
 */
public class CurveBuildingInstrumentsDataSets {

  private static final Calendar CALENDAR_EUR = new MondayToFridayCalendar("EUR");
  private static final IborIndex EURIBOR_3M = new Euribor3M(CALENDAR_EUR);
  private static final IborIndex EURIBOR_6M = new Euribor6M(CALENDAR_EUR);
  private static final IndexOIS EONIA = new Eonia(CALENDAR_EUR);
  private static final Currency EUR = EURIBOR_3M.getCurrency();
  private static final int SETTLEMENT_DAYS_EUR = EURIBOR_3M.getSettlementDays();

  private static final ZonedDateTime REFERENCE_DATE = DateUtils.getUTCDate(2011, 11, 9);
  private static final double NOTIONAL_DEFAULT = 1.0;

  private static final Period[] DEPOSIT_TENOR = new Period[] {Period.ofDays(1), Period.ofDays(1)};
  private static final int[] DEPOSIT_START = new int[] {0, 1};
  private static final double[] DEPOSIT_RATE = new double[] {0.01, 0.011};
  private static final int DEPOSIT_NB = DEPOSIT_TENOR.length;
  private static final Convention CONVENTION_DEPOSIT_EUR = new Convention(SETTLEMENT_DAYS_EUR, EURIBOR_3M.getDayCount(), EURIBOR_3M.getBusinessDayConvention(), CALENDAR_EUR, "EUR Deposit Convention");
  private static final CashDefinition[] DEPOSIT_DEFINITION = new CashDefinition[DEPOSIT_NB];
  static {
    for (int loopdepo = 0; loopdepo < DEPOSIT_NB; loopdepo++) {
      ZonedDateTime startDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, CALENDAR_EUR, DEPOSIT_START[loopdepo]);
      ZonedDateTime maturityDate = ScheduleCalculator.getAdjustedDate(startDate, EURIBOR_3M.getBusinessDayConvention(), CALENDAR_EUR, EURIBOR_3M.isEndOfMonth(), DEPOSIT_TENOR[loopdepo]);
      DEPOSIT_DEFINITION[loopdepo] = new CashDefinition(EUR, maturityDate, 1.0, NOTIONAL_DEFAULT, CONVENTION_DEPOSIT_EUR);
    }
  }

  private static final Period[] SWAP_OIS_TENOR = new Period[] {Period.ofMonths(1), Period.ofMonths(3), Period.ofMonths(6), Period.ofMonths(12), Period.ofYears(2), Period.ofYears(3), Period.ofYears(5)};
  private static final double[] SWAP_OIS_RATE = new double[] {0.012, 0.013, 0.014, 0.015, 0.016, 0.017, 0.018};
  private static final int SWAP_OIS_NB = SWAP_OIS_TENOR.length;
  private static final SwapFixedOISSimplifiedDefinition[] SWAP_OIS_DEFINITION = new SwapFixedOISSimplifiedDefinition[SWAP_OIS_NB];
  static {
    for (int loopois = 0; loopois < SWAP_OIS_NB; loopois++) {
      ZonedDateTime startDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, CALENDAR_EUR, SETTLEMENT_DAYS_EUR);
      SWAP_OIS_DEFINITION[loopois] = SwapFixedOISSimplifiedDefinition.from(startDate, SWAP_OIS_TENOR[loopois], Period.ofYears(1), NOTIONAL_DEFAULT, EONIA, SWAP_OIS_RATE[loopois], true, 1,
          EURIBOR_3M.getBusinessDayConvention(), EURIBOR_3M.isEndOfMonth());
    }
  }

}
