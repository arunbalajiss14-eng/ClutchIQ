package com.mevsme.view;

import lombok.AllArgsConstructor;
import lombok.Data;

/** One routine row on the check-in form. value is DONE, PARTIAL, MISSED or "" (not logged). */
@Data @AllArgsConstructor
public class CatRow {
    private String key;
    private String label;
    private String hint;
    private String value;
}
