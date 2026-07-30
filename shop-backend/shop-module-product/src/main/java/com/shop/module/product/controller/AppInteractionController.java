package com.shop.module.product.controller;

import com.shop.common.exception.ServerException;
import com.shop.framework.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/app-api")
@RequiredArgsConstructor
public class AppInteractionController {
    private final JdbcTemplate jdbc;

    @PostMapping("/collect/addordelete")
    @Transactional
    public Map<String, Object> toggleCollect(@RequestParam(defaultValue = "0") int typeId, @RequestParam Long valueId) {
        Long userId = userId(); requireGoods(valueId);
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM member_collect WHERE user_id=? AND spu_id=? AND deleted=0", Integer.class, userId, valueId);
        String type;
        if (count != null && count > 0) { jdbc.update("UPDATE member_collect SET deleted=1 WHERE user_id=? AND spu_id=? AND deleted=0", userId, valueId); type = "delete"; }
        else {
            int restored = jdbc.update("UPDATE member_collect SET deleted=0, update_time=CURRENT_TIMESTAMP WHERE user_id=? AND spu_id=? AND deleted=1", userId, valueId);
            if (restored == 0) jdbc.update("INSERT INTO member_collect(user_id,spu_id) VALUES (?,?)", userId, valueId);
            type = "add";
        }
        return ok(Map.of("type", type));
    }

    @RequestMapping("/collect/list")
    public Map<String, Object> collectList(@RequestParam(defaultValue = "0") int typeId) {
        List<Map<String,Object>> list = jdbc.queryForList("SELECT c.spu_id valueId, p.id, p.name, p.introduction goodsBrief, p.pic_url listPicUrl, p.price retailPrice FROM member_collect c JOIN product_spu p ON p.id=c.spu_id WHERE c.user_id=? AND c.deleted=0 AND p.status=1 AND p.deleted=0 ORDER BY c.create_time DESC", userId()).stream().map(this::price).toList();
        return ok(list);
    }

    @RequestMapping("/footprint/list")
    public Map<String, Object> footprintList() {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT f.id, f.spu_id goodsId, DATE_FORMAT(f.browse_date,'%Y-%m-%d') addTime, p.name, p.introduction goodsBrief, p.pic_url listPicUrl, p.price retailPrice FROM member_footprint f JOIN product_spu p ON p.id=f.spu_id WHERE f.user_id=? AND f.deleted=0 AND p.status=1 AND p.deleted=0 ORDER BY f.browse_date DESC,f.update_time DESC", userId()).stream().map(this::price).toList();
        List<List<Map<String,Object>>> groups = new ArrayList<>(rows.stream().collect(Collectors.groupingBy(row -> String.valueOf(row.get("addTime")), LinkedHashMap::new, Collectors.toList())).values());
        return ok(Map.of("data", groups));
    }

    @PostMapping("/footprint/delete")
    public Map<String,Object> deleteFootprint(@RequestParam Long footprintId) { return ok(Map.of("deleted", jdbc.update("UPDATE member_footprint SET deleted=1 WHERE id=? AND user_id=? AND deleted=0", footprintId, userId()) > 0)); }

    @RequestMapping("/comment/list")
    public Map<String,Object> commentList(@RequestParam Long valueId, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size) {
        int offset = Math.max(page - 1, 0) * size;
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT c.id,c.content,DATE_FORMAT(c.create_time,'%Y-%m-%d') addTime,u.nickname,u.avatar FROM product_comment c JOIN member_user u ON u.id=c.user_id WHERE c.spu_id=? AND c.status=1 AND c.deleted=0 ORDER BY c.create_time DESC LIMIT ? OFFSET ?", valueId, size, offset).stream().map(row -> { row.put("userInfo", Map.of("nickname", row.get("nickname"), "avatar", row.get("avatar") == null ? "" : row.get("avatar"))); row.put("picList", List.of()); return row; }).toList();
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM product_comment WHERE spu_id=? AND status=1 AND deleted=0", Integer.class, valueId);
        return ok(Map.of("records", rows, "total", total == null ? 0 : total));
    }

    @RequestMapping("/comment/count")
    public Map<String,Object> commentCount(@RequestParam Long valueId) { Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM product_comment WHERE spu_id=? AND status=1 AND deleted=0",Integer.class,valueId); return ok(Map.of("allCount",count==null?0:count,"hasPicCount",0)); }

    @PostMapping("/comment/post")
    public Map<String,Object> postComment(@RequestParam Long valueId, @RequestParam String content) { requireGoods(valueId); if(content==null||content.isBlank()) throw new ServerException(400,"评论内容不能为空"); jdbc.update("INSERT INTO product_comment(user_id,spu_id,content) VALUES (?,?,?)",userId(),valueId,content.trim()); return ok(Map.of()); }

    @PostMapping("/footprint/record")
    @Transactional
    public Map<String,Object> recordFootprint(@RequestParam Long goodsId) { Long user=userId(); requireGoods(goodsId); int updated=jdbc.update("UPDATE member_footprint SET update_time=CURRENT_TIMESTAMP WHERE user_id=? AND spu_id=? AND browse_date=? AND deleted=0",user,goodsId,LocalDate.now()); if(updated==0) { int restored=jdbc.update("UPDATE member_footprint SET deleted=0, update_time=CURRENT_TIMESTAMP WHERE user_id=? AND spu_id=? AND browse_date=? AND deleted=1",user,goodsId,LocalDate.now()); if(restored==0) jdbc.update("INSERT INTO member_footprint(user_id,spu_id,browse_date) VALUES (?,?,?)",user,goodsId,LocalDate.now()); } return ok(Map.of()); }

    private Long userId() { Authentication a=SecurityContextHolder.getContext().getAuthentication(); if(a!=null&&a.getPrincipal() instanceof LoginUser u) return u.getUserId(); throw new ServerException(401,"请先登录"); }
    private void requireGoods(Long id) { Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM product_spu WHERE id=? AND status=1 AND deleted=0",Integer.class,id); if(count==null||count==0) throw new ServerException(404,"商品不存在或已下架"); }
    private Map<String,Object> price(Map<String,Object> row) { Object value=row.get("retailPrice"); if(value instanceof Number n) row.put("retailPrice",String.format("%.2f",n.longValue()/100.0)); return row; }
    private Map<String,Object> ok(Object data) { Map<String,Object> result=new LinkedHashMap<>(); result.put("code",0);result.put("msg","success");result.put("data",data);return result; }
}
