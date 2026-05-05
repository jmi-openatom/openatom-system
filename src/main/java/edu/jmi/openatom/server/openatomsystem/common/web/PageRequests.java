package edu.jmi.openatom.server.openatomsystem.common.web;

public final class PageRequests {
  public static final long DEFAULT_PAGE = 1L;
  public static final long DEFAULT_PAGE_SIZE = 10L;
  public static final long MAX_PAGE_SIZE = 100L;

  private PageRequests() {}

  public static long page(Long page) {
    return page == null || page < 1 ? DEFAULT_PAGE : page;
  }

  public static long pageSize(Long pageSize) {
    if (pageSize == null || pageSize < 1) {
      return DEFAULT_PAGE_SIZE;
    }
    return Math.min(pageSize, MAX_PAGE_SIZE);
  }
}
