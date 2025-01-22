package io.edurt.datacap.service.service.impl;

import io.edurt.datacap.common.response.CommonResponse;
import io.edurt.datacap.service.adapter.PageRequestAdapter;
import io.edurt.datacap.service.body.FilterBody;
import io.edurt.datacap.service.entity.ChatEntity;
import io.edurt.datacap.service.entity.PageEntity;
import io.edurt.datacap.service.repository.BaseRepository;
import io.edurt.datacap.service.repository.ChatRepository;
import io.edurt.datacap.service.security.UserDetailsService;
import io.edurt.datacap.service.service.ChatService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl
        implements ChatService
{
    private final ChatRepository repository;

    public ChatServiceImpl(ChatRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CommonResponse<PageEntity<ChatEntity>> getAllByUser(FilterBody filter)
    {
        Pageable pageable = PageRequestAdapter.of(filter);
        return CommonResponse.success(PageEntity.build(repository.findAllByUser(UserDetailsService.getUser(), pageable)));
    }

    @Override
    public CommonResponse<ChatEntity> saveOrUpdate(BaseRepository<ChatEntity, Long> repository, ChatEntity configure)
    {
        configure.setUser(UserDetailsService.getUser());
        return CommonResponse.success(repository.save(configure));
    }
}
