package com.shop.module.trade.service;

import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.MemberAddressDO;
import com.shop.module.trade.dal.mysql.MemberAddressMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberAddressServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(MemberAddressDO.class);
    }

    @Mock
    private MemberAddressMapper memberAddressMapper;
    @InjectMocks
    private MemberAddressService memberAddressService;

    @Test
    void shouldMakeFirstAddressDefaultAutomatically() {
        when(memberAddressMapper.selectCount(any())).thenReturn(0L);
        ArgumentCaptor<MemberAddressDO> captor = ArgumentCaptor.forClass(MemberAddressDO.class);

        memberAddressService.saveAddress(1L, validAddress());

        verify(memberAddressMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getIsDefault());
        assertEquals("浙江省杭州市西湖区", captor.getValue().getFullRegion());
    }

    @Test
    void shouldRejectInvalidMobileBeforeInsert() {
        when(memberAddressMapper.selectCount(any())).thenReturn(0L);
        Map<String, Object> request = new java.util.HashMap<>(validAddress());
        request.put("telNumber", "12345");

        ServerException exception = assertThrows(ServerException.class,
                () -> memberAddressService.saveAddress(1L, request));

        assertEquals(400, exception.getCode());
        verify(memberAddressMapper, never()).insert(any());
    }

    private Map<String, Object> validAddress() {
        return Map.ofEntries(
                Map.entry("id", 0), Map.entry("isDefault", 0),
                Map.entry("userName", "张三"), Map.entry("telNumber", "13800000000"),
                Map.entry("provinceId", 330000), Map.entry("cityId", 330100),
                Map.entry("districtId", 330106), Map.entry("provinceName", "浙江省"),
                Map.entry("cityName", "杭州市"), Map.entry("countyName", "西湖区"),
                Map.entry("detailInfo", "文三路 100 号"));
    }
}
