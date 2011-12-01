/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.instrument.index.priceindex;

import javax.time.calendar.Period;

import com.opengamma.financial.instrument.index.IndexPrice;
import com.opengamma.util.money.Currency;

/**
 * Class describing the Euroland HICP.
 * Harmonised indices of consumer prices (HICP) - Overall index excluding tobacco. - Euro area (EA11-2000, EA12-2006, EA13-2007, EA15-2008, EA16-2010, EA17) - code: 00XTOBAC
 */
public class EurolandHicpXT extends IndexPrice {

  public EurolandHicpXT() {
    super("Euroland HICP-XT", Currency.EUR, Currency.EUR, Period.ofDays(14));
  }

}
