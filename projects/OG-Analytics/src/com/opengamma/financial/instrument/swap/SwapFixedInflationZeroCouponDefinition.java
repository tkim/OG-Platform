/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.instrument.swap;

import javax.time.calendar.Period;
import javax.time.calendar.ZonedDateTime;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.convention.businessday.BusinessDayConvention;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.instrument.annuity.AnnuityCouponFixedDefinition;
import com.opengamma.financial.instrument.annuity.AnnuityDefinition;
import com.opengamma.financial.instrument.index.IndexPrice;
import com.opengamma.financial.instrument.inflation.CouponInflationDefinition;
import com.opengamma.financial.instrument.inflation.CouponInflationZeroCouponInterpolationDefinition;
import com.opengamma.financial.instrument.payment.CouponDefinition;
import com.opengamma.financial.instrument.payment.CouponFixedDefinition;
import com.opengamma.financial.instrument.payment.PaymentDefinition;
import com.opengamma.financial.interestrate.annuity.definition.GenericAnnuity;
import com.opengamma.financial.interestrate.payments.Coupon;
import com.opengamma.financial.interestrate.payments.CouponFixed;
import com.opengamma.financial.interestrate.swap.definition.FixedCouponSwap;
import com.opengamma.financial.schedule.ScheduleCalculator;
import com.opengamma.util.timeseries.DoubleTimeSeries;

/**
 * Class describing a zero-coupon inflation (with interpolated index) swap.
 */
public class SwapFixedInflationZeroCouponDefinition extends SwapDefinition {

  /**
   * Zero-coupon inflation swap constructor.
   * @param fixedLeg The swap fixed leg.
   * @param inflationLeg The swap inflation leg.
   */
  public SwapFixedInflationZeroCouponDefinition(final AnnuityCouponFixedDefinition fixedLeg, final AnnuityDefinition<? extends CouponDefinition> inflationLeg) {
    super(fixedLeg, inflationLeg);
  }

  /**
   * Zero-coupon inflation swap constructor for the fixed and inflation coupons.
   * @param fixedCpn The swap fixed leg.
   * @param inflationCpn The swap inflation leg.
   */
  public SwapFixedInflationZeroCouponDefinition(final CouponFixedDefinition fixedCpn, final CouponInflationDefinition inflationCpn) {
    super(new AnnuityCouponFixedDefinition(new CouponFixedDefinition[] {fixedCpn}), new AnnuityDefinition<PaymentDefinition>(new CouponInflationDefinition[] {inflationCpn}));
  }

  /**
   * Builder from financial details and the time series of exiting price index values.
   * @param index The price index.
   * @param settlementDate The swap settlement date.
   * @param tenor The swap tenor in years.
   * @param fixedRate The swap fixed rate (annual compounding). The fixed payment is (1+fixedRate)^tenor-1.
   * @param notional The swap notional.
   * @param isPayer The flag 
   * @param businessDayConvention The business day convention used to compute the payment date.
   * @param calendar The calendar used to compute the payment date.
   * @param endOfMonth The end-of-month convention used to compute the payment date.
   * @param monthLag The price index fixing lag in months.
   * @param priceIndexTimeSeries The time series with the relevant price index values.
   * @return The zero coupon inflation swap.
   */
  public static SwapFixedInflationZeroCouponDefinition fromInterpolation(final IndexPrice index, final ZonedDateTime settlementDate, int tenor, double fixedRate, double notional,
      final boolean isPayer, final BusinessDayConvention businessDayConvention, final Calendar calendar, final boolean endOfMonth, final int monthLag,
      final DoubleTimeSeries<ZonedDateTime> priceIndexTimeSeries) {
    Validate.notNull(index, "Price index");
    Validate.notNull(settlementDate, "Settlement date");
    Validate.notNull(businessDayConvention, "Business day convention");
    Validate.notNull(calendar, "Calendar");
    Validate.notNull(priceIndexTimeSeries, "Time series of price index");
    double rateComposed = Math.pow(1 + fixedRate, tenor) - 1;
    ZonedDateTime paymentDate = ScheduleCalculator.getAdjustedDate(settlementDate, Period.ofYears(tenor), businessDayConvention, calendar, endOfMonth);
    CouponFixedDefinition fixedCpn = CouponFixedDefinition.from(index.getCurrency(), paymentDate, settlementDate, paymentDate, 1.0, (isPayer ? -1.0 : 1.0) * notional, rateComposed);
    CouponInflationZeroCouponInterpolationDefinition inflationCpn = CouponInflationZeroCouponInterpolationDefinition.from(settlementDate, paymentDate, (isPayer ? 1.0 : -1.0) * notional, index,
        priceIndexTimeSeries, monthLag, false);
    return new SwapFixedInflationZeroCouponDefinition(fixedCpn, inflationCpn);
  }

  /**
   * The fixed leg of the swap.
   * @return Fixed leg.
   */
  public AnnuityCouponFixedDefinition getFixedLeg() {
    return (AnnuityCouponFixedDefinition) getFirstLeg();
  }

  @Override
  public FixedCouponSwap<Coupon> toDerivative(final ZonedDateTime date, final DoubleTimeSeries<ZonedDateTime>[] indexDataTS, final String... yieldCurveNames) {
    Validate.notNull(indexDataTS, "index data time series array");
    Validate.isTrue(indexDataTS.length > 0, "index data time series must contain at least one element");
    final GenericAnnuity<CouponFixed> fixedLeg = this.getFixedLeg().toDerivative(date, yieldCurveNames);
    @SuppressWarnings("unchecked")
    final GenericAnnuity<Coupon> inflLeg = (GenericAnnuity<Coupon>) this.getSecondLeg().toDerivative(date, indexDataTS[0], yieldCurveNames);
    return new FixedCouponSwap<Coupon>(fixedLeg, inflLeg);
  }

}
