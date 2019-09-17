package com.mut.mybatis.interceptors.vo;

import java.util.List;

public class PagedResult<T> {

  /**
   * 总记录数，如果请求时要求不包含计算总记录数时，该参数返回当前页数据量，调用方可以通过 < pageSize 来判断是否有下一页
   */
  private long total;

  /**
   * 分页数据
   */
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
