/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.analytics.financial.instrument;

import static org.testng.AssertJUnit.assertEquals;

import javax.time.calendar.Period;
import javax.time.calendar.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.analytics.financial.commodity.definition.AgricultureForwardDefinition;
import com.opengamma.analytics.financial.commodity.definition.AgricultureFutureDefinition;
import com.opengamma.analytics.financial.commodity.definition.AgricultureFutureOptionDefinition;
import com.opengamma.analytics.financial.commodity.definition.EnergyForwardDefinition;
import com.opengamma.analytics.financial.commodity.definition.EnergyFutureDefinition;
import com.opengamma.analytics.financial.commodity.definition.EnergyFutureOptionDefinition;
import com.opengamma.analytics.financial.commodity.definition.MetalForwardDefinition;
import com.opengamma.analytics.financial.commodity.definition.MetalFutureDefinition;
import com.opengamma.analytics.financial.commodity.definition.MetalFutureOptionDefinition;
import com.opengamma.analytics.financial.forex.definition.ForexDefinition;
import com.opengamma.analytics.financial.forex.definition.ForexNonDeliverableForwardDefinition;
import com.opengamma.analytics.financial.forex.definition.ForexNonDeliverableOptionDefinition;
import com.opengamma.analytics.financial.forex.definition.ForexOptionDigitalDefinition;
import com.opengamma.analytics.financial.forex.definition.ForexOptionSingleBarrierDefinition;
import com.opengamma.analytics.financial.forex.definition.ForexOptionVanillaDefinition;
import com.opengamma.analytics.financial.forex.definition.ForexSwapDefinition;
import com.opengamma.analytics.financial.instrument.annuity.AnnuityCouponCMSDefinition;
import com.opengamma.analytics.financial.instrument.annuity.AnnuityCouponFixedDefinition;
import com.opengamma.analytics.financial.instrument.annuity.AnnuityCouponIborDefinition;
import com.opengamma.analytics.financial.instrument.annuity.AnnuityCouponIborSpreadDefinition;
import com.opengamma.analytics.financial.instrument.annuity.AnnuityDefinition;
import com.opengamma.analytics.financial.instrument.bond.BillSecurityDefinition;
import com.opengamma.analytics.financial.instrument.bond.BillTransactionDefinition;
import com.opengamma.analytics.financial.instrument.bond.BondCapitalIndexedSecurityDefinition;
import com.opengamma.analytics.financial.instrument.bond.BondCapitalIndexedTransactionDefinition;
import com.opengamma.analytics.financial.instrument.bond.BondFixedSecurityDefinition;
import com.opengamma.analytics.financial.instrument.bond.BondFixedTransactionDefinition;
import com.opengamma.analytics.financial.instrument.bond.BondIborSecurityDefinition;
import com.opengamma.analytics.financial.instrument.bond.BondIborTransactionDefinition;
import com.opengamma.analytics.financial.instrument.cash.CashDefinition;
import com.opengamma.analytics.financial.instrument.cash.DepositCounterpartDefinition;
import com.opengamma.analytics.financial.instrument.cash.DepositIborDefinition;
import com.opengamma.analytics.financial.instrument.cash.DepositZeroDefinition;
import com.opengamma.analytics.financial.instrument.cds.ISDACDSDefinition;
import com.opengamma.analytics.financial.instrument.fra.ForwardRateAgreementDefinition;
import com.opengamma.analytics.financial.instrument.future.BondFutureDefinition;
import com.opengamma.analytics.financial.instrument.future.BondFutureOptionPremiumSecurityDefinition;
import com.opengamma.analytics.financial.instrument.future.BondFutureOptionPremiumTransactionDefinition;
import com.opengamma.analytics.financial.instrument.future.DeliverableSwapFuturesSecurityDefinition;
import com.opengamma.analytics.financial.instrument.future.FederalFundsFutureSecurityDefinition;
import com.opengamma.analytics.financial.instrument.future.FederalFundsFutureTransactionDefinition;
import com.opengamma.analytics.financial.instrument.future.FutureInstrumentsDescriptionDataSet;
import com.opengamma.analytics.financial.instrument.future.InterestRateFutureDefinition;
import com.opengamma.analytics.financial.instrument.future.InterestRateFutureOptionMarginSecurityDefinition;
import com.opengamma.analytics.financial.instrument.future.InterestRateFutureOptionMarginTransactionDefinition;
import com.opengamma.analytics.financial.instrument.future.InterestRateFutureOptionPremiumSecurityDefinition;
import com.opengamma.analytics.financial.instrument.future.InterestRateFutureOptionPremiumTransactionDefinition;
import com.opengamma.analytics.financial.instrument.index.IborIndex;
import com.opengamma.analytics.financial.instrument.index.IndexSwap;
import com.opengamma.analytics.financial.instrument.inflation.CouponInflationZeroCouponInterpolationDefinition;
import com.opengamma.analytics.financial.instrument.inflation.CouponInflationZeroCouponInterpolationGearingDefinition;
import com.opengamma.analytics.financial.instrument.inflation.CouponInflationZeroCouponMonthlyDefinition;
import com.opengamma.analytics.financial.instrument.inflation.CouponInflationZeroCouponMonthlyGearingDefinition;
import com.opengamma.analytics.financial.instrument.payment.CapFloorCMSDefinition;
import com.opengamma.analytics.financial.instrument.payment.CapFloorCMSSpreadDefinition;
import com.opengamma.analytics.financial.instrument.payment.CapFloorIborDefinition;
import com.opengamma.analytics.financial.instrument.payment.CouponCMSDefinition;
import com.opengamma.analytics.financial.instrument.payment.CouponFixedDefinition;
import com.opengamma.analytics.financial.instrument.payment.CouponIborCompoundedDefinition;
import com.opengamma.analytics.financial.instrument.payment.CouponIborDefinition;
import com.opengamma.analytics.financial.instrument.payment.CouponIborGearingDefinition;
import com.opengamma.analytics.financial.instrument.payment.CouponIborRatchetDefinition;
import com.opengamma.analytics.financial.instrument.payment.CouponIborSpreadDefinition;
import com.opengamma.analytics.financial.instrument.payment.CouponOISDefinition;
import com.opengamma.analytics.financial.instrument.payment.CouponOISSimplifiedDefinition;
import com.opengamma.analytics.financial.instrument.payment.PaymentDefinition;
import com.opengamma.analytics.financial.instrument.payment.PaymentFixedDefinition;
import com.opengamma.analytics.financial.instrument.swap.SwapDefinition;
import com.opengamma.analytics.financial.instrument.swap.SwapFixedIborDefinition;
import com.opengamma.analytics.financial.instrument.swap.SwapFixedIborSpreadDefinition;
import com.opengamma.analytics.financial.instrument.swap.SwapIborIborDefinition;
import com.opengamma.analytics.financial.instrument.swap.SwapXCcyIborIborDefinition;
import com.opengamma.analytics.financial.instrument.swaption.SwaptionBermudaFixedIborDefinition;
import com.opengamma.analytics.financial.instrument.swaption.SwaptionCashFixedIborDefinition;
import com.opengamma.analytics.financial.instrument.swaption.SwaptionInstrumentsDescriptionDataSet;
import com.opengamma.analytics.financial.instrument.swaption.SwaptionPhysicalFixedIborDefinition;
import com.opengamma.analytics.financial.instrument.swaption.SwaptionPhysicalFixedIborSpreadDefinition;
import com.opengamma.financial.convention.businessday.BusinessDayConvention;
import com.opengamma.financial.convention.businessday.BusinessDayConventionFactory;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.financial.convention.daycount.DayCount;
import com.opengamma.financial.convention.daycount.DayCountFactory;
import com.opengamma.util.money.Currency;
import com.opengamma.util.time.DateUtils;

