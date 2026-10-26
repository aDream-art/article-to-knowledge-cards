package com.luojiajun.chat.mapper;

import com.luojiajun.chat.entity.ChatFavorite;
import com.luojiajun.chat.entity.ChatTag;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FavoriteMapper {

    int insert(ChatFavorite favorite);

    List<ChatFavorite> selectPage(@Param("userId") Long userId,
                                  @Param("tagId") Long tagId,
                                  @Param("keyword") String keyword);

    ChatFavorite selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    int insertTagRelations(@Param("favoriteId") Long favoriteId, @Param("tagIds") List<Long> tagIds);

    int deleteRelationsByFavorite(@Param("favoriteId") Long favoriteId);

    List<ChatTag> selectTagsByFavoriteIds(@Param("favoriteIds") List<Long> favoriteIds);
}
