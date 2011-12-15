/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.interestrate.AbstractInstrumentDerivativeVisitor;
import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.annuity.definition.AnnuityCouponFixed;
import com.opengamma.financial.interestrate.annuity.definition.GenericAnnuity;
import com.opengamma.financial.interestrate.bond.definition.BondCapitalIndexedSecurity;
import com.opengamma.financial.interestrate.bond.definition.BondCapitalIndexedTransaction;
import com.opengamma.financial.interestrate.cash.derivative.Cash;
import com.opengamma.financial.interestrate.cash.market.CashDiscountingMarketMethod;
import com.opengamma.financial.interestrate.fra.ForwardRateAgreement;
import com.opengamma.financial.interestrate.fra.market.ForwardRateAgreementDiscountingMarketMethod;
import com.opengamma.financial.interestrate.future.definition.InterestRateFuture;
import com.opengamma.financial.interestrate.future.market.InterestRateFutureDiscountingMarketMethod;
import com.opengamma.financial.interestrate.inflation.derivatives.CouponInflationZeroCouponInterpolation;
import com.opengamma.financial.interestrate.inflation.derivatives.CouponInflationZeroCouponInterpolationGearing;
import com.opengamma.financial.interestrate.inflation.derivatives.CouponInflationZeroCouponMonthly;
import com.opengamma.financial.interestrate.inflation.derivatives.CouponInflationZeroCouponMonthlyGearing;
import com.opengamma.financial.interestrate.inflation.method.CouponInflationZeroCouponInterpolationDiscountingMethod;
import com.opengamma.financial.interestrate.inflation.method.CouponInflationZeroCouponInterpolationGearingDiscountingMethod;
import com.opengamma.financial.interestrate.inflation.method.CouponInflationZeroCouponMonthlyDiscountingMethod;
import com.opengamma.financial.interestrate.inflation.method.CouponInflationZeroCouponMonthlyGearingDiscountingMethod;
import com.opengamma.financial.interestrate.payments.CouponFixed;
import com.opengamma.financial.interestrate.payments.CouponIbor;
import com.opengamma.financial.interestrate.payments.CouponIborFixed;
import com.opengamma.financial.interestrate.payments.Payment;
import com.opengamma.financial.interestrate.payments.derivative.CouponOIS;
import com.opengamma.financial.interestrate.payments.market.CouponFixedDiscountingMarketMethod;
import com.opengamma.financial.interestrate.payments.market.CouponIborDiscountingMarketMethod;
import com.opengamma.financial.interestrate.payments.market.CouponOISDiscountingMarketMethod;
import com.opengamma.financial.interestrate.swap.definition.FixedCouponSwap;
import com.opengamma.financial.interestrate.swap.definition.Swap;
import com.opengamma.util.money.MultipleCurrencyAmount;

/**
 * Calculates the present value of instruments for a given MarketBundle (set of yield and price curves).
 * Calculator for linear instruments requiring only discounting.
 */
public class PresentValueMarketCalculator extends AbstractInstrumentDerivativeVisitor<MarketBundle, MultipleCurrencyAmount> {

  /**
   * The unique instance of the method.
   */
  private static final PresentValueMarketCalculator INSTANCE = new PresentValueMarketCalculator();

  /**
   * Return the unique instance of the class.
   * @return The instance.
   */
  public static PresentValueMarketCalculator getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  protected PresentValueMarketCalculator() {
  }

  /**
   * The methods used by the different instruments.
   */
  private static final CashDiscountingMarketMethod METHOD_CASH = CashDiscountingMarketMethod.getInstance();
  private static final ForwardRateAgreementDiscountingMarketMethod METHOD_FRA = ForwardRateAgreementDiscountingMarketMethod.getInstance();
  private static final CouponIborDiscountingMarketMethod METHOD_IBOR = CouponIborDiscountingMarketMethod.getInstance();
  private static final CouponFixedDiscountingMarketMethod METHOD_FIXED = CouponFixedDiscountingMarketMethod.getInstance();
  private static final CouponOISDiscountingMarketMethod METHOD_OIS = CouponOISDiscountingMarketMethod.getInstance();
  private static final InterestRateFutureDiscountingMarketMethod METHOD_FUT = InterestRateFutureDiscountingMarketMethod.getInstance();

  private static final CouponInflationZeroCouponMonthlyDiscountingMethod METHOD_ZC_MONTHLY = new CouponInflationZeroCouponMonthlyDiscountingMethod();
  private static final CouponInflationZeroCouponInterpolationDiscountingMethod METHOD_ZC_INTERPOLATION = CouponInflationZeroCouponInterpolationDiscountingMethod.getInstance();
  private static final CouponInflationZeroCouponMonthlyGearingDiscountingMethod METHOD_ZC_MONTHLY_GEARING = new CouponInflationZeroCouponMonthlyGearingDiscountingMethod();
  private static final CouponInflationZeroCouponInterpolationGearingDiscountingMethod METHOD_ZC_INTERPOLATION_GEARING = new CouponInflationZeroCouponInterpolationGearingDiscountingMethod();

