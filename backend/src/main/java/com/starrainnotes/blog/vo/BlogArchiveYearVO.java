package com.starrainnotes.blog.vo;

import java.util.List;

public record BlogArchiveYearVO(int year, List<BlogArchiveMonthVO> months) {
}
