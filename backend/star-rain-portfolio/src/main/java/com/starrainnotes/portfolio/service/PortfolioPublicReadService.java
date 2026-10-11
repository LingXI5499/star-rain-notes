/*
 * PortfolioPublicReadService：作品模块的业务契约文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：WorkVO。
 * 阅读入口：read（读取作品公开读取）。
 */

package com.starrainnotes.portfolio.service;

import com.starrainnotes.portfolio.vo.WorkVO;

// 作品集公开阅读入口：读取已发布作品详情并记录浏览事件。
/**
 * 作品公开读取业务服务，属于作品模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface PortfolioPublicReadService {

    /**
     * 读取作品公开读取。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO read(String slug);
}
