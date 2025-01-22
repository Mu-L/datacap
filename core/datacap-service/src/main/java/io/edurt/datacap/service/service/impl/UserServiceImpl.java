package io.edurt.datacap.service.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.edurt.datacap.common.enums.ServiceState;
import io.edurt.datacap.common.response.CommonResponse;
import io.edurt.datacap.common.response.JwtResponse;
import io.edurt.datacap.common.utils.CodeUtils;
import io.edurt.datacap.common.utils.JsonUtils;
import io.edurt.datacap.common.utils.NullAwareBeanUtils;
import io.edurt.datacap.fs.FsRequest;
import io.edurt.datacap.fs.FsResponse;
import io.edurt.datacap.fs.FsService;
import io.edurt.datacap.plugin.Plugin;
import io.edurt.datacap.plugin.PluginManager;
import io.edurt.datacap.service.adapter.PageRequestAdapter;
import io.edurt.datacap.service.audit.AuditUserLog;
import io.edurt.datacap.service.body.FilterBody;
import io.edurt.datacap.service.body.UserNameBody;
import io.edurt.datacap.service.body.UserPasswordBody;
import io.edurt.datacap.service.entity.MenuEntity;
import io.edurt.datacap.service.entity.PageEntity;
import io.edurt.datacap.service.entity.RoleEntity;
import io.edurt.datacap.service.entity.SourceEntity;
import io.edurt.datacap.service.entity.UserEntity;
import io.edurt.datacap.service.entity.itransient.user.UserEditorEntity;
import io.edurt.datacap.service.initializer.InitializerConfigure;
import io.edurt.datacap.service.model.AiModel;
import io.edurt.datacap.service.record.TreeRecord;
import io.edurt.datacap.service.repository.RoleRepository;
import io.edurt.datacap.service.repository.SourceRepository;
import io.edurt.datacap.service.repository.UserRepository;
import io.edurt.datacap.service.repository.admin.MenuRepository;
import io.edurt.datacap.service.security.UserDetailsService;
import io.edurt.datacap.service.service.JwtService;
import io.edurt.datacap.service.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "EI_EXPOSE_REP2"})
@Service
@Slf4j
public class UserServiceImpl
        implements UserService
{
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SourceRepository sourceRepository;
    private final MenuRepository menuRepository;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RedisTemplate redisTemplate;
    private final Environment environment;
    private final InitializerConfigure initializerConfigure;
    private final PluginManager pluginManager;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, SourceRepository sourceRepository, MenuRepository menuRepository, PasswordEncoder encoder, AuthenticationManager authenticationManager, JwtService jwtService, RedisTemplate redisTemplate, Environment environment, InitializerConfigure initializerConfigure, PluginManager pluginManager)
    {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.sourceRepository = sourceRepository;
        this.menuRepository = menuRepository;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
        this.environment = environment;
        this.initializerConfigure = initializerConfigure;
        this.pluginManager = pluginManager;
    }

    @Override
    public CommonResponse<UserEntity> saveOrUpdate(UserEntity configure)
    {
        if (ObjectUtils.isEmpty(configure.getId())) {
            Optional<UserEntity> userOptional = this.userRepository.findByUsername(configure.getUsername());
            if (userOptional.isPresent()) {
                return CommonResponse.failure(ServiceState.USER_EXISTS);
            }
            configure.setCode(CodeUtils.generateCode(false));
            configure.setUsername(configure.getUsername());
            configure.setPassword(encoder.encode(configure.getPassword()));
            Set<RoleEntity> userRoles = configure.getRoles();
            Set<RoleEntity> roles = new HashSet<>();
            if (ObjectUtils.isEmpty(userRoles)) {
                Optional<RoleEntity> userRoleOptional = roleRepository.findByName("User");
                if (!userRoleOptional.isPresent()) {
                    return CommonResponse.failure(ServiceState.USER_ROLE_NOT_FOUND);
                }
                roles.add(userRoleOptional.get());
            }
            configure.setRoles(roles);
        }
        else {
            userRepository.findById(configure.getId())
                    .ifPresent(value -> {
                        NullAwareBeanUtils.copyNullProperties(value, configure);
                        if (StringUtils.isEmpty(value.getCode())) {
                            value.setCode(CodeUtils.generateCode(false));
                        }
                    });
            configure.setRoles(configure.getRoles());
        }
        return CommonResponse.success(userRepository.save(configure));
    }

    @AuditUserLog
    @Override
    public CommonResponse<Object> authenticate(UserEntity configure)
    {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(configure.getUsername(), configure.getPassword());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtService.generateJwtToken(authentication);

        UserDetailsService userDetails = (UserDetailsService) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return CommonResponse.success(new JwtResponse(jwt, userDetails.getCode(), userDetails.getUsername(), roles, userDetails.getAvatar()));
    }

    @Override
    public CommonResponse<UserEntity> info(String code)
    {
        return userRepository.findByCode(UserDetailsService.getUser().getCode())
                .map(CommonResponse::success)
                .orElseGet(() -> CommonResponse.failure(ServiceState.USER_NOT_FOUND));
    }

    @Override
    public CommonResponse<Long> changePassword(UserPasswordBody configure)
    {
        Optional<UserEntity> userOptional = this.userRepository.findById(UserDetailsService.getUser().getId());
        if (!userOptional.isPresent()) {
            return CommonResponse.failure(ServiceState.USER_NOT_FOUND);
        }
        UserEntity user = userOptional.get();
        if (!encoder.matches(configure.getOldPassword(), user.getPassword())) {
            return CommonResponse.failure(ServiceState.USER_PASSWORD_INCORRECT);
        }
        if (!configure.getNewPassword().equals(configure.getConfirmPassword())) {
            return CommonResponse.failure(ServiceState.USER_PASSWORD_DIFFERENT);
        }
        user.setPassword(encoder.encode(configure.getNewPassword()));
        this.userRepository.save(user);
        return CommonResponse.success(user.getId());
    }

    @Override
    public CommonResponse<Long> changeUsername(UserNameBody configure)
    {
        Optional<UserEntity> userOptional = this.userRepository.findById(UserDetailsService.getUser().getId());
        if (!userOptional.isPresent()) {
            return CommonResponse.failure(ServiceState.USER_NOT_FOUND);
        }
        UserEntity user = userOptional.get();
        if (!encoder.matches(configure.getPassword(), user.getPassword())) {
            return CommonResponse.failure(ServiceState.USER_PASSWORD_INCORRECT);
        }
        if (configure.getNewUsername().equals(user.getUsername())) {
            return CommonResponse.failure(ServiceState.USER_NAME_EQUALS);
        }
        user.setUsername(configure.getNewUsername());
        this.userRepository.save(user);
        return CommonResponse.success(user.getId());
    }

    @Override
    public CommonResponse<Long> changeThirdConfigure(AiModel configure)
    {
        Optional<UserEntity> userOptional = this.userRepository.findById(UserDetailsService.getUser().getId());
        if (!userOptional.isPresent()) {
            return CommonResponse.failure(ServiceState.USER_NOT_FOUND);
        }
        UserEntity user = userOptional.get();
        user.setChatConfigure(JsonUtils.toJSON(configure));
        this.userRepository.save(user);
        return CommonResponse.success(user.getId());
    }

    @Override
    public CommonResponse<List<Object>> getSugs(Long id)
    {
        Optional<SourceEntity> sourceEntityOptional = sourceRepository.findById(id);
        if (!sourceEntityOptional.isPresent()) {
            return CommonResponse.failure(ServiceState.SOURCE_NOT_FOUND);
        }

        SourceEntity entity = sourceEntityOptional.get();
        String key = String.join("_", entity.getType(), entity.getId().toString());
        int sugsMaxSize = Integer.parseInt(environment.getProperty("datacap.editor.sugs.maxSize"));
        List<Object> sugs = (List<Object>) redisTemplate.opsForSet()
                .members(key)
                .stream()
                .limit(sugsMaxSize)
                .collect(Collectors.toList());
        return CommonResponse.success(sugs);
    }

    @Override
    public CommonResponse<List<TreeRecord>> getMenus()
    {
        Map<Long, TreeRecord> treeMap = new ConcurrentHashMap<>();
        Optional<UserEntity> optionalUser = userRepository.findByCode(UserDetailsService.getUser().getCode());
        UserEntity user = optionalUser.get();
        List<TreeRecord> tree = new ArrayList<>();
        user.getRoles().forEach(role -> {
            List<MenuEntity> menuList = StreamSupport.stream(role.getMenus().spliterator(), false)
                    .sorted(Comparator.comparing(MenuEntity::getParent))
                    .collect(Collectors.toList());
            // Sets the parent menu sort
            menuList.forEach(menu -> {
                if (menu.getParent() == 0) {
                    TreeRecord parent = TreeRecord.of(menu, true, true, Lists.newArrayList());
                    parent.setNew(menu.isNew());
                    treeMap.put(menu.getId(), parent);
                }
                else {
                    TreeRecord temp = treeMap.get(menu.getParent());
                    if (temp != null) {
                        List<TreeRecord> childrens = temp.getChildren();
                        if (ObjectUtils.isEmpty(childrens)) {
                            childrens = Lists.newArrayList();
                        }
                        TreeRecord children = TreeRecord.of(menu, true, true, Lists.newArrayList());
                        children.setNew(menu.isNew());
                        children.setDescription(menu.getDescription());
                        childrens.add(children);
                        childrens.sort(Comparator.comparing(TreeRecord::getSorted));
                        temp.setChildren(childrens);
                        treeMap.put(temp.getId(), temp);
                    }
                    else {
                        // Handle cases where no parent menu is selected, only child menus are selected
                        menuRepository.findById(menu.getParent())
                                .ifPresent(v -> {
                                    TreeRecord parent = TreeRecord.of(v, true, true, Lists.newArrayList());
                                    parent.setNew(v.isNew());
                                    List<TreeRecord> children = Lists.newArrayList();
                                    children.add(TreeRecord.of(menu, true, true, Lists.newArrayList()));
                                    parent.setChildren(children);
                                    treeMap.put(v.getId(), parent);
                                });
                    }
                }
            });
            treeMap.keySet().forEach(v -> tree.add(treeMap.get(v)));
        });
        tree.sort(Comparator.comparing(TreeRecord::getSorted));
        return CommonResponse.success(tree);
    }

    @Override
    public CommonResponse<PageEntity<UserEntity>> getAll(FilterBody filter)
    {
        Pageable pageable = PageRequestAdapter.of(filter);
        return CommonResponse.success(PageEntity.build(this.userRepository.findAll(pageable)));
    }

    @Override
    public CommonResponse<Long> changeEditorConfigure(UserEditorEntity configure)
    {
        Optional<UserEntity> userOptional = this.userRepository.findById(UserDetailsService.getUser().getId());
        if (!userOptional.isPresent()) {
            return CommonResponse.failure(ServiceState.USER_NOT_FOUND);
        }
        UserEntity user = userOptional.get();
        user.setEditorConfigure(configure);
        this.userRepository.save(user);
        return CommonResponse.success(user.getId());
    }

    @Override
    public CommonResponse<FsResponse> uploadAvatar(MultipartFile file)
    {
        if (file == null || file.isEmpty()) {
            return CommonResponse.failure("Upload file cannot be empty");
        }

        return pluginManager.getPlugin(initializerConfigure.getFsConfigure().getType())
                .map(plugin -> processAvatarUpload(plugin, file))
                .map(response -> processUploadResult(response, file))
                .orElse(CommonResponse.failure(String.format("Not found Fs [%s]",
                        initializerConfigure.getFsConfigure().getType())));
    }

    private FsResponse processAvatarUpload(Plugin plugin, MultipartFile file)
    {
        UserEntity user = UserDetailsService.getUser();
        try {
            String avatarPath = initializerConfigure.getAvatarPath();
            log.info("Upload avatar user [{}] home [{}]", user.getUsername(), avatarPath);

            FsRequest fsRequest = buildFsRequest(user, avatarPath, file);
            FsService fsService = plugin.getService(FsService.class);
            return fsService.writer(fsRequest);
        }
        catch (IOException e) {
            log.error("Failed to process avatar upload for user [{}]", user.getUsername(), e);
            throw new IllegalStateException("Failed to process avatar upload", e);
        }
    }

    private FsRequest buildFsRequest(UserEntity user, String avatarPath, MultipartFile file)
            throws IOException
    {
        return FsRequest.builder()
                .access(initializerConfigure.getFsConfigure().getAccess())
                .secret(initializerConfigure.getFsConfigure().getSecret())
                .endpoint(avatarPath)
                .bucket(initializerConfigure.getFsConfigure().getBucket())
                .stream(file.getInputStream())
                .fileName(String.format("%s.png", user.getId()))
                .build();
    }

    private CommonResponse<FsResponse> processUploadResult(FsResponse response, MultipartFile file)
    {
        try {
            UserEntity user = UserDetailsService.getUser();
            UserEntity entity = userRepository.findById(user.getId())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + user.getId()));

            Map<String, String> avatar = createAvatarConfig(response, file);
            entity.setAvatarConfigure(avatar);
            userRepository.save(entity);

            return CommonResponse.success(response);
        }
        catch (Exception e) {
            log.error("Failed to process upload result", e);
            return CommonResponse.failure("Failed to process upload result: " + e.getMessage());
        }
    }

    private Map<String, String> createAvatarConfig(FsResponse response, MultipartFile file)
            throws IOException
    {
        Map<String, String> avatar = Maps.newConcurrentMap();
        String fsType = initializerConfigure.getFsConfigure().getType();

        avatar.put("fsType", fsType);
        avatar.put("local", response.getRemote());

        if ("Local".equals(fsType)) {
            avatar.put("path", encodeImageToBase64(file.getInputStream()));
        }
        else {
            avatar.put("path", response.getRemote());
        }

        return avatar;
    }

    private String encodeImageToBase64(InputStream inputStream)
            throws IOException
    {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return String.format("data:image/jpeg;base64,%s", Base64.getEncoder().encodeToString(outputStream.toByteArray()));
        }
        catch (IOException e) {
            log.warn("Encode image to base64 exception", e);
            return null;
        }
        finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
}
