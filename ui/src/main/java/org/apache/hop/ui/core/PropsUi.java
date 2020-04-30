/*! ******************************************************************************
 *
 * Hop : The Hop Orchestration Platform
 *
 * http://www.project-hop.org
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

package org.apache.hop.ui.core;

import org.apache.commons.lang.StringUtils;
import org.apache.hop.core.Const;
import org.apache.hop.core.Props;
import org.apache.hop.core.gui.IGuiPosition;
import org.apache.hop.core.gui.Point;
import org.apache.hop.core.logging.LogChannel;
import org.apache.hop.core.plugins.IPlugin;
import org.apache.hop.core.plugins.PluginRegistry;
import org.apache.hop.core.util.Utils;
import org.apache.hop.history.AuditState;
import org.apache.hop.laf.BasePropertyHandler;
import org.apache.hop.ui.core.gui.GuiResource;
import org.apache.hop.ui.core.gui.WindowProperty;
import org.apache.hop.ui.hopgui.HopGui;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * We use Props to store all kinds of user interactive information such as the selected colors, fonts, positions of
 * windows, etc.
 *
 * @author Matt
 * @since 15-12-2003
 */
public class PropsUi extends Props {

  private static String OS = System.getProperty( "os.name" ).toLowerCase();

  private static final String NO = "N";

  private static final String YES = "Y";

  private static Display display;
  private static double nativeZoomFactor;

  private static final String STRING_SHOW_COPY_OR_DISTRIBUTE_WARNING = "ShowCopyOrDistributeWarning";

  private static final String SHOW_TOOL_TIPS = "ShowToolTips";

  private static final String SHOW_HELP_TOOL_TIPS = "ShowHelpToolTips";

  private static final String CANVAS_GRID_SIZE = "CanvasGridSize";

  private static final String LEGACY_PERSPECTIVE_MODE = "LegacyPerspectiveMode";

  private static final String DISABLE_BROWSER_ENVIRONMENT_CHECK = "DisableBrowserEnvironmentCheck";

  /**
   * Initialize the properties: load from disk.
   *
   * @param d The Display
   */
  public static void init( Display d ) {
    if ( props == null ) {
      display = d;
      props = new PropsUi();

      // Calculate the native default zoom factor...
      // We take the default font and render it, calculate the height.
      // Compare that to the standard small icon size of 16
      //
      Image image = new Image( display, 500, 500 );
      GC gc = new GC( image );
      org.eclipse.swt.graphics.Point extent = gc.textExtent( "The quick brown fox jumped over the lazy dog!" );
      nativeZoomFactor = (double) extent.y / (double) ConstUi.SMALL_ICON_SIZE;
      gc.dispose();
      image.dispose();

      // Also init the colors and fonts to use...
      // The icons and so on are corrected with a zoom factor
      //
      GuiResource.getInstance();
    } else {
      throw new RuntimeException( "The Properties systems settings are already initialised!" );
    }
  }

  /**
   * Initialize the properties: load from disk.
   *
   * @param d        The Display
   * @param filename the filename to use
   */
  public static void init( Display d, String filename ) {
    if ( props == null ) {
      display = d;
      props = new PropsUi( filename );

      // Also init the colors and fonts to use...
      GuiResource.getInstance();
    } else {
      throw new RuntimeException( "The properties systems settings are already initialised!" );
    }
  }

  public static PropsUi getInstance() {
    if ( props != null ) {
      return (PropsUi) props;
    }

    throw
      new RuntimeException( "Properties, Hop systems settings, not initialised!" );
  }

  private PropsUi() {
    super();
    initialize();
  }

  private PropsUi( String filename ) {
    super( filename );
    initialize();
  }

  @Override
  protected synchronized void initialize() {
    super.createLogChannel();
    super.initialize();
    properties = new Properties();

    setDefault();
    loadProps();
    addDefaultEntries();
  }

