package com.shop.module.trade.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.ServerException;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 固定版本离线省市区；不以行政区划存在性承诺配送可达。 */
public final class RegionCatalog {
    public record Region(long id, String name, long parentId, int type) {}
    private static final List<Region> REGIONS = load();
    private static final Map<Long, Region> BY_ID = REGIONS.stream()
            .collect(Collectors.toUnmodifiableMap(Region::id, Function.identity()));

    private RegionCatalog() {}

    private static List<Region> load() {
        try (InputStream stream = RegionCatalog.class.getResourceAsStream("/regions/mainland.json")) {
            if (stream == null) throw new IllegalStateException("缺少离线地区资源");
            List<Region> result = new ObjectMapper().readValue(stream, new TypeReference<>() {});
            if (result.stream().filter(region -> region.type() == 1).count() != 31) {
                throw new IllegalStateException("地区省级数据不完整");
            }
            return List.copyOf(result);
        } catch (Exception exception) {
            throw new IllegalStateException("地区数据加载失败，禁止使用演示区县", exception);
        }
    }

    private static long normalize(long id) {
        if (id > 1 && id < 100) return id * 10000;
        if (id >= 1000 && id < 10000) return id * 100;
        return id;
    }

    public static List<Map<String, Object>> children(Long parentId) {
        long parent = parentId == null ? 1 : normalize(parentId);
        return REGIONS.stream().filter(region -> region.parentId() == parent)
                .map(region -> Map.<String, Object>of("id", region.id(), "name", region.name(),
                        "parentId", region.parentId(), "type", region.type())).toList();
    }

    public static List<Region> validateChain(Long provinceId, Long cityId, Long districtId) {
        Region province = provinceId == null ? null : BY_ID.get(normalize(provinceId));
        Region city = cityId == null ? null : BY_ID.get(normalize(cityId));
        Region district = districtId == null ? null : BY_ID.get(districtId);
        if (province == null || city == null || district == null || province.type() != 1
                || city.type() != 2 || district.type() != 3 || city.parentId() != province.id()
                || district.parentId() != city.id()) {
            throw new ServerException(400, "省市区组合无效或已调整，请重新选择真实地区");
        }
        return List.of(province, city, district);
    }
}
