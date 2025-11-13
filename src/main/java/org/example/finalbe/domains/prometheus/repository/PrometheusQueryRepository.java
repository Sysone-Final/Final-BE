package org.example.finalbe.domains.prometheus.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PrometheusQueryRepository {

    @PersistenceContext(unitName = "appEntityManagerFactory")
    private final EntityManager entityManager;

    public List<Map<String, Object>> executeNativeQuery(String query, Map<String, Object> params) {
        var nativeQuery = entityManager.createNativeQuery(query);

        if (params != null) {
            params.forEach(nativeQuery::setParameter);
        }

        return nativeQuery.getResultList();
    }

    public Object executeSingleResultQuery(String query, Map<String, Object> params) {
        var nativeQuery = entityManager.createNativeQuery(query);

        if (params != null) {
            params.forEach(nativeQuery::setParameter);
        }

        return nativeQuery.getSingleResult();
    }
}