/**
 * Class testing the Fixed income instrument definition visitor.
 */
public class InstrumentDefinitionVisitorTest {
  private static final Currency CUR = Currency.USD;
  private static final BusinessDayConvention BD = BusinessDayConventionFactory.INSTANCE.getBusinessDayConvention("Following");
  private static final Calendar C = new MondayToFridayCalendar("F");
  private static final CashDefinition CASH = new CashDefinition(CUR, DateUtils.getUTCDate(2011, 1, 2), DateUtils.getUTCDate(2012, 1, 2), 1.0, 0.04, 1.0);
  private static final ZonedDateTime SETTLE_DATE = DateUtils.getUTCDate(2011, 1, 1);
  private static final Period TENOR = Period.ofYears(2);
  private static final Period FIXED_PERIOD = Period.ofMonths(6);
  private static final DayCount FIXED_DAY_COUNT = DayCountFactory.INSTANCE.getDayCount("30/360");
  private static final boolean IS_EOM = true;
  private static final double NOTIONAL = 100000000; //100m
  private static final double FIXED_RATE = 0.05;
  private static final boolean IS_PAYER = true;
  private static final AnnuityCouponFixedDefinition ANNUITY_FIXED = AnnuityCouponFixedDefinition.from(CUR, SETTLE_DATE, TENOR, FIXED_PERIOD, C, FIXED_DAY_COUNT, BD,
      IS_EOM, NOTIONAL, FIXED_RATE, IS_PAYER);
  private static final Period IBOR_PERIOD_1 = Period.ofMonths(3);
  private static final int SPOT_LAG = 2;
  private static final DayCount IBOR_DAY_COUNT = DayCountFactory.INSTANCE.getDayCount("ACT/360");
  private static final IborIndex IBOR_INDEX_1 = new IborIndex(CUR, IBOR_PERIOD_1, SPOT_LAG, C, IBOR_DAY_COUNT, BD, IS_EOM);
  private static final IndexSwap CMS_INDEX = new IndexSwap(IBOR_PERIOD_1, IBOR_DAY_COUNT, IBOR_INDEX_1, IBOR_PERIOD_1);
  private static final AnnuityCouponIborDefinition ANNUITY_IBOR = AnnuityCouponIborDefinition.from(SETTLE_DATE, TENOR, NOTIONAL, IBOR_INDEX_1, !IS_PAYER);
  private static final Period IBOR_PERIOD_2 = Period.ofMonths(6);
  private static final IborIndex IBOR_INDEX_2 = new IborIndex(CUR, IBOR_PERIOD_2, SPOT_LAG, C, IBOR_DAY_COUNT, BD, IS_EOM);
  private static final double SPREAD = 0.001;
  private static final AnnuityCouponIborSpreadDefinition ANNUITY_IBOR_SPREAD_1 = AnnuityCouponIborSpreadDefinition.from(SETTLE_DATE, TENOR, NOTIONAL, IBOR_INDEX_2,
      SPREAD, !IS_PAYER);
  private static final AnnuityCouponIborSpreadDefinition ANNUITY_IBOR_SPREAD_2 = AnnuityCouponIborSpreadDefinition.from(SETTLE_DATE, TENOR, NOTIONAL, IBOR_INDEX_1, 0.0,
      IS_PAYER);
  private static final SwapFixedIborDefinition SWAP_FIXED_IBOR = new SwapFixedIborDefinition(ANNUITY_FIXED, ANNUITY_IBOR);
  private static final SwapFixedIborSpreadDefinition SWAP_FIXED_IBOR_SPREAD = new SwapFixedIborSpreadDefinition(ANNUITY_FIXED, ANNUITY_IBOR_SPREAD_1);
  private static final SwapIborIborDefinition SWAP_IBOR_IBOR = new SwapIborIborDefinition(ANNUITY_IBOR_SPREAD_2, ANNUITY_IBOR_SPREAD_1);
  private static final AnnuityDefinition<PaymentFixedDefinition> GENERAL_ANNUITY = new AnnuityDefinition<PaymentFixedDefinition>(new PaymentFixedDefinition[] {
      new PaymentFixedDefinition(CUR, DateUtils.getUTCDate(2011, 1, 1), 1000), new PaymentFixedDefinition(CUR, DateUtils.getUTCDate(2012, 1, 1), 1000)});
  private static final CouponCMSDefinition COUPON_CMS = CouponCMSDefinition.from(CouponIborDefinition.from(1000, SETTLE_DATE, IBOR_INDEX_1), CMS_INDEX);
  private static final AnnuityCouponCMSDefinition ANNUITY_COUPON_CMS = new AnnuityCouponCMSDefinition(new CouponCMSDefinition[] {COUPON_CMS});

