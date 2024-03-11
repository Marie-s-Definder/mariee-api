package shu.scie.mariee.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ApiResult<T> {

    public boolean ok;
    public T data;

}
