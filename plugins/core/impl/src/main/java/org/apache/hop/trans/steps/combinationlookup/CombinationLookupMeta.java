/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
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

package org.apache.hop.trans.steps.combinationlookup;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.hop.core.CheckResult;
import org.apache.hop.core.CheckResultInterface;
import org.apache.hop.core.Const;
import org.apache.hop.core.annotations.Step;
import org.apache.hop.core.injection.AfterInjection;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.ProvidesModelerMeta;
import org.apache.hop.core.SQLStatement;
import org.apache.hop.core.database.Database;
import org.apache.hop.core.database.DatabaseMeta;
import org.apache.hop.core.exception.HopDatabaseException;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopStepException;
import org.apache.hop.core.exception.HopXMLException;
import org.apache.hop.core.injection.Injection;
import org.apache.hop.core.injection.InjectionSupported;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.RowMetaInterface;
import org.apache.hop.core.row.ValueMetaInterface;
import org.apache.hop.core.row.value.ValueMetaDate;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.variables.VariableSpace;
import org.apache.hop.core.xml.XMLHandler;
import org.apache.hop.i18n.BaseMessages;

import org.apache.hop.shared.SharedObjectInterface;
import org.apache.hop.trans.DatabaseImpact;
import org.apache.hop.trans.Trans;
import org.apache.hop.trans.TransMeta;
import org.apache.hop.trans.step.BaseStepMeta;
import org.apache.hop.trans.step.StepDataInterface;
import org.apache.hop.trans.step.StepInterface;
import org.apache.hop.trans.step.StepMeta;
import org.apache.hop.trans.step.StepMetaInterface;
import org.apache.hop.metastore.api.IMetaStore;
import org.w3c.dom.Node;

/*
 * Created on 14-may-2003
 *
 * TODO: In the distant future the use_autoinc flag should be removed since its
 *       functionality is now taken over by techKeyCreation (which is cleaner).
 */
@Step( id = "CombinationLookup", name = "BaseStep.TypeLongDesc.CombinationUpdate",
  categoryDescription = "BaseStep.Category.DataWarehouse", description = "BaseStep.TypeTooltipDesc.CombinationUpdate",
  i18nPackageName = "org.apache.hop.trans.step" )
