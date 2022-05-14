package com.mut.mybatis.interceptors;

import java.util.Map;

public interface ParameterProvider {

    Map<String, Object> getParameter();
}
