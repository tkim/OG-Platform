/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market.curvebuilding;

import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.model.interestrate.curve.YieldAndDiscountCurve;
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
    YieldAndDiscountCurve[] curves = new YieldAndDiscountCurve[data.getNbYieldCurve()];
    int p = 0;
    for (int loopcurve = 0; loopcurve < data.getNbYieldCurve(); loopcurve++) {
      int l = data.getNodePointsYieldCurve()[loopcurve].length;
      double[] y = new double[l];
      System.arraycopy(x.getData(), p, y, 0, l);
      p += l;
      InterpolatedDoublesCurve curve = new InterpolatedDoublesCurve(data.getNodePointsYieldCurve()[loopcurve], y, data.getInterpolatorsYieldCurve()[loopcurve], true);
      curves[loopcurve] = new YieldCurve(curve);
    }
    MarketBundle market = MarketBundle.from(data.getKnownMarket());
    for (Currency cur : data.getDiscountingReferences().keySet()) {
      market.setCurve(cur, curves[data.getDiscountingReferences().get(cur)]);
    }
    for (IndexDeposit index : data.getForwardReferences().keySet()) {
      market.setCurve(index, curves[data.getForwardReferences().get(index)]);
    }
    return market;
  }

}
