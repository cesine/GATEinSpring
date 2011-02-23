package gate.groovy.gui;

import gate.Resource;
import gate.creole.AbstractVisualResource;
import gate.creole.metadata.*;
import gate.groovy.ScriptableController;
import gate.util.GateRuntimeException;
import groovy.ui.ConsoleTextEditor;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import java.awt.BorderLayout;

@CreoleResource(name = "Control Script", comment = "Editor for the Groovy " +
    "script controlling a scriptable controller", guiType = GuiType.LARGE,
    resourceDisplayed = "gate.groovy.ScriptableController")
public class ControllerScriptEditor extends AbstractVisualResource
                                    implements DocumentListener,
                                               PropertyChangeListener {

  protected ConsoleTextEditor editor;

  protected ScriptableController controller;

  public Resource init() {
    initGuiComponents();

    return this;
  }

  protected void initGuiComponents() {
    setLayout(new BorderLayout());
    editor = new ConsoleTextEditor();
    editor.getTextEditor().getDocument().addDocumentListener(this);
    add(editor, BorderLayout.CENTER);
  }

  public void setTarget(Object target) {
    if(controller != null && target != controller) {
      controller.removePropertyChangeListener("controlScript", this);
    }
    if(target == null) return;
    if(!(target instanceof ScriptableController)) {
      throw new GateRuntimeException(this.getClass().getName() +
                                     " can only be used to display " +
                                     ScriptableController.class.getName() +
                                     "\n" + target.getClass().getName() +
                                     " is not a " +
                                     ScriptableController.class.getName() + "!");
    }

    controller = (ScriptableController)target;
    // populate the editor from the current script value on the controller
    propertyChange(null);

    controller.addPropertyChangeListener("controlScript", this);
  }

  // PropertyChangeListener methods

  /**
   * Trap to ensure we don't get an infinite loop between the property change
   * events from the controller and the document events from the editor.
   */
  private volatile boolean changeEvents = true;

  public void propertyChange(PropertyChangeEvent e) {
    if(changeEvents && controller != null) {
      changeEvents = false;
      editor.getTextEditor().setText(controller.getControlScript());
      changeEvents = true;
    }
  }

  // DocumentListener methods

  public void insertUpdate(DocumentEvent e) {
    setControllerScript();
  }

  public void removeUpdate(DocumentEvent e) {
    setControllerScript();
  }

  public void changedUpdate(DocumentEvent e) {
    // do nothing, we don't care about changes to attributes
  }

  protected void setControllerScript() {
    if(changeEvents && controller != null) {
      changeEvents = false;
      controller.setControlScript(editor.getTextEditor().getText());
      changeEvents = true;
    }
  }
}
