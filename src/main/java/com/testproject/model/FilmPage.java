package com.testproject.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class FilmPage {
    List<Film> content;
    int totalElements;
    int totalPages;
}
