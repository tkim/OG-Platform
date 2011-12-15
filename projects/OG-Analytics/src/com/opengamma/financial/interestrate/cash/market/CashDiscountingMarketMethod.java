/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.cash.market;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.cash.derivative.Cash;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.PresentValueCurveSensitivityMarket;
import com.opengamma.financial.interestrate.method.PricingMarketMethod;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.tuple.DoublesPair;

/**
 * Method to compute present value and present value sensitivity for loans/deposits.
 */
public final class CashDiscountingMarketMethod implements PricingMarketMethod {

  /**
   * The unique instance of the method.
   */
  private static final CashDiscountingMarketMethod INSTANCE = new CashDiscountingMarketMethod();

  /**
   * Return the unique instance of the class.
   * @return The instance.
   */
  public static CashDiscountingMarketMethod getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  private CashDiscountingMarketMethod() {
  }

  /**
   * Computes the present value of a loan/deposit y discounting the initial and final cash flows.
   * @param deposit The loan/deposit.
   * @param market The market.
   * @return The present value.
   */
  public MultipleCurrencyAmount presentValue(final Cash deposit, final MarketBundle market) {
    Validate.notNull(deposit, "Deposit");
    Validate.notNull(market, "Market");
    final double dfStart = market.getDiscountingFactor(deposit.getCurrency(), deposit.getStartTime());
    final double dfEnd = market.getDiscountingFactor(deposit.getCurrency(), deposit.getEndTime());
    final double pv = -deposit.getInitialAmount() * dfStart + (deposit.getNotional() + deposit.getInterestAmount()) * dfEnd;
    return MultipleCurrencyAmount.of(deposit.getCurrency(), pv);
  }

  @Override
  public MultipleCurrencyAmount presentValue(final InstrumentDerivative instrument, final MarketBundle market) {
    Validate.isTrue(instrument instanceof Cash, "Coupon Fixed");
    return presentValue((Cash) instrument, market);
  }

  /**
   * Compute the present value sensitivity to rates of a cash loan/deposit by discounting.
   * @param deposit The loan/deposit.
   * @param market The market curves.
   * @return The present value sensitivity.
   */
  public PresentValueCurveSensitivityMarket presentValueCurveSensitivity(final Cash deposit, final MarketBundle market) {
    Validate.notNull(deposit, "Deposit");
    Validate.notNull(market, "Market");
    final double dfStart = market.getDiscountingFactor(deposit.getCurrency(), deposit.getStartTime());
    final double dfEnd = market.getDiscountingFactor(deposit.getCurrency(), deposit.getEndTime());
    final double finalAmount = deposit.getNotional() + deposit.getInterestAmount();
    final double pvBar = 1.0;
    final double dfEndBar = finalAmount * pvBar;
    final double dfStartBar = -deposit.getInitialAmount() * pvBar;
    final Map<String, List<DoublesPair>> result = new HashMap<String, List<DoublesPair>>();
    final List<DoublesPair> listDiscounting = new ArrayList<DoublesPair>();
    listDiscounting.add(new DoublesPair(deposit.getStartTime(), -deposit.getStartTime() * dfStart * dfStartBar));
    listDiscounting.add(new DoublesPair(deposit.getEndTime(), -deposit.getEndTime() * dfEnd * dfEndBar));
    result.put(market.getCurve(deposit.getCurrency()).getCurve().getName(), listDiscounting);
    return new PresentValueCurveSensitivityMarket(result);
  }

}
