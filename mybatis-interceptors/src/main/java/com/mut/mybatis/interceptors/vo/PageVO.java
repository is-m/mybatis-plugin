package com.mut.mybatis.interceptors.vo;

import java.io.Serializable;

public class PageVO implements Serializable {


  private static final long serialVersionUID = -5748509209645323438L;

  /**
   * 最大页大小
   */
  public static final int MAX_PAGE_SIZE = 2000;

  /**
   * 最小页大小
   */
  public static final int MIN_PAGE_SIZE = 1;

  /**
   * 最小页
   */
  public static final int MIN_PAGE = 1;

  /**
   * 当前页
   */
  private int curPage = 1;

  /**
   * 页大小
   */
  private int pageSize = 15;

  /**
   * 总记录数
   */
  private long totalRecord = 0;

  private boolean includeTotal = true;

  public int getCurPage() {
    return curPage;
  }

  public void setCurPage(int curPage) {
    this.curPage = curPage < MIN_PAGE ? MIN_PAGE : curPage;
  }

  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize < MIN_PAGE_SIZE ? MIN_PAGE_SIZE : (pageSize > MAX_PAGE_SIZE ? MAX_PAGE_SIZE : pageSize);
  }

  public long getTotalRecord() {
    return totalRecord;
  }

  public void setTotalRecord(long totalRecord) {
    this.totalRecord = totalRecord;
  }

  public int getTotalPage() {
    return (int) Math.ceil((double) totalRecord / pageSize);
  }

  public int getPageStartIndex() {
    return (curPage - 1) * pageSize + 1;
  }

  public int getPageEndIndex() {
    return curPage * pageSize;
  }

  public PageVO() {}

  public PageVO(int curPage, int pageSize) {
    this(curPage, pageSize, 0);
  }

  public PageVO(int curPage, int pageSize, int totalRecord) {
    setCurPage(curPage);
    setPageSize(pageSize);
    setTotalRecord(totalRecord);
  }

  @Override
  public String toString() {
    return "PageVO [curPage=" + curPage + ", pageSize=" + pageSize + ", totalRecord=" + totalRecord + "]";
  }

  public boolean isIncludeTotal() {
    return includeTotal;
  }

  public void setIncludeTotal(boolean includeTotal) {
    this.includeTotal = includeTotal;
  }
}
