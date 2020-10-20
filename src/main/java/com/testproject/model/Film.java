package com.testproject.model;

import lombok.*;

@Data
@NoArgsConstructor
public class Film {
    private String id;
    private int genre;
    private int estimation;
    private String name;
}
