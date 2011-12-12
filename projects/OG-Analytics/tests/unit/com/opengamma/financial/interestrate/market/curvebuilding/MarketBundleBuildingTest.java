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

import javax.time.calendar.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.instrument.index.IndexPrice;
import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.LastTimeCalculator;
import com.opengamma.financial.interestrate.cash.definition.Cash;
import com.opengamma.financial.interestrate.inflation.derivatives.CouponInflationZeroCouponInterpolation;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.MarketDataSets;
import com.opengamma.financial.interestrate.market.MarketWithHullWhiteBundle;
import com.opengamma.financial.interestrate.market.PresentValueHullWhiteMarketCalculator;
import com.opengamma.financial.interestrate.market.PresentValueMarketCalculator;
import com.opengamma.financial.interestrate.payments.Coupon;
import com.opengamma.financial.interestrate.payments.CouponIbor;
import com.opengamma.financial.interestrate.payments.Payment;
import com.opengamma.financial.interestrate.payments.derivative.CouponOIS;
import com.opengamma.financial.interestrate.swap.definition.Swap;
import com.opengamma.financial.model.interestrate.definition.HullWhiteOneFactorPiecewiseConstantParameters;
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
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.time.TimeCalculator;
import com.opengamma.util.timeseries.zoneddatetime.ArrayZonedDateTimeDoubleTimeSeries;

/**
 * Tests the MaketBundle building with different configurations.
 */
public class MarketBundleBuildingTest {

  private static final double EPS = 1e-8;
  private static final int STEPS = 100;

  private static final PresentValueMarketCalculator PVC = PresentValueMarketCalculator.getInstance();
  private static final PresentValueHullWhiteMarketCalculator PVC_HW = PresentValueHullWhiteMarketCalculator.getInstance();
  private static final LastTimeCalculator LTC = LastTimeCalculator.getInstance();

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
    //TODO: transfer into market.curvebuilding?
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

