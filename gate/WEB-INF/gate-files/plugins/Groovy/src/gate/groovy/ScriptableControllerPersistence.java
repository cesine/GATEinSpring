package gate.groovy;

import gate.groovy.ScriptableController;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.util.persistence.ControllerPersistence;
import gate.util.persistence.PersistenceManager;
import gate.Corpus;

public class ScriptableControllerPersistence extends ControllerPersistence {
  private static final long serialVersionUID = -3300898388318335063L;
  /**
   * Populates this Persistence with the data that needs to be stored from the
   * original source object.
   */
  public void extractDataFromSource(Object source)throws PersistenceException{
    if(! (source instanceof ScriptableController)){
      throw new UnsupportedOperationException(
                getClass().getName() + " can only be used for " +
                ScriptableController.class.getName() +
                " objects!\n" + source.getClass().getName() +
                " is not a " + ScriptableController.class.getName());
    }

    super.extractDataFromSource(source);

    ScriptableController sc = (ScriptableController)source;
    corpus = PersistenceManager.getPersistentRepresentation(sc.getCorpus());
    controlScript = PersistenceManager.getPersistentRepresentation(sc.getControlScript());
  }

  /**
   * Creates a new object from the data contained. This new object is supposed
   * to be a copy for the original object used as source for data extraction.
   */
  public Object createObject()throws PersistenceException,
                                     ResourceInstantiationException{
    ScriptableController sc = (ScriptableController)
                                  super.createObject();
    sc.setCorpus((Corpus)PersistenceManager.getTransientRepresentation(corpus));
    sc.setControlScript((String)PersistenceManager.getTransientRepresentation(controlScript));
    return sc;
  }
  protected Object corpus;
  protected Object controlScript;
}
