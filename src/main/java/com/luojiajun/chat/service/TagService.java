package com.luojiajun.chat.service;

import com.luojiajun.chat.common.Asserts;
import com.luojiajun.chat.common.ApiException;
import com.luojiajun.chat.common.ResultCode;
import com.luojiajun.chat.dto.TagCreateParam;
import com.luojiajun.chat.entity.ChatTag;
import com.luojiajun.chat.mapper.TagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagService {

    private final TagMapper tagMapper;

    public TagService(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    @Transactional
    public ChatTag create(Long userId, TagCreateParam param) {
        String name = param.getName().trim();
        boolean exists = tagMapper.selectByUser(userId).stream()
                .anyMatch(tag -> tag.getName().equalsIgnoreCase(name));
        if (exists) {
            throw new ApiException(ResultCode.VALIDATE_FAILED, "标签已存在");
        }
        ChatTag tag = new ChatTag();
        tag.setUserId(userId);
        tag.setName(name);
        tagMapper.insert(tag);
        tag.setCount(0);
        return tag;
    }

    public List<ChatTag> list(Long userId) {
        return tagMapper.selectByUser(userId);
    }

    @Transactional
    public void delete(Long userId, Long tagId) {
        ChatTag tag = tagMapper.selectByIdAndUser(tagId, userId);
        Asserts.notNull(tag, "标签不存在");
        tagMapper.deleteRelationsByTag(tagId);
        tagMapper.deleteByIdAndUser(tagId, userId);
    }
}
