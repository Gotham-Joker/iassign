package com.github.iassign.vo;

import lombok.Data;

@Data
public class SysMenuQuery {
    private String id;
    private String text;

    public SysMenuQuery() {

    }

    public SysMenuQuery(String id, String text) {
        this.id = id;
        this.text = text;
    }
}
