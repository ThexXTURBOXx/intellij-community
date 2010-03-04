package com.jetbrains.python.codeInsight;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actions.EditorActionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.psi.PyStatementWithElse;
import com.jetbrains.python.psi.PyTryExceptStatement;
import com.jetbrains.python.psi.patterns.Matcher;
import com.jetbrains.python.psi.patterns.ParentMatcher;

import java.util.List;

/**
 * Adjusts indentation after a final part keyword is inserted, e.g. an "else:".
 * User: dcheryasov
 * Date: Mar 2, 2010 6:48:40 PM
 */
public class UnindentingInsertHandler implements InsertHandler<PythonLookupElement> {

  private final static ParentMatcher TRY_MATCHER = new ParentMatcher(PyTryExceptStatement.class);
  private final static ParentMatcher ELSE_MATCHER = new ParentMatcher(PyStatementWithElse.class);

  /**
   * An instance good for 'try' final parts, that is, for 'except' and 'finally'.
   */
  public final static UnindentingInsertHandler OF_TRY = new UnindentingInsertHandler(TRY_MATCHER);

  /**
   * An instance good for 'else' parts of any statements that may contain an 'else' clause.
   */
  public final static UnindentingInsertHandler OF_ELSE = new UnindentingInsertHandler(ELSE_MATCHER);

  private ParentMatcher myTopMatcher;

  private UnindentingInsertHandler(ParentMatcher matcher) {
    myTopMatcher = matcher;
  }

  public void handleInsert(InsertionContext context, PythonLookupElement item) {
    unindentAsNeeded(context.getProject(), context.getEditor(), context.getFile(), myTopMatcher);
  }

  /**
   * Unindent current line to be flush with a starting part, detecting the part if necessary.
   * @param project
   * @param editor
   * @param file
   * @param matcher it must detect the outer partful statement; if null, the beginning of current line is analyzed
   * for keyword and a suitable matcher is found (e.g. for 'finally' the OF_TRY is found).
   * @return true if unindenting succeeded
   */
  public static boolean unindentAsNeeded(Project project, Editor editor, PsiFile file, Matcher matcher) {
    // TODO: handle things other than "else"
    final Document document = editor.getDocument();
    int offset = editor.getCaretModel().getOffset();
    CharSequence text = document.getCharsSequence();
    if (offset >= text.length()) offset = text.length() - 1;

    int line_start_offset = document.getLineStartOffset(document.getLineNumber(offset));
    int nonspace_offset = findBeginning(line_start_offset, text);

    if (matcher == null) {
      int last_offset = nonspace_offset + 7; // length of 'except:'
      if (last_offset > offset) last_offset = offset;
      int local_length = last_offset - nonspace_offset;
      if (local_length > 0) {
        final int else_len = "else".length();
        String piece = text.subSequence(nonspace_offset, last_offset+1).toString();
        if (local_length >= else_len) {
          if ((piece.startsWith("else") || piece.startsWith("elif")) && (piece.charAt(else_len) < 'a' || piece.charAt(else_len) < 'z')) {
            matcher = ELSE_MATCHER;
          }
        }
        final int except_len = "except".length();
        if (local_length >= except_len) {
          if (piece.startsWith("except") && (piece.charAt(except_len) < 'a' || piece.charAt(except_len) < 'z')) {
            matcher = TRY_MATCHER;
          }
        }
        final int finally_len = "finally".length();
        if (local_length >= finally_len) {
          if (piece.startsWith("finally") && (piece.charAt(finally_len) < 'a' || piece.charAt(finally_len) < 'z')) {
            matcher = TRY_MATCHER;
          }
        }
      }

    }

    if (matcher == null) return false; // failed

    PsiElement token = file.findElementAt(offset-1);
    List<? extends PsiElement> result = matcher.search(token);
    if (result != null && result.size() > 0) {
      PsiElement outer = result.get(0);
      int outer_offset = outer.getTextOffset();
      int outer_indent = outer_offset - document.getLineStartOffset(document.getLineNumber(outer_offset));
      assert outer_indent >= 0;
      int current_indent = nonspace_offset - line_start_offset;
      EditorActionUtil.indentLine(project, editor, document.getLineNumber(offset), outer_indent - current_indent);
      return true;
    }
    return false;
  }


  // finds offset of first non-space in the line
  private static int findBeginning(int start_offset, CharSequence text) {
    int current_offset = start_offset;
    int text_length = text.length();
    while (current_offset < text_length) {
      char current_char = text.charAt(current_offset);
      if (current_char != ' ' && current_char != '\t' && current_char != '\n') break;
      current_offset += 1;
    }
    return current_offset;
  }
}