  public void setDefault() {

    properties.setProperty( STRING_LOG_LEVEL, getLogLevel() );
    properties.setProperty( STRING_LOG_FILTER, getLogFilter() );

    if ( display != null ) {
      // Set Default Look for all dialogs and sizes.
      String prop = BasePropertyHandler.getProperty( "Default_UI_Properties_Resource", "org.apache.hop.ui.core.default" );
      try {
        ResourceBundle bundle = PropertyResourceBundle.getBundle( prop );
        if ( bundle != null ) {
          Enumeration<String> enumer = bundle.getKeys();
          String theKey;
          while ( enumer.hasMoreElements() ) {
            theKey = enumer.nextElement();
            properties.setProperty( theKey, bundle.getString( theKey ) );
          }
        }
      } catch ( Exception ex ) {
        // don't throw an exception, but log it.
        ex.printStackTrace();
      }

      FontData fontData = getFixedFont();
      properties.setProperty( STRING_FONT_FIXED_NAME, fontData.getName() );
      properties.setProperty( STRING_FONT_FIXED_SIZE, "" + fontData.getHeight() );
      properties.setProperty( STRING_FONT_FIXED_STYLE, "" + fontData.getStyle() );

      fontData = getDefaultFont();
      properties.setProperty( STRING_FONT_DEFAULT_NAME, fontData.getName() );
      properties.setProperty( STRING_FONT_DEFAULT_SIZE, "" + fontData.getHeight() );
      properties.setProperty( STRING_FONT_DEFAULT_STYLE, "" + fontData.getStyle() );

      fontData = getDefaultFont();
      properties.setProperty( STRING_FONT_GRAPH_NAME, fontData.getName() );
      properties.setProperty( STRING_FONT_GRAPH_SIZE, "" + fontData.getHeight() );
      properties.setProperty( STRING_FONT_GRAPH_STYLE, "" + fontData.getStyle() );

      fontData = getDefaultFont();
      properties.setProperty( STRING_FONT_GRID_NAME, fontData.getName() );
      properties.setProperty( STRING_FONT_GRID_SIZE, "" + fontData.getHeight() );
      properties.setProperty( STRING_FONT_GRID_STYLE, "" + fontData.getStyle() );

      fontData = getDefaultFont();
      properties.setProperty( STRING_FONT_NOTE_NAME, fontData.getName() );
      properties.setProperty( STRING_FONT_NOTE_SIZE, "" + fontData.getHeight() );
      properties.setProperty( STRING_FONT_NOTE_STYLE, "" + fontData.getStyle() );

      RGB color = getBackgroundRGB();
      properties.setProperty( STRING_BACKGROUND_COLOR_R, "" + color.red );
      properties.setProperty( STRING_BACKGROUND_COLOR_G, "" + color.green );
      properties.setProperty( STRING_BACKGROUND_COLOR_B, "" + color.blue );

      color = getGraphColorRGB();
      properties.setProperty( STRING_GRAPH_COLOR_R, "" + color.red );
      properties.setProperty( STRING_GRAPH_COLOR_G, "" + color.green );
      properties.setProperty( STRING_GRAPH_COLOR_B, "" + color.blue );

      properties.setProperty( STRING_ICON_SIZE, "" + getIconSize() );
      properties.setProperty( STRING_LINE_WIDTH, "" + getLineWidth() );
      properties.setProperty( STRING_MAX_UNDO, "" + getMaxUndo() );
    }
  }



  public void saveProps() {
    super.saveProps();
  }


  public String getFilename() {
    if ( this.filename == null ) {
      String s = System.getProperty( "org.apache.hop.ui.PropsUIFile" );
      if ( s != null ) {
        return s;
      } else {
        return super.getFilename();
      }
    } else {
      return this.filename;
    }
  }

