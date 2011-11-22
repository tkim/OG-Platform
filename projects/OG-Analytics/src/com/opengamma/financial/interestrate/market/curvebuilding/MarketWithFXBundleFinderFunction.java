/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.market.curvebuilding;

import com.opengamma.financial.interestrate.InstrumentDerivativeVisitor;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.MarketWithFXBundle;
import com.opengamma.math.function.Function1D;
import com.opengamma.math.matrix.DoubleMatrix1D;
import com.opengamma.util.money.MultipleCurrencyAmount;

/**
 * Class used in curve building. Provides the fitting errors for a set of curve values.
 */
public class MarketWithFXBundleFinderFunction extends Function1D<DoubleMatrix1D, DoubleMatrix1D> {

  /**
   * Calculator used to compute the instruments values.
   */
  private final InstrumentDerivativeVisitor<MarketBundle, MultipleCurrencyAmount> _calculator;
  /**
   * The base data to construct the curves.
   */
  private final MarketFinderDataBundle _data;

  /**
   * Constructor.
   * @param calculator The calculator used to value the instruments.
   * @param data The market finder data bundle.
   */
  public MarketWithFXBundleFinderFunction(InstrumentDerivativeVisitor<MarketBundle, MultipleCurrencyAmount> calculator, MarketFinderDataBundle data) {
    _calculator = calculator;
    _data = data;
  }

  @Override
  public DoubleMatrix1D evaluate(DoubleMatrix1D x) {
    MarketBundle market = MarketBundleBuildingFunction.build(_data, x);
    MarketWithFXBundle marketFX = (MarketWithFXBundle) market;
    final double[] residual = new double[_data.getNbInstruments()];
    for (int loopins = 0; loopins < _data.getNbInstruments(); loopins++) {
      residual[loopins] = marketFX.getFXMatrix().convert(_calculator.visit(_data.getInstruments()[loopins], market), _data.getMarketValue()[loopins].getCurrency()).getAmount()
          - _data.getMarketValue()[loopins].getAmount();
    }
    return new DoubleMatrix1D(residual);
  }

}