  private static final InterestRateFutureDefinition IR_FUT_SECURITY_DEFINITION = FutureInstrumentsDescriptionDataSet.createInterestRateFutureSecurityDefinition();
  private static final BondFutureDefinition BNDFUT_SECURITY_DEFINITION = FutureInstrumentsDescriptionDataSet.createBondFutureSecurityDefinition();
  private static final SwaptionCashFixedIborDefinition SWAPTION_CASH = SwaptionInstrumentsDescriptionDataSet.createSwaptionCashFixedIborDefinition();
  private static final SwaptionPhysicalFixedIborDefinition SWAPTION_PHYS = SwaptionInstrumentsDescriptionDataSet.createSwaptionPhysicalFixedIborDefinition();

  @SuppressWarnings("synthetic-access")
  private static final MyVisitor<Object, String> VISITOR = new MyVisitor<Object, String>();

  @Test
  public void test() {
    final Object o = "G";
    assertEquals(CASH.accept(VISITOR), "Cash2");
    assertEquals(CASH.accept(VISITOR, o), "Cash1");
    assertEquals(ANNUITY_FIXED.accept(VISITOR), "Annuity2");
    assertEquals(ANNUITY_FIXED.accept(VISITOR, o), "Annuity1");
    assertEquals(ANNUITY_IBOR.accept(VISITOR), "Annuity2");
    assertEquals(ANNUITY_IBOR.accept(VISITOR, o), "Annuity1");
    assertEquals(ANNUITY_IBOR_SPREAD_1.accept(VISITOR), "Annuity2");
    assertEquals(ANNUITY_IBOR_SPREAD_1.accept(VISITOR, o), "Annuity1");
    assertEquals(SWAP_FIXED_IBOR.accept(VISITOR), "SwapFixedIbor2");
    assertEquals(SWAP_FIXED_IBOR.accept(VISITOR, o), "SwapFixedIbor1");
    assertEquals(SWAP_FIXED_IBOR_SPREAD.accept(VISITOR), "SwapFixedIborSpread2");
    assertEquals(SWAP_FIXED_IBOR_SPREAD.accept(VISITOR, o), "SwapFixedIborSpread1");
    assertEquals(SWAP_IBOR_IBOR.accept(VISITOR), "SwapIborIbor2");
    assertEquals(SWAP_IBOR_IBOR.accept(VISITOR, o), "SwapIborIbor1");
    assertEquals(GENERAL_ANNUITY.accept(VISITOR), "Annuity2");
    assertEquals(GENERAL_ANNUITY.accept(VISITOR, o), "Annuity1");
    assertEquals(COUPON_CMS.accept(VISITOR), "CouponCMS2");
    assertEquals(COUPON_CMS.accept(VISITOR, o), "CouponCMS1");
    assertEquals(ANNUITY_COUPON_CMS.accept(VISITOR), "Annuity2");
    assertEquals(ANNUITY_COUPON_CMS.accept(VISITOR, o), "Annuity1");
    assertEquals(IR_FUT_SECURITY_DEFINITION.accept(VISITOR), "InterestRateFutureSecurity1");
    assertEquals(IR_FUT_SECURITY_DEFINITION.accept(VISITOR, o), "InterestRateFutureSecurity2");
    assertEquals(BNDFUT_SECURITY_DEFINITION.accept(VISITOR), "BondFutureSecurity1");
    assertEquals(BNDFUT_SECURITY_DEFINITION.accept(VISITOR, o), "BondFutureSecurity2");
    assertEquals(SWAPTION_CASH.accept(VISITOR), "SwaptionCashFixedIbor1");
    assertEquals(SWAPTION_CASH.accept(VISITOR, o), "SwaptionCashFixedIbor2");
    assertEquals(SWAPTION_PHYS.accept(VISITOR), "SwaptionPhysicalFixedIbor1");
    assertEquals(SWAPTION_PHYS.accept(VISITOR, o), "SwaptionPhysicalFixedIbor2");
  }