  public void setFixedFont( FontData fd ) {
    properties.setProperty( STRING_FONT_FIXED_NAME, fd.getName() );
    properties.setProperty( STRING_FONT_FIXED_SIZE, "" + fd.getHeight() );
    properties.setProperty( STRING_FONT_FIXED_STYLE, "" + fd.getStyle() );
  }

  public FontData getFixedFont() {
    FontData def = getDefaultFontData();

    String name = properties.getProperty( STRING_FONT_FIXED_NAME );
    int size = Const.toInt( properties.getProperty( STRING_FONT_FIXED_SIZE ), def.getHeight() );
    int style = Const.toInt( properties.getProperty( STRING_FONT_FIXED_STYLE ), def.getStyle() );

    return new FontData( name, size, style );
  }

  public void setDefaultFont( FontData fd ) {
    if ( fd != null ) {
      properties.setProperty( STRING_FONT_DEFAULT_NAME, fd.getName() );
      properties.setProperty( STRING_FONT_DEFAULT_SIZE, "" + fd.getHeight() );
      properties.setProperty( STRING_FONT_DEFAULT_STYLE, "" + fd.getStyle() );
    }
  }

  public FontData getDefaultFont() {
    FontData def = getDefaultFontData();

    if ( isOSLookShown() ) {
      return def;
    }

    String name = properties.getProperty( STRING_FONT_DEFAULT_NAME, def.getName() );
    int size = Const.toInt( properties.getProperty( STRING_FONT_DEFAULT_SIZE ), def.getHeight() );
    int style = Const.toInt( properties.getProperty( STRING_FONT_DEFAULT_STYLE ), def.getStyle() );

    return new FontData( name, size, style );
  }

  public void setGraphFont( FontData fd ) {
    properties.setProperty( STRING_FONT_GRAPH_NAME, fd.getName() );
    properties.setProperty( STRING_FONT_GRAPH_SIZE, "" + fd.getHeight() );
    properties.setProperty( STRING_FONT_GRAPH_STYLE, "" + fd.getStyle() );
  }

  public FontData getGraphFont() {
    FontData def = getDefaultFontData();

    String name = properties.getProperty( STRING_FONT_GRAPH_NAME, def.getName() );

    int size = Const.toInt( properties.getProperty( STRING_FONT_GRAPH_SIZE ), def.getHeight() );

    // Correct the size with the native zoom factor...
    //
    int correctedSize = (int) Math.round( size / PropsUi.getNativeZoomFactor() );

    int style = Const.toInt( properties.getProperty( STRING_FONT_GRAPH_STYLE ), def.getStyle() );

    return new FontData( name, correctedSize, style );
  }

  public void setGridFont( FontData fd ) {
    properties.setProperty( STRING_FONT_GRID_NAME, fd.getName() );
    properties.setProperty( STRING_FONT_GRID_SIZE, "" + fd.getHeight() );
    properties.setProperty( STRING_FONT_GRID_STYLE, "" + fd.getStyle() );
  }

  public FontData getGridFont() {
    FontData def = getDefaultFontData();

    String name = properties.getProperty( STRING_FONT_GRID_NAME, def.getName() );
    String ssize = properties.getProperty( STRING_FONT_GRID_SIZE );
    String sstyle = properties.getProperty( STRING_FONT_GRID_STYLE );

    int size = Const.toInt( ssize, def.getHeight() );
    int style = Const.toInt( sstyle, def.getStyle() );

    return new FontData( name, size, style );
  }

  public void setNoteFont( FontData fd ) {
    properties.setProperty( STRING_FONT_NOTE_NAME, fd.getName() );
    properties.setProperty( STRING_FONT_NOTE_SIZE, "" + fd.getHeight() );
    properties.setProperty( STRING_FONT_NOTE_STYLE, "" + fd.getStyle() );
  }

