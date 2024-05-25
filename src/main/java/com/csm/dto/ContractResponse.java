package com.csm.dto;

public class ContractResponse {

    private Integer code;
    private String message;
    private Object obj;

    public ContractResponse(Integer icode, String imessage) {
        this.code = icode;
        this.message = imessage;
    }
    public ContractResponse(Integer icode, String imessage, Object iobj) {
        this.code = icode;
        this.message = imessage;
        this.obj = iobj;
    }
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }
}
