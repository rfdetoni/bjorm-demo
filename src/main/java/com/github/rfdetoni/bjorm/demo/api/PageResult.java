package com.github.rfdetoni.bjorm.demo.api;

import java.util.List;

public record PageResult<T>(List<T> items, long total, int page, int size, int pages) {}