  public FontData getNoteFont() {
    FontData def = getDefaultFontData();

    String name = properties.getProperty( STRING_FONT_NOTE_NAME, def.getName() );
    String ssize = properties.getProperty( STRING_FONT_NOTE_SIZE );
    String sstyle = properties.getProperty( STRING_FONT_NOTE_STYLE );

    int size = Const.toInt( ssize, def.getHeight() );
    int style = Const.toInt( sstyle, def.getStyle() );

    return new FontData( name, size, style );
  }

  public void setBackgroundRGB( RGB c ) {
    properties.setProperty( STRING_BACKGROUND_COLOR_R, c != null ? "" + c.red : "" );
    properties.setProperty( STRING_BACKGROUND_COLOR_G, c != null ? "" + c.green : "" );
    properties.setProperty( STRING_BACKGROUND_COLOR_B, c != null ? "" + c.blue : "" );
  }

  public RGB getBackgroundRGB() {
    int r = Const.toInt( properties.getProperty( STRING_BACKGROUND_COLOR_R ), ConstUi.COLOR_BACKGROUND_RED ); // Defaut:
    int g = Const.toInt( properties.getProperty( STRING_BACKGROUND_COLOR_G ), ConstUi.COLOR_BACKGROUND_GREEN );
    int b = Const.toInt( properties.getProperty( STRING_BACKGROUND_COLOR_B ), ConstUi.COLOR_BACKGROUND_BLUE );

    return new RGB( r, g, b );
  }

  public void setGraphColorRGB( RGB c ) {
    properties.setProperty( STRING_GRAPH_COLOR_R, "" + c.red );
    properties.setProperty( STRING_GRAPH_COLOR_G, "" + c.green );
    properties.setProperty( STRING_GRAPH_COLOR_B, "" + c.blue );
  }

  public RGB getGraphColorRGB() {
    int r = Const.toInt( properties.getProperty( STRING_GRAPH_COLOR_R ), ConstUi.COLOR_GRAPH_RED ); // default White
    int g = Const.toInt( properties.getProperty( STRING_GRAPH_COLOR_G ), ConstUi.COLOR_GRAPH_GREEN );
    int b = Const.toInt( properties.getProperty( STRING_GRAPH_COLOR_B ), ConstUi.COLOR_GRAPH_BLUE );

    return new RGB( r, g, b );
  }

  public void setTabColorRGB( RGB c ) {
    properties.setProperty( STRING_TAB_COLOR_R, "" + c.red );
    properties.setProperty( STRING_TAB_COLOR_G, "" + c.green );
    properties.setProperty( STRING_TAB_COLOR_B, "" + c.blue );
  }

  public RGB getTabColorRGB() {
    int r = Const.toInt( properties.getProperty( STRING_TAB_COLOR_R ), ConstUi.COLOR_TAB_RED ); // default White
    int g = Const.toInt( properties.getProperty( STRING_TAB_COLOR_G ), ConstUi.COLOR_TAB_GREEN );
    int b = Const.toInt( properties.getProperty( STRING_TAB_COLOR_B ), ConstUi.COLOR_TAB_BLUE );

    return new RGB( r, g, b );
  }

  public void setIconSize( int size ) {
    properties.setProperty( STRING_ICON_SIZE, "" + size );
  }

  public int getIconSize() {
    return Const.toInt( properties.getProperty( STRING_ICON_SIZE ), ConstUi.ICON_SIZE );
  }

  public void setZoomFactor( double factor ) {
    properties.setProperty( STRING_ZOOM_FACTOR, Double.toString( factor ) );
  }

  public double getZoomFactor() {
    String zoomFactorString = properties.getProperty( STRING_ZOOM_FACTOR );
    if ( StringUtils.isNotEmpty( zoomFactorString ) ) {
      return Const.toDouble( zoomFactorString, nativeZoomFactor );
    } else {
      return nativeZoomFactor;
    }
  }

  /**
   * Get the margin compensated for the zoom factor
   *
   * @return
   */
  public int getMargin() {
    return (int) Math.round( getZoomFactor() * Const.MARGIN );
  }