  @Override
  public MultipleCurrencyAmount visit(final InstrumentDerivative derivative, final MarketBundle market) {
    Validate.notNull(market);
    Validate.notNull(derivative);
    return derivative.accept(this, market);
  }

  @Override
  public MultipleCurrencyAmount visitCash(final Cash cash, final MarketBundle market) {
    return METHOD_CASH.presentValue(cash, market);
  }

  @Override
  public MultipleCurrencyAmount visitForwardRateAgreement(final ForwardRateAgreement fra, final MarketBundle market) {
    return METHOD_FRA.presentValue(fra, market);
  }

  @Override
  public MultipleCurrencyAmount visitFixedCouponPayment(final CouponFixed payment, final MarketBundle market) {
    return METHOD_FIXED.presentValue(payment, market);
  }

  @Override
  public MultipleCurrencyAmount visitCouponIbor(final CouponIbor payment, final MarketBundle market) {
    return METHOD_IBOR.presentValue(payment, market);
  }

  @Override
  //TOTO: remove when not necessary anymore.
  public MultipleCurrencyAmount visitCouponIborFixed(final CouponIborFixed payment, final MarketBundle market) {
    return visitFixedCouponPayment(payment, market);
  }

  @Override
  public MultipleCurrencyAmount visitCouponOIS(final CouponOIS payment, final MarketBundle market) {
    return METHOD_OIS.presentValue(payment, market);
  }

  @Override
  public MultipleCurrencyAmount visitGenericAnnuity(final GenericAnnuity<? extends Payment> annuity, final MarketBundle market) {
    Validate.notNull(annuity);
    Validate.notNull(market);
    MultipleCurrencyAmount pv = MultipleCurrencyAmount.of(annuity.getCurrency(), 0.0);
    for (final Payment p : annuity.getPayments()) {
      pv = pv.plus(visit(p, market));
    }
    return pv;
  }

  @Override
  public MultipleCurrencyAmount visitFixedCouponAnnuity(final AnnuityCouponFixed annuity, final MarketBundle market) {
    return visitGenericAnnuity(annuity, market);
  }

  @Override
  public MultipleCurrencyAmount visitSwap(final Swap<?, ?> swap, final MarketBundle market) {
    Validate.notNull(swap);
    return visit(swap.getFirstLeg(), market).plus(visit(swap.getSecondLeg(), market));
  }

  @Override
  public MultipleCurrencyAmount visitFixedCouponSwap(final FixedCouponSwap<?> swap, final MarketBundle market) {
    return visitSwap(swap, market);
  }

  @Override
  public MultipleCurrencyAmount visitInterestRateFuture(final InterestRateFuture future, final MarketBundle market) {
    return METHOD_FUT.presentValue(future, market);
  }

  @Override
  public MultipleCurrencyAmount visitCouponInflationZeroCouponMonthly(final CouponInflationZeroCouponMonthly coupon, final MarketBundle market) {
    return METHOD_ZC_MONTHLY.presentValue(coupon, market);
  }

  @Override
  public MultipleCurrencyAmount visitCouponInflationZeroCouponInterpolation(final CouponInflationZeroCouponInterpolation coupon, final MarketBundle market) {
    return METHOD_ZC_INTERPOLATION.presentValue(coupon, market);
  }

  @Override
  public MultipleCurrencyAmount visitCouponInflationZeroCouponMonthlyGearing(final CouponInflationZeroCouponMonthlyGearing coupon, final MarketBundle market) {
    return METHOD_ZC_MONTHLY_GEARING.presentValue(coupon, market);
  }

  @Override
  public MultipleCurrencyAmount visitCouponInflationZeroCouponInterpolationGearing(final CouponInflationZeroCouponInterpolationGearing coupon, final MarketBundle market) {
    return METHOD_ZC_INTERPOLATION_GEARING.presentValue(coupon, market);
  }

  @Override
  public MultipleCurrencyAmount visitBondCapitalIndexedSecurity(final BondCapitalIndexedSecurity<?> bond, final MarketBundle market) {
    Validate.notNull(bond, "Bond");
    MarketBundle creditDiscounting = new MarketDiscountingDecorated(market, bond.getCurrency(), market.getCurve(bond.getIssuer()));
    final MultipleCurrencyAmount pvNominal = visit(bond.getNominal(), creditDiscounting);
    final MultipleCurrencyAmount pvCoupon = visit(bond.getCoupon(), creditDiscounting);
    return pvNominal.plus(pvCoupon);
  }

  @Override
  public MultipleCurrencyAmount visitBondCapitalIndexedTransaction(final BondCapitalIndexedTransaction<?> bond, final MarketBundle market) {
    Validate.notNull(bond, "Bond");
    final MultipleCurrencyAmount pvBond = visit(bond.getBondTransaction(), market);
    MultipleCurrencyAmount pvSettlement = visit(bond.getBondTransaction().getSettlement(), market).multipliedBy(
        bond.getQuantity() * bond.getBondTransaction().getCoupon().getNthPayment(0).getNotional());
    return pvBond.multipliedBy(bond.getQuantity()).plus(pvSettlement);
  }

}
