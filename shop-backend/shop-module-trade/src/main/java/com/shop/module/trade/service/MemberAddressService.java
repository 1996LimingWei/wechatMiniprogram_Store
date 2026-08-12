package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.MemberAddressDO;
import com.shop.module.trade.dal.mysql.MemberAddressMapper;
import com.shop.module.trade.util.TradeRequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MemberAddressService {

    private static final int MAX_ADDRESS_COUNT = 20;
    private static final String MOBILE_PATTERN = "^1[3-9]\\d{9}$";

    private final MemberAddressMapper memberAddressMapper;

    public List<Map<String, Object>> getAddressList(Long userId) {
        return memberAddressMapper.selectList(new LambdaQueryWrapper<MemberAddressDO>()
                        .eq(MemberAddressDO::getUserId, userId)
                        .orderByDesc(MemberAddressDO::getIsDefault)
                        .orderByDesc(MemberAddressDO::getUpdateTime))
                .stream()
                .map(this::toResp)
                .toList();
    }

    public MemberAddressDO getDefaultOrFirst(Long userId) {
        List<MemberAddressDO> list = memberAddressMapper.selectList(new LambdaQueryWrapper<MemberAddressDO>()
                .eq(MemberAddressDO::getUserId, userId)
                .orderByDesc(MemberAddressDO::getIsDefault)
                .orderByDesc(MemberAddressDO::getUpdateTime));
        return list.isEmpty() ? null : list.get(0);
    }

    public MemberAddressDO getAddress(Long userId, Long id) {
        if (id == null || id <= 0) {
            return getDefaultOrFirst(userId);
        }
        MemberAddressDO address = memberAddressMapper.selectOne(new LambdaQueryWrapper<MemberAddressDO>()
                .eq(MemberAddressDO::getUserId, userId)
                .eq(MemberAddressDO::getId, id));
        if (address == null) {
            throw new ServerException(1404, "收货地址不存在");
        }
        return address;
    }

    public Map<String, Object> getAddressDetail(Long userId, Long id) {
        return toResp(getAddress(userId, id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveAddress(Long userId, Map<String, Object> body) {
        Long id = TradeRequestUtils.getLong(body, "id", 0L);
        Integer isDefault = TradeRequestUtils.getInt(body, "isDefault", 0);
        if (isDefault != 0 && isDefault != 1) {
            throw new ServerException(400, "默认地址状态不正确");
        }

        MemberAddressDO address = id > 0 ? getAddress(userId, id) : new MemberAddressDO();
        if (id <= 0 && memberAddressMapper.selectCount(new LambdaQueryWrapper<MemberAddressDO>()
                .eq(MemberAddressDO::getUserId, userId)) >= MAX_ADDRESS_COUNT) {
            throw new ServerException(400, "收货地址最多保存 20 条");
        }
        address.setUserId(userId);
        address.setUserName(TradeRequestUtils.getString(body, "userName", "").trim());
        address.setTelNumber(TradeRequestUtils.getString(body, "telNumber", "").trim());
        address.setProvinceId(TradeRequestUtils.getLong(body, "provinceId", 0L));
        address.setCityId(TradeRequestUtils.getLong(body, "cityId", 0L));
        address.setDistrictId(TradeRequestUtils.getLong(body, "districtId", 0L));
        address.setProvinceName(TradeRequestUtils.getString(body, "provinceName", "").trim());
        address.setCityName(TradeRequestUtils.getString(body, "cityName", "").trim());
        address.setDistrictName(TradeRequestUtils.getString(body, "countyName",
                TradeRequestUtils.getString(body, "districtName", "")).trim());
        address.setFullRegion(address.getProvinceName() + address.getCityName() + address.getDistrictName());
        address.setDetailInfo(TradeRequestUtils.getString(body, "detailInfo", "").trim());
        address.setIsDefault(isDefault);

        validateAddress(address);
        boolean hasDefault = memberAddressMapper.selectCount(new LambdaQueryWrapper<MemberAddressDO>()
                .eq(MemberAddressDO::getUserId, userId)
                .eq(MemberAddressDO::getIsDefault, 1)
                .ne(id > 0, MemberAddressDO::getId, id)) > 0;
        if (!hasDefault && isDefault == 0) {
            address.setIsDefault(1);
        }
        if (address.getIsDefault() == 1) {
            memberAddressMapper.update(null, new LambdaUpdateWrapper<MemberAddressDO>()
                    .eq(MemberAddressDO::getUserId, userId)
                    .ne(id > 0, MemberAddressDO::getId, id)
                    .set(MemberAddressDO::getIsDefault, 0));
        }

        if (id > 0) {
            memberAddressMapper.updateById(address);
        } else {
            try {
                memberAddressMapper.insert(address);
            } catch (DuplicateKeyException exception) {
                throw new ServerException(409, "默认地址已被其他操作更新，请刷新后重试");
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Long userId, Long id) {
        MemberAddressDO address = getAddress(userId, id);
        memberAddressMapper.deleteById(address.getId());
        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            MemberAddressDO replacement = memberAddressMapper.selectOne(
                    new LambdaQueryWrapper<MemberAddressDO>()
                            .eq(MemberAddressDO::getUserId, userId)
                            .orderByDesc(MemberAddressDO::getUpdateTime)
                            .orderByDesc(MemberAddressDO::getId)
                            .last("LIMIT 1"));
            if (replacement != null) {
                memberAddressMapper.update(null, new LambdaUpdateWrapper<MemberAddressDO>()
                        .eq(MemberAddressDO::getId, replacement.getId())
                        .eq(MemberAddressDO::getIsDefault, 0)
                        .set(MemberAddressDO::getIsDefault, 1));
            }
        }
    }

    private void validateAddress(MemberAddressDO address) {
        if (address.getUserName().isEmpty() || address.getUserName().length() > 32) {
            throw new ServerException(400, "收货人姓名长度应为 1 至 32 个字符");
        }
        if (!address.getTelNumber().matches(MOBILE_PATTERN)) {
            throw new ServerException(400, "请输入正确的中国大陆手机号");
        }
        if (address.getProvinceId() <= 0 || address.getCityId() <= 0 || address.getDistrictId() <= 0
                || address.getProvinceName().isEmpty() || address.getCityName().isEmpty()
                || address.getDistrictName().isEmpty()) {
            throw new ServerException(400, "请选择完整的省市区");
        }
        if (address.getProvinceName().length() > 32 || address.getCityName().length() > 32
                || address.getDistrictName().length() > 32) {
            throw new ServerException(400, "省市区名称长度不正确");
        }
        if (address.getDetailInfo().length() < 2 || address.getDetailInfo().length() > 128) {
            throw new ServerException(400, "详细地址长度应为 2 至 128 个字符");
        }
    }

    public Map<String, Object> toResp(MemberAddressDO address) {
        if (address == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("id", 0);
            return empty;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", address.getId());
        result.put("userName", address.getUserName());
        result.put("telNumber", address.getTelNumber());
        result.put("provinceId", address.getProvinceId());
        result.put("cityId", address.getCityId());
        result.put("districtId", address.getDistrictId());
        result.put("provinceName", address.getProvinceName());
        result.put("cityName", address.getCityName());
        result.put("districtName", address.getDistrictName());
        result.put("fullRegion", address.getFullRegion());
        result.put("detailInfo", address.getDetailInfo());
        result.put("isDefault", address.getIsDefault());
        return result;
    }
}
