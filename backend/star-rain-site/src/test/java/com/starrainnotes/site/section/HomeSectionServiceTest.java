/*
 * HomeSectionServiceTest：站点模块的测试文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：HomeSectionOrderDTO、HomeSectionPatchDTO、HomeSectionEntity、HomeSectionMapper、HomeSectionServiceImpl、SiteConfigException。
 * 阅读入口：malformedOrderDoesNotChangeAnySection（malformed顺序Does不ChangeAny区块处理入口）；overflowingLimitCannotBecomeValidThroughIntegerConversion（overflowing上限CannotBecome有效ThroughIntegerConversion处理入口）；section（区块处理入口）。
 */

package com.starrainnotes.site.section;

import com.starrainnotes.site.dto.HomeSectionOrderDTO;
import com.starrainnotes.site.dto.HomeSectionPatchDTO;
import com.starrainnotes.site.entity.HomeSectionEntity;
import com.starrainnotes.site.mapper.HomeSectionMapper;
import com.starrainnotes.site.service.impl.HomeSectionServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.site.exception.SiteConfigException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 首页区块业务服务测试，属于站点模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class HomeSectionServiceTest {
    /**
     * malformed顺序Does不ChangeAny区块处理入口。
     * 关键处理步骤调用 <code>mapper.all</code>、<code>List.of</code>、<code>section</code>、<code>order.setSectionCodes</code>。
     */
    @Test
    void malformedOrderDoesNotChangeAnySection() {
        HomeSectionMapper mapper = mock(HomeSectionMapper.class);
        when(mapper.all()).thenReturn(List.of(section("HERO"), section("BLOG")));
        HomeSectionOrderDTO order = new HomeSectionOrderDTO();
        order.setSectionCodes(List.of("HERO", "HERO"));

        SiteConfigException error = assertThrows(SiteConfigException.class,
                () -> new HomeSectionServiceImpl(mapper, new ObjectMapper()).reorder(order));

        assertEquals("SITE_SECTION_CONFIG_INVALID", error.getCode());
        verify(mapper, never()).update(any(HomeSectionEntity.class));
    }

    /**
     * overflowing上限CannotBecome有效ThroughIntegerConversion处理入口。
     * 关键处理步骤调用 <code>mapper.byCode</code>、<code>section</code>、<code>patch.setConfig</code>、<code>error.getCode</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    void overflowingLimitCannotBecomeValidThroughIntegerConversion() throws Exception {
        HomeSectionMapper mapper = mock(HomeSectionMapper.class);
        when(mapper.byCode("BLOG")).thenReturn(section("BLOG"));
        HomeSectionPatchDTO patch = new HomeSectionPatchDTO();
        patch.setConfig(new ObjectMapper().readTree("{\"limit\":4294967302}"));

        SiteConfigException error = assertThrows(SiteConfigException.class,
                () -> new HomeSectionServiceImpl(mapper, new ObjectMapper()).patch("BLOG", patch));

        assertEquals("SITE_SECTION_CONFIG_INVALID", error.getCode());
        verify(mapper, never()).update(any(HomeSectionEntity.class));
    }

    /**
     * 区块处理入口。
     * 关键处理步骤调用 <code>row.setSectionCode</code>、<code>row.setEnabled</code>。
     *
     * @param code 代码；类型为 <code>String</code>
     * @return 类型为 <code>HomeSectionEntity</code> 的处理结果
     */
    private HomeSectionEntity section(String code) {
        HomeSectionEntity row = new HomeSectionEntity();
        row.setSectionCode(code);
        row.setEnabled(true);
        return row;
    }
}
