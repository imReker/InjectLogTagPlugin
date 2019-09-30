package jp.tomorrowkey.intellij.injectlogtag;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;

/**
 * @author tomorrowkey
 */
public class InjectLogTagAction
        extends BaseGenerateAction {

    public InjectLogTagAction() {
        super(new GenerateInjectLogHandler());
    }

    static class GenerateInjectLogHandler
            implements CodeInsightActionHandler {

        @Override
        public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
            final Document document = editor.getDocument();
            CaretModel caretModel = editor.getCaretModel();
            final PsiClass psiClass = getPsiClass(editor, psiFile);
            if (psiClass == null) {
                warning("Inject TAG", "Caret must be in a class.");
                return;
            }

            if (findFieldByName(psiClass, "TAG") != null) {
                warning("Inject TAG", "TAG is already in the class.");
                return;
            }

            PsiElement lBrace = psiClass.getLBrace();
            if (lBrace != null) {
                final int line = document.getLineNumber(lBrace.getNode().getStartOffset()) + 1;
                caretModel.moveToLogicalPosition(new LogicalPosition(line, 0));
                final int offset = caretModel.getOffset();

                ApplicationManager.getApplication().runWriteAction(() -> {
                    String code = generateLogTagCode(psiClass);
                    document.insertString(offset, code + '\n');
                    CodeStyleManager.getInstance(project).adjustLineIndent(document, offset);

                });
            }
        }

        private PsiField findFieldByName(PsiClass psiClass, String fieldName) {
            PsiField[] fields = psiClass.getFields();
            for (PsiField field : fields) {
                if (field.getName().equals(fieldName)) {
                    return field;
                }
            }
            return null;
        }

        private PsiClass getPsiClass(Editor editor, PsiFile psiFile) {
            int offset = editor.getCaretModel().getOffset();
            PsiElement psiElement = psiFile.findElementAt(offset);
            return PsiTreeUtil.getParentOfType(psiElement, PsiClass.class);
        }

        private String generateLogTagCode(PsiClass psiClass) {
            return String.format("private static final String TAG = %s.class.getSimpleName();", psiClass.getName());
        }

        private void warning(String title, String message) {
            Notification notification = new Notification("inject-log-tag", title, message, NotificationType.WARNING);
            Notifications.Bus.notify(notification);
        }

        @Override
        public boolean startInWriteAction() {
            return false;
        }
    }
}
