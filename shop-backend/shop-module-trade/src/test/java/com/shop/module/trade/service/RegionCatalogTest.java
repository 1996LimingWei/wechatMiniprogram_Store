package com.shop.module.trade.service;

import com.shop.common.exception.ServerException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegionCatalogTest {
    @Test
    void coversMainlandAndActualShaanxiDistrict() {
        assertEquals(31, RegionCatalog.children(1L).size());
        assertFalse(RegionCatalog.children(61L).isEmpty());
        assertEquals("陕西省", RegionCatalog.validateChain(61L, 6101L, 610113L).get(0).name());
        assertEquals("雁塔区", RegionCatalog.validateChain(610000L, 610100L, 610113L).get(2).name());
    }

    @Test
    void rejectsInventedOrCrossProvinceDistricts() {
        assertThrows(ServerException.class, () -> RegionCatalog.validateChain(61L, 4403L, 440305L));
        assertThrows(ServerException.class, () -> RegionCatalog.validateChain(61L, 6101L, 610199L));
        assertTrue(RegionCatalog.children(999999L).isEmpty());
    }

    @Test
    void supportsMunicipalitiesAndOldShortCodes() {
        assertEquals("东城区", RegionCatalog.validateChain(11L, 1101L, 110101L).get(2).name());
        assertEquals("南山区", RegionCatalog.validateChain(44L, 4403L, 440305L).get(2).name());
    }

    @Test
    void everyPublishedDistrictHasValidParentsAndCanonicalNames() {
        int districts = 0;
        for (var province : RegionCatalog.children(1L)) {
            long provinceId = ((Number) province.get("id")).longValue();
            for (var city : RegionCatalog.children(provinceId)) {
                long cityId = ((Number) city.get("id")).longValue();
                for (var district : RegionCatalog.children(cityId)) {
                    long districtId = ((Number) district.get("id")).longValue();
                    assertEquals(district.get("name"), RegionCatalog.validateChain(provinceId, cityId, districtId).get(2).name());
                    districts++;
                }
            }
        }
        assertEquals(3061, districts);
    }
}
