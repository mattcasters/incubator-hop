/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.apache.hop.core.util;

import java.util.prefs.Preferences;

import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.xml.XMLHandler;

import org.apache.hop.metastore.api.IMetaStore;
import org.w3c.dom.Node;

/**
 * @author <a href="mailto:thomas.hoedl@aschauer-edv.at">Thomas Hoedl(asc042)</a>
 *
 */
public class IntegerPluginProperty extends KeyValue<Integer> implements PluginProperty {

  /**
   * Serial version UID.
   */
  private static final long serialVersionUID = -2990345692552430357L;

  /**
   * Constructor. Value is null.
   *
   * @param key
   *          key to set.
   * @throws IllegalArgumentException
   *           if key is invalid.
   */
  public IntegerPluginProperty( final String key ) throws IllegalArgumentException {
    super( key, DEFAULT_INTEGER_VALUE );
  }

  /**
   */
  public boolean evaluate() {
    final Integer value = this.getValue();
    return value != null && value != 0;
  }

  /**
   */
  public void appendXml( final StringBuilder builder ) {
    builder.append( XMLHandler.addTagValue( this.getKey(), this.getValue() ) );
  }

  /**
   */
  public void loadXml( final Node node ) {
    final Integer value = Integer.parseInt( XMLHandler.getTagValue( node, this.getKey() ) );
    this.setValue( value );
  }

  /**
   */
  public void saveToPreferences( final Preferences node ) {
    node.putInt( this.getKey(), this.getValue() );
  }

  /**
   */
  public void readFromPreferences( final Preferences node ) {
    this.setValue( node.getInt( this.getKey(), this.getValue() ) );
  }

}