  public void setLineWidth( int width ) {
    properties.setProperty( STRING_LINE_WIDTH, "" + width );
  }

  public int getLineWidth() {
    return Const.toInt( properties.getProperty( STRING_LINE_WIDTH ), ConstUi.LINE_WIDTH );
  }

  public void setLastPipeline( String pipeline ) {
    properties.setProperty( STRING_LAST_PREVIEW_PIPELINE, pipeline );
  }

  public String getLastPipeline() {
    return properties.getProperty( STRING_LAST_PREVIEW_PIPELINE, "" );
  }

  public void setLastPreview( String[] lastpreview, int[] transformsize ) {
    properties.setProperty( STRING_LAST_PREVIEW_TRANSFORM, "" + lastpreview.length );

    for ( int i = 0; i < lastpreview.length; i++ ) {
      properties.setProperty( STRING_LAST_PREVIEW_TRANSFORM + ( i + 1 ), lastpreview[ i ] );
      properties.setProperty( STRING_LAST_PREVIEW_SIZE + ( i + 1 ), "" + transformsize[ i ] );
    }
  }

  public String[] getLastPreview() {
    String snr = properties.getProperty( STRING_LAST_PREVIEW_TRANSFORM );
    int nr = Const.toInt( snr, 0 );
    String[] lp = new String[ nr ];
    for ( int i = 0; i < nr; i++ ) {
      lp[ i ] = properties.getProperty( STRING_LAST_PREVIEW_TRANSFORM + ( i + 1 ), "" );
    }
    return lp;
  }

  public int[] getLastPreviewSize() {
    String snr = properties.getProperty( STRING_LAST_PREVIEW_TRANSFORM );
    int nr = Const.toInt( snr, 0 );
    int[] si = new int[ nr ];
    for ( int i = 0; i < nr; i++ ) {
      si[ i ] = Const.toInt( properties.getProperty( STRING_LAST_PREVIEW_SIZE + ( i + 1 ), "" ), 0 );
    }
    return si;
  }

  public FontData getDefaultFontData() {
    return display.getSystemFont().getFontData()[ 0 ];
  }

  public void setMaxUndo( int max ) {
    properties.setProperty( STRING_MAX_UNDO, "" + max );
  }

  public int getMaxUndo() {
    return Const.toInt( properties.getProperty( STRING_MAX_UNDO ), Const.MAX_UNDO );
  }

  public void setMiddlePct( int pct ) {
    properties.setProperty( STRING_MIDDLE_PCT, "" + pct );
  }

  public int getMiddlePct() {
    return Const.toInt( properties.getProperty( STRING_MIDDLE_PCT ), Const.MIDDLE_PCT );
  }

  public void setScreen( WindowProperty windowProperty ) {
    HopGui.getInstance().auditDelegate.storeState( "shells", windowProperty.getName(), windowProperty.getStateProperties() );
  }

  public WindowProperty getScreen( String windowName ) {
    if ( windowName == null ) {
      return null;
    }
    AuditState auditState = HopGui.getInstance().auditDelegate.retrieveState("shells", windowName);
    if (auditState==null) {
      return null;
    }
    return new WindowProperty(windowName, auditState.getStateMap());
  }


  public void setOpenLastFile( boolean open ) {
    properties.setProperty( STRING_OPEN_LAST_FILE, open ? YES : NO );
  }

  public boolean openLastFile() {
    String open = properties.getProperty( STRING_OPEN_LAST_FILE );
    return !NO.equalsIgnoreCase( open );
  }

  public void setAutoSave( boolean autosave ) {
    properties.setProperty( STRING_AUTO_SAVE, autosave ? YES : NO );
  }

  public boolean getAutoSave() {
    String autosave = properties.getProperty( STRING_AUTO_SAVE );
    return YES.equalsIgnoreCase( autosave ); // Default = OFF
  }

