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
import com.opengamma.financial.interestrate.cash.derivative.Cash;
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

/**
 * Calculates the present value of instruments for a given MarketBundle (set of yield and price curves).
 * Calculator for linear instruments requiring only discounting.
 */
public final class PresentValueCurveSensitivityMarketCalculator extends AbstractInstrumentDerivativeVisitor<MarketBundle, PresentValueCurveSensitivityMarket> {

  /*
   * The unique instance of the method.
   */
  private static final PresentValueCurveSensitivityMarketCalculator INSTANCE = new PresentValueCurveSensitivityMarketCalculator();

  /*
   * Gets the method unique instance.
   */
  public static PresentValueCurveSensitivityMarketCalculator getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  private PresentValueCurveSensitivityMarketCalculator() {
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
  public PresentValueCurveSensitivityMarket visit(final InstrumentDerivative derivative, final MarketBundle market) {
    Validate.notNull(market);
    Validate.notNull(derivative);
    return derivative.accept(this, market);
  }

  @Override
  public PresentValueCurveSensitivityMarket visitCash(final Cash cash, final MarketBundle market) {
    return METHOD_CASH.presentValueCurveSensitivity(cash, market);
  }

  @Override
  public PresentValueCurveSensitivityMarket visitForwardRateAgreement(final ForwardRateAgreement fra, final MarketBundle market) {
    return METHOD_FRA.presentValueCurveSensitivity(fra, market);
  }

  @Override
  public PresentValueCurveSensitivityMarket visitFixedCouponPayment(final CouponFixed payment, final MarketBundle market) {
    return METHOD_FIXED.presentValueCurveSensitivity(payment, market);
  }

  @Override
  public PresentValueCurveSensitivityMarket visitCouponIbor(final CouponIbor payment, final MarketBundle market) {
    return METHOD_IBOR.presentValueCurveSensitivity(payment, market);
  }

  @Override
  //TOTO: remove when not necessary anymore.
  public PresentValueCurveSensitivityMarket visitCouponIborFixed(final CouponIborFixed payment, final MarketBundle market) {
    return visitFixedCouponPayment(payment, market);
  }

  @Override
  public PresentValueCurveSensitivityMarket visitCouponOIS(final CouponOIS payment, final MarketBundle market) {
    return METHOD_OIS.presentValueCurveSensitivity(payment, market);
  }

  @Override
  public PresentValueCurveSensitivityMarket visitGenericAnnuity(final GenericAnnuity<? extends Payment> annuity, final MarketBundle market) {
    Validate.notNull(annuity);
    Validate.notNull(market);
    PresentValueCurveSensitivityMarket pvcs = new PresentValueCurveSensitivityMarket();
    for (final Payment p : annuity.getPayments()) {
      pvcs = PresentValueCurveSensitivityMarket.plus(pvcs, visit(p, market));
    }
    return pvcs;
  }

  @Override
  public PresentValueCurveSensitivityMarket visitFixedCouponAnnuity(final AnnuityCouponFixed annuity, final MarketBundle market) {
    return visitGenericAnnuity(annuity, market);
  }

  @Override
  public PresentValueCurveSensitivityMarket visitSwap(final Swap<?, ?> swap, final MarketBundle market) {
    Validate.notNull(swap);
    return PresentValueCurveSensitivityMarket.plus(visit(swap.getFirstLeg(), market), visit(swap.getSecondLeg(), market));
  }

  @Override
  public PresentValueCurveSensitivityMarket visitFixedCouponSwap(final FixedCouponSwap<?> swap, final MarketBundle market) {
    return visitSwap(swap, market);
  }

}
