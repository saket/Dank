package me.saket.dank.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.CommentSort;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.meta.SubmissionSerializer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Moshi adapter for {@link Submission}.
 */
public class MoshiSubmissionAdapter {

  private static Field commentSortField;
  private JacksonHelper jacksonHelper;

  public MoshiSubmissionAdapter(JacksonHelper jacksonHelper) {
    this.jacksonHelper = jacksonHelper;
  }

  @FromJson
  public Submission fromJson(String json) throws IOException {
    if (json.startsWith("{")) {
      JsonNode submissionJsonNode = jacksonHelper.parseJsonNode(json.substring(1));
      return new Submission(submissionJsonNode);

    } else {
      JsonNode jsonNode = jacksonHelper.parseJsonNode(json);
      CommentSort commentSort = CommentSort.valueOf(jsonNode.get(2).get("dank_comments_sort").asText());
      return SubmissionSerializer.withComments(jsonNode, commentSort);
    }
  }

  @ToJson
  public String toJson(Submission submission) throws IOException, NoSuchFieldException, IllegalAccessException {
    if (submission.getComments() == null) {
      return "{" + jacksonHelper.toJson(submission);
    } else {
      return writeSubmissionWithCommentsToJson(submission);
    }
  }

  /**
   * Constructs a JSON equivalent to what Reddit produces so that {@link SubmissionSerializer#withComments(JsonNode, CommentSort)} can be used.
   */
  public String writeSubmissionWithCommentsToJson(Submission submission) throws IOException, NoSuchFieldException, IllegalAccessException {
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append("[");

    // Submission object.
    jsonBuilder.append("{");
    jsonBuilder.append("\"kind\":\"Listing\",");
    jsonBuilder.append("\"data\":");
    jsonBuilder.append("{");
    jsonBuilder.append("\"children\":");
    jsonBuilder.append("[");
    jsonBuilder.append("{");
    jsonBuilder.append("\"kind\":\"t3\",");
    jsonBuilder.append("\"data\":");
    jsonBuilder.append(jacksonHelper.toJson(submission));
    jsonBuilder.append("}");
    jsonBuilder.append("]");
    jsonBuilder.append("}");
    jsonBuilder.append("},");

    // Comments object.
    jsonBuilder.append("{");
    jsonBuilder.append("\"kind\":\"Listing\",");
    jsonBuilder.append("\"data\":");
    jsonBuilder.append("{");

    jsonBuilder.append("\"children\":");
    jsonBuilder.append("[");
    List<CommentNode> children = submission.getComments().getChildren();
    for (int i = 0; i < children.size(); i++) {
      CommentNode childCommentNode = children.get(i);
      jsonBuilder.append("{");
      jsonBuilder.append("\"kind\":\"t1\",");
      jsonBuilder.append("\"data\":");
      String commentJson = jacksonHelper.toJson(childCommentNode.getComment().getDataNode());
      jsonBuilder.append(commentJson);
      jsonBuilder.append("}");

      if (i < children.size() - 1) {
        jsonBuilder.append(",");
      }
    }
    jsonBuilder.append("]");
    jsonBuilder.append("}");
    jsonBuilder.append("},");

    // Sort
    jsonBuilder.append("{");
    CommentSort commentSort = getCommentSort(submission);
    jsonBuilder.append("\"dank_comments_sort\":\"").append(commentSort).append("\"");
    jsonBuilder.append("}");

    jsonBuilder.append("]");

    return jsonBuilder.toString();
  }

  private CommentSort getCommentSort(Submission submission) throws NoSuchFieldException, IllegalAccessException {
    if (commentSortField == null) {
      commentSortField = CommentNode.class.getDeclaredField("commentSort");
      commentSortField.setAccessible(true);
    }
    return (CommentSort) commentSortField.get(submission.getComments());
  }
}
