/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.instrument.index.iborindex;

import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.daycount.DayCountFactory;
import com.opengamma.financial.instrument.index.IndexOIS;
import com.opengamma.util.money.Currency;

/**
 * Class describing the Eonia (EUR overnight) index.
 */
public class Eonia extends IndexOIS {

  /**
   * Constructor.
   * @param calendar A EUR calendar.
   */
  public Eonia(final Calendar calendar) {
    super("Eonia", Currency.EUR, DayCountFactory.INSTANCE.getDayCount("Actual/360"), 0, calendar);
  }

}