@InjectionSupported( localizationPrefix = "CombinationLookup.Injection." )
public class CombinationLookupMeta extends BaseStepMeta implements StepMetaInterface,
  ProvidesModelerMeta {
  private static Class<?> PKG = CombinationLookupMeta.class; // for i18n purposes, needed by Translator2!!

  /**
   * Default cache size: 0 will cache everything
   */
  public static final int DEFAULT_CACHE_SIZE = 9999;

  private List<? extends SharedObjectInterface> databases;

  /**
   * what's the lookup schema?
   */
  @Injection( name = "SCHEMA_NAME" )
  private String schemaName;

  /**
   * what's the lookup table?
   */
  @Injection( name = "TABLE_NAME" )
  private String tablename;

  /**
   * database connection
   */
  private DatabaseMeta databaseMeta;

  /**
   * replace fields with technical key?
   */
  @Injection( name = "REPLACE_FIELDS" )
  private boolean replaceFields;

  /**
   * which fields do we use to look up a value?
   */
  @Injection( name = "KEY_FIELDS" )
  private String[] keyField;

  /**
   * With which fields in dimension do we look up?
   */
  @Injection( name = "KEY_LOOKUP" )
  private String[] keyLookup;

  /**
   * Use checksum algorithm to limit index size?
   */
  @Injection( name = "USE_HASH" )
  private boolean useHash;

  /**
   * Name of the CRC field in the dimension
   */
  @Injection( name = "HASH_FIELD" )
  private String hashField;

  /**
   * Technical Key field to return
   */
  @Injection( name = "TECHNICAL_KEY_FIELD" )
  private String technicalKeyField;

  /**
   * Where to get the sequence from...
   */
  @Injection( name = "SEQUENCE_FROM" )
  private String sequenceFrom;

  /**
   * Commit size for insert / update
   */
  @Injection( name = "COMMIT_SIZE" )
  private int commitSize;

  /**
   * Preload the cache, defaults to false
   *
   * @author nicow2
   */
  @Injection( name = "PRELOAD_CACHE" )
  private boolean preloadCache = false;

  /**
   * Limit the cache size to this!
   */
  @Injection( name = "CACHE_SIZE" )
  private int cacheSize;

  /**
   * Use the auto-increment feature of the database to generate keys.
   */
  @Injection( name = "AUTO_INC" )
  private boolean useAutoinc;

  /**
   * Which method to use for the creation of the tech key
   */
  @Injection( name = "TECHNICAL_KEY_CREATION" )
  private String techKeyCreation = null;

  @Injection( name = "LAST_UPDATE_FIELD" )
  private String lastUpdateField;

  public static String CREATION_METHOD_AUTOINC = "autoinc";
  public static String CREATION_METHOD_SEQUENCE = "sequence";
  public static String CREATION_METHOD_TABLEMAX = "tablemax";

  @Injection( name = "CONNECTIONNAME" )
  public void setConnection( String connectionName ) {
    databaseMeta = DatabaseMeta.findDatabase( getParentStepMeta().getParentTransMeta().getDatabases(), connectionName );
  }

  /**
   * @return Returns the database.
   */
  @Override
  public DatabaseMeta getDatabaseMeta() {
    return databaseMeta;
  }

  /**
   * @param database The database to set.
   */
  public void setDatabaseMeta( DatabaseMeta database ) {
    this.databaseMeta = database;
  }

  /**
   * Set the way how the technical key field should be created.
   *
   * @param techKeyCreation which method to use for the creation of the technical key.
   */
  public void setTechKeyCreation( String techKeyCreation ) {
    this.techKeyCreation = techKeyCreation;
  }

  /**
   * Get the way how the technical key field should be created.
   *
   * @return creation way for the technical key.
   */
  public String getTechKeyCreation() {
    return this.techKeyCreation;
  }

  /**
   * @return Returns the commitSize.
   */
  public int getCommitSize() {
    return commitSize;
  }

  /**
   * @param commitSize The commitSize to set.
   */
  public void setCommitSize( int commitSize ) {
    this.commitSize = commitSize;
  }

  /**
   * @return Returns the cacheSize.
   */
  public int getCacheSize() {
    return cacheSize;
  }

  /**
   * @param cacheSize The cacheSize to set.
   */
  public void setCacheSize( int cacheSize ) {
    this.cacheSize = cacheSize;
  }

  /**
   * @return Returns the hashField.
   */
  public String getHashField() {
    return hashField;
  }

  /**
   * @param hashField The hashField to set.
   */
  public void setHashField( String hashField ) {
    this.hashField = hashField;
  }

  /**
   * @return Returns the keyField (names in the stream).
   */
  public String[] getKeyField() {
    return keyField;
  }

  /**
   * @param keyField The keyField to set.
   */
  public void setKeyField( String[] keyField ) {
    this.keyField = keyField;
  }

  /**
   * @return Returns the keyLookup (names in the dimension table)
   */
  public String[] getKeyLookup() {
    return keyLookup;
  }

  /**
   * @param keyLookup The keyLookup to set.
   */
  public void setKeyLookup( String[] keyLookup ) {
    this.keyLookup = keyLookup;
  }

  /**
   * @return Returns the replaceFields.
   */
  public boolean replaceFields() {
    return replaceFields;
  }

  /**
   * @param replaceFields The replaceFields to set.
   */
  public void setReplaceFields( boolean replaceFields ) {
    this.replaceFields = replaceFields;
  }

  /**
   * @param preloadCache true to preload the cache
   */
  public void setPreloadCache( boolean preloadCache ) {
    this.preloadCache = preloadCache;
  }

  /**
   * @return Returns true if preload the cache.
   */
  public boolean getPreloadCache() {
    return preloadCache;
  }

  /**
   * @return Returns the sequenceFrom.
   */
  public String getSequenceFrom() {
    return sequenceFrom;
  }

  /**
   * @param sequenceFrom The sequenceFrom to set.
   */
  public void setSequenceFrom( String sequenceFrom ) {
    this.sequenceFrom = sequenceFrom;
  }

  /**
   * @return Returns the tablename.
   */
  @Override
  public String getTableName() {
    return tablename;
  }

  /**
   * @param tablename The tablename to set.
   */
  public void setTablename( String tablename ) {
    this.tablename = tablename;
  }

  /**
   * @return Returns the technicalKeyField.
   */
  public String getTechnicalKeyField() {
    return technicalKeyField;
  }

  /**
   * @param technicalKeyField The technicalKeyField to set.
   */
  public void setTechnicalKeyField( String technicalKeyField ) {
    this.technicalKeyField = technicalKeyField;
  }

  /**
   * @return Returns the useAutoinc.
   */
  public boolean isUseAutoinc() {
    return useAutoinc;
  }

  /**
   * @param useAutoinc The useAutoinc to set.
   */
  public void setUseAutoinc( boolean useAutoinc ) {
    this.useAutoinc = useAutoinc;
  }

  /**
   * @return Returns the useHash.
   */
  public boolean useHash() {
    return useHash;
  }

  /**
   * @param useHash The useHash to set.
   */
  public void setUseHash( boolean useHash ) {
    this.useHash = useHash;
  }

  @Override
  public void loadXML( Node stepnode, IMetaStore metaStore ) throws HopXMLException {
    readData( stepnode, metaStore );
  }

  public void allocate( int nrkeys ) {
    keyField = new String[ nrkeys ];
    keyLookup = new String[ nrkeys ];
  }

  @Override
  public Object clone() {
    CombinationLookupMeta retval = (CombinationLookupMeta) super.clone();

    int nrkeys = keyField.length;

    retval.allocate( nrkeys );
    System.arraycopy( keyField, 0, retval.keyField, 0, nrkeys );
    System.arraycopy( keyLookup, 0, retval.keyLookup, 0, nrkeys );

    return retval;
  }

  private void readData( Node stepnode, IMetaStore metaStore ) throws HopXMLException {
    this.databases = databases;
    try {
      String commit, csize;

      schemaName = XMLHandler.getTagValue( stepnode, "schema" );
      tablename = XMLHandler.getTagValue( stepnode, "table" );
      String con = XMLHandler.getTagValue( stepnode, "connection" );
      databaseMeta = DatabaseMeta.loadDatabase( metaStore, con );
      commit = XMLHandler.getTagValue( stepnode, "commit" );
      commitSize = Const.toInt( commit, 0 );
      csize = XMLHandler.getTagValue( stepnode, "cache_size" );
      cacheSize = Const.toInt( csize, 0 );

      replaceFields = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "replace" ) );
      preloadCache = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "preloadCache" ) );
      useHash = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "crc" ) );

      hashField = XMLHandler.getTagValue( stepnode, "crcfield" );

      Node keys = XMLHandler.getSubNode( stepnode, "fields" );
      int nrkeys = XMLHandler.countNodes( keys, "key" );

      allocate( nrkeys );

      // Read keys to dimension
      for ( int i = 0; i < nrkeys; i++ ) {
        Node knode = XMLHandler.getSubNodeByNr( keys, "key", i );
        keyField[ i ] = XMLHandler.getTagValue( knode, "name" );
        keyLookup[ i ] = XMLHandler.getTagValue( knode, "lookup" );
      }

      // If this is empty: use auto-increment field!
      sequenceFrom = XMLHandler.getTagValue( stepnode, "sequence" );

      Node fields = XMLHandler.getSubNode( stepnode, "fields" );
      Node retkey = XMLHandler.getSubNode( fields, "return" );
      technicalKeyField = XMLHandler.getTagValue( retkey, "name" );
      useAutoinc = !"N".equalsIgnoreCase( XMLHandler.getTagValue( retkey, "use_autoinc" ) );
      lastUpdateField = XMLHandler.getTagValue( stepnode, "last_update_field" );

      setTechKeyCreation( XMLHandler.getTagValue( retkey, "creation_method" ) );
    } catch ( Exception e ) {
      throw new HopXMLException( BaseMessages.getString(
        PKG, "CombinationLookupMeta.Exception.UnableToLoadStepInfo" ), e );
    }
  }

  @Override
  public void setDefault() {
    schemaName = "";
    tablename = BaseMessages.getString( PKG, "CombinationLookupMeta.DimensionTableName.Label" );
    databaseMeta = null;
    commitSize = 100;
    cacheSize = DEFAULT_CACHE_SIZE;
    replaceFields = false;
    preloadCache = false;
    useHash = false;
    hashField = "hashcode";
    int nrkeys = 0;

    allocate( nrkeys );

    // Read keys to dimension
    for ( int i = 0; i < nrkeys; i++ ) {
      keyField[ i ] = "key" + i;
      keyLookup[ i ] = "keylookup" + i;
    }

    technicalKeyField = "technical/surrogate key field";
    useAutoinc = false;
  }

  @Override
  public void getFields( RowMetaInterface row, String origin, RowMetaInterface[] info, StepMeta nextStep,
                         VariableSpace space, IMetaStore metaStore ) throws HopStepException {
    ValueMetaInterface v = new ValueMetaInteger( technicalKeyField );
    v.setLength( 10 );
    v.setPrecision( 0 );
    v.setOrigin( origin );
    row.addValueMeta( v );

    if ( replaceFields ) {
      for ( int i = 0; i < keyField.length; i++ ) {
        int idx = row.indexOfValue( keyField[ i ] );
        if ( idx >= 0 ) {
          row.removeValueMeta( idx );
        }
      }
    }
  }

  @Override
  public String getXML() {
    StringBuilder retval = new StringBuilder( 512 );

    retval.append( "      " ).append( XMLHandler.addTagValue( "schema", schemaName ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "table", tablename ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "connection",
      databaseMeta == null ? "" : databaseMeta.getName() ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "commit", commitSize ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "cache_size", cacheSize ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "replace", replaceFields ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "preloadCache", preloadCache ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "crc", useHash ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "crcfield", hashField ) );

    retval.append( "      <fields>" ).append( Const.CR );
    for ( int i = 0; i < keyField.length; i++ ) {
      retval.append( "        <key>" ).append( Const.CR );
      retval.append( "          " ).append( XMLHandler.addTagValue( "name", keyField[ i ] ) );
      retval.append( "          " ).append( XMLHandler.addTagValue( "lookup", keyLookup[ i ] ) );
      retval.append( "        </key>" ).append( Const.CR );
    }

    retval.append( "        <return>" ).append( Const.CR );
    retval.append( "          " ).append( XMLHandler.addTagValue( "name", technicalKeyField ) );
    retval.append( "          " ).append( XMLHandler.addTagValue( "creation_method", techKeyCreation ) );
    retval.append( "          " ).append( XMLHandler.addTagValue( "use_autoinc", useAutoinc ) );
    retval.append( "        </return>" ).append( Const.CR );

    retval.append( "      </fields>" ).append( Const.CR );

    // If sequence is empty: use auto-increment field!
    retval.append( "      " ).append( XMLHandler.addTagValue( "sequence", sequenceFrom ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "last_update_field", lastUpdateField ) );

    return retval.toString();
  }

  @Override
  public void check( List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta,
                     RowMetaInterface prev, String[] input, String[] output, RowMetaInterface info, VariableSpace space,
                     IMetaStore metaStore ) {
    CheckResult cr;
    String error_message = "";

    if ( databaseMeta != null ) {
      Database db = new Database( loggingObject, databaseMeta );
      try {
        db.connect();

        if ( !Utils.isEmpty( tablename ) ) {
          boolean first = true;
          boolean error_found = false;
          error_message = "";

          String schemaTable = databaseMeta.getQuotedSchemaTableCombination( schemaName, tablename );
          RowMetaInterface r = db.getTableFields( schemaTable );
          if ( r != null ) {
            for ( int i = 0; i < keyLookup.length; i++ ) {
              String lufield = keyLookup[ i ];

              ValueMetaInterface v = r.searchValueMeta( lufield );
              if ( v == null ) {
                if ( first ) {
                  first = false;
                  error_message +=
                    BaseMessages.getString( PKG, "CombinationLookupMeta.CheckResult.MissingCompareFields" )
                      + Const.CR;
                }
                error_found = true;
                error_message += "\t\t" + lufield + Const.CR;
              }
            }
            if ( error_found ) {
              cr = new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta );
            } else {
              cr =
                new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(
                  PKG, "CombinationLookupMeta.CheckResult.AllFieldsFound" ), stepMeta );
            }
            remarks.add( cr );

            /* Also, check the fields: tk, version, from-to, ... */
            if ( r.indexOfValue( technicalKeyField ) < 0 ) {
              error_message =
                BaseMessages.getString(
                  PKG, "CombinationLookupMeta.CheckResult.TechnicalKeyNotFound", technicalKeyField )
                  + Const.CR;
              cr = new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta );
            } else {
              error_message =
                BaseMessages.getString(
                  PKG, "CombinationLookupMeta.CheckResult.TechnicalKeyFound", technicalKeyField )
                  + Const.CR;
              cr = new CheckResult( CheckResultInterface.TYPE_RESULT_OK, error_message, stepMeta );
            }
            remarks.add( cr );
          } else {
            error_message =
              BaseMessages.getString( PKG, "CombinationLookupMeta.CheckResult.CouldNotReadTableInfo" );
            cr = new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta );
            remarks.add( cr );
          }
        }

        // Look up fields in the input stream <prev>
        if ( prev != null && prev.size() > 0 ) {
          boolean first = true;
          error_message = "";
          boolean error_found = false;

          for ( int i = 0; i < keyField.length; i++ ) {
            ValueMetaInterface v = prev.searchValueMeta( keyField[ i ] );
            if ( v == null ) {
              if ( first ) {
                first = false;
                error_message +=
                  BaseMessages.getString( PKG, "CombinationLookupMeta.CheckResult.MissingFields" ) + Const.CR;
              }
              error_found = true;
              error_message += "\t\t" + keyField[ i ] + Const.CR;
            }
          }
          if ( error_found ) {
            cr = new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta );
          } else {
            cr =
              new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(
                PKG, "CombinationLookupMeta.CheckResult.AllFieldsFoundInInputStream" ), stepMeta );
          }
          remarks.add( cr );
        } else {
          error_message =
            BaseMessages.getString( PKG, "CombinationLookupMeta.CheckResult.CouldNotReadFields" ) + Const.CR;
          cr = new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta );
          remarks.add( cr );
        }

        // Check sequence
        if ( databaseMeta.supportsSequences() && CREATION_METHOD_SEQUENCE.equals( getTechKeyCreation() ) ) {
          if ( Utils.isEmpty( sequenceFrom ) ) {
            error_message +=
              BaseMessages.getString( PKG, "CombinationLookupMeta.CheckResult.ErrorNoSequenceName" ) + "!";
            cr = new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta );
            remarks.add( cr );
          } else {
            // It doesn't make sense to check the sequence name
            // if it's not filled in.
            if ( db.checkSequenceExists( sequenceFrom ) ) {
              error_message =
                BaseMessages
                  .getString( PKG, "CombinationLookupMeta.CheckResult.ReadingSequenceOK", sequenceFrom );
              cr = new CheckResult( CheckResultInterface.TYPE_RESULT_OK, error_message, stepMeta );
              remarks.add( cr );
            } else {
              error_message +=
                BaseMessages.getString( PKG, "CombinationLookupMeta.CheckResult.ErrorReadingSequence" )
                  + sequenceFrom + "!";
              cr = new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta );
              remarks.add( cr );
            }
          }
        }

        if ( techKeyCreation != null ) {
          // post 2.2 version
          if ( !( CREATION_METHOD_AUTOINC.equals( techKeyCreation )
            || CREATION_METHOD_SEQUENCE.equals( techKeyCreation ) || CREATION_METHOD_TABLEMAX
            .equals( techKeyCreation ) ) ) {
            error_message +=
              BaseMessages.getString( PKG, "CombinationLookupMeta.CheckResult.ErrorTechKeyCreation" )
                + ": " + techKeyCreation + "!";
            cr = new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta );
            remarks.add( cr );
          }

        }
      } catch ( HopException e ) {
        error_message =
          BaseMessages.getString( PKG, "CombinationLookupMeta.CheckResult.ErrorOccurred" ) + e.getMessage();
        cr = new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta );
        remarks.add( cr );
      } finally {
        db.disconnect();
      }
    } else {
      error_message = BaseMessages.getString( PKG, "CombinationLookupMeta.CheckResult.InvalidConnection" );
      cr = new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta );
      remarks.add( cr );
    }

    // See if we have input streams leading to this step!
    if ( input.length > 0 ) {
      cr =
        new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "CombinationLookupMeta.CheckResult.ReceivingInfoFromOtherSteps" ), stepMeta );
      remarks.add( cr );
    } else {
      cr =
        new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "CombinationLookupMeta.CheckResult.NoInputReceived" ), stepMeta );
      remarks.add( cr );
    }
  }

  @Override
  public SQLStatement getSQLStatements( TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev,
                                        IMetaStore metaStore ) {
    SQLStatement retval = new SQLStatement( stepMeta.getName(), databaseMeta, null ); // default: nothing to do!

    int i;

    if ( databaseMeta != null ) {
      if ( prev != null && prev.size() > 0 ) {
        if ( !Utils.isEmpty( tablename ) ) {
          String schemaTable = databaseMeta.getQuotedSchemaTableCombination( schemaName, tablename );
          Database db = new Database( loggingObject, databaseMeta );
          try {
            boolean doHash = false;
            String cr_table = null;

            db.connect();

            // OK, what do we put in the new table??
            RowMetaInterface fields = new RowMeta();

            // First, the new technical key...
            ValueMetaInterface vkeyfield = new ValueMetaInteger( technicalKeyField );
            vkeyfield.setLength( 10 );
            vkeyfield.setPrecision( 0 );

            // Then the hashcode (optional)
            ValueMetaInterface vhashfield = null;
            if ( useHash && !Utils.isEmpty( hashField ) ) {
              vhashfield = new ValueMetaInteger( hashField );
              vhashfield.setLength( 15 );
              vhashfield.setPrecision( 0 );
              doHash = true;
            }

            // Then the last update field (optional)
            ValueMetaInterface vLastUpdateField = null;
            if ( !Utils.isEmpty( lastUpdateField ) ) {
              vLastUpdateField = new ValueMetaDate( lastUpdateField );
            }

            if ( !db.checkTableExists( schemaTable ) ) {
              // Add technical key field.
              fields.addValueMeta( vkeyfield );

              // Add the keys only to the table
              if ( keyField != null && keyLookup != null ) {
                int cnt = keyField.length;
                for ( i = 0; i < cnt; i++ ) {
                  String error_field = "";

                  // Find the value in the stream
                  ValueMetaInterface v = prev.searchValueMeta( keyField[ i ] );
                  if ( v != null ) {
                    String name = keyLookup[ i ];
                    ValueMetaInterface newValue = v.clone();
                    newValue.setName( name );

                    if ( name.equals( vkeyfield.getName() )
                      || ( doHash == true && name.equals( vhashfield.getName() ) ) ) {
                      error_field += name;
                    }
                    if ( error_field.length() > 0 ) {
                      retval.setError( BaseMessages.getString(
                        PKG, "CombinationLookupMeta.ReturnValue.NameCollision", error_field ) );
                    } else {
                      fields.addValueMeta( newValue );
                    }
                  }
                }
              }

              if ( doHash == true ) {
                fields.addValueMeta( vhashfield );
              }

              if ( vLastUpdateField != null ) {
                fields.addValueMeta( vLastUpdateField );
              }
            } else {
              // Table already exists

              // Get the fields that are in the table now:
              RowMetaInterface tabFields = db.getTableFields( schemaTable );

              // Don't forget to quote these as well...
              databaseMeta.quoteReservedWords( tabFields );

              if ( tabFields.searchValueMeta( vkeyfield.getName() ) == null ) {
                // Add technical key field if it didn't exist yet
                fields.addValueMeta( vkeyfield );
              }

              // Add the already existing fields
              int cnt = tabFields.size();
              for ( i = 0; i < cnt; i++ ) {
                ValueMetaInterface v = tabFields.getValueMeta( i );

                fields.addValueMeta( v );
              }

              // Find the missing fields in the real table
              String[] keyLookup = getKeyLookup();
              String[] keyField = getKeyField();
              if ( keyField != null && keyLookup != null ) {
                cnt = keyField.length;
                for ( i = 0; i < cnt; i++ ) {
                  // Find the value in the stream
                  ValueMetaInterface v = prev.searchValueMeta( keyField[ i ] );
                  if ( v != null ) {
                    ValueMetaInterface newValue = v.clone();
                    newValue.setName( keyLookup[ i ] );

                    // Does the corresponding name exist in the table
                    if ( tabFields.searchValueMeta( newValue.getName() ) == null ) {
                      fields.addValueMeta( newValue ); // nope --> add
                    }
                  }
                }
              }

              if ( doHash == true && tabFields.searchValueMeta( vhashfield.getName() ) == null ) {
                // Add hash field
                fields.addValueMeta( vhashfield );
              }

              if ( vLastUpdateField != null && tabFields.searchValueMeta( vLastUpdateField.getName() ) == null ) {
                fields.addValueMeta( vLastUpdateField );
              }
            }

            cr_table =
              db.getDDL(
                schemaTable, fields, ( CREATION_METHOD_SEQUENCE.equals( getTechKeyCreation() )
                  && sequenceFrom != null && sequenceFrom.length() != 0 ) ? null : technicalKeyField,
                CREATION_METHOD_AUTOINC.equals( getTechKeyCreation() ), null, true );

            //
            // OK, now let's build the index
            //

            // What fields do we put int the index?
            // Only the hashcode or all fields?
            String cr_index = "";
            String cr_uniq_index = "";
            String[] idx_fields = null;
            if ( useHash ) {
              if ( hashField != null && hashField.length() > 0 ) {
                idx_fields = new String[] { hashField };
              } else {
                retval.setError( BaseMessages.getString(
                  PKG, "CombinationLookupMeta.ReturnValue.NotHashFieldSpecified" ) );
              }
            } else {
              // index on all key fields...
              if ( !Utils.isEmpty( keyLookup ) ) {
                int nrfields = keyLookup.length;
                int maxFields = databaseMeta.getMaxColumnsInIndex();
                if ( maxFields > 0 && nrfields > maxFields ) {
                  nrfields = maxFields; // For example, oracle indexes are limited to 32 fields...
                }
                idx_fields = new String[ nrfields ];
                for ( i = 0; i < nrfields; i++ ) {
                  idx_fields[ i ] = keyLookup[ i ];
                }
              } else {
                retval.setError( BaseMessages.getString(
                  PKG, "CombinationLookupMeta.ReturnValue.NotFieldsSpecified" ) );
              }
            }

            // OK, now get the create index statement...

            if ( !Utils.isEmpty( technicalKeyField ) ) {
              String[] techKeyArr = new String[] { technicalKeyField };
              if ( !db.checkIndexExists( schemaTable, techKeyArr ) ) {
                String indexname = "idx_" + tablename + "_pk";
                cr_uniq_index =
                  db.getCreateIndexStatement(
                    schemaTable, indexname, techKeyArr, true, true, false, true );
                cr_uniq_index += Const.CR;
              }
            }

            // OK, now get the create lookup index statement...
            if ( !Utils.isEmpty( idx_fields ) && !db.checkIndexExists( schemaTable, idx_fields ) ) {
              String indexname = "idx_" + tablename + "_lookup";
              cr_index =
                db.getCreateIndexStatement(
                  schemaTable, indexname, idx_fields, false, false, false, true );
              cr_index += Const.CR;
            }

            //
            // Don't forget the sequence (optional)
            //
            String cr_seq = "";
            if ( databaseMeta.supportsSequences() && !Utils.isEmpty( sequenceFrom ) ) {
              if ( !db.checkSequenceExists( schemaName, sequenceFrom ) ) {
                cr_seq += db.getCreateSequenceStatement( schemaName, sequenceFrom, 1L, 1L, -1L, true );
                cr_seq += Const.CR;
              }
            }
            retval.setSQL( transMeta.environmentSubstitute( cr_table + cr_uniq_index + cr_index + cr_seq ) );
          } catch ( HopException e ) {
            retval.setError( BaseMessages.getString( PKG, "CombinationLookupMeta.ReturnValue.ErrorOccurred" )
              + Const.CR + e.getMessage() );
          }
        } else {
          retval.setError( BaseMessages.getString( PKG, "CombinationLookupMeta.ReturnValue.NotTableDefined" ) );
        }
      } else {
        retval.setError( BaseMessages.getString( PKG, "CombinationLookupMeta.ReturnValue.NotReceivingField" ) );
      }
    } else {
      retval.setError( BaseMessages.getString( PKG, "CombinationLookupMeta.ReturnValue.NotConnectionDefined" ) );
    }

    return retval;
  }

  @Override
  public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr,
                                TransMeta transMeta, Trans trans ) {
    return new CombinationLookup( stepMeta, stepDataInterface, cnr, transMeta, trans );
  }

  @Override
  public StepDataInterface getStepData() {
    return new CombinationLookupData();
  }

  @Override
  public void analyseImpact( List<DatabaseImpact> impact, TransMeta transMeta, StepMeta stepMeta,
                             RowMetaInterface prev, String[] input, String[] output, RowMetaInterface info,
                             IMetaStore metaStore ) {
    // The keys are read-only...
    for ( int i = 0; i < keyField.length; i++ ) {
      ValueMetaInterface v = prev.searchValueMeta( keyField[ i ] );
      DatabaseImpact ii =
        new DatabaseImpact(
          DatabaseImpact.TYPE_IMPACT_READ_WRITE, transMeta.getName(), stepMeta.getName(), databaseMeta
          .getDatabaseName(), tablename, keyLookup[ i ], keyField[ i ], v != null ? v.getOrigin() : "?", "",
          useHash ? BaseMessages.getString( PKG, "CombinationLookupMeta.ReadAndInsert.Label" ) : BaseMessages
            .getString( PKG, "CombinationLookupMeta.LookupAndInsert.Label" ) );
      impact.add( ii );
    }

    // Do we lookup-on the hash-field?
    if ( useHash ) {
      DatabaseImpact ii =
        new DatabaseImpact(
          DatabaseImpact.TYPE_IMPACT_READ_WRITE, transMeta.getName(), stepMeta.getName(),
          databaseMeta.getDatabaseName(), tablename, hashField, "", "", "",
          BaseMessages.getString( PKG, "CombinationLookupMeta.KeyLookup.Label" ) );
      impact.add( ii );
    }
  }

  @Override
  public DatabaseMeta[] getUsedDatabaseConnections() {
    if ( databaseMeta != null ) {
      return new DatabaseMeta[] { databaseMeta };
    } else {
      return super.getUsedDatabaseConnections();
    }
  }

  @Override
  public boolean equals( Object other ) {
    if ( other == this ) {
      return true;
    }
    if ( other == null ) {
      return false;
    }
    if ( getClass() != other.getClass() ) {
      return false;
    }
    CombinationLookupMeta o = (CombinationLookupMeta) other;

    if ( getCommitSize() != o.getCommitSize() ) {
      return false;
    }
    if ( getCacheSize() != o.getCacheSize() ) {
      return false;
    }
    if ( !getTechKeyCreation().equals( o.getTechKeyCreation() ) ) {
      return false;
    }
    if ( replaceFields() != o.replaceFields() ) {
      return false;
    }
    if ( useHash() != o.useHash() ) {
      return false;
    }
    if ( getPreloadCache() != o.getPreloadCache() ) {
      return false;
    }
    if ( ( getSequenceFrom() == null && o.getSequenceFrom() != null )
      || ( getSequenceFrom() != null && o.getSequenceFrom() == null )
      || ( getSequenceFrom() != null && o.getSequenceFrom() != null && !getSequenceFrom().equals(
      o.getSequenceFrom() ) ) ) {
      return false;
    }

    if ( ( getSchemaName() == null && o.getSchemaName() != null )
      || ( getSchemaName() != null && o.getSchemaName() == null )
      || ( getSchemaName() != null && o.getSchemaName() != null && !getSchemaName().equals( o.getSchemaName() ) ) ) {
      return false;
    }

    if ( ( getTableName() == null && o.getTableName() != null )
      || ( getTableName() != null && o.getTableName() == null )
      || ( getTableName() != null && o.getTableName() != null && !getTableName().equals( o.getTableName() ) ) ) {
      return false;
    }

    if ( ( getHashField() == null && o.getHashField() != null )
      || ( getHashField() != null && o.getHashField() == null )
      || ( getHashField() != null && o.getHashField() != null && !getHashField().equals( o.getHashField() ) ) ) {
      return false;
    }

    if ( ( getTechnicalKeyField() == null && o.getTechnicalKeyField() != null )
      || ( getTechnicalKeyField() != null && o.getTechnicalKeyField() == null )
      || ( getTechnicalKeyField() != null && o.getTechnicalKeyField() != null && !getTechnicalKeyField().equals(
      o.getTechnicalKeyField() ) ) ) {
      return false;
    }

    // comparison missing for the following, but can be added later
    // if required.
    // getKeyField()
    // getKeyLookup()

    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash( getCommitSize(), getCacheSize(), getTechKeyCreation(), replaceFields(), useHash(),
      getPreloadCache(), getSequenceFrom(), getSchemaName(), getTableName(), getHashField(), getTechnicalKeyField() );
  }

  /**
   * @return the schemaName
   */
  @Override
  public String getSchemaName() {
    return schemaName;
  }

  @Override public String getMissingDatabaseConnectionInformationMessage() {
    return null;
  }

  /**
   * @param schemaName the schemaName to set
   */
  public void setSchemaName( String schemaName ) {
    this.schemaName = schemaName;
  }

  /**
   * @return the lastUpdateField
   */
  public String getLastUpdateField() {
    return lastUpdateField;
  }

  /**
   * @param lastUpdateField the lastUpdateField to set
   */
  public void setLastUpdateField( String lastUpdateField ) {
    this.lastUpdateField = lastUpdateField;
  }

  @Override
  public boolean supportsErrorHandling() {
    return true;
  }

  protected RowMetaInterface getDatabaseTableFields( Database db, String schemaName, String tableName )
    throws HopDatabaseException {
    // First try without connecting to the database... (can be S L O W)
    String schemaTable = databaseMeta.getQuotedSchemaTableCombination( schemaName, tableName );
    RowMetaInterface extraFields = db.getTableFields( schemaTable );
    if ( extraFields == null ) { // now we need to connect
      db.connect();
      extraFields = db.getTableFields( schemaTable );
    }
    return extraFields;
  }

  Database createDatabaseObject() {
    return new Database( loggingObject, databaseMeta );
  }

  @Override public RowMeta getRowMeta( StepDataInterface stepData ) {
    try {
      return (RowMeta) getDatabaseTableFields( createDatabaseObject(), schemaName, getTableName() );
    } catch ( HopDatabaseException e ) {
      log.logError( "", e );
      return new RowMeta();
    }
  }

  @Override public List<String> getDatabaseFields() {
    return Arrays.asList( keyLookup );
  }

  @Override public List<String> getStreamFields() {
    return Arrays.asList( keyField );
  }

  /**
   * If we use injection we can have different arrays lengths. We need synchronize them for consistency behavior with
   * UI
   */
  @AfterInjection
  public void afterInjectionSynchronization() {
    int nrFields = ( keyField == null ) ? -1 : keyField.length;
    if ( nrFields <= 0 ) {
      return;
    }
    String[][] rtn = Utils.normalizeArrays( nrFields, keyLookup );
    keyLookup = rtn[ 0 ];
  }
}