  private static class MyVisitor<T, U> implements InstrumentDefinitionVisitor<T, String> {

    @Override
    public String visit(final InstrumentDefinition<?> definition, final T data) {
      return definition.accept(this, data);
    }

    @Override
    public String visit(final InstrumentDefinition<?> definition) {
      return definition.accept(this);
    }

    @Override
    public String visitCashDefinition(final CashDefinition cash, final T data) {
      return "Cash1";
    }

    @Override
    public String visitCashDefinition(final CashDefinition cash) {
      return "Cash2";
    }

    @Override
    public String visitPaymentFixedDefinition(final PaymentFixedDefinition payment, final T data) {
      return "PaymentFixed1";
    }

    @Override
    public String visitPaymentFixedDefinition(final PaymentFixedDefinition payment) {
      return "PaymentFixed2";
    }

    @Override
    public String visitCouponFixedDefinition(final CouponFixedDefinition payment, final T data) {
      return "CouponFixed1";
    }

    @Override
    public String visitCouponFixedDefinition(final CouponFixedDefinition payment) {
      return "CouponFixed2";
    }

    @Override
    public String visitCouponIborDefinition(final CouponIborDefinition payment, final T data) {
      return "CouponIbor1";
    }

    @Override
    public String visitCouponIborDefinition(final CouponIborDefinition payment) {
      return "CouponIbor2";
    }

    @Override
    public String visitCouponCMSDefinition(final CouponCMSDefinition payment, final T data) {
      return "CouponCMS1";
    }

    @Override
    public String visitCouponCMSDefinition(final CouponCMSDefinition payment) {
      return "CouponCMS2";
    }

