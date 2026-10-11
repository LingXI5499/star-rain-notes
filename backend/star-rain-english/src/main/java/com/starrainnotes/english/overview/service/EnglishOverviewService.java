/*
 * EnglishOverviewService：英语学习模块的业务契约文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：EnglishOverviewRequestDTO、EnglishOverviewVO。
 * 阅读入口：get（读取英语Overview）；update（更新英语Overview）。
 */

package com.starrainnotes.english.overview.service;

import com.starrainnotes.english.overview.dto.EnglishOverviewRequestDTO;
import com.starrainnotes.english.overview.vo.EnglishOverviewVO;

// 英语模块总览内容读写入口。
/**
 * 英语Overview业务服务，属于英语学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface EnglishOverviewService {

    /**
     * 读取英语Overview。
     * @return 类型为 <code>EnglishOverviewVO</code> 的处理结果
     */
    EnglishOverviewVO get();

    /**
     * 更新英语Overview。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>EnglishOverviewRequestDTO</code>
     * @return 类型为 <code>EnglishOverviewVO</code> 的处理结果
     */
    EnglishOverviewVO update(EnglishOverviewRequestDTO request);
}
