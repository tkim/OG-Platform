/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market.curvebuilding;

import javax.time.calendar.Period;
import javax.time.calendar.ZonedDateTime;

import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.forex.definition.ForexSwapDefinition;
import com.opengamma.financial.forex.derivative.ForexSwap;
import com.opengamma.financial.instrument.index.IborIndex;
import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.schedule.ScheduleCalculator;
import com.opengamma.util.money.Currency;

/**
 * Set of instruments with Forex component for curve construction.
 */
public class CurveBuildingForexInstrumentsDataSets {

  private static final ZonedDateTime REFERENCE_DATE = CurveBuildingInstrumentsDataSets.referenceDate();
  private static final IndexDeposit[] INDEXES = CurveBuildingInstrumentsDataSets.indexes();
  private static final IborIndex EURIBOR_3M = (IborIndex) INDEXES[0];
  private static final IborIndex USDLIBOR_3M = (IborIndex) INDEXES[2];
  private static final Calendar CALENDAR_EUR = EURIBOR_3M.getCalendar();
  private static final Currency EUR = EURIBOR_3M.getCurrency();
  private static final Currency USD = USDLIBOR_3M.getCurrency();
  private static final int SETTLEMENT_DAYS_EUR = EURIBOR_3M.getSpotLag();

  private static final double NOTIONAL_DEFAULT = 1000000.0;

  private static final double EUR_USD_SPOT = 1.35205; // The FX rate used for FX instrument construction should be the same as the one used in the FXMatrix.

  // ===== FX =====
  private static final Period[] FX_SWAP_TENOR = new Period[] {Period.ofDays(1), Period.ofDays(1), Period.ofMonths(1), Period.ofMonths(3), Period.ofMonths(6), Period.ofMonths(9), Period.ofMonths(12)};
  private static final double[] FX_SWAP_PTS = new double[] {-0.12 / 10000.0, -0.11 / 10000.0, -0.065 / 10000.0, 12.97 / 10000.0, 25.02 / 10000.0, 38.32 / 10000.0, 50.00 / 10000.0}; // Quoted in bps
  private static final int FX_SWAP_NB = FX_SWAP_TENOR.length;
  private static final ForexSwapDefinition[] FX_SWAP_DEFINITION = new ForexSwapDefinition[FX_SWAP_NB];
  static {
    ZonedDateTime startDate;
    ZonedDateTime maturityDate;
    // ON and TN have a special treatment (quoted rate is spot)
    maturityDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, FX_SWAP_TENOR[0], EURIBOR_3M.getBusinessDayConvention(), CALENDAR_EUR, EURIBOR_3M.isEndOfMonth());
    FX_SWAP_DEFINITION[0] = new ForexSwapDefinition(EUR, USD, REFERENCE_DATE, maturityDate, NOTIONAL_DEFAULT, EUR_USD_SPOT - FX_SWAP_PTS[0] - FX_SWAP_PTS[1], FX_SWAP_PTS[0]);
    startDate = maturityDate;
    maturityDate = ScheduleCalculator.getAdjustedDate(startDate, FX_SWAP_TENOR[1], EURIBOR_3M.getBusinessDayConvention(), CALENDAR_EUR, EURIBOR_3M.isEndOfMonth());
    FX_SWAP_DEFINITION[1] = new ForexSwapDefinition(EUR, USD, startDate, maturityDate, NOTIONAL_DEFAULT, EUR_USD_SPOT - FX_SWAP_PTS[1], FX_SWAP_PTS[1]);
    for (int loopfx = 2; loopfx < FX_SWAP_NB; loopfx++) {
      startDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, SETTLEMENT_DAYS_EUR, CALENDAR_EUR);
      // TODO: Change the calendar - see [PLAT-1747]
      maturityDate = ScheduleCalculator.getAdjustedDate(startDate, FX_SWAP_TENOR[loopfx], EURIBOR_3M.getBusinessDayConvention(), CALENDAR_EUR, EURIBOR_3M.isEndOfMonth());
      FX_SWAP_DEFINITION[loopfx] = new ForexSwapDefinition(EUR, USD, startDate, maturityDate, NOTIONAL_DEFAULT, EUR_USD_SPOT, FX_SWAP_PTS[loopfx]);
    }
  }

  // ===== Derivatives =====
  private static final String NOT_USED = "Not used";
  private static final String[] NOT_USED_2 = new String[] {NOT_USED, NOT_USED};

  private static final ForexSwap[] FX_SWAP = new ForexSwap[FX_SWAP_NB];
  static {
    for (int loopfx = 0; loopfx < FX_SWAP_NB; loopfx++) {
      FX_SWAP[loopfx] = FX_SWAP_DEFINITION[loopfx].toDerivative(REFERENCE_DATE, NOT_USED_2);
    }
  }

  /**
   * Return a set of instruments (FX swaps) for (foreign) discounting curve construction.
   * @return The instrument set.
   */
  public static ForexSwap[] instrumentsForex() {
    return FX_SWAP;
  }

  /**
   * Return the times associated to the instrument for the curve construction.
   * @return The times.
   */
  public static double[] timeForex() {
    double[] times = new double[FX_SWAP_NB];
    for (int loopfx = 0; loopfx < FX_SWAP_NB; loopfx++) {
      times[loopfx] = FX_SWAP[loopfx].getFarLeg().getPaymentTime();
    }
    return times;
  }

}
