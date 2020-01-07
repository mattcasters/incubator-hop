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

package org.apache.hop.job.entries.dummy;

import org.apache.hop.cluster.SlaveServer;
import org.apache.hop.core.Result;
import org.apache.hop.core.database.DatabaseMeta;
import org.apache.hop.core.exception.HopDatabaseException;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopXMLException;
import org.apache.hop.core.xml.XMLHandler;
import org.apache.hop.job.entry.JobEntryBase;
import org.apache.hop.job.entry.JobEntryInterface;

import org.apache.hop.metastore.api.IMetaStore;
import org.w3c.dom.Node;
import java.util.List;


/**
 * This defines an FTP job entry.
 *
 * @author Matt
 * @since 05-11-2003
 */

@org.apache.hop.core.annotations.JobEntry( id = "DummyJob", suggestion = "DummyPlugin.Job.SuggestedEntry",
      i18nPackageName = "pdi.jobentry.dummy", image = "deprecated.svg",
      name = "DummyPlugin.Job.Name", description = "DummyPlugin.Job.Description",
      categoryDescription = "i18n:org.apache.hop.job:JobCategory.Category.Deprecated" )
public class JobEntryDummy extends JobEntryBase implements Cloneable, JobEntryInterface {

  private static final String WILDCARD = "wildcard";

  private static final String TARGETDIRECTORY = "targetdirectory";

  private static final String SOURCEDIRECTORY = "sourcedirectory";

  private String sourceDirectory;

  private String targetDirectory;

  private String wildcard;

  public final String getSourceDirectory() {
    return sourceDirectory;
  }

  public final void setSourceDirectory( String sourceDirectory ) {
    this.sourceDirectory = sourceDirectory;
  }

  public final String getWildcard() {
    return wildcard;
  }

  public final void setWildcard( String wildcard ) {
    this.wildcard = wildcard;
  }

  public JobEntryDummy( String n ) {
    super( n, "" );
  }

  public JobEntryDummy() {
    this( "" );
  }

  @Override
  public Object clone() {
    JobEntryDummy je = (JobEntryDummy) super.clone();
    return je;
  }

  @Override
  public String getXML() {
    StringBuffer retval = new StringBuffer();

    retval.append( super.getXML() );

    retval.append( "      " + XMLHandler.addTagValue( SOURCEDIRECTORY, sourceDirectory ) );
    retval.append( "      " + XMLHandler.addTagValue( TARGETDIRECTORY, targetDirectory ) );
    retval.append( "      " + XMLHandler.addTagValue( WILDCARD, wildcard ) );

    return retval.toString();
  }

  @Override
  public void loadXML( Node entrynode, List<SlaveServer> slaveServers, IMetaStore metaStore ) throws HopXMLException {
    try {
      super.loadXML( entrynode, slaveServers );
      sourceDirectory = XMLHandler.getTagValue( entrynode, SOURCEDIRECTORY );
      targetDirectory = XMLHandler.getTagValue( entrynode, TARGETDIRECTORY );
      wildcard = XMLHandler.getTagValue( entrynode, WILDCARD );
    } catch ( HopXMLException xe ) {
      throw new HopXMLException( "Unable to load file exists job entry from XML node",
          xe );
    }
  }

  /**
   * @return Returns the targetDirectory.
   */
  public String getTargetDirectory() {
    return targetDirectory;
  }

  /**
   * @param targetDirectory
   *          The targetDirectory to set.
   */
  public void setTargetDirectory( String targetDirectory ) {
    this.targetDirectory = targetDirectory;
  }

  @Override
  public String getDialogClassName() {
    return JobEntryDummyDialog.class.getName();
  }

  @Override
  public Result execute( Result prev_result, int nr ) {
    Result result = new Result( nr );
    result.setResult( false );
    long filesRetrieved = 0;

    logDetailed( toString(), "Start of processing" );

    // String substitution..
    String realWildcard = environmentSubstitute( wildcard );
    String realTargetDirectory = environmentSubstitute( targetDirectory );
    String realSourceDirectory = environmentSubstitute( sourceDirectory );
    DummyJob proc = new DummyJob( realSourceDirectory, realTargetDirectory,
        realWildcard );

    try {
      filesRetrieved = proc.process();
      result.setResult( true );
      result.setNrFilesRetrieved( filesRetrieved );
    } catch ( Exception e ) {
      result.setNrErrors( 1 );
      e.printStackTrace();
      logError( toString(), "Error processing DummyJob : " + e.getMessage() );
    }

    return result;
  }

  @Override
  public boolean evaluates() {
    return true;
  }
}