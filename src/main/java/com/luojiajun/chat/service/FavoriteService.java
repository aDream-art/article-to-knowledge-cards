package com.luojiajun.chat.service;

import com.github.pagehelper.PageHelper;
import com.luojiajun.chat.common.Asserts;
import com.luojiajun.chat.common.CommonPage;
import com.luojiajun.chat.dto.FavoriteCreateParam;
import com.luojiajun.chat.entity.ChatFavorite;
import com.luojiajun.chat.entity.ChatTag;
import com.luojiajun.chat.mapper.FavoriteMapper;
import com.luojiajun.chat.mapper.TagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 收藏存的是问答快照（ADR-0003），所以删了会话也不影响它。
 */
@Service
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final TagMapper tagMapper;

    public FavoriteService(FavoriteMapper favoriteMapper, TagMapper tagMapper) {
        this.favoriteMapper = favoriteMapper;
        this.tagMapper = tagMapper;
    }

    @Transactional
    public ChatFavorite create(Long userId, FavoriteCreateParam param) {
        ChatFavorite favorite = new ChatFavorite();
        favorite.setUserId(userId);
        favorite.setSessionId(param.getSessionId());
        favorite.setQuestion(param.getQuestion());
        favorite.setAnswer(param.getAnswer());
        favorite.setProvider(param.getProvider());
        favorite.setModel(param.getModel());
        favoriteMapper.insert(favorite);

        List<ChatTag> tags = resolveTags(userId, param.getTagIds());
        if (!tags.isEmpty()) {
            favoriteMapper.insertTagRelations(favorite.getId(),
                    tags.stream().map(ChatTag::getId).toList());
        }
        favorite.setTags(tags);
        return favorite;
    }

    public CommonPage<ChatFavorite> list(Long userId, int pageNum, int pageSize, Long tagId, String keyword) {
        PageHelper.startPage(pageNum, pageSize);
        List<ChatFavorite> favorites = favoriteMapper.selectPage(userId, tagId, keyword);
        CommonPage<ChatFavorite> page = CommonPage.restPage(favorites);
        attachTags(page.getList());
        return page;
    }

    @Transactional
    public void delete(Long userId, Long favoriteId) {
        ChatFavorite favorite = favoriteMapper.selectByIdAndUser(favoriteId, userId);
        Asserts.notNull(favorite, "收藏不存在");
        favoriteMapper.deleteRelationsByFavorite(favoriteId);
        favoriteMapper.deleteByIdAndUser(favoriteId, userId);
    }

    private void attachTags(List<ChatFavorite> favorites) {
        if (favorites == null || favorites.isEmpty()) {
            return;
        }
        List<Long> ids = favorites.stream().map(ChatFavorite::getId).toList();
        List<ChatTag> tagRows = favoriteMapper.selectTagsByFavoriteIds(ids);
        Map<Long, List<ChatTag>> grouped = new HashMap<>();
        for (ChatTag tag : tagRows) {
            grouped.computeIfAbsent(tag.getFavoriteId(), k -> new ArrayList<>()).add(tag);
        }
        for (ChatFavorite favorite : favorites) {
            favorite.setTags(grouped.getOrDefault(favorite.getId(), new ArrayList<>()));
        }
    }

    private List<ChatTag> resolveTags(Long userId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        List<ChatTag> tags = tagMapper.selectByIds(tagIds.stream().distinct().toList());
        boolean allMine = tags.stream().allMatch(tag -> Objects.equals(tag.getUserId(), userId));
        Asserts.isTrue(allMine && tags.size() == tagIds.stream().distinct().count(), "存在无效标签");
        return tags;
    }
}
