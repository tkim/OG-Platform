/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market.curvebuilding;

import static com.opengamma.math.interpolation.Interpolator1DFactory.FLAT_EXTRAPOLATOR;
import static org.testng.AssertJUnit.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.LastTimeCalculator;
import com.opengamma.financial.interestrate.cash.derivative.Cash;
import com.opengamma.financial.interestrate.cash.derivative.DepositIbor;
import com.opengamma.financial.interestrate.future.definition.InterestRateFuture;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.MarketQuoteMarketCalculator;
import com.opengamma.financial.interestrate.market.PresentValueMarketCalculator;
import com.opengamma.financial.interestrate.payments.Payment;
import com.opengamma.financial.interestrate.payments.derivative.CouponOIS;
import com.opengamma.financial.interestrate.swap.definition.Swap;
import com.opengamma.math.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.math.interpolation.Interpolator1DFactory;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;

/**
 * Presents examples of curve building and curve manipulation from market data.
 */
public class MarketBundleBuilderAnalysis {

  private static final PresentValueMarketCalculator PVC = PresentValueMarketCalculator.getInstance();
  private static final LastTimeCalculator LTC = LastTimeCalculator.getInstance();
  private static final MarketQuoteMarketCalculator MQC = MarketQuoteMarketCalculator.getInstance();
  /**
   * Tolerance for the price convergence (equivalent to 0.01 currency unit for 100m notional).
   */
  private static final double TOLERANCE_PRICE = 1E-4;
  private static final double TOLERANCE_RATE = 1E-8;

  @Test
  /**
   * Build the discounting curve in EUR from deposits and OIS swaps and forward 3M curve from Euribor futures and Euribor 3M swaps. 
   * The same curve is used for discounting and OIS forward projection.
   * The market quotes for swaps with standard maturities are computed from the curves.
   */
  public void forward3FutAfterDiscounting() {
    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscountingOIS();
    int nbInstrumentsDsc = instrumentsDsc.length;
    InstrumentDerivative[] instrumentsFwd3 = CurveBuildingInstrumentsDataSets.instrumentsForward3FutSwap();
    int nbInstrumentsFwd3 = instrumentsFwd3.length;
    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();
    IndexDeposit euribor3m = ((DepositIbor) instrumentsFwd3[0]).getIndex();
    Map<Currency, Integer> discountingReferencesDsc = new HashMap<Currency, Integer>();
    discountingReferencesDsc.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferencesDsc = new HashMap<IndexDeposit, Integer>();
    forwardReferencesDsc.put(eonia, 0);
    Map<Currency, Integer> discountingReferencesFwd3 = new HashMap<Currency, Integer>();
    Map<IndexDeposit, Integer> forwardReferencesFwd3 = new HashMap<IndexDeposit, Integer>();
    forwardReferencesFwd3.put(euribor3m, 0);

    List<Map<Currency, Integer>> discountingReferences = new ArrayList<Map<Currency, Integer>>();
    discountingReferences.add(discountingReferencesDsc);
    discountingReferences.add(discountingReferencesFwd3);
    List<Map<IndexDeposit, Integer>> forwardReferences = new ArrayList<Map<IndexDeposit, Integer>>();
    forwardReferences.add(forwardReferencesDsc);
    forwardReferences.add(forwardReferencesFwd3);

    final String interpolator = Interpolator1DFactory.DOUBLE_QUADRATIC;
    final CombinedInterpolatorExtrapolator quadraticFlat = CombinedInterpolatorExtrapolatorFactory.getInterpolator(interpolator, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    InstrumentDerivative[][][] instruments = new InstrumentDerivative[][][] { {instrumentsDsc}, {instrumentsFwd3}};
    String[][] curveNames = new String[][] { {eur.toString() + " Discounting"}, {euribor3m.toString()}};

    MarketBundle market = MarketBundleBuilder.discountingForwardConsecutive(instruments, curveNames, discountingReferences, forwardReferences, quadraticFlat, LTC, PVC);
    // Check instrument have PV 0.
    MultipleCurrencyAmount[] pv = new MultipleCurrencyAmount[nbInstrumentsDsc + nbInstrumentsFwd3];
    for (int loopins = 0; loopins < nbInstrumentsDsc; loopins++) {
      pv[loopins] = PVC.visit(instrumentsDsc[loopins], market);
      assertEquals("Curve building - discounting curve - instrument " + loopins, 0.0, pv[loopins].getAmount(eur), TOLERANCE_PRICE);
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd3; loopins++) {
      pv[nbInstrumentsDsc + loopins] = PVC.visit(instrumentsFwd3[loopins], market);
      assertEquals("Curve building - forward curve - instrument " + loopins, 0.0, pv[nbInstrumentsDsc + loopins].getAmount(eur), TOLERANCE_PRICE);
    }
    // Market quotes check.
    Double[] marketQuoteComputedDsc = MQC.visit(instrumentsDsc, market);
    double[] marketQuoteOriginalDsc = CurveBuildingInstrumentsDataSets.marketRateDiscountingOIS();
    for (int loopins = 0; loopins < nbInstrumentsDsc; loopins++) {
      assertEquals("Curve building - discounting curve - market quote " + loopins, marketQuoteOriginalDsc[loopins], marketQuoteComputedDsc[loopins], TOLERANCE_RATE);
    }
    Double[] marketQuoteComputedFwd3 = MQC.visit(instrumentsFwd3, market);
    double[] marketQuoteOriginalFwd3 = CurveBuildingInstrumentsDataSets.marketRateForward3FutSwap();
    for (int loopins = 0; loopins < nbInstrumentsFwd3; loopins++) {
      if (instrumentsFwd3[loopins] instanceof InterestRateFuture) {
        assertEquals("Curve building - forward curve - market quote " + loopins, marketQuoteOriginalFwd3[loopins], 1.0 - marketQuoteComputedFwd3[loopins], TOLERANCE_RATE);
      } else {
        assertEquals("Curve building - forward curve - market quote " + loopins, marketQuoteOriginalFwd3[loopins], marketQuoteComputedFwd3[loopins], TOLERANCE_RATE);
      }
    }
    // Market quotes: standard
    InstrumentDerivative[] instrumentsFwd3Swap = CurveBuildingInstrumentsDataSets.instrumentsForward3FullSwap();
    Double[] marketQuoteSwap = MQC.visit(instrumentsFwd3Swap, market);

    //    double[] test2 = CurveBuildingInstrumentsDataSets.marketRateForward3FullSwap();
    double test = 0.0;
    test++;
  }

}
