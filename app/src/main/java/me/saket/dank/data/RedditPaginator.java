package me.saket.dank.data;

import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Thing;
import net.dean.jraw.paginators.Paginator;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.TimePeriod;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: Remove.
 */
public class RedditPaginator<T extends Thing> {

  private RedditClient redditClient;
  private Sorting sorting;
  private TimePeriod timePeriod;
  private int limit = Paginator.DEFAULT_LIMIT;

//    public Listing<T> load() throws NetworkException, IllegalStateException {
//        String path = getBaseUri();
//
//        Map<String, String> args = new HashMap<>();
//        args.put("limit", String.valueOf(limit));
//        if (current != null && current.getAfter() != null)
//            args.put("after", current.getAfter());
//
//        String sorting = getSortingString();
//        boolean sortingUsed = sorting != null;
//
//        if (sortingUsed) {
//            args.put("sort", sorting);
//
//            if (timePeriod != null) {
//                // Time period only applies to controversial and top listings
//                args.put("t", timePeriod.name().toLowerCase());
//            }
//        }
//
//        Map<String, String> extraArgs = getExtraQueryArgs();
//        if (extraArgs != null && extraArgs.size() > 0) {
//            args.putAll(extraArgs);
//        }
//
//        HttpRequest request = redditClient.request()
//                .path(path)
//                .query(args)
//                // Force a network response if sorting by new or explicitly declared
//                .cacheControl(CacheControl.FORCE_NETWORK)
//                .build();
//
//        RestResponse response;
//        response = redditClient.execute(request);
//        Listing<T> listing = parseListing(response);
//        this.current = listing;
//        pageNumber++;
//
//        return listing;
//    }

  /**
   * Generates extra arguments to be included in the query string.
   *
   * @return A non-null map of paginator-implementation-specific arguments
   */
  protected Map<String, String> getExtraQueryArgs() {
    return new HashMap<>();
  }

  protected String getSortingString() {
    if (timePeriod == null) {
      return null;
    }

    if (sorting == Sorting.CONTROVERSIAL || sorting == Sorting.TOP) {
      return sorting.name().toLowerCase();
    } else {
      return null;
    }
  }

}