    @Override
    public String visitCouponIborSpreadDefinition(final CouponIborSpreadDefinition payment, final T data) {
      return "CouponIborSpread1";
    }

    @Override
    public String visitCouponIborSpreadDefinition(final CouponIborSpreadDefinition payment) {
      return "CouponIborSpread2";
    }

    @Override
    public String visitAnnuityDefinition(final AnnuityDefinition<? extends PaymentDefinition> annuity, final T data) {
      return "Annuity1";
    }

    @Override
    public String visitAnnuityDefinition(final AnnuityDefinition<? extends PaymentDefinition> annuity) {
      return "Annuity2";
    }

    @Override
    public String visitSwapDefinition(final SwapDefinition swap, final T data) {
      return "Swap1";
    }

    @Override
    public String visitSwapDefinition(final SwapDefinition swap) {
      return "Swap2";
    }

    @Override
    public String visitSwapFixedIborDefinition(final SwapFixedIborDefinition swap, final T data) {
      return "SwapFixedIbor1";
    }

    @Override
    public String visitSwapFixedIborDefinition(final SwapFixedIborDefinition swap) {
      return "SwapFixedIbor2";
    }

    @Override
    public String visitSwapFixedIborSpreadDefinition(final SwapFixedIborSpreadDefinition swap, final T data) {
      return "SwapFixedIborSpread1";
    }

    @Override
    public String visitSwapFixedIborSpreadDefinition(final SwapFixedIborSpreadDefinition swap) {
      return "SwapFixedIborSpread2";
    }

    @Override
    public String visitSwapIborIborDefinition(final SwapIborIborDefinition swap, final T data) {
      return "SwapIborIbor1";
    }

    @Override
    public String visitSwapIborIborDefinition(final SwapIborIborDefinition swap) {
      return "SwapIborIbor2";
    }

    @Override
    public String visitForwardRateAgreementDefinition(final ForwardRateAgreementDefinition fra) {
      return "ForwardRateAgreement2";
    }

    @Override
    public String visitInterestRateFutureSecurityDefinition(final InterestRateFutureDefinition future, final T data) {
      return "InterestRateFutureSecurity2";
    }

    @Override
    public String visitInterestRateFutureSecurityDefinition(final InterestRateFutureDefinition future) {
      return "InterestRateFutureSecurity1";
    }

    @Override
    public String visitInterestRateFutureOptionPremiumSecurityDefinition(final InterestRateFutureOptionPremiumSecurityDefinition future, final T data) {
      return "InterestRateFutureOptionPremiumSecurity1";
    }

    @Override
    public String visitInterestRateFutureOptionPremiumSecurityDefinition(final InterestRateFutureOptionPremiumSecurityDefinition future) {
      return "InterestRateFutureOptionPremiumSecurity2";
    }

    @Override
    public String visitInterestRateFutureOptionPremiumTransactionDefinition(final InterestRateFutureOptionPremiumTransactionDefinition future, final T data) {
      return "InterestRateFutureOptionPremiumTransaction1";
    }

    @Override
    public String visitInterestRateFutureOptionPremiumTransactionDefinition(final InterestRateFutureOptionPremiumTransactionDefinition future) {
      return "InterestRateFutureOptionPremiumTransaction2";
    }

    @Override
    public String visitBondFixedTransactionDefinition(final BondFixedTransactionDefinition bond, final T data) {
      return "BondFixedTransaction1";
    }

    @Override
    public String visitBondFixedTransactionDefinition(final BondFixedTransactionDefinition bond) {
      return "BondFixedTransaction2";
    }

    @Override
    public String visitBondFixedSecurityDefinition(final BondFixedSecurityDefinition bond, final T data) {
      return "BondFixedSecurity1";
    }

    @Override
    public String visitBondFixedSecurityDefinition(final BondFixedSecurityDefinition bond) {
      return "BondFixedSecurity2";
    }

    @Override
    public String visitBondIborTransactionDefinition(final BondIborTransactionDefinition bond, final T data) {
      return "BondIborTransaction1";
    }

    @Override
    public String visitBondIborTransactionDefinition(final BondIborTransactionDefinition bond) {
      return "BondIborTransaction2";
    }

    @Override
    public String visitBondIborSecurityDefinition(final BondIborSecurityDefinition bond, final T data) {
      return "BondIborSecurity1";
    }

    @Override
    public String visitBondIborSecurityDefinition(final BondIborSecurityDefinition bond) {
      return "BondIborSecurity2";
    }

