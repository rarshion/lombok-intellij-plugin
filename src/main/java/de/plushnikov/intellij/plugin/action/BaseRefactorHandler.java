package de.plushnikov.intellij.plugin.action;

import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.codeInsight.generation.EncapsulatableClassMember;
import com.intellij.codeInsight.generation.OverrideImplementUtil;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.ide.util.MemberChooser;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.ui.LightweightHint;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * Date: 15.12.13 Time: 23:06
 */
public abstract class BaseRefactorHandler implements Runnable {
  protected final Project project;
  protected final Editor editor;
  protected final MemberChooser<ClassMember> chooser;

  public BaseRefactorHandler(DataContext dataContext, Project project) {
    this.project = project;
    editor = PlatformDataKeys.EDITOR.getData(dataContext);

    PsiFile psiFile = DataKeys.PSI_FILE.getData(dataContext);
    PsiClass psiClass = OverrideImplementUtil.getContextClass(project, editor, psiFile, false);

    List<EncapsulatableClassMember> classMembers = getEncapsulatableClassMembers(psiClass);
    chooser = new MemberChooser<ClassMember>(
        classMembers.toArray(new ClassMember[classMembers.size()]), true, true, project);
    chooser.setTitle(getChooserTitle());
    chooser.setCopyJavadocVisible(false);
  }

  public boolean processChooser() {
    chooser.show();

    List<ClassMember> selectedElements = chooser.getSelectedElements();

    if (selectedElements == null) {
      HintManager.getInstance().showErrorHint(editor, getNothingFoundMessage());
      return false;
    }
    if (selectedElements.isEmpty()) {
      HintManager.getInstance().showErrorHint(editor, getNothingAcceptedMessage());
      return false;
    }
    return true;
  }

  protected abstract String getChooserTitle();

  protected abstract String getNothingFoundMessage();

  protected abstract String getNothingAcceptedMessage();

  protected abstract List<EncapsulatableClassMember> getEncapsulatableClassMembers(PsiClass psiClass);

  @Override
  public void run() {
    if (!prepareEditorForWrite(editor)) {
      return;
    }
    if (!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)) {
      return;
    }

    process(chooser.getSelectedElements());
  }

  protected abstract void process(List<ClassMember> classMembers);

  // Backported from IntellijCE 12

  private boolean prepareEditorForWrite(@NotNull Editor editor) {
    if (!editor.isViewer()) {
      return true;
    }
    showReadOnlyViewWarning(editor);
    return false;
  }

  private void showReadOnlyViewWarning(Editor editor) {
    if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
      return;
    }

    JComponent component = HintUtil.createInformationLabel("This view is read-only");
    final LightweightHint hint = new LightweightHint(component);
    HintManagerImpl.getInstanceImpl().showEditorHint(hint, editor, HintManager.UNDER,
        HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING, 0, false);
  }

}
