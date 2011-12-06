/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market.curvebuilding;

import static com.opengamma.math.interpolation.Interpolator1DFactory.FLAT_EXTRAPOLATOR;
import static com.opengamma.math.interpolation.Interpolator1DFactory.LINEAR_EXTRAPOLATOR;
import static org.testng.AssertJUnit.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.opengamma.financial.forex.derivative.ForexSwap;
import com.opengamma.financial.forex.market.PresentValueForexMarketCalculator;
import com.opengamma.financial.forex.method.FXMatrix;
import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.cash.definition.Cash;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.MarketWithFXBundle;
import com.opengamma.financial.interestrate.payments.Payment;
import com.opengamma.financial.interestrate.payments.derivative.CouponOIS;
import com.opengamma.financial.interestrate.swap.definition.Swap;
import com.opengamma.math.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.math.interpolation.Interpolator1D;
import com.opengamma.math.interpolation.Interpolator1DFactory;
import com.opengamma.math.matrix.DoubleMatrix1D;
import com.opengamma.math.rootfinding.newton.BroydenVectorRootFinder;
import com.opengamma.math.rootfinding.newton.NewtonVectorRootFinder;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.money.MultipleCurrencyAmount;

public class MarketBundleForexBuildingTest {

  private static final double EPS = 1e-8;
  private static final int STEPS = 100;

  private static final PresentValueForexMarketCalculator PVFC = PresentValueForexMarketCalculator.getInstance();

  private static final int NB_TEST = 100;
  /**
   * Tolerance for the price convergence (equivalent to 0.01 currency unit for 100m notional).
   */
  private static final double TOLERANCE = 1E-4;

  @Test
  /**
   * Build the discounting curve in EUR from ON and TN deposits and OIS swaps. The same curve is used for discounting and OIS forward projection.
   */
  public void discounting() {
    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsDscTime = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferences = new HashMap<Currency, Integer>();
    discountingReferences.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferences = new HashMap<IndexDeposit, Integer>();
    forwardReferences.put(eonia, 0);
    int nbInstruments = instrumentsDsc.length;

    MarketBundle market = MarketBundleBuildingTest.discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);