    @Override
    public String visitBondFutureSecurityDefinition(final BondFutureDefinition bond, final T data) {
      return "BondFutureSecurity2";
    }

    @Override
    public String visitBondFutureSecurityDefinition(final BondFutureDefinition bond) {
      return "BondFutureSecurity1";
    }

    @Override
    public String visitSwaptionCashFixedIborDefinition(final SwaptionCashFixedIborDefinition swaption, final T data) {
      return "SwaptionCashFixedIbor2";
    }

    @Override
    public String visitSwaptionCashFixedIborDefinition(final SwaptionCashFixedIborDefinition swaption) {
      return "SwaptionCashFixedIbor1";
    }

    @Override
    public String visitSwaptionPhysicalFixedIborDefinition(final SwaptionPhysicalFixedIborDefinition swaption, final T data) {
      return "SwaptionPhysicalFixedIbor2";
    }

    @Override
    public String visitSwaptionPhysicalFixedIborDefinition(final SwaptionPhysicalFixedIborDefinition swaption) {
      return "SwaptionPhysicalFixedIbor1";
    }

    @Override
    public String visitInterestRateFutureOptionMarginSecurityDefinition(final InterestRateFutureOptionMarginSecurityDefinition future, final T data) {
      return null;
    }

    @Override
    public String visitInterestRateFutureOptionMarginSecurityDefinition(final InterestRateFutureOptionMarginSecurityDefinition future) {
      return null;
    }

    @Override
    public String visitInterestRateFutureOptionMarginTransactionDefinition(final InterestRateFutureOptionMarginTransactionDefinition future, final T data) {
      return null;
    }

    @Override
    public String visitInterestRateFutureOptionMarginTransactionDefinition(final InterestRateFutureOptionMarginTransactionDefinition future) {
      return null;
    }

    @Override
    public String visitSwaptionBermudaFixedIborDefinition(final SwaptionBermudaFixedIborDefinition swaption, final T data) {
      return "SwaptionBermudaFixedIbor2";
    }

    @Override
    public String visitSwaptionBermudaFixedIborDefinition(final SwaptionBermudaFixedIborDefinition swaption) {
      return "SwaptionBermudaFixedIbor1";
    }

    @Override
    public String visitCapFloorCMSDefinition(final CapFloorCMSDefinition payment, final T data) {
      return null;
    }

    @Override
    public String visitCapFloorCMSDefinition(final CapFloorCMSDefinition payment) {
      return null;
    }

    @Override
    public String visitCouponInflationZeroCouponFirstOfMonth(final CouponInflationZeroCouponMonthlyDefinition coupon, final T data) {
      return null;
    }

    @Override
    public String visitCouponInflationZeroCouponFirstOfMonth(final CouponInflationZeroCouponMonthlyDefinition coupon) {
      return null;
    }

    @Override
    public String visitCouponInflationZeroCouponInterpolation(final CouponInflationZeroCouponInterpolationDefinition coupon, final T data) {
      return null;
    }

    @Override
    public String visitCouponInflationZeroCouponInterpolation(final CouponInflationZeroCouponInterpolationDefinition coupon) {
      return null;
    }

    @Override
    public String visitBondCapitalIndexedSecurity(final BondCapitalIndexedSecurityDefinition<?> bond, final T data) {
      return null;
    }

    @Override
    public String visitBondCapitalIndexedSecurity(final BondCapitalIndexedSecurityDefinition<?> bond) {
      return null;
    }

    @Override
    public String visitBondCapitalIndexedTransaction(final BondCapitalIndexedTransactionDefinition<?> bond, final T data) {
      return null;
    }

    @Override
    public String visitBondCapitalIndexedTransaction(final BondCapitalIndexedTransactionDefinition<?> bond) {
      return null;
    }

    @Override
    public String visitCouponInflationZeroCouponInterpolationGearing(final CouponInflationZeroCouponInterpolationGearingDefinition coupon, final T data) {
      return null;
    }

    @Override
    public String visitCouponInflationZeroCouponInterpolationGearing(final CouponInflationZeroCouponInterpolationGearingDefinition coupon) {
      return null;
    }

    @Override
    public String visitCouponInflationZeroCouponMonthlyGearing(final CouponInflationZeroCouponMonthlyGearingDefinition coupon, final T data) {
      return null;
    }

    @Override
    public String visitCouponInflationZeroCouponMonthlyGearing(final CouponInflationZeroCouponMonthlyGearingDefinition coupon) {
      return null;
    }

    @Override
    public String visitCouponOISSimplifiedDefinition(final CouponOISSimplifiedDefinition payment, final T data) {
      return null;
    }