  public void setSaveConfirmation( boolean saveconf ) {
    properties.setProperty( STRING_SAVE_CONF, saveconf ? YES : NO );
  }

  public boolean getSaveConfirmation() {
    String saveconf = properties.getProperty( STRING_SAVE_CONF );
    return YES.equalsIgnoreCase( saveconf ); // Default = OFF
  }

  public void setAutoSplit( boolean autosplit ) {
    properties.setProperty( STRING_AUTO_SPLIT, autosplit ? YES : NO );
  }

  public boolean getAutoSplit() {
    String autosplit = properties.getProperty( STRING_AUTO_SPLIT );
    return YES.equalsIgnoreCase( autosplit ); // Default = OFF
  }

  public void setAutoCollapseCoreObjectsTree( boolean autoCollapse ) {
    properties.setProperty( STRING_AUTO_COLLAPSE_CORE_TREE, autoCollapse ? YES : NO );
  }

  public boolean getAutoCollapseCoreObjectsTree() {
    String autoCollapse = properties.getProperty( STRING_AUTO_COLLAPSE_CORE_TREE );
    return YES.equalsIgnoreCase( autoCollapse ); // Default = OFF
  }

  public void setExitWarningShown( boolean show ) {
    properties.setProperty( STRING_SHOW_EXIT_WARNING, show ? YES : NO );
  }

  public boolean isShowCanvasGridEnabled() {
    String showCanvas = properties.getProperty( STRING_SHOW_CANVAS_GRID, NO );
    return YES.equalsIgnoreCase( showCanvas ); // Default: don't show canvas grid
  }

  public void setShowCanvasGridEnabled( boolean anti ) {
    properties.setProperty( STRING_SHOW_CANVAS_GRID, anti ? YES : NO );
  }

  public boolean showExitWarning() {
    String show = properties.getProperty( STRING_SHOW_EXIT_WARNING, YES );
    return YES.equalsIgnoreCase( show ); // Default: show repositories dialog at startup
  }

  public boolean isOSLookShown() {
    String show = properties.getProperty( STRING_SHOW_OS_LOOK, NO );
    return YES.equalsIgnoreCase( show ); // Default: don't show gray dialog boxes, show Hop look.
  }

  public void setOSLookShown( boolean show ) {
    properties.setProperty( STRING_SHOW_OS_LOOK, show ? YES : NO );
  }

  public static void setGCFont( GC gc, Device device, FontData fontData ) {
    if ( Const.getOS().startsWith( "Windows" ) ) {
      Font font = new Font( device, fontData );
      gc.setFont( font );
      font.dispose();
    } else {
      gc.setFont( device.getSystemFont() );
    }
  }

  public void setLook( Control widget ) {
    if (widget instanceof Combo ) {
      return; // Just keep the default
    }

    setLook( widget, WIDGET_STYLE_DEFAULT );

    if ( widget instanceof Composite ) {
      for ( Control child : ( (Composite) widget ).getChildren() ) {
        setLook( child );
      }
    }
  }

