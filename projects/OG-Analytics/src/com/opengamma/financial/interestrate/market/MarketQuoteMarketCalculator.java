/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.interestrate.AbstractInstrumentDerivativeVisitor;
import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.cash.derivative.Cash;
import com.opengamma.financial.interestrate.cash.derivative.DepositIbor;
import com.opengamma.financial.interestrate.cash.market.CashDiscountingMarketMethod;
import com.opengamma.financial.interestrate.cash.market.DepositIborDiscountingMarketMethod;
import com.opengamma.financial.interestrate.fra.ForwardRateAgreement;
import com.opengamma.financial.interestrate.fra.market.ForwardRateAgreementDiscountingMarketMethod;
import com.opengamma.financial.interestrate.future.definition.InterestRateFuture;
import com.opengamma.financial.interestrate.future.market.InterestRateFutureDiscountingMarketMethod;
import com.opengamma.financial.interestrate.swap.definition.FixedCouponSwap;

/**
 * Calculator for the "market quote" of different instrument. 
 * <P>The market quote will have different meaning for different instruments.
 * <P>For deposit, FRA and swap it is the forward rate, for futures it is the price, for bonds it is the clean price,...
 */
public class MarketQuoteMarketCalculator extends AbstractInstrumentDerivativeVisitor<MarketBundle, Double> {

  /**
   * The unique instance of the method.
   */
  private static final MarketQuoteMarketCalculator INSTANCE = new MarketQuoteMarketCalculator();

  /**
   * Return the unique instance of the class.
   * @return The instance.
   */
  public static MarketQuoteMarketCalculator getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  protected MarketQuoteMarketCalculator() {
  }

  /**
   * The methods used by the different instruments.
   */
  private static final CashDiscountingMarketMethod METHOD_CASH = CashDiscountingMarketMethod.getInstance();
  private static final DepositIborDiscountingMarketMethod METHOD_DEPO_IBOR = DepositIborDiscountingMarketMethod.getInstance();
  private static final ForwardRateAgreementDiscountingMarketMethod METHOD_FRA = ForwardRateAgreementDiscountingMarketMethod.getInstance();

  private static final InterestRateFutureDiscountingMarketMethod METHOD_FUT = InterestRateFutureDiscountingMarketMethod.getInstance();

  private static final PresentValueMarketCalculator PVC = PresentValueMarketCalculator.getInstance();

  @Override
  public Double[] visit(final InstrumentDerivative[] derivative, final MarketBundle market) {
    Validate.notNull(derivative, "derivative");
    Validate.noNullElements(derivative, "derivative");
    Validate.notNull(market, "Market");
    final Double[] output = new Double[derivative.length];
    for (int loopderivative = 0; loopderivative < derivative.length; loopderivative++) {
      output[loopderivative] = derivative[loopderivative].accept(this, market);
    }
    return output;
  }

  @Override
  public Double visitCash(final Cash deposit, final MarketBundle market) {
    return METHOD_CASH.parRate(deposit, market);
  }

  @Override
  public Double visitDepositIbor(final DepositIbor deposit, final MarketBundle market) {
    return METHOD_DEPO_IBOR.parRate(deposit, market);
  }

  @Override
  public Double visitForwardRateAgreement(final ForwardRateAgreement fra, final MarketBundle market) {
    return METHOD_FRA.parRate(fra, market);
  }

  @Override
  /**
   * The market quote for interest futures is the price (without convexity adjustment).
   * @param future The interest rate future.
   * @param market The market curves.
   * @return The future price.
   */
  public Double visitInterestRateFuture(final InterestRateFuture future, final MarketBundle market) {
    return METHOD_FUT.price(future, market);
  }

  /**
   * Computes the par rate of a swap with one fixed leg. 
   * Computed as the fixed leg with unit coupon pv divided by the other leg pv.
   * @param swap The Fixed coupon swap.
   * @param market The market curves.
   * @return The swap par rate.
   */
  @Override
  public Double visitFixedCouponSwap(final FixedCouponSwap<?> swap, final MarketBundle market) {
    final double pvSecond = PVC.visit(swap.getSecondLeg(), market).getAmount(swap.getSecondLeg().getCurrency());
    final double pvbp = PVC.visit(swap.getFixedLeg().withUnitCoupon(), market).getAmount(swap.getFixedLeg().getCurrency());
    return -pvSecond / pvbp;
  }

}
