package com.dreamuniverse_server.auth.config;
import com.dreamuniverse_server.auth.service.AccessTokenService;
import com.dreamuniverse_server.user.entity.*;
import com.dreamuniverse_server.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import javax.crypto.spec.SecretKeySpec;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AccountTokenTest {
    @Test void previouslyValidSignedAccessTokenIsRejectedAfterAccountDeletion() {
        var repository=mock(UserRepository.class);
        var user=User.create(OAuthProvider.GOOGLE,"identity");
        user.completeSignup("test@test.invalid","test","test",LocalDate.of(2000,1,1),"Asia/Seoul",Instant.now());
        when(repository.findById(7L)).thenReturn(Optional.of(user));
        var key=new SecretKeySpec(new byte[32],"HmacSHA256");
        var properties=new AuthProperties(new AuthProperties.Google("test"),new AuthProperties.Token("unused",900,3600,600),new AuthProperties.Kakao(null));
        var token=new AccessTokenService(key,properties).issue(7L);
        var decoder=new AuthConfig().accessTokenDecoder(key,repository);
        assertEquals("7",decoder.decode(token).getSubject());
        when(repository.findById(7L)).thenReturn(Optional.empty());
        assertThrows(BadJwtException.class,()->decoder.decode(token));
    }
}