  public void setLook( final Control control, int style ) {
    if ( this.isOSLookShown() && style != WIDGET_STYLE_FIXED ) {
      return;
    }

    final GuiResource gui = GuiResource.getInstance();
    Font font = null;
    Color background = null;

    switch ( style ) {
      case WIDGET_STYLE_DEFAULT:
        background = gui.getColorBackground();
        if ( control instanceof Group && OS.indexOf( "mac" ) > -1 ) {
          control.addPaintListener( new PaintListener() {
            @Override
            public void paintControl( PaintEvent paintEvent ) {
              paintEvent.gc.setBackground( gui.getColorBackground() );
              paintEvent.gc.fillRectangle( 2, 0, control.getBounds().width - 8, control.getBounds().height - 20 );
            }
          } );
        }
        font = null; // GuiResource.getInstance().getFontDefault();
        break;
      case WIDGET_STYLE_FIXED:
        if ( !this.isOSLookShown() ) {
          background = gui.getColorBackground();
        }
        font = gui.getFontFixed();
        break;
      case WIDGET_STYLE_TABLE:
        background = gui.getColorBackground();
        font = null; // gui.getFontGrid();
        break;
      case WIDGET_STYLE_NOTEPAD:
        background = gui.getColorBackground();
        font = gui.getFontNote();
        break;
      case WIDGET_STYLE_GRAPH:
        background = gui.getColorBackground();
        font = gui.getFontGraph();
        break;
      case WIDGET_STYLE_TOOLBAR:
        background = GuiResource.getInstance().getColorDemoGray();
        break;
      case WIDGET_STYLE_TAB:
        background = GuiResource.getInstance().getColorWhite();
        CTabFolder tabFolder = (CTabFolder) control;
        tabFolder.setSimple( false );
        tabFolder.setBorderVisible( true );
        // need to make a copy of the tab selection background color to get around PDI-13940
        Color c = GuiResource.getInstance().getColorTab();
        Color tabColor = new Color( c.getDevice(), c.getRed(), c.getGreen(), c.getBlue() );
        tabFolder.setSelectionBackground( tabColor );
        break;
      default:
        background = gui.getColorBackground();
        font = null; // gui.getFontDefault();
        break;
    }

    if ( font != null && !font.isDisposed() ) {
      control.setFont( font );
    }

    if ( background != null && !background.isDisposed() ) {
      if ( control instanceof Button ) {
        Button b = (Button) control;
        if ( ( b.getStyle() & SWT.PUSH ) != 0 ) {
          return;
        }
      }
      control.setBackground( background );
    }
  }

  public static void setTableItemLook( TableItem item, Display disp ) {
    if ( !Const.getOS().startsWith( "Windows" ) ) {
      return;
    }

    Color background = GuiResource.getInstance().getColorBackground();
    if ( background != null ) {
      item.setBackground( background );
    }
  }

  /**
   * @return Returns the display.
   */
  public static Display getDisplay() {
    return display;
  }

  /**
   * @param d The display to set.
   */
  public static void setDisplay( Display d ) {
    display = d;
  }

  public void setDefaultPreviewSize( int size ) {
    properties.setProperty( STRING_DEFAULT_PREVIEW_SIZE, "" + size );
  }

  public int getDefaultPreviewSize() {
    return Const.toInt( properties.getProperty( STRING_DEFAULT_PREVIEW_SIZE ), 1000 );
  }

  public boolean showCopyOrDistributeWarning() {
    String show = properties.getProperty( STRING_SHOW_COPY_OR_DISTRIBUTE_WARNING, YES );
    return YES.equalsIgnoreCase( show );
  }

  public void setShowCopyOrDistributeWarning( boolean show ) {
    properties.setProperty( STRING_SHOW_COPY_OR_DISTRIBUTE_WARNING, show ? YES : NO );
  }


  public int getWorkflowsDialogStyle() {
    String prop = properties.getProperty( "WorkflowDialogStyle" );
    return parseStyle( prop );
  }

  public int getDialogStyle( String styleProperty ) {
    String prop = properties.getProperty( styleProperty );
    if ( Utils.isEmpty( prop ) ) {
      return SWT.NONE;
    }

    return parseStyle( prop );
  }

  private int parseStyle( String sStyle ) {
    int style = SWT.DIALOG_TRIM;
    String[] styles = sStyle.split( "," );
    for ( String style1 : styles ) {
      if ( "APPLICATION_MODAL".equals( style1 ) ) {
        style |= SWT.APPLICATION_MODAL | SWT.SHEET;
      } else if ( "RESIZE".equals( style1 ) ) {
        style |= SWT.RESIZE;
      } else if ( "MIN".equals( style1 ) ) {
        style |= SWT.MIN;
      } else if ( "MAX".equals( style1 ) ) {
        style |= SWT.MAX;
      }
    }

    return style;
  }

