package com.dreamuniverse_server.user.repository;

import com.dreamuniverse_server.user.entity.OAuthProvider;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountDeletionRepository {
    private final EntityManager entityManager;

    public void deleteRelatedData(Long userId, String provider, String providerUserId) {
        delete("delete from RefreshToken t where t.sessionId in (select s.id from RefreshSession s where s.userId = :id)", userId);
        delete("delete from RefreshSession s where s.userId = :id", userId);
        entityManager.createQuery("delete from SignupSession s where s.provider = :provider and s.providerUserId = :identity")
                .setParameter("provider", OAuthProvider.valueOf(provider))
                .setParameter("identity", providerUserId).executeUpdate();
        delete("delete from RewardedAdSession s where s.userId = :id", userId);
        delete("delete from DreamImage i where i.dream.id in (select d.id from Dream d where d.user.id = :id)", userId);
        delete("delete from DreamEmotion e where e.analysis.id in (select a.id from DreamAnalysis a where a.dream.user.id = :id)", userId);
        delete("delete from DreamElement e where e.analysis.id in (select a.id from DreamAnalysis a where a.dream.user.id = :id)", userId);
        delete("delete from DreamAnalysis a where a.dream.id in (select d.id from Dream d where d.user.id = :id)", userId);
        delete("delete from DreamAiUsage u where u.user.id = :id", userId);
        delete("delete from Subscription s where s.user.id = :id", userId);
        delete("delete from Dream d where d.user.id = :id", userId);
        delete("delete from User u where u.id = :id", userId);
        entityManager.clear();
    }

    private void delete(String query, Long userId) {
        entityManager.createQuery(query).setParameter("id", userId).executeUpdate();
    }
}
