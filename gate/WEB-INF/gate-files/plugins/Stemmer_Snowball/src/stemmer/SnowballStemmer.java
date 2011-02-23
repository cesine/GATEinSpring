/**
 * A CREOLE wrapper for the Snowball stemmer Java implementation.
 * See <a href="http://snowball.tartarus.org/index.php">http://snowball.tartarus.org/index.php</a>.
 */
package stemmer;

import gate.*;
import gate.creole.*;
import gate.util.*;
import java.util.Iterator;

/**
 * A simple CREOLE wrapper for the Snowball stemmer.
 */
public class SnowballStemmer extends AbstractLanguageAnalyser implements
                                                             ProcessingResource {

  public Resource init() throws ResourceInstantiationException{
    fireStatusChanged("Creating a stemmer");
    fireProgressChanged(0);
    try {
      Class stemClass = Class.forName(String.valueOf(String
              .valueOf((new StringBuffer("org.tartarus.snowball.ext.")).append(
                      language).append("Stemmer"))));
      stemmer = (org.tartarus.snowball.SnowballStemmer)stemClass.newInstance();
    } catch(ClassNotFoundException e) {
      throw new ResourceInstantiationException("Unsupported language:  " + 
              language);
    }
    catch(InstantiationException e) {
      throw new ResourceInstantiationException("Exception while instantiating stemmer", 
              e);
    }
    catch(IllegalAccessException e) {
      throw new ResourceInstantiationException("Exception while instantiating stemmer", 
              e);
    } finally{
      fireProgressChanged(100);
      fireProcessFinished();
    }
    return this;
  }

  public void execute() throws ExecutionException {
    super.interrupted = false;
    if(super.document == null) throw new GateRuntimeException(
            "No document to process!");
    fireProgressChanged(0);
    fireStatusChanged("Stemming " + document.getName() + "...");
    if(annotationSetName != null && annotationSetName.equals(""))
      annotationSetName = null;
    AnnotationSet inputAS =
        (annotationSetName == null || annotationSetName.trim().length() == 0) ?
        document.getAnnotations() :
        document.getAnnotations(annotationSetName);  
    AnnotationSet tokensAS = inputAS.get(annotationType);
    if(tokensAS == null){
      throw new GateRuntimeException(
              "No annotations to process!\n" +
              "Please run Tokeniser first, if using default Stemmer features!");
    }
    Iterator<Annotation> iter = tokensAS.iterator();
    int allTokens = tokensAS.size();
    int processedTokens = 0;
    int lastReport = 0;
    while(iter.hasNext()){
      if(isInterrupted()){
        throw new ExecutionInterruptedException(String
                .valueOf(String.valueOf((new StringBuffer(
                        "The execution of the \"")).append(getName()).append(
                        "\" stemmer has been abruptly interrupted!"))));
      }
      Annotation token = (Annotation)iter.next();
      FeatureMap allFeatures = token.getFeatures();
      String tokenString = (String)allFeatures.get(annotationFeature);
      stemmer.setCurrent(tokenString.toLowerCase());
      stemmer.stem();
      allFeatures.put("stem", stemmer.getCurrent());
      if(++processedTokens - lastReport > 100) {
        lastReport = processedTokens;
        fireProgressChanged((processedTokens * 100) / allTokens);
      }
    }
    fireProcessFinished();
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getLanguage() {
    return language;
  }

  public void setAnnotationSetName(String annotationSetName) {
    this.annotationSetName = annotationSetName;
  }

  public String getAnnotationSetName() {
    return annotationSetName;
  }

  public void setAnnotationType(String annotationType) {
    this.annotationType = annotationType;
  }

  public String getAnnotationType() {
    return annotationType;
  }

  public void setAnnotationFeature(String annotationFeature) {
    this.annotationFeature = annotationFeature;
  }

  public String getAnnotationFeature() {
    return annotationFeature;
  }

  public static final String SNOW_STAM_DOCUMENT_PARAMETER_NAME = "document";

  public static final String SNOW_STAM_ANNOT_SET_PARAMETER_NAME = "annotationSetName";

  public static final String SNOW_STAM_ANNOT_TYPE_PARAMETER_NAME = "annotationType";

  public static final String SNOW_STAM_ANNOT_FEATURE_PARAMETER_NAME = "annotationFeature";

  public static final String SNOW_STAM_LANGUAGE_PARAMETER_NAME = "language";

  /**
   * The actual stemmer implementation.
   */
  private org.tartarus.snowball.SnowballStemmer stemmer;

  private String language;

  private String annotationSetName;

  private String annotationType;

  private String annotationFeature;
}
