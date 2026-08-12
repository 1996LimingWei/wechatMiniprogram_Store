package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.module.product.dal.dataobject.CategoryDO;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.dal.mysql.CategoryMapper;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import com.shop.common.exception.ServerException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductSpuMapper productSpuMapper;

    public List<CategoryDO> getEnabledList() {
        return categoryMapper.selectList(new LambdaQueryWrapper<CategoryDO>()
                .eq(CategoryDO::getStatus, 1)
                .orderByDesc(CategoryDO::getSort));
    }

    public List<CategoryDO> getAllList() {
        return categoryMapper.selectList(new LambdaQueryWrapper<CategoryDO>()
                .orderByDesc(CategoryDO::getSort));
    }

    public void create(CategoryDO category) {
        normalizeAndValidate(category, null);
        categoryMapper.insert(category);
    }

    public void update(CategoryDO category) {
        if (category == null || category.getId() == null || categoryMapper.selectById(category.getId()) == null) {
            throw new ServerException(404, "商品分类不存在");
        }
        normalizeAndValidate(category, category.getId());
        if (Integer.valueOf(0).equals(category.getStatus()) && hasOnSaleProduct(category.getId())) {
            throw new ServerException(400, "分类或其子分类下仍有上架商品，不能停用");
        }
        if (categoryMapper.updateById(category) != 1) {
            throw new ServerException(409, "分类信息已变化，请刷新后重试");
        }
    }

    public void delete(Long id) {
        CategoryDO category = id == null ? null : categoryMapper.selectById(id);
        if (category == null) {
            throw new ServerException(404, "商品分类不存在");
        }
        Long childCount = categoryMapper.selectCount(new LambdaQueryWrapper<CategoryDO>()
                .eq(CategoryDO::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new ServerException(400, "请先删除或迁移子分类");
        }
        Long productCount = productSpuMapper.selectCount(new LambdaQueryWrapper<ProductSpuDO>()
                .eq(ProductSpuDO::getCategoryId, id));
        if (productCount != null && productCount > 0) {
            throw new ServerException(400, "分类仍被商品引用，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    private void normalizeAndValidate(CategoryDO category, Long currentId) {
        if (category == null) throw new ServerException(400, "分类信息不能为空");
        String name = category.getName() == null ? "" : category.getName().trim();
        if (name.isEmpty() || name.length() > 64) {
            throw new ServerException(400, "分类名称长度应为 1 至 64 个字符");
        }
        category.setName(name);
        Long parentId = category.getParentId() == null ? 0L : category.getParentId();
        category.setParentId(parentId);
        if (currentId != null && currentId.equals(parentId)) {
            throw new ServerException(400, "分类不能选择自身作为父分类");
        }
        if (parentId > 0) {
            CategoryDO parent = categoryMapper.selectById(parentId);
            if (parent == null) throw new ServerException(400, "父分类不存在");
            Set<Long> visited = new HashSet<>();
            while (parent != null && parent.getParentId() != null && parent.getParentId() > 0) {
                if (!visited.add(parent.getId()) || (currentId != null && currentId.equals(parent.getId()))) {
                    throw new ServerException(400, "分类层级不能形成循环");
                }
                parent = categoryMapper.selectById(parent.getParentId());
            }
        }
        LambdaQueryWrapper<CategoryDO> duplicate = new LambdaQueryWrapper<CategoryDO>()
                .eq(CategoryDO::getParentId, parentId)
                .eq(CategoryDO::getName, name);
        if (currentId != null) duplicate.ne(CategoryDO::getId, currentId);
        if (categoryMapper.selectCount(duplicate) > 0) {
            throw new ServerException(400, "同级分类名称不能重复");
        }
        if (category.getStatus() == null || (category.getStatus() != 0 && category.getStatus() != 1)) {
            throw new ServerException(400, "分类状态不正确");
        }
        if (category.getSort() == null) category.setSort(0);
        if (category.getIcon() != null && category.getIcon().length() > 512) {
            throw new ServerException(400, "分类图标地址过长");
        }
    }

    private boolean hasOnSaleProduct(Long categoryId) {
        Set<Long> ids = new HashSet<>();
        ids.add(categoryId);
        boolean changed;
        do {
            int before = ids.size();
            categoryMapper.selectList(new LambdaQueryWrapper<CategoryDO>()
                            .in(CategoryDO::getParentId, new HashSet<>(ids)))
                    .forEach(category -> ids.add(category.getId()));
            changed = ids.size() > before;
        } while (changed);
        return productSpuMapper.selectCount(new LambdaQueryWrapper<ProductSpuDO>()
                .in(ProductSpuDO::getCategoryId, ids)
                .eq(ProductSpuDO::getStatus, 1)) > 0;
    }
}
