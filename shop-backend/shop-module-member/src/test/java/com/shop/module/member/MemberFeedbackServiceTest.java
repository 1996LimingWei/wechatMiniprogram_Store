package com.shop.module.member;

import com.shop.common.exception.ServerException;
import com.shop.module.member.dal.mysql.MemberFeedbackMapper;
import com.shop.module.member.dal.dataobject.MemberFeedbackDO;
import com.shop.module.member.service.MemberFeedbackService;
import com.shop.module.member.vo.FeedbackCreateReqVO;
import com.shop.module.member.vo.FeedbackHandleReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberFeedbackServiceTest {

    @Test
    void shouldRejectFrequentFeedbackSubmissions() {
        MemberFeedbackMapper mapper = mock(MemberFeedbackMapper.class);
        when(mapper.selectCount(any())).thenReturn(3L);
        MemberFeedbackService service = new MemberFeedbackService(mapper, mock(JdbcTemplate.class));
        FeedbackCreateReqVO request = new FeedbackCreateReqVO();
        request.setType(1);
        request.setContent("商品包装需要改进");

        ServerException exception = assertThrows(ServerException.class, () -> service.create(1L, request));

        assertEquals(429, exception.getCode());
    }

    @Test
    void shouldNotAllowCompletedFeedbackToBeReopened() {
        MemberFeedbackMapper mapper = mock(MemberFeedbackMapper.class);
        MemberFeedbackDO feedback = new MemberFeedbackDO();
        feedback.setId(8L);
        feedback.setStatus(2);
        when(mapper.selectById(8L)).thenReturn(feedback);
        FeedbackHandleReqVO request = new FeedbackHandleReqVO();
        request.setId(8L);
        request.setStatus(1);
        request.setHandleRemark("重新处理");

        ServerException exception = assertThrows(ServerException.class,
                () -> new MemberFeedbackService(mapper, mock(JdbcTemplate.class)).handle(2L, request));

        assertEquals(409, exception.getCode());
    }

}
