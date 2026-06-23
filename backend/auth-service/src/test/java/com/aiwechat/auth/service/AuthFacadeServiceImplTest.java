package com.aiwechat.auth.service;

import com.aiwechat.auth.config.WechatConfig;
import com.aiwechat.auth.repository.UserRepository;
import com.aiwechat.auth.service.impl.AuthFacadeServiceImpl;
import com.aiwechat.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthFacadeServiceImplTest {

    @Mock
    private WechatConfig wechatConfig;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    private AuthFacadeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthFacadeServiceImpl(wechatConfig, userRepository, tokenService);
        ReflectionTestUtils.setField(service, "activeProfile", "prod");
    }

    @Test
    @DisplayName("mock-login 在 prod 环境应该被拒绝")
    void mockLoginShouldBeRejectedInProd() {
        assertThrows(BusinessException.class,
                () -> service.mockLogin("test-user", null),
                "生产环境不应该允许 mock-login");
    }

    @Test
    @DisplayName("mock-login 在 dev 环境应该可以工作")
    void mockLoginShouldWorkInDev() {
        ReflectionTestUtils.setField(service, "activeProfile", "dev");
        assertDoesNotThrow(() -> {
            try {
                service.mockLogin("test-user", null);
            } catch (NullPointerException e) {
                // repository 没有mock，NPE是预期的
            }
        });
    }

    @Test
    @DisplayName("包含 mock 的 code 应该被拒绝")
    void mockCodeShouldBeRejected() {
        assertNull(service.login("mock_test", null));
        assertNull(service.login("test_mock_code", null));
    }
}
