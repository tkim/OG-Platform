/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market.curvebuilding;

import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.instrument.index.PriceIndex;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.model.interestrate.curve.PriceIndexCurve;
import com.opengamma.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.math.curve.InterpolatedDoublesCurve;
import com.opengamma.math.matrix.DoubleMatrix1D;
import com.opengamma.util.money.Currency;

/**
 * Class constructing a MarketBundle from a set yield curve values at the node points.
 */
public class MarketBundleBuildingFunction {

  /**
   * Build a market bundle from a finder data bundle and a vector representing the different node rates.
   * @param data The finder data bundle.
   * @param x The vector with the different node points rates.
   * @return The market.
   */
  public static MarketBundle build(final MarketFinderDataBundle data, final DoubleMatrix1D x) {
    MarketBundle market = MarketBundle.from(data.getKnownMarket());
    // Yield curves
    YieldCurve[] curvesYield = new YieldCurve[data.getNbYieldCurve()];
    int p = 0;
    for (int loopcurve = 0; loopcurve < data.getNbYieldCurve(); loopcurve++) {
      int l = data.getNodePointsYieldCurve()[loopcurve].length;
      double[] y = new double[l];
      System.arraycopy(x.getData(), p, y, 0, l);
      p += l;
      InterpolatedDoublesCurve curveTmp = new InterpolatedDoublesCurve(data.getNodePointsYieldCurve()[loopcurve], y, data.getInterpolatorsYieldCurve()[loopcurve], true);
      curvesYield[loopcurve] = new YieldCurve(curveTmp);
    }
    for (Currency cur : data.getDiscountingReferences().keySet()) {
      market.setCurve(cur, curvesYield[data.getDiscountingReferences().get(cur)]);
    }
    for (IndexDeposit index : data.getForwardReferences().keySet()) {
      market.setCurve(index, curvesYield[data.getForwardReferences().get(index)]);
    }
    // Price index curves
    PriceIndexCurve[] curvesPrice = new PriceIndexCurve[data.getNbPriceCurve()];
    for (int loopcurve = 0; loopcurve < data.getNbPriceCurve(); loopcurve++) {
      int l = data.getNodePointsPriceCurve()[loopcurve].length;
      double[] y = new double[l];
      System.arraycopy(x.getData(), p, y, 0, l);
      p += l;
      InterpolatedDoublesCurve curveTmp = new InterpolatedDoublesCurve(data.getNodePointsPriceCurve()[loopcurve], y, data.getInterpolatorsPriceCurve()[loopcurve], true);
      curvesPrice[loopcurve] = new PriceIndexCurve(curveTmp);
    }
    for (PriceIndex index : data.getPriceIndexReferences().keySet()) {
      market.setCurve(index, curvesPrice[data.getPriceIndexReferences().get(index)]);
    }
    return market;
  }
}
