package com.luojiajun.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class FavoriteCreateParam {

    private Long sessionId;

    @NotBlank(message = "问题不能为空")
    private String question;

    @NotBlank(message = "回答不能为空")
    private String answer;

    @Size(max = 20)
    private String provider;

    @Size(max = 50)
    private String model;

    private List<Long> tagIds;
}