    MarketBundle market = discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);

    IndexDeposit[] indexes = new IndexDeposit[] {eonia};
    final String interpolator = Interpolator1DFactory.DOUBLE_QUADRATIC;
    final CombinedInterpolatorExtrapolator extrainterpolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(interpolator, FLAT_EXTRAPOLATOR);
    MarketBundle marketFromBuilder = MarketBundleBuilder.discounting(instrumentsDsc, eur, indexes, extrainterpolator, LTC, PVC);
    MarketBundle marketFromBuilder2 = MarketBundleBuilder.discounting(instrumentsDsc, marketRateDsc, eur, indexes, extrainterpolator, LTC, PVC);

    MultipleCurrencyAmount[] pv = new MultipleCurrencyAmount[nbInstruments];
    MultipleCurrencyAmount[] pvBuilder = new MultipleCurrencyAmount[nbInstruments];
    MultipleCurrencyAmount[] pvBuilder2 = new MultipleCurrencyAmount[nbInstruments];
    for (int loopins = 0; loopins < nbInstruments; loopins++) {
      pv[loopins] = PVC.visit(instrumentsDsc[loopins], market);
      assertEquals("Curve building - discounting curve - instrument " + loopins, 0.0, pv[loopins].getAmount(eur), TOLERANCE);
      pvBuilder[loopins] = PVC.visit(instrumentsDsc[loopins], marketFromBuilder);
      assertEquals("Curve building - discounting curve - instrument " + loopins, 0.0, pvBuilder[loopins].getAmount(eur), TOLERANCE);
      pvBuilder2[loopins] = PVC.visit(instrumentsDsc[loopins], marketFromBuilder2);
      assertEquals("Curve building - discounting curve - instrument " + loopins, 0.0, pvBuilder2[loopins].getAmount(eur), TOLERANCE);
    }
  }

  public static MarketBundle discountingBuild(InstrumentDerivative[] instrumentsDsc, double[] intrumentsDscTime, double[] marketRateDsc, Map<Currency, Integer> discountingReferences,
      Map<IndexDeposit, Integer> forwardReferences) {
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
    MarketFinderDataBundle data = new MarketFinderDataBundle(instrumentsDsc, marketValue, discountingReferences, forwardReferences, nodePointsYieldCurve, interpolatorsYieldCurve, new String[] {name});
    MarketBundleFinderFunction func = new MarketBundleFinderFunction(PVC, data);
    final NewtonVectorRootFinder rootFinder = new BroydenVectorRootFinder(EPS, EPS, STEPS);
    final DoubleMatrix1D yieldCurveNodes = rootFinder.getRoot(func, new DoubleMatrix1D(marketRateDsc));
    MarketBundle market = MarketBundleBuildingFunction.build(data, yieldCurveNodes);
    return market;
  }

  @Test
  /**
   * Build the discounting curve in EUR from ON and TN deposits and OIS swaps and forward 3M curve from Euribor 3M swaps. 
   * The same curve is used for discounting and OIS forward projection.
   */
  public void forward3FullSwap() {
    checkForwardFullSwap(CurveBuildingInstrumentsDataSets.instrumentsForward3FullSwap(), CurveBuildingInstrumentsDataSets.timeForward3FullSwap(),
        CurveBuildingInstrumentsDataSets.marketRateForward3FullSwap());
  }

  @Test
  /**
   * Build the discounting curve in EUR from ON and TN deposits and OIS swaps and forward 6M curve from Euribor 6M swaps. 
   * The same curve is used for discounting and OIS forward projection.
   */
  public void forward6FullSwap() {
    checkForwardFullSwap(CurveBuildingInstrumentsDataSets.instrumentsForward6FullSwap(), CurveBuildingInstrumentsDataSets.timeForward6FullSwap(),
        CurveBuildingInstrumentsDataSets.marketRateForward6FullSwap());
  }

  public void checkForwardFullSwap(InstrumentDerivative[] instrumentsFwd, double[] intrumentsTimeFwd, double[] marketRateFwd) {
    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsTimeDsc = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    int nbInstrumentsDsc = instrumentsDsc.length;
    int nbInstrumentsFwd = instrumentsFwd.length;
    int nbInstruments = instrumentsDsc.length + instrumentsFwd.length;
    double[] intrumentsTime = new double[nbInstruments];
    System.arraycopy(intrumentsTimeDsc, 0, intrumentsTime, 0, nbInstrumentsDsc);
    System.arraycopy(intrumentsTimeFwd, 0, intrumentsTime, nbInstrumentsDsc, nbInstrumentsFwd);
    double[] marketRate = new double[nbInstruments];
    System.arraycopy(marketRateDsc, 0, marketRate, 0, nbInstrumentsDsc);
    System.arraycopy(marketRateFwd, 0, marketRate, nbInstrumentsDsc, nbInstrumentsFwd);
    InstrumentDerivative[] instruments = new InstrumentDerivative[nbInstruments];
    for (int loopins = 0; loopins < nbInstrumentsDsc; loopins++) {
      instruments[loopins] = instrumentsDsc[loopins];
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd; loopins++) {
      instruments[nbInstrumentsDsc + loopins] = instrumentsFwd[loopins];
    }
    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();
    @SuppressWarnings("unchecked")
    IndexDeposit euribor6m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd[0]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferences = new HashMap<Currency, Integer>();
    discountingReferences.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferences = new HashMap<IndexDeposit, Integer>();
    forwardReferences.put(eonia, 0);
    forwardReferences.put(euribor6m, 1);

    MarketBundle market = discountingForwardBuild(PVC, instruments, 2, new int[] {nbInstrumentsDsc, nbInstrumentsFwd}, intrumentsTime, marketRate, discountingReferences, forwardReferences);

    MultipleCurrencyAmount[] pv = new MultipleCurrencyAmount[nbInstruments];
    for (int loopins = 0; loopins < nbInstrumentsDsc; loopins++) {
      pv[loopins] = PVC.visit(instrumentsDsc[loopins], market);
      assertEquals("Curve building - discounting curve - instrument " + loopins, 0.0, pv[loopins].getAmount(eur), TOLERANCE);
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd; loopins++) {
      pv[nbInstrumentsDsc + loopins] = PVC.visit(instrumentsFwd[loopins], market);
      assertEquals("Curve building - forward curve - instrument " + loopins, 0.0, pv[nbInstrumentsDsc + loopins].getAmount(eur), TOLERANCE);
    }
  }

  @Test
  /**
   * Build the discounting curve in EUR from ON and TN deposits and OIS swaps and forward 3M curve from Euribor 3M swaps. 
   * The same curve is used for discounting and OIS forward projection.
   */
  public void forward3FraSwap() {
    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsTimeDsc = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    int nbInstrumentsDsc = instrumentsDsc.length;
    InstrumentDerivative[] instrumentsFwd3 = CurveBuildingInstrumentsDataSets.instrumentsForward3FraSwap();
    double[] intrumentsTimeFwd3 = CurveBuildingInstrumentsDataSets.timeForward3FraSwap();
    double[] marketRateFwd3 = CurveBuildingInstrumentsDataSets.marketRateForward3FraSwap();
    int nbInstrumentsFwd3 = instrumentsFwd3.length;
    int nbInstruments = instrumentsDsc.length + instrumentsFwd3.length;
    double[] intrumentsTime = new double[nbInstruments];
    System.arraycopy(intrumentsTimeDsc, 0, intrumentsTime, 0, nbInstrumentsDsc);
    System.arraycopy(intrumentsTimeFwd3, 0, intrumentsTime, nbInstrumentsDsc, nbInstrumentsFwd3);
    double[] marketRate = new double[nbInstruments];
    System.arraycopy(marketRateDsc, 0, marketRate, 0, nbInstrumentsDsc);
    System.arraycopy(marketRateFwd3, 0, marketRate, nbInstrumentsDsc, nbInstrumentsFwd3);
    InstrumentDerivative[] instruments = new InstrumentDerivative[nbInstruments];
    for (int loopins = 0; loopins < nbInstrumentsDsc; loopins++) {
      instruments[loopins] = instrumentsDsc[loopins];
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd3; loopins++) {
      instruments[nbInstrumentsDsc + loopins] = instrumentsFwd3[loopins];
    }
    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();
    @SuppressWarnings("unchecked")
    IndexDeposit euribor3m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd3[0]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferences = new HashMap<Currency, Integer>();
    discountingReferences.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferences = new HashMap<IndexDeposit, Integer>();
    forwardReferences.put(eonia, 0);
    forwardReferences.put(euribor3m, 1);

    MarketBundle market = discountingForwardBuild(PVC, instruments, 2, new int[] {nbInstrumentsDsc, nbInstrumentsFwd3}, intrumentsTime, marketRate, discountingReferences, forwardReferences);

    MultipleCurrencyAmount[] pv = new MultipleCurrencyAmount[nbInstruments];
    for (int loopins = 0; loopins < nbInstrumentsDsc; loopins++) {
      pv[loopins] = PVC.visit(instrumentsDsc[loopins], market);
      assertEquals("Curve building - discounting curve - instrument " + loopins, 0.0, pv[loopins].getAmount(eur), TOLERANCE);
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd3; loopins++) {
      pv[nbInstrumentsDsc + loopins] = PVC.visit(instrumentsFwd3[loopins], market);
      assertEquals("Curve building - forward curve - instrument " + loopins, 0.0, pv[nbInstrumentsDsc + loopins].getAmount(eur), TOLERANCE);
    }
  }

  @Test
  /**
   * Build the discounting curve in EUR from ON and TN deposits and OIS swaps and forward 3M and 6M curve from Euribor 3M and Euribor 6M swaps. 
   * The same curve is used for discounting and OIS forward projection.
   */
  public void forward36FullSwap() {

    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsTimeDsc = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    int nbInstrumentsDsc = instrumentsDsc.length;
    InstrumentDerivative[] instrumentsFwd3 = CurveBuildingInstrumentsDataSets.instrumentsForward3FullSwap();
    double[] intrumentsTimeFwd3 = CurveBuildingInstrumentsDataSets.timeForward3FullSwap();
    double[] marketRateFwd3 = CurveBuildingInstrumentsDataSets.marketRateForward3FullSwap();
    int nbInstrumentsFwd3 = instrumentsFwd3.length;
    InstrumentDerivative[] instrumentsFwd6 = CurveBuildingInstrumentsDataSets.instrumentsForward6FullSwap();
    double[] intrumentsTimeFwd6 = CurveBuildingInstrumentsDataSets.timeForward6FullSwap();
    double[] marketRateFwd6 = CurveBuildingInstrumentsDataSets.marketRateForward6FullSwap();
    int nbInstrumentsFwd6 = instrumentsFwd6.length;
    int nbInstruments = instrumentsDsc.length + instrumentsFwd3.length + instrumentsFwd6.length;
    double[] intrumentsTime = new double[nbInstruments];
    System.arraycopy(intrumentsTimeDsc, 0, intrumentsTime, 0, nbInstrumentsDsc);
    System.arraycopy(intrumentsTimeFwd3, 0, intrumentsTime, nbInstrumentsDsc, nbInstrumentsFwd3);
    System.arraycopy(intrumentsTimeFwd6, 0, intrumentsTime, nbInstrumentsDsc + nbInstrumentsFwd3, nbInstrumentsFwd6);
    double[] marketRate = new double[nbInstruments];
    System.arraycopy(marketRateDsc, 0, marketRate, 0, nbInstrumentsDsc);
    System.arraycopy(marketRateFwd3, 0, marketRate, nbInstrumentsDsc, nbInstrumentsFwd3);
    System.arraycopy(marketRateFwd6, 0, marketRate, nbInstrumentsDsc + nbInstrumentsFwd3, nbInstrumentsFwd6);
    InstrumentDerivative[] instruments = new InstrumentDerivative[nbInstruments];
    for (int loopins = 0; loopins < nbInstrumentsDsc; loopins++) {
      instruments[loopins] = instrumentsDsc[loopins];
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd3; loopins++) {
      instruments[nbInstrumentsDsc + loopins] = instrumentsFwd3[loopins];
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd6; loopins++) {
      instruments[nbInstrumentsDsc + nbInstrumentsFwd3 + loopins] = instrumentsFwd6[loopins];
    }
    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();
    @SuppressWarnings("unchecked")
    IndexDeposit euribor3m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd3[0]).getSecondLeg().getNthPayment(0)).getIndex();
    @SuppressWarnings("unchecked")
    IndexDeposit euribor6m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd6[0]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferences = new HashMap<Currency, Integer>();
    discountingReferences.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferences = new HashMap<IndexDeposit, Integer>();
    forwardReferences.put(eonia, 0);
    forwardReferences.put(euribor3m, 1);
    forwardReferences.put(euribor6m, 2);

    MarketBundle market = discountingForwardBuild(PVC, instruments, 3, new int[] {nbInstrumentsDsc, nbInstrumentsFwd3, nbInstrumentsFwd6}, intrumentsTime, marketRate, discountingReferences,
        forwardReferences);

    MultipleCurrencyAmount[] pv = new MultipleCurrencyAmount[nbInstruments];
    for (int loopins = 0; loopins < nbInstrumentsDsc; loopins++) {
      pv[loopins] = PVC.visit(instrumentsDsc[loopins], market);
      assertEquals("Curve building - dsc/fwd3/fwd6 curve - instrument dsc " + loopins, 0.0, pv[loopins].getAmount(eur), TOLERANCE);
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd3; loopins++) {
      pv[nbInstrumentsDsc + loopins] = PVC.visit(instrumentsFwd3[loopins], market);
      assertEquals("Curve building - dsc/fwd3/fwd6 curve - instrument fwd3 " + loopins, 0.0, pv[nbInstrumentsDsc + loopins].getAmount(eur), TOLERANCE);
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd6; loopins++) {
      pv[nbInstrumentsDsc + nbInstrumentsFwd3 + loopins] = PVC.visit(instrumentsFwd6[loopins], market);
      assertEquals("Curve building - dsc/fwd3/fwd6 curve - instrument fwd6 " + loopins, 0.0, pv[nbInstrumentsDsc + nbInstrumentsFwd3 + loopins].getAmount(eur), TOLERANCE);
    }

  }

  @Test
  /**
   * Build the discounting curve in EUR from ON and TN deposits and OIS swaps and forward 3M curve from Euribor 3M swaps. 
   * The same curve is used for discounting and OIS forward projection.
   */
  public void forward3FullSwapAfterDiscounting() {
    // Discounting
    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsDscTime = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    int nbInstrumentsDsc = instrumentsDsc.length;
    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferences = new HashMap<Currency, Integer>();
    discountingReferences.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferences = new HashMap<IndexDeposit, Integer>();
    forwardReferences.put(eonia, 0);
    // Forward 3M
    InstrumentDerivative[] instrumentsFwd3 = CurveBuildingInstrumentsDataSets.instrumentsForward3FullSwap();
    double[] intrumentsTimeFwd3 = CurveBuildingInstrumentsDataSets.timeForward3FullSwap();
    double[] marketRateFwd3 = CurveBuildingInstrumentsDataSets.marketRateForward3FullSwap();
    int nbInstrumentsFwd3 = instrumentsFwd3.length;
    @SuppressWarnings("unchecked")
    IndexDeposit euribor3m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd3[0]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferencesFwd = new HashMap<Currency, Integer>();
    Map<IndexDeposit, Integer> forwardReferencesFwd = new HashMap<IndexDeposit, Integer>();
    forwardReferencesFwd.put(euribor3m, 0);
    // Curve building
    MarketBundle marketDsc = discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);
    MarketBundle market = discountingForwardBuild(PVC, marketDsc, instrumentsFwd3, intrumentsTimeFwd3, marketRateFwd3, discountingReferencesFwd, forwardReferencesFwd);

    int nbInstruments = nbInstrumentsDsc + nbInstrumentsFwd3;
    MultipleCurrencyAmount[] pv = new MultipleCurrencyAmount[nbInstruments];
    for (int loopins = 0; loopins < nbInstrumentsDsc; loopins++) {
      pv[loopins] = PVC.visit(instrumentsDsc[loopins], market);
      assertEquals("Curve building - discounting curve - instrument " + loopins, 0.0, pv[loopins].getAmount(eur), TOLERANCE);
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd3; loopins++) {
      pv[nbInstrumentsDsc + loopins] = PVC.visit(instrumentsFwd3[loopins], market);
      assertEquals("Curve building - forward curve - instrument " + loopins, 0.0, pv[nbInstrumentsDsc + loopins].getAmount(eur), TOLERANCE);
    }
  }

  @Test
  /**
   * Build the discounting curve in EUR from ON and TN deposits and OIS swaps and forward 3M curve from Euribor futures and Euribor 3M swaps. 
   * The same curve is used for discounting and OIS forward projection.
   */
  public void forward3FutAfterDiscounting() {
    // Discounting
    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsDscTime = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    int nbInstrumentsDsc = instrumentsDsc.length;
    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferences = new HashMap<Currency, Integer>();
    discountingReferences.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferences = new HashMap<IndexDeposit, Integer>();
    forwardReferences.put(eonia, 0);
    // Forward 3M
    InstrumentDerivative[] instrumentsFwd3 = CurveBuildingInstrumentsDataSets.instrumentsForward3FutSwap();
    double[] intrumentsTimeFwd3 = CurveBuildingInstrumentsDataSets.timeForward3FutSwap();
    double[] marketRateFwd3 = CurveBuildingInstrumentsDataSets.marketRateForward3FutSwap();
    int nbInstrumentsFwd3 = instrumentsFwd3.length;
    @SuppressWarnings("unchecked")
    IndexDeposit euribor3m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd3[0]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferencesFwd = new HashMap<Currency, Integer>();
    Map<IndexDeposit, Integer> forwardReferencesFwd = new HashMap<IndexDeposit, Integer>();
    forwardReferencesFwd.put(euribor3m, 0);
    // Curve building
    MarketBundle marketDsc = discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);
    MarketBundle market = discountingForwardBuild(PVC, marketDsc, instrumentsFwd3, intrumentsTimeFwd3, marketRateFwd3, discountingReferencesFwd, forwardReferencesFwd);

    int nbInstruments = nbInstrumentsDsc + nbInstrumentsFwd3;
    MultipleCurrencyAmount[] pv = new MultipleCurrencyAmount[nbInstruments];
    for (int loopins = 0; loopins < nbInstrumentsDsc; loopins++) {
      pv[loopins] = PVC.visit(instrumentsDsc[loopins], market);
      assertEquals("Curve building - discounting curve - instrument " + loopins, 0.0, pv[loopins].getAmount(eur), TOLERANCE);
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd3; loopins++) {
      pv[nbInstrumentsDsc + loopins] = PVC.visit(instrumentsFwd3[loopins], market);
      assertEquals("Curve building - forward curve - instrument " + loopins, 0.0, pv[nbInstrumentsDsc + loopins].getAmount(eur), TOLERANCE);
    }
  }

  @Test
  /**
   * Build the discounting curve in EUR from ON and TN deposits and OIS swaps and forward 3M curve from Euribor futures (with convexity) and Euribor 3M swaps. 
   * The same curve is used for discounting and OIS forward projection.
   */
  public void forward3FutConvexityAfterDiscounting() {
    // Hull-White parameters
    double mr = 0.01;
    double[] sigma = {0.01, 0.011};
    HullWhiteOneFactorPiecewiseConstantParameters param = new HullWhiteOneFactorPiecewiseConstantParameters(mr, sigma, new double[] {1.0});
    // Discounting
    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsDscTime = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    int nbInstrumentsDsc = instrumentsDsc.length;
    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferences = new HashMap<Currency, Integer>();
    discountingReferences.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferences = new HashMap<IndexDeposit, Integer>();
    forwardReferences.put(eonia, 0);
    // Forward 3M
    InstrumentDerivative[] instrumentsFwd3 = CurveBuildingInstrumentsDataSets.instrumentsForward3FutSwap();
    double[] intrumentsTimeFwd3 = CurveBuildingInstrumentsDataSets.timeForward3FutSwap();
    double[] marketRateFwd3 = CurveBuildingInstrumentsDataSets.marketRateForward3FutSwap();
    int nbInstrumentsFwd3 = instrumentsFwd3.length;
    @SuppressWarnings("unchecked")
    IndexDeposit euribor3m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd3[0]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferencesFwd = new HashMap<Currency, Integer>();
    Map<IndexDeposit, Integer> forwardReferencesFwd = new HashMap<IndexDeposit, Integer>();
    forwardReferencesFwd.put(euribor3m, 0);
    // Curve building
    MarketBundle marketDsc = discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);
    MarketWithHullWhiteBundle marketDscHW = new MarketWithHullWhiteBundle(eur, param, marketDsc);
    MarketBundle marketDscFwdHW = discountingForwardBuild(PVC_HW, marketDscHW, instrumentsFwd3, intrumentsTimeFwd3, marketRateFwd3, discountingReferencesFwd, forwardReferencesFwd);

    int nbInstruments = nbInstrumentsDsc + nbInstrumentsFwd3;
    MultipleCurrencyAmount[] pv = new MultipleCurrencyAmount[nbInstruments];
    for (int loopins = 0; loopins < nbInstrumentsDsc; loopins++) {
      pv[loopins] = PVC_HW.visit(instrumentsDsc[loopins], marketDscFwdHW);
      assertEquals("Curve building - discounting curve - instrument " + loopins, 0.0, pv[loopins].getAmount(eur), TOLERANCE);
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd3; loopins++) {
      pv[nbInstrumentsDsc + loopins] = PVC_HW.visit(instrumentsFwd3[loopins], marketDscFwdHW);
      assertEquals("Curve building - forward curve - instrument " + loopins, 0.0, pv[nbInstrumentsDsc + loopins].getAmount(eur), TOLERANCE);
    }

    //    MarketBundle marketDscFwd = discountingForwardBuild(PVC, marketDscHW, instrumentsFwd3, intrumentsTimeFwd3, marketRateFwd3, discountingReferencesFwd, forwardReferencesFwd);
    //
    //    double test = 0;
    //    test++;
  }

  @Test
  /**
   * Build the discounting curve in EUR from ON and TN deposits and OIS swaps and forward 3M curve from Euribor 3M swaps. 
   * The same curve is used for discounting and OIS forward projection.
   */
  public void forward6AfterFprward3AfterDiscounting() {
    // Discounting
    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsDscTime = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    int nbInstrumentsDsc = instrumentsDsc.length;
    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferences = new HashMap<Currency, Integer>();
    discountingReferences.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferences = new HashMap<IndexDeposit, Integer>();
    forwardReferences.put(eonia, 0);
    // Forward 3M
    InstrumentDerivative[] instrumentsFwd3 = CurveBuildingInstrumentsDataSets.instrumentsForward3FullSwap();
    double[] intrumentsTimeFwd3 = CurveBuildingInstrumentsDataSets.timeForward3FullSwap();
    double[] marketRateFwd3 = CurveBuildingInstrumentsDataSets.marketRateForward3FullSwap();
    int nbInstrumentsFwd3 = instrumentsFwd3.length;
    @SuppressWarnings("unchecked")
    IndexDeposit euribor3m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd3[0]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferencesFwd3 = new HashMap<Currency, Integer>();
    Map<IndexDeposit, Integer> forwardReferencesFwd3 = new HashMap<IndexDeposit, Integer>();
    forwardReferencesFwd3.put(euribor3m, 0);
    // Forward 3M
    InstrumentDerivative[] instrumentsFwd6 = CurveBuildingInstrumentsDataSets.instrumentsForward6FullSwap();
    double[] intrumentsTimeFwd6 = CurveBuildingInstrumentsDataSets.timeForward6FullSwap();
    double[] marketRateFwd6 = CurveBuildingInstrumentsDataSets.marketRateForward6FullSwap();
    int nbInstrumentsFwd6 = instrumentsFwd6.length;
    @SuppressWarnings("unchecked")
    IndexDeposit euribor6m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd6[0]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferencesFwd6 = new HashMap<Currency, Integer>();
    Map<IndexDeposit, Integer> forwardReferencesFwd6 = new HashMap<IndexDeposit, Integer>();
    forwardReferencesFwd6.put(euribor6m, 0);
    // Curve building
    MarketBundle marketDsc = discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);
    MarketBundle marketFwd3 = discountingForwardBuild(PVC, marketDsc, instrumentsFwd3, intrumentsTimeFwd3, marketRateFwd3, discountingReferencesFwd3, forwardReferencesFwd3);
    MarketBundle market = discountingForwardBuild(PVC, marketFwd3, instrumentsFwd6, intrumentsTimeFwd6, marketRateFwd6, discountingReferencesFwd6, forwardReferencesFwd6);

    int nbInstruments = nbInstrumentsDsc + nbInstrumentsFwd3 + nbInstrumentsFwd6;
    MultipleCurrencyAmount[] pv = new MultipleCurrencyAmount[nbInstruments];
    for (int loopins = 0; loopins < nbInstrumentsDsc; loopins++) {
      pv[loopins] = PVC.visit(instrumentsDsc[loopins], market);
      assertEquals("Curve building - discounting curve - instrument " + loopins, 0.0, pv[loopins].getAmount(eur), TOLERANCE);
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd3; loopins++) {
      pv[nbInstrumentsDsc + loopins] = PVC.visit(instrumentsFwd3[loopins], market);
      assertEquals("Curve building - forward 3M curve - instrument " + loopins, 0.0, pv[nbInstrumentsDsc + loopins].getAmount(eur), TOLERANCE);
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd6; loopins++) {
      pv[nbInstrumentsDsc + nbInstrumentsFwd3 + loopins] = PVC.visit(instrumentsFwd6[loopins], market);
      assertEquals("Curve building - forward 6M curve - instrument " + loopins, 0.0, pv[nbInstrumentsDsc + nbInstrumentsFwd3 + loopins].getAmount(eur), TOLERANCE);
    }
  }

  private MarketBundle discountingForwardBuild(PresentValueMarketCalculator pvc, InstrumentDerivative[] instruments, int nbCurve, int[] nbInstrumentsByCurve, double[] intrumentsTime,
      double[] marketRate, Map<Currency, Integer> discountingReferences, Map<IndexDeposit, Integer> forwardReferences) {
    int nbInstruments = instruments.length;
    CurrencyAmount[] marketValue = new CurrencyAmount[nbInstruments];
    for (int loopins = 0; loopins < nbInstruments; loopins++) {
      marketValue[loopins] = CurrencyAmount.of(forwardReferences.keySet().iterator().next().getCurrency(), 0);
    }
    double[][] nodePointsYieldCurve = new double[nbCurve][];
    int insNum = 0;
    for (int loopcurve = 0; loopcurve < nbCurve; loopcurve++) {
      nodePointsYieldCurve[loopcurve] = new double[nbInstrumentsByCurve[loopcurve]];
      System.arraycopy(intrumentsTime, insNum, nodePointsYieldCurve[loopcurve], 0, nbInstrumentsByCurve[loopcurve]);
      insNum += nbInstrumentsByCurve[loopcurve];
    }
    Interpolator1D[] interpolatorsYieldCurve = new Interpolator1D[nbCurve];
    final String interpolator = Interpolator1DFactory.DOUBLE_QUADRATIC;
    final CombinedInterpolatorExtrapolator extrapolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(interpolator, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    for (int loopins = 0; loopins < nbCurve; loopins++) {
      interpolatorsYieldCurve[loopins] = extrapolator;
    }
    String[] name = new String[nbCurve];
    for (IndexDeposit index : forwardReferences.keySet()) {
      name[forwardReferences.get(index)] = index.getName();
    }
    for (Currency ccy : discountingReferences.keySet()) {
      name[discountingReferences.get(ccy)] = ccy.toString() + " discounting";
    }
    MarketFinderDataBundle data = new MarketFinderDataBundle(instruments, marketValue, discountingReferences, forwardReferences, nodePointsYieldCurve, interpolatorsYieldCurve, name);
    MarketBundleFinderFunction func = new MarketBundleFinderFunction(pvc, data);
    final NewtonVectorRootFinder rootFinder = new BroydenVectorRootFinder(EPS, EPS, STEPS);
    final DoubleMatrix1D yieldCurveNodes = rootFinder.getRoot(func, new DoubleMatrix1D(marketRate));
    MarketBundle market = MarketBundleBuildingFunction.build(data, yieldCurveNodes);
    return market;
  }

  private MarketBundle discountingForwardBuild(PresentValueMarketCalculator pvc, MarketBundle knownMarket, InstrumentDerivative[] instruments, double[] intrumentsTime, double[] marketRate,
      Map<Currency, Integer> discountingReferences, Map<IndexDeposit, Integer> forwardReferences) {
    int nbInstruments = instruments.length;
    CurrencyAmount[] marketValue = new CurrencyAmount[nbInstruments];
    for (int loopins = 0; loopins < nbInstruments; loopins++) {
      marketValue[loopins] = CurrencyAmount.of(forwardReferences.keySet().iterator().next().getCurrency(), 0);
    }
    double[][] nodePointsYieldCurve = new double[1][nbInstruments];
    nodePointsYieldCurve[0] = intrumentsTime;
    final String interpolator = Interpolator1DFactory.DOUBLE_QUADRATIC;
    final CombinedInterpolatorExtrapolator extrapolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(interpolator, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    Interpolator1D[] interpolatorsYieldCurve = new Interpolator1D[] {extrapolator};
    String name = forwardReferences.keySet().iterator().next().toString();
    MarketFinderDataBundle data = new MarketFinderDataBundle(knownMarket, instruments, marketValue, discountingReferences, forwardReferences, nodePointsYieldCurve, interpolatorsYieldCurve,
        new String[] {name});
    MarketBundleFinderFunction func = new MarketBundleFinderFunction(pvc, data);
    final NewtonVectorRootFinder rootFinder = new BroydenVectorRootFinder(EPS, EPS, STEPS);
    final DoubleMatrix1D yieldCurveNodes = rootFinder.getRoot(func, new DoubleMatrix1D(marketRate));
    MarketBundle market = MarketBundleBuildingFunction.build(data, yieldCurveNodes);
    return market;
  }

  @Test
  /**
   * Build the inflation curve (HICP-XT) with the discounting curve known.
   */
  public void inflation1() {
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
    MarketBundle marketDsc = discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);

    InstrumentDerivative[] instrumentsInflation = CurveBuildingInstrumentsDataSets.instrumentsInflation();

    Map<IndexPrice, Integer> priceIndexReferences = new HashMap<IndexPrice, Integer>();
    @SuppressWarnings("unchecked")
    IndexPrice eurHicp = ((CouponInflationZeroCouponInterpolation) ((Swap<Coupon, Coupon>) instrumentsInflation[0]).getSecondLeg().getNthPayment(0)).getPriceIndex();
    priceIndexReferences.put(eurHicp, 0);

    int nbInstrumentsInflation = instrumentsInflation.length;
    ArrayZonedDateTimeDoubleTimeSeries hicpxTS = MarketDataSets.eurolandHICPXTFrom2005();
    ZonedDateTime[] indexDate = new ZonedDateTime[] {DateUtils.getUTCDate(2011, 7, 1), DateUtils.getUTCDate(2011, 8, 1), DateUtils.getUTCDate(2011, 9, 1)};

    int nbIndexDate = indexDate.length;
    double[] knownPointsPriceCurve = new double[nbIndexDate];
    for (int loopdate = 0; loopdate < nbIndexDate; loopdate++) {
      knownPointsPriceCurve[loopdate] = hicpxTS.getValue(indexDate[loopdate]);
    }
    double[] knownTime = TimeCalculator.getTimeBetween(CurveBuildingInstrumentsDataSets.referenceDate(), indexDate);
    double[] curveTime = new double[nbIndexDate + nbInstrumentsInflation];
    System.arraycopy(knownTime, 0, curveTime, 0, nbIndexDate);
    System.arraycopy(CurveBuildingInstrumentsDataSets.timeInflation(), 0, curveTime, nbIndexDate, nbInstrumentsInflation);

    MarketBundle marketInflation = inflationBuild(marketDsc, instrumentsInflation, curveTime, knownPointsPriceCurve, CurveBuildingInstrumentsDataSets.marketRateInflation(), priceIndexReferences);

    MultipleCurrencyAmount[] pv = new MultipleCurrencyAmount[nbInstrumentsInflation];
    for (int loopins = 0; loopins < nbInstrumentsInflation; loopins++) {
      pv[loopins] = PVC.visit(instrumentsInflation[loopins], marketInflation);
      assertEquals("Curve building - discounting curve - instrument " + loopins, 0.0, pv[loopins].getAmount(eur), TOLERANCE);
    }

  }

  private MarketBundle inflationBuild(MarketBundle knownMarket, InstrumentDerivative[] instruments, double[] nodeTime, double[] knownPointsPriceCurve, double[] marketRate,
      Map<IndexPrice, Integer> priceIndexReferences) {
    int nbInstruments = instruments.length;
    Interpolator1D[] interpolatorsPriceCurve = new Interpolator1D[] {Interpolator1DFactory.LINEAR_INSTANCE};
    CurrencyAmount[] marketValue = new CurrencyAmount[nbInstruments];
    for (int loopins = 0; loopins < nbInstruments; loopins++) {
      marketValue[loopins] = CurrencyAmount.of(priceIndexReferences.keySet().iterator().next().getCurrency(), 0);
    }
    double[][] nodePointsPriceCurve = new double[][] {nodeTime};
    double[][] knownPointsPriceCurveArray = new double[][] {knownPointsPriceCurve};
    String name = priceIndexReferences.keySet().iterator().next().toString();
    MarketFinderDataBundle dataInfl = new MarketFinderDataBundle(knownMarket, instruments, marketValue, priceIndexReferences, nodePointsPriceCurve, interpolatorsPriceCurve,
        knownPointsPriceCurveArray, new String[] {name});
    double[] indexStartValue = new double[nbInstruments];
    for (int loopins = 0; loopins < nbInstruments; loopins++) {
      indexStartValue[loopins] = knownPointsPriceCurve[0] * Math.pow(marketRate[loopins], loopins);
    }
    MarketBundleFinderFunction func = new MarketBundleFinderFunction(PVC, dataInfl);
    final NewtonVectorRootFinder rootFinder = new BroydenVectorRootFinder(EPS, EPS, STEPS);

    final DoubleMatrix1D yieldCurveNodes = rootFinder.getRoot(func, new DoubleMatrix1D(indexStartValue));
    return MarketBundleBuildingFunction.build(dataInfl, yieldCurveNodes);
  }

  @Test(enabled = true)
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

    MarketBundle marketDsc = discountingBuild(instrumentsDsc, intrumentsTimeDsc, marketRateDsc, discountingReferencesDsc, forwardReferencesDsc);

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < NB_TEST; looptest++) {
      marketDsc = discountingBuild(instrumentsDsc, intrumentsTimeDsc, marketRateDsc, discountingReferencesDsc, forwardReferencesDsc);
    }
    endTime = System.currentTimeMillis();
    System.out.println(NB_TEST + " discounting curve building (" + nbInstrumentsDsc + " instruments): " + (endTime - startTime) + " ms " + marketDsc.toString());
    // Performance note: Dsc building: 9-Nov-11: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 425 ms for 100 constructions (18 instruments - no Jacobian).

    IndexDeposit[] indexes = new IndexDeposit[] {eonia};
    final String interpolator = Interpolator1DFactory.DOUBLE_QUADRATIC;
    final CombinedInterpolatorExtrapolator extrainterpolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(interpolator, FLAT_EXTRAPOLATOR);
    MarketBundle market2 = MarketBundleBuilder.discounting(instrumentsDsc, marketRateDsc, eur, indexes, extrainterpolator, LTC, PVC);

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < NB_TEST; looptest++) {
      market2 = MarketBundleBuilder.discounting(instrumentsDsc, marketRateDsc, eur, indexes, extrainterpolator, LTC, PVC);
    }
    endTime = System.currentTimeMillis();
    System.out.println(NB_TEST + " discounting curve building - Builder (" + nbInstrumentsDsc + " instruments): " + (endTime - startTime) + " ms " + market2.toString());
    // Performance note: Dsc building: 5-Dec-11: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 415 ms for 100 constructions (18 instruments - no Jacobian).

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < NB_TEST; looptest++) {
      marketDsc = discountingBuild(instrumentsDsc, intrumentsTimeDsc, marketRateDsc, discountingReferencesDsc, forwardReferencesDsc);
    }
    endTime = System.currentTimeMillis();
    System.out.println(NB_TEST + " discounting curve building (" + nbInstrumentsDsc + " instruments): " + (endTime - startTime) + " ms " + marketDsc.toString());

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < NB_TEST; looptest++) {
      market2 = MarketBundleBuilder.discounting(instrumentsDsc, marketRateDsc, eur, indexes, extrainterpolator, LTC, PVC);
    }
    endTime = System.currentTimeMillis();
    System.out.println(NB_TEST + " discounting curve building - Builder (" + nbInstrumentsDsc + " instruments): " + (endTime - startTime) + " ms " + market2.toString());
  }

  @Test(enabled = false)
  /**
   * Performance.
   */
  public void performanceDscFwd3FullSwap() {
    long startTime, endTime;

    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsTimeDsc = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    int nbInstrumentsDsc = instrumentsDsc.length;
    InstrumentDerivative[] instrumentsFwd3 = CurveBuildingInstrumentsDataSets.instrumentsForward3FullSwap();
    double[] intrumentsTimeFwd3 = CurveBuildingInstrumentsDataSets.timeForward3FullSwap();
    double[] marketRateFwd3 = CurveBuildingInstrumentsDataSets.marketRateForward3FullSwap();
    int nbInstrumentsFwd3 = instrumentsFwd3.length;
    int nbInstruments = instrumentsDsc.length + instrumentsFwd3.length;
    double[] intrumentsTime = new double[nbInstruments];
    System.arraycopy(intrumentsTimeDsc, 0, intrumentsTime, 0, nbInstrumentsDsc);
    System.arraycopy(intrumentsTimeFwd3, 0, intrumentsTime, nbInstrumentsDsc, nbInstrumentsFwd3);
    double[] marketRate = new double[nbInstruments];
    System.arraycopy(marketRateDsc, 0, marketRate, 0, nbInstrumentsDsc);
    System.arraycopy(marketRateFwd3, 0, marketRate, nbInstrumentsDsc, nbInstrumentsFwd3);
    InstrumentDerivative[] instruments = new InstrumentDerivative[nbInstruments];
    for (int loopins = 0; loopins < nbInstrumentsDsc; loopins++) {
      instruments[loopins] = instrumentsDsc[loopins];
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd3; loopins++) {
      instruments[nbInstrumentsDsc + loopins] = instrumentsFwd3[loopins];
    }

    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();

    @SuppressWarnings("unchecked")
    IndexDeposit euribor3m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd3[0]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferencesFwd = new HashMap<Currency, Integer>();
    discountingReferencesFwd.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferencesFwd = new HashMap<IndexDeposit, Integer>();
    forwardReferencesFwd.put(eonia, 0);
    forwardReferencesFwd.put(euribor3m, 1);

    MarketBundle marketFwd = discountingForwardBuild(PVC, instruments, 2, new int[] {nbInstrumentsDsc, nbInstrumentsFwd3}, intrumentsTime, marketRate, discountingReferencesFwd, forwardReferencesFwd);

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < NB_TEST; looptest++) {
      marketFwd = discountingForwardBuild(PVC, instruments, 2, new int[] {nbInstrumentsDsc, nbInstrumentsFwd3}, intrumentsTime, marketRate, discountingReferencesFwd, forwardReferencesFwd);
    }
    endTime = System.currentTimeMillis();
    System.out.println(NB_TEST + " discounting/forward 3M (with swap) curve building (" + nbInstruments + " instruments): " + (endTime - startTime) + " ms " + marketFwd.toString());
    // Performance note: Dsc/Fwd building: 9-Nov-11: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 1925 ms for 100 constructions (36 instruments - no Jacobian).
  }

  @Test(enabled = false)
  /**
   * Performance.
   */
  public void performanceDscFwd3FraSwap() {
    long startTime, endTime;

    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsTimeDsc = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    int nbInstrumentsDsc = instrumentsDsc.length;
    InstrumentDerivative[] instrumentsFwd3 = CurveBuildingInstrumentsDataSets.instrumentsForward3FraSwap();
    double[] intrumentsTimeFwd3 = CurveBuildingInstrumentsDataSets.timeForward3FraSwap();
    double[] marketRateFwd3 = CurveBuildingInstrumentsDataSets.marketRateForward3FraSwap();
    int nbInstrumentsFwd3 = instrumentsFwd3.length;
    int nbInstruments = instrumentsDsc.length + instrumentsFwd3.length;
    double[] intrumentsTime = new double[nbInstruments];
    System.arraycopy(intrumentsTimeDsc, 0, intrumentsTime, 0, nbInstrumentsDsc);
    System.arraycopy(intrumentsTimeFwd3, 0, intrumentsTime, nbInstrumentsDsc, nbInstrumentsFwd3);
    double[] marketRate = new double[nbInstruments];
    System.arraycopy(marketRateDsc, 0, marketRate, 0, nbInstrumentsDsc);
    System.arraycopy(marketRateFwd3, 0, marketRate, nbInstrumentsDsc, nbInstrumentsFwd3);
    InstrumentDerivative[] instruments = new InstrumentDerivative[nbInstruments];
    for (int loopins = 0; loopins < nbInstrumentsDsc; loopins++) {
      instruments[loopins] = instrumentsDsc[loopins];
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd3; loopins++) {
      instruments[nbInstrumentsDsc + loopins] = instrumentsFwd3[loopins];
    }

    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();

    @SuppressWarnings("unchecked")
    IndexDeposit euribor3m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd3[0]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferencesFwd = new HashMap<Currency, Integer>();
    discountingReferencesFwd.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferencesFwd = new HashMap<IndexDeposit, Integer>();
    forwardReferencesFwd.put(eonia, 0);
    forwardReferencesFwd.put(euribor3m, 1);

    MarketBundle marketFwd = discountingForwardBuild(PVC, instruments, 2, new int[] {nbInstrumentsDsc, nbInstrumentsFwd3}, intrumentsTime, marketRate, discountingReferencesFwd, forwardReferencesFwd);

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < NB_TEST; looptest++) {
      marketFwd = discountingForwardBuild(PVC, instruments, 2, new int[] {nbInstrumentsDsc, nbInstrumentsFwd3}, intrumentsTime, marketRate, discountingReferencesFwd, forwardReferencesFwd);
    }
    endTime = System.currentTimeMillis();
    System.out.println(NB_TEST + " discounting/forward 3M (with FRA-swap) curve building (" + nbInstruments + " instruments): " + (endTime - startTime) + " ms " + marketFwd.toString());
    // Performance note: Dsc/Fwd building: 9-Nov-11: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 2150 ms for 100 constructions (40 instruments - no Jacobian).
  }

  @Test(enabled = false)
  /**
   * Performance.
   */
  public void performanceFwd3FullSwapAferDsc() {
    long startTime, endTime;
    // Discounting
    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsDscTime = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    int nbInstrumentsDsc = instrumentsDsc.length;
    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferences = new HashMap<Currency, Integer>();
    discountingReferences.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferences = new HashMap<IndexDeposit, Integer>();
    forwardReferences.put(eonia, 0);
    // Forward 3M
    InstrumentDerivative[] instrumentsFwd3 = CurveBuildingInstrumentsDataSets.instrumentsForward3FullSwap();
    double[] intrumentsTimeFwd3 = CurveBuildingInstrumentsDataSets.timeForward3FullSwap();
    double[] marketRateFwd3 = CurveBuildingInstrumentsDataSets.marketRateForward3FullSwap();
    int nbInstrumentsFwd3 = instrumentsFwd3.length;

    @SuppressWarnings("unchecked")
    IndexDeposit euribor3m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd3[0]).getSecondLeg().getNthPayment(0)).getIndex();

    Map<Currency, Integer> discountingReferencesFwd = new HashMap<Currency, Integer>();
    Map<IndexDeposit, Integer> forwardReferencesFwd = new HashMap<IndexDeposit, Integer>();
    forwardReferencesFwd.put(euribor3m, 0);

    MarketBundle marketDsc = discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);
    MarketBundle market = discountingForwardBuild(PVC, marketDsc, instrumentsFwd3, intrumentsTimeFwd3, marketRateFwd3, discountingReferencesFwd, forwardReferencesFwd);

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < NB_TEST; looptest++) {
      marketDsc = discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);
      market = discountingForwardBuild(PVC, marketDsc, instrumentsFwd3, intrumentsTimeFwd3, marketRateFwd3, discountingReferencesFwd, forwardReferencesFwd);
    }
    endTime = System.currentTimeMillis();
    System.out.println(NB_TEST + " forward (swap) after discounting curve building (" + nbInstrumentsDsc + " instruments dsc + " + nbInstrumentsFwd3 + " instruments fwd): " + (endTime - startTime)
        + " ms " + market.toString());
    // Performance note: Fwd6 after Fwd3 after Dsc building: 23-Nov-11: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 1500 ms for 100 constructions (18+18 instruments - no Jacobian).
  }

  @Test(enabled = false)
  /**
   * Performance.
   */
  public void performanceFwd3FutAferDsc() {
    long startTime, endTime;
    // Discounting
    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsDscTime = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    int nbInstrumentsDsc = instrumentsDsc.length;
    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferences = new HashMap<Currency, Integer>();
    discountingReferences.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferences = new HashMap<IndexDeposit, Integer>();
    forwardReferences.put(eonia, 0);
    // Forward 3M
    InstrumentDerivative[] instrumentsFwd3 = CurveBuildingInstrumentsDataSets.instrumentsForward3FutSwap();
    double[] intrumentsTimeFwd3 = CurveBuildingInstrumentsDataSets.timeForward3FutSwap();
    double[] marketRateFwd3 = CurveBuildingInstrumentsDataSets.marketRateForward3FutSwap();
    int nbInstrumentsFwd3 = instrumentsFwd3.length;
    @SuppressWarnings("unchecked")
    IndexDeposit euribor3m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd3[0]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferencesFwd = new HashMap<Currency, Integer>();
    Map<IndexDeposit, Integer> forwardReferencesFwd = new HashMap<IndexDeposit, Integer>();
    forwardReferencesFwd.put(euribor3m, 0);
    // Curve building
    MarketBundle marketDsc = discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);
    MarketBundle market = discountingForwardBuild(PVC, marketDsc, instrumentsFwd3, intrumentsTimeFwd3, marketRateFwd3, discountingReferencesFwd, forwardReferencesFwd);

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < NB_TEST; looptest++) {
      marketDsc = discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);
      market = discountingForwardBuild(PVC, marketDsc, instrumentsFwd3, intrumentsTimeFwd3, marketRateFwd3, discountingReferencesFwd, forwardReferencesFwd);
    }
    endTime = System.currentTimeMillis();
    System.out.println(NB_TEST + " forward (fut-swap) after discounting curve building (" + nbInstrumentsDsc + " instruments dsc + " + nbInstrumentsFwd3 + " instruments fwd): "
        + (endTime - startTime) + " ms " + market.toString());
    // Performance note: Fwd3 after Dsc building: 10-Nov-11: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 1685 ms for 100 constructions (18+22 instruments - no Jacobian).
  }

  @Test(enabled = false)
  /**
   * Performance.
   */
  public void performanceFwd3FutConvexityAferDsc() {
    long startTime, endTime;
    // Hull-White parameters
    double mr = 0.01;
    double[] sigma = {0.01, 0.011};
    HullWhiteOneFactorPiecewiseConstantParameters param = new HullWhiteOneFactorPiecewiseConstantParameters(mr, sigma, new double[] {1.0});
    // Discounting
    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsDscTime = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    int nbInstrumentsDsc = instrumentsDsc.length;
    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferences = new HashMap<Currency, Integer>();
    discountingReferences.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferences = new HashMap<IndexDeposit, Integer>();
    forwardReferences.put(eonia, 0);
    // Forward 3M
    InstrumentDerivative[] instrumentsFwd3 = CurveBuildingInstrumentsDataSets.instrumentsForward3FutSwap();
    double[] intrumentsTimeFwd3 = CurveBuildingInstrumentsDataSets.timeForward3FutSwap();
    double[] marketRateFwd3 = CurveBuildingInstrumentsDataSets.marketRateForward3FutSwap();
    int nbInstrumentsFwd3 = instrumentsFwd3.length;
    @SuppressWarnings("unchecked")
    IndexDeposit euribor3m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd3[0]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferencesFwd = new HashMap<Currency, Integer>();
    Map<IndexDeposit, Integer> forwardReferencesFwd = new HashMap<IndexDeposit, Integer>();
    forwardReferencesFwd.put(euribor3m, 0);
    // Curve building
    MarketBundle marketDsc = discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);
    MarketWithHullWhiteBundle marketDscHW = new MarketWithHullWhiteBundle(eur, param, marketDsc);
    MarketBundle marketDscFwdHW = discountingForwardBuild(PVC_HW, marketDscHW, instrumentsFwd3, intrumentsTimeFwd3, marketRateFwd3, discountingReferencesFwd, forwardReferencesFwd);

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < NB_TEST; looptest++) {
      marketDsc = discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);
      marketDscHW = new MarketWithHullWhiteBundle(eur, param, marketDsc);
      marketDscFwdHW = discountingForwardBuild(PVC_HW, marketDscHW, instrumentsFwd3, intrumentsTimeFwd3, marketRateFwd3, discountingReferencesFwd, forwardReferencesFwd);
    }
    endTime = System.currentTimeMillis();
    System.out.println(NB_TEST + " forward (fut conv-swap) after discounting curve building (" + nbInstrumentsDsc + " instruments dsc + " + nbInstrumentsFwd3 + " instruments fwd): "
        + (endTime - startTime) + " ms " + marketDscFwdHW.toString());
    // Performance note: Fwd3 after Dsc building: 22-Nov-11: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 1765 ms for 100 constructions (18+22 instruments - no Jacobian).
  }

  @Test(enabled = false)
  /**
   * Performance.
   */
  public void performanceDscFwd36FullSwap() {
    long startTime, endTime;

    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsTimeDsc = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    int nbInstrumentsDsc = instrumentsDsc.length;
    InstrumentDerivative[] instrumentsFwd3 = CurveBuildingInstrumentsDataSets.instrumentsForward3FullSwap();
    double[] intrumentsTimeFwd3 = CurveBuildingInstrumentsDataSets.timeForward3FullSwap();
    double[] marketRateFwd3 = CurveBuildingInstrumentsDataSets.marketRateForward3FullSwap();
    int nbInstrumentsFwd3 = instrumentsFwd3.length;
    InstrumentDerivative[] instrumentsFwd6 = CurveBuildingInstrumentsDataSets.instrumentsForward6FullSwap();
    double[] intrumentsTimeFwd6 = CurveBuildingInstrumentsDataSets.timeForward6FullSwap();
    double[] marketRateFwd6 = CurveBuildingInstrumentsDataSets.marketRateForward6FullSwap();
    int nbInstrumentsFwd6 = instrumentsFwd6.length;
    int nbInstruments = instrumentsDsc.length + instrumentsFwd3.length + instrumentsFwd6.length;
    double[] intrumentsTime = new double[nbInstruments];
    System.arraycopy(intrumentsTimeDsc, 0, intrumentsTime, 0, nbInstrumentsDsc);
    System.arraycopy(intrumentsTimeFwd3, 0, intrumentsTime, nbInstrumentsDsc, nbInstrumentsFwd3);
    System.arraycopy(intrumentsTimeFwd6, 0, intrumentsTime, nbInstrumentsDsc + nbInstrumentsFwd3, nbInstrumentsFwd6);
    double[] marketRate = new double[nbInstruments];
    System.arraycopy(marketRateDsc, 0, marketRate, 0, nbInstrumentsDsc);
    System.arraycopy(marketRateFwd3, 0, marketRate, nbInstrumentsDsc, nbInstrumentsFwd3);
    System.arraycopy(marketRateFwd6, 0, marketRate, nbInstrumentsDsc + nbInstrumentsFwd3, nbInstrumentsFwd6);
    InstrumentDerivative[] instruments = new InstrumentDerivative[nbInstruments];
    for (int loopins = 0; loopins < nbInstrumentsDsc; loopins++) {
      instruments[loopins] = instrumentsDsc[loopins];
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd3; loopins++) {
      instruments[nbInstrumentsDsc + loopins] = instrumentsFwd3[loopins];
    }
    for (int loopins = 0; loopins < nbInstrumentsFwd6; loopins++) {
      instruments[nbInstrumentsDsc + nbInstrumentsFwd3 + loopins] = instrumentsFwd6[loopins];
    }
    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();
    @SuppressWarnings("unchecked")
    IndexDeposit euribor3m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd3[0]).getSecondLeg().getNthPayment(0)).getIndex();
    @SuppressWarnings("unchecked")
    IndexDeposit euribor6m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd6[0]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferences = new HashMap<Currency, Integer>();
    discountingReferences.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferences = new HashMap<IndexDeposit, Integer>();
    forwardReferences.put(eonia, 0);
    forwardReferences.put(euribor3m, 1);
    forwardReferences.put(euribor6m, 2);

    MarketBundle market = discountingForwardBuild(PVC, instruments, 3, new int[] {nbInstrumentsDsc, nbInstrumentsFwd3, nbInstrumentsFwd6}, intrumentsTime, marketRate, discountingReferences,
        forwardReferences);

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < NB_TEST; looptest++) {
      market = discountingForwardBuild(PVC, instruments, 3, new int[] {nbInstrumentsDsc, nbInstrumentsFwd3, nbInstrumentsFwd6}, intrumentsTime, marketRate, discountingReferences, forwardReferences);
    }
    endTime = System.currentTimeMillis();
    System.out.println(NB_TEST + " discounting/forward 3M/forward 6M (with swap) curve building (" + nbInstruments + " instruments): " + (endTime - startTime) + " ms " + market.toString());
    // Performance note: Dsc/Fwd building: 9-Nov-11: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 3875 ms for 100 constructions (36 instruments - no Jacobian).
  }

  @Test(enabled = false)
  /**
   * Performance. Construction of discounting, forward 3m and forward 6m curves in succession (not concurrently).
   */
  public void performanceFwd6AfterFwd3AferDsc() {
    long startTime, endTime;

    // Discounting
    InstrumentDerivative[] instrumentsDsc = CurveBuildingInstrumentsDataSets.instrumentsDiscounting();
    double[] intrumentsDscTime = CurveBuildingInstrumentsDataSets.timeDiscounting();
    double[] marketRateDsc = CurveBuildingInstrumentsDataSets.marketRateDiscounting();
    int nbInstrumentsDsc = instrumentsDsc.length;
    Currency eur = ((Cash) instrumentsDsc[0]).getCurrency();
    @SuppressWarnings("unchecked")
    IndexDeposit eonia = ((CouponOIS) ((Swap<Payment, Payment>) instrumentsDsc[2]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferences = new HashMap<Currency, Integer>();
    discountingReferences.put(eur, 0);
    Map<IndexDeposit, Integer> forwardReferences = new HashMap<IndexDeposit, Integer>();
    forwardReferences.put(eonia, 0);
    // Forward 3M
    InstrumentDerivative[] instrumentsFwd3 = CurveBuildingInstrumentsDataSets.instrumentsForward3FullSwap();
    double[] intrumentsTimeFwd3 = CurveBuildingInstrumentsDataSets.timeForward3FullSwap();
    double[] marketRateFwd3 = CurveBuildingInstrumentsDataSets.marketRateForward3FullSwap();
    int nbInstrumentsFwd3 = instrumentsFwd3.length;
    @SuppressWarnings("unchecked")
    IndexDeposit euribor3m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd3[0]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferencesFwd3 = new HashMap<Currency, Integer>();
    Map<IndexDeposit, Integer> forwardReferencesFwd3 = new HashMap<IndexDeposit, Integer>();
    forwardReferencesFwd3.put(euribor3m, 0);
    // Forward 3M
    InstrumentDerivative[] instrumentsFwd6 = CurveBuildingInstrumentsDataSets.instrumentsForward6FullSwap();
    double[] intrumentsTimeFwd6 = CurveBuildingInstrumentsDataSets.timeForward6FullSwap();
    double[] marketRateFwd6 = CurveBuildingInstrumentsDataSets.marketRateForward6FullSwap();
    int nbInstrumentsFwd6 = instrumentsFwd6.length;
    @SuppressWarnings("unchecked")
    IndexDeposit euribor6m = ((CouponIbor) ((Swap<Payment, Payment>) instrumentsFwd6[0]).getSecondLeg().getNthPayment(0)).getIndex();
    Map<Currency, Integer> discountingReferencesFwd6 = new HashMap<Currency, Integer>();
    Map<IndexDeposit, Integer> forwardReferencesFwd6 = new HashMap<IndexDeposit, Integer>();
    forwardReferencesFwd6.put(euribor6m, 0);
    // Curve building
    MarketBundle marketDsc = discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);
    MarketBundle marketFwd3 = discountingForwardBuild(PVC, marketDsc, instrumentsFwd3, intrumentsTimeFwd3, marketRateFwd3, discountingReferencesFwd3, forwardReferencesFwd3);
    MarketBundle market = discountingForwardBuild(PVC, marketFwd3, instrumentsFwd6, intrumentsTimeFwd6, marketRateFwd6, discountingReferencesFwd6, forwardReferencesFwd6);

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < NB_TEST; looptest++) {
      marketDsc = discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);
      marketFwd3 = discountingForwardBuild(PVC, marketDsc, instrumentsFwd3, intrumentsTimeFwd3, marketRateFwd3, discountingReferencesFwd3, forwardReferencesFwd3);
      market = discountingForwardBuild(PVC, marketFwd3, instrumentsFwd6, intrumentsTimeFwd6, marketRateFwd6, discountingReferencesFwd6, forwardReferencesFwd6);
    }
    endTime = System.currentTimeMillis();
    System.out.println(NB_TEST + " forward 6M (swap) after forward 3M after discounting curve building (" + nbInstrumentsDsc + " dsc + " + nbInstrumentsFwd3 + " fwd3 + " + nbInstrumentsFwd6
        + " fwd6): " + (endTime - startTime) + " ms " + market.toString());
    // Performance note: Fwd after Dsc building: 10-Nov-11: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 2150 ms for 100 constructions (18+18+18 instruments - no Jacobian).
  }

  @Test(enabled = false)
  /**
   * Performance. Construction of discounting and inflation curves in succession (not concurrently).
   */
  public void performanceInflAferDsc() {
    long startTime, endTime;

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
    int nbInstrumentsDsc = instrumentsDsc.length;

    InstrumentDerivative[] instrumentsInflation = CurveBuildingInstrumentsDataSets.instrumentsInflation();
    Map<IndexPrice, Integer> priceIndexReferences = new HashMap<IndexPrice, Integer>();
    @SuppressWarnings("unchecked")
    IndexPrice eurHicp = ((CouponInflationZeroCouponInterpolation) ((Swap<Coupon, Coupon>) instrumentsInflation[0]).getSecondLeg().getNthPayment(0)).getPriceIndex();
    priceIndexReferences.put(eurHicp, 0);

    int nbInstrumentsInflation = instrumentsInflation.length;
    ArrayZonedDateTimeDoubleTimeSeries hicpxTS = MarketDataSets.eurolandHICPXTFrom2005();
    ZonedDateTime[] indexDate = new ZonedDateTime[] {DateUtils.getUTCDate(2011, 7, 1), DateUtils.getUTCDate(2011, 8, 1), DateUtils.getUTCDate(2011, 9, 1)};

    int nbIndexDate = indexDate.length;
    double[] knownPointsPriceCurve = new double[nbIndexDate];
    for (int loopdate = 0; loopdate < nbIndexDate; loopdate++) {
      knownPointsPriceCurve[loopdate] = hicpxTS.getValue(indexDate[loopdate]);
    }
    double[] knownTime = TimeCalculator.getTimeBetween(CurveBuildingInstrumentsDataSets.referenceDate(), indexDate);
    double[] curveTime = new double[nbIndexDate + nbInstrumentsInflation];
    System.arraycopy(knownTime, 0, curveTime, 0, nbIndexDate);
    System.arraycopy(CurveBuildingInstrumentsDataSets.timeInflation(), 0, curveTime, nbIndexDate, nbInstrumentsInflation);

    MarketBundle marketDsc = discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);
    MarketBundle marketInflation = inflationBuild(marketDsc, instrumentsInflation, curveTime, knownPointsPriceCurve, CurveBuildingInstrumentsDataSets.marketRateInflation(), priceIndexReferences);

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < NB_TEST; looptest++) {
      marketDsc = discountingBuild(instrumentsDsc, intrumentsDscTime, marketRateDsc, discountingReferences, forwardReferences);
      marketInflation = inflationBuild(marketDsc, instrumentsInflation, curveTime, knownPointsPriceCurve, CurveBuildingInstrumentsDataSets.marketRateInflation(), priceIndexReferences);
    }
    endTime = System.currentTimeMillis();
    System.out.println(NB_TEST + " inflation and discounting curve building (consecutive - instruments: " + nbInstrumentsDsc + " dsc + " + nbInstrumentsInflation + " infl): " + (endTime - startTime)
        + " ms " + marketInflation.toString());
    // Performance note: Inflation after Dsc building: 18-Nov-11: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 325 ms for 100 constructions (18 + 12 instruments - no Jacobian).
  }

}
