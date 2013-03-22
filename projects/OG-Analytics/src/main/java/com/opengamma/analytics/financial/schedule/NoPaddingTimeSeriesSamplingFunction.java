/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.analytics.financial.schedule;

import java.util.ArrayList;
import java.util.List;

import org.threeten.bp.LocalDate;

import com.opengamma.timeseries.DoubleTimeSeries;
import com.opengamma.timeseries.localdate.ArrayLocalDateDoubleTimeSeries;
import com.opengamma.timeseries.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.ArgumentChecker;

/**
 * 
 */
public class NoPaddingTimeSeriesSamplingFunction implements TimeSeriesSamplingFunction {

  @Override
  public DoubleTimeSeries<?> getSampledTimeSeries(final DoubleTimeSeries<?> ts, final LocalDate[] schedule) {
    ArgumentChecker.notNull(ts, "time series");
    ArgumentChecker.notNull(schedule, "schedule");
    final LocalDateDoubleTimeSeries localDateTS = ts.toLocalDateDoubleTimeSeries();
    final List<LocalDate> tsDates = localDateTS.times();
    final List<LocalDate> scheduledDates = new ArrayList<>();
    final List<Double> scheduledData = new ArrayList<>();
    for (final LocalDate localDate : schedule) {
      if (tsDates.contains(localDate)) {
        scheduledDates.add(localDate);
        scheduledData.add(localDateTS.getValue(localDate));
      }
    }
    return new ArrayLocalDateDoubleTimeSeries(scheduledDates, scheduledData);
  }

}
