package com.mut.mybatis.interceptors.vo;

import java.util.List;

public class PagedResult<T> {


  private long total;

  private List<T> data;

  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }

  public List<T> getData() {
    return data;
  }

  public void setData(List<T> data) {
    this.data = data;
  }


  public PagedResult() {}

  public PagedResult(long total, List<T> data) {
    this.total = total;
    this.data = data;
  }

}
