package com.starrainnotes.message.service;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.message.dto.MessageSubmitDTO;
import com.starrainnotes.message.vo.AdminMessageVO;
import com.starrainnotes.message.vo.MessageActionVO;
import com.starrainnotes.message.vo.PublicMessageVO;
import java.util.List;

public interface MessageService {
    Long submit(MessageSubmitDTO request, String remoteAddress);
    PageResult<PublicMessageVO> publicMessages(int page, int pageSize);
    PageResult<AdminMessageVO> adminMessages(int page, int pageSize, String status);
    List<MessageActionVO> actions(long id);
    AdminMessageVO approve(long id);
    AdminMessageVO reject(long id, String reason);
    AdminMessageVO hide(long id);
    AdminMessageVO restore(long id);
    void delete(long id);
}
