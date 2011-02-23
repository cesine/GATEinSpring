package gate.groovy

import gate.*
import gate.Factory.DuplicationContext;
import gate.creole.*
import gate.creole.metadata.*
import gate.util.*

import java.beans.PropertyChangeSupport
import java.beans.PropertyChangeListener

@CreoleResource(name = "Scriptable Controller", comment =
    "A controller whose execution strategy is controlled by a Groovy script",
    helpURL = "http://gate.ac.uk/userguide/sec:api:groovy:controller",
    icon = "/gate/groovy/scriptable-controller")
public class ScriptableController extends SerialController
                          implements CorpusController, LanguageAnalyser {

  /**
   * The corpus over which we are running.
   */
  Corpus corpus

  /**
   * The document over which we are running, when in LanguageAnalyser mode.
   */
  Document document

  /**
   * Text of the Groovy script that controls execution.
   */
  String controlScript = """\
eachDocument {
  allPRs()
}"""

  /**
   * Explicit setter to fire PropertyChangeEvent.
   */
  public void setControlScript(String newScript) {
    def oldScript = controlScript
    controlScript = newScript
    // clear the cached compiled script if the script text has been changed
    if(controlScript != oldScript) {
      script = null
    }
    pcs.firePropertyChange("controlScript", oldScript, newScript)
  }

  private Script script

  /**
   * Map from PR name to List of the indexes into the prList at which PRs with
   * that name are found.  Only valid during a call to executeImpl.
   */
  private transient Map prsByName

  /**
   * Check for interruption, throwing an exception if required.
   */
  protected void checkInterrupted() throws ExecutionException {
    if(isInterrupted()) {
      throw new ExecutionInterruptedException("The execution of the " +
          name + " application has been abruptly interrupted")
    }
  }

  /**
   * Parse the control script and set up its metaclass.
   */
  protected void parseScript() throws ExecutionException {
    try {
      script = new GroovyShell(Gate.getClassLoader()).parse(
          controlScript + "\n\n\n" + GroovySupport.STANDARD_IMPORTS)
      GroovySystem.metaClassRegistry.removeMetaClass(script.getClass())
      def mc = script.getClass().metaClass

      // this closure runs a single PR from the PR list, with an optional set
      // of parameter value overrides.  The original values for these
      // parameters are restored after the PR has been run.
      def runPr =  { params, index ->
        def pr = prList[index]
        checkInterrupted()
        FeatureMap savedParams = [:].toFeatureMap()
        try {
          // save original parameter values
          params.each { k, v ->
            savedParams[k] = pr.getParameterValue(k)
            pr.setParameterValue(k, v)
          }
          // inject the corpus and current document (if any)
          if(pr instanceof LanguageAnalyser) {
            if(corpus) {
              pr.corpus = corpus
            }
            // check if the script knows about a current document
            if(script.binding.variables.doc) {
              pr.document = script.binding.variables.doc
            }
          }
          // execute the PR using SerialController.runComponent
          runComponent(index)
        }
        finally {
          if(pr instanceof LanguageAnalyser) {
            pr.corpus = null
            pr.document = null
          }
          pr.setParameterValues(savedParams)
        }
      }

      mc.invokeMethod = { String name, args ->
        checkInterrupted()
        if(prsByName.containsKey(name) && (!args || args[0] instanceof Map)) {
          def params = args ? args[0] : [:]
          prsByName[name].each(runPr.curry(params))
        }
        else if("eachDocument".equals(name) && args && args[0] instanceof Closure) {
          def savedCurrentDoc = script.binding.variables.doc
          try {
            if(document) {
              // we are a language analyser, so just process the single document
              script.binding.setVariable('doc', document)
              args[0].call(document)
            }
            else {
              benchmarkFeatures.put(Benchmark.CORPUS_NAME_FEATURE, corpus.name)

              // process each document in the corpus - corpus.each does the
              // right thing with corpora stored in datastores
              corpus.each {
                String savedBenchmarkId = getBenchmarkId()
                try {
                  // include the document name in the benchmark ID for sub-events
                  setBenchmarkId(Benchmark.createBenchmarkId("doc_${it.name}",
                          getBenchmarkId()))
                  benchmarkFeatures.put(Benchmark.DOCUMENT_NAME_FEATURE, it.name)
                  checkInterrupted()
                  script.binding.setVariable('doc', it)
                  args[0].call(it)
                }
                finally {
                  setBenchmarkId(savedBenchmarkId)
                  benchmarkFeatures.remove(Benchmark.DOCUMENT_NAME_FEATURE)
                }
              }
            }
          }
          finally {
            script.binding.setVariable('doc', savedCurrentDoc)
            benchmarkFeatures.remove(Benchmark.CORPUS_NAME_FEATURE)
          }
        }
        else if("allPRs".equals(name) && !args) {
          // special case - allPrs() runs all the PRs in order
          (0 ..< prList.size()).each(runPr.curry([:]))
        }
        else {
          MetaMethod mm = mc.getMetaMethod(name, args)
          if(mm) {
            return mm.invoke(delegate, args)
          }
          else {
            throw new MissingMethodException(name, getClass(), args)
          }
        }
      }

      script.metaClass = mc
    }
    catch(Exception e) {
      throw new ExecutionException("Error parsing control script", e)
    }
  }

  protected void executeImpl() throws ExecutionException {
    interrupted = false
    if(script == null) {
      parseScript()
    }
    // build the prsByName map
    prsByName = (0 ..< prList.size()).groupBy { prList[it].name }

    if(log.isDebugEnabled()) {
      prof.initRun("Execute controller [" + getName() + "]");
    }

    // Set initial variable values
    script.binding.setVariable('prs', prList)
    script.binding.setVariable('corpus', corpus)
    if(document) {
      script.binding.setVariable('doc', document)
    }
    try {
      script.run()
    }
    finally {
      prsByName = null
    }

    if(log.isDebugEnabled()) {
      prof.checkPoint("Execute controller [" + getName() + "] finished");
    }
  }

  /**
   * Always return an empty list for "offending" PRs - even if a parameter is
   * not set before execution, it might be set dynamically by the script at
   * runtime.
   *
   * Yes, this is another typo in the superclass, the method really is called
   * getOffendingP(r)ocessingResources.
   */
  public List getOffendingPocessingResources() {
    return []
  }

  /**
   * Copy the control script text when duplicating a ScriptableController.
   */
  public Resource duplicate(DuplicationContext ctx)
          throws ResourceInstantiationException {
    ScriptableController dup = (ScriptableController)super.duplicate(ctx);
    dup.controlScript = this.controlScript
    return dup
  }
  /**
   * Property change support for the controlScript bound property.
   */
  private PropertyChangeSupport pcs = new PropertyChangeSupport(this)

  public void addPropertyChangeListener(String propName, PropertyChangeListener l) {
    pcs.addPropertyChangeListener(propName, l)
  }

  public void removePropertyChangeListener(String propName, PropertyChangeListener l) {
    pcs.removePropertyChangeListener(propName, l)
  }
}
