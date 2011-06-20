/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.master.historicaldata.impl;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;

import javax.time.calendar.LocalDate;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.opengamma.core.historicaldata.HistoricalTimeSeries;
import com.opengamma.id.Identifier;
import com.opengamma.id.IdentifierBundle;
import com.opengamma.id.UniqueIdentifier;
import com.opengamma.master.historicaldata.HistoricalTimeSeriesDocument;
import com.opengamma.master.historicaldata.HistoricalTimeSeriesGetRequest;
import com.opengamma.master.historicaldata.HistoricalTimeSeriesInfo;
import com.opengamma.master.historicaldata.HistoricalTimeSeriesResolver;
import com.opengamma.master.historicaldata.HistoricalTimeSeriesMaster;
import com.opengamma.master.historicaldata.HistoricalTimeSeriesSearchRequest;
import com.opengamma.master.historicaldata.HistoricalTimeSeriesSearchResult;
import com.opengamma.master.historicaldata.impl.MasterHistoricalTimeSeriesSource;
import com.opengamma.util.time.DateUtil;
import com.opengamma.util.timeseries.localdate.ArrayLocalDateDoubleTimeSeries;
import com.opengamma.util.timeseries.localdate.ListLocalDateDoubleTimeSeries;
import com.opengamma.util.timeseries.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.timeseries.localdate.MutableLocalDateDoubleTimeSeries;

/**
 * Test MasterHistoricalTimeSeriesSource.
 * Ensure it makes the right method calls to the underlying master and resolver.
 */
@Test
public class MasterHistoricalTimeSeriesSourceTest {

  private static final String TEST_CONFIG = "TEST_CONFIG";
  private static final UniqueIdentifier UID = UniqueIdentifier.of("A", "1");
  private static final String CLOSE_DATA_FIELD = "CLOSE";
  private static final String CMPL_DATA_PROVIDER = "CMPL";
  private static final String BBG_DATA_SOURCE = "BLOOMBERG";
  private static final IdentifierBundle IDENTIFIERS = IdentifierBundle.of(Identifier.of("A", "B"));
  
  private HistoricalTimeSeriesMaster _mockMaster;
  private HistoricalTimeSeriesResolver _mockResolver;
  private MasterHistoricalTimeSeriesSource _tsSource;

  @BeforeMethod
  public void setUp() throws Exception {
    _mockMaster = mock(HistoricalTimeSeriesMaster.class);
    _mockResolver = mock(HistoricalTimeSeriesResolver.class);
    _tsSource = new MasterHistoricalTimeSeriesSource(_mockMaster, _mockResolver);
  }

  @AfterMethod
  public void tearDown() throws Exception {
    _tsSource = null;
    _mockResolver = null;
    _mockMaster = null;
  }

