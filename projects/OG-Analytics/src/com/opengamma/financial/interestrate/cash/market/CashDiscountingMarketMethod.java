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

import com.opengamma.financial.interestrate.InterestRateDerivative;
import com.opengamma.financial.interestrate.cash.definition.Cash;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.PresentValueCurveSensitivityMarket;
import com.opengamma.financial.interestrate.method.PricingMarketMethod;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.tuple.DoublesPair;

/**
 * Method to compute present value and present value sensitivity for loans/deposits.
 */
public final class CashDiscountingMarketMethod implements PricingMarketMethod {

  /*
   * The unique instance of the method.
   */
  private static final CashDiscountingMarketMethod INSTANCE = new CashDiscountingMarketMethod();

  /*
   * Gets the method unique instance.
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
  public CurrencyAmount presentValue(final Cash deposit, final MarketBundle market) {
    Validate.notNull(deposit, "Deposit");
    Validate.notNull(market, "Market");
    final double dfStart = market.getDiscountingFactor(deposit.getCurrency(), deposit.getTradeTime());
    final double dfEnd = market.getDiscountingFactor(deposit.getCurrency(), deposit.getMaturity());
    final double pv = -deposit.getNotional() * dfStart + deposit.getFinalAmount() * dfEnd;
    return CurrencyAmount.of(deposit.getCurrency(), pv);
  }

  @Override
  public CurrencyAmount presentValue(final InterestRateDerivative instrument, final MarketBundle market) {
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
    final double dfStart = market.getDiscountingFactor(deposit.getCurrency(), deposit.getTradeTime());
    final double dfEnd = market.getDiscountingFactor(deposit.getCurrency(), deposit.getMaturity());
    final double finalAmount = deposit.getFinalAmount();
    final double pvBar = 1.0;
    final double dfEndBar = finalAmount * pvBar;
    final double dfStartBar = -deposit.getNotional() * pvBar;
    final Map<String, List<DoublesPair>> result = new HashMap<String, List<DoublesPair>>();
    final List<DoublesPair> listDiscounting = new ArrayList<DoublesPair>();
    listDiscounting.add(new DoublesPair(deposit.getTradeTime(), -deposit.getTradeTime() * dfStart * dfStartBar));
    listDiscounting.add(new DoublesPair(deposit.getMaturity(), -deposit.getMaturity() * dfEnd * dfEndBar));
    result.put(market.getCurve(deposit.getCurrency()).getCurve().getName(), listDiscounting);
    return new PresentValueCurveSensitivityMarket(result);
  }

}