    @Override
    public String visitCouponOISSimplifiedDefinition(final CouponOISSimplifiedDefinition payment) {
      return null;
    }

    @Override
    public String visitCouponOISDefinition(final CouponOISDefinition payment, final T data) {
      return null;
    }

    @Override
    public String visitCouponOISDefinition(final CouponOISDefinition payment) {
      return null;
    }

    @Override
    public String visitForexDefinition(final ForexDefinition fx, final T data) {
      return null;
    }

    @Override
    public String visitForexDefinition(final ForexDefinition fx) {
      return null;
    }

    @Override
    public String visitForexSwapDefinition(final ForexSwapDefinition fx, final T data) {
      return null;
    }

    @Override
    public String visitForexSwapDefinition(final ForexSwapDefinition fx) {
      return null;
    }

    @Override
    public String visitForexOptionVanillaDefinition(final ForexOptionVanillaDefinition fx, final T data) {
      return null;
    }

    @Override
    public String visitForexOptionVanillaDefinition(final ForexOptionVanillaDefinition fx) {
      return null;
    }

    @Override
    public String visitForexOptionSingleBarrierDefiniton(final ForexOptionSingleBarrierDefinition fx, final T data) {
      return null;
    }

    @Override
    public String visitForexOptionSingleBarrierDefiniton(final ForexOptionSingleBarrierDefinition fx) {
      return null;
    }

    @Override
    public String visitForexNonDeliverableForwardDefinition(final ForexNonDeliverableForwardDefinition ndf, final T data) {
      return null;
    }

    @Override
    public String visitForexNonDeliverableForwardDefinition(final ForexNonDeliverableForwardDefinition ndf) {
      return null;
    }

    @Override
    public String visitForexNonDeliverableOptionDefinition(final ForexNonDeliverableOptionDefinition ndo, final T data) {
      return null;
    }

    @Override
    public String visitForexNonDeliverableOptionDefinition(final ForexNonDeliverableOptionDefinition ndo) {
      return null;
    }

    @Override
    public String visitDepositIborDefinition(final DepositIborDefinition deposit, final T data) {
      return null;
    }

    @Override
    public String visitDepositIborDefinition(final DepositIborDefinition deposit) {
      return null;
    }

    @Override
    public String visitDepositCounterpartDefinition(final DepositCounterpartDefinition deposit, final T data) {
      return null;
    }

    @Override
    public String visitDepositCounterpartDefinition(final DepositCounterpartDefinition deposit) {
      return null;
    }

    @Override
    public String visitForexOptionDigitalDefinition(final ForexOptionDigitalDefinition fx, final T data) {
      return null;
    }

    @Override
    public String visitForexOptionDigitalDefinition(final ForexOptionDigitalDefinition fx) {
      return null;
    }

    @Override
    public String visitBillSecurityDefinition(final BillSecurityDefinition bill, final T data) {
      return null;
    }

    @Override
    public String visitBillSecurityDefinition(final BillSecurityDefinition bill) {
      return null;
    }

    @Override
    public String visitBillTransactionDefinition(final BillTransactionDefinition bill, final T data) {
      return null;
    }

    @Override
    public String visitBillTransactionDefinition(final BillTransactionDefinition bill) {
      return null;
    }

    @Override
    public String visitFederalFundsFutureSecurityDefinition(final FederalFundsFutureSecurityDefinition future, final T data) {
      return null;
    }

    @Override
    public String visitFederalFundsFutureSecurityDefinition(final FederalFundsFutureSecurityDefinition future) {
      return null;
    }

    @Override
    public String visitFederalFundsFutureTransactionDefinition(final FederalFundsFutureTransactionDefinition future, final T data) {
      return null;
    }

    @Override
    public String visitFederalFundsFutureTransactionDefinition(final FederalFundsFutureTransactionDefinition future) {
      return null;
    }

    @Override
    public String visitDepositZeroDefinition(final DepositZeroDefinition deposit, final T data) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visitDepositZeroDefinition(final DepositZeroDefinition deposit) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visitBondFutureOptionPremiumSecurityDefinition(final BondFutureOptionPremiumSecurityDefinition bond, final T data) {
      return null;
    }

    @Override
    public String visitBondFutureOptionPremiumSecurityDefinition(final BondFutureOptionPremiumSecurityDefinition bond) {
      return null;
    }

    @Override
    public String visitBondFutureOptionPremiumTransactionDefinition(final BondFutureOptionPremiumTransactionDefinition bond, final T data) {
      return null;
    }

