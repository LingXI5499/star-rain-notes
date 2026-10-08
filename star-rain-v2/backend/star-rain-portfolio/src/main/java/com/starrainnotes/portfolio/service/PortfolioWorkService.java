package com.starrainnotes.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.dto.IdOrderDTO;
import com.starrainnotes.portfolio.dto.WorkCreateDTO;
import com.starrainnotes.portfolio.dto.WorkLinkDTO;
import com.starrainnotes.portfolio.dto.WorkMediaDTO;
import com.starrainnotes.portfolio.dto.WorkPatchDTO;
import com.starrainnotes.portfolio.enumeration.WorkType;
import com.starrainnotes.portfolio.vo.WorkVO;

public interface PortfolioWorkService {
    PageResult<WorkVO> publicWorks(int page, int pageSize, WorkType type);
    PageResult<WorkVO> filteredWorks(int page, int pageSize, WorkType type, String status, String keyword,
        com.starrainnotes.portfolio.dto.WorkFilterDTO filter, boolean publicOnly);
    WorkVO publicWork(String slug);
    WorkVO publicWorkById(Long id);
    PageResult<WorkVO> adminWorks(int page, int pageSize, WorkType type, String status, String keyword);
    WorkVO adminWork(Long id);
    WorkVO create(WorkCreateDTO request);
    WorkVO update(Long id, WorkPatchDTO request);
    WorkVO updateBody(Long id, String markdown);
    WorkVO updateDetail(Long id, JsonNode detail);
    WorkVO addMedia(Long id, WorkMediaDTO request);
    WorkVO updateMedia(Long mediaId, WorkMediaDTO request);
    void removeMedia(Long mediaId);
    WorkVO orderMedia(Long id, IdOrderDTO request);
    WorkVO addLink(Long id, WorkLinkDTO request);
    WorkVO updateLink(Long linkId, WorkLinkDTO request);
    void removeLink(Long linkId);
    WorkVO orderLinks(Long id, IdOrderDTO request);
    WorkVO publish(Long id);
    WorkVO withdraw(Long id);
    WorkVO restore(Long id);
    void delete(Long id);
}
