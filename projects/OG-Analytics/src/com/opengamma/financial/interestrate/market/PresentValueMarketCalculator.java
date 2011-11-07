/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.interestrate.AbstractInterestRateDerivativeVisitor;
import com.opengamma.financial.interestrate.InterestRateDerivative;
import com.opengamma.financial.interestrate.annuity.definition.AnnuityCouponFixed;
import com.opengamma.financial.interestrate.annuity.definition.GenericAnnuity;
import com.opengamma.financial.interestrate.cash.definition.Cash;
import com.opengamma.financial.interestrate.cash.market.CashDiscountingMarketMethod;
import com.opengamma.financial.interestrate.fra.ForwardRateAgreement;
import com.opengamma.financial.interestrate.fra.market.ForwardRateAgreementDiscountingMarketMethod;
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
import com.opengamma.util.money.CurrencyAmount;


/**
 * Calculates the present value of instruments for a given MarketBundle (set of yield and price curves).
 * Calculator for linear instruments requiring only discounting.
 */
public final class PresentValueMarketCalculator extends AbstractInterestRateDerivativeVisitor<MarketBundle, CurrencyAmount> {

  /*
   * The unique instance of the method.
   */
  private static final PresentValueMarketCalculator INSTANCE = new PresentValueMarketCalculator();

  /*
   * Gets the method unique instance.
   */
  public static PresentValueMarketCalculator getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  private PresentValueMarketCalculator() {
  }

  /**
   * The methods used by the different instruments.
   */
  private static final CashDiscountingMarketMethod METHOD_CASH = CashDiscountingMarketMethod.getInstance();
  private static final ForwardRateAgreementDiscountingMarketMethod METHOD_FRA = ForwardRateAgreementDiscountingMarketMethod.getInstance();
  private static final CouponIborDiscountingMarketMethod METHOD_IBOR = CouponIborDiscountingMarketMethod.getInstance();
  private static final CouponFixedDiscountingMarketMethod METHOD_FIXED = CouponFixedDiscountingMarketMethod.getInstance();
  private static final CouponOISDiscountingMarketMethod METHOD_OIS = CouponOISDiscountingMarketMethod.getInstance();

  @Override
  public CurrencyAmount visit(final InterestRateDerivative derivative, final MarketBundle market) {
    Validate.notNull(market);
    Validate.notNull(derivative);
    return derivative.accept(this, market);
  }

  @Override
  public CurrencyAmount visitCash(final Cash cash, final MarketBundle market) {
    return METHOD_CASH.presentValue(cash, market);
  }

  @Override
  public CurrencyAmount visitForwardRateAgreement(final ForwardRateAgreement fra, final MarketBundle market) {
    return METHOD_FRA.presentValue(fra, market);
  }

  @Override
  public CurrencyAmount visitFixedCouponPayment(final CouponFixed payment, final MarketBundle market) {
    return METHOD_FIXED.presentValue(payment, market);
  }

  @Override
  public CurrencyAmount visitCouponIbor(final CouponIbor payment, final MarketBundle market) {
    return METHOD_IBOR.presentValue(payment, market);
  }

  @Override
  //TOTO: remove when not necessary anymore.
  public CurrencyAmount visitCouponIborFixed(final CouponIborFixed payment, final MarketBundle market) {
    return visitFixedCouponPayment(payment, market);
  }

  @Override
  public CurrencyAmount visitCouponOIS(final CouponOIS payment, final MarketBundle market) {
    return METHOD_OIS.presentValue(payment, market);
  }

  @Override
  public CurrencyAmount visitGenericAnnuity(final GenericAnnuity<? extends Payment> annuity, final MarketBundle market) {
    Validate.notNull(annuity);
    Validate.notNull(market);
    CurrencyAmount pv = CurrencyAmount.of(annuity.getCurrency(), 0.0);
    for (final Payment p : annuity.getPayments()) {
      pv = pv.plus(visit(p, market));
    }
    return pv;
  }

  @Override
  public CurrencyAmount visitFixedCouponAnnuity(final AnnuityCouponFixed annuity, final MarketBundle market) {
    return visitGenericAnnuity(annuity, market);
  }

  @Override
  public CurrencyAmount visitSwap(final Swap<?, ?> swap, final MarketBundle market) {
    Validate.notNull(swap);
    return visit(swap.getFirstLeg(), market).plus(visit(swap.getSecondLeg(), market));
  }

  @Override
  public CurrencyAmount visitFixedCouponSwap(final FixedCouponSwap<?> swap, final MarketBundle market) {
    return visitSwap(swap, market);
  }

}