    MultipleCurrencyAmount[] pv = new MultipleCurrencyAmount[nbInstruments];
    for (int loopins = 0; loopins < nbInstruments; loopins++) {
      pv[loopins] = PVFC.visit(instrumentsDsc[loopins], market);
      assertEquals("Curve building - discounting curve - instrument " + loopins, 0.0, pv[loopins].getAmount(eur), TOLERANCE);
    }
  }

  @Test
  /**
   * Build the discounting curve in EUR from ON and TN deposits and OIS swaps. The USD discounting curve is build from EUR curve and FX swaps.
   */
  public void discountingForeign() {
    InstrumentDerivative[] instrumentsDscEUR = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsDscEURTime = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDscEUR = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    Currency eur = ((Cash) instrumentsDscEUR[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDscEUR[2]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferencesEUR = new HashMap<Currency, Integer>();
    discountingReferencesEUR.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferencesEUR = new HashMap<IndexDeposit, Integer>();
    forwardReferencesEUR.put(eonia, 0);

    MarketBundle marketEUR = MarketBundleBuildingTest.discountingBuild(instrumentsDscEUR, intrumentsDscEURTime, marketRateDscEUR, discountingReferencesEUR, forwardReferencesEUR);

    ForexSwap[] instrumentsDscUSD = CurveBuildingForexInstrumentsDataSets.instrumentsForex();
    Currency usd = instrumentsDscUSD[0].getFarLeg().getCurrency2();

    double fxToday = -instrumentsDscUSD[0].getNearLeg().getPaymentCurrency2().getAmount() / instrumentsDscUSD[0].getNearLeg().getPaymentCurrency1().getAmount();
    FXMatrix fxMatrix = new FXMatrix(eur, usd, fxToday);

    MarketWithFXBundle marketFXEUR = new MarketWithFXBundle(fxMatrix, marketEUR);

    double[] intrumentsDscUSDTime = CurveBuildingForexInstrumentsDataSets.timeForex();
    Map<Currency, Integer> discountingReferencesUSD = new HashMap<Currency, Integer>();
    discountingReferencesUSD.put(usd, 0);
    Map<IndexDeposit, Integer> forwardReferencesUSD = new HashMap<IndexDeposit, Integer>();
    int nbInstrumentsDscUSD = instrumentsDscUSD.length;
    double[] startDscUSD = new double[nbInstrumentsDscUSD];
    MarketWithFXBundle marketEURUSD = discountingBuild(marketFXEUR, instrumentsDscUSD, intrumentsDscUSDTime, startDscUSD, discountingReferencesUSD, forwardReferencesUSD);

    MultipleCurrencyAmount[] pvMultipleCurrency = new MultipleCurrencyAmount[nbInstrumentsDscUSD];
    CurrencyAmount[] pvUSD = new CurrencyAmount[nbInstrumentsDscUSD];
    for (int loopins = 0; loopins < nbInstrumentsDscUSD; loopins++) {
      pvMultipleCurrency[loopins] = PVFC.visit(instrumentsDscUSD[loopins], marketEURUSD);
      pvUSD[loopins] = fxMatrix.convert(pvMultipleCurrency[loopins], usd);
      assertEquals("Curve building - discounting curve - instrument " + loopins, 0.0, pvUSD[loopins].getAmount(), TOLERANCE);
    }
  }

  private MarketWithFXBundle discountingBuild(final MarketWithFXBundle marketKnown, InstrumentDerivative[] instrumentsDsc, double[] intrumentsDscTime, double[] marketRateDsc,
      Map<Currency, Integer> discountingReferences, Map<IndexDeposit, Integer> forwardReferences) {
    int nbInstruments = instrumentsDsc.length;
    CurrencyAmount[] marketValue = new CurrencyAmount[nbInstruments];
    for (int loopins = 0; loopins < nbInstruments; loopins++) {
      marketValue[loopins] = CurrencyAmount.of(discountingReferences.keySet().iterator().next(), 0);
    }
    double[][] nodePointsYieldCurve = new double[1][nbInstruments];
    nodePointsYieldCurve[0] = intrumentsDscTime;
    final String interpolator = Interpolator1DFactory.DOUBLE_QUADRATIC;
    final CombinedInterpolatorExtrapolator extrapolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(interpolator, LINEAR_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    Interpolator1D[] interpolatorsYieldCurve = new Interpolator1D[] {extrapolator};
    String name = discountingReferences.keySet().iterator().next().toString() + " discounting";
    MarketFinderDataBundle data = new MarketFinderDataBundle(marketKnown, instrumentsDsc, marketValue, discountingReferences, forwardReferences, nodePointsYieldCurve, interpolatorsYieldCurve,
        new String[] {name});
    MarketWithFXBundleFinderFunction func = new MarketWithFXBundleFinderFunction(PVFC, data);
    final NewtonVectorRootFinder rootFinder = new BroydenVectorRootFinder(EPS, EPS, STEPS);
    final DoubleMatrix1D yieldCurveNodes = rootFinder.getRoot(func, new DoubleMatrix1D(marketRateDsc));
    MarketWithFXBundle market = (MarketWithFXBundle) MarketBundleBuildingFunction.build(data, yieldCurveNodes);
    return market;
  }

  @Test(enabled = false)
  /**
   * Performance.
   */
  public void performanceDsc() {
    long startTime, endTime;

    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsTimeDsc = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    int nbInstrumentsDsc = instrumentsDsc.length;

    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferencesDsc = new HashMap<Currency, Integer>();
    discountingReferencesDsc.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferencesDsc = new HashMap<IndexDeposit, Integer>();
    forwardReferencesDsc.put(eonia, 0);

    MarketBundle marketDsc = MarketBundleBuildingTest.discountingBuild(instrumentsDsc, intrumentsTimeDsc, marketRateDsc, discountingReferencesDsc, forwardReferencesDsc);

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < NB_TEST; looptest++) {
      marketDsc = MarketBundleBuildingTest.discountingBuild(instrumentsDsc, intrumentsTimeDsc, marketRateDsc, discountingReferencesDsc, forwardReferencesDsc);
    }
    endTime = System.currentTimeMillis();
    System.out.println(NB_TEST + " discounting curve building (" + nbInstrumentsDsc + " instruments): " + (endTime - startTime) + " ms " + marketDsc.toString());
    // Performance note: Dsc building: 22-Nov-11: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 400 ms for 100 constructions (18 instruments - no Jacobian).
  }

  @Test(enabled = false)
  /**
   * Performance.
   */
  public void performanceDscEURUSD() {
    long startTime, endTime;

    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsTimeDsc = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    int nbInstrumentsDsc = instrumentsDsc.length;

    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferencesDsc = new HashMap<Currency, Integer>();
    discountingReferencesDsc.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferencesDsc = new HashMap<IndexDeposit, Integer>();
    forwardReferencesDsc.put(eonia, 0);

    ForexSwap[] instrumentsDscUSD = CurveBuildingForexInstrumentsDataSets.instrumentsForex();
    Currency usd = instrumentsDscUSD[0].getFarLeg().getCurrency2();

    double fxToday = -instrumentsDscUSD[0].getNearLeg().getPaymentCurrency2().getAmount() / instrumentsDscUSD[0].getNearLeg().getPaymentCurrency1().getAmount();
    FXMatrix fxMatrix = new FXMatrix(eur, usd, fxToday);

    double[] intrumentsDscUSDTime = CurveBuildingForexInstrumentsDataSets.timeForex();
    Map<Currency, Integer> discountingReferencesUSD = new HashMap<Currency, Integer>();
    discountingReferencesUSD.put(usd, 0);
    Map<IndexDeposit, Integer> forwardReferencesUSD = new HashMap<IndexDeposit, Integer>();
    int nbInstrumentsDscUSD = instrumentsDscUSD.length;
    double[] startDscUSD = new double[nbInstrumentsDscUSD];

    MarketBundle marketEUR = MarketBundleBuildingTest.discountingBuild(instrumentsDsc, intrumentsTimeDsc, marketRateDsc, discountingReferencesDsc, forwardReferencesDsc);
    MarketWithFXBundle marketFXEUR = new MarketWithFXBundle(fxMatrix, marketEUR);
    MarketWithFXBundle marketEURUSD = discountingBuild(marketFXEUR, instrumentsDscUSD, intrumentsDscUSDTime, startDscUSD, discountingReferencesUSD, forwardReferencesUSD);

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < NB_TEST; looptest++) {
      marketEUR = MarketBundleBuildingTest.discountingBuild(instrumentsDsc, intrumentsTimeDsc, marketRateDsc, discountingReferencesDsc, forwardReferencesDsc);
      marketFXEUR = new MarketWithFXBundle(fxMatrix, marketEUR);
      marketEURUSD = discountingBuild(marketFXEUR, instrumentsDscUSD, intrumentsDscUSDTime, startDscUSD, discountingReferencesUSD, forwardReferencesUSD);
    }
    endTime = System.currentTimeMillis();
    System.out.println(NB_TEST + " discounting curves EUR/USD building (" + nbInstrumentsDsc + " instruments EUR + " + nbInstrumentsDscUSD + " instruments USD): " + (endTime - startTime) + " ms "
        + marketEURUSD.toString());
    // Performance note: Dsc building: 22-Nov-11: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 430 ms for 100 constructions (18+7 instruments - no Jacobian).
  }

}
