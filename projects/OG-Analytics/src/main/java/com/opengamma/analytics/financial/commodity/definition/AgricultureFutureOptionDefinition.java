/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.analytics.financial.commodity.definition;

import javax.time.calendar.ZonedDateTime;

import com.opengamma.analytics.financial.ExerciseDecisionType;
import com.opengamma.analytics.financial.commodity.derivative.AgricultureFutureOption;
import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.instrument.InstrumentDefinitionVisitor;
import com.opengamma.analytics.util.time.TimeCalculator;
import com.opengamma.util.ArgumentChecker;

/**
 * Agriculture future options definition
 */
public class AgricultureFutureOptionDefinition extends CommodityFutureOptionDefinition implements InstrumentDefinition<AgricultureFutureOption> {

  /**
   * Constructor for option
   *
   * @param expiryDate is the time and the day that a particular delivery month of a futures contract stops trading, as well as the final settlement price for that contract.
   * @param underlying Underlying future
   * @param strike Strike price
   * @param exerciseType Exercise type - European or American
   * @param isCall Call if true, Put if false
   */
  public AgricultureFutureOptionDefinition(final ZonedDateTime expiryDate, final CommodityFutureDefinition underlying, final double strike,
      final ExerciseDecisionType exerciseType, final boolean isCall) {
    super(expiryDate, underlying, strike, exerciseType, isCall);
  }

  /**
   * Get the derivative at a given fix time from the definition
   * @param date fixing time
   * @return the fixed derivative
   */
  @Override
  public AgricultureFutureOption toDerivative(final ZonedDateTime date, final String... yieldCurveNames) {
    ArgumentChecker.inOrderOrEqual(date, this.getExpiryDate(), "date", "expiry date");
    double timeToFixing = TimeCalculator.getTimeBetween(date, this.getExpiryDate());
    return new AgricultureFutureOption(timeToFixing, getUnderlying(), getStrike(), getExerciseType(), isCall());
  }

  @Override
  public <U, V> V accept(final InstrumentDefinitionVisitor<U, V> visitor, final U data) {
    return visitor.visitAgricultureFutureOptionDefinition(this, data);
  }

  @Override
  public <V> V accept(final InstrumentDefinitionVisitor<?, V> visitor) {
    return visitor.visitAgricultureFutureOptionDefinition(this);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AgricultureFutureOptionDefinition)) {
      return false;
    }
    return super.equals(obj);
  }


}
