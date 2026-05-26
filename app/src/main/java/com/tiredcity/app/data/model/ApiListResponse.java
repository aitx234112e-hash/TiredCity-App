package com.tiredcity.app.data.model;

import java.util.List;

public class ApiListResponse<T> {
    private boolean success;
    private String message;
    private List<T> data;
    private int total;
    private int page;
    private int pageSize;
    private int totalPages;

    public ApiListResponse() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