  public void setDialogSize( Shell shell, String styleProperty ) {
    String prop = properties.getProperty( styleProperty );
    if ( Utils.isEmpty( prop ) ) {
      return;
    }

    String[] xy = prop.split( "," );
    if ( xy.length != 2 ) {
      return;
    }

    shell.setSize( Integer.parseInt( xy[ 0 ] ), Integer.parseInt( xy[ 1 ] ) );
  }

  public boolean showToolTips() {
    return YES.equalsIgnoreCase( properties.getProperty( SHOW_TOOL_TIPS, YES ) );
  }

  public void setShowToolTips( boolean show ) {
    properties.setProperty( SHOW_TOOL_TIPS, show ? YES : NO );
  }

  public boolean isShowingHelpToolTips() {
    return YES.equalsIgnoreCase( properties.getProperty( SHOW_HELP_TOOL_TIPS, YES ) );
  }

  public void setShowingHelpToolTips( boolean show ) {
    properties.setProperty( SHOW_HELP_TOOL_TIPS, show ? YES : NO );
  }

  public int getCanvasGridSize() {
    return Const.toInt( properties.getProperty( CANVAS_GRID_SIZE, "16" ), 16 );
  }

  public void setCanvasGridSize( int gridSize ) {
    properties.setProperty( CANVAS_GRID_SIZE, Integer.toString( gridSize ) );
  }

  /**
   * Gets the supported version of the requested software.
   *
   * @param property the key for the software version
   * @return an integer that represents the supported version for the software.
   */
  public int getSupportedVersion( String property ) {
    return Integer.parseInt( properties.getProperty( property ) );
  }

  /**
   * Ask if the browsing environment checks are disabled.
   *
   * @return 'true' if disabled 'false' otherwise.
   */
  public boolean isBrowserEnvironmentCheckDisabled() {
    return "Y".equalsIgnoreCase( properties.getProperty( DISABLE_BROWSER_ENVIRONMENT_CHECK, "N" ) );
  }

  public boolean isLegacyPerspectiveMode() {
    return "Y".equalsIgnoreCase( properties.getProperty( LEGACY_PERSPECTIVE_MODE, "N" ) );
  }

  public static void setLocation( IGuiPosition guiElement, int x, int y ) {
    if ( x < 0 ) {
      x = 0;
    }
    if ( y < 0 ) {
      y = 0;
    }
    guiElement.setLocation( calculateGridPosition( new Point( x, y ) ) );
  }

  public static Point calculateGridPosition( Point p ) {
    int gridSize = PropsUi.getInstance().getCanvasGridSize();
    if ( gridSize > 1 ) {
      // Snap to grid...
      //
      return new Point( gridSize * Math.round( p.x / gridSize ), gridSize * Math.round( p.y / gridSize ) );
    } else {
      // Normal draw
      //
      return p;
    }
  }

  public boolean isIndicateSlowPipelineTransformsEnabled() {
    String indicate = properties.getProperty( STRING_INDICATE_SLOW_PIPELINE_TRANSFORMS, "Y" );
    return YES.equalsIgnoreCase( indicate );
  }

  public void setIndicateSlowPipelineTransformsEnabled( boolean indicate ) {
    properties.setProperty( STRING_INDICATE_SLOW_PIPELINE_TRANSFORMS, indicate ? YES : NO );
  }

  /**
   * Gets nativeZoomFactor
   *
   * @return value of nativeZoomFactor
   */
  public static double getNativeZoomFactor() {
    return nativeZoomFactor;
  }

  /**
   * @param nativeZoomFactor The nativeZoomFactor to set
   */
  public static void setNativeZoomFactor( double nativeZoomFactor ) {
    PropsUi.nativeZoomFactor = nativeZoomFactor;
  }


}
