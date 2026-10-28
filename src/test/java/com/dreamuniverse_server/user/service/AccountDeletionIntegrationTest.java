package com.dreamuniverse_server.user.service;

import org.springframework.web.server.ResponseStatusException;
import com.dreamuniverse_server.user.entity.*;
import com.dreamuniverse_server.user.repository.*;
import com.dreamuniverse_server.auth.entity.*;
import com.dreamuniverse_server.auth.service.*;
import com.dreamuniverse_server.auth.config.AuthProperties;
import com.dreamuniverse_server.auth.exception.InvalidRefreshTokenException;
import com.dreamuniverse_server.ad.entity.*;
import com.dreamuniverse_server.dream.entity.*;
import com.dreamuniverse_server.dream.service.DreamImageService;
import com.dreamuniverse_server.dream.service.DreamCommandService;
import com.dreamuniverse_server.subscription.entity.Subscription;
import com.dreamuniverse_server.global.exception.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-only", "spring.flyway.enabled=false", "spring.liquibase.enabled=false"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Transactional(propagation=Propagation.NOT_SUPPORTED)
@Import({AccountDeletionService.class, AccountDeletionRepository.class, RefreshTokenService.class, UserService.class, DreamCommandService.class})
class AccountDeletionIntegrationTest {
    @Container static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }
    @Autowired AccountDeletionService accountDeletionService;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean DreamImageService dreamImageService;
    @Autowired DreamCommandService dreamCommandService;
    @Autowired RefreshTokenService refreshTokenService;
    @MockitoBean AuthProperties authProperties;
    @MockitoBean AccessTokenService accessTokenService;
    record Fixture(Long userId, Long dreamId, Long analysisId, Long sessionId, String identity, String key) {}
    Fixture seed() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            var identity = UUID.randomUUID().toString();
            var user = User.create(OAuthProvider.GOOGLE, identity);
            user.completeSignup(identity+"@test.invalid", "name", identity.substring(0,20), LocalDate.of(2000,1,1), "Asia/Seoul", Instant.now());
            entityManager.persist(user);
            var dream = Dream.create(user, "title", "content", LocalDate.now(), false, false, false);
            entityManager.persist(dream);
            var analysis = DreamAnalysis.create(dream, "interpretation", 1, 2, 3);
            entityManager.persist(analysis);
            entityManager.persist(DreamEmotion.create(analysis, DreamEmotionType.JOY, 50));
            entityManager.persist(DreamElement.create(analysis, DreamElementType.PERSON, "person"));
            String key = "dreams/"+dream.getId()+"/"+identity+".png";
            entityManager.persist(DreamImage.create(dream, key, 1));
            entityManager.persist(DreamAiUsage.create(user, LocalDate.now()));
            entityManager.persist(Subscription.createPremium(user, LocalDateTime.now(), LocalDateTime.now().plusDays(1)));
            var session = RefreshSession.create(user.getId(), Instant.now().plusSeconds(600));
            entityManager.persist(session);
            entityManager.persist(RefreshToken.create(identity, session.getId()));
            var reward = RewardedAdSession.create(user.getId(), Instant.now());
            reward.verify(identity, Instant.now());
            entityManager.persist(reward);
            entityManager.createNativeQuery("INSERT INTO auth_signup_sessions(provider,provider_user_id,token_hash,email,name,expires_at,created_at) VALUES ('GOOGLE',:identity,:hash,'test@test.invalid','test',UTC_TIMESTAMP()+INTERVAL 10 MINUTE,UTC_TIMESTAMP())")
                .setParameter("identity",identity).setParameter("hash",identity).executeUpdate();
            return new Fixture(user.getId(), dream.getId(), analysis.getId(), session.getId(), identity, key);
        });
    }
    long count(String table, String column, Object id) {
        return ((Number)entityManager.createNativeQuery("SELECT COUNT(*) FROM "+table+" WHERE "+column+" = :id").setParameter("id",id).getSingleResult()).longValue();
    }
    void assertData(Fixture f, long expected) {
        assertEquals(expected,count("users","id",f.userId()));
        assertEquals(expected,count("dreams","id",f.dreamId()));
        assertEquals(expected,count("dream_analyses","id",f.analysisId()));
        for (String table: List.of("dream_emotions","dream_elements")) assertEquals(expected,count(table,"analysis_id",f.analysisId()));
        assertEquals(expected,count("dream_images","dream_id",f.dreamId()));
        for (String table: List.of("dream_ai_usages","subscriptions","auth_refresh_sessions","rewarded_ad_sessions")) assertEquals(expected,count(table,"user_id",f.userId()));
        assertEquals(expected,count("auth_refresh_tokens","session_id",f.sessionId()));
        assertEquals(expected,count("auth_signup_sessions","provider_user_id",f.identity()));
    }
    @Test void deletesAllRelationsWithoutForeignKeyFailureAndPreservesOtherUser() {
        var target=seed(); var other=seed();
        accountDeletionService.delete(target.userId());
        assertData(target,0); assertData(other,1);
        verify(dreamImageService).deleteDreamImagesStrict(target.dreamId(),List.of(target.key()));
        verify(dreamImageService,never()).deleteDreamImagesStrict(eq(other.dreamId()),anyList());
        assertEquals(ErrorCode.UNAUTHORIZED, assertThrows(CustomException.class,()->accountDeletionService.delete(target.userId())).getErrorCode());
    }
    @Test void storageFailurePreservesAllDatabaseRecordsAndAllowsRetry() {
        var target=seed();
        doThrow(new IllegalStateException("storage unavailable")).when(dreamImageService).deleteDreamImagesStrict(eq(target.dreamId()),anyList());
        assertEquals(ErrorCode.USER_IMAGE_DELETE_FAILED,assertThrows(CustomException.class,()->accountDeletionService.delete(target.userId())).getErrorCode());
        assertData(target,1);
        doNothing().when(dreamImageService).deleteDreamImagesStrict(eq(target.dreamId()),anyList());
        accountDeletionService.delete(target.userId());
        assertData(target,0);
    }
    @Test void processingDreamPreventsDeletionAndNewOrphanUploads() {
        var target=seed();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> entityManager.createNativeQuery("UPDATE dreams SET ai_status='PROCESSING' WHERE id=:id").setParameter("id",target.dreamId()).executeUpdate());
        assertEquals(ErrorCode.USER_DELETE_WHILE_PROCESSING,assertThrows(CustomException.class,()->accountDeletionService.delete(target.userId())).getErrorCode());
        assertData(target,1); verifyNoInteractions(dreamImageService);
    }
    @Test void refreshTokenCannotBeRotatedOrNewSessionCreatedAfterDeletion() {
        var target=seed();
        when(authProperties.token()).thenReturn(new AuthProperties.Token("unused",900,3600,600));
        when(accessTokenService.issue(target.userId())).thenReturn("signed-test-token");
        var tokens=refreshTokenService.create(target.userId());
        var rotated=refreshTokenService.refresh(tokens.refreshToken());
        accountDeletionService.delete(target.userId());
        assertThrows(InvalidRefreshTokenException.class,()->refreshTokenService.refresh(rotated.refreshToken()));
        var failure=assertThrows(ResponseStatusException.class,()->refreshTokenService.create(target.userId()));
        assertEquals(401,failure.getStatusCode().value());
    }
    @Test void individualDreamDeletionAlsoPreservesKeysWhenStorageFails() {
        var target=seed();
        doThrow(new IllegalStateException("storage unavailable")).when(dreamImageService).deleteDreamImagesStrict(eq(target.dreamId()),anyList());
        assertEquals(ErrorCode.DREAM_IMAGE_DELETE_FAILED,assertThrows(CustomException.class,()->dreamCommandService.delete(target.dreamId(),target.userId())).getErrorCode());
        assertData(target,1);
        doNothing().when(dreamImageService).deleteDreamImagesStrict(eq(target.dreamId()),anyList());
        dreamCommandService.delete(target.dreamId(),target.userId());
        assertEquals(0,count("dreams","id",target.dreamId()));
        assertEquals(0,count("dream_images","dream_id",target.dreamId()));
        assertEquals(1,count("users","id",target.userId()));
    }
}
