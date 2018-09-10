/*== ValidatingJAXBContext.java ========================================
A wrapper for a JAXBContext with schema validation on unmarshaling. 
Version     : $Id: ValidatingJAXBContext.java 610 2016-02-29 16:12:36Z hartwig $
Application : JAXB Utilities
Description : A wrapper for a JAXBContext with schema validation on unmarshaling.
------------------------------------------------------------------------
Copyright  : 2012, Enter AG, Zurich, Switzerland
Created    : 29.05.2012, Hartwig Thomas
======================================================================*/
package ch.enterag.utils.jaxb;

import java.io.*;
import java.net.*;
import javax.xml.*;
import javax.xml.bind.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.validation.*;
import org.xml.sax.*;
import ch.enterag.utils.logging.*;

/*====================================================================*/
/** ValidatingJAXBContext wraps a JAXBContext adding schema validation. 
 * @author Hartwig
 */
public class ValidatingJAXBContext extends JAXBContext
{
  private static IndentLogger m_il = IndentLogger.getIndentLogger(ValidatingJAXBContext.class.getName());
  private JAXBContext m_jc = null;
  private URL m_urlSchema = null;
  
  /* instantiation with suppression of illegal reflective access warning.
   * We have to move to JAXB 2.4.0 as soon as it is available.
   */
  static JAXBContext instantiateJAXBContext(Class<?>... classesToBeBound)
    throws JAXBException
  {
    if (System.err != System.out)
    {
      System.err.close();
      System.setErr(System.out);
    }
    return JAXBContext.newInstance(classesToBeBound);
  }
  static JAXBContext instantiateJAXBContext(String sPackage)
    throws JAXBException
  {
    if (System.err != System.out)
    {
      System.err.close();
      System.setErr(System.out);
    }
    return JAXBContext.newInstance(sPackage);
  }
  
  /*==================================================================*/
  /** ContextValidationEventHandler is used for more explicit logging. 
   */
  private class ContextValidationEventHandler implements ValidationEventHandler
  {
    @Override
    public boolean handleEvent(ValidationEvent ve)
    {
      boolean bContinue = false;
      m_il.enter(ve);
      if ((ve.getSeverity() == ValidationEvent.ERROR) ||
          (ve.getSeverity() == ValidationEvent.FATAL_ERROR))
      {
        ValidationEventLocator vel = ve.getLocator();
        m_il.severe("XML validation for "+ String.valueOf(vel.getURL()) +
            " failed at line "+ String.valueOf(vel.getLineNumber()) +
            " and column "+ String.valueOf(vel.getColumnNumber()) +
            " with message "+ve.getMessage());
      }
      else
        bContinue = true;
      m_il.exit(String.valueOf(bContinue));
      return bContinue;
    } /* handleEvent */

  } /* ContextValidationEventHandler */
  /*==================================================================*/
  
  /*------------------------------------------------------------------*/
  /** constructor
   * @param jc JAXBContext to be wrapped.
   * @param schema Schema to be used for validation or null. 
   */
  private ValidatingJAXBContext(JAXBContext jc, URL urlSchema)
  {
    m_jc = jc;
    m_urlSchema = urlSchema;
  } /* constructor ValidatingJAXBContext */
  
  /*------------------------------------------------------------------*/
  /** factory
   * @param urlSchema schema for validation
   * @param classesToBeBound classes for binding.
   * @return class that validates on unmarshalling.
   */
  public static ValidatingJAXBContext newInstance(URL urlSchema, Class<?>... classesToBeBound)
    throws JAXBException
  {
    JAXBContext jc = instantiateJAXBContext(classesToBeBound);
    ValidatingJAXBContext vjc = new ValidatingJAXBContext(jc,urlSchema);
    return vjc;
  } /* newInstance */

  /*------------------------------------------------------------------*/
  /** factory
   * @param urlSchema schema for validation
   * @param sPackage package containing classes for binding.
   * @return class that validates on unmarshalling.
   */
  public static ValidatingJAXBContext newInstance(URL urlSchema, String sPackage)
    throws JAXBException
  {
    JAXBContext jc = instantiateJAXBContext(sPackage);
    ValidatingJAXBContext vjc = new ValidatingJAXBContext(jc,urlSchema);
    return vjc;
  } /* newInstance */

  /*------------------------------------------------------------------*/
  /** create a validating unmarshaller.
   * @return validating unmarshaller.
   */
  @Override
  public Unmarshaller createUnmarshaller() 
    throws JAXBException
  {
    m_il.enter();
    Unmarshaller u = m_jc.createUnmarshaller();
    if (m_urlSchema != null)
    {
      m_il.event("Creating validating Unmarshaller ...");
      SchemaFactory sf = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
      try
      {
        InputStream is = m_urlSchema.openStream();
        Source source = new StreamSource(is);
        Schema schema = sf.newSchema(source);
        is.close();
        u.setSchema(schema);
      }
      catch (SAXException se) { throw new RuntimeException("Unable to create Schema "+String.valueOf(m_urlSchema)+"!", se); }
      catch (IOException ie) { throw new RuntimeException("Unable to read Schema "+String.valueOf(m_urlSchema)+"!", ie); }
    }
    u.setEventHandler(new ContextValidationEventHandler());
    m_il.exit(u);
    return u;
  } /* createUnmarshaller */

  /*------------------------------------------------------------------*/
  /** create a validating and formatting marshaller.
   * @return formatting marshaller.
   */
  @Override
  public Marshaller createMarshaller() 
    throws JAXBException
  {
    m_il.enter();
    Marshaller m = m_jc.createMarshaller();
    if (m_urlSchema != null)
    {
      m_il.event("Creating validating Unmarshaller ...");
      SchemaFactory sf = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
      try
      {
        InputStream is = m_urlSchema.openStream();
        Source source = new StreamSource(is);
        Schema schema = sf.newSchema(source);
        is.close();
        m.setSchema(schema);
      }
      catch (SAXException se) { throw new RuntimeException("Unable to create Schema "+String.valueOf(m_urlSchema)+"!", se); }
      catch (IOException ie) { throw new RuntimeException("Unable to read Schema "+String.valueOf(m_urlSchema)+"!", ie); }
    }
    m.setEventHandler(new ContextValidationEventHandler());
    m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
    m_il.exit(m);
    return m;
  } /* createMarshaller */

  /*------------------------------------------------------------------*/
  /** create a validator.
   * @return null, as the Validator class is deprecated anyway.
   */
  @SuppressWarnings("deprecation")
  @Override
  public javax.xml.bind.Validator createValidator() throws JAXBException
  {
    throw new RuntimeException("createValidator is deprecated!");
  } /* createValidator */

} /* ValidatingJAXBContext */
