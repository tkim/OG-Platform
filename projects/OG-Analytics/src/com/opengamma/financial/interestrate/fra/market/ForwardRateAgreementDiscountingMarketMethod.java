/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.fra.market;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.fra.ForwardRateAgreement;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.PresentValueCurveSensitivityMarket;
import com.opengamma.financial.interestrate.method.PricingMarketMethod;
import com.opengamma.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.tuple.DoublesPair;

/**
 * Method to compute the present value and its sensitivities for a FRA with discounting. The present value is computed as the (forward rate - FRA rate)
 * multiplied by the notional and the payment accrual factor and discounted to settlement. The discounting to settlement is done using the forward rate over
 * the fixing period. The value is further discounted from settlement to today using the discounting curve.
 * {@latex.ilb %preamble{\\usepackage{amsmath}}
 * \\begin{equation*}
 * P^D(0,t_1)\\frac{\\delta_P(F-K)}{1+\\delta_F F} \\quad \\mbox{and}\\quad F = \\frac{1}{\\delta_F}\\left( \\frac{P^j(0,t_1)}{P^j(0,t_2)}-1\\right)
 * \\end{equation*}
 * }
 * This approach is valid subject to a independence hypothesis between the discounting curve and some spread.
 * <P> Reference: Henrard, M. (2010). The irony in the derivatives discounting part II: the crisis. Wilmott Journal, 2(6):301-316.
 */
public final class ForwardRateAgreementDiscountingMarketMethod implements PricingMarketMethod {

  /**
   * The unique instance of the method.
   */
  private static final ForwardRateAgreementDiscountingMarketMethod INSTANCE = new ForwardRateAgreementDiscountingMarketMethod();

  /**
   * Return the unique instance of the class.
   * @return The instance.
   */
  public static ForwardRateAgreementDiscountingMarketMethod getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  private ForwardRateAgreementDiscountingMarketMethod() {
  }

  /**
   * Compute the present value of a FRA by discounting.
   * @param fra The FRA.
   * @param market The market.
   * @return The present value.
   */
  public MultipleCurrencyAmount presentValue(final ForwardRateAgreement fra, final MarketBundle market) {
    Validate.notNull(fra, "FRA");
    Validate.notNull(market, "Market");
    final double discountFactorSettlement = market.getDiscountingFactor(fra.getCurrency(), fra.getPaymentTime());
    final double forward = market.getForwardRate(fra.getIndex(), fra.getFixingPeriodStartTime(), fra.getFixingPeriodEndTime(), fra.getFixingYearFraction());
    final double presentValue = discountFactorSettlement * fra.getPaymentYearFraction() * fra.getNotional() * (forward - fra.getRate()) / (1 + fra.getPaymentYearFraction() * forward);
    return MultipleCurrencyAmount.of(fra.getCurrency(), presentValue);
  }

  @Override
  public MultipleCurrencyAmount presentValue(final InstrumentDerivative instrument, final MarketBundle market) {
    Validate.isTrue(instrument instanceof ForwardRateAgreement, "ForwardRateAgreementMethod: The instrument should be of type ForwardRateAgreement");
    return presentValue((ForwardRateAgreement) instrument, market);
  }

  /**
   * Compute the present value sensitivity to rates of a FRA by discounting.
   * @param fra The FRA.
   * @param market The market.
   * @return The present value sensitivity.
   */
  public PresentValueCurveSensitivityMarket presentValueCurveSensitivity(final ForwardRateAgreement fra, final MarketBundle market) {
    Validate.notNull(fra, "FRA");
    Validate.notNull(market, "Market");
    final YieldAndDiscountCurve forwardCurve = market.getCurve(fra.getIndex());
    final double df = market.getDiscountingFactor(fra.getCurrency(), fra.getPaymentTime());
    final double dfForwardStart = forwardCurve.getDiscountFactor(fra.getFixingPeriodStartTime());
    final double dfForwardEnd = forwardCurve.getDiscountFactor(fra.getFixingPeriodEndTime());
    final double forward = (dfForwardStart / dfForwardEnd - 1.0) / fra.getFixingYearFraction();
    // Backward sweep
    final double pvBar = 1.0;
    final double forwardBar = df * fra.getPaymentYearFraction() * fra.getNotional() * (1 - (forward - fra.getRate()) / (1 + fra.getPaymentYearFraction() * forward) * fra.getPaymentYearFraction())
        / (1 + fra.getPaymentYearFraction() * forward);
    final double dfForwardEndBar = -dfForwardStart / (dfForwardEnd * dfForwardEnd) / fra.getFixingYearFraction() * forwardBar;
    final double dfForwardStartBar = 1.0 / (fra.getFixingYearFraction() * dfForwardEnd) * forwardBar;
    final double dfBar = fra.getPaymentYearFraction() * fra.getNotional() * (forward - fra.getRate()) / (1 + fra.getFixingYearFraction() * forward) * pvBar;

    final Map<String, List<DoublesPair>> resultDsc = new HashMap<String, List<DoublesPair>>();
    final List<DoublesPair> listDiscounting = new ArrayList<DoublesPair>();
    listDiscounting.add(new DoublesPair(fra.getPaymentTime(), -fra.getPaymentTime() * df * dfBar));
    resultDsc.put(market.getCurve(fra.getCurrency()).getCurve().getName(), listDiscounting);
    final PresentValueCurveSensitivityMarket result = new PresentValueCurveSensitivityMarket(resultDsc);

    final Map<String, List<DoublesPair>> resultFwd = new HashMap<String, List<DoublesPair>>();
    final List<DoublesPair> listForward = new ArrayList<DoublesPair>();
    listForward.add(new DoublesPair(fra.getFixingPeriodStartTime(), -fra.getFixingPeriodStartTime() * dfForwardStart * dfForwardStartBar));
    listForward.add(new DoublesPair(fra.getFixingPeriodEndTime(), -fra.getFixingPeriodEndTime() * dfForwardEnd * dfForwardEndBar));
    resultFwd.put(market.getCurve(fra.getIndex()).getCurve().getName(), listForward);

    return PresentValueCurveSensitivityMarket.plus(result, new PresentValueCurveSensitivityMarket(resultFwd));
  }

  /**
   * Compute the par rate or forward rate of the FRA.
   * @param fra The FRA.
   * @param market The market.
   * @return The par rate.
   */
  public double parRate(final ForwardRateAgreement fra, final MarketBundle market) {
    Validate.notNull(fra, "FRA");
    Validate.notNull(market, "Market");
    return market.getForwardRate(fra.getIndex(), fra.getFixingPeriodStartTime(), fra.getFixingPeriodEndTime(), fra.getFixingYearFraction());
  }

}
