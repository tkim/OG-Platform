/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.core.position.impl;

import java.io.Serializable;
import java.util.Map;

import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectBean;
import org.joda.beans.impl.direct.DirectBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.core.position.Counterparty;
import com.opengamma.id.ExternalId;
import com.opengamma.util.ArgumentChecker;

/**
 * Simple implementation of {@code Counterparty}.
 * <p>
 * This is the simplest possible implementation of the {@link Counterparty} interface.
 * <p>
 * This class is mutable and not thread-safe.
 * It is intended to be used in the engine via the read-only {@code Counterparty} interface.
 */
/**
 * A simple mutable implementation of {@code Counterparty}.
 */
@BeanDefinition
public class SimpleCounterparty extends DirectBean
    implements Counterparty, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The counterparty identifier.
   */
  @PropertyDefinition(validate = "notNull")
  private ExternalId _externalId;

  /**
   * Creates an instance.
   */
  private SimpleCounterparty() {
  }

  /**
   * Creates an instance.
   * 
   * @param counterpartyId  the identifier, not null
   */
  public SimpleCounterparty(ExternalId counterpartyId) {
    ArgumentChecker.notNull(counterpartyId, "counterpartyId");
    setExternalId(counterpartyId);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SimpleCounterparty}.
   * @return the meta-bean, not null
   */
  public static SimpleCounterparty.Meta meta() {
    return SimpleCounterparty.Meta.INSTANCE;
  }
  static {
    JodaBeanUtils.registerMetaBean(SimpleCounterparty.Meta.INSTANCE);
  }

  @Override
  public SimpleCounterparty.Meta metaBean() {
    return SimpleCounterparty.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(String propertyName, boolean quiet) {
    switch (propertyName.hashCode()) {
      case -1699764666:  // externalId
        return getExternalId();
    }
    return super.propertyGet(propertyName, quiet);
  }

  @Override
  protected void propertySet(String propertyName, Object newValue, boolean quiet) {
    switch (propertyName.hashCode()) {
      case -1699764666:  // externalId
        setExternalId((ExternalId) newValue);
        return;
    }
    super.propertySet(propertyName, newValue, quiet);
  }

  @Override
  protected void validate() {
    JodaBeanUtils.notNull(_externalId, "externalId");
    super.validate();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SimpleCounterparty other = (SimpleCounterparty) obj;
      return JodaBeanUtils.equal(getExternalId(), other.getExternalId());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getExternalId());
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the counterparty identifier.
   * @return the value of the property, not null
   */
  public ExternalId getExternalId() {
    return _externalId;
  }

  /**
   * Sets the counterparty identifier.
   * @param externalId  the new value of the property, not null
   */
  public void setExternalId(ExternalId externalId) {
    JodaBeanUtils.notNull(externalId, "externalId");
    this._externalId = externalId;
  }

  /**
   * Gets the the {@code externalId} property.
   * @return the property, not null
   */
  public final Property<ExternalId> externalId() {
    return metaBean().externalId().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SimpleCounterparty}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code externalId} property.
     */
    private final MetaProperty<ExternalId> _externalId = DirectMetaProperty.ofReadWrite(
        this, "externalId", SimpleCounterparty.class, ExternalId.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<Object>> _map = new DirectMetaPropertyMap(
        this, null,
        "externalId");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1699764666:  // externalId
          return _externalId;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SimpleCounterparty> builder() {
      return new DirectBeanBuilder<SimpleCounterparty>(new SimpleCounterparty());
    }

    @Override
    public Class<? extends SimpleCounterparty> beanType() {
      return SimpleCounterparty.class;
    }

    @Override
    public Map<String, MetaProperty<Object>> metaPropertyMap() {
      return _map;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code externalId} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ExternalId> externalId() {
      return _externalId;
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
