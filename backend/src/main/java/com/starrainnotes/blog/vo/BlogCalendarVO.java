package com.starrainnotes.blog.vo;

import java.util.List;

public record BlogCalendarVO(String month, List<BlogCalendarDayVO> days) {
}
