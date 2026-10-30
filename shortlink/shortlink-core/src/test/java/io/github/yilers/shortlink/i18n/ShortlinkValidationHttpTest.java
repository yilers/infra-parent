package io.github.yilers.shortlink.i18n;

import io.github.yilers.shortlink.request.ShortlinkDomainRequest;
import io.github.yilers.web.exception.GlobalExceptionHandler;
import io.github.yilers.web.i18n.InfraMessageResolver;
import io.github.yilers.web.i18n.LocalizedTextResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShortlinkValidationHttpTest {

    @Test
    void defaultValidationMessagesFollowRequestLanguage() throws Exception {
        InfraMessageResolver resolver = new InfraMessageResolver();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ValidationController())
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler(resolver, new LocalizedTextResolver(resolver, List.of())))
                .build();
        try {
            assertTrue(validate(mvc, "en-US").contains("must not be blank"));
            assertTrue(validate(mvc, "zh-CN").contains("不能为空"));
            assertTrue(validate(mvc, null).contains("不能为空"));
            assertTrue(validate(mvc, "fr-FR").contains("不能为空"));
            String longHost = "a".repeat(254);
            assertTrue(validate(mvc, "en-US", longHost).contains("size must be between 0 and 253"));
            assertTrue(validate(mvc, "fr-FR", longHost).contains("长度需要在0和253之间"));
            String custom = mvc.perform(post("/test/custom-validation").contentType(MediaType.APPLICATION_JSON)
                            .header("Accept-Language", "en-US").content("{\"name\":\"\"}"))
                    .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
            assertTrue(custom.contains("自定义校验"));
        } finally {
            validator.close();
        }
    }

    private String validate(MockMvc mvc, String language) throws Exception {
        return validate(mvc, language, "");
    }

    private String validate(MockMvc mvc, String language, String host) throws Exception {
        var request = post("/test/shortlink-domain").contentType(MediaType.APPLICATION_JSON)
                .content("{\"host\":\"" + host + "\"}");
        if (language != null) {
            request.header("Accept-Language", language);
        }
        return mvc.perform(request).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
    }

    @RestController
    static class ValidationController {

        @PostMapping("/test/shortlink-domain")
        void validate(@Valid @RequestBody ShortlinkDomainRequest request) {
        }

        @PostMapping("/test/custom-validation")
        void validateCustom(@Valid @RequestBody CustomRequest request) {
        }
    }

    record CustomRequest(@NotBlank(message = "自定义校验") String name) {
    }
}
