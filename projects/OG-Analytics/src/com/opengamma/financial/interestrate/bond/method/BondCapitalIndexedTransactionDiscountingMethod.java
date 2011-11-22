/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.bond.method;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.bond.definition.BondCapitalIndexedTransaction;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.PresentValueMarketCalculator;
import com.opengamma.financial.interestrate.method.PricingMarketMethod;
import com.opengamma.financial.interestrate.payments.Coupon;
import com.opengamma.util.money.MultipleCurrencyAmount;

/**
 * Pricing method for inflation bond transaction. The price is computed by index estimation and discounting.
 */
public final class BondCapitalIndexedTransactionDiscountingMethod implements PricingMarketMethod {

  /**
   * The present value inflation calculator (for the different parts of the bond transaction).
   */
  private static final PresentValueMarketCalculator PVIC = PresentValueMarketCalculator.getInstance();
  /**
   * The method used for security computation.
   */
  private static final BondCapitalIndexedSecurityDiscountingMethod METHOD_SECURITY = new BondCapitalIndexedSecurityDiscountingMethod();

  /**
   * Computes the present value of a capital indexed bound transaction by index estimation and discounting.
   * @param bond The bond transaction.
   * @param market The market.
   * @return The present value.
   */
  public MultipleCurrencyAmount presentValue(final BondCapitalIndexedTransaction<?> bond, final MarketBundle market) {
    final MultipleCurrencyAmount pvBond = PVIC.visit(bond.getBondTransaction(), market);
    MultipleCurrencyAmount pvSettlement = PVIC.visit(bond.getBondTransaction().getSettlement(), market).multipliedBy(
        bond.getQuantity() * bond.getBondTransaction().getCoupon().getNthPayment(0).getNotional());
    return pvBond.multipliedBy(bond.getQuantity()).plus(pvSettlement);
  }

  @Override
  public MultipleCurrencyAmount presentValue(final InstrumentDerivative instrument, final MarketBundle market) {
    Validate.isTrue(instrument instanceof BondCapitalIndexedTransaction<?>, "Capital inflation indexed bond.");
    return presentValue((BondCapitalIndexedTransaction<?>) instrument, market);
  }

  /**
   * Computes the security present value from a quoted clean real price.
   * @param bond The bond transaction.
   * @param market The market.
   * @param cleanPriceReal The clean price.
   * @return The present value.
   */
  public MultipleCurrencyAmount presentValueFromCleanPriceReal(final BondCapitalIndexedTransaction<Coupon> bond, final MarketBundle market, final double cleanPriceReal) {
    Validate.notNull(bond, "Coupon");
    Validate.notNull(market, "Market");
    MultipleCurrencyAmount pvBond = METHOD_SECURITY.presentValueFromCleanPriceReal(bond.getBondTransaction(), market, cleanPriceReal);
    MultipleCurrencyAmount pvSettlement = PVIC.visit(bond.getBondTransaction().getSettlement(), market).multipliedBy(
        bond.getQuantity() * bond.getBondTransaction().getCoupon().getNthPayment(0).getNotional());
    return pvBond.plus(pvSettlement);
  }

  // TODO: curve sensitivity
  // TODO: price index sensitivity

}