  //-------------------------------------------------------------------------
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void constructorWith1ArgNull() throws Exception {
    HistoricalTimeSeriesResolver mock = mock(HistoricalTimeSeriesResolver.class);
    new MasterHistoricalTimeSeriesSource(null, mock);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void constructorWith2ArgNull() throws Exception {
    HistoricalTimeSeriesMaster mock = mock(HistoricalTimeSeriesMaster.class);
    new MasterHistoricalTimeSeriesSource(mock, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void constructorWithNull() throws Exception {
    new MasterHistoricalTimeSeriesSource(null, null);
  }

  public void getHistoricalTimeSeriesByIdentifierWithMetaData() throws Exception {
    HistoricalTimeSeriesSearchRequest request = new HistoricalTimeSeriesSearchRequest();
    request.setIdentifiers(IDENTIFIERS);
    request.setDataSource(BBG_DATA_SOURCE);
    request.setDataProvider(CMPL_DATA_PROVIDER);
    request.setDataField(CLOSE_DATA_FIELD);
    request.setStart(null);
    request.setEnd(null);
    request.setLoadTimeSeries(true);
    
    HistoricalTimeSeriesSearchResult searchResult = new HistoricalTimeSeriesSearchResult();
    HistoricalTimeSeriesDocument tsDoc = new HistoricalTimeSeriesDocument();
    tsDoc.setTimeSeries(randomTimeSeries());
    tsDoc.setUniqueId(UID);
    searchResult.getDocuments().add(tsDoc);
    
    when(_mockMaster.search(request)).thenReturn(searchResult);
    
    HistoricalTimeSeries hts = _tsSource.getHistoricalTimeSeries(IDENTIFIERS, BBG_DATA_SOURCE, CMPL_DATA_PROVIDER, CLOSE_DATA_FIELD);
    verify(_mockMaster, times(1)).search(request);
    
    assertEquals(UID, hts.getUniqueId());
    assertEquals(tsDoc.getTimeSeries().times(), hts.getTimeSeries().times());
    assertEquals(tsDoc.getTimeSeries().values(), hts.getTimeSeries().values());
  }

  public void getHistoricalTimeSeriesByIdentifierWithoutMetaData() throws Exception {
    HistoricalTimeSeriesSearchRequest request = new HistoricalTimeSeriesSearchRequest();
    request.setIdentifiers(IDENTIFIERS);
    request.setDataSource(BBG_DATA_SOURCE);
    request.setDataProvider(CMPL_DATA_PROVIDER);
    request.setDataField(CLOSE_DATA_FIELD);
    request.setStart(null);
    request.setEnd(null);
    request.setLoadTimeSeries(true);
    
    HistoricalTimeSeriesSearchResult searchResult = new HistoricalTimeSeriesSearchResult();
    HistoricalTimeSeriesDocument tsDoc = new HistoricalTimeSeriesDocument();
    tsDoc.setTimeSeries(randomTimeSeries());
    tsDoc.setUniqueId(UID);
    searchResult.getDocuments().add(tsDoc);
    
    HistoricalTimeSeriesInfo info = new HistoricalTimeSeriesInfo();
    info.setDataField(CLOSE_DATA_FIELD);
    info.setDataProvider(CMPL_DATA_PROVIDER);
    info.setDataSource(BBG_DATA_SOURCE);
    
    when(_mockMaster.search(request)).thenReturn(searchResult);
    when(_mockResolver.getInfo(IDENTIFIERS, TEST_CONFIG)).thenReturn(info);
    
    HistoricalTimeSeries hts = _tsSource.getHistoricalTimeSeries(IDENTIFIERS, TEST_CONFIG);
    verify(_mockMaster, times(1)).search(request);
    
    assertEquals(UID, hts.getUniqueId());
    assertEquals(tsDoc.getTimeSeries().times(), hts.getTimeSeries().times());
    assertEquals(tsDoc.getTimeSeries().values(), hts.getTimeSeries().values());
  }

  public void getHistoricalWithInclusiveExclusiveDates() throws Exception {
    
    LocalDate end = DateUtil.previousWeekDay();
    LocalDate start = end.minusDays(7);
    
    HistoricalTimeSeriesSearchRequest request = new HistoricalTimeSeriesSearchRequest();
    request.setIdentifiers(IDENTIFIERS);
    request.setDataSource(BBG_DATA_SOURCE);
    request.setDataProvider(CMPL_DATA_PROVIDER);
    request.setDataField(CLOSE_DATA_FIELD);
    request.setLoadTimeSeries(true);
    LocalDateDoubleTimeSeries timeSeries = randomTimeSeries();
    
    HistoricalTimeSeriesSearchResult searchResult = new HistoricalTimeSeriesSearchResult();
    HistoricalTimeSeriesDocument tsDoc = new HistoricalTimeSeriesDocument();
    tsDoc.setUniqueId(UID);
    searchResult.getDocuments().add(tsDoc);
    
    for (boolean startIncluded : new boolean[]{true, false})  {
      for (boolean endExcluded : new boolean[]{true, false}) {
        if (startIncluded) {
          request.setStart(start);
        } else {
          request.setStart(start.plusDays(1));
        }
        if (endExcluded) {
          request.setEnd(end.minusDays(1));
        } else {
          request.setEnd(end);
        }
        LocalDateDoubleTimeSeries subSeries = timeSeries.subSeries(start, startIncluded, end, endExcluded).toLocalDateDoubleTimeSeries();
        tsDoc.setTimeSeries(subSeries);
        when(_mockMaster.search(request)).thenReturn(searchResult);
        
        HistoricalTimeSeries hts = _tsSource.getHistoricalTimeSeries(IDENTIFIERS, BBG_DATA_SOURCE, CMPL_DATA_PROVIDER, CLOSE_DATA_FIELD, start, startIncluded, end, endExcluded);
        verify(_mockMaster, times(1)).search(request);
        assertEquals(UID, hts.getUniqueId());
        assertEquals(tsDoc.getTimeSeries(), hts.getTimeSeries());
      }
    }
  }

  private LocalDateDoubleTimeSeries randomTimeSeries() {
    MutableLocalDateDoubleTimeSeries dts = new ListLocalDateDoubleTimeSeries();
    LocalDate start = LocalDate.of(2000, 1, 2);
    LocalDate end = start.plusYears(10);
    LocalDate current = start;
    while (current.isBefore(end)) {
      current = current.plusDays(1);
      dts.putDataPoint(current, Math.random());
    }
    return dts;
  }

  public void getHistoricalTimeSeriesByUID() throws Exception {
    HistoricalTimeSeriesGetRequest request = new HistoricalTimeSeriesGetRequest(UID);
    request.setLoadEarliestLatest(false);
    request.setLoadTimeSeries(true);
    request.setStart(null);
    request.setEnd(null);
    
    HistoricalTimeSeriesDocument tsDoc = new HistoricalTimeSeriesDocument();
    tsDoc.setTimeSeries(new ArrayLocalDateDoubleTimeSeries());
    tsDoc.setUniqueId(UID);
    tsDoc.setTimeSeries(randomTimeSeries());
    
    when(_mockMaster.get(request)).thenReturn(tsDoc);
    
    HistoricalTimeSeries hts = _tsSource.getHistoricalTimeSeries(UID);
    verify(_mockMaster, times(1)).get(request);
    
    assertEquals(UID, hts.getUniqueId());
    assertEquals(tsDoc.getTimeSeries().times(), hts.getTimeSeries().times());
    assertEquals(tsDoc.getTimeSeries().values(), hts.getTimeSeries().values());
  }

}
