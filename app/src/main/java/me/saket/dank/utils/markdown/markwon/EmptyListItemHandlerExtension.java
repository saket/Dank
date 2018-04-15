package me.saket.dank.utils.markdown.markwon;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.ListItem;
import org.commonmark.node.Paragraph;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.parser.PostProcessor;

import javax.inject.Inject;

/**
 * Text like "24601." get parsed as ordered list items. But without any text, the index doesn't
 * get printed either resulting in an empty text. This extension adds empty text to force printing
 * of the index.
 */
class EmptyListItemHandlerExtension implements Parser.ParserExtension {

  @Inject
  public EmptyListItemHandlerExtension() {
  }

  @Override
  public void extend(Parser.Builder parserBuilder) {
    PostProcessor processor = node -> {
      ListItemVisitor visitor = new ListItemVisitor();
      node.accept(visitor);
      return node;
    };
    parserBuilder.postProcessor(processor);
  }

  private static class ListItemVisitor extends AbstractVisitor {
    @Override
    public void visit(ListItem listItem) {
      if (listItem.getFirstChild() == null) {
        // List item without any text.
        Paragraph paragraph = new Paragraph();
        paragraph.appendChild(new Text("\u00a0"));
        listItem.appendChild(paragraph);
      }
      super.visit(listItem);
    }
  }
}