    @Override
    public String visitBondFutureOptionPremiumTransactionDefinition(final BondFutureOptionPremiumTransactionDefinition bond) {
      return null;
    }

    @Override
    public String visitSwapXCcyIborIborDefinition(final SwapXCcyIborIborDefinition swap, final T data) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visitSwapXCcyIborIborDefinition(final SwapXCcyIborIborDefinition swap) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visitCouponIborGearingDefinition(final CouponIborGearingDefinition payment, final T data) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visitCouponIborGearingDefinition(final CouponIborGearingDefinition payment) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visitCouponIborCompoundedDefinition(final CouponIborCompoundedDefinition payment, final T data) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visitCouponIborCompoundedDefinition(final CouponIborCompoundedDefinition payment) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visitForwardRateAgreementDefinition(final ForwardRateAgreementDefinition payment, final T data) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visitCouponIborRatchetDefinition(final CouponIborRatchetDefinition payment, final T data) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visitCouponIborRatchetDefinition(final CouponIborRatchetDefinition payment) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visitCapFloorIborDefinition(final CapFloorIborDefinition payment, final T data) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visitCapFloorIborDefinition(final CapFloorIborDefinition payment) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visitCapFloorCMSSpreadDefinition(final CapFloorCMSSpreadDefinition payment, final T data) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visitCapFloorCMSSpreadDefinition(final CapFloorCMSSpreadDefinition payment) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visitSwaptionPhysicalFixedIborSpreadDefinition(final SwaptionPhysicalFixedIborSpreadDefinition swaption, final T data) {
      return null;
    }

    @Override
    public String visitSwaptionPhysicalFixedIborSpreadDefinition(final SwaptionPhysicalFixedIborSpreadDefinition swaption) {
      return null;
    }

    @Override
    public String visitDeliverableSwapFuturesSecurityDefinition(final DeliverableSwapFuturesSecurityDefinition futures, final T data) {
      return null;
    }

    @Override
    public String visitDeliverableSwapFuturesSecurityDefinition(final DeliverableSwapFuturesSecurityDefinition futures) {
      return null;
    }

    @Override
    public String visitCDSDefinition(ISDACDSDefinition cds, T data) {
      return null;
    }

    @Override
    public String visitCDSDefinition(ISDACDSDefinition cds) {
      return null;
    }

    @Override
    public String visitMetalForwardDefinition(MetalForwardDefinition future, T data) {
      return null;
    }

    @Override
    public String visitMetalForwardDefinition(MetalForwardDefinition future) {
      return null;
    }

    @Override
    public String visitMetalFutureDefinition(MetalFutureDefinition future, T data) {
      return null;
    }

    @Override
    public String visitMetalFutureDefinition(MetalFutureDefinition future) {
      return null;
    }

    @Override
    public String visitMetalFutureOptionDefinition(MetalFutureOptionDefinition future, T data) {
      return null;
    }

    @Override
    public String visitMetalFutureOptionDefinition(MetalFutureOptionDefinition future) {
      return null;
    }

    @Override
    public String visitAgricultureForwardDefinition(AgricultureForwardDefinition future, T data) {
      return null;
    }

    @Override
    public String visitAgricultureForwardDefinition(AgricultureForwardDefinition future) {
      return null;
    }

    @Override
    public String visitAgricultureFutureDefinition(AgricultureFutureDefinition future, T data) {
      return null;
    }

    @Override
    public String visitAgricultureFutureDefinition(AgricultureFutureDefinition future) {
      return null;
    }

    @Override
    public String visitAgricultureFutureOptionDefinition(AgricultureFutureOptionDefinition future, T data) {
      return null;
    }

    @Override
    public String visitAgricultureFutureOptionDefinition(AgricultureFutureOptionDefinition future) {
      return null;
    }

    @Override
    public String visitEnergyForwardDefinition(EnergyForwardDefinition future, T data) {
      return null;
    }

    @Override
    public String visitEnergyForwardDefinition(EnergyForwardDefinition future) {
      return null;
    }

    @Override
    public String visitEnergyFutureDefinition(EnergyFutureDefinition future, T data) {
      return null;
    }

    @Override
    public String visitEnergyFutureDefinition(EnergyFutureDefinition future) {
      return null;
    }

    @Override
    public String visitEnergyFutureOptionDefinition(EnergyFutureOptionDefinition future, T data) {
      return null;
    }

    @Override
    public String visitEnergyFutureOptionDefinition(EnergyFutureOptionDefinition future) {
      return null;
    }

  }
}
