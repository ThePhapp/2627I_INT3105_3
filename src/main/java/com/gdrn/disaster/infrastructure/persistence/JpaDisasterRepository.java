package com.gdrn.disaster.infrastructure.persistence;

import com.gdrn.disaster.application.*;
import com.gdrn.disaster.application.port.DisasterRepository;
import com.gdrn.disaster.domain.Disaster;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class JpaDisasterRepository implements DisasterRepository {
    private final EntityManager entityManager;
    public JpaDisasterRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override @Transactional
    public void add(Disaster disaster) {
        entityManager.persist(DisasterEntity.from(disaster));
        entityManager.flush();
    }

    @Override public Optional<Disaster> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(DisasterEntity.class, id)).map(DisasterEntity::toDomain);
    }

    @Override public Map<UUID, Disaster> findByIds(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return entityManager.createQuery("select d from DisasterEntity d where d.id in :ids", DisasterEntity.class)
                .setParameter("ids", ids).getResultList().stream().map(DisasterEntity::toDomain)
                .collect(Collectors.toUnmodifiableMap(Disaster::id, disaster -> disaster));
    }

    @Override public DisasterPage search(DisasterSearch search) {
        StringBuilder where = new StringBuilder(" where 1=1");
        if (search.status().isPresent()) where.append(" and d.status = :status");
        if (search.type().isPresent()) where.append(" and d.type = :type");
        String direction = search.sort() == DisasterSearch.Sort.CREATED_AT_ASC ? "asc" : "desc";
        var items = entityManager.createQuery("select d from DisasterEntity d" + where
                        + " order by d.createdAt " + direction + ", d.id asc", DisasterEntity.class);
        var count = entityManager.createQuery("select count(d) from DisasterEntity d" + where, Long.class);
        search.status().ifPresent(value -> { items.setParameter("status", value); count.setParameter("status", value); });
        search.type().ifPresent(value -> { items.setParameter("type", value); count.setParameter("type", value); });
        List<Disaster> content = items.setFirstResult(search.page() * search.size()).setMaxResults(search.size())
                .getResultList().stream().map(DisasterEntity::toDomain).toList();
        long total = count.getSingleResult();
        int totalPages = total == 0 ? 0 : (int) ((total + search.size() - 1) / search.size());
        return new DisasterPage(content, search.page(), search.size(), total, totalPages);
    }

    @Override @Transactional
    public boolean update(Disaster disaster, long expectedVersion) {
        int changed = entityManager.createQuery("""
                update DisasterEntity d set d.name=:name, d.type=:type, d.severity=:severity,
                    d.description=:description, d.latitude=:latitude, d.longitude=:longitude,
                    d.status=:status, d.version=:version, d.updatedAt=:updatedAt
                where d.id=:id and d.version=:expectedVersion
                """).setParameter("name", disaster.name()).setParameter("type", disaster.type())
                .setParameter("severity", disaster.severity()).setParameter("description", disaster.description())
                .setParameter("latitude", disaster.latitude()).setParameter("longitude", disaster.longitude())
                .setParameter("status", disaster.status()).setParameter("version", disaster.version())
                .setParameter("updatedAt", disaster.updatedAt()).setParameter("id", disaster.id())
                .setParameter("expectedVersion", expectedVersion).executeUpdate();
        entityManager.clear();
        return changed == 1;
    }
}
